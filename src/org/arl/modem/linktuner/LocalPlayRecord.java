package org.arl.modem.linktuner;

public class LocalPlayRecord {
	int[] bandit;
	Integer banditPlayed=-1;
	Boolean success=null;
	Double BER = -1.0;
	Double absoluteInstDataRate=-1.0;
	int[] scheme_values;
	BanditParams bandit_params=null;
	
	public String toString(){
		String bandit2str = intArray2str(bandit,"");
		String scheme_values2str=intArray2str(scheme_values," ");
		String bandit_params2str= (bandit_params==null ? "" : bandit_params.toString());
		//format: bandit	banditPlayed	success		BER		data_rate		bandit_params		scheme_values
		String master_string = String.format("%s %.3f %.2f %s %s", bandit2str+" "+banditPlayed.toString() +" "+success.toString()+" ", 
											BER, 
											absoluteInstDataRate,
											bandit_params2str,
											scheme_values2str);
		return master_string;
	}
	private String intArray2str(int[] arr, String delimit_character){
		String _t="";
		for (int i = 0; i < arr.length; i++) {
			_t=_t.concat((Integer.toString(arr[i]))+delimit_character);
		}
		return new String(_t);
	}
}
