package org.arl.modem.linktuner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.*;

public class BrigandGenerator implements BrigandGenerable {
	private ParamSetter param_setter;
	private Logger log;
	private HistoryAnalyzer history_analyzer;
	private ExperimentalBandits experimental_bandits;
	
	private Integer _NpUpperLim;
	private Integer _NpLowerLim;
	
	private ArrayList<TweakRecord> tweak_log;
	
	private static final int TWEAKING_NP=2000;
//	private static final int TWEAKING_TX_ATT=1002;
	
	private Integer tuner_state;
	
	public BrigandGenerator(ExperimentalBandits _experimental_bandits, ParamSetter _param_setter, Logger _log){
		this.experimental_bandits = _experimental_bandits;
		this.param_setter = _param_setter;
		this.log = _log;
		this.history_analyzer = new HistoryAnalyzer(param_setter, log);
		tweak_log = new ArrayList<TweakRecord>();
		_NpUpperLim=1024;
		_NpLowerLim=0;
		tuner_state=ExperimentalBandits.SPARSE_SAMPLING_IN_PROGRESS;
	}
	@SuppressWarnings("unchecked")
	public int[][] genBrigandSetAlternate(int[] winningBandit, int[][] _brigands, List<LocalPlayRecord> _local_play_history){
		//function control params
		//loop iterator
		int[][] brigands = _brigands;
		Integer i;
		//Np control params
		int NpCurrent;
		int NpInterLength=3;
		Integer NpIntermediate[] = new Integer[NpInterLength];
		int _slice;
		int jj;
		List<Integer> NpValuesSubArray;		
		ArrayList<Integer> NpIntermediateDist=new ArrayList<Integer>();
		ArrayList<ArrayList<Integer>> NpInterDistSuperList=new ArrayList<ArrayList<Integer>>();
		int NpInterIndices[] = new int[NpIntermediate.length];
		int TX_ATT_indices[];
		
		List<LocalPlayRecord> local_play_history = _local_play_history;
		List<List<LocalParamResults>> local_param_filtered_lists=new ArrayList<List<LocalParamResults>>();
		LocalParamResults local_param_results_successful = new LocalParamResults();
		LocalParamResults local_param_results_failiure = new LocalParamResults();
		TweakRecord _tweak_record = new TweakRecord();
		
		switch (tuner_state) {
		case ExperimentalBandits.SPARSE_SAMPLING_IN_PROGRESS:
			log.fine("tuner_state = SPARSE_SAMPLING_IN_PROGRESS");
			NpCurrent=ParamSetter.Scheme_Params_values_list.get(ParamSetter.Scheme_Params_map_keylist.indexOf("Np")).get(winningBandit[ParamSetter.Scheme_Params_map_keylist.indexOf("Np")]);
			if(!tweak_log.isEmpty()){
				log.warning("expecting an empty tweak log!");
			}
			//NpUpperLim=NpCurrent;
			//NpLowerLim=0;
			_NpUpperLim=NpCurrent;
			//_NpLowerLim=NpLowerLim;
			NpValuesSubArray= ParamSetter.Scheme_Params_values_list.get(ParamSetter.Scheme_Params_map_keylist.indexOf("Np"));
			
			_slice=Math.abs((_NpUpperLim-_NpLowerLim)) / NpIntermediate.length;
			for (int j = 0; j < NpIntermediate.length; j++) {
				NpIntermediate[j]=_NpUpperLim-j*_slice;
				log.fine("NpIntermediate[j] = "+NpIntermediate[j]);
			}
						
			for (int j = 0; j < NpIntermediate.length; j++) {
				NpIntermediateDist.clear();
				for (int j2 = 0; j2 < NpValuesSubArray.size(); j2++) {
					NpIntermediateDist.add(Math.abs(NpValuesSubArray.get(j2)-NpIntermediate[j]));
				}
				NpInterDistSuperList.add((ArrayList<Integer>) NpIntermediateDist.clone());
			}
			for (int j = 0; j < NpInterIndices.length; j++) {
				NpInterIndices[j]=NpInterDistSuperList.get(j).indexOf(Collections.min(NpInterDistSuperList.get(j)));
			}
			
			//reset bandits:
			brigands=new int[NpInterIndices.length*3][winningBandit.length];
			log.fine("brignads.length = "+brigands.length);
			jj=0;
			while (jj<NpInterIndices.length*3) {
				brigands[jj]=winningBandit.clone(); brigands[jj][ParamSetter.Scheme_Params_map_keylist.indexOf("Np")]=NpInterIndices[jj / 3]; jj++;
				brigands[jj]=brigands[jj-1].clone();brigands[jj][ParamSetter.Scheme_Params_map_keylist.indexOf("FEC")]=2; jj++;
				brigands[jj]=brigands[jj-1].clone();brigands[jj][ParamSetter.Scheme_Params_map_keylist.indexOf("FEC")]=1; jj++;
			}

			log.fine("_NpUpperLim = "+_NpUpperLim);
			log.fine("_NpLowerLim = "+_NpLowerLim);
			
			_tweak_record.paramTweaked="Np";
			_tweak_record.upperValue=NpIntermediate[0];
			_tweak_record.lowerValue=NpIntermediate[NpIntermediate.length-1];
			_tweak_record.upperValueIndex=NpInterIndices[0];
			_tweak_record.lowerValueIndex=NpInterIndices[NpInterIndices.length-1];
			_tweak_record.indexList=Arrays.asList(NpInterIndices);
		
			tuner_state=TWEAKING_NP;
			log.fine("tuner_state = TWEAKING_NP");

			break;
		case TWEAKING_NP:
			log.fine("tuner_state = TWEAKING_NP");
			NpCurrent=ParamSetter.Scheme_Params_values_list.get(ParamSetter.Scheme_Params_map_keylist.indexOf("Np")).get(winningBandit[ParamSetter.Scheme_Params_map_keylist.indexOf("Np")]);
			local_param_filtered_lists=history_analyzer.analyzeLocalHistory(local_play_history, brigands);
			log.fine(" local_param_filtered_lists : ");
			for (Iterator iterator = local_param_filtered_lists.iterator(); iterator.hasNext();) {
				List<LocalParamResults> list = (List<LocalParamResults>) iterator.next();
				log.fine(" list from local_param_filtered_lists : ");
				for (Iterator iterator2 = list.iterator(); iterator2.hasNext();) {
					LocalParamResults localParamResults = (LocalParamResults) iterator2.next();
					log.fine(""+localParamResults);
				}
			}

			if (!local_param_filtered_lists.get(0).isEmpty()) { //the success list is non-empty
				log.fine("success list is non-empty");
				local_param_results_successful=local_param_filtered_lists.get(0).get(0);
				_NpUpperLim=local_param_results_successful.paramValue;
				log.fine("_NpUpperLim = "+_NpUpperLim);
				if (!local_param_filtered_lists.get(1).isEmpty()) { //we've had some failures too!
					log.fine("we've had some failures too!");
					local_param_results_failiure=local_param_filtered_lists.get(1).get(local_param_filtered_lists.get(1).size()-1);
					_NpLowerLim=local_param_results_failiure.paramValue;
					log.fine("_NpLowerLim = "+_NpLowerLim);
				}
			}
			else if(local_param_filtered_lists.get(0).isEmpty() ){ //we've had no success!. everything has failed :(((
				log.fine("we've had no success!. everything has failed :(((");
				if(!local_param_filtered_lists.get(1).isEmpty()){  //adjust the lower limit if you get a non-empty failiure list
					local_param_results_failiure=local_param_filtered_lists.get(1).get(local_param_filtered_lists.get(1).size()-1);
					_NpLowerLim=local_param_results_failiure.paramValue;
					log.fine("_NpLowerLim = "+_NpLowerLim);
				}
				_NpUpperLim=2*_NpLowerLim;
				if(_NpUpperLim>ParamSetter.Scheme_Params_values_list.get(ParamSetter.Scheme_Params_map_keylist.indexOf("Np")). //if NpUpperLim exceeds max Np Value, set it to max value
						get(ParamSetter.Scheme_Params_values_list.get(ParamSetter.Scheme_Params_map_keylist.indexOf("Np")).size()-1) ){
					_NpUpperLim=ParamSetter.Scheme_Params_values_list.get(ParamSetter.Scheme_Params_map_keylist.indexOf("Np")).
							get(ParamSetter.Scheme_Params_values_list.get(ParamSetter.Scheme_Params_map_keylist.indexOf("Np")).size()-1);
				}
				log.fine("_NpUpperLim = "+_NpUpperLim);
			}
			//
//			_NpUpperLim=NpUpperLim;
//			_NpLowerLim=NpLowerLim; 
		
			NpValuesSubArray= ParamSetter.Scheme_Params_values_list.get(ParamSetter.Scheme_Params_map_keylist.indexOf("Np"));
			_slice=Math.abs((_NpUpperLim-_NpLowerLim)) / NpIntermediate.length;
			for (int j = 0; j < NpIntermediate.length; j++) {
				NpIntermediate[j]=_NpUpperLim-j*_slice;
				log.fine("NpIntermediate[j] = "+NpIntermediate[j]);
			}
			
			log.fine("NpValuesSubArray = "+NpValuesSubArray.toString());
			
			for (int j = 0; j < NpIntermediate.length; j++) {
				NpIntermediateDist.clear();
				for (int j2 = 0; j2 < NpValuesSubArray.size(); j2++) {
					NpIntermediateDist.add(Math.abs(NpValuesSubArray.get(j2)-NpIntermediate[j]));
				}
				NpInterDistSuperList.add((ArrayList<Integer>) NpIntermediateDist.clone());
			}
			for (int j = 0; j < NpInterIndices.length; j++) {
				NpInterIndices[j]=NpInterDistSuperList.get(j).indexOf(Collections.min(NpInterDistSuperList.get(j)));
			}
			
			//reset bandits:
			brigands=new int[NpInterIndices.length*3][winningBandit.length];
			log.fine("brignads.length = "+brigands.length);
			jj=0;
			while (jj<NpInterIndices.length*3) {
				brigands[jj]=winningBandit.clone(); brigands[jj][ParamSetter.Scheme_Params_map_keylist.indexOf("Np")]=NpInterIndices[jj / 3]; jj++;
				brigands[jj]=brigands[jj-1].clone();brigands[jj][ParamSetter.Scheme_Params_map_keylist.indexOf("FEC")]=2; jj++;
				brigands[jj]=brigands[jj-1].clone();brigands[jj][ParamSetter.Scheme_Params_map_keylist.indexOf("FEC")]=1; jj++;
			}

			
			log.fine("_NpUpperLim = "+_NpUpperLim);
			log.fine("_NpLowerLim = "+_NpLowerLim);			
			_tweak_record.paramTweaked="Np";
			_tweak_record.upperValue=NpIntermediate[0];
			_tweak_record.lowerValue=NpIntermediate[NpIntermediate.length-1];
			_tweak_record.upperValueIndex=NpInterIndices[0];
			_tweak_record.lowerValueIndex=NpInterIndices[NpInterIndices.length-1];
			_tweak_record.indexList=Arrays.asList(NpInterIndices);
		
			if(!tweak_log.isEmpty()){
				if ( Math.abs(tweak_log.get(tweak_log.size()-1).upperValueIndex-tweak_log.get(tweak_log.size()-1).lowerValueIndex) < 3 
						&& !local_param_filtered_lists.get(0).isEmpty()) {
						if (HasBanditSettled.check(local_play_history, experimental_bandits.BANDIT_SETTLE_FACTOR, experimental_bandits.banditCurrent)) {
							tuner_state=ExperimentalBandits.STEADY_STATE;
							log.fine("tuner_state = STEADY_STATE");
						}
					}else{
						tuner_state=TWEAKING_NP;
						log.fine("tuner_state = TWEAKING_NP");
					}
			}else{
				log.warning("tweak_log is empty! how can!?!");
			}
			break;
		default:
			brigands=new int[1][winningBandit.length];
			brigands[0]=winningBandit.clone();
			break;
//		case TWEAKING_TX_ATT:
//			log.fine("tuner_state = TWEAKING_TX_ATT");
//			List<Integer> _TX_ATT_value_list = param_setter.Scheme_Params_values_list.get(param_setter.Scheme_Params_map_keylist.indexOf("TX_ATT"));
//			TX_ATT_indices=new int[_TX_ATT_value_list.size()];
//			brigands=new int[_TX_ATT_value_list.size()][winningBandit.length];
//			for (int j = 0; j < _TX_ATT_value_list.size(); j++) {
//				TX_ATT_indices[j]=j;
//				brigands[j]=winningBandit.clone();
//				brigands[j][param_setter.Scheme_Params_map_keylist.indexOf("TX_ATT")]=TX_ATT_indices[j];
//			}
//			if (hasBanditSettled()) {
//				tuner_state=STEADY_STATE;
//				log.fine("tuner_state = STEADY_STATE");
//			}
//			_tweak_record.paramTweaked="TX_ATT";
//			_tweak_record.upperValue=_TX_ATT_value_list.get(0);
//			_tweak_record.lowerValue=_TX_ATT_value_list.get(_TX_ATT_value_list.size()-1);
//			_tweak_record.upperValueIndex=TX_ATT_indices[0];
//			_tweak_record.lowerValueIndex=TX_ATT_indices[TX_ATT_indices.length-1];
//			_tweak_record.indexList=Arrays.asList(TX_ATT_indices);
//			break;
//		case TWEAKING_FEC:
//			//reset brigands just before mutating them
//			log.fine("tuner_state = TWEAKING_FEC");
//			brigands=new int[3][8];
//			brigands[0]=winningBandit.clone();
//			brigands[1]=winningBandit.clone();
//			brigands[2]=winningBandit.clone();
//			
//			brigands[0][param_setter.Scheme_Params_map_keylist.indexOf("FEC")]=1; //changed to convolutional only
//			brigands[1][param_setter.Scheme_Params_map_keylist.indexOf("FEC")]=2; //changed to golay only
//			brigands[2][param_setter.Scheme_Params_map_keylist.indexOf("FEC")]=3; //changed to convolutional + golay
//			
//			_tweak_record.paramTweaked="FEC";
//			_tweak_record.upperValue=param_setter.Scheme_Params_values_list.get(param_setter.Scheme_Params_map_keylist.indexOf("FEC")).
//									get(brigands[1][param_setter.Scheme_Params_map_keylist.indexOf("FEC")]);
//			_tweak_record.lowerValue=param_setter.Scheme_Params_values_list.get(param_setter.Scheme_Params_map_keylist.indexOf("FEC")).
//					get(brigands[2][param_setter.Scheme_Params_map_keylist.indexOf("FEC")]);
//			_tweak_record.upperValueIndex=2;
//			_tweak_record.lowerValueIndex=3;
//			
//			tuner_state=STEADY_STATE;
//			log.fine("tuner_state = STEADY_STATE");
//			break;
//		case STEADY_STATE:
//			brigands=new int[3][8];
//			brigands[0]=winningBandit.clone();
//			brigands[1]=winningBandit.clone();
//			brigands[2]=winningBandit.clone();
//
//			tuner_state=STEADY_STATE;
//			log.fine("tuner_state=STEADY_STATE");
//		default:
//			break;
		}
		
		tweak_log.add(_tweak_record);
		log.fine(" tweak_log :");
		for (Iterator iterator = tweak_log.iterator(); iterator
				.hasNext();) {
			log.fine(""+iterator.next());
		}
		log.fine("just modified brigands. Lets check if the modification actually happended. brigand list: ");
		for (int i1 = 0; i1 < brigands.length; i1++) {
			log.fine("bandit "+i1+" = "+StrRepr.strRepr(brigands[i1]));
		}
		return brigands;
	}
	public Integer getTunerState(){
		return tuner_state;
	}
}
