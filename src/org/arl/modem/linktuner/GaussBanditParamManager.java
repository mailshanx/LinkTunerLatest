package org.arl.modem.linktuner;

import java.util.logging.Logger;

public class GaussBanditParamManager extends BanditParamManager {

	public GaussBanditParamManager(Exploit _exploit, Logger _log, int[][] _brigands) {
		super(_exploit, _log, _brigands);
	}
	
	@Override
	protected void updateBanditParamsAlphaBeta(int[] _bandit_updated){
		String bandit_updated_as_str=StrRepr.strRepr(_bandit_updated);
		if(exploit.getStats(Exploit.GET_RECENT_BIT_CNT)!=-1.0){
			double _bit_cnt=exploit.getStats(Exploit.GET_RECENT_BIT_CNT);
			double _fail_bit_cnt=exploit.getStats(Exploit.GET_RECENT_ERROR_CNT);
			double _success_bit_cnt=_bit_cnt-_fail_bit_cnt;
//			double _bit_error_rate = _fail_bit_cnt / _bit_cnt;
//			if(_bit_error_rate < 0.05){
//				_fail_bit_cnt*=2.0;
//				_success_bit_cnt=_bit_cnt - _fail_bit_cnt;
//			}else if(_bit_error_rate < 0.10){
//				_fail_bit_cnt*=4.0;
//				_success_bit_cnt=_bit_cnt - _fail_bit_cnt;
//			}else if(_bit_error_rate < 0.15){
//				_fail_bit_cnt*=6.0;
//				_success_bit_cnt=_bit_cnt - _fail_bit_cnt;
//			}else{
//				_fail_bit_cnt=exploit.getStats(Exploit.GET_RECENT_BIT_CNT);
//				_success_bit_cnt=0;
//			}
			bandit_param_list.get(bandit_updated_as_str).alpha+=_success_bit_cnt;
			bandit_param_list.get(bandit_updated_as_str).beta+=_fail_bit_cnt;
		}else{
			//assumption that batch size =1
			bandit_param_list.get(bandit_updated_as_str).beta+=8.0*ParamSetter.getPhyParam(ParamSetter.ACTIVE_SCHEME, ParamSetter.PKTLEN);//????
		}
	}

}
