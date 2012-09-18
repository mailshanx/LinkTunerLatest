package org.arl.modem.linktuner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.*;

public class BanditHistoryTracker{
	protected Exploit exploit;
	private Logger log;
	protected List<List<LocalPlayRecord>> grand_play_history;
	protected List<LocalPlayRecord> local_play_history;
	private LocalPlayRecord _local_play_record;
	Integer grand_play_index;
	//private List<LocalPlayRecord> local_play_history;
	public BanditHistoryTracker(Exploit _exploit, Logger _log){
		grand_play_history=new ArrayList<List<LocalPlayRecord>>();
		local_play_history = new ArrayList<LocalPlayRecord>();
		grand_play_index=0;
		this.exploit=_exploit;
		this.log=_log;
	}
	public void updateHistory(int[][] _brigands, int _banditCurrent){
		int[][] brigands=_brigands;
		int banditCurrent = _banditCurrent;
		_local_play_record=new LocalPlayRecord();
		updateSomeLocalPlayRecordParams(_local_play_record, brigands, banditCurrent);
		
		local_play_history.add(_local_play_record);
		updateGrandPlayHistory();
	}
	public void updateHistory(int[][] _brigands, int _banditCurrent, BanditParams bandit_params){
		int[][] brigands=_brigands;
		int banditCurrent = _banditCurrent;
		_local_play_record=new LocalPlayRecord();
		updateSomeLocalPlayRecordParams(_local_play_record, brigands, banditCurrent);
		_local_play_record.bandit_params = bandit_params;
		
		local_play_history.add(_local_play_record);
		updateGrandPlayHistory();
	}
	private void updateSomeLocalPlayRecordParams(LocalPlayRecord _local_play_record, int[][] _brigands, int _banditCurrent){
		_local_play_record.bandit=_brigands[_banditCurrent].clone();
		_local_play_record.scheme_values=retrieveSchemeValues(_brigands[_banditCurrent].clone());
		_local_play_record.banditPlayed=(Integer)_banditCurrent;
		updateLocalPlayRecordSuccess(_local_play_record);
		_local_play_record.BER=exploit.getStats(Exploit.GET_RECENT_BER);
		_local_play_record.absoluteInstDataRate=getDataRate(_local_play_record.bandit, exploit.getStats(Exploit.GET_RECENT_BER));
	}
	
	protected void updateLocalPlayRecordSuccess(LocalPlayRecord _local_play_record) {
		//implicit assumption that each play consists of sending a single testpacket 
		if (exploit.getStats(Exploit.GET_RECENT_ERROR_CNT)>0.0 || exploit.getStats(Exploit.GET_RECENT_RX_TESTPKT_CNT)==0.0) {
			_local_play_record.success=false;
		}else{ 
			_local_play_record.success=true;
		}
	}
	
	public Double getDataRate(int[] bandit, Double _BER){
		Double _pkt_duration=(double) ParamSetter.getPhyParam(ParamSetter.ACTIVE_SCHEME, ParamSetter.PKT_DURATION);
		Double _pkt_len=(double) ParamSetter.getPhyParam(ParamSetter.ACTIVE_SCHEME, ParamSetter.PKTLEN);
		Double _data_rate=(_pkt_len*8) / _pkt_duration;
		int fec_index=ParamSetter.Scheme_Params_map_keylist.indexOf("FEC");
		int fec_mask=ParamSetter.Scheme_Params_values_list.get(fec_index).get(bandit[fec_index]);
		if(fec_mask==0){
			new FECPenalty();
			return _data_rate*FECPenalty.getCodeRate(_BER);
		}else{
			if(_BER==0.0){
				return _data_rate;
			}else{
				return 0.0;
			}
		}
	}
	public void updateGrandPlayHistory(){
		if(ExperimentalBandits.EXPERIMENT_COMPLETED || ExperimentalBandits.LOCAL_EXPERIMENT_COMPLETED){
			grand_play_history.add(local_play_history);
			grand_play_index++;
			local_play_history = new ArrayList<LocalPlayRecord>();
		}		
	}
	private int[] retrieveSchemeValues(int[] bandit){
		int[] scheme_values = new int[bandit.length];
		for (int i = 0; i < scheme_values.length; i++) {
			scheme_values[i]=ParamSetter.Scheme_Params_values_list.get(i).get(bandit[i]);
		}
		return scheme_values;
	}
	public void printLatestLocalPlayHistory(){
		if(!local_play_history.isEmpty()){
			printLocalPlayHistory(local_play_history);
		}else{
			printLocalPlayHistory(grand_play_history.get(grand_play_history.size()-1));
		}
	}
	private void printLocalPlayHistory(List<LocalPlayRecord> _local_play_history){
		for (Iterator<LocalPlayRecord> iterator = local_play_history.iterator(); iterator
				.hasNext();) {
			log.fine(""+iterator.next());
		}
	}
	public void printLatestHistory(){
		if (_local_play_record != null) {
			log.fine(""+_local_play_record);
		}
	}
	public void printGrandPlayHistory(){
		for (List<LocalPlayRecord> play_record_list : grand_play_history) {
			for(LocalPlayRecord _play_record: play_record_list){
				log.fine(""+_play_record);
			}
			log.fine("end of local experiment");
		}
	}
	public List<LocalPlayRecord> getLatestNonEmptyLocalPlayHistory(){
		if(!local_play_history.isEmpty()){
			return local_play_history;
		}else if(!grand_play_history.isEmpty()){
			return grand_play_history.get(grand_play_history.size()-1);
		}else{
			log.fine("returned an empty local_play_history, likely 'coz no records have been updated yet");
			return new ArrayList<LocalPlayRecord>();
		}
	}
	public List<LocalPlayRecord> getLocalPlayHistory(){
		return local_play_history;
	}
	public List<List<LocalPlayRecord>> getGrandPlayHistory(){
		return grand_play_history;
	}
}
