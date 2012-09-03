package org.arl.modem.linktuner;

public class LocalParamResults {
	public Integer paramValue;
	public Double paramSuccessCount;
	public Double paramCount;
	public Double successRatio;
	public Boolean Success;
	
	public LocalParamResults(){
		paramValue=null;
		paramSuccessCount=0.0;
		paramCount=0.0;
		successRatio=0.0;
		Success=null;
	}
	
	public String toString(){
		try {
			return "paramValue = "+paramValue+" paramSuccessCount = "+paramSuccessCount+" paramCount = "
					+paramCount+" successRatio = "+successRatio+" Success = "+Success;
		} catch (Exception e) {
			return "null value in LocalParamResults instance";
		}
	}
}
