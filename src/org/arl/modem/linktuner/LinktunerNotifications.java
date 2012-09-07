package org.arl.modem.linktuner;

public class LinktunerNotifications {
	private int testPktTrainCnt=0;
	private String Ntfsrc="";
	private String msg="";
	public LinktunerNotifications(String _ntf_src,  int _test_pkt_train_cnt){
		this.Ntfsrc=_ntf_src;
		this.testPktTrainCnt=_test_pkt_train_cnt;
	}
	public LinktunerNotifications(String _ntf_src, String _msg){
		this.Ntfsrc=_ntf_src;
		this.msg=_msg;
	}
	public String getNtfSrc(){
		return Ntfsrc;
	}
	public int getTestPktTrainCnt(){
		return testPktTrainCnt;
	}
	public String getMsg(){
		return msg;
	}
}
