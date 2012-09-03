package org.arl.modem.linktuner;

import jade.util.Logger;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;

import org.arl.modem.FAPIMessage;
 public class ExperimentalBandits {
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
	public static boolean ENABLE_CALLBACK=false;
	
	//private int banditID=0;
	public int banditCurrent;
	private static int playCount=0;
	public static int BANDIT_SETTLE_FACTOR=3;  //No of times(+1) you need to play the same arm consecutively 
												//(and successfully) to declare that the bandit 
												//has "settled".
	
	private static int bandit_flutter_factor=8;//If the bandit hasn't settled
	
	public static final int SPARSE_SAMPLING_IN_PROGRESS=1000; 
	public static final int STEADY_STATE=1001;
		
	private Logger log;
	private ParamSetter param_setter;
	private Exploit exploit;
	protected TweakRecord tweak_record;
	private PktLenManager pkt_len_manager;	//reserved for later use
	private BrigandGenerable brigand_generator;
	private GaussianBanditHistoryTracker gauss_bandit_history_tracker;
	private GaussBanditParamManager bandit_param_manager;
	
	protected static ArrayList<TweakRecord> tweak_log;
	
	public final static int INITIATE_EXPERIMENTS=999;
//	public final static int SET_SCHEME_REQUEST_COMPLETED=1000;
	public final static int TRIGGER_TEST_PKT_TRAINS=1001;
	public static final int UPDATE_RECENT_STATS=1002;
	
	protected ExperimentalBandits(SmartBoy _smart_boy, ParamSetter _param_setter, Exploit _exploit, Logger _log){
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
		resetLocalExperiment();
		log.fine("ExperimentalBandits is loaded");
	}
	protected void handleMessage(FAPIMessage msg){
		if (msg.getAsInteger("ExperimentalBanditCommands", -1)==INITIATE_EXPERIMENTS) {
			log.fine("turning on ENABLE_CALLBACK on experimentalBandits");
			ENABLE_CALLBACK=true;
			log.fine("brigand 1 ======== "+StrRepr.strRepr(brigands[1]));
			bandit_param_manager.printBanditParamStatus();
			runExperiment(INITIATE_EXPERIMENTS);
		}
	}
	public void runExperiment(int msgType){
		switch (msgType) {
		case INITIATE_EXPERIMENTS:
			//log.fine("in runExperiment : INITIATE_EXPERIMENTS");
			banditCurrent=bandit_param_manager.getBestBanditID();
			param_setter.currentSetSchemeParam=brigands[banditCurrent];
			param_setter.setSchemeRequest(param_setter.currentSetSchemeParam, true);
			log.fine("banditCurrent = "+banditCurrent+", bandit= "+StrRepr.strRepr(brigands[banditCurrent])+", index = "+bandit_param_manager.getBestBanditNormalizedIndex()+" playCount = "+playCount);				
			break;
		case TRIGGER_TEST_PKT_TRAINS:
			Exploit.ENABLE=1;
			log.fine("set exploit.ENABLE = 1");
			log.fine("about to call sendTestPacketTrain()");
			exploit.sendTestPacketTrain(ParamSetter.ACTIVE_SCHEME, 1, true);
			break;
		case UPDATE_RECENT_STATS:
			//log.fine("in runExperiment : UPDATE_RECENT_STATS");
			playCount++;
			LOCAL_EXPERIMENT_COMPLETED=HasBanditSettled.check(gauss_bandit_history_tracker.getLocalPlayHistory(), BANDIT_SETTLE_FACTOR, banditCurrent)
					||gauss_bandit_history_tracker.getLocalPlayHistory().size()>bandit_flutter_factor;
			bandit_param_manager.updateBanditParams(brigands, banditCurrent);
			gauss_bandit_history_tracker.updateHistory(brigands, banditCurrent, bandit_param_manager.getBanditParams(StrRepr.strRepr(brigands[banditCurrent])));
//			bandit_param_manager.printBanditParamStatus();
			gauss_bandit_history_tracker.printLatestHistory();
			if (!EXPERIMENT_COMPLETED && !LOCAL_EXPERIMENT_COMPLETED) {
				log.fine("calling runExperiment(INITIATE_EXPERIMENTS) ");
				runExperiment(INITIATE_EXPERIMENTS);				
			}else if(!EXPERIMENT_COMPLETED && LOCAL_EXPERIMENT_COMPLETED){
				log.fine("terminal bandit = "+bandit2str(brigands, banditCurrent)); 
				log.fine("end of local experiment! starting a new one now");
				if (brigand_generator.getTunerState()!=STEADY_STATE) {
//					log.fine("brignads[] list before generating a new set: size = "+brigands.length);
//					logBrigands(brigands);
//					log.fine("local_play_history : ");
//					log.fine("why can't i print local_play_history? trying again:");
//					for(LocalPlayRecord _play_rec : gauss_bandit_history_tracker.getLatestLocalPlayHistory()){
//						log.fine(""+_play_rec.toString());
//					}
					brigands = brigand_generator.genBrigandSetAlternate(brigands[banditCurrent], brigands, gauss_bandit_history_tracker.getLatestNonEmptyLocalPlayHistory());
					//brigands=pkt_len_manager.adjustPktLen(brigands);
//					log.fine("brignads[] list after generating a new set: size = "+brigands.length);
//					logBrigands(brigands);
					resetLocalExperiment();
					bandit_param_manager.printBanditParamStatus();
				}
				runExperiment(INITIATE_EXPERIMENTS);
				break;
			}else if(EXPERIMENT_COMPLETED){
				log.fine("end of experiment. printing grandPlayHistory");
				gauss_bandit_history_tracker.printGrandPlayHistory();
				break;
			}
			break;
		default:
			break;
		}
	}
	protected void resetLocalExperiment(){
		log.fine("resetting local experiment");
		EXPERIMENT_COMPLETED=false;
		LOCAL_EXPERIMENT_COMPLETED=false;
		playCount=0;
		bandit_param_manager.resetBanditParams(brigands, false);
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














