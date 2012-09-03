package org.arl.modem.linktuner;

import java.util.List;

public interface BrigandGenerable {
	public int[][] genBrigandSetAlternate(int[] winningBandit, int[][] _brigands, List<LocalPlayRecord> _local_play_history);
	public Integer getTunerState();
}
