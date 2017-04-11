package com.cloud.cloudapi.util.http;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpClientForOsRequest {

	private HttpClientForRest client = null;
	private Logger log = LogManager.getLogger(HttpClientForRest.class);

	public HttpClientForOsRequest() {
		client = new HttpClientForRest();
	}

	public Map<String, String> httpDoPost(String url, Map<String, String> headers, String jsonbody) {

		CloseableHttpResponse rs = null;
		Map<String, String> rsmap = null;
		try {
			rs = client.post(url, headers, jsonbody);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			log.error(e);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}

		if (rs == null) {

			log.error("wo cha:request failed"); // prints true

		} else {
			rsmap = new HashMap<String, String>();
			rsmap.put("httpcode", String.valueOf(rs.getStatusLine().getStatusCode()));

			try {
				Header[] rsheaders = rs.getAllHeaders();
				for (Header one : rsheaders) {
					log.error(one.getName()+":"+one.getValue());
					rsmap.put(one.getName(), one.getValue());
				}

				if (rs.getEntity() != null) {
					ObjectMapper mapper = new ObjectMapper();
					String resData = EntityUtils.toString(rs.getEntity());
					if(null != resData && !resData.isEmpty()){
					JsonNode node = mapper.readTree(resData);
					rsmap.put("jsonbody", node.toString());
					}
				} else {
					rsmap.put("jsonbody", "noresult");
				}
			} catch (ParseException | IOException e) {
				// TODO Auto-generated catch block
				log.error("json change failed:" + e.getMessage());
				rsmap.put("jsonbody", "error when json convert");
			}
		}
		return rsmap;

	}

	public Map<String, String> httpDoPost(String url, String tokenid, String jsonbody) {

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("X-Auth-Token", tokenid);

		return httpDoPost(url, headers, jsonbody);
	}

	public Map<String, String> httpDoPut(String url, Map<String, String> headers, String jsonbody) {

		CloseableHttpResponse rs = null;
		Map<String, String> rsmap = null;

		try {
			rs = client.put(url, headers, jsonbody);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			log.error(e);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}

		if (rs == null) {
			log.error("wo cha:request failed");

		} else {
			rsmap = new HashMap<String, String>();
			rsmap.put("httpcode", String.valueOf(rs.getStatusLine().getStatusCode()));

			try {
				Header[] rsheaders = rs.getAllHeaders();
				for (Header one : rsheaders) {
					log.error(one.getName()+":"+one.getValue());
					rsmap.put(one.getName(), one.getValue());
				}

				if (rs.getEntity() != null) {
					ObjectMapper mapper = new ObjectMapper();
					String resData = EntityUtils.toString(rs.getEntity());
					if(null != resData && !resData.isEmpty()){
					JsonNode node = mapper.readTree(resData);
					rsmap.put("jsonbody", node.toString());
					}
				} else {
					rsmap.put("jsonbody", "noresult");
				}
			} catch (ParseException | IOException e) {
				// TODO Auto-generated catch block
				log.error("json change failed:" + e);
				rsmap.put("jsonbody", "error when json convert");
			}
		}
		return rsmap;

	}

	public Map<String, String> httpDoPut(String url, String tokenid, String jsonbody) {

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("X-Auth-Token", tokenid);

		return httpDoPut(url, headers, jsonbody);
	}

	public Map<String, String> httpDoPatch(String url, Map<String, String> headers, String jsonbody) {

		CloseableHttpResponse rs = null;
		Map<String, String> rsmap = null;
		try {
			rs = client.patch(url, headers, jsonbody);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			log.error(e);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}

		if (rs == null) {
			log.error("wo cha:request failed");

		} else {
			rsmap = new HashMap<String, String>();
			rsmap.put("httpcode", String.valueOf(rs.getStatusLine().getStatusCode()));

			try {
				Header[] rsheaders = rs.getAllHeaders();
				for (Header one : rsheaders) {
					log.error(one.getName()+":"+one.getValue());
					rsmap.put(one.getName(), one.getValue());
				}

				if (rs.getEntity() != null) {
					ObjectMapper mapper = new ObjectMapper();
					String resData = EntityUtils.toString(rs.getEntity());
					if(null != resData && !resData.isEmpty()){
					JsonNode node = mapper.readTree(resData);
					rsmap.put("jsonbody", node.toString());
					}
				} else {
					rsmap.put("jsonbody", "noresult");
				}
			} catch (ParseException | IOException e) {
				// TODO Auto-generated catch block
				log.error("json change failed:" + e);
				rsmap.put("jsonbody", "error when json convert");
			}
		}
		return rsmap;

	}

	public Map<String, String> httpDoPatch(String url, String tokenid, String jsonbody) {

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("X-Auth-Token", tokenid);

		return httpDoPatch(url, headers, jsonbody);
	}

	public Map<String, String> httpDoDelete(String url,  Map<String, String> headers, String jsonbody) {

		CloseableHttpResponse rs = null;
		Map<String, String> rsmap = null;
		try {
			rs = client.delete(url, headers, jsonbody);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			log.error(e);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}

		if (rs == null) {
			log.error("wo cha:request failed");

		} else {
			rsmap = new HashMap<String, String>();
			rsmap.put("httpcode", String.valueOf(rs.getStatusLine().getStatusCode()));

			try {
				Header[] rsheaders = rs.getAllHeaders();
				for (Header one : rsheaders) {
					log.error(one.getName()+":"+one.getValue());
					rsmap.put(one.getName(), one.getValue());
				}

				if (rs.getEntity() != null) {
					ObjectMapper mapper = new ObjectMapper();
					String resData = EntityUtils.toString(rs.getEntity());
					if(null != resData && !resData.isEmpty()){
					JsonNode node = mapper.readTree(resData);
					rsmap.put("jsonbody", node.toString());
					}
				} else {
					rsmap.put("jsonbody", "noresult");
				}
			} catch (ParseException | IOException e) {
				// TODO Auto-generated catch block
				log.error("json change failed:" + e);
				rsmap.put("jsonbody", "error when json convert");
			}
		}
		return rsmap;
	}
	public Map<String, String> httpDoDelete(String url, Map<String, String> headers) {
		return httpDoDelete(url,headers,null);
	}

	public Map<String, String> httpDoDelete(String url, String tokenid) {

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("X-Auth-Token", tokenid);

		return httpDoDelete(url, headers);
	}

	public Map<String, String> httpDoGet(String url, Map<String, String> headers) {

		CloseableHttpResponse rs = null;
		Map<String, String> rsmap = null;
		try {
			rs = client.get(url, headers, null);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			log.error(e);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}

		if (rs == null) {
			log.error("wo cha:request failed");
		} else {
			rsmap = new HashMap<String, String>();
			rsmap.put("httpcode", String.valueOf(rs.getStatusLine().getStatusCode()));

			try {
				Header[] rsheaders = rs.getAllHeaders();
				for (Header one : rsheaders) {
					log.error(one.getName()+":"+one.getValue());
					rsmap.put(one.getName(), one.getValue());
				}

				if (rs.getEntity() != null) {
					ObjectMapper mapper = new ObjectMapper();
					String resData = EntityUtils.toString(rs.getEntity());
					if(null != resData && !resData.isEmpty()){
					JsonNode node = mapper.readTree(resData);
					rsmap.put("jsonbody", node.toString());
					}
				} else {
					rsmap.put("jsonbody", "noresult");
				}
			} catch (ParseException | IOException e) {
				// TODO Auto-generated catch block
				log.error("json change failed:" + e);
				rsmap.put("jsonbody", "error when json convert");
			}
		}
		return rsmap;
	}

	public Map<String, String> httpDoGet(String url, String tokenid) {

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("X-Auth-Token", tokenid);

		return httpDoGet(url, headers);
	}

}
