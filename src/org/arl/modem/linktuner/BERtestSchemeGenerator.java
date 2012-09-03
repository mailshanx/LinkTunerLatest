package org.arl.modem.linktuner;

import java.util.*;
import java.util.logging.Logger;

//class to comprehensively generate schemes with all combinations of MTYPE,DMODE,MPSK,Nc,Np
public class BERtestSchemeGenerator {
	private Logger log;
	private int brigands[][];
	private LinkedHashMap<String, Integer>cardinality=new LinkedHashMap<String, Integer>();
 
	public BERtestSchemeGenerator(Logger _log){
		this.log=_log;
	}

	private void generateSchemes(){
//		--------------------------------
//		order in which you populate the map is important: go from right-to-left always
//		---------------------------------
		cardinality.put("TX_ATT", ParamSetter.Scheme_Params_values_list.get(ParamSetter.getIndexOfParam("TX_ATT")).size());
		cardinality.put("Np", ParamSetter.Scheme_Params_values_list.get(ParamSetter.getIndexOfParam("Np")).size());
		cardinality.put("Nc", ParamSetter.Scheme_Params_values_list.get(ParamSetter.getIndexOfParam("Nc")).size());
		cardinality.put("MPSK", ParamSetter.Scheme_Params_values_list.get(ParamSetter.getIndexOfParam("MPSK")).size());
		cardinality.put("DMODE", ParamSetter.Scheme_Params_values_list.get(ParamSetter.getIndexOfParam("DMODE")).size());
		cardinality.put("MTYPE", ParamSetter.Scheme_Params_values_list.get(ParamSetter.getIndexOfParam("MTYPE")).size());
		int _total_combinations=1;
		for(Map.Entry<String, Integer> entries : cardinality.entrySet()){
			_total_combinations*=entries.getValue();
		}
		brigands = new int[_total_combinations][ParamSetter.Scheme_Params_values_list.size()];
		System.out.println("_total_combinations = "+_total_combinations);
		System.out.println("ParamSetter.Scheme_Params_values_list.size() = "+ParamSetter.Scheme_Params_values_list.size());
		for (int i = 0; i < brigands.length; i++) {
			for (int j = 0; j < brigands[i].length; j++) {
				brigands[i][j]=0;
			}
			int _div_factor=1;
			int _mod_factor=1;
			for(Map.Entry<String, Integer> entry : cardinality.entrySet()){
				_mod_factor=entry.getValue();
				brigands[i][ParamSetter.getIndexOfParam(entry.getKey())]=(i / _div_factor) % _mod_factor;
				_div_factor*=_mod_factor;
			}
		}
		//System.out.println("brigands = "+ExperimentalBandits.brigands2str(brigands));
		log.fine("printing generated brigands for BER test. no of brigands = "+brigands.length);
		for(int[] _bandit : brigands){
			log.fine(StrRepr.strRepr(_bandit));
		}
	}
 	public int[][] getSchemes(){
 		generateSchemes();
 		return brigands.clone();
	}
}
