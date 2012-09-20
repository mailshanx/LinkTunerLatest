package org.arl.modem.linktuner;


import java.util.*;
import java.util.logging.*;

public class GaussianBrigandGenerator implements BrigandGenerable{
	
	private Logger log;
	private int[][] brigands;
	private List<LocalPlayRecord> local_play_history;
	private Double min_success_percent=0.4;
	private Double min_play_cnt=0.0;
	private List<String> params_to_be_generated;
	private Map<String, GaussianParams> gauss_params_map;
	private sortByNormIndex sort_by_norm_index;
	private Double max_no_of_new_bandits=40.0;
	private Double max_no_of_new_bandits_shrink_factor=1.00;
	private Double std_dev_shrink_factor=0.80;
	
	public GaussianBrigandGenerator(Logger _log){
		this.log=_log;
		min_success_percent=0.4;
		min_play_cnt=0.0;
		String pwd = System.getProperty("user.dir");
		In in = new In(pwd+"/linktuner.config");
		while(!in.isEmpty()){
			String s = in.readLine();
			String _param=s.split("\\s+")[0];
			if(_param.equals("max_no_of_new_bandits")){
				max_no_of_new_bandits=Double.parseDouble(s.split("\\s+")[1]);
				log.fine("set max_no_of_new_bandits = "+max_no_of_new_bandits);
			}else if (_param.equals("max_no_of_new_bandits_shrink_factor")) {
				max_no_of_new_bandits_shrink_factor=Double.parseDouble(s.split("\\s+")[1]);
				log.fine("set max_no_of_new_bandits_shrink_factor = "+max_no_of_new_bandits_shrink_factor);
			}else if(_param.equals("std_dev_shrink_factor")){
				std_dev_shrink_factor=Double.parseDouble(s.split("\\s+")[1]);
				log.fine("set std_dev_shrink_factor = "+std_dev_shrink_factor);
			}else if(_param.equals("min_success_percent")){
				min_success_percent=Double.parseDouble(s.split("\\s+")[1]);
				log.fine("set min_success_percent = "+min_success_percent);
			}else if(_param.equals("min_play_cnt")){
				min_play_cnt=Double.parseDouble(s.split("\\s+")[1]);
				log.fine("set min_play_cnt = "+min_play_cnt);
			}
		}
		sort_by_norm_index=new sortByNormIndex();
		params_to_be_generated=new LinkedList<String>();
		Collections.addAll(params_to_be_generated, "DMODE","MPSK","Nc","Np","TX_ATT");		//add params here for future extension
		gauss_params_map=new LinkedHashMap<String, GaussianParams>();
		for(String params : params_to_be_generated){
			gauss_params_map.put(params, new GaussianParams(params, std_dev_shrink_factor));
		}
	}
	
	@Override
	public int[][] genBrigandSetAlternate(int[] winningBandit, int[][] _brigands, List<LocalPlayRecord> _local_play_history) {
		log.fine("inside genBrigandSetAlternate---------------");
		brigands=_brigands.clone();
		local_play_history=_local_play_history;
		ArrayList<int[]> brigands_as_list= new ArrayList<int[]>(Arrays.asList(brigands));
		ArrayList<int[]> filtered_brigand_list = new ArrayList<int[]>();
		Set<String> brigands_as_set;
		List<SuccessfulBandits> list_of_successful_bandits=analyzeLocalHistory(local_play_history, min_success_percent, min_play_cnt);
		max_no_of_new_bandits=(double) Math.round(max_no_of_new_bandits*max_no_of_new_bandits_shrink_factor);
//		if(max_no_of_new_bandits<2) max_no_of_new_bandits=2.0;
		log.fine("list_of_successful_bandits : ");
		for(SuccessfulBandits _success_bandits : list_of_successful_bandits){
			log.fine(""+_success_bandits);
		}
		if(!list_of_successful_bandits.isEmpty()){
			Collections.sort(list_of_successful_bandits, sort_by_norm_index);
			int[] best_performing_bandit=list_of_successful_bandits.get(list_of_successful_bandits.size()-1).bandit.clone();
			log.fine("best_performing_bandit : "+StrRepr.strRepr(best_performing_bandit));
			for(String params : params_to_be_generated){
				int _param_index=ParamSetter.Scheme_Params_map_keylist.indexOf(params);
				gauss_params_map.get(params).updateParams(best_performing_bandit[_param_index]);
				log.fine("params : "+params+" mean : "+gauss_params_map.get(params).getMean()+" std_dev : "+gauss_params_map.get(params).getStdDev());
			}
			for (int i = 0; i < max_no_of_new_bandits; i++) {
				brigands_as_list.add(gaussianMutation(best_performing_bandit.clone()));
			}
		}else{
			for(String params : params_to_be_generated){
				gauss_params_map.get(params).resetParams();
			}
			for(int i=0; i<max_no_of_new_bandits; i++){
				brigands_as_list.add(gaussianMutation(brigands_as_list.get(0).clone()));
			}
		}
		brigands_as_set=new LinkedHashSet<String>(brigands_as_list.size());
		for(int[] _bandit : brigands_as_list){
			if(!brigands_as_set.contains(StrRepr.strRepr(_bandit))){
				filtered_brigand_list.add(_bandit.clone());
			}
			brigands_as_set.add(StrRepr.strRepr(_bandit));
		}
		brigands=(int[][]) filtered_brigand_list.toArray(new int[filtered_brigand_list.size()][filtered_brigand_list.get(0).length]);
		return brigands.clone();
	}
	
	@Override
	public Integer getTunerState() {
		if(max_no_of_new_bandits>=2){
			return ExperimentalBandits.SPARSE_SAMPLING_IN_PROGRESS;
		}else{
			return ExperimentalBandits.STEADY_STATE;
		}
		
	}
	
	private int[] gaussianMutation(int[] _temp_bandit){
		for(String params : params_to_be_generated){
			int _param_index=ParamSetter.Scheme_Params_map_keylist.indexOf(params);
			GaussianParams _gaussian_params = gauss_params_map.get(params);
			int _param_index_value=gaussianIndex(_gaussian_params.getSpan(), _gaussian_params.getMean(), _gaussian_params.getStdDev());
			_temp_bandit[_param_index]=_param_index_value;
		}
		return _temp_bandit.clone();
	}
	
	private Integer gaussianIndex(Integer arrayLength, Integer meanIndex, Double stdDev){
		assert(meanIndex>=0 & meanIndex<arrayLength);
		Random random=new Random();
		Integer _index=(int) Math.round(stdDev*random.nextGaussian() + meanIndex);
		while(_index<0 || _index>=arrayLength){
			_index=(int) Math.round(stdDev*random.nextGaussian() + meanIndex);
		}
		return new Integer(_index);
	}
	
	private List<SuccessfulBandits> analyzeLocalHistory(List<LocalPlayRecord> _local_play_history, Double _min_success_percent, Double _min_play_count){
		HashMap<String, LocalPlayHistStats> play_hist_map = new HashMap<String, LocalPlayHistStats>();
		List<SuccessfulBandits> list_of_successful_bandits=new ArrayList<SuccessfulBandits>();
		int[] _bandit;
		LocalPlayHistStats _local_play_hist_stats;
		for (LocalPlayRecord _local_play_record : _local_play_history) {
			_bandit=_local_play_record.bandit.clone();
			String _bandit_str=StrRepr.strRepr(_bandit);
			if(!play_hist_map.containsKey(_bandit_str)){
				_local_play_hist_stats=new LocalPlayHistStats(_bandit, _local_play_record.success, _local_play_record.bandit_params.getGittinsIndex());
				play_hist_map.put(_bandit_str, _local_play_hist_stats);
			}else{
				play_hist_map.get(_bandit_str).updateLocalPlayHistSats(_local_play_record.success, _local_play_record.bandit_params.getGittinsIndex());
			}
		}
		for(Map.Entry<String, LocalPlayHistStats> entry : play_hist_map.entrySet()){
			if(entry.getValue().success_percentage > _min_success_percent && entry.getValue().play_count > _min_play_count){
				list_of_successful_bandits.add(new SuccessfulBandits(entry.getValue().bandit.clone(), entry.getValue().normalized_index));
			}
		}	
		return list_of_successful_bandits;
	}

	private static class sortByNormIndex implements Comparator<SuccessfulBandits>{
		public int compare(SuccessfulBandits s1, SuccessfulBandits s2){
			return(s1.normalized_index.compareTo(s2.normalized_index));
		}	
	}

}


class SuccessfulBandits{
	int[] bandit;
	Double normalized_index;
	public SuccessfulBandits(int[] _bandit, Double _normalized_index) {
		bandit=_bandit.clone();
		normalized_index=_normalized_index;
	}
	public String toString(){
		return new String(StrRepr.strRepr(bandit)+" "+normalized_index.toString());
	}
}

class LocalPlayHistStats{
	int[] bandit;
	Double play_count;
	Double success_cnt;
	Double fail_cnt;
	Double success_percentage;
	Double normalized_index;
	public LocalPlayHistStats(int[] _bandit, Boolean _success, Double _normalized_index) {
		bandit=_bandit.clone();
		success_cnt=0.0;
		fail_cnt=0.0;
		success_percentage=0.0;
		normalized_index=_normalized_index;
		if(_success){
			success_cnt=1.0;
		}else {
			fail_cnt=1.0;
		}
		play_count=success_cnt+fail_cnt;
		success_percentage=success_cnt / play_count;
	}
	
	public void updateLocalPlayHistSats(Boolean _success, Double _normalized_index){
		normalized_index=_normalized_index;
		if(_success){
			success_cnt++;
		}else{
			fail_cnt++;
		}
		play_count=success_cnt+fail_cnt;
		success_percentage=success_cnt / play_count;
	}
}

class GaussianParams{
	private Integer span;
	private Integer mean;
	private Double std_dev;
	private Double std_dev_resize_factor;
	private String param;
	public GaussianParams(String _param, Double _std_dev_resize_factor){
		this.param=_param;
		int _index = ParamSetter.Scheme_Params_map_keylist.indexOf(param);
		std_dev_resize_factor=_std_dev_resize_factor;
		assert(_index>=0);
		span=ParamSetter.Scheme_Params_values_list.get(_index).size();
		mean=(span-1) / 2;
		std_dev= (double) mean / 2.0;
	}
	public Integer getMean(){
		return mean;
	}
	public Double getStdDev(){
		return std_dev;
	}
	public Integer getSpan(){
		return span;
	}
	public void updateParams(Integer _mean){
		mean=_mean;
		Double _interval;
		if(mean>=span / 2){
			_interval=(double) (span-mean);
		}else{
			_interval=(double) mean;
		}
		std_dev=_interval / 2.0;
		if(param.equals("Np")){
			std_dev=_interval;		//explore more in Np domain
		}
		if(std_dev==0.0)
			std_dev=0.5;
		System.out.println("mean = "+mean+" _interval = "+_interval+" std_dev = "+std_dev);
		//std_dev=std_dev*std_dev_resize_factor;
	}
	public void resetParams(){
		mean=(span - 1) / 2;
		std_dev=(double) mean / 2.0;
	}
	public String toString(){
		return new String(span+" "+mean+" "+std_dev+" "+std_dev_resize_factor);
	}
}









