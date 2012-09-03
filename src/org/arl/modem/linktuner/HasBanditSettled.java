package org.arl.modem.linktuner;

import java.util.List;

public class HasBanditSettled {
	public static boolean check(List<LocalPlayRecord> local_play_history, int bandit_settle_factor, int banditCurrent){
		if (local_play_history.size()>=bandit_settle_factor) {
			int index=local_play_history.size()-1;
			Boolean settle=true;
			while (settle && index>=local_play_history.size()-bandit_settle_factor) {
				if (local_play_history.get(index).banditPlayed!=banditCurrent || !local_play_history.get(index).success) {
					settle=false;
				}
				index--;
			}
			return settle.booleanValue();
		}
		else{
			return false;
		}
	}
}
