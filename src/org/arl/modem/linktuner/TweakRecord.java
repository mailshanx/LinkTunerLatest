package org.arl.modem.linktuner;

import java.util.ArrayList;
import java.util.List;

public class TweakRecord {
	protected  String paramTweaked;
	protected  Integer upperValue;
	protected  Integer lowerValue;
	protected  Integer upperValueIndex;
	protected  Integer lowerValueIndex;
	protected  List<int[]> indexList;
	//protected  Boolean upperValueResult;//true indicates success
	//protected  Boolean lowerValueResult;
	//protected  Boolean localSuccess;
	//protected  Boolean continueTweaking;
	
	public TweakRecord(){
		this.paramTweaked=null;
		this.upperValue=null;
		this.lowerValue=null;
		this.indexList=null;
		this.upperValueIndex=null;
		this.lowerValueIndex=null;
		//this.upperValueResult=null;
		//this.lowerValueResult=null;
		//this.localSuccess=false;
		//this.continueTweaking=true;
	}
	public String toString(){
		return  "paramTweaked = "+paramTweaked+" upperValue = "+upperValue.toString()+" lowerValue = "
				+lowerValue.toString()+" upperValueIndex = "+upperValueIndex.toString()+" lowerValueIndex = "+lowerValueIndex.toString()
				+" indexList = "+indexList.toArray().toString();
						//+" continueTweaking = "+continueTweaking.toString();
	}
}
