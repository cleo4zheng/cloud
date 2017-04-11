package com.cloud.cloudapi.util.http.pool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 
* @author  wangw
* @create  2016年5月25日 下午4:04:04 
* 
*/

@Component
@DependsOn("commonPoolingHttpManager")
public class OSHttpClientUtil extends RestHttpClientUtil{
	
	private Logger log = LogManager.getLogger(CommonPoolingHttpManager.httpLogName);
	

	public Map<String,String>  httpDoPost(String url,Map<String,String> headers,String jsonbody){
		
		log.debug("--------> Openstack http 请求: POST url:"+url+" <--------");
		CloseableHttpResponse rs = null;
		Map<String,String>   rsmap=null;
		try {
			rs=post(url, headers, jsonbody);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			log.error(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		
		if (rs == null) {
			log.warn("Respone is null.");
			
		} else {
			rsmap =new HashMap<String,String>();
			rsmap.put(ResponseConstant.HTTPCODE, String.valueOf(rs.getStatusLine().getStatusCode()));
			Header[] header = rs.getHeaders(ResponseConstant.LOCATION); //for create private image
			if(null != header){
				for(int index = 0; index < header.length; ++index){
					rsmap.put(ResponseConstant.LOCATION, header[index].getValue());
				}
			}
			try {
				ObjectMapper mapper = new ObjectMapper();
				if(null != rs.getEntity()){
					String resData = EntityUtils.toString(rs.getEntity());
					if(Util.isNullOrEmptyValue(resData)){
						rsmap.put(ResponseConstant.JSONBODY, "");
					}else{
						JsonNode node = mapper.readTree(resData);
						rsmap.put(ResponseConstant.JSONBODY, node.toString());	
						log.debug("Response - > { httpCode:"+rs.getStatusLine().getStatusCode()+", body:"+node.toString()+" }");
					}
				}else{
					rsmap.put(ResponseConstant.JSONBODY, "");
				}
//				String resData = EntityUtils.toString(rs.getEntity());
//				JsonNode node = mapper.readTree(resData);
//				rsmap.put(ResponseConstant.JSONBODY, node.toString());
//				log.debug("Response - > { httpCode:"+rs.getStatusLine().getStatusCode()+", body:"+node.toString()+" }");
			} catch (ParseException | IOException e) {
				// TODO Auto-generated catch block
				log.error(e);
			}
		}		
		return rsmap;
		
	}
	
	public Map<String,String>  httpDoPost(String url,String tokenid,String jsonbody){
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
		
		return httpDoPost(url,headers,jsonbody);
	}
	
	public Map<String,String>  httpDoPut(String url,Map<String,String> headers,String jsonbody){
		log.debug("--------> Openstack http 请求: PUT url:"+url+" <--------");
		CloseableHttpResponse rs = null;
		Map<String,String>   rsmap=null;
		
		try {
			rs=put(url, headers, jsonbody);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			log.error(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		
		if (rs == null) {
			log.warn("Respone is null.");
			
		} else {
			rsmap =new HashMap<String,String>();
			rsmap.put(ResponseConstant.HTTPCODE, String.valueOf(rs.getStatusLine().getStatusCode()));
			
			try {
				ObjectMapper mapper = new ObjectMapper();
				if(null != rs.getEntity()){
					String resData = EntityUtils.toString(rs.getEntity());
					JsonNode node = mapper.readTree(resData);
					rsmap.put(ResponseConstant.JSONBODY, node.toString());
					log.debug("Response - > {httpCode:"+rs.getStatusLine().getStatusCode()+", body:"+node.toString());
				}else{
					rsmap.put(ResponseConstant.JSONBODY, "");
				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
			}
		}		
		return rsmap;
		
	}
	
	public Map<String,String>  httpDoPut(String url,String tokenid,String jsonbody){
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
		
		return httpDoPut(url,headers,jsonbody);
	}
	
	public Map<String,String>  httpDoPatch(String url,Map<String,String> headers,String jsonbody){
		
		log.debug("--------> Openstack http 请求: PATCH url:"+url+" <--------");
		CloseableHttpResponse rs = null;
		Map<String,String>   rsmap=null;
		try {
			rs=patch(url, headers, jsonbody);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			log.error(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		
		if (rs == null) {
			log.warn("Respone is null.");
			
		} else {
			rsmap =new HashMap<String,String>();
			rsmap.put(ResponseConstant.HTTPCODE, String.valueOf(rs.getStatusLine().getStatusCode()));
			
			try {
				ObjectMapper mapper = new ObjectMapper();
				if(null != rs.getEntity()){
					String resData = EntityUtils.toString(rs.getEntity());
					if(Util.isNullOrEmptyValue(resData)){
						rsmap.put(ResponseConstant.JSONBODY, "");
					}else{
						JsonNode node = mapper.readTree(resData);
						rsmap.put(ResponseConstant.JSONBODY, node.toString());	
						log.debug("Response - > { httpCode:"+rs.getStatusLine().getStatusCode()+", body:"+node.toString()+" }");
					}
				}else{
					rsmap.put(ResponseConstant.JSONBODY, "");
				}
//				String resData = EntityUtils.toString(rs.getEntity());
//				JsonNode node = mapper.readTree(resData);
//				rsmap.put(ResponseConstant.JSONBODY, node.toString());
//				log.debug("Response - > { httpCode:"+rs.getStatusLine().getStatusCode()+", body:"+node.toString()+" }");
			} catch (Exception e) {
				log.error(e);
			}
		}		
		return rsmap;
		
	}
	
	public Map<String,String>  httpDoPatch(String url,String tokenid,String jsonbody){
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
		
		return httpDoPatch(url,headers,jsonbody);
	}
	
	public Map<String,String>  httpDoDelete(String url,Map<String,String> headers){
		
		log.debug("--------> Openstack http 请求: DELETE url:"+url+" <--------");
		CloseableHttpResponse rs = null;
		Map<String,String>   rsmap=null;
		try {
			rs=delete(url, headers, null);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			log.error(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		
		if (rs == null) {
			log.warn("Respone is null.");
			
		} else {
			rsmap =new HashMap<String,String>();
			rsmap.put(ResponseConstant.HTTPCODE, String.valueOf(rs.getStatusLine().getStatusCode()));
			
			try {
				ObjectMapper mapper = new ObjectMapper();
				if(null != rs.getEntity()){
					String resData = EntityUtils.toString(rs.getEntity());
					if(Util.isNullOrEmptyValue(resData)){
						rsmap.put(ResponseConstant.JSONBODY, "");
					}else{
						JsonNode node = mapper.readTree(resData);
						rsmap.put(ResponseConstant.JSONBODY, node.toString());	
						log.debug("Response - > { httpCode:"+rs.getStatusLine().getStatusCode()+", body:"+node.toString()+" }");
					}
				}else{
					rsmap.put(ResponseConstant.JSONBODY, "");
				}
				
			} catch (ParseException | IOException e) {
				// TODO Auto-generated catch block
				log.error(e);
			}
		}		
		return rsmap;	
	}
	
	public Map<String,String>  httpDoDelete(String url,String tokenid){
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
		
		return httpDoDelete(url,headers);
		
	}
	
	public Map<String,String>  httpDoGet(String url,Map<String,String> headers){
		
		log.debug("--------> Openstack http 请求: GET url:"+url+" <--------");
		CloseableHttpResponse rs = null;
		Map<String,String>   rsmap=null;
		try {
			rs=get(url, headers, null);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		
		if (rs == null) {
			log.warn("Respone is null.");
			
		} else {
			rsmap =new HashMap<String,String>();
			rsmap.put(ResponseConstant.HTTPCODE, String.valueOf(rs.getStatusLine().getStatusCode()));
			
			try {
				ObjectMapper mapper = new ObjectMapper();
				if(null != rs.getEntity()){
					String resData = EntityUtils.toString(rs.getEntity());
					if(Util.isNullOrEmptyValue(resData)){
						rsmap.put(ResponseConstant.JSONBODY, "");
					}else{
						JsonNode node = mapper.readTree(resData);
						rsmap.put(ResponseConstant.JSONBODY, node.toString());	
						log.debug("Response - > { httpCode:"+rs.getStatusLine().getStatusCode()+", body:"+node.toString()+" }");
					}
				}else{
					rsmap.put(ResponseConstant.JSONBODY, "");
				}
//				String resData = EntityUtils.toString(rs.getEntity());
//				JsonNode node = mapper.readTree(resData);
//				rsmap.put(ResponseConstant.JSONBODY, node.toString());
//				log.debug("Response - > { httpCode:"+rs.getStatusLine().getStatusCode()+", body:"+node.toString()+" }");
			} catch (ParseException | IOException e) {
				// TODO Auto-generated catch block
				log.error(e);
			}
		}
		return rsmap;	
	}
	
	public Map<String,String>  httpDoGet(String url,String tokenid){
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
		
		return httpDoGet(url,headers);
	}
	
	
	public Map<String, String> httpDoGet(String url) {

		log.debug("--------> Openstack http 请求: GET url:" + url + " <--------");
		CloseableHttpResponse rs = null;
		Map<String, String> rsmap = null;
		Map<String, String> headers = new HashMap<String, String>();
		try {
			rs = get(url, headers, null);
			if (rs == null) {
				log.warn("Respone is null.");
			} else {
				InputStream is = rs.getEntity().getContent();
				StringBuffer sb = new StringBuffer();
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				String line = "";
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
				rsmap = new HashMap<String, String>();
				rsmap.put(ResponseConstant.HTTPCODE, String.valueOf(rs.getStatusLine().getStatusCode()));
				rsmap.put(ResponseConstant.JSONBODY, sb.toString());
			}
		} catch (Exception e) {
			log.error("error", e);
		}
		return rsmap;
	}
	
}
