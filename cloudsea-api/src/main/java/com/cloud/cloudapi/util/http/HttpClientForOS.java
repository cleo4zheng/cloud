package com.cloud.cloudapi.util.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpClientForOS {
	
//	private final static String ks_adminurl="http://193.160.31.45:35357/v3/";
//	private final static String ks_puburlv2="http://193.160.31.45:5000/v2.0/";
//	private final static String ks_puburlv3="http://193.160.31.45:5000/v3/";
//	private final static String ks_admin="admin";
//	private final static String ks_adminpwd="ADMIN_PASS";
//	private final static String ks_admindomain="default";
	
	
//	private  String ks_adminurl="http://193.168.141.33:35357/v3/";
//	private  String ks_puburlv2="http://193.168.141.33:5000/v2.0/";
//	private  String ks_puburlv3="http://193.168.141.33:5000/v3/";
//	private  String ks_admin="admin";
//	private  String ks_tenant="admin";
//	private  String ks_adminpwd="elb1234";
//	private  String ks_admindomain="default";
	
//	private  String ks_puburlv2=null;
//	private  String ks_puburlv3=null;
	private  String ks_puburl=null;
	private  String ks_user=null;
	private  String ks_tenant=null;
	private  String ks_pwd=null;
	private  String ks_domainid=null;
	
	private  HttpClientForRest client=null;

	private Logger log = LogManager.getLogger(HttpClientForOS.class);
	
	public HttpClientForOS(String ks_puburl, String ks_user,String ks_pwd, String ks_domainid_tenant) {
		super();	
		if(ks_puburl.indexOf("v3")>-1){
//			this.ks_puburlv3 = ks_puburl;
			this.ks_domainid = ks_domainid_tenant;
		}else{
//			this.ks_puburlv2 = ks_puburl;
			this.ks_tenant = ks_domainid_tenant;
		}
		this.ks_puburl =ks_puburl;
		this.ks_user = ks_user;
		this.ks_pwd = ks_pwd;
		this.client = new HttpClientForRest();
	}
	
    public String getToken(){
    	if(this.ks_puburl.indexOf("v3")>-1){
    	 return getTokenV3();
    	}else{
    	 return getTokenV2();
    	}
    }
    
	private String getTokenV2(){
     System.out.println("token-response-v2:start");	 
	 String token=null;
	 
	 HashMap<String,String> headers= new HashMap<String,String>();
	 headers.put("Content-type","application/json; charset=utf-8");
	 headers.put("Accept", "application/json"); 
	 String jsonbody="{\"auth\": {\"tenantName\": \""+ks_tenant+"\",\"passwordCredentials\": {\"username\":\""+ks_user+"\",\"password\":\""+ks_pwd+"\"}}}";
	 CloseableHttpResponse rs=null;
		try {
			rs = client.post(ks_puburl + "tokens", headers, jsonbody);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			log.error(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}

		if (rs == null) {

			System.out.println("wo cha:request failed"); 

		} else if (rs.getStatusLine().getStatusCode() > 400) {

			System.out.println("wo cha:reponse code is unsscessfully :" + rs.getStatusLine().getStatusCode()); 
																												
			System.out.println("wo cha:reponse  :" + rs.toString()); 
																		

		} else {
			try {
				ObjectMapper mapper = new ObjectMapper();
				String resData = EntityUtils.toString(rs.getEntity());
				JsonNode node = mapper.readTree(resData);

				// lets see what type the node is
				System.out.println(node.getNodeType()); 
				// is it a container
				System.out.println(node.isContainerNode()); 
				// lets find out what fields it has
				// Iterator<String> fieldNames = node.fieldNames();
				// while (fieldNames.hasNext()) {
				// String fieldName = fieldNames.next();
				// System.out.println(fieldName);// prints title, message,
				// // errors,
				// // total,
				// // total_pages, page, limit,
				// // dataset
				// }

				System.out.println(node.toString());

				// Iterator<Entry<String, JsonNode>> ir = node.fields();
				//
				// while (ir.hasNext()) {
				//
				// Entry<String, JsonNode> et = ir.next();
				// System.out.println(et.getKey() + "-->" + et.getValue());
				//
				// }

//				JsonNode ids = node.path("access").path("token").path("audit_ids");
//				System.out.println("audit_ids:" + ids.toString());
//				System.out.println("audit_ids:" + ids.isArray());
//				System.out.println("audit_ids:" + ids.get(0).toString());
				
				JsonNode tokenid= node.path("access").path("token").path("id");
				
// 用tostring会给结果字符传上加上双引号，结果导致token值无法使�?			
//				token = tokenid.toString();
//				System.out.println("tokenid.toString():"+token);
				
				token = tokenid.textValue();
				System.out.println("tokenid.textValue():"+token);
				
				// test arrays
				JsonNode ss = node.findPath("serviceCatalog");
				List<JsonNode> eds = ss.findValues("endpoints");
				for (JsonNode one : eds) {
					System.out.println("adminurl:" + one.get(0).path("adminURL").toString());
				}

			} catch (ParseException | IOException e) {
				// TODO Auto-generated catch block
				log.error(e);
			}

		}
		System.out.println("token-response-v2:end");
		return token;
	}
	
	
	private String getTokenV3(){
		System.out.println("token-response-v3:start");
		String token = null;

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("Content-type", "application/json; charset=utf-8");
		headers.put("Accept", "application/json");
		String jsonbody = "{\"auth\": {\"identity\": {\"methods\": [\"password\"],\"password\": {\"user\": {\"name\": \""
				+ ks_user + "\",\"domain\": {\"id\": \"" + ks_domainid + "\"},\"password\": \"" + ks_pwd + "\"}}}}}";
		CloseableHttpResponse rs = null;
	
		try {
			rs = client.post(ks_puburl + "auth/tokens", headers, jsonbody);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			log.error(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		
		if (rs == null) {
			
			System.out.println("wo cha:request failed"); // prints true
			
		} else if (rs.getStatusLine().getStatusCode() >400) {

			System.out.println("wo cha:reponse code is not 200 :"+rs.getStatusLine().getStatusCode()); // prints true
			System.out.println("wo cha:reponse  :"+rs.toString()); // prints true
		} else {	

		try {
//			ObjectMapper mapper = new ObjectMapper();
//			String resData = EntityUtils.toString(rs.getEntity());
//			JsonNode node = mapper.readTree(resData);

//			// lets see what type the node is
//			System.out.println(node.getNodeType()); // prints OBJECT
//			// is it a container
//			System.out.println(node.isContainerNode()); // prints true
//			// lets find out what fields it has
//			Iterator<String> fieldNames = node.fieldNames();
//			while (fieldNames.hasNext()) {
//				String fieldName = fieldNames.next();
//				System.out.println(fieldName);// prints title, message, errors,
//												// total,
//												// total_pages, page, limit,
//												// dataset
//			}
//
//			System.out.println(node.toString());
//			System.out.println("tokens:" + node.findValue("audit_ids").get(0).toString());
//			System.out.println("tokens:" + node.findPath("audit_ids").get(0).toString());
//			
////			Header[] rsheaders=rs.getHeaders("X-Auth-Token");
////			Header[] rsheaders=rs.getAllHeaders();
////			for (Header one :rsheaders){
////			System.out.println("header:" + one.getName()+":"+one.getValue());			
////			}
						
			Header tokenheader=rs.getFirstHeader("X-Subject-Token");
			
			token=tokenheader.getValue();
			
			// Iterator<JsonNode> albums = node.path("token").iterator();
			// while (albums.hasNext()) {
			// System.out.println(albums.next().path("issued_at").asText());
			// }

		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		}
		System.out.println("token-response-v3:end");
		return token;
	}
	
	public Map<String,String>  httpDoPost(String url,Map<String,String> headers,String jsonbody){
		
		CloseableHttpResponse rs = null;
		Map<String,String>   rsmap=null;
		try {
			rs=client.post(url, headers, jsonbody);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			log.error(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		
		if (rs == null) {
			
			System.out.println("wo cha:request failed"); // prints true
			
		} else {
			rsmap =new HashMap<String,String>();
			rsmap.put("httpcode", String.valueOf(rs.getStatusLine().getStatusCode()));
			
			try {
				ObjectMapper mapper = new ObjectMapper();
				String resData = EntityUtils.toString(rs.getEntity());
				JsonNode node = mapper.readTree(resData);
				rsmap.put("jsonbody", node.toString());
			} catch (ParseException | IOException e) {
				// TODO Auto-generated catch block
				log.error(e);
			}
		}		
		return rsmap;
		
	}
	
	public Map<String,String>  httpDoPut(String url,Map<String,String> headers,String jsonbody){
		
		CloseableHttpResponse rs = null;
		Map<String,String>   rsmap=null;
		
		try {
			rs=client.put(url, headers, jsonbody);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			log.error(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		
		if (rs == null) {
			
			System.out.println("wo cha:request failed"); // prints true
			
		} else {
			rsmap =new HashMap<String,String>();
			rsmap.put("httpcode", String.valueOf(rs.getStatusLine().getStatusCode()));
			
			try {
				ObjectMapper mapper = new ObjectMapper();
				String resData = EntityUtils.toString(rs.getEntity());
				JsonNode node = mapper.readTree(resData);
				rsmap.put("jsonbody", node.toString());
			} catch (ParseException | IOException e) {
				// TODO Auto-generated catch block
				log.error(e);
			}
		}		
		return rsmap;
		
	}
	
	public Map<String,String>  httpDoPatch(String url,Map<String,String> headers,String jsonbody){
		
		CloseableHttpResponse rs = null;
		Map<String,String>   rsmap=null;
		try {
			rs=client.patch(url, headers, jsonbody);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			log.error(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		
		if (rs == null) {
			
			System.out.println("wo cha:request failed"); // prints true
			
		} else {
			rsmap =new HashMap<String,String>();
			rsmap.put("httpcode", String.valueOf(rs.getStatusLine().getStatusCode()));
			
			try {
				ObjectMapper mapper = new ObjectMapper();
				String resData = EntityUtils.toString(rs.getEntity());
				JsonNode node = mapper.readTree(resData);
				rsmap.put("jsonbody", node.toString());
			} catch (ParseException | IOException e) {
				// TODO Auto-generated catch block
				log.error(e);
			}
		}		
		return rsmap;
		
	}
	
	public Map<String,String>  httpDoDelete(String url,Map<String,String> headers){
		
		CloseableHttpResponse rs = null;
		Map<String,String>   rsmap=null;
		try {
			rs=client.delete(url, headers, null);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			log.error(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		
		if (rs == null) {
			
			System.out.println("wo cha:request failed"); // prints true
			
		} else {
			rsmap =new HashMap<String,String>();
			rsmap.put("httpcode", String.valueOf(rs.getStatusLine().getStatusCode()));
			
			try {
				ObjectMapper mapper = new ObjectMapper();
				String resData = EntityUtils.toString(rs.getEntity());
				JsonNode node = mapper.readTree(resData);
				rsmap.put("jsonbody", node.toString());
			} catch (ParseException | IOException e) {
				// TODO Auto-generated catch block
				log.error(e);
			}
		}		
		return rsmap;	
	}
	
	public Map<String,String>  httpDoGet(String url,Map<String,String> headers){
		
		CloseableHttpResponse rs = null;
		Map<String,String>   rsmap=null;
		try {
			rs=client.get(url, headers, null);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			log.error(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		
		if (rs == null) {
			
			System.out.println("wo cha:request failed"); // prints true
			
		} else {
			rsmap =new HashMap<String,String>();
			rsmap.put("httpcode", String.valueOf(rs.getStatusLine().getStatusCode()));
			
			try {
				ObjectMapper mapper = new ObjectMapper();
				String resData = EntityUtils.toString(rs.getEntity());
				JsonNode node = mapper.readTree(resData);
				rsmap.put("jsonbody", node.toString());
			} catch (ParseException | IOException e) {
				// TODO Auto-generated catch block
				log.error(e);
			}
		}		
		return rsmap;	
	}

}
