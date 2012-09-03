package org.arl.modem.linktuner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

public class HistoryAnalyzer {
	private ParamSetter param_setter;
	private Logger log;
	private int[][] brigands;
	public HistoryAnalyzer(ParamSetter _param_setter, Logger _log){
		this.param_setter = _param_setter;
		this.log = _log;
	}
	public List<List<LocalParamResults>> analyzeLocalHistory(List<LocalPlayRecord> _local_play_history, int[][] _brigands){
		//another version 
		//assumes that the parameter that has varied in this local history is Np.
		String paramUnderAnalysis="Np";
		List<LocalParamResults> local_param_result_list = new ArrayList<LocalParamResults>();
		List<LocalParamResults> local_param_success_list=new ArrayList<LocalParamResults>();
		List<LocalParamResults> local_param_failiure_list=new ArrayList<LocalParamResults>();
		List<List<LocalParamResults>> local_param_filtered_lists=new ArrayList<List<LocalParamResults>>();
		List<Integer> local_param_list=new ArrayList<Integer>();
		List<LocalPlayRecord> local_play_history = _local_play_history;
		LocalParamResults _local_param_results;
		
		brigands = _brigands;
		
		int _ParamIndexPosition=ParamSetter.Scheme_Params_map_keylist.indexOf(paramUnderAnalysis);
		for (int i = 0; i < brigands.length; i++) {
			_local_param_results=new LocalParamResults();
			_local_param_results.paramValue=ParamSetter.Scheme_Params_values_list.get(_ParamIndexPosition).get(brigands[i][_ParamIndexPosition]);
			local_param_list.add(_local_param_results.paramValue);
			local_param_result_list.add(_local_param_results);
		}
		//check for duplicates
		java.util.Set<Integer> dup_check_set=new HashSet<Integer>(local_param_list);
		if(dup_check_set.size()<local_param_list.size()){
			log.warning("found duplicates! looks like you are analyzing the wrong param. local_param_list = "+local_param_list);
		}
		for (int i = 0; i < local_play_history.size(); i++) {
			int _param_value=ParamSetter.Scheme_Params_values_list.get(_ParamIndexPosition).get(local_play_history.get(i).bandit[_ParamIndexPosition]);
			int _index=local_param_list.indexOf((Integer)_param_value);
			local_param_result_list.get(_index).paramCount++;
			if (local_play_history.get(i).success) {
				local_param_result_list.get(_index).paramSuccessCount++;
			}
			local_param_result_list.get(_index).successRatio = local_param_result_list.get(_index).paramSuccessCount / local_param_result_list.get(_index).paramCount;
		}
		for (ListIterator<LocalParamResults> iterator = local_param_result_list.listIterator(); iterator.hasNext();) {
			LocalParamResults __local_param_results= iterator.next();
			if((__local_param_results.paramCount>1 && __local_param_results.successRatio>0.5)){
				__local_param_results.Success=true;
				local_param_success_list.add(__local_param_results);
			}else if(__local_param_results.paramCount>1 && __local_param_results.successRatio<=0.5){
				__local_param_results.Success=false;
				local_param_failiure_list.add(__local_param_results);
			}
			SortByParamValue sort_by_param_value = new SortByParamValue();
			Collections.sort(local_param_success_list, sort_by_param_value);
			Collections.sort(local_param_failiure_list, sort_by_param_value);			
		}
		local_param_filtered_lists.add(local_param_success_list);
		local_param_filtered_lists.add(local_param_failiure_list);
		return local_param_filtered_lists;
	}

	private static class SortByParamValue implements Comparator<LocalParamResults>{
		public int compare(LocalParamResults o1, LocalParamResults o2) {
			return o1.paramValue.compareTo(o2.paramValue);
		}
	}
	private static class SortBySuccessValue implements Comparator<LocalParamResults>{
		public int compare(LocalParamResults o1, LocalParamResults o2) {
			return o1.Success.compareTo(o2.Success);
		}
	}

}
