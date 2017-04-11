package com.cloud.cloudapi.util.http;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class RequestUrlHelper {
	
	public static String createFullUrl(String baseurl, Map<String, String> paraMap) {

		if (paraMap == null || paraMap.size() < 1) {

			return baseurl;

		} else {
			StringBuffer sb = new StringBuffer();
			Iterator<Entry<String, String>> ir = paraMap.entrySet().iterator();
			sb.append(baseurl + "?");
			int i = 0;
			while (ir.hasNext()) {
				if (i != 0) {
					sb.append("&");
				}
				Entry<String, String> ie = ir.next();
				sb.append(ie.getKey());
				sb.append("=");
				sb.append(ie.getValue());
				i++;
			}

			return sb.toString();

		}
	}
	
	public static String checkUrlEnd(String url){	
		if(url.endsWith("/")) return url;
		return url+"/";
	}
    
	public static String urlPlus(String baseurl,String plusurl){		
         return checkUrlEnd(baseurl)+delUrlNoSeparator(plusurl);
	}
	
	private static String delUrlNoSeparator(String url){
		String [] arr=url.split("/");
		StringBuffer sb=new StringBuffer(5);
		for(int i=0;i<arr.length;i++){
			if(!arr[i].equals("")){
				sb.append(arr[i]);
				if(i!=arr.length-1){
				 sb.append("/");
				}
			}
		}
		return sb.toString();
	} 

}
