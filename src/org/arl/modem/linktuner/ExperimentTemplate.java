package org.arl.modem.linktuner;

import jade.util.Logger;

public abstract class ExperimentTemplate implements Notifiable<LinktunerNotifications>{

	protected Logger log;
	protected Exploit exploit;
	
	protected static final int INITIATE_EXPERIMENTS=999;
	protected static final int TRIGGER_TEST_PKT_TRAINS=1001;
	protected static final int UPDATE_RECENT_STATS=1002;
		
	private static int triggerTestPktTrainCnt=0;
	private static int playCount=0;

//	@Override
//	public void handleLinktunerNotifications(LinktunerNotifications ntf){
//		if(ENABLE_CALLBACK){
//			if(ntf.getNtfSrc().equals(ParamSetter.LINKTUNER_NTF_ID)){
//				if(ntf.getMsg().equals(ParamSetter.SET_SCHEME_PARAM_DONE_NTF)){
//					runExperiment(TRIGGER_TEST_PKT_TRAINS);
//				}
//			}
//			if(ntf.getNtfSrc().equals(Exploit.LINKTUNER_NTF_ID)){
//				if(ntf.getMsg().equals(Exploit.TEST_PKT_TRAIN_SENT)){
//					runExperiment(UPDATE_RECENT_STATS);
//				}
//			}
//		}
//	}
	
	protected void handleSomeLinktunerNtfs(LinktunerNotifications ntf){
		if(ntf.getNtfSrc().equals(ParamSetter.LINKTUNER_NTF_ID)){
			if(ntf.getMsg().equals(ParamSetter.SET_SCHEME_PARAM_DONE_NTF)){
				runExperiment(TRIGGER_TEST_PKT_TRAINS);
			}
		}
		if(ntf.getNtfSrc().equals(Exploit.LINKTUNER_NTF_ID)){
			if(ntf.getMsg().equals(Exploit.TEST_PKT_TRAIN_SENT)){
				runExperiment(UPDATE_RECENT_STATS);
			}
		}
	}
	
	protected void runExperiment(int msgType){
		switch (msgType) {
		case INITIATE_EXPERIMENTS:
			//log.fine("in runExperiment : INITIATE_EXPERIMENTS");
			initiateExperiment();
			break;
		case TRIGGER_TEST_PKT_TRAINS:
			triggerTestPktTrainCnt++;
			triggerTestPktTrain();
			break;
		case UPDATE_RECENT_STATS:
			playCount++;
			updateRecentStats();
			break;
		default:
			break;
		}
	}

	protected abstract void initiateExperiment();
	protected void triggerTestPktTrain(){
		Exploit.ENABLE=1;
		log.fine("about to call sendTestPacketTrain()");
		exploit.sendTestPacketTrain(ParamSetter.ACTIVE_SCHEME, 1, true);
	}
	protected abstract void updateRecentStats();
	
	protected static int getPlayCount(){
		return playCount;
	}
	
}
