package org.arl.modem.linktuner;

import jade.core.AID;
import jade.util.Logger;

import org.arl.modem.FAPIAgent;
import org.arl.modem.FAPIMessage;
import org.arl.modem.link.Link;
import org.arl.modem.phy.Physical;

public class SmartBoy extends FAPIAgent{
	protected  AID link;
	protected  AID phy;
	protected  AID notify;
	
	public final static String NOTIFICATIONS = "SmartBoy-ntf";
	
	public static int ENABLE=0;
	
	private ParamSetter param_setter;
	private Exploit ex;
	private ExperimentalBandits experimental_bandits;
	private BERtester BER_tester;
	
	public void setup() {
		super.setup();
		info("$Id: SmartBoy agent $");
		log.fine("inside SmartBoy.setup()");
		subscribeTopic(Link.NOTIFICATIONS);
		subscribeTopic(Physical.NOTIFICATIONS);
		link = new AID(getAgentProperty("lnk", "lnk"), AID.ISLOCALNAME);
	    phy = new AID(getAgentProperty("phy", "phy"), AID.ISLOCALNAME);
		notify = getTopic(NOTIFICATIONS);
		
		param_setter = new ParamSetter(this, (Logger) this.log);
		ex = new org.arl.modem.linktuner.Exploit(this, (Logger)this.log);
		log.fine("about to instantiate experimental_bandits-------->");
		experimental_bandits=new ExperimentalBandits(this, param_setter, ex, (Logger)this.log);
		log.fine("after instantiating experimental_bandits");
		BER_tester=new BERtester(param_setter, ex, (Logger)this.log);
		log.fine("after instantiating BER_tester");
		param_setter.assignExperimentalBandits(experimental_bandits);
		param_setter.assignBERtester(BER_tester);
		ex.assignExperimentalBandits(experimental_bandits);
		ex.assignBERtester(BER_tester);
	}
	protected void handleMessage(FAPIMessage msg){
		if (msg.getPerformative() == FAPIMessage.REQUEST) {
			FAPIMessage rsp = msg.createReply();
			rsp.setPerformative(FAPIMessage.AGREE);
			send(rsp);
		}
		FAPIMessage ntf=createMessage(FAPIMessage.INFORM,notify);
		param_setter.handleMessage(msg);
		ex.handleMessage(msg);
		experimental_bandits.handleMessage(msg);
		BER_tester.handleMessage(msg);
		send(ntf);
	}
}
