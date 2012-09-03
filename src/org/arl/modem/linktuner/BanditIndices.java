package org.arl.modem.linktuner;

//class to calculate the gittins index. just call BanditIndices.index(alpha,beta,gamma) to get the 
//gittins index of an (alpha,beta,gamma) arm.

public class BanditIndices {

	private double alpha;
	private double beta;
	private double gamma;

	int n=12;       //No of lookahead levels in calculating reward function
	int maxIter=12; //max iterations to find zero of function f.
	double upperBound=1.0;
	double lowerBound=0.0;
	
	protected double gittinsIndex(double _alpha, double _beta, double _gamma){
		this.alpha=_alpha;
		this.beta=_beta;
		this.gamma=_gamma;
		return indexSearch(upperBound, lowerBound, maxIter);
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
		return reward(alpha, beta, nu, n, 1) - (nu/(1-gamma));
	}
	private double reward(double a,double b,double rho,int n,int m){
		 double v;
		 double p;
		 double v0,v1,ev;
		 if(n<=0){
			 v=0;
		 }
		 else{
			 if(a==b){
				 p=.5; 
			 }else{
				 p=a/(a+b);
			 }
			 v0=reward(a,b+1,rho,n-1,0);
			 v1=reward(a+1,b,rho,n-1,0);
			 ev=p + gamma*( (1-p)*v0 + p*v1 );
			 if(m==1){
				 v=ev;
			 }else{
				 v=Math.max(rho/(1-gamma),ev);
			 }
		 }
		 return v;
	 }
}
