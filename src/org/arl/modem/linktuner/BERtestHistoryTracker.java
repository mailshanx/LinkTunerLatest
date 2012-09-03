package org.arl.modem.linktuner;

import java.util.ArrayList;
import java.util.logging.Logger;

public class BERtestHistoryTracker extends BanditHistoryTracker {

	public BERtestHistoryTracker(Exploit _exploit, Logger _log) {
		super(_exploit, _log);
	}
	
	@Override
	public void updateGrandPlayHistory(){
		if(BERtester.EXPERIMENT_COMPLETED){
			grand_play_history.add(local_play_history);
			grand_play_index++;
			local_play_history = new ArrayList<LocalPlayRecord>();
		}		
	}
	
	@Override
	public Double updateDataRate(Double _BER){
		Double _pkt_duration=(double) ParamSetter.getPhyParam(ParamSetter.ACTIVE_SCHEME, ParamSetter.PKT_DURATION);
		Double _pkt_len=(double) ParamSetter.getPhyParam(ParamSetter.ACTIVE_SCHEME, ParamSetter.PKTLEN);
		Double _data_rate=(_pkt_len*8) / _pkt_duration;
		
		return _data_rate;
	}
}
