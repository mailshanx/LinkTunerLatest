package org.arl.modem.linktuner;

import java.util.ArrayList;
import java.util.List;

public class KalmanFilter {
	private Double Q;		//tuning parameter for effective avging time (process variance)
	private Double R;		//measurement error
//	private List<Double> a_prior_est_list;
//	private List<Double> a_post_est_list;
//	private List<Double> stateCovariancePosterioriEstimates;
	private Double aPrioriEst;
	private Double aPosterioriEst;
	private Double stateCov_aPriori;
	private Double stateCov_aPosteriori;
	private Double gain;
	public KalmanFilter(Double _R){
		Q=0.00001;
		aPrioriEst=1.0;		//we are highly optimistic that the packet will succeed
		aPosterioriEst=aPrioriEst;
		stateCov_aPriori=5.0;
		gain=1.0;
		stateCov_aPosteriori=stateCov_aPriori;
		R=_R;
	}
	public KalmanFilter getFilterCopy(){
		KalmanFilter filterCopy=new KalmanFilter(this.R);
		filterCopy.Q=this.Q;
		filterCopy.aPrioriEst=this.aPrioriEst;
		filterCopy.aPosterioriEst=this.aPosterioriEst;
		filterCopy.stateCov_aPriori=this.stateCov_aPriori;
		filterCopy.stateCov_aPosteriori=this.stateCov_aPosteriori;
		return filterCopy;
	}
	//run this at every time step
	public void performTimeUpdate(){
		stateCov_aPriori=stateCov_aPosteriori+Q;
		aPrioriEst=aPosterioriEst;
	}
	
	public void measurementUpdate(Double _measurement){
		gain=stateCov_aPriori/(stateCov_aPriori+R);
		aPosterioriEst=aPrioriEst+(gain*(_measurement-aPrioriEst));
//		if(aPosterioriEst<0.0)
//			aPosterioriEst=0.0;
//		if(aPosterioriEst>1.0)
//			aPosterioriEst=1.0;
		stateCov_aPosteriori=(1-gain)*stateCov_aPriori;
	}

	public void setR(Double _R){
		this.R=_R;
	}
	
	public Double getEstimate(){
		return aPosterioriEst;
	}
	
	public Double getStateCov(){
		return stateCov_aPosteriori;
	}
	public Double getGain(){
		return gain;
	}
	public String toString(){
		return aPosterioriEst+" "+aPrioriEst+" "+stateCov_aPosteriori+" "+stateCov_aPriori+" "+gain;
	}
	
}
