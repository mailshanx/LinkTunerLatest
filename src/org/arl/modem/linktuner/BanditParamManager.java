package org.arl.modem.linktuner;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.*;


public class BanditParamManager {
	private Logger log;
	protected Exploit exploit;
	protected Map<String, BanditParams> bandit_param_collection;
	private int[][] brigands;
	private BanditParamStatusPrinter bandit_param_status_printer;
	public final static String NTF_ID="BanditParamManagerNtfId";
	public final static String BanditParamUpdateMsg="please_update_bandit_params";
	public final static String NormalizeBanditReward="please_normalize_bandit_reward";
	
	public BanditParamManager(Exploit _exploit, Logger _log, int[][] _brigands){
		this.exploit=_exploit; 
		this.log = _log;
		this.brigands=_brigands;
		this.bandit_param_collection=new LinkedHashMap<String, BanditParams>(brigands.length);
		hardResetBanditParams(brigands);
		this.bandit_param_status_printer = new BanditParamStatusPrinter(bandit_param_collection, log);			
	}
	
	public void updateBanditParams(int[][] _brigands, int banditCurrent){
		brigands=_brigands;
		int[] bandit_updated=brigands[banditCurrent];
		String bandit_updated_as_str=StrRepr.strRepr(bandit_updated);
		//code to update the new bandit version
		int _fec_index=ParamSetter.Scheme_Params_map_keylist.indexOf("FEC");
		int fec = ParamSetter.Scheme_Params_values_list.get(_fec_index).get(bandit_updated[_fec_index]);
		double _Ns_incr=0.0, _Nf_incr=0.0, _Bs_incr=0.0, _Bf_incr=0.0;
		if(fec==0){ //test packet
			double _bit_cnt=exploit.getStats(Exploit.GET_RECENT_BIT_CNT);
			double _fail_bit_cnt=exploit.getStats(Exploit.GET_RECENT_ERROR_CNT);
			double _success_bit_cnt=_bit_cnt-_fail_bit_cnt;
			_Bs_incr= _success_bit_cnt;
			_Bf_incr= _fail_bit_cnt;
		}else{		//data packet
			if (exploit.getStats(Exploit.GET_RECENT_ERROR_CNT)>0.0 || exploit.getStats(Exploit.GET_RECENT_RX_TESTPKT_CNT)==0.0) {
				_Nf_incr= exploit.getStats(Exploit.GET_RECENT_RX_TESTPKT_CNT);
			}else if (exploit.getStats(Exploit.GET_RECENT_ERROR_CNT)==0.0 && exploit.getStats(Exploit.GET_RECENT_RX_TESTPKT_CNT)>0) {
				_Ns_incr= exploit.getStats(Exploit.GET_RECENT_RX_TESTPKT_CNT);
			}
		}
		for(BanditParams _bandit_params:bandit_param_collection.values()){
			_bandit_params.handleLinktunerNotifications(new BanditParamNtf(NTF_ID, BanditParamUpdateMsg,
														bandit_updated, _Bs_incr, _Bf_incr, _Ns_incr, _Nf_incr));
		}
		if(!bandit_param_collection.containsKey(bandit_updated_as_str)){
			throw new RuntimeException("bandit_params does not contain params for bandit "+bandit_updated_as_str);
		}
	}
	public void hardResetBanditParams(int[][] _brigands){
		brigands=_brigands.clone();
		log.fine("hard-reset of bandit_params");
		//code to hard reset newer bandit version
		bandit_param_collection=new LinkedHashMap<String, BanditParams>(brigands.length);
		for(int[] _bandit:brigands){
			String _bandit_str=StrRepr.strRepr(_bandit);
			bandit_param_collection.put(_bandit_str, new BanditParams(this.log,_bandit, getCodedDataRate(_bandit)));
		}
		if(!(brigands.length==bandit_param_collection.size())){
			throw new RuntimeException("size mismatch. brigands.length = "
										+brigands.length+" newer_bandit_param_list.size() = "+bandit_param_collection.size());
		}
		normalizeBanditRewards();
	}
	public void softResetBanditParams(int[][] _brigands){
		brigands=_brigands.clone();
		log.fine("soft reset of bandit_params");
		//code to soft update new version of bandits
		for(int[] _bandit:brigands){
			String _bandit_str=StrRepr.strRepr(_bandit);
			if(!bandit_param_collection.containsKey(_bandit_str)){
				bandit_param_collection.put(_bandit_str, new BanditParams(this.log, _bandit, getCodedDataRate(_bandit)));
			}
		}
		if(!(brigands.length==bandit_param_collection.size())){
			throw new RuntimeException("size mismatch. brigands.length = "
										+brigands.length+" newer_bandit_param_list.size() = "+bandit_param_collection.size());
		}
		normalizeBanditRewards();	
	}
	
	private void normalizeBanditRewards(){
		Double max_reward=0.0;
		for(BanditParams bandit_params:bandit_param_collection.values()){
			if(bandit_params.getReward()>max_reward){
				max_reward=bandit_params.getReward();
			}
		}
		for(BanditParams bandit_params:bandit_param_collection.values()){ 
			bandit_params.handleLinktunerNotifications(new BanditParamNtf(NTF_ID, NormalizeBanditReward, max_reward));
		}
	}
	
	
	//assumes optimistic code rate in absence of BER information
	private Double getCodedDataRate(int[] _bandit){
		Double _data_rate = absDataRate(_bandit);
		int _fec_index=ParamSetter.Scheme_Params_map_keylist.indexOf("FEC");
		int _bandit_fec=ParamSetter.Scheme_Params_values_list.get(_fec_index).get(_bandit[_fec_index]);
		if(_bandit_fec==0){	//if FEC is off
			new FECPenalty();
			return _data_rate*FECPenalty.getCodeRateEstimate();
		}else{
			return _data_rate;
		}
	}
	
	private Double absDataRate(int[] _bandit){
		int n=0;
		while (n<_bandit.length) {
			if (n == ParamSetter.Scheme_Params_map_keylist.indexOf("TX_ATT")) {		//TX_ATT is a modem param
			ParamSetter.setPhyParam(0, ParamSetter.Scheme_Params[n],					
						ParamSetter.Scheme_Params_values_list.get(n).get(_bandit[n]));					
			}else {
//					System.out.println("n = "+n);
//					System.out.println("_bandit[n] = "+_bandit[n]);
//					System.out.println("ParamSetter.Scheme_Params[n] = "+ParamSetter.Scheme_Params[n]);
//					System.out.println("ParamSetter.Scheme_Params_values_list.get(n) = "+ParamSetter.Scheme_Params_values_list.get(n).toString());
//					System.out.println("ParamSetter.Scheme_Params_values_list.get(n).get(_bandit[n]) = "+ParamSetter.Scheme_Params_values_list.get(n).get(_bandit[n]));
					ParamSetter.setPhyParam(ParamSetter.ACTIVE_SCHEME, ParamSetter.Scheme_Params[n],
						ParamSetter.Scheme_Params_values_list.get(n).get(_bandit[n]));					
			}
//			if (n==ParamSetter.Scheme_Params_map_keylist.indexOf("TX_ATT")) {
//				log.fine("family = 0");
//				log.fine("n = "+n+" param_setter.Scheme_Params_map_keylist.indexOf(TX_ATT) = "+ParamSetter.Scheme_Params_map_keylist.indexOf("TX_ATT"));
//			}
			n++;
		}
		Double _pkt_duration = (double) ParamSetter.getPhyParam(ParamSetter.ACTIVE_SCHEME, ParamSetter.PKT_DURATION);								
		//add penalty for using high power. If TX-ATT = 30, penalty = (36-30)*10
		//Double _pkt_duration_penalized = _pkt_duration + (36 - param_setter.getPhyParam(0, param_setter.Scheme_Params_map.get("TX_ATT")))*10;
		Double _pkt_duration_penalized = _pkt_duration;
//		log.fine("_pkt_duration_penalized  = "+_pkt_duration_penalized );
		//log.fine("param_setter.getPhyParam(0, param_setter.Scheme_Params_map.get(TX_ATT) = "+param_setter.getPhyParam(0, param_setter.Scheme_Params_map.get("TX_ATT")));
		//log.fine("param_setter.Scheme_Params_map.get(TX_ATT) = "+param_setter.Scheme_Params_map.get("TX_ATT"));
//		log.fine(" _pkt_duration = "+ _pkt_duration+" _pkt_duration_penalized = "+ _pkt_duration_penalized);
		int _pkt_len_index=ParamSetter.Scheme_Params_map_keylist.indexOf("PKTLEN");
		Integer _pkt_len= ParamSetter.Scheme_Params_values_list.get(_pkt_len_index).get(_bandit[_pkt_len_index]);
		Double _data_rate=(_pkt_len*8)/_pkt_duration;
		return _data_rate;
	}
	private double[] findMaxValueIndex(Map<String, BanditParams> _bandit_params){
		BanditParams[] bandit_params=(BanditParams[]) _bandit_params.values().toArray(new BanditParams[_bandit_params.size()]);
		double[] maxValueIndex={0,0};
		for (int i = 0; i < bandit_params.length; i++) {
			if(bandit_params[i].getGittinsIndex()>maxValueIndex[0]){
				maxValueIndex[0]=bandit_params[i].getGittinsIndex();
				maxValueIndex[1]=i;
			}
		}
		return maxValueIndex;
	}
	public int getBestBanditID(){
		return (int)findMaxValueIndex(bandit_param_collection)[1];
	}
	public double getBestBanditNormalizedIndex(){
		return findMaxValueIndex(bandit_param_collection)[0];
	}
	public void printBanditParamStatus(){
		bandit_param_status_printer.printBanditParamsStatus(bandit_param_collection);
	}
	public BanditParams getBanditParams(String _bandit_str){
		return bandit_param_collection.get(_bandit_str);
	}
	
}
