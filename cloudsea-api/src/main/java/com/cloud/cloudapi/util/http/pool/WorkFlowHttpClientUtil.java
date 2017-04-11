package com.cloud.cloudapi.util.http.pool;

import java.io.IOException;
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

import com.cloud.cloudapi.util.ResponseConstant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 
* @author  wangw
* @create  2016年6月13日 下午1:31:33 
* @function workflow专用的httpclient
*/
@Component
@DependsOn("commonPoolingHttpManager")
public class WorkFlowHttpClientUtil extends RestHttpClientUtil {
	
	private Logger log = LogManager.getLogger(CommonPoolingHttpManager.httpLogName);

/**
 * post请求
 * @param url
 * @param headers
 * @param jsonbody
 * @return
 * @throws IOException 
 * @throws JsonProcessingException 
 */
	
public Map<String,String>  httpDoPost(String url,String username, String password, String jsonBody) throws JsonProcessingException, IOException{
	
	    log.debug("--------> WorkFlow http 请求: POST url:"+url+" ,username:"+username+" <--------");
	    
		CloseableHttpResponse rs = null;
		Map<String,String>   rsmap=null;
	    rs=post(url,username,password, jsonBody);
		
		if (rs == null) {
			
			log.warn("Respone is null.");
			
		} else {
			rsmap =new HashMap<String,String>();
			rsmap.put(ResponseConstant.HTTPCODE, String.valueOf(rs.getStatusLine().getStatusCode()));
			
			try {
				ObjectMapper mapper = new ObjectMapper();
				String resData = EntityUtils.toString(rs.getEntity(),"utf-8");
				JsonNode node = mapper.readTree(resData);
				rsmap.put(ResponseConstant.JSONBODY, node.toString());
				log.debug("Response - > { httpCode:"+rs.getStatusLine().getStatusCode()+", body:"+node.toString()+" }");
			} catch (ParseException e) {
				log.error("从Response中解析Json数据出错 !");
				rsmap.put(ResponseConstant.JSONBODY, "");
			}
		}		
		return rsmap;
		
	}

/**
 * http GET请求
 * @param url
 * @param username
 * @param password
 * @return
 * @throws IOException 
 * @throws ClientProtocolException 
 */
	
public Map<String,String>  httpDoGet(String url,String username, String password) throws IOException {
	
	log.debug("--------> WorkFlow http 请求: GET url:"+url+" ,username:"+username+" <--------");
	CloseableHttpResponse rs = null;
	Map<String, String> rsmap = null;
    rs = get(url, username, password);
	if (rs == null) {
		log.warn("Respone is null.");

	} else {
		rsmap = new HashMap<String, String>();
		rsmap.put(ResponseConstant.HTTPCODE, String.valueOf(rs.getStatusLine().getStatusCode()));

		try {
			Header[] rsheaders = rs.getAllHeaders();
			for (Header one : rsheaders) {
				rsmap.put(one.getName(), one.getValue());
			}

			if (rs.getEntity() != null) {
				ObjectMapper mapper = new ObjectMapper();
				String resData = EntityUtils.toString(rs.getEntity(),"utf-8");
				if(null != resData && !resData.isEmpty()){
					JsonNode node = mapper.readTree(resData);
					rsmap.put(ResponseConstant.JSONBODY, node.toString());
					log.debug("Response - > { httpCode:"+rs.getStatusLine().getStatusCode()+", body:"+node.toString()+" }");
				}
			} else {
				rsmap.put(ResponseConstant.JSONBODY, "");
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			log.error("从Response中解析Json数据出错 !");
			rsmap.put(ResponseConstant.JSONBODY, "");
		}
	}
	return rsmap;
}

}
