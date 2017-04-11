package com.cloud.cloudapi.service.openstackapi.impl;

import java.io.IOException;
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
import com.cloud.cloudapi.json.forgui.QosBandwithJSON;
import com.cloud.cloudapi.json.forgui.QosPolicyJSON;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.QosBandwith;
import com.cloud.cloudapi.pojo.openstackapi.forgui.QosDscp;
import com.cloud.cloudapi.pojo.openstackapi.forgui.QosPolicy;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.QosService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("qosService")
public class QosServiceImpl implements QosService {
	@Resource
	private OSHttpClientUtil client;
	
	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(QosServiceImpl.class);
	
	@Override
	public List<QosPolicy> getQosPolicies(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException{
//		int limitItems = Util.getLimit(paramMap);
//		List<QosPolicy> qospoliciesFromDB = getQosPoliciesFromDB(ostoken,response,limitItems);
//		if(!Util.isNullOrEmptyList(qospoliciesFromDB))
//			return qospoliciesFromDB;
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion(); // we should get the regioninfo by the guiTokenId
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/qos/policies", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
        Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(url, ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
        List<QosPolicy> policies = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				policies = getPolicies(rs);
			}  catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
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
			rs = client.httpDoGet(url, headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_NETWORK_QOS_POLICY_GET_FAILED,httpCode,locale);
			try {
				policies = getPolicies(rs);
			}  catch (Exception e) {
				// TODO Auto-generated catch block
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
			throw new ResourceBusinessException(Message.CS_NETWORK_QOS_POLICY_GET_FAILED,httpCode,locale);
		}
		
//		policies = storePorts2DB(policies,ostoken,response);
//		return getLimitItems(policies,limitItems);
		
		return policies;
	}
	
	@Override
	public QosPolicy getQosPolicy(String policyId,TokenOs ostoken) throws BusinessException{
//		QosPolicy policy = routerMapper.selectByPrimaryKey(routerId);
//		if (null != router)
//			return router;


		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/qos/policies/");
		sb.append(policyId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		QosPolicy policy = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				policy = getPolicy(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
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
			rs = client.httpDoGet(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_NETWORK_QOS_POLICY_DETAIL_GET_FAILED,httpCode,locale);
			try {
				policy = getPolicy(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
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
			throw new ResourceBusinessException(Message.CS_NETWORK_QOS_POLICY_DETAIL_GET_FAILED,httpCode,locale);
		}

	//	storeRouter2DB(router);
		return policy;
	}
	
	
	@Override
	public QosPolicy createQosPolicy(String createBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException{
		
		QosPolicy policyCreateInfo = new QosPolicy();
		makeQosPolicyCreateInfo(createBody,policyCreateInfo);
		String policyCreateBody = getQosPolicyCreatedBody(policyCreateInfo);
		
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/qos/policies", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoPost(url, ostoken.getTokenid(), policyCreateBody);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		QosPolicy policy = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				policy = getPolicy(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
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
			rs = client.httpDoPost(url, tokenid, policyCreateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
            if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
            	throw new ResourceBusinessException(Message.CS_NETWORK_QOS_POLICY_CREATE_FAILED,httpCode,locale);
			try {
				policy = getPolicy(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
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
			throw new ResourceBusinessException(Message.CS_NETWORK_QOS_POLICY_CREATE_FAILED,httpCode,locale);
		}

		return policy;
	}
	
	@Override
	public List<QosBandwith> getQosBandwiths(Map<String,String> paramMap,String policyId,TokenOs ostoken) throws BusinessException{
//		int limitItems = Util.getLimit(paramMap);
//		List<QosPolicy> qospoliciesFromDB = getQosPoliciesFromDB(ostoken,response,limitItems);
//		if(!Util.isNullOrEmptyList(qospoliciesFromDB))
//			return qospoliciesFromDB;
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion(); // we should get the regioninfo by the guiTokenId
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/qos/policies/");
		sb.append(policyId);
		sb.append("/bandwidth_limit_rules");

		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
        List<QosBandwith> bandwiths = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				bandwiths = getBandwiths(rs);
			}  catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
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
			rs = client.httpDoGet(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_NETWORK_QOS_BANDWITH_GET_FAILED,httpCode,locale);
			try {
				bandwiths = getBandwiths(rs);
			}  catch (Exception e) {
				// TODO Auto-generated catch block
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
			throw new ResourceBusinessException(Message.CS_NETWORK_QOS_BANDWITH_GET_FAILED,httpCode,locale);
		}
		
//		policies = storePorts2DB(policies,ostoken,response);
//		return getLimitItems(policies,limitItems);
		
		return bandwiths;
	}
	
	@Override
	public QosBandwith getQosBandwith(String policyId,String bandwithId,TokenOs ostoken) throws BusinessException{
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/qos/policies/");
		sb.append(policyId);
		sb.append("/bandwidth_limit_rules/");
		sb.append(bandwithId);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		QosBandwith bandwith = null;
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				bandwith = getBandwith(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
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
			rs = client.httpDoGet(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_NETWORK_QOS_BANDWITH_DETAIL_GET_FAILED,httpCode,locale);
			try {
				bandwith = getBandwith(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
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
			throw new ResourceBusinessException(Message.CS_NETWORK_QOS_BANDWITH_DETAIL_GET_FAILED,httpCode,locale);
		}

	//	storeRouter2DB(router);
		return bandwith;
	}
	
	@Override
	public QosBandwith createBandwith(String createBody,String policyId,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException{
		QosBandwith bandwithCreateInfo = new QosBandwith();
		makeQosBandwithCreateInfo(createBody,bandwithCreateInfo);
		String bandwithCreateBody = getQosBandwithCreatedBody(bandwithCreateInfo);
		
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/qos/policies/");
		sb.append(policyId);
		sb.append("/bandwidth_limit_rules");

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoPost(sb.toString(), ostoken.getTokenid(), bandwithCreateBody);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		QosBandwith bandwith = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				bandwith = getBandwith(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
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
			rs = client.httpDoPost(sb.toString(), tokenid, bandwithCreateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_NETWORK_QOS_BANDWITH_CREATE_FAILED,httpCode,locale);
			try {
				bandwith = getBandwith(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
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
			throw new ResourceBusinessException(Message.CS_NETWORK_QOS_BANDWITH_CREATE_FAILED,httpCode,locale);
		}

		return bandwith;
	}
	
	private void makeQosPolicyCreateInfo(String createBody,QosPolicy policyCreateInfo) throws JsonProcessingException, IOException, ResourceBusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(createBody);
		
		policyCreateInfo.setName(rootNode.path(ResponseConstant.NAME).textValue());
		policyCreateInfo.setDescription(rootNode.path(ResponseConstant.DESCRIPTION).textValue());
		policyCreateInfo.setShared(false);
	}
	
	private String getQosBandwithCreatedBody(QosBandwith bandwith){
		QosBandwithJSON createdQosBandwithJSON = new QosBandwithJSON(bandwith);
		JsonHelper<QosBandwithJSON, String> jsonHelp = new JsonHelper<QosBandwithJSON, String>();
		return jsonHelp.generateJsonBodySimple(createdQosBandwithJSON);
	}
	
	private void makeQosBandwithCreateInfo(String createBody,QosBandwith bandwithCreateInfo) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(createBody);
		
		bandwithCreateInfo.setMax_kbps(rootNode.path(ResponseConstant.MAX_KBPS).intValue()*ParamConstant.MB);
	}
	
	private String getQosPolicyCreatedBody(QosPolicy policy){
		QosPolicyJSON createdQosPolicyJSON = new QosPolicyJSON(policy);
		JsonHelper<QosPolicyJSON, String> jsonHelp = new JsonHelper<QosPolicyJSON, String>();
		return jsonHelp.generateJsonBodySimple(createdQosPolicyJSON);
	}
	
	private QosPolicy getPolicy(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode policieNode = rootNode.path(ResponseConstant.POLICY);
		return getPolicyInfo(policieNode);
	}
	
	private QosBandwith getBandwith(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode bandwithNode = rootNode.path(ResponseConstant.BANDWIDTH_LIMIT_RULE);
		return getBandwithInfo(bandwithNode);
	}
	
	private List<QosBandwith> getBandwiths(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode bandwithsNode = rootNode.path(ResponseConstant.BANDWIDTH_LIMIT_RULES);
		int bandwithsCount = bandwithsNode.size();
		if (0 == bandwithsCount)
			return null;
		List<QosBandwith> bandwiths = new ArrayList<QosBandwith>();
		for (int index = 0; index < bandwithsCount; ++index) {
			QosBandwith bandwith = getBandwithInfo(bandwithsNode.get(index));
			if(null == bandwith)
				continue;
			bandwiths.add(bandwith);
		}
		return bandwiths;
	}
	
	private List<QosPolicy> getPolicies(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode policiesNode = rootNode.path(ResponseConstant.POLICIES);
		int policiesCount = policiesNode.size();
		if (0 == policiesCount)
			return null;
		
		List<QosPolicy> policies = new ArrayList<QosPolicy>();
		for (int index = 0; index < policiesCount; ++index) {
			QosPolicy policy = getPolicyInfo(policiesNode.get(index));
			if(null == policy)
				continue;
			policies.add(policy);
		}

		return policies;
	}
	
	private void setBandwithRules(QosPolicy policy,JsonNode bandwithsNode){
		if(null == bandwithsNode)
			return;
		
		int bandwithsCount = bandwithsNode.size();
		if (0 == bandwithsCount)
			return;
		
		List<QosBandwith> bandwiths = new ArrayList<QosBandwith>();
		List<String> ids = new ArrayList<String>();
		
		for (int index = 0; index < bandwithsCount; ++index) {
			QosBandwith bandwith = getBandwithInfo(bandwithsNode.get(index));
			if(null == bandwith)
				continue;
			bandwiths.add(bandwith);
			ids.add(bandwith.getId());
		}
		policy.setBandwidth_limit_rules(bandwiths);
		policy.setBandwithIds(Util.listToString(ids, ','));
	}
	
	private void setDscpRules(QosPolicy policy,JsonNode dscpsNode){
		if(null == dscpsNode)
			return;
		
		int dscpsCount = dscpsNode.size();
		if (0 == dscpsCount)
			return;
		
		List<QosDscp> dscps = new ArrayList<QosDscp>();
		List<String> ids = new ArrayList<String>();
		
		for (int index = 0; index < dscpsCount; ++index) {
			QosDscp dscp = getDscpInfo(dscpsNode.get(index));
			if(null == dscp)
				continue;
			dscps.add(dscp);
			ids.add(dscp.getId());
		}
		policy.setDscp_marking_rules(dscps);
		policy.setDscpIds(Util.listToString(ids, ','));
	}
	
	private QosBandwith getBandwithInfo(JsonNode bandwithNode){
		if(null == bandwithNode)
			return null;
		QosBandwith bandwith = new QosBandwith();
		bandwith.setId(bandwithNode.path(ResponseConstant.ID).textValue());
		bandwith.setPolicy_id(bandwithNode.path(ResponseConstant.POLICY_ID).textValue());
		bandwith.setMax_kbps(bandwithNode.path(ResponseConstant.MAX_KBPS).intValue());
		bandwith.setMax_burst_kbps(bandwithNode.path(ResponseConstant.MAX_BURST_KBPS).intValue());
		return bandwith;	
	}
	
	private QosDscp getDscpInfo(JsonNode dscpNode){
		if(null == dscpNode)
			return null;
		QosDscp dscp = new QosDscp();
		dscp.setId(dscpNode.path(ResponseConstant.ID).textValue());
		dscp.setPolicy_id(dscpNode.path(ResponseConstant.POLICY_ID).textValue());
		dscp.setDscp_mark(dscpNode.path(ResponseConstant.DSCP_MARK).intValue());
		return dscp;	
	}
	
	
	private QosPolicy getPolicyInfo(JsonNode policyNode){
		if(null == policyNode)
			return null;
		
		QosPolicy policy = new QosPolicy();
		policy.setId(policyNode.path(ResponseConstant.ID).textValue());
		policy.setTenant_id(policyNode.path(ResponseConstant.TENANT_ID).textValue());
		policy.setName(policyNode.path(ResponseConstant.NAME).textValue());
		policy.setDescription(policyNode.path(ResponseConstant.DESCRIPTION).textValue());
		policy.setShared(policyNode.path(ResponseConstant.SHARED).booleanValue());
		setBandwithRules(policy,policyNode.path(ResponseConstant.BANDWIDTH_LIMIT_RULES));
		setDscpRules(policy,policyNode.path(ResponseConstant.DSCP_LIMIT_RULES));
		
		return policy;
	}
}
