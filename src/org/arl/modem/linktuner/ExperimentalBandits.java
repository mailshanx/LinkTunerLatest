package org.arl.modem.linktuner;

import jade.util.Logger;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.logging.Level;

import org.arl.modem.FAPIMessage;
 public class ExperimentalBandits extends ExperimentTemplate implements Notifiable<LinktunerNotifications> {
	 									   //  MTYPE   DMODE   MPSK    NC       NP   	Nz   PKTLEN    FEC     TX-ATT
	public final static int brigands_init[][]={ 
												{1,		1,		1,		4,		32,		0,		1,		0,		22},    //Coherent,	freq,	QPSK,	Nc=1024,	Np=512,		TX_ATT(=36),		Pktlen=48,		full coding. 	duration=?				
												{1,		1,		1,		3,		32,		0,		1,		0,		22},	   //Coherent,	freq,	QPSK,	Nc=512, 	Np=256,		TX_ATT(=36),		Pktlen=48,		full coding.	duration=?
												{1,		0,		1,		0,		32,		0,		1,		0,		22},	   //Coherent,	time,	QPSK,	Nc=64,		Np=32,		TX_ATT(=36),		Pktlen=48,		full coding.	duration=?	
												{1,		0,		1,		1,		32,		0,		1,		0,		22},	   //Coherent,	time,	QPSK,	Nc=128,		Np=64,		TX_ATT(=36),		Pktlen=48,		full coding.	duration=?
												{1,		1,		0,		4,		32,		0,		1,		0,		22},    //Coherent,	freq,	BPSK,	Nc=1024,	Np=512,		TX_ATT(=36),		Pktlen=48,		full coding. 	duration=?				
												{1,		1,		0,		3,		32,		0,		1,		0,		22},	   //Coherent,	freq,	BPSK,	Nc=512, 	Np=256,		TX_ATT(=36),		Pktlen=48,		full coding.	duration=?
												{1,		0,		0,		0,		32,		0,		1,		0,		22},	   //Coherent,	time,	BPSK,	Nc=64,		Np=32,		TX_ATT(=36),		Pktlen=48,		full coding.	duration=?	
												{1,		0,		0,		1,		32,		0,		1,		0,		22},	   //Coherent,	time,	BPSK,	Nc=128,		Np=64,		TX_ATT(=36),		Pktlen=48,		full coding.	duration=?
												{1,		0,		1,		3,		32,		0,		1,		0,		22},
												{1,		0,		1,		2,		32,		0,		1,		0,		22},
												
												{0,		0,		1,		1,		16,		0,		1,		0,		22},
												};
	
	public static int brigands[][]=brigands_init.clone();
	
//	public static int brigands[][] = {{0,0,0,0,0,0,0,0,0}
//									 };
	
	//check if these are valid
	//public static Double relativeDataRates[];
	
	public static boolean EXPERIMENT_COMPLETED=false;
	public static boolean LOCAL_EXPERIMENT_COMPLETED=false;
	
	//private int banditID=0;
	public int banditCurrent;
	private int localExperimentPlayCount=0;
	private int playCountInitValue=0;
	private int pilotPacketCntForActiveTuning=0;
	
	public static int BANDIT_SETTLE_FACTOR=3;  //No of times(+1) you need to play the same arm consecutively 
												//(and successfully) to declare that the bandit 
												//has "settled".
	
	private static int bandit_flutter_factor=8;//If the bandit hasn't settled
	
	public static final int SPARSE_SAMPLING_IN_PROGRESS=1000; 
	public static final int STEADY_STATE=1001;
		
	private ParamSetter param_setter;
	protected TweakRecord tweak_record;
	private PktLenManager pkt_len_manager;	//reserved for later use
	private BrigandGenerable brigand_generator;
	private GaussianBanditHistoryTracker gauss_bandit_history_tracker;
	private GaussBanditParamManager bandit_param_manager;
	
	protected static ArrayList<TweakRecord> tweak_log;
	
	private boolean ENABLE_CALLBACK=false;
	
	public final static int INITIATE_EXPERIMENTS=999;
//	public final static int SET_SCHEME_REQUEST_COMPLETED=1000;
	public final static int TRIGGER_TEST_PKT_TRAINS=1001;
	public static final int UPDATE_RECENT_STATS=1002;
	
	protected ExperimentalBandits(ParamSetter _param_setter, Exploit _exploit, Logger _log){
		this.param_setter=_param_setter;
		this.exploit=_exploit;
		this.log=_log;
		this.pkt_len_manager = new PktLenManager(_param_setter, _log);
		this.brigand_generator = new GaussianBrigandGenerator(log);
		this.gauss_bandit_history_tracker=new GaussianBanditHistoryTracker(exploit, log);
		this.bandit_param_manager=new GaussBanditParamManager(exploit, log, brigands);
		tweak_log = new ArrayList<TweakRecord>();
		//brigands=pkt_len_manager.adjustPktLen(brigands);
		log.fine("about to call resetLocalExperiment()");
		String pwd = System.getProperty("user.dir");
		In in = new In(pwd+"/linktuner.config");
		while(!in.isEmpty()){
			String s = in.readLine();
			String param=s.split("\\s+")[0];
			if(param.equals("BANDIT_SETTLE_FACTOR")){
				BANDIT_SETTLE_FACTOR=Integer.parseInt(s.split("\\s+")[1]);
				log.fine("set BANDIT_SETTLE_FACTOR = "+BANDIT_SETTLE_FACTOR);
			}else if (param.equals("bandit_flutter_factor")) {
				bandit_flutter_factor=Integer.parseInt(s.split("\\s+")[1]);
				log.fine("set bandit_flutter_factor = "+bandit_flutter_factor);
			}
		}
		resetLocalExperiment(brigands);
		log.fine("ExperimentalBandits is loaded");
	}
	protected void handleMessage(FAPIMessage msg){
		if (msg.getAsInteger("ExperimentalBanditCommands", -1)==INITIATE_EXPERIMENTS) {
			log.fine("turning on ENABLE_CALLBACK on experimentalBandits");
			ENABLE_CALLBACK=true;
			log.fine("brigand 1 ======== "+StrRepr.strRepr(brigands[1]));
			bandit_param_manager.printBanditParamStatus();
			beginActiveTuning(70);
		}
	}
	
	private void beginActiveTuning(int _no_of_pilot_packets){
		pilotPacketCntForActiveTuning=_no_of_pilot_packets;
		playCountInitValue=ExperimentTemplate.getPlayCount();
		runExperiment(INITIATE_EXPERIMENTS);
	}
	
	@Override
	public void handleLinktunerNotifications(LinktunerNotifications ntf){
		if(ENABLE_CALLBACK){
			log.fine("inside ExperimentalBandits");
			handleSomeLinktunerNtfs(ntf);
		}
	}
	
	@Override
	protected void initiateExperiment(){
		banditCurrent=bandit_param_manager.getBestBanditID();
		param_setter.currentSetSchemeParam=brigands[banditCurrent];
		param_setter.setSchemeRequest(param_setter.currentSetSchemeParam, true);
		log.fine("banditCurrent = "+banditCurrent+", bandit= "+StrRepr.strRepr(brigands[banditCurrent])+
				", index = "+bandit_param_manager.getBestBanditNormalizedIndex()+" localExperimentPlayCount = "+localExperimentPlayCount);			
	}
	
	@Override
	protected void updateRecentStats(){
		//log.fine("in runExperiment : UPDATE_RECENT_STATS");
		localExperimentPlayCount++;
		LOCAL_EXPERIMENT_COMPLETED=HasBanditSettled.check(gauss_bandit_history_tracker.getLocalPlayHistory(), BANDIT_SETTLE_FACTOR, banditCurrent)
				||gauss_bandit_history_tracker.getLocalPlayHistory().size()>bandit_flutter_factor;
		if(ExperimentTemplate.getPlayCount()>=(playCountInitValue+pilotPacketCntForActiveTuning)){
			EXPERIMENT_COMPLETED=true;
		}else{
			EXPERIMENT_COMPLETED=false;
		}
		bandit_param_manager.updateBanditParams(brigands, banditCurrent);
		gauss_bandit_history_tracker.updateHistory(brigands, banditCurrent, bandit_param_manager.getBanditParams(StrRepr.strRepr(brigands[banditCurrent])));
		bandit_param_manager.printBanditParamStatus();
		gauss_bandit_history_tracker.printLatestHistory();
		if (!EXPERIMENT_COMPLETED && !LOCAL_EXPERIMENT_COMPLETED) {
			log.fine("calling runExperiment(INITIATE_EXPERIMENTS) ");
			runExperiment(INITIATE_EXPERIMENTS);				
		}else if(!EXPERIMENT_COMPLETED && LOCAL_EXPERIMENT_COMPLETED){
			log.fine("terminal bandit = "+bandit2str(brigands, banditCurrent)); 
			log.fine("end of local experiment! starting a new one now");
			if (brigand_generator.getTunerState()!=STEADY_STATE) {
				brigands = brigand_generator.genBrigandSetAlternate(brigands[banditCurrent], brigands, 
																	gauss_bandit_history_tracker.getLatestNonEmptyLocalPlayHistory());
				resetLocalExperiment(brigands);
				bandit_param_manager.printBanditParamStatus();
			}
			runExperiment(INITIATE_EXPERIMENTS);
			return;
		}else if(EXPERIMENT_COMPLETED){
			log.fine("end of experiment. printing grandPlayHistory");
			gauss_bandit_history_tracker.printGrandPlayHistory();
			return;
		}
	}
		
	protected void resetLocalExperiment(int[][] _brigands){
		log.fine("resetting local experiment");
		EXPERIMENT_COMPLETED=false;
		LOCAL_EXPERIMENT_COMPLETED=false;
		localExperimentPlayCount=0;
		bandit_param_manager.softResetBanditParams(_brigands);
	}
	public void endOfExperiment(){
		gauss_bandit_history_tracker.printGrandPlayHistory();	
	}
	public void logBrigands(int[][] brigands){
		for(int[] bandit : brigands){
			log.fine(""+StrRepr.strRepr(bandit));
		}
	}
	public static String bandit2str(int[][] brigands, int banditID){
		String _banditStr="";
		for (int i = 0; i < brigands[banditID].length; i++) {
			_banditStr=_banditStr.concat(((Integer)brigands[banditID][i]).toString());
		}
		return new String(_banditStr);
	}
	public void stackDump(Throwable ex) {
		  log.log(Level.WARNING, "ExperimentalBandits Stack Dump:", ex);
		  ex.printStackTrace();
		  File file = new File("stackDump.txt");
			FileOutputStream fos;
			try {
				fos = new FileOutputStream(file);
				PrintStream ps = new PrintStream(fos);
				System.setErr(ps);
			   ex.printStackTrace(ps);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
 }















