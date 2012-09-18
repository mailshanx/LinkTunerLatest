package org.arl.modem.linktuner;

public class BanditParamNtf {
	
	private String NtfSrc="";
	private String msg="";
	private int[] bandit_updated;
	protected double BsIncrement=-1.0;
	protected double BfIncrement=-1.0;
	protected double NsIncrement=-1.0;
	protected double NfIncrement=-1.0;
	
	private Double max_reward=-1.0;
	
	public BanditParamNtf(String _nft_src, String _msg, int[] _bandit_updated, double _Bs_incr, double _Bf_incr, double _Ns_incr, double _Nf_incr){
		this.NtfSrc=_nft_src;
		this.msg=_msg;
		this.bandit_updated=_bandit_updated.clone();
		this.BsIncrement=_Bs_incr;
		this.BfIncrement=_Bf_incr;
		this.NsIncrement=_Ns_incr;
		this.NfIncrement=_Nf_incr;
	}
	
	public BanditParamNtf(String _ntf_src, String _msg, Double _max_reward){
		this.NtfSrc=_ntf_src;
		this.msg=_msg;
		this.max_reward=_max_reward;
	}
	
	public Double getMaxReward(){
		return max_reward;
	}
	
	public String getNtfSrc(){
		return NtfSrc;
	}
	
	public String getMsg(){
		return msg;
	}
	
	public int[] getBandit(){
		return bandit_updated;
	}
	
}
