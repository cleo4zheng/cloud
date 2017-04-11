package com.cloud.cloudapi.util.http;

import java.io.IOException;
import java.util.Map;

import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;

public class HttpClientForRest {
	
	public static final String HTTP_POST="POST";
	public static final String HTTP_GET="GET";
	public static final String HTTP_PUT="PUT";
	public static final String HTTP_DELETE="DELETE";
	public static final String HTTP_HEAD="HEAD";
	public static final String HTTP_TTACE="TRACE";
	public static final String HTTP_PATCH="PATCH";
	
	private CloseableHttpResponse sendRequest(String url,String method,Map<String,String> headers,String jsonbody) throws ClientProtocolException, IOException {  
		
		  CloseableHttpResponse response=null;
		  headers.put("Content-type","application/json; charset=utf-8");
		  headers.put("Accept", "application/json"); 
		  
		  if (HTTP_PUT.equals(method) ||HTTP_POST.equals(method)){
			  
			  response =put_post_patch(url,method,headers,jsonbody);
		  }else{
			  response =delete_get_trace(url,method,headers,jsonbody);
		  }

         return response;  
	}
	
	private CloseableHttpResponse put_post_patch(String url,String method,Map<String,String> headers,String jsonbody) throws ClientProtocolException, IOException {
		  
		  CloseableHttpClient httpclient = HttpClients.createDefault();
		  CloseableHttpResponse response=null;
		  HttpEntityEnclosingRequestBase httpmethod=null;
		  
		  if(HTTP_PUT.equals(method)){
			  httpmethod =new HttpPut(url);

		  }else if(HTTP_POST.equals(method)){
			  httpmethod =new HttpPost(url);
		  }else{
			  httpmethod =new HttpPatch(url);
		  }
		  
//		  httpmethod.addHeader("Content-type","application/json; charset=utf-8");  
//		  httpmethod.setHeader("Accept", "application/json");  
		  
		  for (Map.Entry<String, String> entry : headers.entrySet()) {  			  
//			  httpmethod.addHeader(entry.getKey(),entry.getValue());  
			  //@todo check
			  httpmethod.setHeader(entry.getKey(),entry.getValue());  
			}  
		  
          StringEntity entity = new StringEntity(jsonbody,"utf-8");//解决中文乱码问题  
          entity.setContentEncoding("UTF-8");    
          entity.setContentType("application/json");    
		  httpmethod.setEntity(entity); 
		  
		try {

			long startTime = System.currentTimeMillis();
			System.out.println("executing request " + httpmethod.getURI());
			
			Header[] rqheaders = httpmethod.getAllHeaders();
			for (Header one : rqheaders) {
				System.out.println("header-rq:" + one.getName() + ":" + one.getValue());
			}
			System.out.println("header-rq-jsonbody:"+jsonbody.toString());
			System.out.println("header-rq-body:"+entity.toString());
			
			response = httpclient.execute(httpmethod);

			Header[] rsheaders = response.getAllHeaders();
			for (Header one : rsheaders) {
				System.out.println("header-rs:" + one.getName() + ":" + one.getValue());
			}

			long endTime = System.currentTimeMillis();
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {

			}

		} catch (IOException e) {  

          } finally {  

          }  

          return 	response;	
	}
	
	
	private CloseableHttpResponse delete_get_trace(String url,String method,Map<String,String> headers,String jsonbody) throws ClientProtocolException, IOException {  
		
		  CloseableHttpClient httpclient = HttpClients.createDefault();
		  CloseableHttpResponse response=null;
		  HttpRequestBase httpmethod=null;
		  
		  if(HTTP_GET.equals(method)){
			  httpmethod =new HttpGet(url);
		  }else if(HTTP_DELETE.equals(method)){
			  httpmethod =new HttpDelete(url);
		  }else{
			  httpmethod =new HttpTrace(url);
		  }
		  
		  for (Map.Entry<String, String> entry : headers.entrySet()) {  			  
//			  httpmethod.addHeader(entry.getKey(),entry.getValue());  
			  //@todo check
			  httpmethod.setHeader(entry.getKey(),entry.getValue());  
			}    
		  
		  try {  
			    
            long startTime = System.currentTimeMillis();  
            System.out.println("executing request " + httpmethod.getURI());  
              
			Header[] rqheaders = httpmethod.getAllHeaders();
			for (Header one : rqheaders) {
				System.out.println("header-rq:" + one.getName() + ":" + one.getValue());
			}
			response = httpclient.execute(httpmethod);

			Header[] rsheaders = response.getAllHeaders();
			for (Header one : rsheaders) {
				System.out.println("header-rs:" + one.getName() + ":" + one.getValue());
			}
            
            long  endTime = System.currentTimeMillis();  
            int statusCode = response.getStatusLine().getStatusCode();  
              
//            logger.info("statusCode:" + statusCode);  
//            logger.info("调用API 花费时间(单位：毫秒)：" + (endTime - startTime));  
            if (statusCode != HttpStatus.SC_OK) {  
//                logger.error("Method failed:" + response.getStatusLine());  
//                status = 1;  
            }  
  
        } catch (IOException e) {  
//
        } finally {  
//               logger.info("调用接口状态：" + status);  
        }  

        return 	response;			
			
	}
	
	public CloseableHttpResponse post(String url,Map<String,String> headers,String jsonbody) throws ClientProtocolException, IOException{
		
		return sendRequest(url,HTTP_POST,headers,jsonbody);
	}
	
	public CloseableHttpResponse get(String url,Map<String,String> headers,String jsonbody) throws ClientProtocolException, IOException{
		
		return sendRequest(url,HTTP_GET,headers,jsonbody);
	}
	
	public CloseableHttpResponse put(String url,Map<String,String> headers,String jsonbody) throws ClientProtocolException, IOException{
		
		return sendRequest(url,HTTP_PUT,headers,jsonbody);
	}
	
	public CloseableHttpResponse delete(String url,Map<String,String> headers,String jsonbody) throws ClientProtocolException, IOException{
		
		return sendRequest(url,HTTP_DELETE,headers,jsonbody);
	}
	
	public CloseableHttpResponse patch(String url,Map<String,String> headers,String jsonbody) throws ClientProtocolException, IOException{
		
		return sendRequest(url,HTTP_PATCH,headers,jsonbody);
	}
	
	public CloseableHttpResponse trace(String url,Map<String,String> headers,String jsonbody) throws ClientProtocolException, IOException{
		
		return sendRequest(url,HTTP_TTACE,headers,jsonbody);
	}
	
	
	
}
