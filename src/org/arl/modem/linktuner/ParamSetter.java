package org.arl.modem.linktuner;

import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.util.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;

import org.arl.modem.FAPIMessage;
import org.arl.modem.phy.Physical;
import org.omg.PortableInterceptor.INACTIVE;

public class ParamSetter{
	
	protected static SmartBoy smartboy;
	protected Logger log;
	private ExperimentalBandits experimental_bandits;
	private BERtester BER_tester;
	
	protected  AID notify;	
	//Anyone who wants to subscribe to the dumb-tuner notifications can use this.
	public final static String NOTIFICATIONS = "ParamSetter-ntf";
	//List of parameters that can be tuned
	protected final static int MTYPE=0;
	protected final static int DMODE=263;
	protected final static int MPSK=261;
	protected final static int Nc=256;
	protected final static int Np=257;
	protected final static int Nz=259;
	protected final static int PKTLEN=4;
	protected final static int FEC=2;
	protected final static int ENABLE=1;
	protected final static int TX_ATT=4;
	
	//used for calculating instantaneous data rates
	public final static int PKT_DURATION=14;
	
	//ensure that the order of entries matches those in Scheme_Params_map
	protected final static int Scheme_Params[]={MTYPE,DMODE,MPSK,Nc,Np,Nz,PKTLEN,FEC,TX_ATT};
	public static LinkedHashMap<String, Integer> Scheme_Params_map;
	public static LinkedList<ArrayList<Integer>> Scheme_Params_values_list;
	public static List<String> Scheme_Params_map_keylist;
	public static Map<Integer, Double> FEC_mask_to_coderate_mapping;
	public static List<Notifiable<LinktunerNotifications>> Subscriber_List=new ArrayList<Notifiable<LinktunerNotifications>>();
	//To include further parameters, append them as a new row. Do not disturb existing order of parameters
	private final static int Scheme_Params_Values[][]=  {{1,2},								//MTYPE:1=incoherent,2=coherent		
														{1,2},								//DMODE:1=time,2=freq
														{2,4},								//MPSK:2=BPSK,4=QPSK 											
														{64,128,256,512,1024},				//Nc
														{0,8,16,32,64,128,256,512,1024},    //Np --->Only indicative. Actual Np values are many more.
														{0},								//Nz
														{116, 	234,	72,		96,		120,//most frequently used pktlen: 432
															
														144,	168,	192,	216,	240,
														264,	288,	312,	336,	360,
														384,	408,	432,	456,	480,
														504,	528,	552,	576,	600,
														624,	648,	672,	696,	720,
														744,	768,	792,	816,	840},//PKTLEN
														{1024,1024},                //FEC:0=none, 1024=conv(rate=1/3) only, 65536=golay only(rate =1/2), 66560=both.
														{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,
														15,16,17,18,19,20,21,22,23,24,25,26,
														27,28,29,30,31,32,33,34,35,36}		//TX-ATT: 0=full power, 50=damn low power. If operating at sea, 
																		  					//you must ensure the power level ranges are appropriate
														};
//0,2,4,6,8,10,12,14,16,
	protected final static int ACTIVE_SCHEME=2;
	
	public final static Integer MODEM_FAMILY=0;
	public static Integer POWER_LVL_CONTROL=18;
	public static int POWER_LEVEL_TEST=18;
	public static String mode="experimentalbandits";
	
	public final static int TYPE_SET_SCHEME_PARAM_M=0xFF;
	public final static int TYPE_SET_SCHEME_PARAM_M_headerLen=1;
	public final static int TYPE_SEND_SCHEME_PARAM_ACK_S=0xFE;
	public final static int PARAM_SETTER_PROTOCOL_ID = 4;
	
	public final static String LINKTUNER_NTF_ID="ParamSetter_Ntf";
	public final static String SET_SCHEME_PARAM_DONE_NTF="set_scheme_param_done_ntf";
	
	public int currentSetSchemeParam[]={1,1,1,4,6,0,1,3,0};
	
	private resendControlPacketBehaviour resendSetSchemeRequest;
	public int resendDelay=5000;
	public int resendMaxTries=4;

	protected ParamSetter(SmartBoy smartboy, Logger log){
		ParamSetter.smartboy=smartboy;
		this.log=log;
		String pwd = System.getProperty("user.dir");
		In in = new In(pwd+"/linktuner.config");
		while(!in.isEmpty()){
			String s = in.readLine();
			String param=s.split("\\s+")[0];
			if(param.equals("POWER_LVL_CONTROL")){
				POWER_LVL_CONTROL=Integer.parseInt(s.split("\\s+")[1]);
				log.fine("set POWER_LVL_CONTROL = "+POWER_LVL_CONTROL);
			}else if(param.equals("mode")){
				mode = new String(s.split("\\s+")[1]);
			}
		}
		resetSchemeParams(mode);
		log.fine("Inside ParamSetter constructor");
	}
	protected void handleMessage(FAPIMessage msg) {
		switch (msg.getAsInteger("ParamSetterCommands",-1)){
			case SmartBoyCommands.S_INIT_COMMAND:
				setSchemeRequest(currentSetSchemeParam,true);
				break;
			case TYPE_SET_SCHEME_PARAM_M:
				log.fine("calling logSchemeParamValues(1)");
				logSchemeParamValues(1);
				setSchemeParam(msg, 1); //call with second argument=1 if you want to return an ack
				break;
			case TYPE_SEND_SCHEME_PARAM_ACK_S:
				logSchemeParamValues(1);
				processSendSchemeParamAck(msg);
				break;
		}
	}
	//Master sends out a request to the slave to set some scheme params 
	protected boolean setSchemeRequest(int[] currentSetSchemeParam, boolean invokeResendBehaviour){
		log.fine("in setShemeRequest() method");
		if(!ParamSetter.setPowerLevelToControl()){
			log.warning("unable to set power level to control levels. TX-ATT = "+ParamSetter.getPhyParam(ParamSetter.MODEM_FAMILY, ParamSetter.TX_ATT));
		}
		byte[] setSchemeParam=new byte[currentSetSchemeParam.length];
		for(int i=0;i<currentSetSchemeParam.length;i++){
			setSchemeParam[i]=(byte)currentSetSchemeParam[i];
		}
		FAPIMessage req=smartboy.createMessage(FAPIMessage.REQUEST,smartboy.link);
		req.add("protocol",PARAM_SETTER_PROTOCOL_ID);
		byte[] rScheme=new byte[(TYPE_SET_SCHEME_PARAM_M_headerLen+setSchemeParam.length)];
		rScheme[0]=(byte)TYPE_SET_SCHEME_PARAM_M;
		System.arraycopy(setSchemeParam, 0, rScheme, TYPE_SET_SCHEME_PARAM_M_headerLen, setSchemeParam.length);
		req.setData(rScheme);
		log.fine("req.getData().length = "+req.getData().length);
		FAPIMessage reply=smartboy.request(req, 2000);
//		if(reply==null){
//			System.out.println("reply ==  null");
//		}
		if(reply != null && (reply.getPerformative() == FAPIMessage.AGREE || reply.getPerformative() == FAPIMessage.CONFIRM)){
			log.fine("master has sent a request to set scheme parameters:"+StrRepr.strRepr(setSchemeParam));			
			if(invokeResendBehaviour){
				log.fine("about to instantiate resend behaviour");
				resendSetSchemeRequest=new resendControlPacketBehaviour(resendDelay, resendMaxTries, System.currentTimeMillis(), TYPE_SET_SCHEME_PARAM_M);
				smartboy.addBehaviour(resendSetSchemeRequest);
			}			
			return true;
		}
		else {
			log.warning("not successful in sending out set scheme parameter request");
			diagnose(reply);
			return false;
		}
	}
	//Sets ownself's physical layer params. Use value==1 upon receiving the setSchemeRequest to send back an ack. Use any other value
	//if you are the master and have received an ack, and just want to set your own params.   
	private void setSchemeParam(FAPIMessage msg, int value){
			log.fine("msg.getData().length = "+ msg.getData().length);
			byte[] valueArrayReceived=new byte[currentSetSchemeParam.length];
			System.arraycopy(msg.getData(), TYPE_SET_SCHEME_PARAM_M_headerLen, valueArrayReceived, 0, valueArrayReceived.length);
			log.fine("valueArrayReceived = "+StrRepr.strRepr(valueArrayReceived)+", size = "+valueArrayReceived.length);
			boolean setSchemeParamOperation=true;
			int n=0;
			FAPIMessage reply;
//			log.fine("DEBUG: Np value = "+Scheme_Params_values_list.get(4).get(valueArrayReceived[4]));
			while(n<valueArrayReceived.length){
				if (n == Scheme_Params_map_keylist.indexOf("TX_ATT")) {
					log.fine("adjusting TX_ATT!");
					POWER_LEVEL_TEST=Scheme_Params_values_list.get(n).get(valueArrayReceived[n]);
					reply=setSingleSchemeParam(Scheme_Params[n], POWER_LEVEL_TEST, 0); //TX-ATT is a modem param
				}else {
//					log.fine("param = "+Scheme_Params[n]+", value = "+Scheme_Params_values_list.get(n).get(valueArrayReceived[n])+", n = "+n);
					reply=setSingleSchemeParam(Scheme_Params[n], Scheme_Params_values_list.get(n).get(valueArrayReceived[n]), ACTIVE_SCHEME);
				}
				if(reply!=null && reply.getPerformative()==FAPIMessage.INFORM){
					log.fine("set ownself's parameter "+reply.getAsInteger("parameter", -1)+" to value = "+reply.getAsInteger("value", -1));
				}
				else{
					setSchemeParamOperation=false;
					log.warning("failed to set scheme paramter");
				}
				n++;
			}
			//After changing MTYPE, u need to enable the scheme. We do and enable=1 anyways.
			if (setPhyParam(ACTIVE_SCHEME, ParamSetter.ENABLE, 1)) {
				log.fine("set ownself's parameter "+ParamSetter.ENABLE+" to value = "+getPhyParam(ACTIVE_SCHEME, ParamSetter.ENABLE));
			}else {
				setSchemeParamOperation=false;
				log.warning("failed to enable active scheme. something's wrong!");
				throw new RuntimeException("failed to enable active scheme. something's wrong!");
			}
			//proceed to set packet sizes to be equal to the max available :p
			//int maxPktLen=getPhyParam(ACTIVE_SCHEME, 13);
			//setSchemeParamOperation=setPhyParam(ACTIVE_SCHEME, 4, maxPktLen);
			//log.fine("set ownself's parameter "+13+" to value = "+maxPktLen);
			
		log.fine("just asked the phy layer to set params");
		log.fine("setSchemeParamOperation = "+setSchemeParamOperation);
		log.fine("value = "+value);
		if(setSchemeParamOperation==true && value!=1){     //indicates that the master has successfully completed scheme changes
			for (Notifiable<LinktunerNotifications> subscribers: Subscriber_List) {
				subscribers.handleLinktunerNotifications(new LinktunerNotifications(LINKTUNER_NTF_ID, SET_SCHEME_PARAM_DONE_NTF));
			}
		}
		if (setSchemeParamOperation==false){
	    	log.warning("----seem unable to set phy params :(");
	    }
		else{
	    	if(value==1){
	    		log.fine("slave is now going to return ack");
	    		sendSchemeParamAck(msg); //send back an ack iff value==1
	    	}
		}
	}
	//sets the value for a single parameter
	private FAPIMessage setSingleSchemeParam(int param, int value, int family){
		if (family == 0) {
			log.fine("attempting to set TX_ATT!!! param = "+param+" value = "+value);
		}
		log.fine("inside the setSingleSchemeParam method");
		FAPIMessage req=smartboy.createMessage(FAPIMessage.REQUEST, smartboy.phy, Physical.PARAM_SET);
		req.setContent(Physical.PARAM_SET);
		req.add("family", family);
		req.add("parameter", param);
		req.add("value",value);
		FAPIMessage reply=smartboy.request(req,2000);
		return reply;
	}
	private void sendSchemeParamAck(FAPIMessage msg){
		log.fine("inside the sendSchemeParamAck method");
		if(!ParamSetter.setPowerLevelToControl()){
			log.warning("unable to set power level to control levels. TX-ATT = "+ParamSetter.getPhyParam(ParamSetter.MODEM_FAMILY, ParamSetter.TX_ATT));
		}
		FAPIMessage req=smartboy.createMessage(FAPIMessage.REQUEST,smartboy.link);
		req.add("protocol",PARAM_SETTER_PROTOCOL_ID);
		byte[] rScheme=msg.getData();
		rScheme[0]=(byte)TYPE_SEND_SCHEME_PARAM_ACK_S;
		req.setData(rScheme);
		FAPIMessage reply=smartboy.request(req, 2000);
		if(reply != null && reply.getPerformative() == FAPIMessage.AGREE){
			log.fine("slave has sent an ack back to the master");
			//return true;
		}
		else {
			log.warning("slave is not successful in sending out set scheme parameter ack");
			diagnose(reply);
			//return false;
		}
	}
	private void processSendSchemeParamAck(FAPIMessage msg){
		byte[] valueArray=new byte[currentSetSchemeParam.length];
		System.arraycopy(msg.getData(), TYPE_SET_SCHEME_PARAM_M_headerLen, valueArray, 0, valueArray.length);
		byte[] currentSetSchemeParamByteArray=new byte[currentSetSchemeParam.length];
		for(int i=0;i<currentSetSchemeParam.length;i++){
			currentSetSchemeParamByteArray[i]=(byte)currentSetSchemeParam[i];
		}
		if(Arrays.equals(currentSetSchemeParamByteArray, valueArray)){
			smartboy.removeBehaviour(resendSetSchemeRequest);
			log.fine("received ack corresponding to current set scheme param request");
			setSchemeParam(msg, 0);  //remember to pass the second argument as zero if you are the master and want to send a test pkt train after setting your params. 
		}
		else{
			log.fine("recieved ack does not match current set scheme param request");
		}
	}
	public static int getPhyParam(int family, int param) {
	      FAPIMessage req = smartboy.createMessage(FAPIMessage.REQUEST, smartboy.phy,
	            Physical.PARAM_GET);
	      req.add("family", family);
	      req.add("parameter", param);
	      FAPIMessage reply = smartboy.request(req, 1000);
	      if (reply == null || reply.getPerformative() != FAPIMessage.INFORM){
	    	  return -1;  
	      }
	      return reply.getAsInteger("value",-1);
	   }
    public static boolean setPhyParam(int family, int param, int value) {
      FAPIMessage req = smartboy.createMessage(FAPIMessage.REQUEST, smartboy.phy,
            Physical.PARAM_SET);
      req.add("family", family);
      req.add("parameter", param);
      req.add("value", value);
      FAPIMessage reply = smartboy.request(req, 2000);
      if (reply == null || reply.getPerformative() != FAPIMessage.INFORM){
	         return false;
      }
      return true;
    }
	public static int getIndexOfParam(String param){
		return ParamSetter.Scheme_Params_map_keylist.indexOf(param);
	}
    //tries to resend the control packet after a delay of delay milliseconds and for upto maxTries number of tries.
	@SuppressWarnings("serial")
	private class resendControlPacketBehaviour extends SimpleBehaviour{
		private int _delay;
		private int _ctrlPktType;
		private int _maxTries;
		private long _timeInvocation;
		private int state;
		resendControlPacketBehaviour(int delay, int maxTries, long timeInvocation, int ctrlPktType){
			log.fine("resendControlPacketBehaviour constructor invoked!");
			this._delay=delay;
			this._ctrlPktType=ctrlPktType;
			this._maxTries=maxTries;
			this._timeInvocation=timeInvocation;
			this.state=0;
		}
		public void action() {
			long t=System.currentTimeMillis()-_timeInvocation;
			if(t>=_delay*(state+1)){
				if(state==0){
					log.fine("state = 0, about to call block(), time diff = "+t);
					block(_delay);
				}else if(state>_maxTries){
					log.warning("Slave modem doesn't seem to be responding");
				}else if(state>0 && state<(_maxTries+1)){     
					log.fine("state = "+state+", about to enter switch() block");
					switch(_ctrlPktType){
						case TYPE_SET_SCHEME_PARAM_M:
							log.warning("Timer has been triggered. calling setSchemeRequest(), time diff = "+t);
							logSchemeParamValues(1);
							setSchemeRequest(currentSetSchemeParam, false);
							break;
					}
				}
				state++;
			}
		}
		public boolean done() {
			return (state>_maxTries);
		}
	}
	public void stackDump(Throwable ex) {
		  log.log(Level.WARNING, "ParamSetter Stack Dump:", ex);
		  ex.printStackTrace();
		  File file = new File("stackDump.txt");
			FileOutputStream fos;
			try {
				fos = new FileOutputStream(file);
				PrintStream ps = new PrintStream(fos);
				System.setErr(ps);
			   ex.printStackTrace(ps);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	private void diagnose(FAPIMessage reply){
		if(reply==null)log.fine("reply==null");
		if(reply.getPerformative()==FAPIMessage.FAILURE)log.fine("perf=failiure");
		if(reply.getPerformative()==FAPIMessage.NOT_UNDERSTOOD)log.fine("perf=not-understood");
		if(reply.getPerformative()==FAPIMessage.REFUSE)log.fine("perf=refuse");
		if(reply.getPerformative()==FAPIMessage.DISCONFIRM)log.fine("perf=disconfirm");
		log.fine("exiting diagnose");
	}
	public static void resetSchemeParams(String mode){
		Scheme_Params_map = new LinkedHashMap<String, Integer>();
		Scheme_Params_values_list = new LinkedList<ArrayList<Integer>>();
		//ensure that the order of additions to the map is the same as the order of entries in the array Scheme_Params[].
		Integer i;
		Scheme_Params_map.put("MTYPE", 0);
		Scheme_Params_map.put("DMODE", 263);
		Scheme_Params_map.put("MPSK", 261);
		Scheme_Params_map.put("Nc", 256);
		Scheme_Params_map.put("Np", 257);
		Scheme_Params_map.put("Nz", 259);
		Scheme_Params_map.put("PKTLEN", 4);
		Scheme_Params_map.put("FEC", 2);
		Scheme_Params_map.put("TX_ATT", 4);

		Scheme_Params_map_keylist = new ArrayList<String>(Scheme_Params_map.keySet());
		//generating Np values
		List<Integer> Np=new ArrayList<Integer>();
		if(mode.toLowerCase().equals("experimentalbandits")){
			i=0;
			while (i<=1024) {
				Np.add(i);
				i=i+8;
			}	
		}else if(mode.toLowerCase().equals("bertest")){
			Integer[] _Np_values_for_bertest={0,16,64,256,512,1024};
			Np.addAll(Arrays.asList(_Np_values_for_bertest));
		}else{
			throw new RuntimeException("unspecified mode");
		}
		
		for (int j = 0; j < Scheme_Params_Values.length; j++) {
			Scheme_Params_values_list.add((ArrayList<Integer>)convertToIntegerList(Scheme_Params_Values[j]));
		}
//		if (override_np) {
			//overriding the Np values with the ones generated above
			Scheme_Params_values_list.set(Scheme_Params_map_keylist.indexOf("Np"), (ArrayList<Integer>) Np);			
//		}
		//verifying that this map and the Scheme_Params array match
		if(Scheme_Params_map_keylist.size()!=Scheme_Params.length){
			throw new Error("sizes of Scheme_Params does not match Scheme_Params_map");
			//log.warning("sizes of Scheme_Params does not match Scheme_Params_map");
		}
		i=0;
		for(Map.Entry<String, Integer> _iter : Scheme_Params_map.entrySet()){
			if(_iter.getValue()!=Scheme_Params[i]){
				throw new Error("The array Scheme_Params does not match Scheme_Params_Map");
				//log.warning("The array Scheme_Params does not match Scheme_Params_Map");
			}
			i++;
		}
		FEC_mask_to_coderate_mapping = new LinkedHashMap<Integer, Double>();
		FEC_mask_to_coderate_mapping.put(0, 0.0);
		FEC_mask_to_coderate_mapping.put(1024, 0.33);
		FEC_mask_to_coderate_mapping.put(65536, 0.5);
		FEC_mask_to_coderate_mapping.put(66560, 0.15);
		
		//printing out the values list:
//		System.out.println("----------brace yourself to see THE LIST!!!!------------------");
//		System.out.println("Scheme_Params_values_list.size() after = "+Scheme_Params_values_list.size());
//		for (List<Integer> list_entry: Scheme_Params_values_list) {
//			System.out.println("List: "+StrRepr.strRepr(list_entry));
//		}
	}
	private static List<Integer> convertToIntegerList(int[] arr){
		List<Integer> _lst = new ArrayList<Integer>();
		for (int i = 0; i < arr.length; i++) {
			_lst.add(arr[i]);
		}
		return _lst;
	}
	public void assignExperimentalBandits(ExperimentalBandits _experimental_bandits){
		this.experimental_bandits = _experimental_bandits;
		Subscriber_List.add(experimental_bandits);
	}
	public void assignBERtester(BERtester _BER_tester ){
		this.BER_tester = _BER_tester;
		Subscriber_List.add(BER_tester);
	}
	private void logSchemeParamValues(int _family){
		int __family;
		String enable="ENABLE";
		String _str=new String("Scheme="+_family+", ");
		for(String _param_as_str : Scheme_Params_map_keylist){
			int _param_as_int = Scheme_Params_map.get(_param_as_str);
			if(_param_as_str=="TX_ATT"){
				__family=0;
			}else{
				__family=_family;
			}
			_str=_str.concat(_param_as_str+"="+getPhyParam(__family, _param_as_int)+", ");
		}
		_str=_str.concat(enable+"="+getPhyParam(_family, ENABLE));
		log.fine(""+_str);
	}
	public static Boolean setPowerLevelToControl(){
		return setPhyParam(MODEM_FAMILY, TX_ATT, POWER_LVL_CONTROL);
	}
	public static Boolean setPowerLevelToTest(){
		return setPhyParam(MODEM_FAMILY, TX_ATT, POWER_LEVEL_TEST);
	}
}
