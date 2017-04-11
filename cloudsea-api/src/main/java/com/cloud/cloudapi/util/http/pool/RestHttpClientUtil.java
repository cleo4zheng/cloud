package com.cloud.cloudapi.util.http.pool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import sun.misc.BASE64Encoder;

/** 
* @author  wangw 
* @create  2016年5月25日 下午3:07:34 
* 
*/


public class RestHttpClientUtil {
	@Autowired
	IPoolingHttpManager commonPoolingHttpManager;
	
	private Logger log = LogManager.getLogger(CommonPoolingHttpManager.httpLogName);
	
	
		
		public static final String HTTP_POST="POST";
		public static final String HTTP_GET="GET";
		public static final String HTTP_PUT="PUT";
		public static final String HTTP_DELETE="DELETE";
		public static final String HTTP_HEAD="HEAD";
		public static final String HTTP_TTACE="TRACE";
		public static final String HTTP_PATCH="PATCH";
		
	private CloseableHttpResponse sendRequest(HttpClient client, String url, String method, Map<String, String> headers,
			String jsonbody) throws ClientProtocolException, IOException {
		
		log.info("执行http请求开始: request - > { method:"+method+" ,url:"+url+" }");
		long startTime = System.currentTimeMillis();
		
		CloseableHttpResponse response = null;
		if(!headers.containsKey("Accept")){
			headers.put("Accept", "application/json;charset=UTF-8");
		}
        try{
        	if (HTTP_PUT.equals(method) || HTTP_POST.equals(method)) {

    			response = put_post_patch(client, url, method, headers, jsonbody);
    		} else {
    			response = delete_get_trace(client, url, method, headers, jsonbody);
    		}
        }catch (Exception e){
        	log.error("执行http请求失败:" +e.getMessage(),e);
        	throw e;
        }
        long endTime = System.currentTimeMillis();
		log.info("执行http请求结束 -> 耗时:"+(endTime-startTime) +"ms");
		return response;
	}

	private CloseableHttpResponse put_post_patch(HttpClient httpclient, String url, String method,
			Map<String, String> headers, String jsonbody) throws ClientProtocolException, IOException {

		CloseableHttpResponse response = null;
		HttpEntityEnclosingRequestBase httpmethod = null;
		if (HTTP_PUT.equals(method)) {
			httpmethod = new HttpPut(url);

		} else if (HTTP_POST.equals(method)) {
			httpmethod = new HttpPost(url);
		} else {
			httpmethod = new HttpPatch(url);
		}

		for (Map.Entry<String, String> entry : headers.entrySet()) {
			httpmethod.setHeader(entry.getKey(), entry.getValue());
		}

		StringEntity entity = new StringEntity(jsonbody, "utf-8");// 解决中文乱码问题
        if(!headers.containsKey("Content-Type")){
        	entity.setContentType("application/json");
		}
		entity.setContentEncoding("UTF-8");
		httpmethod.setEntity(entity);

		Header[] rqheaders = httpmethod.getAllHeaders();
		response = (CloseableHttpResponse) httpclient.execute(httpmethod);

		// ================输出curl命令开始====================
		createCURL(url, method, rqheaders, jsonbody);
		// ================输出curl命令结束====================
		return response;
	}
		
		
		private CloseableHttpResponse delete_get_trace(HttpClient httpclient,String url,String method,Map<String,String> headers,String jsonbody) throws ClientProtocolException, IOException {  
		
		CloseableHttpResponse response = null;
		HttpRequestBase httpmethod = null;

		if (HTTP_GET.equals(method)) {
			httpmethod = new HttpGet(url);
		} else if (HTTP_DELETE.equals(method)) {
			httpmethod = new HttpDelete(url);
		} else {
			httpmethod = new HttpTrace(url);
		}

		for (Map.Entry<String, String> entry : headers.entrySet()) {

			httpmethod.setHeader(entry.getKey(), entry.getValue());
		}
			  

		Header[] rqheaders = httpmethod.getAllHeaders();
		response = (CloseableHttpResponse) httpclient.execute(httpmethod);

		Header[] rsheaders = response.getAllHeaders();
		
		int statusCode = response.getStatusLine().getStatusCode();
		// ================输出curl命令开始====================
		createCURL(url, method, rqheaders, jsonbody);
		// ================输出curl命令结束====================
	    return 	response;			
				
		}
		
		public CloseableHttpResponse post(String url,Map<String,String> headers,String jsonbody) throws ClientProtocolException, IOException{
			
			return sendRequest(commonPoolingHttpManager.getClient(),url,HTTP_POST,headers,jsonbody);
		}
		
		public CloseableHttpResponse get(String url,Map<String,String> headers,String jsonbody) throws ClientProtocolException, IOException{
			
			return sendRequest(commonPoolingHttpManager.getClient(),url,HTTP_GET,headers,jsonbody);
		}
		
		public CloseableHttpResponse put(String url,Map<String,String> headers,String jsonbody) throws ClientProtocolException, IOException{
			
			return sendRequest(commonPoolingHttpManager.getClient(),url,HTTP_PUT,headers,jsonbody);
		}
		
		public CloseableHttpResponse delete(String url,Map<String,String> headers,String jsonbody) throws ClientProtocolException, IOException{
			
			return sendRequest(commonPoolingHttpManager.getClient(),url,HTTP_DELETE,headers,jsonbody);
		}
		
		public CloseableHttpResponse patch(String url,Map<String,String> headers,String jsonbody) throws ClientProtocolException, IOException{
			
			return sendRequest(commonPoolingHttpManager.getClient(),url,HTTP_PATCH,headers,jsonbody);
		}
		
		public CloseableHttpResponse trace(String url,Map<String,String> headers,String jsonbody) throws ClientProtocolException, IOException{
			
			return sendRequest(commonPoolingHttpManager.getClient(),url,HTTP_TTACE,headers,jsonbody);
		}
		
		
		/**
		 * 带有用户名密码的POST请求
		 * @param url
		 * @param jsonbody
		 * @return
		 * @throws ClientProtocolException
		 * @throws IOException
		 */
       public CloseableHttpResponse post(String url,String username, String password,String jsonBody) throws ClientProtocolException, IOException{
    	   
			//URL connectionURL = new URL(url);
			//HttpClient client = commonPoolingHttpManager.getClientWithCredential(connectionURL.getHost(),connectionURL.getPort(),username,password);
			Map header =  new HashMap();
			String auth = new BASE64Encoder().encode((username+":"+password).getBytes()); 
			header.put("Authorization","Basic "+auth);
			return sendRequest(commonPoolingHttpManager.getClient(),url,HTTP_POST,header,jsonBody);
		}
       /**
        * 带有用户名密码的GET请求
        * @param url
        * @return
        * @throws ClientProtocolException
        * @throws IOException
        */
       public CloseableHttpResponse get(String url,String username, String password) throws ClientProtocolException, IOException{
    	  // URL connectionURL = new URL(url);
    	   //HttpClient client = commonPoolingHttpManager.getClientWithCredential(connectionURL.getHost(),connectionURL.getPort(),username,password);
    	   Map header =  new HashMap();
			String auth = new BASE64Encoder().encode((username+":"+password).getBytes()); 
			header.put("Authorization","Basic "+auth);
    	   return sendRequest(commonPoolingHttpManager.getClient(),url,HTTP_GET,header,null);
       }
       
       /**
        * 带有用户名密码的GET请求
        * 自定义header
        * @param url
        * @return
        * @throws ClientProtocolException
        * @throws IOException
        */
       public CloseableHttpResponse get(String url,String username, String password, HashMap header) throws ClientProtocolException, IOException{
    	  // URL connectionURL = new URL(url);
    	   //HttpClient client = commonPoolingHttpManager.getClientWithCredential(connectionURL.getHost(),connectionURL.getPort(),username,password);
    	   return sendRequest(commonPoolingHttpManager.getClient(),url,HTTP_GET,header,null);
       }
       
       /**
        * 根据参数构造CURL 命令，提供DEBUG使用
        * @param url
        * @param method
        * @param rqheaders
        * @param jsonBody
        */
       private void createCURL(String url,String method,Header[] rqheaders,String jsonBody){
    	   StringBuilder debugMessage = new StringBuilder();
			debugMessage.append("curl -i -X ").append(method).append(" ").append(url);
			for (Header one : rqheaders) {
				debugMessage.append(" -H ").append("\"").append(one.getName()).append(":").append(one.getValue()).append("\" ");
			}
			if (jsonBody != null)
			    debugMessage.append("-d ").append("\'").append(jsonBody).append("\'");
			log.debug(debugMessage);
       }
		
	}


