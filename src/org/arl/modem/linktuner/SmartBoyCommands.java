package org.arl.modem.linktuner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;

import jade.core.AID;
import jade.util.Logger;

import org.arl.modem.ATCommand;
import org.arl.modem.ATCommandInterpreter;
import org.arl.modem.CommandHandler;
import org.arl.modem.FAPIAgent;
import org.arl.modem.FAPIMessage;
import org.arl.modem.link.Link;

public class SmartBoyCommands extends CommandHandler{
	protected AID sb;
	private AID phy;
	private AID link;
	
	public final static int S_INIT_COMMAND=1;
	
	public final static int BEGIN_EX_COMMAND=2;
	public final static int ENABLE_COMMAND=3;
	
	public SmartBoyCommands(ATCommandInterpreter agent) {
		super("~S", agent);
		agent.subscribeTopic(Link.NOTIFICATIONS);
	    phy = new AID(agent.getAgentProperty("phy", "phy"), AID.ISLOCALNAME);
	    sb = new AID(agent.getAgentProperty("sb", "sb"), AID.ISLOCALNAME);
	    //the local variable link now holds the identifier info for the agent lnk. In case you have more that a single link layer 
	    //agent, you can associate a particular link layer agent by writing lt.lnk=lnk1 in the file modem.properties
		link = new AID(agent.getAgentProperty("lnk", "lnk"), AID.ISLOCALNAME);
	}
	
	protected boolean processNotification(FAPIMessage msg) {
		//note that msg.getAsInteger("protocol") will return an Integer class in the absence of a def value. Trying to compare a 
		//null integer class with something will cause a null pointer exception. Hence, always specify a def value of -1, since
		//you know that there is no protocol ID with a value of -1...
		if(msg.getPerformative()==FAPIMessage.INFORM && (msg.getAsInteger("protocol",-1)==ParamSetter.PARAM_SETTER_PROTOCOL_ID
				|| msg.getAsInteger("protocol",-1)==Exploit.EXPLOIT_PROTOCOL_ID)){
			FAPIMessage req=agent.createMessage(FAPIMessage.REQUEST, sb);
			switch(msg.getAsInteger("protocol", -1)){
				case ParamSetter.PARAM_SETTER_PROTOCOL_ID:
					req.add("ParamSetterCommands", identifyMsgType(msg));
					break;
				case Exploit.EXPLOIT_PROTOCOL_ID:
					req.add("ExploitCommands", identifyMsgType(msg));
					break;
			}
			req.setData(msg.getData());
			if(sendAndCheckAck(req)){
				log.fine("processed notification");
			}else{
				log.fine("failed to process notification");
			}
			return true;
		}
		return false;
	}

	public boolean INITz(ATCommand c){
		//ExperimentalBandits.runExperiment(ExperimentalBandits.INITIATE_EXPERIMENTS);
		FAPIMessage msg = agent.createMessage(FAPIMessage.REQUEST, sb);
		msg.add("ExperimentalBanditCommands", ExperimentalBandits.INITIATE_EXPERIMENTS);
		return sendAndCheckAck(msg);
	}	

	public boolean BERTESTz(ATCommand c){
		FAPIMessage msg = agent.createMessage(FAPIMessage.REQUEST, sb);
		System.out.println("sending msg to BERtester");
		msg.add("BERtesterCommands", BERtester.INITIATE_EXPERIMENTS);
		return sendAndCheckAck(msg);
	}	
	
	public boolean BEGINz(ATCommand c){
		FAPIMessage msg=agent.createMessage(FAPIMessage.REQUEST,sb);
		msg.add("ExploitCommands",BEGIN_EX_COMMAND);
		return sendAndCheckAck(msg);
	}
	public boolean ENABLEz(ATCommand c){
		FAPIMessage msg=agent.createMessage(FAPIMessage.REQUEST,sb);
		msg.add("ExploitCommands",ENABLE_COMMAND);
		return sendAndCheckAck(msg);
	}
	public boolean STOPz(ATCommand c){
		Exploit.ENABLE=0;
		return true;
	}
	public boolean STARTz(ATCommand c){
		Exploit.ENABLE=1;
		return true;
	}

	//identifies the message type. Returns -1 if found no match.
	private int identifyMsgType(FAPIMessage msg){
		byte[] data = msg.getData();
		switch(msg.getAsInteger("protocol", -1)){
			case ParamSetter.PARAM_SETTER_PROTOCOL_ID:
				switch ((byte)data[0]){
					case (byte)ParamSetter.TYPE_SET_SCHEME_PARAM_M:
						return ParamSetter.TYPE_SET_SCHEME_PARAM_M;
					case (byte)ParamSetter.TYPE_SEND_SCHEME_PARAM_ACK_S:
						return ParamSetter.TYPE_SEND_SCHEME_PARAM_ACK_S;
				}
			break;
			case Exploit.EXPLOIT_PROTOCOL_ID:
				switch((byte)data[0]){
					case (byte)Exploit.TYPE_QUERY_TEST_RESULTS_M:
						log.fine("received a test result query from master");
						return Exploit.TYPE_QUERY_TEST_RESULTS_M;
					case (byte)Exploit.TYPE_TEST_RESULT_QUERY_ACK_S:
						return Exploit.TYPE_TEST_RESULT_QUERY_ACK_S;
					case (byte)Exploit.TYPE_ENABLE_EXPLOITATION_SET_M:
						return Exploit.TYPE_ENABLE_EXPLOITATION_SET_M;
					case (byte)Exploit.TYPE_ENABLE_EXPLOITATION_SET_ACK_S:
						return Exploit.TYPE_ENABLE_EXPLOITATION_SET_ACK_S;
				}
			break;
		}
		return -1;
	}

	public void stackDump(Throwable ex) {
		  log.log(Level.WARNING, "SmartBoyCommands Stack Dump:", ex);
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
}
