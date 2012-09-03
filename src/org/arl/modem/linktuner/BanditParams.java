package org.arl.modem.linktuner;

public class BanditParams {
	protected double alpha;
	protected double beta;
	protected double gittinsIndex;
	protected double normalizedIndex;
	
	protected BanditParams(){
		this.alpha=1;
		this.beta=1;
		this.gittinsIndex=1.0;			//for alpha=1, beta=1, gittins index = 0.61452
		this.normalizedIndex=0;
	}

	public BanditParams(double alpha, double beta, double gittinsIndex,
			double normalizedIndex) {
		super();
		this.alpha = alpha;
		this.beta = beta;
		this.gittinsIndex = gittinsIndex;
		this.normalizedIndex = normalizedIndex;
	}

	public Double[] getBanditParams(){
		Double[] _bandit_params=new Double[4];
		_bandit_params[0]=alpha;
		_bandit_params[1]=beta;
		_bandit_params[2]=gittinsIndex;
		_bandit_params[3]=normalizedIndex;
		return _bandit_params.clone();
	}
	public double getAlpha(){
		return alpha;
	}
	public double getBeta(){
		return beta;
	}
	public double getGittinsIndex(){
		return gittinsIndex;
	}
	public double getNormIndex(){
		return normalizedIndex;
	}
	
	public String toString(){
		return new String(alpha+" "+beta+" "+gittinsIndex+" "+normalizedIndex);
	}
}
