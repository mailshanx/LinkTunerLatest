package org.arl.modem.linktuner;

import java.util.ArrayList;
import java.util.List;

public class GittinsIndexGenerator {

	private double gamma=0.75;		//discount factor

	int n=12;       //No of lookahead levels in calculating reward function
	int maxIter=12; //max iterations to find zero of function f.
	double upperBound=1.0;
	double lowerBound=0.0;
	
	KalmanFilter filter;
	Double bandit_reward;
	Double current_measurement;


	protected double gittinsIndex(Double _measurement, KalmanFilter _filter, Double _bandit_reward){
		this.filter=_filter.getFilterCopy();
		this.bandit_reward=_bandit_reward;
		this.current_measurement=_measurement;
		return indexSearch(upperBound, lowerBound, maxIter)*bandit_reward;
	}
	private double indexSearch(double upperBound, double lowerBound, int maxIter){
		double midPoint = 0;
		for (int i = 0; i < maxIter; i++) {
			midPoint=(upperBound+lowerBound)/2.0;
			double V_ub=f(upperBound);
			double V_lb=f(lowerBound);
			double V_mp=f(midPoint);
			if(V_ub==0){
				return upperBound;
			}
			if(V_lb==0){
				return lowerBound;
			}
			if(V_mp==0){
				return midPoint;
			}
			if(V_ub*V_mp<0){
				lowerBound=midPoint;
			}
			else if(V_lb*V_mp<0){
				upperBound=midPoint;
			}			
		}
		return midPoint;
	}
	private double f(double nu){
		return value_func(current_measurement, filter, nu, n, 1, false) - (nu/(1-gamma));
	}
	private double value_func(Double _measurement, KalmanFilter _filter, double rho, int n, int m, boolean updateFilter){
		double v;
		double p;
		double v0, v1, ev;
		Double current_measurement = _measurement;
		KalmanFilter my_filter=_filter.getFilterCopy();
		if(n<=0){
			v=0;
		}else{
			if(updateFilter){
				my_filter.performTimeUpdate();
				my_filter.measurementUpdate(current_measurement);				
			}
			p=my_filter.getEstimate();
			v0=value_func(0.0, my_filter.getFilterCopy(), rho, n-1, 0, true);
			v1=value_func(1.0, my_filter.getFilterCopy(), rho, n-1, 0, true);
//			ev=(p*bandit_reward) + gamma*(p*v1*bandit_reward + (1-p)*v0);
			ev=p + gamma*(p*v1+ (1-p)*v0);
			if(m==1){
				v=ev;
			}else{
				v=Math.max(rho/(1-gamma), ev);
			}
		}
		return v;
	}
	
	
}
