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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.SecurityGroupRuleMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.SecurityGroupRuleJSON;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SecurityGroupRule;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.SecurityGroupRuleService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("securityGroupRuleService")
public class SecurityGroupRuleServiceImpl implements SecurityGroupRuleService{
	
	@Resource
	private OSHttpClientUtil client;
	
	@Resource
	private AuthService authService;

	@Autowired
	private SecurityGroupRuleMapper securityGroupRuleMapper;
	
	private Logger log = LogManager.getLogger(SecurityGroupRuleServiceImpl.class);
	
	@Override
	public List<SecurityGroupRule> getSecurityGroupRuleList(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException{
		List<SecurityGroupRule> securityGroupRules = securityGroupRuleMapper.selectAllList();
		if(!Util.isNullOrEmptyList(securityGroupRules))
			return securityGroupRules;
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/security-group-rules", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
        Locale locale = new Locale(ostoken.getLocale());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoGet(url, ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);

		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));

		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				securityGroupRules = getSecurityGroupRules(rs);
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
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				securityGroupRules = getSecurityGroupRules(rs);
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
			throw new ResourceBusinessException(Message.CS_NETWORK_SECURITYGROUP_RULE_GET_FAILED,httpCode,locale);
		}
		
		storeSecurityGroupRules2DB(securityGroupRules);
		return securityGroupRules;
	}
	
	@Override
    public SecurityGroupRule getSecurityGroupRule(String securityGroupRuleId,TokenOs ostoken) throws BusinessException{
		SecurityGroupRule securityGroupRuleFromDB = securityGroupRuleMapper.selectByPrimaryKey(securityGroupRuleId);
		if(null != securityGroupRuleFromDB)
			return securityGroupRuleFromDB;
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/security-group-rules/");
		sb.append(securityGroupRuleId);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);

		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		
		SecurityGroupRule securityGroupRule = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				securityGroupRule = getSecurityGroupRule(rs);
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
			rs = client.httpDoGet(sb.toString(), ostoken.getTokenid());
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				securityGroupRule = getSecurityGroupRule(rs);
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
			throw new ResourceBusinessException(Message.CS_NETWORK_SECURITYGROUP_RULE_DETAIL_GET_FAILED,httpCode,locale);
		}
		
		storeSecurityGroupRule2DB(securityGroupRule);
		return securityGroupRule;
		
	}
	 
	@Override
	public SecurityGroupRule createSecurityGroupRule(String createBody,TokenOs ostoken) throws BusinessException{
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/security-group-rules", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoPost(url, ostoken.getTokenid(),createBody);
		Util.checkResponseBody(rs,locale);

		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		SecurityGroupRule securityGroupRule = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				securityGroupRule = getSecurityGroupRule(rs);
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
			rs = client.httpDoPost(url, ostoken.getTokenid(),createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				securityGroupRule = getSecurityGroupRule(rs);
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
			throw new ResourceBusinessException(Message.CS_NETWORK_SECURITYGROUP_RULE_CREATE_FAILED,httpCode,locale);
		}
		
		//String currentDate = Util.getCurrentDate();
		securityGroupRule.setCreatedAt(Util.getCurrentDate());
//		securityGroupRule.setName(String.format("%s_SecurityGroupRule", currentDate));
//		securityGroupRuleMapper.insertSelective(securityGroupRule);
		return securityGroupRule;
	}
	
	@Override
	public List<SecurityGroupRule> createSecurityGroupRule(List<SecurityGroupRule> rulesCreateInfo,String securityGroupName,TokenOs ostoken) throws BusinessException{
		List<SecurityGroupRule> createdRules = new ArrayList<SecurityGroupRule>();
		int index = 1;
		for(SecurityGroupRule rule : rulesCreateInfo){
			SecurityGroupRuleJSON securityGroupRuleJSON  = new SecurityGroupRuleJSON(rule);
			JsonHelper<SecurityGroupRuleJSON, String> jsonHelp = new JsonHelper<SecurityGroupRuleJSON, String>();
			SecurityGroupRule createdRule = createSecurityGroupRule(jsonHelp.generateJsonBodyWithEmpty(securityGroupRuleJSON),ostoken);
			if(null == createdRule)
				continue;
			createdRule.setName(String.format("%s_rule_%s",securityGroupName,index));
			createdRules.add(createdRule);
			++index;
		}
		return createdRules;
	}
	
	@Override
	public void deleteSecurityGroupRule(String securityGroupRuleId,TokenOs ostoken) throws BusinessException{
	
		Locale locale = new Locale(ostoken.getLocale());
//		checkResource(securityGroupRuleId,locale);
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/security-group-rules/");
		sb.append(securityGroupRuleId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoDelete(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE: {
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
			rs = client.httpDoDelete(sb.toString(), ostoken.getTokenid());
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
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
			throw new ResourceBusinessException(Message.CS_NETWORK_SECURITYGROUP_RULE_DELETE_FAILED,httpCode,locale);
		}
		
		securityGroupRuleMapper.deleteByPrimaryKey(securityGroupRuleId);
	}
	
	private List<SecurityGroupRule> getSecurityGroupRules(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode securityGropRulesNode = rootNode.path(ResponseConstant.SECURITY_GROUP_RULES);
		int securityGroupRulesCount = securityGropRulesNode.size();
		
		if (0 == securityGroupRulesCount)
			return null;
		
		List<SecurityGroupRule> securityGroupRules = new ArrayList<SecurityGroupRule>();
		for(int index = 0; index < securityGroupRulesCount; ++index){
			SecurityGroupRule securityGroupRule = getSecurityGroupRuleInfo(securityGropRulesNode.get(index));
			if(null == securityGroupRule)
				continue;
			securityGroupRules.add(securityGroupRule);
		}
		return securityGroupRules;
	}
	
	private SecurityGroupRule getSecurityGroupRule(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode securityGroupRuleNode = rootNode.path(ResponseConstant.SECURITY_GROUP_RULE);
		return getSecurityGroupRuleInfo(securityGroupRuleNode);
	}
	
	private SecurityGroupRule getSecurityGroupRuleInfo(JsonNode securityGroupRuleNode){
		if(null == securityGroupRuleNode)
			return null;
		SecurityGroupRule securityGroupRule = new SecurityGroupRule();
		securityGroupRule.setId(securityGroupRuleNode.path(ResponseConstant.ID).textValue());
		securityGroupRule.setDirection(securityGroupRuleNode.path(ResponseConstant.DIRECTION).textValue());
		securityGroupRule.setEthertype(securityGroupRuleNode.path(ResponseConstant.ETHERTYPE).textValue());
		securityGroupRule.setPort_range_max(securityGroupRuleNode.path(ResponseConstant.PORT_RANGE_MAX).intValue());
		securityGroupRule.setPort_range_min(securityGroupRuleNode.path(ResponseConstant.PORT_RANGE_MIN).intValue());
		securityGroupRule.setProtocol(securityGroupRuleNode.path(ResponseConstant.PROTOCOL).textValue());
		securityGroupRule.setRemote_group_id(securityGroupRuleNode.path(ResponseConstant.REMOTE_GROUP_ID).textValue());
		securityGroupRule.setRemoteIpPrefix(securityGroupRuleNode.path(ResponseConstant.REMOTE_IP_PREFIX).textValue());
		securityGroupRule.setSecurity_group_id(securityGroupRuleNode.path(ResponseConstant.SECURITY_GROUP_ID).textValue());
		securityGroupRule.setTenantId(securityGroupRuleNode.path(ResponseConstant.TENANT_ID).textValue());
		securityGroupRule.setCreatedAt(Util.getCurrentDate());
		return securityGroupRule;
	}
	
	private void storeSecurityGroupRule2DB(SecurityGroupRule securityGroupRule) {
		if (null == securityGroupRule)
			return;
		securityGroupRuleMapper.insertOrUpdate(securityGroupRule);
//		if (null != securityGroupRuleMapper.selectByPrimaryKey(securityGroupRule.getId()))
//			securityGroupRuleMapper.updateByPrimaryKeySelective(securityGroupRule);
//		else
//			securityGroupRuleMapper.insertSelective(securityGroupRule);
	}
	
	private void storeSecurityGroupRules2DB(List<SecurityGroupRule> securityGroupRules){
		if(Util.isNullOrEmptyList(securityGroupRules))
			return;
		securityGroupRuleMapper.insertOrUpdateBatch(securityGroupRules);
//		for(SecurityGroupRule securityGroupRule : securityGroupRules){
//			storeSecurityGroupRule2DB(securityGroupRule);
//		}
	}
	
//	private void checkResource(String id,Locale locale) throws BusinessException{
//		//check instance sync resource
//		SecurityGroupRule rule = securityGroupRuleMapper.selectByPrimaryKey(id);
//		if(null == rule)
//			return;
//		List<Instance> instances = instanceMapper.selectInstanceBySecurityGroupId(rule.getSecurity_group_id());
//		if(!Util.isNullOrEmptyList(instances))
//			throw new ResourceBusinessException(Message.CS_HAVE_RELATED_INSTANCE_RESOURCE,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
//		return;
//	}
	
}
