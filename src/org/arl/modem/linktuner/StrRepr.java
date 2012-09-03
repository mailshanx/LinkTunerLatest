package org.arl.modem.linktuner;

import java.util.List;

public class StrRepr {
	public static String strRepr(int[] arr){
		String _str="";
		for (int i = 0; i < arr.length; i++) {
			_str=_str.concat(arr[i]+"");
		}
		return new String(_str);		
	}
	public static String strRepr(int[][] arr) {
		String _brigandStr="";
		for (int i = 0; i < arr.length; i++) {
			String _banditStr="";
			for (int j = 0; j < arr[i].length; j++) {
				_banditStr=_banditStr.concat(((Integer)arr[i][j]).toString()+"");
			}
			_banditStr=_banditStr.concat(" \t");		//'coz \n is not recognized by the logging framework
			_brigandStr=_brigandStr.concat(_banditStr);
		}
		return new String(_brigandStr);
	}
	
	public static String strRepr(byte[] arr){
		String _str="";
		for (int i = 0; i < arr.length; i++) {
			_str=_str.concat(arr[i]+"");
		}
		return new String(_str);		
	}
		
	public static <T> String strRepr(List<T> _list){
		String _str="";
		int i =0;
		for (T _element : _list) {
			if (_element instanceof List<?>) {
				List<?> _element_list = (List<?>)_element;
				String _element_str=new String("\t List "+(i++)+": ");		//'coz \n is not recognized by the logging framework
				for(Object __element : _element_list){
					_element_str=_element_str.concat(__element.toString()+" ");
				}
				_str=_str.concat(_element_str);	
			}else if (_element instanceof Integer) {
				_str=_str.concat(_element.toString()+" ");
			}
		}
		return new String(_str);		
	}
	
	public static void log(int[] arr){
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
}
