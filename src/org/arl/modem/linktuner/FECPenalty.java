package org.arl.modem.linktuner;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class FECPenalty {
	private static Map<Double, Double> code_penalty_map;
	private static Double codeRateEstimate;
	
	public FECPenalty(){
		code_penalty_map=new LinkedHashMap<Double, Double>();
		code_penalty_map.put(0.15, 0.00);
		code_penalty_map.put(0.09, 0.15);
		code_penalty_map.put(0.04, 0.33);
		code_penalty_map.put(0.00, 0.50);
		
		codeRateEstimate=0.50;
	}
	
	public static Double getCodeRate(Double BER){
		for(Map.Entry<Double, Double> entry : code_penalty_map.entrySet()){
			if(BER>entry.getKey()){
				return entry.getValue();
			}
		}
		//if ur here, it means BER = -1, which implies the test packet din't get thru
		return 0.0;
	}
	public static Double getMaxTolerableBER(){
		Iterator<Double> iter = code_penalty_map.keySet().iterator();
		return (Double) iter.next();
	}
	public static Double getCodeRateEstimate(){
		return codeRateEstimate;
	}
	
}
