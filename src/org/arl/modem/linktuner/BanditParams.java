package org.arl.modem.linktuner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.*;

public class BanditParams implements Notifiable<BanditParamNtf>{
	private Logger log;
	protected double gittinsIndex;
	private double z; 		//measurement for the kalman filter
	private List<Double> measurement_history;		//the state actually just consists of a list of measurement history
	private Double absoluteDataRate;
	private Double abs_bandit_reward;
	private Double normalized_bandit_reward;
	private int[] bandit;
	private KalmanFilter kalman_filter;
	private GittinsIndexGenerator gittins_index_generator;
	
	public BanditParams(Logger _log, int[] _bandit, Double _abs_data_rate){
		this.log=_log;
		this.bandit=_bandit.clone();
		this.absoluteDataRate=_abs_data_rate;
		this.abs_bandit_reward=computeReward(absoluteDataRate);
		normalized_bandit_reward=abs_bandit_reward;
		gittins_index_generator=new GittinsIndexGenerator();
		
		z=0.0;
		measurement_history=new ArrayList<Double>();
		measurement_history.add(0.5);				//this is basically our initial state
		kalman_filter=new KalmanFilter(getMeasurementError(this.bandit));
		gittinsIndex=gittins_index_generator.gittinsIndex(measurement_history.get(measurement_history.size()-1), kalman_filter, normalized_bandit_reward);
		log.fine(StrRepr.strRepr(bandit)+" "+gittinsIndex+" "+absoluteDataRate+" "+abs_bandit_reward);
		//update gittins index here
	}
	
	@Override
	public void handleLinktunerNotifications(BanditParamNtf ntf) {
		if(ntf.getNtfSrc().equals(BanditParamManager.NTF_ID)){
			if(ntf.getMsg().equals(BanditParamManager.BanditParamUpdateMsg)){
				updateFilterAndMeasurementsAndGittinsIndex(ntf);
			}else if(ntf.getMsg().equals(BanditParamManager.NormalizeBanditParams)){
				this.normalized_bandit_reward=abs_bandit_reward/ntf.getMaxReward();
				this.gittinsIndex=gittins_index_generator.gittinsIndex(measurement_history.get(measurement_history.size()-1), kalman_filter, normalized_bandit_reward);
			}
		}
	}
	
	//update filter, followed by reward and finally gittins index in that order
	private void updateFilterAndMeasurementsAndGittinsIndex(BanditParamNtf ntf){
		kalman_filter.performTimeUpdate();
		z=getMeasurement(ntf);
		if(z!=-1.0){
			measurement_history.add(z);				//every time u've added a measurement, ur state has changed, and now u need to update ur gittins index
			int len=measurement_history.size();
			kalman_filter.measurementUpdate(measurement_history.get(len-1));	//we update our filter to the second-last measurement, 'coz the index generator
																				//updates for the latest measurement
			abs_bandit_reward=computeReward(absoluteDataRate, (1.0-kalman_filter.getEstimate()));
			gittinsIndex=gittins_index_generator.gittinsIndex(measurement_history.get(len-1), kalman_filter, normalized_bandit_reward);
		}
	}
	
	//return -1.0 if no measurement can be inferred
	private Double getMeasurement(BanditParamNtf ntf){
		if(getMeasurementFromRelevantBandit(ntf)!=-1.0){		//measurements from similar bandits are disabled
//			return getMeasurementFromRelevantBandit(ntf);
		}else if(getMeasurementFromSameBandit(ntf)!=-1.0){
			return getMeasurementFromSameBandit(ntf);
		}else{
			return -1.0;
		}
		return -1.0;
	}
	
	//Infers a measurement if the other bandit is the same as this one
	private Double getMeasurementFromSameBandit(BanditParamNtf ntf){
		int[] bandit_in_msg=ntf.getBandit().clone();
		Double _z=-1.0;
		if(Arrays.equals(bandit_in_msg, this.bandit)){
			if(!banditIsTestPkt(this.bandit)){		//that means i am a data packet
				if(!((ntf.NsIncrement+ntf.NfIncrement)==0.0)){
					_z=ntf.NsIncrement/(ntf.NsIncrement+ntf.NfIncrement);
				}else{
					_z=0.0;
				}
			}else{								//i am a test packet!
				_z=ntf.BsIncrement/(ntf.BsIncrement+ntf.BfIncrement);							//we can just assume that a test packet has pretty much no chance of success at a packet level,
																								// coz it uses no FEC coding??? for now, no
			}
		}
		return _z;
	}
	
	private Double getMeasurementFromRelevantBandit(BanditParamNtf ntf){
		int[] bandit_in_msg=ntf.getBandit().clone();
		Double _z=-1.0;
		if(!Arrays.equals(bandit_in_msg, this.bandit)){
			Boolean bandit_in_msg_is_similar=true;					//they are similar if all params except the FEC are the same
			for(String param:ParamSetter.Scheme_Params_map_keylist){
				if(!param.equals("FEC")){
					int param_index=ParamSetter.Scheme_Params_map_keylist.indexOf(param);
					if(bandit[param_index]!=bandit_in_msg[param_index]){
						bandit_in_msg_is_similar=false;
						break;
					}
				}
			}
			if(bandit_in_msg_is_similar){
				if(banditIsTestPkt(this.bandit)){	//i am a test packet, doomed to failure!
					_z=0.0;
				}else{											//i am a data packet
					if(!banditIsTestPkt(bandit_in_msg)){		//other bandit is a data packet too!
						int fec_index=ParamSetter.Scheme_Params_map_keylist.indexOf("FEC");
						Integer my_fec=ParamSetter.Scheme_Params_values_list.get(fec_index).get(this.bandit[fec_index]);
						Integer other_fec=ParamSetter.Scheme_Params_values_list.get(fec_index).get(bandit_in_msg[fec_index]);
						Double my_code_rate=ParamSetter.FEC_mask_to_coderate_mapping.get(my_fec);
						Double other_code_rate=ParamSetter.FEC_mask_to_coderate_mapping.get(other_fec);
						Double other_z=0.0;
						if(!((ntf.NsIncrement+ntf.NfIncrement)==0.0)){
							other_z=ntf.NsIncrement/(ntf.NsIncrement+ntf.NfIncrement);
						}else{
							other_z=0.0;
						}
						if(other_z>=0.5 && my_code_rate<other_code_rate){	//other bandit was a success and my code rate is lower, so i shud succeed too
							_z=1.0;
						}else if(other_z<0.5 && my_code_rate>other_code_rate){	//other bandit failed, and my code rate is greater, so i'll fail too
							_z=0.0;
						}
					}else{										//other bandit is a test packet
						int fec_index=ParamSetter.Scheme_Params_map_keylist.indexOf("FEC");
						Integer my_fec=ParamSetter.Scheme_Params_values_list.get(fec_index).get(this.bandit[fec_index]);
						Double my_code_rate=ParamSetter.FEC_mask_to_coderate_mapping.get(my_fec);
						Double other_ber=0.5;
						if(!((ntf.BsIncrement+ntf.BfIncrement)==0.0)){
							other_ber=ntf.BfIncrement/(ntf.BsIncrement+ntf.BfIncrement);
						}
						new FECPenalty();
						Double max_possible_coderate=FECPenalty.getCodeRate(other_ber);
						if(my_code_rate<max_possible_coderate){			//i have adequete coding to overcome this amount of BER
							_z=1.0;
						}else{
							_z=0.0;
						}
					}
				}
			}
		}
		return _z;
	}
	
	private Double computeReward(Double _abs_data_rate){
		new FECPenalty();
		return _abs_data_rate*FECPenalty.getCodeRateEstimate();
	}
	
	private Double computeReward(Double _abs_data_rate, Double _ber){
		new FECPenalty();
		return _abs_data_rate*FECPenalty.getCodeRate(_ber);	
	}
	public Double getAbsDataRate(){
		return absoluteDataRate;
	}
	public Double getAbsReward(){
		return abs_bandit_reward;
	}
	public Double getNormalizedReward(){
		return normalized_bandit_reward;
	}
	public Double getFilterEstimate(){
		return kalman_filter.getEstimate();
	}
	public double getGittinsIndex(){
		return gittinsIndex;
	}
	private Double getMeasurementError(int[] _bandit){
		int _fec_index = ParamSetter.Scheme_Params_map_keylist.indexOf("FEC");
		int fec = ParamSetter.Scheme_Params_values_list.get(_fec_index).get(_bandit[_fec_index]);
		if(fec==0){
			return 1.0;		//testpacket
		}else{
			return 5.0;		//data packet
		}
	}
	private Boolean banditIsTestPkt(int[] _bandit){
		int _fec_index = ParamSetter.Scheme_Params_map_keylist.indexOf("FEC");
		int fec = ParamSetter.Scheme_Params_values_list.get(_fec_index).get(_bandit[_fec_index]);
		if(fec==0){
			return true;
		}else{
			return false;
		}
	}
	
}
