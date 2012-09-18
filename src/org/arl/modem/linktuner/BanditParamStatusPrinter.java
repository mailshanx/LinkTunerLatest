package org.arl.modem.linktuner;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BanditParamStatusPrinter {
	private Logger log;
	private Map<String, BanditParams> bandit_params;
	public BanditParamStatusPrinter(Map<String, BanditParams> _bandit_params, Logger _log){
		this.bandit_params=_bandit_params;
		this.log=_log;
	}
	
	public void printBanditParamsStatus(Map<String, BanditParams> _bandit_params){
		int i=0;
		for(Map.Entry<String, BanditParams> entry : _bandit_params.entrySet()){
			String _to_str=entry.getKey()+" "+entry.getValue().getGittinsIndex()+" "+entry.getValue().getReward();
			log.fine(_to_str);
		}
	}
}
