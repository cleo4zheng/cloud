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

import com.cloud.cloudapi.dao.common.InstanceMapper;
import com.cloud.cloudapi.dao.common.PortMapper;
import com.cloud.cloudapi.dao.common.QuotaDetailMapper;
import com.cloud.cloudapi.dao.common.QuotaMapper;
import com.cloud.cloudapi.dao.common.SecurityGroupMapper;
import com.cloud.cloudapi.dao.common.SecurityGroupRuleMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.SecurityGroupJSON;
import com.cloud.cloudapi.json.forgui.SecurityGroupRuleJSON;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SecurityGroup;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SecurityGroupRule;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.PortService;
import com.cloud.cloudapi.service.openstackapi.SecurityGroupRuleService;
import com.cloud.cloudapi.service.openstackapi.SecurityGroupService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.StringHelper;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("securityGroupService")
public class SecurityGroupServiceImpl implements SecurityGroupService {
	
	@Resource
	private OSHttpClientUtil client;
	
	@Resource
	private SecurityGroupRuleService securityGroupRuleService;
	
	@Autowired
	private SecurityGroupMapper securityGroupMapper;

	@Autowired
	private SecurityGroupRuleMapper securityGroupRuleMapper;
	
	@Autowired
	private CloudConfig cloudconfig;

	@Autowired
	private QuotaMapper quotaMapper;

	@Autowired
	private PortMapper portMapper;
	
	@Autowired
	private InstanceMapper instanceMapper;
	
	@Autowired
	private QuotaDetailMapper quotaDetailMapper;
	
	@Resource
	private AuthService authService;

	@Resource
	private PortService portService;
	
	private Logger log = LogManager.getLogger(SecurityGroupServiceImpl.class);
	
	public SecurityGroupMapper getSecurityGroupMapper() {
		return securityGroupMapper;
	}

	public void setSecurityGroupMapper(SecurityGroupMapper securityGroupMapper) {
		this.securityGroupMapper = securityGroupMapper;
	}

	public CloudConfig getCloudconfig() {
		return cloudconfig;
	}

	public void setCloudconfig(CloudConfig cloudconfig) {
		this.cloudconfig = cloudconfig;
	}

	public QuotaMapper getQuotaMapper() {
		return quotaMapper;
	}

	public void setQuotaMapper(QuotaMapper quotaMapper) {
		this.quotaMapper = quotaMapper;
	}

	public QuotaDetailMapper getQuotaDetailMapper() {
		return quotaDetailMapper;
	}

	public void setQuotaDetailMapper(QuotaDetailMapper quotaDetailMapper) {
		this.quotaDetailMapper = quotaDetailMapper;
	}
	
	@Override
	public void makeTenantDefaultSecurityGroup(String securityGroupId,TokenOs ostoken){
		try{
			this.getSecurityGroup(securityGroupId, ostoken);
		}catch(Exception e){
			log.error(e);
		}
	}
	
	@Override
	public Boolean hasDefaultSecurityGroup(String tenantId){
		if(null == securityGroupMapper.selectTenantDefaultSecurityGroup(tenantId))
			return false;
		return true;
	}
	
	@Override
	public List<SecurityGroup> getSecurityGroupList(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException
	{
		int limitItems = Util.getLimit(paramMap);
		List<SecurityGroup> securityGroupsFromDB = getSecurityGroups(limitItems,ostoken.getTenantid());
		if(!Util.isNullOrEmptyList(securityGroupsFromDB)){
		//	normalSecurityGroupTime(securityGroupsFromDB);
			setPortsInfo(securityGroupsFromDB);
			return securityGroupsFromDB;
		}
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/security-groups", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
        Locale locale = new Locale(ostoken.getLocale());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoGet(url, ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		List<SecurityGroup> securityGroups = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				securityGroups = getSecurityGroups(rs,ostoken.getTenantid());
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
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,locale);
			try {
				securityGroups = getSecurityGroups(rs,ostoken.getTenantid());
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
			throw new ResourceBusinessException(Message.CS_NETWORK_SECURITYGROUP_GET_FAILED,httpCode,locale);
		}
		
		storeSecurityGroups2DB(securityGroups);
		securityGroupsFromDB = getSecurityGroups(limitItems,ostoken.getTenantid());
		setPortsInfo(securityGroupsFromDB);
		return securityGroupsFromDB;
	//	normalSecurityGroupTime(securityGroups);
	//	return getLimitItems(securityGroups,ostoken.getTenantid(),limitItems);
	}
	
	@Override
	public List<SecurityGroup> getInstanceAttachedSecurityGroup(JsonNode instanceNode,String tenantId) throws BusinessException{
		 JsonNode securityGroupNodes = instanceNode.path(ResponseConstant.SECURITY_GROUPS);
	     int securityGroupCounts = securityGroupNodes.size();
	     if(0 == securityGroupCounts)
	    	 return null;
	     List<String> securityGroupNames = new ArrayList<String>();
	     for(int index = 0 ; index < securityGroupCounts; ++index){
	    	 securityGroupNames.add(securityGroupNodes.get(index).path(ResponseConstant.NAME).textValue());
	     }
	
	     List<SecurityGroup> securityGroups = securityGroupMapper.selectSecurityGroupsByName(securityGroupNames);
	     if(Util.isNullOrEmptyList(securityGroups))
	    	 return null;
	     List<SecurityGroup> tenantSecurityGroups = new ArrayList<SecurityGroup>();
	     for(SecurityGroup securityGroup : securityGroups){
	    	 if(!securityGroup.getTenantId().equals(tenantId))
	    		 continue;
			 securityGroup.setCreatedAt(Util.millionSecond2Date(securityGroup.getMillionSeconds()));
	    	 tenantSecurityGroups.add(securityGroup);
	     }
	     return tenantSecurityGroups;
	}
	
//	private List<SecurityGroup> getLimitItems(List<SecurityGroup> securityGroups,String tenantId,int limit){
//		if(Util.isNullOrEmptyList(securityGroups))
//			return null;
//		List<SecurityGroup> tenantSecuritys = new ArrayList<SecurityGroup>();
//		for(SecurityGroup security : securityGroups){
//			if(!tenantId.equals(security.getTenantId()))
//				continue;
//			tenantSecuritys.add(security);
//		}
//		if(-1 != limit){
//			if(limit <= tenantSecuritys.size())
//				return tenantSecuritys.subList(0, limit);
//		}
//		return tenantSecuritys;
//	}
	
	private void storeSecurityGroups2DB(List<SecurityGroup> securityGroups){
		if(Util.isNullOrEmptyList(securityGroups))
			return;
		securityGroupMapper.insertOrUpdateBatch(securityGroups);
//		for(SecurityGroup securityGroup : securityGroups){
//			if(null != securityGroupMapper.selectByPrimaryKey(securityGroup.getId()))
//				securityGroupMapper.updateByPrimaryKeySelective(securityGroup);
//			else
//				securityGroupMapper.insertSelective(securityGroup);
//		}
	}
	
	private void appendSecurityGroupRuleInfo(SecurityGroup securityGroupFromDB){
		
		String securityGroupRuleIds = securityGroupFromDB.getSecurityGroupRuleIds();
		if(Util.isNullOrEmptyValue(securityGroupRuleIds))
			return;
		List<SecurityGroupRule> rules = securityGroupRuleMapper.selectListBySecurityGroupRuleIds(securityGroupRuleIds.split(","));
		securityGroupFromDB.setSecurityGroupRules(rules);
	}
	
	@Override
	public SecurityGroup getSecurityGroup(String securityGroupId,TokenOs ostoken) throws BusinessException{
		SecurityGroup securityGroupFromDB = securityGroupMapper.selectByPrimaryKey(securityGroupId);
		if(null != securityGroupFromDB){
			appendSecurityGroupRuleInfo(securityGroupFromDB);
		//	securityGroupFromDB.setCreatedAt(Util.millionSecond2Date(securityGroupFromDB.getMillionSeconds()));
		//	securityGroupFromDB.setName(StringHelper.ncr2String(securityGroupFromDB.getName()));
			return securityGroupFromDB;
		}
	
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/security-groups/");
		sb.append(securityGroupId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoGet(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		SecurityGroup securityGroup = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				securityGroup = getSecurityGroup(rs);
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
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				securityGroup = getSecurityGroup(rs);
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
			throw new ResourceBusinessException(Message.CS_NETWORK_SECURITYGROUP_DETAIL_GET_FAILED,httpCode,locale);
		}
		storeSecurityGroup2DB(securityGroup);
	//	securityGroup.setCreatedAt(Util.millionSecond2Date(securityGroup.getMillionSeconds()));
	//	securityGroup.setName(StringHelper.ncr2String(securityGroup.getName()));
		return securityGroup;
	}

	@Override
	public SecurityGroup getSecurityGroupByName(String securityGroupName,TokenOs ostoken) throws BusinessException{
//		List<String> groupNames = new ArrayList<String>();
//		groupNames.add(securityGroupName);
//		List<SecurityGroup> securityGroups = securityGroupMapper.selectSecurityGroupsByName(groupNames);
//		if(Util.isNullOrEmptyList(securityGroups)){
//			return null;
//		}
		SecurityGroup securityGroup = securityGroupMapper.selectTenantSecurityGroupByName(securityGroupName, ostoken.getTenantid());
		appendSecurityGroupRuleInfo(securityGroup);
		return securityGroup;
	}
	
	@Override
	public SecurityGroup createSecurityGroup(String createBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException{

		String region = ostoken.getCurrentRegion();
		SecurityGroup securityGroup = new SecurityGroup();
		List<SecurityGroupRule> rules = new ArrayList<SecurityGroupRule>();
		makeCreateBody(createBody,securityGroup,rules,false);
		checkName(securityGroup.getName(),ostoken);
		String securityGroupCreateBody = getSecurityGroupCreateBody(securityGroup);
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/security-groups", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoPost(url, ostoken.getTokenid(),securityGroupCreateBody);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				securityGroup = getSecurityGroup(rs);
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
			rs = client.httpDoPost(url, tokenid,securityGroupCreateBody);
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				securityGroup = getSecurityGroup(rs);
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
			throw new ResourceBusinessException(Message.CS_NETWORK_SECURITYGROUP_CREATE_FAILED,httpCode,locale);
		}
		
		makeCreateBody(createBody,securityGroup,rules,true);
		securityGroup.setMillionSeconds(Util.getCurrentMillionsecond());
	    try{
	    	List<SecurityGroupRule> createdRules = securityGroupRuleService.createSecurityGroupRule(rules, StringHelper.ncr2String(securityGroup.getName()),ostoken);
	    	securityGroup.setSecurityGroupRules(createdRules);
	    	securityGroup.makeSecurityGroupRuleIds();
	    }catch(Exception e){
	    	//
	    }
	    storeSecurityGroupToDB(securityGroup);
	   // securityGroup.setName(StringHelper.ncr2String(securityGroup.getName()));
		return securityGroup;
	}
	
	private void storeSecurityGroupToDB(SecurityGroup securityGroup){
	    if(null == securityGroup)
	    	return;
		List<SecurityGroupRule> rules = securityGroup.getSecurityGroupRules();
		if(!Util.isNullOrEmptyList(rules)){
			securityGroupRuleMapper.insertOrUpdateBatch(rules);
//			for(SecurityGroupRule rule : rules){
//				if(null == securityGroupRuleMapper.selectByPrimaryKey(rule.getId()))
//					securityGroupRuleMapper.insertSelective(rule);
//				else
//					securityGroupRuleMapper.updateByPrimaryKeySelective(rule);
//			}
		}
		securityGroupMapper.insertOrUpdate(securityGroup);
//		if(null == securityGroupMapper.selectByPrimaryKey(securityGroup.getId()))
//			securityGroupMapper.insertSelective(securityGroup);
//		else
//			securityGroupMapper.updateByPrimaryKeySelective(securityGroup);
	}
	
	@Override
	public SecurityGroup addSecurityGroupRule(String securityGroupId, String createBody, TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException {

		//checkResource(securityGroupId,false,new Locale(ostoken.getLocale()));

		SecurityGroup securityGroup = securityGroupMapper.selectByPrimaryKey(securityGroupId);
		if (null == securityGroup){
			throw new ResourceBusinessException(Message.CS_NETWORK_SECURITYGROUP_ADD_RULE_FAILED,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}

		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(createBody);
		SecurityGroupRule ruleCreateInfo = new SecurityGroupRule();
		makeSecurityGroupRuleInfo(rootNode,securityGroupId,ruleCreateInfo);

		SecurityGroupRuleJSON securityGroupRuleJSON  = new SecurityGroupRuleJSON(ruleCreateInfo);
		JsonHelper<SecurityGroupRuleJSON, String> jsonHelp = new JsonHelper<SecurityGroupRuleJSON, String>();
		SecurityGroupRule createdRule = securityGroupRuleService.createSecurityGroupRule(jsonHelp.generateJsonBodyWithEmpty(securityGroupRuleJSON),ostoken);

		int index = 1;
		if(Util.isNullOrEmptyValue(securityGroup.getSecurityGroupRuleIds()))
			index = 1;
		else
			index = securityGroup.getSecurityGroupRuleIds().split(",").length + 1;
		createdRule.setName(String.format("%s_rule_%s",securityGroup.getName(),index));
		securityGroupRuleMapper.insertSelective(createdRule);
		
		if (Util.isNullOrEmptyValue(securityGroup.getSecurityGroupRuleIds()))
			securityGroup.setSecurityGroupRuleIds(createdRule.getId());
		else
			securityGroup.setSecurityGroupRuleIds(
					securityGroup.getSecurityGroupRuleIds() + "," + createdRule.getId());

		storeSecurityGroupToDB(securityGroup);
		return securityGroup;
	}
	
	@Override
	public SecurityGroup removeSecurityGroupRule(String securityGroupId,String securityGroupRuleId,TokenOs ostoken) throws BusinessException{
		//checkResource(securityGroupId,false,new Locale(ostoken.getLocale()));

		SecurityGroup securityGroup = securityGroupMapper.selectByPrimaryKey(securityGroupId);
		if(null == securityGroup){
    		throw new ResourceBusinessException(Message.CS_NETWORK_SECURITYGROUP_REMOVE_RULE_FAILED,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale())); 
		}	
	//	checkSecurityGroupRuleResource(securityGroupRuleId,new Locale(ostoken.getLocale()));
		securityGroupRuleService.deleteSecurityGroupRule(securityGroupRuleId, ostoken);
		List<String> correctedRuleIds = Util.getCorrectedIdInfo(securityGroup.getSecurityGroupRuleIds(), securityGroupRuleId);
		securityGroup.setSecurityGroupRuleIds(Util.listToString(correctedRuleIds, ','));
		securityGroupMapper.updateByPrimaryKeySelective(securityGroup);
		return securityGroup;
	}
	
	@Override
	public void addSecurityGroupToPort(String securityGroupId,String updateBody,TokenOs ostoken) throws BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(updateBody);
		} catch (Exception e) {
			log.error("error",e);
    		throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale())); 
		} 
		String portId = rootNode.path(ResponseConstant.ID).textValue();
		portService.addSecurityGroup(securityGroupId, portId, ostoken);
	}
	
	@Override
	public void removeSecurityGroupFromPort(String securityGroupId,String updateBody,TokenOs ostoken) throws BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(updateBody);
		} catch (Exception e) {
			log.error("error",e);
    		throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale())); 
		} 
		String portId = rootNode.path(ResponseConstant.ID).textValue();
		portService.removeSecurityGroup(securityGroupId, portId, ostoken);
	}
	
	@Override
	public SecurityGroup updateSecurityGroup(String securityGroupId,String updateBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException{
		String securityGroupUpdateBody = getUpdateBody(updateBody,ostoken);

		// token should have Regioninfo

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/security-groups/");
		sb.append(securityGroupId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoPut(sb.toString(), ostoken.getTokenid(),securityGroupUpdateBody);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		SecurityGroup securityGroup = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				securityGroup = getSecurityGroup(rs);
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
			rs = client.httpDoPut(sb.toString(), ostoken.getTokenid(),securityGroupUpdateBody);
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				securityGroup = getSecurityGroup(rs);
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
			throw new ResourceBusinessException(Message.CS_NETWORK_SECURITYGROUP_UPDATE_FAILED,httpCode,locale);
		}
		
		SecurityGroup securityGroupFromDB = securityGroupMapper.selectByPrimaryKey(securityGroup.getId());
		securityGroup.setMillionSeconds(securityGroupFromDB.getMillionSeconds());
		storeSecurityGroup2DB(securityGroup);
		//securityGroup.setName(StringHelper.ncr2String(securityGroup.getName()));
		return securityGroup;
		
	}
	
	@Override
	public void deleteSecurityGroup(String securityGroupId,TokenOs ostoken) throws BusinessException{
		
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(securityGroupId,true,locale);
		
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/security-groups/");
		sb.append(securityGroupId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoDelete(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
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
			if(httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE){
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
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
			throw new ResourceBusinessException(Message.CS_NETWORK_SECURITYGROUP_DELETE_FAILED,httpCode,locale);
		}
		securityGroupMapper.deleteByPrimaryKey(securityGroupId);
	//	updateSecurityGroupQuota(ostoken,false);
	}
	
	private String getUpdateBody(String updateBody,TokenOs ostoken) throws JsonProcessingException, IOException, BusinessException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(updateBody);
		SecurityGroup securityGroup = new SecurityGroup();
		if (true != rootNode.path(ResponseConstant.NAME).isMissingNode())
			securityGroup.setName(StringHelper.string2Ncr(rootNode.path(ResponseConstant.NAME).textValue()));
		if (true != rootNode.path(ResponseConstant.DESCRIPTION).isMissingNode())
			securityGroup.setDescription(StringHelper.string2Ncr(rootNode.path(ResponseConstant.DESCRIPTION).textValue()));
        checkName(securityGroup.getName(),ostoken);
		SecurityGroupJSON securityGroupJSON = new SecurityGroupJSON(securityGroup);
		JsonHelper<SecurityGroupJSON, String> jsonHelp = new JsonHelper<SecurityGroupJSON, String>();
		return jsonHelp.generateJsonBodySimple(securityGroupJSON);
	}
	
	private List<SecurityGroup> getSecurityGroups(Map<String, String> rs,String tenantId) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode securityGroupsNode = rootNode.path(ResponseConstant.SECURITY_GROUPS);
		int securityGroupsCount = securityGroupsNode.size();
		if (0 == securityGroupsCount)
			return null;
		List<SecurityGroup> securityGroups = new ArrayList<SecurityGroup>();
		for (int index = 0; index < securityGroupsCount; ++index) {
			SecurityGroup securityGroup = getSecurityGroupInfo(securityGroupsNode.get(index));
			if(!tenantId.equals(securityGroup.getTenantId()))
				continue;
			securityGroups.add(securityGroup);
		}
		return securityGroups;
	}
	
	private SecurityGroup getSecurityGroup(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode securityGroupNode = rootNode.path(ResponseConstant.SECURITY_GROUP);
		return getSecurityGroupInfo(securityGroupNode);
	}
	
	private SecurityGroup getSecurityGroupInfo(JsonNode securityGroupNode){
		if (null == securityGroupNode)
			return null;
		SecurityGroup securityGroup = new SecurityGroup();
		securityGroup.setId(securityGroupNode.path(ResponseConstant.ID).textValue());
		securityGroup.setName(securityGroupNode.path(ResponseConstant.NAME).textValue());
		securityGroup.setDescription(securityGroupNode.path(ResponseConstant.DESCRIPTION).textValue());
		securityGroup.setTenantId(securityGroupNode.path(ResponseConstant.TENANT_ID).textValue());
		//securityGroup.setCreatedAt(Util.getCurrentDate());
		securityGroup.setMillionSeconds(Util.time2Millionsecond(Util.getCurrentDate(),ParamConstant.TIME_FORMAT_01));
		JsonNode securityGropRulesNode =  securityGroupNode.path(ResponseConstant.SECURITY_GROUP_RULES);
		if(null != securityGropRulesNode){
			int securityGroupRulesCount = securityGropRulesNode.size();
			List<SecurityGroupRule> securityGroupRules = new ArrayList<SecurityGroupRule>();
		//	List<String> securityGroupRuleIds = new ArrayList<String>();
			for(int index = 0; index < securityGroupRulesCount; ++index){
				SecurityGroupRule securityGroupRule = getSecurityGroupRuleInfo(securityGropRulesNode.get(index));
				if(null == securityGroupRule)
					continue;
				securityGroupRule.setName(String.format("%s_rule_%s", securityGroup.getName(),index+1));
				securityGroupRules.add(securityGroupRule);
				
		//		securityGroupRuleIds.add(securityGroupRule.getId());
			}
			securityGroup.setSecurityGroupRules(securityGroupRules);
			securityGroup.makeSecurityGroupRuleIds();
		}
		return securityGroup;
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
		return securityGroupRule;
	}
	
	private void storeSecurityGroup2DB(SecurityGroup securityGroup){
		if(null == securityGroup)
			return;
		securityGroupMapper.insertOrUpdate(securityGroup);
//		if(null != securityGroupMapper.selectByPrimaryKey(securityGroup.getId()))
//			securityGroupMapper.updateByPrimaryKeySelective(securityGroup);
//		else
//			securityGroupMapper.insertSelective(securityGroup);
		
		List<SecurityGroupRule> rules = securityGroup.getSecurityGroupRules();
		if(Util.isNullOrEmptyList(rules))
			return;
		securityGroupRuleMapper.insertOrUpdateBatch(rules);
//		for(SecurityGroupRule rule : rules){
//			if(null == securityGroupRuleMapper.selectByPrimaryKey(rule.getId()))
//				securityGroupRuleMapper.insertSelective(rule);
//			else
//				securityGroupRuleMapper.updateByPrimaryKeySelective(rule);
//		}
	}
	
	private String getSecurityGroupCreateBody(SecurityGroup securityGroupInfo){
		SecurityGroupJSON securityGroupJSON = new SecurityGroupJSON(securityGroupInfo);
		JsonHelper<SecurityGroupJSON, String> jsonHelp = new JsonHelper<SecurityGroupJSON, String>();
		return jsonHelp.generateJsonBodyWithEmpty(securityGroupJSON);
	}
	
	private void makeSecurityGroupRuleInfo(JsonNode ruleNode,String securityGroupId,SecurityGroupRule rule){
		rule.setDirection(ruleNode.path(ResponseConstant.DIRECTION).textValue());
		rule.setEthertype(ParamConstant.IPV4);
		rule.setProtocol(ruleNode.path(ResponseConstant.PROTOCOL).textValue());
		rule.setSecurity_group_id(securityGroupId);
		rule.setPort_range_min(ruleNode.path(ResponseConstant.PORT_RANGE_MIN).intValue());
		rule.setPort_range_max(ruleNode.path(ResponseConstant.PORT_RANGE_MAX).intValue());
		if(!ruleNode.path(ResponseConstant.CIDR).isMissingNode())
			rule.setCidr(ruleNode.path(ResponseConstant.CIDR).textValue());
		else{
			if(rule.getProtocol().equals("ICMP"))
				rule.setCidr("0.0.0.0/0");
		}
	}
	
	private void makeCreateBody(String createBody,SecurityGroup securityGroupInfo,List<SecurityGroupRule> rules,Boolean setRuleInfo) throws ResourceBusinessException, JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(createBody);
		
		if(true == setRuleInfo){
			JsonNode rulesNode = rootNode.path(ResponseConstant.RULES);
			int rulesNodeSize = rulesNode.size();
			for(int index = 0; index < rulesNodeSize; ++index){
				SecurityGroupRule rule = new SecurityGroupRule();
				makeSecurityGroupRuleInfo(rulesNode.get(index),securityGroupInfo.getId(),rule);
				rules.add(rule);
			}
		}else{
			securityGroupInfo.setName(StringHelper.string2Ncr(rootNode.path(ResponseConstant.NAME).textValue()));
			securityGroupInfo.setDescription(StringHelper.string2Ncr(rootNode.path(ResponseConstant.DESCRIPTION).textValue()));
		}
	}
	
//	private void normalSecurityGroupTime(List<SecurityGroup> securityGroupsFromDB){
//		for(SecurityGroup securityGroup : securityGroupsFromDB){
//			securityGroup.setCreatedAt(Util.millionSecond2Date(securityGroup.getMillionSeconds()));
//			securityGroup.setName(StringHelper.ncr2String(securityGroup.getName()));
//		}
//	}
	
	private List<SecurityGroup> getSecurityGroups(int limitItems,String tenantId){
		if(-1 == limitItems){
			return securityGroupMapper.selectAllByTenantId(tenantId);
		}else{
			return securityGroupMapper.selectAllByTenantIdWithLimit(tenantId,limitItems);
		}
	}
	
	private void checkResource(String id, Boolean delete, Locale locale) throws BusinessException {
		// check instance sync resource
		List<Instance> instances = instanceMapper.selectInstanceBySecurityGroupId(id);
		if (!Util.isNullOrEmptyList(instances))
			throw new ResourceBusinessException(Message.CS_HAVE_RELATED_INSTANCE_RESOURCE,
					ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE, locale);
		
//		if (true == delete) {
//			if (!Util.isNullOrEmptyList(instances))
//				throw new ResourceBusinessException(Message.CS_HAVE_RELATED_INSTANCE_RESOURCE,
//						ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE, locale);
//		} else {
//			if (!Util.isNullOrEmptyList(instances))
//				throw new ResourceBusinessException(Message.CS_HAVE_RELATED_INSTANCE_RESOURCE_ATTACH_WITH_SECURITYGROUP,
//						ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE, locale);
//		}
		return;
	}
	
	private void checkName(String name,TokenOs ostoken)  throws BusinessException{
		if(Util.isNullOrEmptyValue(name))
			return;
		if(name.equals(ParamConstant.DEFAULT))
			throw new ResourceBusinessException(Message.CS_RESOURCE_NAME_SAME_DEFAULT,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		List<SecurityGroup> securityGroups = securityGroupMapper.selectAllByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(securityGroups))
			return;
		for(SecurityGroup securityGroup : securityGroups){
			if(name.equals(securityGroup.getName()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_NAME_IS_SAME,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		return;
	}
	
	private void setPortsInfo(List<SecurityGroup> securityGroupsFromDB){
		for(SecurityGroup sg : securityGroupsFromDB){
			sg.setPorts(portMapper.selectPortsBySecurityGroupId(sg.getId()));
		}
	}
//	private void checkSecurityGroupRuleResource(String id,Locale locale) throws BusinessException{
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
