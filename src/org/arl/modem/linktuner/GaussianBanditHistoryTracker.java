package org.arl.modem.linktuner;

import java.util.logging.Logger;

public class GaussianBanditHistoryTracker extends BanditHistoryTracker {
	
	private final Double BER_SUCCESS_THRESHOLD;
	public GaussianBanditHistoryTracker(Exploit _exploit, Logger _log) {
		super(_exploit, _log);
		new FECPenalty();
		BER_SUCCESS_THRESHOLD=FECPenalty.getMaxTolerableBER();
	}
	
	@Override
	protected void updateLocalPlayRecordSuccess(LocalPlayRecord _local_play_record) {
		//implicit assumption that each play consists of sending a single testpacket, and FEC=0. hence successful if BER<threshold%
		if(exploit.getStats(Exploit.GET_RECENT_BER) < BER_SUCCESS_THRESHOLD && exploit.getStats(Exploit.GET_RECENT_BER)!=-1.0){
			_local_play_record.success=true;
		}else{
			_local_play_record.success=false;
		}
	}

	
}
