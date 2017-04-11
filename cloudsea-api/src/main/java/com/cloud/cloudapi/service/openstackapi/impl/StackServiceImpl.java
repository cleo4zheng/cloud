package com.cloud.cloudapi.service.openstackapi.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Stack;
import com.cloud.cloudapi.pojo.openstackapi.forgui.StackResource;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.StackService;
import com.cloud.cloudapi.service.pool.BasicResource;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class StackServiceImpl implements StackService {
	@Resource
	private OSHttpClientUtil client;
	
	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(StackServiceImpl.class);
	
	@Override
	public List<Stack> getStackList(Map<String, String> paramMap, TokenOs ostoken) {
		// HttpClientForOsBase osClient = new HttpClientForOsBase(cloudconfig);
		// TokenOs ot = osClient.getToken();
		TokenOs ot = ostoken;
		String region = "RegionOne";
		String url = ot.getEndPoint(TokenOs.EP_TYPE_ORCHESTRATION, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/stacks", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("X-Auth-Token", ot.getTokenid());

		Map<String, String> rs = client.httpDoGet(url, headers);
		int resCode = Integer.parseInt(rs.get("httpcode"));
		String resBody = rs.get("jsonbody");
		System.out.println("【CODE】:  " + resCode);
		System.out.println("【BODY】:  " + resBody);

		List<Stack> list = null;
		if (resCode > 400) {
			System.out.println("List Stacks failed");
		} else {
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(resBody);
				JsonNode stacksNode = rootNode.path("stacks");
				int stacks_num = stacksNode.size();
				if (stacks_num > 0) {
					list = new ArrayList<Stack>();
					for (int i = 0; i < stacks_num; i++) {
						Stack one = new Stack();
						JsonNode oneStack = stacksNode.get(i);
						String id = oneStack.path("id").textValue();
						one.setId(id);
						one.setName(oneStack.path("stack_name").textValue());
						one.setStatus(oneStack.path("stack_status").textValue());
						one.setStatusReason(oneStack.path("stack_status_reason").textValue());
						one.setCreatedAt(oneStack.path("creation_time").textValue());
						one.setUpdatedAt(oneStack.path("updated_time").textValue());
						one.setOwner(oneStack.path("stack_owner").textValue());
						list.add(one);
					}
				}
			} catch (Exception e) {
				log.error(e);
			}
		}
		return list;
	}

	@Override
	public Stack createStack(String stackName, Map<String, String> paramMap, String template, Map<String, String> files,
			String environment, TokenOs ostoken) throws BusinessException {
		TokenOs ot = ostoken;
		String region = "RegionOne";
		String url = ot.getEndPoint(TokenOs.EP_TYPE_ORCHESTRATION, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/stacks", paramMap);

		Map<String, Object> body = new HashMap<String, Object>();
		if (files == null) {
			body.put("files", new HashMap<String, String>());
		} else {
			body.put("files", files);
		}
		if (environment == null) {
			body.put("environment", new HashMap<String, String>());
		} else {
			body.put("environment", environment);
		}
		body.put("disable_rollback", false);
		body.put("stack_name", stackName);
		if (paramMap == null) {
			body.put("parameters", new HashMap<String, String>());
		} else {
			body.put("parameters", paramMap);
		}
		body.put("template", template);

		String bodyStr = BasicResource.convertMAP2JSON(body);

		System.out.println("【BODYSTRING】: " + bodyStr);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("X-Auth-Token", ot.getTokenid());
		headers.put("Content-Type", "application/json");
        Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPost(url, headers, bodyStr);

		Stack stack = null;
		int resCode = Integer.parseInt(rs.get("httpcode"));
		String resBody = rs.get("jsonbody");
		Util.checkResponseBody(rs,locale);
		
		System.out.println("【CODE】:  " + resCode);
		System.out.println("【BODY】:  " + resBody);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(resBody);
				JsonNode stackNode = rootNode.path("stack");
				String id = stackNode.path("id").textValue();
				stack = new Stack();
				stack.setId(id);
				stack.setName(stackName);
				stack.setStatus("CREAT_IN_PROGRESS");
			} catch (Exception e) {
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoPost(url, headers, bodyStr);
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(resBody);
				JsonNode stackNode = rootNode.path("stack");
				String id = stackNode.path("id").textValue();
				stack = new Stack();
				stack.setId(id);
				stack.setName(stackName);
				stack.setStatus("CREAT_IN_PROGRESS");
			} catch (Exception e) {
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		}

		return stack;
	}

	@Override
	public Stack updateStack(String stackName, Map<String, String> paramMap, String template, Map<String, String> files,
			String environment, TokenOs ostoken) throws BusinessException {
		// HttpClientForOsBase osClient = new HttpClientForOsBase(cloudconfig);
		// TokenOs ot = osClient.getToken();
		TokenOs ot = ostoken;
		String region = "RegionOne";
		String url = ot.getEndPoint(TokenOs.EP_TYPE_ORCHESTRATION, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/stacks", paramMap);
		Map<String, Object> body = new HashMap<String, Object>();
		if (files == null) {
			body.put("files", new HashMap<String, String>());
		} else {
			body.put("files", files);
		}
		if (environment == null) {
			body.put("environment", new HashMap<String, String>());
		} else {
			body.put("environment", environment);
		}
		body.put("disable_rollback", false);
		body.put("stack_name", stackName);
		if (paramMap == null) {
			body.put("parameters", new HashMap<String, String>());
		} else {
			body.put("parameters", paramMap);
		}
		body.put("template", template);

		String bodyStr = BasicResource.convertMAP2JSON(body);

		System.out.println("【BODYSTRING】: " + bodyStr);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("X-Auth-Token", ot.getTokenid());
		headers.put("Content-Type", "application/json");

		Map<String, String> rs = client.httpDoPut(url, headers, bodyStr);

		Stack stack = null;
		String resBody = rs.get("jsonbody");
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(resBody);
				JsonNode stackNode = rootNode.path("stack");
				String id = stackNode.path("id").textValue();
				stack = new Stack();
				stack.setId(id);
				stack.setStatus("CREATING");
			} catch (Exception e) {
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
			}
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoPost(url, headers, bodyStr);
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(resBody);
				JsonNode stackNode = rootNode.path("stack");
				String id = stackNode.path("id").textValue();
				stack = new Stack();
				stack.setId(id);
				stack.setStatus("CREATING");
			} catch (Exception e) {
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,new Locale(ostoken.getLocale()));
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,new Locale(ostoken.getLocale()));
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,new Locale(ostoken.getLocale()));
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,new Locale(ostoken.getLocale()));
		default:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,new Locale(ostoken.getLocale()));
		}
		
//		if (resCode != NORMAL_RESPONSE_CODE) {
//			System.out.println("Create Stack failed");
//		} else {
//			try {
//				ObjectMapper mapper = new ObjectMapper();
//				JsonNode rootNode = mapper.readTree(resBody);
//				JsonNode stackNode = rootNode.path("stack");
//				String id = stackNode.path("id").textValue();
//				stack = new Stack();
//				stack.setId(id);
//				stack.setStatus("CREATING");
//			} catch (Exception e) {
//				log.error(e);
//			}
//		}

		return stack;
	}

	@Override
	public Stack getStack(String stackId, TokenOs ostoken) {
		TokenOs ot = ostoken;
		String region = "RegionOne";
		String url = ot.getEndPoint(TokenOs.EP_TYPE_ORCHESTRATION, region).getPublicURL();
		url = url + "/stacks/" + stackId;

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("X-Auth-Token", ot.getTokenid());

		Map<String, String> rs = client.httpDoGet(url, headers);
		int resCode = Integer.parseInt(rs.get("httpcode"));
		String resBody = rs.get("jsonbody");

		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		Stack stack = null;
		if (resCode > 400) {
			System.out.println("Get Stack failed");
		} else {
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(resBody);
				JsonNode stackNode = rootNode.path("stack");
				String id = stackNode.path("id").textValue();
				stack = new Stack();
				stack.setId(id);
				stack.setName(stackNode.path("stack_name").textValue());
				stack.setStatus(stackNode.path("stack_status").textValue());
				stack.setStatusReason(stackNode.path("stack_status_reason").textValue());
				stack.setCreatedAt(stackNode.path("creation_time").textValue());
				stack.setUpdatedAt(stackNode.path("updated_time").textValue());
				stack.setOwner(stackNode.path("stack_owner").textValue());
				JsonNode outNodes = stackNode.path("outputs");
				int out_num = outNodes.size();
				if (out_num > 0) {
					for (int i = 0; i < out_num; i++) {
						JsonNode curNode = outNodes.get(i);
						if (curNode.path("output_key").textValue().equals("template_name")) {
							stack.setTemplate(curNode.path("output_value").textValue());
							break;
						}
					}
				}
			} catch (Exception e) {
				log.error(e);
			}
		}
		return stack;
	}

	@Override
	public List<StackResource> getStackResourceList(String stackName, String stackId, TokenOs ostoken) {
		// HttpClientForOsBase osClient = new HttpClientForOsBase(cloudconfig);
		// TokenOs ot = osClient.getToken();
		TokenOs ot = ostoken;
		String region = "RegionOne";
		String url = ot.getEndPoint(TokenOs.EP_TYPE_ORCHESTRATION, region).getPublicURL();
		url = url + "/stacks/" + stackName + "/" + stackId + "/resources";

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("X-Auth-Token", ot.getTokenid());

		Map<String, String> rs = client.httpDoGet(url, headers);
		int resCode = Integer.parseInt(rs.get("httpcode"));
		String resBody = rs.get("jsonbody");
		System.out.println("【CODE】:  " + resCode);
		System.out.println("【BODY】:  " + resBody);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		List<StackResource> list = null;
		if (resCode > 400) {
			System.out.println("List Stack's Resources failed");
		} else {
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(resBody);
				JsonNode resourcesNode = rootNode.path("resources");
				int stacks_num = resourcesNode.size();
				if (stacks_num > 0) {
					list = new ArrayList<StackResource>();
					for (int i = 0; i < stacks_num; i++) {
						StackResource one = new StackResource();
						JsonNode oneResource = resourcesNode.get(i);
						String id = oneResource.path("logical_resource_id").textValue();
						one.setId(id);
						one.setName(oneResource.path("resource_name").textValue());
						one.setPhysicalResourceId(oneResource.path("physical_resource_id").textValue());

						JsonNode requiredByNode = oneResource.path("required_by");
						String[] rb = new String[requiredByNode.size()];
						for (int a = 0; a < requiredByNode.size(); a++) {
							rb[a] = requiredByNode.get(a).textValue();
						}
						one.setRequiredBy(rb);

						one.setResourceType(oneResource.path("resource_type").textValue());
						one.setStatus(oneResource.path("resource_status").textValue());
						one.setStatusReason(oneResource.path("resource_status_reason").textValue());
						one.setCreatedAt(oneResource.path("creation_time").textValue());
						one.setUpdatedAt(oneResource.path("updated_time").textValue());
						list.add(one);
					}
				}
			} catch (Exception e) {
				log.error(e);
			}
		}
		return list;
	}

	@Override
	public StackResource getStackResource(String stackName, String stackId, String resourceName, TokenOs ostoken) {
		TokenOs ot = ostoken;
		String region = "RegionOne";
		String url = ot.getEndPoint(TokenOs.EP_TYPE_ORCHESTRATION, region).getPublicURL();
		url = url + "/stacks/" + stackName + "/" + stackId + "/resources/" + resourceName;

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("X-Auth-Token", ot.getTokenid());

		Map<String, String> rs = client.httpDoGet(url, headers);
		int resCode = Integer.parseInt(rs.get("httpcode"));
		String resBody = rs.get("jsonbody");
		System.out.println("【CODE】:  " + resCode);
		System.out.println("【BODY】:  " + resBody);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		StackResource one = null;
		if (resCode > 400) {
			return null;
		} else {
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(resBody);
				JsonNode oneResource = rootNode.path("resource");
				one = new StackResource();
				String id = oneResource.path("logical_resource_id").textValue();
				one.setId(id);
				one.setName(oneResource.path("resource_name").textValue());
				one.setPhysicalResourceId(oneResource.path("physical_resource_id").textValue());

				JsonNode requiredByNode = oneResource.path("required_by");
				String[] rb = new String[requiredByNode.size()];
				for (int a = 0; a < requiredByNode.size(); a++) {
					rb[a] = requiredByNode.get(a).textValue();
				}
				one.setRequiredBy(rb);

				one.setResourceType(oneResource.path("resource_type").textValue());
				one.setStatus(oneResource.path("resource_status").textValue());
				one.setStatusReason(oneResource.path("resource_status_reason").textValue());
				one.setUpdatedAt(oneResource.path("updated_time").textValue());
				one.setCreatedAt(oneResource.path("creation_time").textValue());
				one.setAttributes(oneResource.path("attributes").toString());
			} catch (Exception e) {
				log.error(e);
			}
		}
		return one;
	}

	@Override
	public String getStackNameById(String stackId, TokenOs ostoken) {
		List<Stack> stackList = this.getStackList(null, ostoken);
		for (Stack s : stackList) {
			if (s.getId().equals(stackId)) {
				return s.getName();
			}
		}
		return null;
	}

	@Override
	public boolean deleteStack(String stackName, String stackId, TokenOs ostoken) {
		// HttpClientForOsBase osClient = new HttpClientForOsBase(cloudconfig);
		// TokenOs ot = osClient.getToken();
		TokenOs ot = ostoken;
		String region = "RegionOne";
		String url = ot.getEndPoint(TokenOs.EP_TYPE_ORCHESTRATION, region).getPublicURL();
		url = url + "/stacks/" + stackName + "/" + stackId;

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("X-Auth-Token", ot.getTokenid());
		Map<String, String> rs = client.httpDoDelete(url, headers);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int resCode = Integer.parseInt(rs.get("httpcode"));
		String resBody = rs.get("jsonbody");
		System.out.println("【CODE】:  " + resCode);
		System.out.println("【BODY】:  " + resBody);
		if (resCode == 204) {
			return true;
		}
		return false;
	}

}
