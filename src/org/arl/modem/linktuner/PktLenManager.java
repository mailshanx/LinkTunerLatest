package org.arl.modem.linktuner;

import jade.util.Logger;

public class PktLenManager {
	private ParamSetter param_setter;
	private Logger log;
	public PktLenManager(ParamSetter _param_setter, Logger _log){
		this.param_setter = _param_setter;
		this.log = _log;
	}
	public int[][] adjustPktLen(int[][] _brigands){
		int _max_pkt_duration=600;
		int _min_pkt_duration=400;
		int[][] brigands=_brigands;
		Double _pkt_duration;
		Integer _pkt_len;
		Integer _max_pkt_len;
		int _pkt_len_index = ParamSetter.Scheme_Params_map_keylist.indexOf("PKTLEN");
		int _pkt_len_ptr;
		int _pkt_len_ptr_max = ParamSetter.Scheme_Params_values_list.get(_pkt_len_index).size() - 1;
		int _bandit[] = new int[brigands[0].length];
		int n;
		boolean result;
		
		for (int i = 0; i < brigands.length; i++) {
			_bandit=brigands[i];
			log.fine("Checking for _pkt_duration condition. Old bandit = "
					+brigands[i][0]+brigands[i][1]+brigands[i][2]+brigands[i][3]
					+brigands[i][4]+brigands[i][5]+brigands[i][6]+brigands[i][7]);
			n=0;
			while (n<_bandit.length) {
				result=false;
				while (!result) {
					result=ParamSetter.setPhyParam(ParamSetter.ACTIVE_SCHEME, ParamSetter.Scheme_Params[n],
							ParamSetter.Scheme_Params_values_list.get(n).get(_bandit[n]));
					//log.fine("setPhyParam.result = "+result);
				}
				n++;
			}
			_pkt_duration = (double) ParamSetter.getPhyParam(ParamSetter.ACTIVE_SCHEME, 14);
			if (_pkt_duration==-1) {
				log.warning("_pkt_duration - 1");
				while (_pkt_duration!=-1) {
					_pkt_duration = (double) ParamSetter.getPhyParam(ParamSetter.ACTIVE_SCHEME, 14);
				}
			}
			_max_pkt_len = ParamSetter.getPhyParam(ParamSetter.ACTIVE_SCHEME, 13);
			if(_max_pkt_len==-1){
				log.warning("_max_pkt_len = -1");
				while(_max_pkt_len!=-1){
					_max_pkt_len = ParamSetter.getPhyParam(ParamSetter.ACTIVE_SCHEME, 13);
				}
			}
			
			_pkt_len = ParamSetter.Scheme_Params_values_list.get(_pkt_len_index).get(_bandit[_pkt_len_index]);
			_pkt_len_ptr = _bandit[_pkt_len_index];
			while ( (_pkt_duration<_min_pkt_duration || _pkt_duration>_max_pkt_duration) && _pkt_len_ptr > 0 && _pkt_len_ptr < _pkt_len_ptr_max
					&& _pkt_len < _max_pkt_len) {
				if (_pkt_duration<_min_pkt_duration) {
					_pkt_len_ptr++;
				}else if(_pkt_duration>_max_pkt_duration){
					_pkt_len_ptr--;
				}
				_bandit[_pkt_len_index]=_pkt_len_ptr;
				
				_pkt_len = ParamSetter.Scheme_Params_values_list.get(_pkt_len_index).get(_bandit[_pkt_len_index]);
				if(_pkt_len==-1){
					log.warning("_pkt_len = 1!");
					while(_pkt_len!=-1){
						_pkt_len = ParamSetter.Scheme_Params_values_list.get(_pkt_len_index).get(_bandit[_pkt_len_index]);
					}
				}
				
				result=false;
				while (!result) {
					result=ParamSetter.setPhyParam(ParamSetter.ACTIVE_SCHEME, ParamSetter.Scheme_Params[_pkt_len_index],
							ParamSetter.Scheme_Params_values_list.get(_pkt_len_index).get(_bandit[_pkt_len_index]));
				}
				
				_pkt_duration = (double) ParamSetter.getPhyParam(ParamSetter.ACTIVE_SCHEME, 14);
				if (_pkt_duration==-1) {
					log.fine("_pkt_duration - 1");
					while (_pkt_duration!=-1) {
						_pkt_duration = (double) ParamSetter.getPhyParam(ParamSetter.ACTIVE_SCHEME, 14);
					}
				}
			}
			
			if(_pkt_len>_max_pkt_len){
				_pkt_len_ptr--;
				_bandit[_pkt_len_index]=_pkt_len_ptr;
				_pkt_duration = (double) ParamSetter.getPhyParam(ParamSetter.ACTIVE_SCHEME, 14);
				if (_pkt_duration==-1) {
					log.fine("_pkt_duration - 1");
					while (_pkt_duration!=-1) {
						_pkt_duration = (double) ParamSetter.getPhyParam(ParamSetter.ACTIVE_SCHEME, 14);
					}
				}
				log.fine("pktlen is close to max_pktlen.");
			}
			
			brigands[i]=_bandit.clone();
			log.fine("_pkt_duration condition has been satisfied. _pkt_duration = "+_pkt_duration
					+" New bandit = "+brigands[i][0]+brigands[i][1]+brigands[i][2]+brigands[i][3]
					+brigands[i][4]+brigands[i][5]+brigands[i][6]+brigands[i][7]);
			
		}
		return brigands.clone();
	}

}
