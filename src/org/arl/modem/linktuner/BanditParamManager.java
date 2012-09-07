package org.arl.modem.linktuner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.*;


public class BanditParamManager {
	private Logger log;
	protected Exploit exploit;
	protected Map<String, BanditParams> bandit_params;
	private BanditIndices bandit_indices;
	private int[][] brigands;
	private List<int[]> prev_brigands_list;
	private Map<String, DataRateInfo> rel_data_rates;
	private BanditParamStatusPrinter bandit_param_status_printer;
	public BanditParamManager(Exploit _exploit, Logger _log, int[][] _brigands){
		this.exploit=_exploit; 
		this.log = _log;
		this.brigands=_brigands;
		this.prev_brigands_list=Arrays.asList(brigands);
		this.bandit_params= new LinkedHashMap<String, BanditParams>(brigands.length);
		this.bandit_indices=new BanditIndices();
		this.rel_data_rates=new LinkedHashMap<String, DataRateInfo>(brigands.length);
		hardResetBanditParams(brigands);
//		printRelDataRates();
		this.bandit_param_status_printer = new BanditParamStatusPrinter(bandit_params, log);			
	}
	
	public void updateBanditParams(int[][] _brigands,int banditCurrent){
		brigands=_brigands;
		int[] bandit_updated=brigands[banditCurrent];
		String bandit_updated_as_str=StrRepr.strRepr(bandit_updated);
		updateBanditParamsAlphaBeta(bandit_updated);
		updateRelDataRates(bandit_updated, exploit.getStats(Exploit.GET_RECENT_BER));
		if(!bandit_params.containsKey(bandit_updated_as_str)){
			throw new RuntimeException("bandit_params does not contain params for bandit "+bandit_updated_as_str);
		}
		bandit_params.get(bandit_updated_as_str).gittinsIndex=bandit_indices.gittinsIndex(bandit_params.get(bandit_updated_as_str).alpha,
																						bandit_params.get(bandit_updated_as_str).beta, 0.75);
//		bandit_params.get(bandit_updated_as_str).normalizedIndex=bandit_params.get(bandit_updated_as_str).gittinsIndex*rel_data_rates.get(bandit_updated_as_str).rel_data_rate;
//experimental change:
		for(int[] _bandit : brigands){
			bandit_updated_as_str=StrRepr.strRepr(_bandit);
			bandit_params.get(bandit_updated_as_str).normalizedIndex= (1.0*rel_data_rates.get(bandit_updated_as_str).rel_data_rate)
																		*(1.0*bandit_params.get(bandit_updated_as_str).gittinsIndex);
		}
		log.fine("bandit_params["+bandit_updated_as_str+"].alpha = " + bandit_params.get(bandit_updated_as_str).alpha +
				" bandit_params["+bandit_updated_as_str+"].beta = " + bandit_params.get(bandit_updated_as_str).beta);
		log.fine("bandit_params["+bandit_updated_as_str+"].gittinsIndex = " + bandit_params.get(bandit_updated_as_str).gittinsIndex +
				" bandit_params["+bandit_updated_as_str+"].normalizedIndex = " + bandit_params.get(bandit_updated_as_str).normalizedIndex);
//		printRelDataRates();
	}
	
	protected void updateBanditParamsAlphaBeta(int[] _bandit_updated){
		String bandit_updated_as_str=StrRepr.strRepr(_bandit_updated);
		if (exploit.getStats(Exploit.GET_RECENT_ERROR_CNT)>0.0 || exploit.getStats(Exploit.GET_RECENT_RX_TESTPKT_CNT)==0.0) {
			bandit_params.get(bandit_updated_as_str).beta++;
		}else if(exploit.getStats(Exploit.GET_RECENT_RX_TESTPKT_CNT)>0.0){ //need to check this condition 'coz sometimes u may get RxTestPktCnt=0
			bandit_params.get(bandit_updated_as_str).alpha++;
		}
	}
	
	//will update brigand list only if its different
	private void updateBrigandList(int[][] _brigands){
		ArrayList<int[]> _brigands_as_list= new ArrayList<int[]>(Arrays.asList(_brigands));
//		log.fine("size of brigands_as_list : "+_brigands_as_list.size()+"_brigands_as_list : "+StrRepr.strRepr(_brigands_as_list.toArray(new int[0][0])));
//		log.fine("prev_brigands_list: (size = "+prev_brigands_list.size()+")");
//		log.fine(""+ StrRepr.strRepr(prev_brigands_list.toArray(new int[0][0])) );
		Boolean _test=true;
		int i=0;
		while(_test && i < _brigands_as_list.size() && i < prev_brigands_list.size()){
			_test=(prev_brigands_list.get(i)==_brigands_as_list.get(i));
			i++;
		}
		_test = (_test && (_brigands_as_list.size()==prev_brigands_list.size()));
		_test=_brigands_as_list.equals(prev_brigands_list);
		if(!_test){
			prev_brigands_list=new ArrayList<int[]>(Arrays.asList(brigands));
			brigands=_brigands;				
		}
//		log.fine("_brigands_as_list.size() : "+_brigands_as_list.size());
//		log.fine("prev_brigands_list.size() : "+prev_brigands_list.size());
//		log.fine("_test = "+_test);
//		log.fine("prev_brigands_list : "+StrRepr.strRepr(prev_brigands_list));
//		log.fine("brigands : "+StrRepr.strRepr(brigands));
	}
	public void hardResetBanditParams(int[][] _brigands){
		ArrayList<int[]> _brigands_as_list;
		updateBrigandList(_brigands);		
		bandit_indices=new BanditIndices();		
		log.fine("hard-reset of bandit_params");
		bandit_params=new LinkedHashMap<String, BanditParams>(brigands.length);
		resetRelDataRates(brigands);
		log.fine("after resetRelDataRates");
		for (int i = 0; i < brigands.length; i++) {
			int[] _bandit=brigands[i].clone();
			String _bandit_str=StrRepr.strRepr(_bandit);
			bandit_params.put(_bandit_str, new BanditParams());
			bandit_params.get(_bandit_str).normalizedIndex=bandit_params.get(_bandit_str).gittinsIndex*rel_data_rates.get(_bandit_str).rel_data_rate;
		}
		if(!(brigands.length==bandit_params.size())){
			throw new RuntimeException("size mismatch: brigands.length = "+brigands.length+", bandit_params.size() = "+bandit_params.size());
		}
	}
	public void softResetBanditParams(int[][] _brigands){
		updateBrigandList(_brigands);
		ArrayList<int[]> _brigands_as_list=new ArrayList<int[]>(Arrays.asList(brigands));
		log.fine("soft reset of bandit_params");
		updateRelDataRates(brigands);
		for(int[] _bandit: _brigands_as_list){
			String _bandit_str=StrRepr.strRepr(_bandit);
			if(!bandit_params.containsKey(_bandit_str)){
				bandit_params.put(_bandit_str, new BanditParams());
				bandit_params.get(_bandit_str).normalizedIndex=bandit_params.get(_bandit_str).gittinsIndex*rel_data_rates.get(_bandit_str).rel_data_rate;
			}
		}
//	log.fine("brigands.length = "+brigands.length+", bandit_params.size() = "+bandit_params.size());
	if(!(brigands.length==bandit_params.size())){
		throw new RuntimeException("size mismatch: brigands.length = "+brigands.length+", bandit_params.size() = "+bandit_params.size());
	}
	log.fine("completed softResetBanditParams");	
	}
	
	private void resetRelDataRates(int[][] _brigands){
		rel_data_rates = new LinkedHashMap<String, BanditParamManager.DataRateInfo>(_brigands.length);
		for(int[] _bandit : _brigands){
			String _bandit_str = StrRepr.strRepr(_bandit);
			rel_data_rates.put(_bandit_str, new DataRateInfo());
			rel_data_rates.get(_bandit_str).abs_data_rate = getCodedDataRate(_bandit);
		}
		normalizeRelDataRates();
	}
	
	private void updateRelDataRates(int[][] _brigands){
		for(int[] _bandit : _brigands){
			String _bandit_str = StrRepr.strRepr(_bandit);
			if(!rel_data_rates.containsKey(_bandit_str)){
				rel_data_rates.put(_bandit_str, new DataRateInfo());
				rel_data_rates.get(_bandit_str).abs_data_rate=getCodedDataRate(_bandit);
			}
		}
		normalizeRelDataRates();
	}
	
	private void updateRelDataRates(int[] _bandit, Double BER){
		String _bandit_str=StrRepr.strRepr(_bandit);
		new FECPenalty();
		rel_data_rates.get(_bandit_str).abs_data_rate=getCodedDataRate(_bandit, BER);
		normalizeRelDataRates();
	}
	
	private void normalizeRelDataRates(){
		Double _max_data_rate=0.0;
		for(Map.Entry<String, DataRateInfo> entry : rel_data_rates.entrySet()){
			if(entry.getValue().abs_data_rate > _max_data_rate){
				_max_data_rate = entry.getValue().abs_data_rate;
			}
		}
		for(Map.Entry<String, DataRateInfo> entry : rel_data_rates.entrySet()){
			entry.getValue().rel_data_rate = entry.getValue().abs_data_rate / _max_data_rate;
		}		
	}
	
	//assumes optimistic code rate in absence of BER information
	private Double getCodedDataRate(int[] _bandit){
		Double _data_rate = absDataRate(_bandit);
		int _fec_index=ParamSetter.Scheme_Params_map_keylist.indexOf("FEC");
		int _bandit_fec=_bandit[_fec_index];
		if(_bandit_fec==0){	//if FEC is off
			new FECPenalty();
			return _data_rate*FECPenalty.getCodeRateEstimate();
		}else{
			return _data_rate;
		}
	}

	private Double getCodedDataRate(int[] _bandit, Double _BER) {
		int _fec_index=ParamSetter.Scheme_Params_map_keylist.indexOf("FEC");
		int _bandit_fec=_bandit[_fec_index];
		//fec=0 indicates test packet, non-zero fec indicates a data packet
		if(_bandit_fec==0){
			Double _data_rate=absDataRate(_bandit);
			return _data_rate*FECPenalty.getCodeRate(_BER);
		}else{
			if(_BER==0.0){
				return absDataRate(_bandit);
			}else{
				return 0.0;
			}
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
			if(bandit_params[i].normalizedIndex>maxValueIndex[0]){
				maxValueIndex[0]=bandit_params[i].normalizedIndex;
				maxValueIndex[1]=i;
			}
		}
		return maxValueIndex;
	}
	public int getBestBanditID(){
		return (int)findMaxValueIndex(bandit_params)[1];
	}
	public double getBestBanditNormalizedIndex(){
		return findMaxValueIndex(bandit_params)[0];
	}
	public void printBanditParamStatus(){
		bandit_param_status_printer.printBanditParamsStatus(bandit_params);
	}
	public BanditParams getBanditParams(String _bandit_str){
		return bandit_params.get(_bandit_str);
	}
	class DataRateInfo{
		Double abs_data_rate=-1.0;
		Double rel_data_rate=-1.0;
		public String toString(){
			return new String(abs_data_rate.toString()+" "+rel_data_rate.toString());
		}
	}
	
}
