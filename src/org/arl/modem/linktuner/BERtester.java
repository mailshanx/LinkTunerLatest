package org.arl.modem.linktuner;

import org.arl.modem.FAPIMessage;

import jade.util.Logger;

//class to comprehensively test BERs for all schemes
public class BERtester {
	private Logger log;
	private ParamSetter param_setter;
	private Exploit exploit;
	private BERtestSchemeGenerator scheme_generator;
	private BERtestHistoryTracker BER_test_history_tracker;
	private int brigands[][];
	private int banditCurrent;
	
	private int batch_size;
	private int playCount;
	
	public static boolean ENABLE_CALLBACK=false;
	
	public static boolean EXPERIMENT_COMPLETED=false;
	
	public final static int INITIATE_EXPERIMENTS=999;
	public final static int TRIGGER_TEST_PKT_TRAINS=1000;
	public final static int UPDATE_RECENT_STATS=1001;
		
	public BERtester(ParamSetter _param_setter, Exploit _exploit, Logger _log){
		this.log=_log;
		this.param_setter=_param_setter;
		this.exploit=_exploit;
		this.scheme_generator = new BERtestSchemeGenerator(_log);
		this.BER_test_history_tracker = new BERtestHistoryTracker(_exploit, _log);
		batch_size=1;
		
		String pwd = System.getProperty("user.dir");
		In in = new In(pwd+"/linktuner.config");
		while(!in.isEmpty()){
			String s = in.readLine();
			String param=s.split("\\s+")[0];
			if(param.equals("playCount")){
				playCount=Integer.parseInt(s.split("\\s+")[1]);
				log.fine("set playCount = "+playCount);
			}
		}
	}
	protected void handleMessage(FAPIMessage msg){
		if (msg.getAsInteger("BERtesterCommands", -1)==INITIATE_EXPERIMENTS) {
			ENABLE_CALLBACK=true;
			String pwd = System.getProperty("user.dir");
			In in = new In(pwd+"/linktuner.config");
			while(!in.isEmpty()){
				String s = in.readLine();
				String param=s.split("\\s+")[0];
				if(param.equals("mode")){
					if(!s.split("\\s+")[1].equals("bertest")){
						throw new RuntimeException("change mode to bertest in linktuner.config");
					}
				}
			}
			ParamSetter.resetSchemeParams("bertest");
			log.fine(StrRepr.strRepr(ParamSetter.Scheme_Params_values_list));
			brigands = scheme_generator.getSchemes();
			System.out.println("result of scheme_generator.getSchemes(): ");
			//log.fine(StrRepr.strRepr(brigands));
			log.fine("calling RunBERtest(INITIATE_EXPERIMENTS)");
			RunBERtest(INITIATE_EXPERIMENTS);
		}
	}

	public void RunBERtest(int msgType){
		switch (msgType) {
		case INITIATE_EXPERIMENTS:
			log.fine("arrived at RunBERtest.INITIATE_EXPERIMENTS");
			banditCurrent=playCount;
			param_setter.currentSetSchemeParam=brigands[banditCurrent];
			param_setter.setSchemeRequest(param_setter.currentSetSchemeParam, true);
			break;
		case TRIGGER_TEST_PKT_TRAINS:
			log.fine("arrived at RunBERtest.TRIGGER_TEST_PKT_TRAINS");
			Exploit.ENABLE=1;
			log.fine("set exploit.ENABLE = 1");
			log.fine("about to call sendTestPacketTrain()");
			exploit.sendTestPacketTrain(ParamSetter.ACTIVE_SCHEME, batch_size, true);			
			break;
		case UPDATE_RECENT_STATS:
			log.fine("arrived at RunBERtest.UPDATE_RECENT_STATS");
			playCount++;
			log.fine("playCount = "+playCount);
			if (playCount==brigands.length) {
				EXPERIMENT_COMPLETED=true;
				ENABLE_CALLBACK=false;
			}
			log.fine("EXPERIMENT_COMPLETED = "+EXPERIMENT_COMPLETED);
			BER_test_history_tracker.updateHistory(brigands, banditCurrent);
			BER_test_history_tracker.printLatestHistory();
			if(!EXPERIMENT_COMPLETED){
				RunBERtest(INITIATE_EXPERIMENTS);
			}else {
				log.fine("end of BER test");
				Exploit.ENABLE=0;
				ENABLE_CALLBACK=false;
				BER_test_history_tracker.printGrandPlayHistory();
			}
			break;
		default:
			break;
		}
	}
	
}
