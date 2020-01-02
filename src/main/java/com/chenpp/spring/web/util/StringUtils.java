package com.chenpp.spring.web.util;

public class StringUtils {
	public static boolean isEmpty(String value){
		if(value==null || value.isEmpty()){
			return true;
		}
		return false;
	}
	
	public static String toFirstLowChar(String value){
		if(Character.isLowerCase(value.charAt(0))){
			return value;
		}
		return Character.toLowerCase(value.charAt(0))+value.substring(1);
			
	}
}
