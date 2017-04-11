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

import com.cloud.cloudapi.dao.common.FirewallMapper;
import com.cloud.cloudapi.dao.common.FirewallRuleMapper;
import com.cloud.cloudapi.dao.common.ResourceCreateProcessMapper;
import com.cloud.cloudapi.dao.common.ResourceEventMapper;
import com.cloud.cloudapi.dao.common.RouterMapper;
import com.cloud.cloudapi.dao.common.SyncResourceMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.FirewallJSON;
import com.cloud.cloudapi.json.forgui.FirewallPolicyJSON;
import com.cloud.cloudapi.json.forgui.FirewallRuleJSON;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Firewall;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FirewallPolicy;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FirewallRule;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceCreateProcess;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceEvent;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Router;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SyncResource;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.FirewallService;
import com.cloud.cloudapi.service.openstackapi.RouterService;
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

@Service("firewallService")
public class FirewallServiceImpl implements FirewallService {

	@Autowired
	private CloudConfig cloudconfig;

	@Autowired
	private RouterMapper routerMapper;
	
	@Autowired
	private FirewallMapper firewallMapper;
	
	@Autowired
	private FirewallRuleMapper firewallRuleMapper;	

	@Autowired
	private SyncResourceMapper syncResourceMapper;
	
	@Autowired
	private ResourceCreateProcessMapper resourceCreateProcessMapper;

	@Autowired
	private ResourceEventMapper resourceEventMapper;
	
	@Resource
	private RouterService routerService;
	
	@Resource
	private AuthService authService;
	
	public CloudConfig getCloudconfig() {
		return cloudconfig;
	}

	public void setCloudconfig(CloudConfig cloudconfig) {
		this.cloudconfig = cloudconfig;
	}

	@Resource
	private OSHttpClientUtil client;
	
	private Logger log = LogManager.getLogger(FirewallServiceImpl.class);
	
	@Override
	public List<Firewall> getFirewalls(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {
		int limitItems = Util.getLimit(paramMap);
		List<Firewall> firewallsFromDB = getFirewallsFromDB(ostoken.getTenantid(),limitItems);
		if(!Util.isNullOrEmptyList(firewallsFromDB)){
			normalFirewallCreatedTime(firewallsFromDB);
			return firewallsFromDB;
		}
			
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/fw/firewalls", paramMap);
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		
		List<Firewall> firewalls = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				firewalls = getFirewalls(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				firewalls = getFirewalls(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		/*
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_FIREWALL_GET_FAILED,httpCode,locale);
			*/
		}
		if(null == firewalls)
			return null;
		storeFirewallsToDB(firewalls);
		normalFirewallCreatedTime(firewalls);
		return getLimitItems(firewalls,ostoken.getTenantid(),limitItems);
	}

	@Override
	public Firewall getFirewall(String firewallId, TokenOs ostoken)
			throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		Firewall firewall = firewallMapper.selectByPrimaryKey(firewallId);
		if(null != firewall){
			makeFirewallInfo(firewall,ostoken);
		//	firewall.setCreatedAt(Util.millionSecond2Date(firewall.getMillionSeconds()));
			return firewall;
		}
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/fw/firewalls/");
		sb.append(firewallId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				firewall = getFirewall(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				firewall = getFirewall(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_FIREWALL_GET_DETAIL_FAILED,httpCode,locale);
		}
		
		addFirewallRuleInfo(firewall,ostoken);
		storeFirewallToDB(firewall);
//		firewall.setCreatedAt(Util.millionSecond2Date(firewall.getMillionSeconds()));
		return firewall;
	}

	@Override
	public Firewall createFirewall(String createBody, TokenOs ostoken)
			throws BusinessException, JsonProcessingException, IOException {

		Firewall createdFirewall = new Firewall();
		List<FirewallRule> existedRules = new ArrayList<FirewallRule>();
		List<FirewallRule> newRules = new ArrayList<FirewallRule>();

		makeFirewallCreateInfo(createBody, createdFirewall, existedRules, newRules);
		checkName(createdFirewall.getName(),ostoken);
		newRules = createFirewallRules(newRules, ostoken);
		existedRules.addAll(newRules);
		FirewallPolicy firewallPolicy = createFirewallPolicy(existedRules, ostoken);
		createdFirewall.setFirewall_policy_id(firewallPolicy.getId());
	//	createdFirewall.setAdmin_state_up(true);
		createBody = getFirewallCreatedBody(createdFirewall);

		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPost(url + "/v2.0/fw/firewalls", headers, createBody);
		Util.checkResponseBody(rs,locale);
		
		Firewall firewall = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				firewall = getFirewall(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				deletePolicyAndRuleForFirewall(firewallPolicy, newRules, ostoken);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			rs = client.httpDoPost(url + "/v2.0/fw/firewalls", headers, createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				firewall = getFirewall(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				deletePolicyAndRuleForFirewall(firewallPolicy, newRules, ostoken);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE: {
			deletePolicyAndRuleForFirewall(firewallPolicy, newRules, ostoken);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		}
		case ParamConstant.NOT_FOUND_RESPONSE_CODE: {
			deletePolicyAndRuleForFirewall(firewallPolicy, newRules, ostoken);
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		}
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE: {
			deletePolicyAndRuleForFirewall(firewallPolicy, newRules, ostoken);
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		}
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE: {
			deletePolicyAndRuleForFirewall(firewallPolicy, newRules, ostoken);
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		}
		default: {
			deletePolicyAndRuleForFirewall(firewallPolicy, newRules, ostoken);
			throw new ResourceBusinessException(Message.CS_FIREWALL_CREATE_FAILED,httpCode,locale);
		}
		}

		updateSyncResourceInfo(ostoken.getTenantid(),firewall.getId(),Util.listToString(createdFirewall.getRouter_ids(), ','),ParamConstant.ACTIVE_STATUS,ParamConstant.FIREWALL,ostoken.getCurrentRegion(),null,firewall.getName());
		normalFirewallInfo(firewall, existedRules, firewallPolicy.getId());
		storeFirewallToDB(firewall);
		createdFirewall.setId(firewall.getId());
		updateRouterInfo(createdFirewall,true);
		storeResourceEventInfo(ostoken.getTenantid(),firewall.getId(),ParamConstant.FWAAS,null,ParamConstant.ACTIVE_STATUS,firewall.getMillionSeconds());
		return firewall;
	}

	@Override
	public Firewall updateFirewall(String firewalId, String updateBody, TokenOs ostoken)
			throws BusinessException, JsonProcessingException, IOException {
		
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(firewalId,false,locale);
		
		Firewall updatedFirewall = new Firewall();
		makeFirewallCreateInfo(updateBody, updatedFirewall, null, null);
		checkName(updatedFirewall.getName(),ostoken);
		String firewallUpdateBody = getFirewallCreatedBody(updatedFirewall);
        Boolean adminState = updatedFirewall.getAdmin_state_up();
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/fw/firewalls/");
		sb.append(firewalId);
		Map<String, String> rs = client.httpDoPut(sb.toString(), headers, firewallUpdateBody);
		Util.checkResponseBody(rs, locale);

		Firewall firewall = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				firewall = getFirewall(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			rs = client.httpDoPut(sb.toString(), headers, firewallUpdateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				firewall = getFirewall(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_FIREWALL_UPDATE_FAILED,httpCode,locale);
		}

		updatedFirewall = firewallMapper.selectByPrimaryKey(firewalId);
		if(null != updatedFirewall){
			firewall.setRuleIds(updatedFirewall.getRuleIds());
			firewall.setMillionSeconds(updatedFirewall.getMillionSeconds());
		}
		storeFirewallToDB(firewall);
		makeFirewallInfo(firewall,ostoken);
		String firewallState = ParamConstant.ACTIVE_STATUS;
		if(Util.isNullOrEmptyValue(firewall.getRouterIds()))
			firewallState = ParamConstant.INACTIVE_STATUS;
		if(null == adminState || true == adminState){
			updateSyncResourceInfo(ostoken.getTenantid(),firewall.getId(),updatedFirewall.getRouterIds(),firewallState,ParamConstant.FIREWALL,ostoken.getCurrentRegion(),"admin_state_up:1",firewall.getName());
			storeResourceEventInfo(ostoken.getTenantid(),firewall.getId(),ParamConstant.FWAAS,updatedFirewall.getStatus(),firewallState,Util.getCurrentMillionsecond());
		}else{
			updateSyncResourceInfo(ostoken.getTenantid(),firewall.getId(),updatedFirewall.getRouterIds(),ParamConstant.DOWN_STATUS,ParamConstant.FIREWALL,ostoken.getCurrentRegion(),"admin_state_up:0",firewall.getName());
			storeResourceEventInfo(ostoken.getTenantid(),firewall.getId(),ParamConstant.FWAAS,updatedFirewall.getStatus(),ParamConstant.DOWN_STATUS,Util.getCurrentMillionsecond());
		}
		return firewall;
	}
	
	@Override
	public Firewall removeRouter(String firewalId,String routerId,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(firewalId,false,locale);
		
		Firewall existedFirewall = firewallMapper.selectByPrimaryKey(firewalId);
		if(null == existedFirewall || Util.isNullOrEmptyValue(existedFirewall.getRouterIds()))
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
		
		
		List<String> routersId = Util.getCorrectedIdInfo(existedFirewall.getRouterIds(), routerId);
		String firewallUpdateBody = null;
		Firewall updatedFirewall = new Firewall();
		if(Util.isNullOrEmptyList(routersId)){
			StringBuilder sb = new StringBuilder();
			sb.append("{\"firewall\":{");
			sb.append("\"router_ids\":[]");
			sb.append("}}");
			firewallUpdateBody = sb.toString();
		}else{
			updatedFirewall.setRouter_ids(routersId);
			firewallUpdateBody = getFirewallCreatedBody(updatedFirewall);	
		}

		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/fw/firewalls/");
		sb.append(firewalId);
		Map<String, String> rs = client.httpDoPut(sb.toString(), headers, firewallUpdateBody);
		Util.checkResponseBody(rs, locale);

		Firewall firewall = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				firewall = getFirewall(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			rs = client.httpDoPut(sb.toString(), headers, firewallUpdateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				firewall = getFirewall(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_FIREWALL_UPDATE_FAILED,httpCode,locale);
		}

		updatedFirewall = firewallMapper.selectByPrimaryKey(firewalId);
		if(null != updatedFirewall){
			firewall.setRuleIds(updatedFirewall.getRuleIds());
			firewall.setMillionSeconds(updatedFirewall.getMillionSeconds());
		}
		storeFirewallToDB(firewall);
		makeFirewallInfo(firewall,ostoken);
		String firewallState = ParamConstant.ACTIVE_STATUS;
		if(Util.isNullOrEmptyValue(firewall.getRouterIds()))
			firewallState = ParamConstant.INACTIVE_STATUS;
	
		updateSyncResourceInfo(ostoken.getTenantid(),firewall.getId(),updatedFirewall.getRouterIds(),firewallState,ParamConstant.FIREWALL,ostoken.getCurrentRegion(),"",firewall.getName());
		storeResourceEventInfo(ostoken.getTenantid(),firewall.getId(),ParamConstant.FWAAS,updatedFirewall.getStatus(),firewallState,Util.getCurrentMillionsecond());
		
		return firewall;
	}
	
	@Override
	public Firewall bindRouter(String firewalId,String updateBody,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		checkFirewallState(firewalId,locale);
		checkResource(firewalId,false,locale);
		
		Firewall existedFirewall = firewallMapper.selectByPrimaryKey(firewalId);
		List<String> routersId = new ArrayList<String>();
		if(null != existedFirewall){
			List<String> existedRoutersId = Util.stringToList(existedFirewall.getRouterIds(), ",");
			if(!Util.isNullOrEmptyList(existedRoutersId))
				routersId.addAll(existedRoutersId);
		}
		
		Firewall updatedFirewall = new Firewall();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode;
		try {
			rootNode = mapper.readTree(updateBody);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
		}
		
		JsonNode routerIdsNoe = rootNode.path(ResponseConstant.ROUTER_ID);
		int routersCount = routerIdsNoe.size();
		for (int index = 0; index < routersCount; ++index) {
			if(routersId.contains(routerIdsNoe.get(index).textValue()))
				throw new ResourceBusinessException(Message.CS_HAVE_SAME_ROUTER_RESOURCE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			routersId.add(routerIdsNoe.get(index).textValue());
		}
	
		updatedFirewall.setRouter_ids(routersId);
		String firewallUpdateBody = getFirewallCreatedBody(updatedFirewall);
		
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/fw/firewalls/");
		sb.append(firewalId);
		Map<String, String> rs = client.httpDoPut(sb.toString(), headers, firewallUpdateBody);
		Util.checkResponseBody(rs, locale);

		Firewall firewall = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				firewall = getFirewall(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			rs = client.httpDoPut(sb.toString(), headers, firewallUpdateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				firewall = getFirewall(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_FIREWALL_UPDATE_FAILED,httpCode,locale);
		}

		updatedFirewall = firewallMapper.selectByPrimaryKey(firewalId);
		if(null != updatedFirewall){
			firewall.setRuleIds(updatedFirewall.getRuleIds());
			firewall.setMillionSeconds(updatedFirewall.getMillionSeconds());
		}
		storeFirewallToDB(firewall);
		makeFirewallInfo(firewall,ostoken);
		String firewallState = ParamConstant.ACTIVE_STATUS;
		if(Util.isNullOrEmptyValue(firewall.getRouterIds()))
			firewallState = ParamConstant.INACTIVE_STATUS;
	
		updateSyncResourceInfo(ostoken.getTenantid(),firewall.getId(),updatedFirewall.getRouterIds(),firewallState,ParamConstant.FIREWALL,ostoken.getCurrentRegion(),"",firewall.getName());
		storeResourceEventInfo(ostoken.getTenantid(),firewall.getId(),ParamConstant.FWAAS,updatedFirewall.getStatus(),firewallState,Util.getCurrentMillionsecond());
		
		return firewall;
	}
	
	@Override
	public void deleteFirewall(String firewallId,TokenOs ostoken) throws BusinessException{
		
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(firewallId,true,locale);
		
		deleteRelatedFirewallInfo(firewallId,ostoken);
		String region = ostoken.getCurrentRegion();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/fw/firewalls/");
		sb.append(firewallId);
		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);

		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
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
			rs = client.httpDoDelete(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if(httpCode == ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
				break;
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		}
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
//		default:
//			throw new ResourceBusinessException(Message.CS_FIREWALL_DELETE_FAILED,httpCode,locale);
		}
		
		Firewall firewall = firewallMapper.selectByPrimaryKey(firewallId);
		String policyId = firewall.getFirewall_policy_id();	
		firewallMapper.deleteByPrimaryKey(firewallId);
		try{
			deleteFirewallPolicy(policyId, ostoken);	
		}catch(Exception e){
			log.error(e);
		}
		updateRouterInfo(firewall,false);
		updateSyncResourceInfo(ostoken.getTenantid(),firewall.getId(),firewall.getRouterIds(),ParamConstant.DELETED_STATUS,ParamConstant.FIREWALL,ostoken.getCurrentRegion(),null,firewall.getName());
		storeResourceEventInfo(ostoken.getTenantid(),firewall.getId(),ParamConstant.FWAAS,firewall.getStatus(),ParamConstant.DELETED_STATUS,Util.getCurrentMillionsecond());
	//	deleteFirewallFromDB(firewallId);
		return;
	}
	
	private void deleteRelatedFirewallInfo(String firewallId,TokenOs ostoken) throws BusinessException {
		Firewall firewall = firewallMapper.selectByPrimaryKey(firewallId);
		if (null == firewall)
			return;
		
		String policyId = firewall.getFirewall_policy_id();	
//		
//		if(!Util.isNullOrEmptyValue(policyId)){
//			deleteFirewallPolicy(policyId, ostoken, response);
//		}
		
		String firewallRuleIds = firewall.getRuleIds();
		if(!Util.isNullOrEmptyValue(firewallRuleIds)){
			int waitTime = Integer.parseInt(cloudconfig.getSystemWaitTime());
			String[] firewallRuleIdArray = firewallRuleIds.split(",");
			for(int index = 0; index < firewallRuleIdArray.length; ++index){
				StringBuilder sb = new StringBuilder();
				sb.append("{\"firewall_rule_id\":\"");
				sb.append(firewallRuleIdArray[index]);
				sb.append("\"}");
				removeFirewallRuleFromPolicy(policyId, sb.toString(), ostoken);
				try {
					Thread.sleep(waitTime);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					log.error(e);
				}
				deleteFirewallRule(firewallRuleIdArray[index], ostoken);
				try {
					Thread.sleep(waitTime);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					log.error(e);
				}
			}
		}
		
//		String policyId = firewall.getFirewall_policy_id();
//		if(!Util.isNullOrEmptyValue(policyId)){
//			deleteFirewallPolicy(policyId, ostoken, response);
//		}
		
	}
	
//	private void deleteFirewallFromDB(String firewallId){
//		Firewall firewall = firewallMapper.selectByPrimaryKey(firewallId);
//		if(null == firewall)
//			return;
//		try{
//			String firewallRuleIds = firewall.getRuleIds();
//			if(!Util.isNullOrEmptyValue(firewallRuleIds)){
//				String[] firewallRuleIdArray = firewallRuleIds.split(",");
//				firewallRuleMapper.deleteRulesById(firewallRuleIdArray);
//			}
//			firewallMapper.deleteByPrimaryKey(firewallId);	
//		}catch(Exception e){
//			//TODO
//		}
//	}
	
	@Override
	public List<FirewallPolicy> getFirewallPolices(Map<String, String> paramMap, TokenOs ostoken) throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/fw/firewall_policies", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		
		List<FirewallPolicy> firewallPolicies = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				firewallPolicies = getFirewallPolices(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				firewallPolicies = getFirewallPolices(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_FIREWALL_POLICY_DETAIL_GET_FAILED,httpCode,locale);
		}
		return firewallPolicies;
	}

	@Override
	public FirewallPolicy getFirewallPolicy(String firewallPolicyId, TokenOs ostoken)
			throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/fw/firewall_policies/");
		sb.append(firewallPolicyId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		
		FirewallPolicy firewallPolicy = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				firewallPolicy = getFirewallPolicy(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				firewallPolicy = getFirewallPolicy(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_FIREWALL_POLICY_DETAIL_GET_FAILED,httpCode,locale);
		}
		return firewallPolicy;
	}

	@Override
	public FirewallPolicy createFirewallPolicy(String createBody, TokenOs ostoken)
			throws BusinessException {
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPost(url + "/v2.0/fw/firewall_policies", headers, createBody);
		Util.checkResponseBody(rs,locale);

		FirewallPolicy firewallPolicy = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				firewallPolicy = getFirewallPolicy(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			rs = client.httpDoPost(url + "/v2.0/fw/firewall_policies", headers, createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				firewallPolicy = getFirewallPolicy(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_FIREWALL_POLICY_CREATE_FAILED,httpCode,locale);
		}
		return firewallPolicy;
	}


	@Override
	public void deleteFirewallPolicy(String firewallPolicyId,TokenOs ostoken) throws BusinessException{
		String region = ostoken.getCurrentRegion();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/fw/firewall_policies/");
		sb.append(firewallPolicyId);
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);

		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		
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
			rs = client.httpDoDelete(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if(httpCode == ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
				break;
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		}
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
//		default:
//			throw new ResourceBusinessException(Message.CS_FIREWALL_POLICY_DELETE_FAILED,httpCode,locale);
		}
		return;
	}
	
	@Override
	public FirewallPolicy addFirewallRuleToPolicy(String policyId, String createBody, TokenOs ostoken) throws BusinessException {

		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/fw/firewall_policies/");
		sb.append(policyId);
		sb.append("/insert_rule");
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPut(sb.toString(), headers,createBody);
		Util.checkResponseBody(rs,locale);
		
		FirewallPolicy firewallPolicy = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				firewallPolicy = getFirewallPolicy(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			rs = client.httpDoPut(sb.toString(), headers,createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				firewallPolicy = getFirewallPolicy(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_FIREWALL_ADD_RULE_TO_POLICY_FAILED,httpCode,locale);
		}
		return firewallPolicy;
	}

	@Override
	public FirewallPolicy removeFirewallRuleFromPolicy(String policyId,String removeBody,TokenOs ostoken) throws BusinessException{
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/fw/firewall_policies/");
		sb.append(policyId);
		sb.append("/remove_rule");
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPut(sb.toString(), headers,removeBody);
		Util.checkResponseBody(rs,locale);
		
		FirewallPolicy firewallPolicy = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
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
			rs = client.httpDoPut(sb.toString(), headers,removeBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_FIREWALL_REMOVE_RULE_FROM_POLICY_FAILED,httpCode,locale);
			break;
		}
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
//		default:
//			throw new ResourceBusinessException(Message.CS_FIREWALL_REMOVE_RULE_FROM_POLICY_FAILED,httpCode,locale);
		}
		return firewallPolicy;
	}
	
	@Override
	public List<FirewallRule> getFirewallRules(Map<String, String> paramMap, TokenOs ostoken) throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		List<FirewallRule> firewallRules = firewallRuleMapper.selectAllByTenantId(ostoken.getTenantid());
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/fw/firewall_rules", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				firewallRules = getFirewallRules(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_FIREWALL_REMOVE_RULE_FROM_POLICY_FAILED,httpCode,locale);
			try {
				firewallRules = getFirewallRules(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_FIREWALL_RULE_GET_FAILED,httpCode,locale);
		}
		return firewallRules;

	}

	@Override
	public FirewallRule getFirewallRule(String firewallRuleId, TokenOs ostoken)
			throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		FirewallRule firewallRule = firewallRuleMapper.selectByPrimaryKey(firewallRuleId);
		if(null != firewallRule)
			return firewallRule;
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/fw/firewall_rules/");
		sb.append(firewallRuleId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				firewallRule = getFirewallRule(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_FIREWALL_REMOVE_RULE_FROM_POLICY_FAILED,httpCode,locale);
			try {
				firewallRule = getFirewallRule(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_FIREWALL_RULE_DETAIL_GET_FAILED,httpCode,locale);
		}
		
		storeFirewallRuleToDB(firewallRule);
		return firewallRule;
	}

	@Override
	public FirewallRule createFirewallRule(String createBody, TokenOs ostoken)
			throws BusinessException {
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPost(url + "/v2.0/fw/firewall_rules", headers, createBody);
		Util.checkResponseBody(rs,locale);

		FirewallRule firewallRule = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				firewallRule = getFirewallRule(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			rs = client.httpDoPost(url + "/v2.0/fw/firewall_rules", headers, createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_FIREWALL_REMOVE_RULE_FROM_POLICY_FAILED,httpCode,locale);
			try {
				firewallRule = getFirewallRule(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_FIREWALL_RULE_CREATE_FAILED,httpCode,locale);
		}
		
		storeFirewallRuleToDB(firewallRule);
		return firewallRule;
	}

	@Override
	public void deleteFirewallRule(String firewallRuleId,TokenOs ostoken) throws BusinessException{
		String region = ostoken.getCurrentRegion();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/fw/firewall_rules/");
		sb.append(firewallRuleId);
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);

		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE: {
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoDelete(sb.toString(), headers);
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			if(httpCode == ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
				break;
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		}
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
//		default:
//			throw new ResourceBusinessException(Message.CS_FIREWALL_RULE_DELETE_FAILED,httpCode,locale);
		}
		
		firewallRuleMapper.deleteByPrimaryKey(firewallRuleId);
		return;
	}
	
	private FirewallRule getFirewallRule(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode firewallRuleNode = rootNode.path(ResponseConstant.FIREWALL_RULE);
		return getFirewallRuleInfo(firewallRuleNode);
	}

	private FirewallPolicy getFirewallPolicy(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode firewallPolicyNode = rootNode.path(ResponseConstant.FIREWALL_POLICY);
		return getFirewallPolicyInfo(firewallPolicyNode);
	}

	private Firewall getFirewall(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode firewallNode = rootNode.path(ResponseConstant.FIREWALL);
		return getFirewallInfo(firewallNode);
	}

	private Firewall getFirewallInfo(JsonNode firewallNode) {
		if (null == firewallNode)
			return null;
		Firewall firewall = new Firewall();
		firewall.setId(firewallNode.path(ResponseConstant.ID).textValue());
		firewall.setName(firewallNode.path(ResponseConstant.NAME).textValue());
		firewall.setAdmin_state_up(firewallNode.path(ResponseConstant.ADMIN_STATE_UP).booleanValue());
		firewall.setDescription(firewallNode.path(ResponseConstant.DESCRIPTION).textValue());
		firewall.setFirewall_policy_id(firewallNode.path(ResponseConstant.FIREWALL_POLICY_ID).textValue());
		firewall.setStatus(firewallNode.path(ResponseConstant.STATUS).textValue());
		firewall.setTenant_id(firewallNode.path(ResponseConstant.TENANT_ID).textValue());

		// get router id
		JsonNode routerIdNode = firewallNode.path(ResponseConstant.ROUTER_IDS);
		if (null != routerIdNode) {
			int routersCount = routerIdNode.size();
			List<String> routersId = new ArrayList<String>();
			for (int index = 0; index < routersCount; ++index) {
				routersId.add(routerIdNode.get(index).textValue());
			}
			firewall.setRouterIds(Util.listToString(routersId, ','));
		}
		return firewall;
	}

	private FirewallRule getFirewallRuleInfo(JsonNode firewallRuleNoe) {
		if (null == firewallRuleNoe)
			return null;
		FirewallRule firewallRule = new FirewallRule();
		firewallRule.setId(firewallRuleNoe.path(ResponseConstant.ID).textValue());
		firewallRule.setDescription(firewallRuleNoe.path(ResponseConstant.DESCRIPTION).textValue());
		firewallRule.setDestination_ip_address(firewallRuleNoe.path(ResponseConstant.DESTINATION_IP_ADDRESS).textValue());
		firewallRule.setDestination_port(firewallRuleNoe.path(ResponseConstant.DESTINATION_PORT).textValue());
		firewallRule.setFirewall_policy_id(firewallRuleNoe.path(ResponseConstant.FIREWALL_POLICY_ID).textValue());
		firewallRule.setIp_version(firewallRuleNoe.path(ResponseConstant.IP_VERSION).textValue());
		firewallRule.setName(firewallRuleNoe.path(ResponseConstant.NAME).textValue());
		firewallRule.setPosition(firewallRuleNoe.path(ResponseConstant.POSITION).textValue());
		firewallRule.setProtocol(firewallRuleNoe.path(ResponseConstant.PROTOCOL).textValue());
		firewallRule.setShared(firewallRuleNoe.path(ResponseConstant.SHARED).booleanValue());
		firewallRule.setSource_ip_address(firewallRuleNoe.path(ResponseConstant.SOURCE_IP_ADDRESS).textValue());
		firewallRule.setSource_port(firewallRuleNoe.path(ResponseConstant.SOURCE_PORT).textValue());
		firewallRule.setTenant_id(firewallRuleNoe.path(ResponseConstant.TENANT_ID).textValue());
		firewallRule.setAction(firewallRuleNoe.path(ResponseConstant.ACTION).textValue());
		return firewallRule;
	}

	private List<FirewallRule> getFirewallRules(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode firewallRulesNode = rootNode.path(ResponseConstant.FIREWALL_RULES);
		int firewallRulesCount = firewallRulesNode.size();
		if (0 == firewallRulesCount)
			return null;

		List<FirewallRule> firewallRules = new ArrayList<FirewallRule>();
		for (int index = 0; index < firewallRulesCount; ++index) {
			FirewallRule firewallRule = getFirewallRuleInfo(firewallRulesNode.get(index));
			if (null == firewallRule)
				continue;
			firewallRules.add(firewallRule);
		}

		return firewallRules;
	}

	private List<Firewall> getFirewalls(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode firewallsNode = rootNode.path(ResponseConstant.FIREWALLS);
		int firewallsCount = firewallsNode.size();
		if (0 == firewallsCount)
			return null;

		List<Firewall> firewalls = new ArrayList<Firewall>();
		for (int index = 0; index < firewallsCount; ++index) {
			Firewall firewall = getFirewallInfo(firewallsNode.get(index));
			if (null == firewall)
				continue;
			firewalls.add(firewall);
		}

		return firewalls;
	}

	private FirewallPolicy getFirewallPolicyInfo(JsonNode firewallPolicyNode) {
		if (null == firewallPolicyNode)
			return null;
		FirewallPolicy firewallPolicy = new FirewallPolicy();
		firewallPolicy.setId(firewallPolicyNode.path(ResponseConstant.ID).textValue());
		firewallPolicy.setAudited(firewallPolicyNode.path(ResponseConstant.AUDITED).booleanValue());
		firewallPolicy.setDescription(firewallPolicyNode.path(ResponseConstant.DESCRIPTION).textValue());
		firewallPolicy.setName(firewallPolicyNode.path(ResponseConstant.NAME).textValue());
		firewallPolicy.setShared(firewallPolicyNode.path(ResponseConstant.SHARED).booleanValue());
		firewallPolicy.setTenant_id(firewallPolicyNode.path(ResponseConstant.TENANT_ID).textValue());

		JsonNode firewallRuleNode = firewallPolicyNode.path(ResponseConstant.FIREWALL_RULES);
		if (null != firewallRuleNode) {
			int rulesCount = firewallRuleNode.size();
			List<String> ruleIds = new ArrayList<String>();
			for (int index = 0; index < rulesCount; ++index) {
				ruleIds.add(firewallRuleNode.get(index).textValue());
			}
			firewallPolicy.setRuleIds(Util.listToString(ruleIds, ','));
			firewallPolicy.setFirewall_rules(ruleIds);
		}

		return firewallPolicy;
	}

	private List<FirewallPolicy> getFirewallPolices(Map<String, String> rs)
			throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode firewallPoliciesNode = rootNode.path(ResponseConstant.FIREWALL_POLICIES);
		int firewallPoliciesCount = firewallPoliciesNode.size();
		if (0 == firewallPoliciesCount)
			return null;

		List<FirewallPolicy> firewallPolices = new ArrayList<FirewallPolicy>();
		for (int index = 0; index < firewallPoliciesCount; ++index) {
			FirewallPolicy firewallPolicy = getFirewallPolicyInfo(firewallPoliciesNode.get(index));
			if (null == firewallPolicy)
				continue;
			firewallPolices.add(firewallPolicy);
		}
		return firewallPolices;
	}

	private List<FirewallRule> createFirewallRules(List<FirewallRule> newRulesInfo, TokenOs ostoken) throws BusinessException {
		if (Util.isNullOrEmptyList(newRulesInfo))
			return null;
		List<FirewallRule> createdRules = new ArrayList<FirewallRule>();
		for (FirewallRule createdFirewallRuleInfo : newRulesInfo) {
			FirewallRuleJSON createdFirewallRuleJSON = new FirewallRuleJSON(createdFirewallRuleInfo);
			JsonHelper<FirewallRuleJSON, String> jsonHelp = new JsonHelper<FirewallRuleJSON, String>();
			String createdFirewallRuleBody = jsonHelp.generateJsonBodySimple(createdFirewallRuleJSON);
			createdRules.add(createFirewallRule(createdFirewallRuleBody, ostoken));
		}
		return createdRules;
	}
	
	private FirewallPolicy createFirewallPolicy(List<FirewallRule> createdRules, TokenOs ostoken) throws BusinessException {
		if (Util.isNullOrEmptyList(createdRules))
			return null;
		List<String> fireRuleIds = new ArrayList<String>();
		for(FirewallRule firewallRule : createdRules){
			fireRuleIds.add(firewallRule.getId());
		}
		FirewallPolicy createdFirewallPolicy = new FirewallPolicy();
		createdFirewallPolicy.setFirewall_rules(fireRuleIds);
		String name = String.format("firewall_policy_%s", Util.getCurrentDate());
		createdFirewallPolicy.setName(name);
		FirewallPolicyJSON createdFirewallPolicyJSON = new FirewallPolicyJSON(createdFirewallPolicy);
		JsonHelper<FirewallPolicyJSON, String> jsonHelp = new JsonHelper<FirewallPolicyJSON, String>();
		String  createdFirewallPolicyBody = jsonHelp.generateJsonBodySimple(createdFirewallPolicyJSON);
		return createFirewallPolicy(createdFirewallPolicyBody, ostoken);
	}
	
	private String getFirewallCreatedBody(Firewall firewall){
		FirewallJSON createdFirewallJSON = new FirewallJSON(firewall);
		JsonHelper<FirewallJSON, String> jsonHelp = new JsonHelper<FirewallJSON, String>();
		return jsonHelp.generateJsonBodySimple(createdFirewallJSON);
	}
	
	private void makeFirewallCreateInfo(String createBody,Firewall createdFirewall,List<FirewallRule> existedRules,List<FirewallRule> newRules) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(createBody);
		
		if(!rootNode.path(ResponseConstant.NAME).isMissingNode())
			createdFirewall.setName(StringHelper.string2Ncr(rootNode.path(ResponseConstant.NAME).textValue()));
		if(!rootNode.path(ResponseConstant.DESCRIPTION).isMissingNode())
			createdFirewall.setDescription(StringHelper.string2Ncr(rootNode.path(ResponseConstant.DESCRIPTION).textValue()));
		if(!rootNode.path(ResponseConstant.ADMIN_STATE_UP).isMissingNode())
			createdFirewall.setAdmin_state_up(Util.string2Boolean(rootNode.path(ResponseConstant.ADMIN_STATE_UP).textValue()));
		if(null == existedRules)
			return;
		
		JsonNode routerIdsNoe = rootNode.path(ResponseConstant.ROUTER_IDS);
		int routersCount = routerIdsNoe.size();
		List<String> routersId = new ArrayList<String>();
		for (int index = 0; index < routersCount; ++index) {
			routersId.add(routerIdsNoe.get(index).textValue());
		}
		createdFirewall.setRouter_ids(routersId);
	
		JsonNode ruleNodes = rootNode.path(ResponseConstant.FIREWALL_RULES);
		int firewallRulesCount = ruleNodes.size();
		if (0 == firewallRulesCount)
			return;

		for (int index = 0; index < firewallRulesCount; ++index) {
			FirewallRule firewallRule = getFirewallRuleInfo(ruleNodes.get(index));
			if(null == firewallRule)
				continue;
			firewallRule.setEnabled(true); //maybe change it later
            if(Util.isNullOrEmptyValue(firewallRule.getId()))
            	newRules.add(firewallRule);
            else
            	existedRules.add(firewallRule);
		}
	}
	
	private void makeFirewallInfo(Firewall firewall, TokenOs ostoken)
			throws BusinessException {
		
		if (!Util.isNullOrEmptyValue(firewall.getRuleIds())) {
			String[] ruleIds = firewall.getRuleIds().split(",");
			List<FirewallRule> firewallRules = new ArrayList<FirewallRule>();
			for (int index = 0; index < ruleIds.length; ++index) {
				FirewallRule firewallRule = firewallRuleMapper.selectByPrimaryKey(ruleIds[index]);
				if (null == firewallRule) {
					firewallRule = getFirewallRule(ruleIds[index], ostoken);
					// storeFirewallRuleToDB(firewallRule);
				}
				firewallRules.add(firewallRule);
			}
			firewall.setRules(firewallRules);
		}
		
		addRouterInfoToFirewall(firewall,ostoken);
	}
	
	private void addRouterInfoToFirewall(Firewall firewall, TokenOs ostoken) throws BusinessException{
		if (!Util.isNullOrEmptyValue(firewall.getRouterIds())) {
			String[] routerIds = firewall.getRouterIds().split(",");
			List<Router> routers = new ArrayList<Router>();
			for (int index = 0; index < routerIds.length; ++index) {
				Router router = routerMapper.selectByPrimaryKey(routerIds[index]);
				if (null == router) {
					try{
						router = routerService.getRouter(routerIds[index], ostoken);	
					}catch(Exception e){
						log.error(e);
						continue;
					}
				}
				//router.setCreatedAt(Util.millionSecond2Date(router.getMillionSeconds()));
				if(Util.isNullOrEmptyValue(router.getGatewayId()))
					router.setPublicGateway(false);
				else
					router.setPublicGateway(true);
				routers.add(router);
			}
			firewall.setRouters(routers);
		}
	}
	
	private void addFirewallRuleInfo(Firewall firewall,TokenOs ostoken){
		try{
			FirewallPolicy policy = this.getFirewallPolicy(firewall.getFirewall_policy_id(), ostoken);
			List<String> ruleIds = policy.getFirewall_rules();
			List<FirewallRule> firewallRules = new ArrayList<FirewallRule>();
			for(String ruleId : ruleIds){
				FirewallRule rule = this.getFirewallRule(ruleId, ostoken);
				firewallRules.add(rule);
			}
			firewall.setRuleIds(policy.getRuleIds());
			firewall.setRules(firewallRules);
			
			addRouterInfoToFirewall(firewall,ostoken);
		}catch (Exception e) {
			// TODO Auto-generated catch block
		}
	}
	
	private void deletePolicyAndRuleForFirewall(FirewallPolicy firewallPolicy, List<FirewallRule> Rules,
			TokenOs ostoken) {
		try {
			deleteFirewallPolicy(firewallPolicy.getId(), ostoken);
			for (FirewallRule rule : Rules) {
				deleteFirewallRule(rule.getId(), ostoken);
			}
		} catch (Exception e) {

		}
	}
	
	private List<Firewall> getFirewallsFromDB(String tenantId,int limitItems){
		List<Firewall> firewallsFromDB = null;
		if(-1 == limitItems){
			firewallsFromDB = firewallMapper.selectAllByTenantId(tenantId);
		}else{
			firewallsFromDB = firewallMapper.selectAllByTenantIdWithLimit(tenantId,limitItems);
		}
		return firewallsFromDB;
	}
	
	private void storeFirewallToDB(Firewall firewall){
		if(null == firewall)
			return;
		firewallMapper.insertOrUpdate(firewall);
//		if(null == firewallMapper.selectByPrimaryKey(firewall.getId()))
//			firewallMapper.insertSelective(firewall);
//		else
//			firewallMapper.updateByPrimaryKeySelective(firewall);
	}
	
	private void updateRouterInfo(Firewall firewall, Boolean add) {
		List<String> routerIds = firewall.getRouter_ids();
		if(Util.isNullOrEmptyList(routerIds)){
			routerIds = Util.stringToList(firewall.getRouterIds(),",");	
		}
		if(Util.isNullOrEmptyList(routerIds))
			return;
		for (String routerId : routerIds) {
			Router router = routerMapper.selectByPrimaryKey(routerId);
			if (null == router)
				continue;
			if (true == add) {
				router.setFirewallId(firewall.getId());
			} else {
				router.setFirewallId(null);
			}
			routerMapper.updateRouterFirewallInfo(router);
		}
	}
	
	private void storeFirewallRuleToDB(FirewallRule firewallRule){
		if(null == firewallRule)
			return;
		firewallRuleMapper.insertOrUpdate(firewallRule);
//		if(null == firewallRuleMapper.selectByPrimaryKey(firewallRule.getId()))
//			firewallRuleMapper.insertSelective(firewallRule);
//		else
//			firewallRuleMapper.updateByPrimaryKeySelective(firewallRule);
	}
	
	private List<Firewall> storeFirewallsToDB(List<Firewall> firewalls){
		if(Util.isNullOrEmptyList(firewalls))
			return null;
		firewallMapper.insertOrUpdateBatch(firewalls);
//		for (Firewall firewall : firewalls) {
//			storeFirewallToDB(firewall);
//		}
		return firewalls;
	}
	
	private List<Firewall> getLimitItems(List<Firewall> firewalls,String tenantId,int limit){
		if(Util.isNullOrEmptyList(firewalls))
			return null;
		List<Firewall> tenantFirewalls = new ArrayList<Firewall>();
		for(Firewall firewall : firewalls){
			if(!tenantId.equals(firewall.getTenant_id()))
				continue;
			tenantFirewalls.add(firewall);
		}
		if(-1 != limit){
			if(limit <= tenantFirewalls.size())
				return tenantFirewalls.subList(0, limit);
		}
		return tenantFirewalls;
	}
	
	private void normalFirewallInfo(Firewall firewall,List<FirewallRule> rules,String policyId){
		firewall.setMillionSeconds(Util.getCurrentMillionsecond());
		firewall.setFirewall_policy_id(policyId);
		List<String> ruleIds = new ArrayList<String>();
		for(FirewallRule rule : rules){
			ruleIds.add(rule.getId());
		}
		firewall.setRuleIds(Util.listToString(ruleIds, ','));
	}
	
	private void normalFirewallCreatedTime(List<Firewall> firewalls){
		if(null == firewalls)
			return;
		for(Firewall firewall : firewalls){
			if(null == firewall.getMillionSeconds())
				continue;
			firewall.setCreatedAt(Util.millionSecond2Date(firewall.getMillionSeconds()));
		}
	}
	
	private void updateSyncResourceInfo(String tenantId,String id,String relatedResourceId,String status,String type,String region,String relatedResource,String name){
		SyncResource resource = new SyncResource();
		resource.setId(id);
		resource.setType(type);
		resource.setExpectedStatus(status);
		resource.setRegion(region);
		
		if(null != relatedResourceId){
			relatedResourceId = "router:"+relatedResourceId;
			relatedResourceId += ";";
			relatedResourceId += relatedResource;
			resource.setRelatedResource(relatedResourceId);
		}else{
			resource.setRelatedResource(relatedResource);
		}
		syncResourceMapper.insertSelective(resource);
		
		ResourceCreateProcess createProcess = new ResourceCreateProcess();
		createProcess.setId(id);
		createProcess.setName(name);
		createProcess.setTenantId(tenantId);
		createProcess.setType(type);
		createProcess.setBegineSeconds(Util.time2Millionsecond(Util.getCurrentDate(),ParamConstant.TIME_FORMAT_01));
		resourceCreateProcessMapper.insertOrUpdate(createProcess);
	}
	
	private void storeResourceEventInfo(String tenantId,String id,String type,String beginState,String endState,long time){
		ResourceEvent event = new ResourceEvent();
		event.setTenantId(tenantId);
		event.setResourceId(id);
		event.setResourceType(type);
		event.setBeginState(beginState);
		event.setEndState(endState);
		event.setMillionSeconds(time);
		resourceEventMapper.insertSelective(event);
	}
	
	private void checkFirewallState(String id,Locale locale) throws BusinessException {
		Firewall firewall = firewallMapper.selectByPrimaryKey(id);
		if(null == firewall)
			return;
		if(firewall.getAdmin_state_up() != true)
			throw new ResourceBusinessException(Message.CS_FIREWALL_NOT_START,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE, locale);
        return;
	}
	
	
	private void checkResource(String id, Boolean delete,Locale locale) throws BusinessException {
		SyncResource syncResource = syncResourceMapper.selectByPrimaryKey(id);
		if (null != syncResource)
			throw new ResourceBusinessException(Message.CS_RESOURCE_IS_DOING,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE, locale);
		if(false == delete)
			return;
		Firewall firewall = firewallMapper.selectByPrimaryKey(id);
		if(null == firewall)
			return;
		if(Util.isNullOrEmptyValue(firewall.getRouterIds()))
			throw new ResourceBusinessException(Message.CS_HAVE_RELATED_ROUTER_RESOURCE,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE, locale);
        return;
	}
	
	private void checkName(String name,TokenOs ostoken)  throws BusinessException{
		if(Util.isNullOrEmptyValue(name))
			return;
		List<Firewall> firewalls = firewallMapper.selectAllByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(firewalls))
			return;
		for(Firewall firewall : firewalls){
			if(name.equals(firewall.getName()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_NAME_IS_SAME,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		return;
	}
}
