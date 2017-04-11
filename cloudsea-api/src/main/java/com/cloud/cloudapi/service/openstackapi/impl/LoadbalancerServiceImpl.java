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

import com.cloud.cloudapi.dao.common.FloatingIPMapper;
import com.cloud.cloudapi.dao.common.InstanceMapper;
import com.cloud.cloudapi.dao.common.LoadbalancerMapper;
import com.cloud.cloudapi.dao.common.LoadbalancerPoolMapper;
import com.cloud.cloudapi.dao.common.LoadbalancerPoolMemberMapper;
import com.cloud.cloudapi.dao.common.PortMapper;
import com.cloud.cloudapi.dao.common.ResourceCreateProcessMapper;
import com.cloud.cloudapi.dao.common.ResourceEventMapper;
import com.cloud.cloudapi.dao.common.SyncResourceMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.LBHealthMonitorJSON;
import com.cloud.cloudapi.json.forgui.LBPoolJSON;
import com.cloud.cloudapi.json.forgui.LBPoolMemberJSON;
import com.cloud.cloudapi.json.forgui.ListenerJSON;
import com.cloud.cloudapi.json.forgui.LoadbalancerJSON;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FloatingIP;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.LBHealthMonitor;
import com.cloud.cloudapi.pojo.openstackapi.forgui.LBPool;
import com.cloud.cloudapi.pojo.openstackapi.forgui.LBPoolMember;
import com.cloud.cloudapi.pojo.openstackapi.forgui.LBVip;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Listener;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Loadbalancer;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Port;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceCreateProcess;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceEvent;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SyncResource;
import com.cloud.cloudapi.service.businessapi.zabbix.ZabbixService;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.FloatingIPService;
import com.cloud.cloudapi.service.openstackapi.LoadbalancerService;
import com.cloud.cloudapi.service.openstackapi.PortService;
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

@Service("loadblancerService")
public class LoadbalancerServiceImpl implements LoadbalancerService {

	@Autowired
	private InstanceMapper instanceMapper;
	
	@Autowired
	private CloudConfig cloudconfig;
	
	@Autowired
	private PortMapper portMapper;
	
	@Autowired
	private LoadbalancerMapper loadbalancerMapper;
	
	@Autowired
	private LoadbalancerPoolMapper loadbalancerPoolMapper;
	
	@Autowired
	private LoadbalancerPoolMemberMapper loadbalancerPoolMemberMapper;
	
	@Autowired
	private SyncResourceMapper syncResourceMapper;

	@Autowired
	private ResourceCreateProcessMapper resourceCreateProcessMapper;

	@Autowired
	private ResourceEventMapper resourceEventMapper;
	
	@Resource
	private ZabbixService zabbixService;
	
	@Resource
	private FloatingIPService floatingIPService;
	
	@Resource
	private FloatingIPMapper floatingIPMapper;
	
	@Resource
	private OSHttpClientUtil client;

	@Resource
	private PortService portService;

	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(LoadbalancerServiceImpl.class);
	
	@Override
	public List<Loadbalancer> getLoadbalancers(Map<String, String> paramMap, TokenOs ostoken) throws BusinessException {
		int limitItems = Util.getLimit(paramMap);
		List<Loadbalancer> loadbalancersFromDB = getLoadbalancersFromDB(ostoken.getTenantid(),limitItems);
		if(!Util.isNullOrEmptyList(loadbalancersFromDB)){
			normalLoadbalancerCreatedTime(loadbalancersFromDB);
			return loadbalancersFromDB;
		}
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/lbaas/loadbalancers", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
        Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		List<Loadbalancer> loadblancers = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				loadblancers = getLoadbalancers(rs);
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
			rs = client.httpDoGet(url, headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_LOADBLANCER_GET_FAILED,httpCode,locale);
			try {
				loadblancers = getLoadbalancers(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_LOADBLANCER_GET_FAILED,httpCode,locale);
			*/
		}
		if(null == loadblancers)
			return null;
		storeLoadbalancersToDB(loadblancers);
		normalLoadbalancerCreatedTime(loadblancers);
		return getLimitLoadbalancerItems(loadblancers,limitItems);
	}

	private Loadbalancer refreshLoadbalancer(String loadblancerId, TokenOs ostoken)
			throws BusinessException {
		
		Loadbalancer loadbalancer = null;
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/lbaas/loadbalancers/");
		sb.append(loadblancerId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				loadbalancer = getLoadbalancer(rs);
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
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);	
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_LOADBLANCER_DETAIL_GET_FAILED,httpCode,locale);
			try {
				loadbalancer = getLoadbalancer(rs);
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
			throw new ResourceBusinessException(Message.CS_LOADBLANCER_DETAIL_GET_FAILED,httpCode,locale);
		}
		
		loadbalancer.setMillionSeconds(Util.getCurrentMillionsecond());
		//storeLoadbalancerToDB(loadbalancer);
		return loadbalancer;
	}
	
	@Override
	public Loadbalancer getLoadbalancer(String loadblancerId, TokenOs ostoken)
			throws BusinessException {
		
		Loadbalancer loadbalancer = loadbalancerMapper.selectByPrimaryKey(loadblancerId);
		if(null != loadbalancer){
			makeLoadbalancerInfo(loadbalancer,ostoken);
            return loadbalancer;
		}
		
		loadbalancer = refreshLoadbalancer(loadblancerId,ostoken);
		storeLoadbalancerToDB(loadbalancer);
		return loadbalancer;
	}

	@Override
	public Loadbalancer createLoadbalancer(String createBody, TokenOs ostoken)
			throws BusinessException, JsonProcessingException, IOException {

		checkLoadbalancer(ostoken);
		
		Loadbalancer loadbalancerCreateInfo = new Loadbalancer();
		Listener listenerCreateInfo = new Listener();
		LBPool lbPool = new LBPool();
		LBHealthMonitor healthMonitor = new LBHealthMonitor();
		
		List<LBPoolMember> poolMembers = new ArrayList<LBPoolMember>();
		makeLoadBalancerCreateInfo(ostoken,createBody,loadbalancerCreateInfo,listenerCreateInfo,lbPool,poolMembers,healthMonitor);
		String loadbalancerCreateBody = getLoadbalancerCreatedBody(loadbalancerCreateInfo);
		checkName(loadbalancerCreateInfo.getName(),ostoken);
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPost(url + "/v2.0/lbaas/loadbalancers", headers, loadbalancerCreateBody);
		Util.checkResponseBody(rs,locale);

		Loadbalancer loadbalancer = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));

		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				loadbalancer = getLoadbalancer(rs);
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
			rs = client.httpDoPost(url + "/v2.0/lbaas/loadbalancers", headers, loadbalancerCreateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_LOADBLANCER_CREATE_FAILED,httpCode,locale);
			try {
				loadbalancer = getLoadbalancer(rs);
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
			throw new ResourceBusinessException(Message.CS_LOADBLANCER_CREATE_FAILED,httpCode,locale);
		}
		
		loadbalancer.setMillionSeconds(Util.getCurrentMillionsecond());
		
		List<String> instancesid = new ArrayList<String>();
		for(LBPoolMember member : poolMembers){
			instancesid.add(member.getInstanceId());
		}
		

		makeRelatedResourceForLoadbalancer(loadbalancer,listenerCreateInfo,lbPool,healthMonitor,poolMembers,ostoken);		
		
		storeLoadbalancerToDB(loadbalancer);
		
		updateSyncResourceInfo(ostoken.getTenantid(),loadbalancer.getId(),ParamConstant.ACTIVE_STATUS,ParamConstant.LOADBALANCER,ostoken.getCurrentRegion(),"port:add",loadbalancer.getName());
	//	loadbalancer.setName(StringHelper.ncr2String(loadbalancer.getName()));
	//	loadbalancer.setDescription(StringHelper.ncr2String(loadbalancer.getDescription()));
		
		storeResourceEventInfo(ostoken.getTenantid(),loadbalancer.getId(),ParamConstant.LBAAS,null,ParamConstant.ACTIVE_STATUS,loadbalancer.getMillionSeconds());
		updateRelatedInstanceInfo(instancesid,loadbalancer.getId());
		
		return loadbalancer;
	}

	@Override
	public Loadbalancer updateLoadbalancer(String loadblancerId,String updateBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException{
		
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(loadblancerId,locale);
		
		Loadbalancer loadbalancerUpdateInfo = new Loadbalancer();
		makeLoadBalancerCreateInfo(ostoken,updateBody,loadbalancerUpdateInfo,null,null,null,null);
		String loadbalancerUpdateBody = getLoadbalancerCreatedBody(loadbalancerUpdateInfo);
		checkName(loadbalancerUpdateInfo.getName(),ostoken);
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/lbaas/loadbalancers/");
		sb.append(loadblancerId);
		Map<String, String> rs = client.httpDoPut(sb.toString(), headers, loadbalancerUpdateBody);
		Util.checkResponseBody(rs,locale);

		Loadbalancer loadbalancer = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				loadbalancer = getLoadbalancer(rs);
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
			rs = client.httpDoPost(sb.toString(), headers, loadbalancerUpdateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				loadbalancer = getLoadbalancer(rs);
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
			throw new ResourceBusinessException(Message.CS_LOADBLANCER_UPDATE_FAILED,httpCode,locale);
		}
		
		Loadbalancer loadbalancerFromDB = loadbalancerMapper.selectByPrimaryKey(loadbalancer.getId());

		storeLoadbalancerToDB(loadbalancer);
		makeLoadbalancerInfo(loadbalancer,ostoken);
		loadbalancer.setName(StringHelper.ncr2String(loadbalancer.getName()));
		loadbalancer.setDescription(StringHelper.ncr2String(loadbalancer.getDescription()));
		
		if(null == loadbalancerUpdateInfo.getAdmin_state_up() || true == loadbalancerUpdateInfo.getAdmin_state_up()){
			updateSyncResourceInfo(ostoken.getTenantid(),loadbalancer.getId(),ParamConstant.ACTIVE_STATUS,ParamConstant.LOADBALANCER,ostoken.getCurrentRegion(),"admin_state_up:1",loadbalancer.getName());
			storeResourceEventInfo(ostoken.getTenantid(),loadbalancer.getId(),ParamConstant.LBAAS,loadbalancerFromDB.getOperating_status(),ParamConstant.ACTIVE_STATUS,Util.getCurrentMillionsecond());		
		}else{
			updateSyncResourceInfo(ostoken.getTenantid(),loadbalancer.getId(),ParamConstant.ACTIVE_STATUS,ParamConstant.LOADBALANCER,ostoken.getCurrentRegion(),"admin_state_up:0",loadbalancer.getName());
			storeResourceEventInfo(ostoken.getTenantid(),loadbalancer.getId(),ParamConstant.LBAAS,loadbalancerFromDB.getOperating_status(),ParamConstant.STOPPED_STATUS,Util.getCurrentMillionsecond());	
		}
		return loadbalancer;
	}
	
	@Override
	public Loadbalancer bindingFloatingIP(String loadblancerId,String updateBody,TokenOs ostoken) throws BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(updateBody);
		} catch (Exception e) {
			log.error("error",e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		} 
		Loadbalancer lb = loadbalancerMapper.selectByPrimaryKey(loadblancerId);
		if(null == lb || Util.isNullOrEmptyValue(lb.getVip_port_id()))
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
        String floatingIPAddress = rootNode.path(ResponseConstant.ADDRESS).textValue();
        FloatingIP floatingIP = floatingIPMapper.selectByAddress(floatingIPAddress);
        if(null == floatingIP)
        	throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));

		floatingIP = floatingIPService.associateFloatingIp(floatingIP.getId(),loadblancerId,lb.getVip_port_id(), ostoken);
		
		lb.setFloatingIp(floatingIPAddress);
		loadbalancerMapper.insertOrUpdate(lb);
		return lb;
	}
	
	@Override
	public Loadbalancer removeFloatingIP(String loadblancerId,String updateBody,TokenOs ostoken) throws BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(updateBody);
		} catch (Exception e) {
			log.error("error",e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		} 
		Loadbalancer lb = loadbalancerMapper.selectByPrimaryKey(loadblancerId);
		if(null == lb || Util.isNullOrEmptyValue(lb.getVip_port_id()))
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
        String floatingIPAddress = rootNode.path(ResponseConstant.ADDRESS).textValue();
        FloatingIP floatingIP = floatingIPMapper.selectByAddress(floatingIPAddress);
        if(null == floatingIP)
        	throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
        
        floatingIP = floatingIPService.disassociateFloatingIp(floatingIP.getId(), loadblancerId,ostoken);
		lb.setFloatingIp(null);
		loadbalancerMapper.insertOrUpdate(lb);
		return lb;
	}
	
	private void makeRelatedResourceForLoadbalancer(Loadbalancer loadblancer, Listener listenerCreateInfo, LBPool lbPool,
			LBHealthMonitor healthMonitor, List<LBPoolMember> poolMembers, TokenOs ostoken) throws BusinessException {

		int tryCount = 1;
		int maxtryCounts = Integer.parseInt(cloudconfig.getSystemMaxTries());
		int waitTime = Integer.parseInt(cloudconfig.getSystemWaitTime());
		
		Loadbalancer createdLoadblancer =  refreshLoadbalancer(loadblancer.getId(), ostoken);
		while (!createdLoadblancer.getProvisioning_status().equals(ParamConstant.ACTIVE_STATUS)) {
			if (tryCount > maxtryCounts)
				  throw new ResourceBusinessException(Message.CS_LOADBLANCER_CREATE_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
			try {
				Thread.sleep(waitTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				log.error(e);
			}
			++tryCount;
			createdLoadblancer = refreshLoadbalancer(loadblancer.getId(), ostoken);
		}
		listenerCreateInfo.setLoadbalancer_id(loadblancer.getId());
		String listenerCreateBody = getListenerCreatedBody(listenerCreateInfo);
		Listener listener = null;
		LBPool pool = null;
		tryCount = 1;
		List<String> newPoolMemberIds = new ArrayList<String>();
		try {
			listener = buildListener(listenerCreateBody, ostoken);
			Thread.sleep(waitTime*2);
			createdLoadblancer = refreshLoadbalancer(loadblancer.getId(), ostoken);
			while (!createdLoadblancer.getProvisioning_status().equals(ParamConstant.ACTIVE_STATUS) || Util.isNullOrEmptyValue(createdLoadblancer.getListenerIds())) {
				if (tryCount > maxtryCounts)
					 throw new ResourceBusinessException(Message.CS_LOADBLANCER_CREATE_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
				try {
					Thread.sleep(waitTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					log.error(e);
				}
				++tryCount;
				createdLoadblancer = refreshLoadbalancer(loadblancer.getId(), ostoken);
			}
			lbPool.setListener_id(listener.getId());
			lbPool.setLoadbalancer_id(loadblancer.getId());
			String lbpoolCreateBody = getPoolCreatedBody(lbPool);
			pool = buildPool(lbpoolCreateBody, ostoken);


			Thread.sleep(waitTime*2);
			tryCount = 1;
			createdLoadblancer = refreshLoadbalancer(loadblancer.getId(), ostoken);
			while (!createdLoadblancer.getProvisioning_status().equals(ParamConstant.ACTIVE_STATUS)) {
				if (tryCount > maxtryCounts)
					 throw new ResourceBusinessException(Message.CS_LOADBLANCER_CREATE_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
				try {
					Thread.sleep(waitTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					log.error(e);
				}
				++tryCount;
				createdLoadblancer = refreshLoadbalancer(loadblancer.getId(), ostoken);
			}
			
			for (LBPoolMember poolMember : poolMembers) {
				// poolMember.setPool_id(pool.getId());
				String instanceId = poolMember.getInstanceId();
				poolMember.setInstanceId(null);
				String poolMemberCreateBody = getPoolMemberCreatedBody(poolMember);
				LBPoolMember newPoolMember = addPoolMember(pool.getId(), instanceId,poolMemberCreateBody,ostoken);
				try {
					Thread.sleep(waitTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					log.error(e);
				}
				newPoolMemberIds.add(newPoolMember.getId());
			}
			loadblancer.setPoolIds(pool.getId());
			loadblancer.setListenerIds(listener.getId());
			healthMonitor.setPool_id(pool.getId());
			String healthMonitorCreateBody = getHealthMonitorMemberCreatedBody(healthMonitor);
			Thread.sleep(waitTime);
			healthMonitor = buildHealthMonitor(healthMonitorCreateBody, ostoken);
			
			pool.setHealth_monitor_id(healthMonitor.getId());
			pool.setMembers_id(Util.listToString(newPoolMemberIds, ','));
			storePoolToDB(pool);
		} catch (Exception e) {
			log.error(e);
			for (String memberId : newPoolMemberIds) {
				removePoolMember(pool.getId(), memberId, ostoken);
			}
			if (null != pool)
				removePool(pool.getId(), ostoken);
			if (null != listener)
				removeListener(listener.getId(), ostoken);
			if (null != loadblancer)
				deleteLoadbalancer(loadblancer.getId(), ostoken);
			throw new ResourceBusinessException(Message.CS_LOADBLANCER_CREATE_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
	}

	private void deleteRelatedLoadBalancerInfo(String loadbalancerId, TokenOs ostoken)
			throws BusinessException {
		Loadbalancer loadbalancer = loadbalancerMapper.selectByPrimaryKey(loadbalancerId);
		if (null == loadbalancer)
			return;
		int waitTime = Integer.parseInt(cloudconfig.getSystemWaitTime());
		String poolIds = loadbalancer.getPoolIds();
		Map<String, String> healthMonitos = new HashMap<String, String>();
		Map<String, String> poolMembers = new HashMap<String, String>();
		try {
			if (!Util.isNullOrEmptyValue(poolIds)) {
				String[] poolIdArray = poolIds.split(",");
				for (int index = 0; index < poolIdArray.length; ++index) {
					LBPool pool = loadbalancerPoolMapper.selectByPrimaryKey(poolIdArray[index]);
					if (null != pool) {
						healthMonitos.put(pool.getId(), pool.getHealth_monitor_id());
						poolMembers.put(pool.getId(), pool.getMembers_id());
					}
				}

				for (Map.Entry<String, String> heanlthMonitor : healthMonitos.entrySet()) {
					disassociateHealthMonitorWithPool(heanlthMonitor.getKey(), heanlthMonitor.getValue(), ostoken);
					Thread.sleep(waitTime);
				}

				for (Map.Entry<String, String> poolMember : poolMembers.entrySet()) {
					String[] poolMemberIds = poolMember.getValue().split(",");
					for (int index = 0; index < poolMemberIds.length; ++index) {
						removePoolMember(poolMember.getKey(), poolMemberIds[index], ostoken);
						Thread.sleep(waitTime);
					}
				}

				for (int index = 0; index < poolIdArray.length; ++index) {
					removePool(poolIdArray[index], ostoken);
					Thread.sleep(waitTime);
				}

				String listenerIds = loadbalancer.getListenerIds();
				if (!Util.isNullOrEmptyValue(listenerIds)) {
					String[] listenerIdArray = listenerIds.split(",");
					for (int index = 0; index < listenerIdArray.length; ++index) {
						removeListener(listenerIdArray[index], ostoken);
						Thread.sleep(waitTime);
					}
				}
			}
		}catch (Exception e){
			log.error("error",e);
		}
	}
	
	@Override
	public void deleteLoadbalancer(String loadbalancerId, TokenOs ostoken)
			throws BusinessException {
		
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(loadbalancerId,locale);
		checkFloatingIP(loadbalancerId,locale);
		
		deleteRelatedLoadBalancerInfo(loadbalancerId,ostoken);
		// token should have Regioninfo
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/lbaas/loadbalancers/");
		sb.append(loadbalancerId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);

		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE:
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
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
//			if (httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE
//					|| httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
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
//			throw new ResourceBusinessException(Message.CS_LOADBLANCER_DELETE_FAILED,httpCode,locale);
		}
		
		Loadbalancer loadbalancer = loadbalancerMapper.selectByPrimaryKey(loadbalancerId);
		loadbalancerMapper.deleteByPrimaryKey(loadbalancerId);
		
		//delete monitor obj about LoadBanlance
		zabbixService.deleteMonitorObj(loadbalancerId);
		
		updateSyncResourceInfo(ostoken.getTenantid(),loadbalancer.getId(),ParamConstant.DELETED_STATUS,ParamConstant.LOADBALANCER,ostoken.getCurrentRegion(),null,loadbalancer.getName());
		storeResourceEventInfo(ostoken.getTenantid(),loadbalancer.getId(),ParamConstant.LBAAS,loadbalancer.getProvisioning_status(),ParamConstant.DELETED_STATUS,Util.getCurrentMillionsecond());
		updateRelatedInstanceInfo(null,loadbalancerId);
	}
	
	@Override
	public List<Listener> getListeners(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/lbaas/listeners", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		List<Listener> listeners = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				listeners = getListeners(rs);
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
			rs = client.httpDoGet(url, headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_LISTENER_GET_FAILED,httpCode,locale);
			try {
				listeners = getListeners(rs);
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
			throw new ResourceBusinessException(Message.CS_LISTENER_GET_FAILED,httpCode,locale);
		}
		return listeners;
	}

	@Override
	public Listener getListener(String listenerId, TokenOs ostoken)
			throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/lbaas/listeners/");
		sb.append(listenerId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		Listener listener = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				listener = getListener(rs);
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
				throw new ResourceBusinessException(Message.CS_LISTENER_DETAIL_GET_FAILED,httpCode,locale);
			try {
				listener = getListener(rs);
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
			throw new ResourceBusinessException(Message.CS_LISTENER_DETAIL_GET_FAILED,httpCode,locale);
		}
		return listener;
	}

	@Override
	public Listener buildListener(String createBody, TokenOs ostoken)
			throws BusinessException {
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPost(url + "/v2.0/lbaas/listeners", headers, createBody);
		Util.checkResponseBody(rs,locale);

		Listener listener = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				listener = getListener(rs);
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
			rs = client.httpDoPost(url + "/v2.0/lbaas/listeners", headers, createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_LISTENER_CREATE_FAILED,httpCode,locale);
			try {
				listener = getListener(rs);
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
			throw new ResourceBusinessException(Message.CS_LISTENER_CREATE_FAILED,httpCode,locale);
		}
		return listener;
	}

	@Override
	public void removeListener(String listenerId,TokenOs ostoken) throws BusinessException{
		// token should have Regioninfo
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/lbaas/listeners/");
		sb.append(listenerId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);

		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE:
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
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if (httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE
					|| httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
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
//			throw new ResourceBusinessException(Message.CS_LISTENER_DELETE_FAILED,httpCode,locale);
		}

	}
	
	@Override
	public List<LBPool> getPools(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {
		
		int limitItems = Util.getLimit(paramMap);
		List<LBPool> pools = getPoolsFromDB(ostoken.getTenantid(),limitItems);
		if(!Util.isNullOrEmptyList(pools))
			return pools;
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/lbaas/pools", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs, locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				pools = getPools(rs);
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
			rs = client.httpDoGet(url, headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_POOL_GET_FAILED,httpCode,locale);
			try {
				pools = getPools(rs);
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
			throw new ResourceBusinessException(Message.CS_POOL_GET_FAILED,httpCode,locale);
		}
		
		storePoolsToDB(pools);
		return getLimitPoolItems(pools,limitItems);
	}

	@Override
	public LBPool getPool(String poolId, TokenOs ostoken) throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		LBPool pool = loadbalancerPoolMapper.selectByPrimaryKey(poolId);
		if(null != pool)
			return pool;
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/lbaas/pools/");
		sb.append(poolId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				pool = getPool(rs);
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
				throw new ResourceBusinessException(Message.CS_POOL_DETAIL_GET_FAILED,httpCode,locale);
			try {
				pool = getPool(rs);
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
			throw new ResourceBusinessException(Message.CS_POOL_DETAIL_GET_FAILED,httpCode,locale);
		}
		
		storePoolToDB(pool);
		return pool;
	}

	@Override
	public LBPool buildPool(String createBody, TokenOs ostoken)
			throws BusinessException {
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPost(url + "/v2.0/lbaas/pools", headers, createBody);
		Util.checkResponseBody(rs,locale);

		LBPool pool = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				pool = getPool(rs);
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
			rs = client.httpDoPost(url + "/v2.0/lbaas/pools", headers, createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_POOL_CREATE_FAILED,httpCode,locale);
			try {
				pool = getPool(rs);
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
			throw new ResourceBusinessException(Message.CS_POOL_CREATE_FAILED,httpCode,locale);
		}
		storePoolToDB(pool);
		return pool;
	}

	@Override
	public void removePool(String poolId, TokenOs ostoken) throws BusinessException {
		// token should have Regioninfo
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/lbaas/pools/");
		sb.append(poolId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);

		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE:
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
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if (httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE
					|| httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
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
//			throw new ResourceBusinessException(Message.CS_POOL_DELETE_FAILED,httpCode,locale);
		}		
		loadbalancerPoolMapper.deleteByPrimaryKey(poolId);
	}
	
	@Override
	public List<LBPoolMember> getPoolMembers(String poolId, TokenOs ostoken)
			throws BusinessException {
		List<LBPoolMember> poolMembers = loadbalancerPoolMemberMapper.selectPoolMembersByPoolId(poolId);
		if(!Util.isNullOrEmptyList(poolMembers))
			return poolMembers;
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/lbaas/pools/");
		sb.append(poolId);
		sb.append("/members");

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				poolMembers = getPoolMembers(rs);
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
				throw new ResourceBusinessException(Message.CS_POOL_MEMBER_GET_FAILED,httpCode,locale);
			try {
				poolMembers = getPoolMembers(rs);
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
			throw new ResourceBusinessException(Message.CS_POOL_MEMBER_GET_FAILED,httpCode,locale);
		}
		
		storeLBPoolMembersToDB(poolMembers);
		return poolMembers;
	}

	@Override
	public LBPoolMember getPoolMember(String poolId, String memberId, TokenOs ostoken)
			throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		LBPoolMember poolMember = loadbalancerPoolMemberMapper.selectByPrimaryKey(memberId);
		if(null != poolMember)
			return poolMember;
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/lbaas/pools/");
		sb.append(poolId);
		sb.append("/members/");
		sb.append(memberId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				poolMember = getPoolMember(rs);
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
				throw new ResourceBusinessException(Message.CS_POOL_MEMBER_DETAIL_GET_FAILED,httpCode,locale);
			try {
				poolMember = getPoolMember(rs);
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
			throw new ResourceBusinessException(Message.CS_POOL_MEMBER_DETAIL_GET_FAILED,httpCode,locale);
		}
		
		poolMember.setPool_id(poolId);
		storePoolMemberToDB(poolMember);
		return poolMember;
	}

	private LBPoolMember createPoolMember(String poolId, String createBody, TokenOs ostoken) throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/lbaas/pools/");
		sb.append(poolId);
		sb.append("/members");

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPost(sb.toString(), headers, createBody);
		// Map<String, String> rs =
		// client.httpDoPost(url+"/v2.0/lbaas/pools/134-134/members", headers,
		// createBody);
		Util.checkResponseBody(rs, locale);

		LBPoolMember poolMember = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				poolMember = getPoolMember(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,
						ParamConstant.SERVICE_ERROR_RESPONSE_CODE, locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(), ostoken.getCurrentRegion(),
						ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,
						ParamConstant.SERVICE_ERROR_RESPONSE_CODE, locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoPost(sb.toString(), headers, createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if (httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_POOL_MEMBER_ADD_FAILED, httpCode, locale);
			try {
				poolMember = getPoolMember(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,
						ParamConstant.SERVICE_ERROR_RESPONSE_CODE, locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, httpCode, locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING, httpCode, locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN, httpCode, locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, httpCode, locale);
		default:
			throw new ResourceBusinessException(Message.CS_POOL_MEMBER_ADD_FAILED, httpCode, locale);
		}

		poolMember.setPool_id(poolId);
		return poolMember;
	}
	
	@Override
	public LBPoolMember addPoolMember(String poolId, String instanceId,String createBody, TokenOs ostoken) throws BusinessException{
		LBPoolMember poolMember  = addPoolMember(poolId,createBody,ostoken);
		poolMember.setInstanceId(instanceId);
		storePoolMemberToDB(poolMember);
		return poolMember;
	}

	@Override
	public LBPoolMember addPoolMember(String poolId, String createBody, TokenOs ostoken)
			throws BusinessException {
		LBPoolMember poolMember = createPoolMember(poolId,createBody,ostoken);
        storePoolMemberToDB(poolMember);
		return poolMember;
	}

	@Override
	public void removePoolMember(String poolId, String memberId, TokenOs ostoken)
			throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/lbaas/pools/");
		sb.append(poolId);
		sb.append("/members/");
		sb.append(memberId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
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
			rs = client.httpDoDelete(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if (httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
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
//			throw new ResourceBusinessException(Message.CS_POOL_MEMBER_REMOVE_FAILED,httpCode,locale);
		}
		
		loadbalancerPoolMemberMapper.deleteByPrimaryKey(memberId);
	}

	@Override
	public List<LBHealthMonitor> getHealthMonitors(Map<String, String> paramMap, TokenOs ostoken) throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/lbaas/healthmonitors", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		List<LBHealthMonitor> healthMonitors = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				healthMonitors = getHealthMonitors(rs);
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
			rs = client.httpDoGet(url, headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_LOADBLANCER_GET_FAILED,httpCode,locale);
			try {
				healthMonitors = getHealthMonitors(rs);
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
			throw new ResourceBusinessException(Message.CS_LOADBLANCER_GET_FAILED,httpCode,locale);
		}
		return healthMonitors;
	}

	@Override
	public LBHealthMonitor getHealthMonitor(String healthMonitorId, TokenOs ostoken)
			throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/lbaas/healthmonitors/");
		sb.append(healthMonitorId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		LBHealthMonitor healthMonitor = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				healthMonitor = getHealthMonitor(rs);
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
				throw new ResourceBusinessException(Message.CS_HEALTH_MONITOR_DETAIL_GET_FAILED,httpCode,locale);
			try {
				healthMonitor = getHealthMonitor(rs);
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
			throw new ResourceBusinessException(Message.CS_HEALTH_MONITOR_DETAIL_GET_FAILED,httpCode,locale);
		}
		return healthMonitor;
	}

	@Override
	public LBHealthMonitor buildHealthMonitor(String createBody, TokenOs ostoken)
			throws BusinessException {
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPost(url + "/v2.0/lbaas/healthmonitors", headers, createBody);
		Util.checkResponseBody(rs,locale);

		LBHealthMonitor healthMonitor = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				healthMonitor = getHealthMonitor(rs);
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
			rs = client.httpDoPost(url + "/v2.0/lbaas/health_monitors", headers, createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_HEALTH_MONITOR_CREATE_FAILED,httpCode,locale);
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				healthMonitor = getHealthMonitor(rs);
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
			throw new ResourceBusinessException(Message.CS_HEALTH_MONITOR_CREATE_FAILED,httpCode,locale);
		}
		return healthMonitor;
	}

	@Override
	public LBHealthMonitor associateHealthMonitorWithPool(String healthMonitorAssociateBody, String poolId, TokenOs ostoken) throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/lb/pools/");
		sb.append(poolId);
		sb.append("/healthmonitors");
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPost(sb.toString(), headers,healthMonitorAssociateBody);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		LBHealthMonitor healthMonitor = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				healthMonitor = getHealthMonitor(rs);
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
			rs = client.httpDoPost(sb.toString(), headers,healthMonitorAssociateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_ASSOCIATE_HEALTH_MONITOR_WITH_POOL_FAILED,httpCode,locale);
			try {
				healthMonitor = getHealthMonitor(rs);
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
			throw new ResourceBusinessException(Message.CS_ASSOCIATE_HEALTH_MONITOR_WITH_POOL_FAILED,httpCode,locale);
		}
		return healthMonitor;
	}
	
	@Override
	public void disassociateHealthMonitorWithPool(String poolId, String headthMonitorId,TokenOs ostoken) throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/lbaas/healthmonitors/");
		sb.append(headthMonitorId);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));

		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE:
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
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE
					|| httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
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
//			throw new ResourceBusinessException(Message.CS_LOADBLANCER_DELETE_FAILED,httpCode,locale);
		}
	}
	
	@Override
	public List<LBVip> getVips(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/lb/vips", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		List<LBVip> vip = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				vip = getVips(rs);
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
			rs = client.httpDoGet(url, headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_VIP_DETAIL_GET_FAILED,httpCode,locale);
			try {
				vip = getVips(rs);
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
			throw new ResourceBusinessException(Message.CS_VIP_DETAIL_GET_FAILED,httpCode,locale);
		}
		return vip;
	}

	@Override
	public LBVip getVip(String vipId,TokenOs ostoken) throws BusinessException{
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/lb/vips/");
		sb.append(vipId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		LBVip vip = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				vip = getVip(rs);
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
				throw new ResourceBusinessException(Message.CS_VIP_DETAIL_GET_FAILED,httpCode,locale);
			try {
				vip = getVip(rs);
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
			throw new ResourceBusinessException(Message.CS_VIP_DETAIL_GET_FAILED,httpCode,locale);
		}
		return vip;
	}
	
	@Override
	public LBVip createVip(String createBody, TokenOs ostoken) throws BusinessException{
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPost(url + "/v2.0/lb/vips", headers, createBody);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		LBVip vip = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				vip = getVip(rs);
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
			rs = client.httpDoPost(url + "/v2.0/lbaas/health_monitors", headers, createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
            if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
            	throw new ResourceBusinessException(Message.CS_VIP_CREATE_FAILED,httpCode,locale);
			try {
				vip = getVip(rs);
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
			throw new ResourceBusinessException(Message.CS_VIP_CREATE_FAILED,httpCode,locale);
		}
		return vip;
	}

	private List<LBVip> getVips(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode vipsNode = rootNode.path(ResponseConstant.VIPS);
		int vipsCount = vipsNode.size();
		if (0 == vipsCount)
			return null;
		
		List<LBVip> vips = new ArrayList<LBVip>();
		for (int index = 0; index < vipsCount; ++index) {
			LBVip vip = getVipInfo(vipsNode.get(index));
			if (null == vip)
				continue;
			vips.add(vip);
		}
		
		return vips;
	}
	
	private LBVip getVip(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode vipNode = rootNode.path(ResponseConstant.VIP);
		return getVipInfo(vipNode);
	}
	
	private LBVip getVipInfo(JsonNode vipNode){
		if (null == vipNode)
			return null;
		LBVip vip = new LBVip();
		vip.setStatus(vipNode.path(ResponseConstant.STATUS).textValue());
		vip.setProtocol(vipNode.path(ResponseConstant.PROTOCOL).textValue());
		vip.setDescription(vipNode.path(ResponseConstant.DESCRIPTION).textValue());
		vip.setAddress(vipNode.path(ResponseConstant.ADDRESS).textValue());
		vip.setProtocol_port(vipNode.path(ResponseConstant.PROTOCOL_PORT).intValue());
		vip.setPort_id(vipNode.path(ResponseConstant.PORT_ID).textValue());
		vip.setId(vipNode.path(ResponseConstant.ID).textValue());
		vip.setStatus_description(vipNode.path(ResponseConstant.STATUS_DESCRIPTION).textValue());
		vip.setName(vipNode.path(ResponseConstant.NAME).textValue());
		vip.setAdmin_state_up(vipNode.path(ResponseConstant.ADMIN_STATE_UP).booleanValue());
		vip.setSubnet_id(vipNode.path(ResponseConstant.SUBNET_ID).textValue());
		vip.setTenant_id(vipNode.path(ResponseConstant.TENANT_ID).textValue());
		vip.setConnection_limit(vipNode.path(ResponseConstant.CONNECTION_LIMIT).intValue());
        vip.setPool_id(vipNode.path(ResponseConstant.POOL_ID).textValue());
        //session_persistence object
		return vip;
	}
	
	private List<LBHealthMonitor> getHealthMonitors(Map<String, String> rs)
			throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode healthMonitorsNode = rootNode.path(ResponseConstant.HEALTH_MONITORS);
		int healthMonitorsCount = healthMonitorsNode.size();
		if (0 == healthMonitorsCount)
			return null;

		List<LBHealthMonitor> healthMonitors = new ArrayList<LBHealthMonitor>();
		for (int index = 0; index < healthMonitorsCount; ++index) {
			LBHealthMonitor healthMonitor = getHealthMonitorInfo(healthMonitorsNode.get(index));
			if (null == healthMonitor)
				continue;
			healthMonitors.add(healthMonitor);
		}

		return healthMonitors;
	}

	private LBHealthMonitor getHealthMonitor(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode healthMonitorNode = rootNode.path(ResponseConstant.HEALTH_MONITOR);
		return getHealthMonitorInfo(healthMonitorNode);
	}

	private LBHealthMonitor getHealthMonitorInfo(JsonNode healthMonitorNode) {
		if (null == healthMonitorNode)
			return null;
		LBHealthMonitor healthMonitor = new LBHealthMonitor();
		healthMonitor.setId(healthMonitorNode.path(ResponseConstant.ID).textValue());
		healthMonitor.setAdmin_state_up(healthMonitorNode.path(ResponseConstant.ADMIN_STATE_UP).booleanValue());
		healthMonitor.setTenant_id(healthMonitorNode.path(ResponseConstant.TENANT_ID).textValue());
		healthMonitor.setDelay(healthMonitorNode.path(ResponseConstant.DELAY).intValue());
		healthMonitor.setExpected_codes(healthMonitorNode.path(ResponseConstant.EXPECTED_CODES).textValue());
		healthMonitor.setMax_retries(healthMonitorNode.path(ResponseConstant.MAX_RETRIES).intValue());
		healthMonitor.setHttp_method(healthMonitorNode.path(ResponseConstant.HTTP_METHOD).textValue());
		healthMonitor.setTimeout(healthMonitorNode.path(ResponseConstant.TIMEOUT).intValue());
		healthMonitor.setUrl_path(healthMonitorNode.path(ResponseConstant.URL_PATH).textValue());
		healthMonitor.setType(healthMonitorNode.path(ResponseConstant.TYPE).textValue());

		JsonNode poolsNode = healthMonitorNode.path(ResponseConstant.POOLS);
		if (null != poolsNode) {
			int poolsCount = poolsNode.size();
			List<LBPool> pools = new ArrayList<LBPool>();
			for (int index = 0; index < poolsCount; ++index) {
				LBPool pool = new LBPool();
				pool.setStatus(poolsNode.get(index).path(ResponseConstant.STATUS).textValue());
				pool.setDescription(poolsNode.get(index).path(ResponseConstant.STATUS_DESCRIPTION).textValue());
				pool.setStatus(poolsNode.get(index).path(ResponseConstant.ID).textValue());
				pools.add(pool);
			}
			healthMonitor.setPools(pools);
		}
		return healthMonitor;
	}

	private List<LBPoolMember> getPoolMembers(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode poolMembersNode = rootNode.path(ResponseConstant.MEMBERS);
		int poolMembersCount = poolMembersNode.size();
		if (0 == poolMembersCount)
			return null;

		List<LBPoolMember> poolMembers = new ArrayList<LBPoolMember>();
		for (int index = 0; index < poolMembersCount; ++index) {
			LBPoolMember poolMember = getPoolMemberInfo(poolMembersNode.get(index));
			if (null == poolMember)
				continue;
			poolMembers.add(poolMember);
		}

		return poolMembers;
	}

	private LBPoolMember getPoolMember(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode poolMemberNode = rootNode.path(ResponseConstant.MEMBER);
		return getPoolMemberInfo(poolMemberNode);
	}

	private LBPoolMember getPoolMemberInfo(JsonNode poolMemberNode) {
		if (null == poolMemberNode)
			return null;
		LBPoolMember poolMember = new LBPoolMember();
		poolMember.setAddress(poolMemberNode.path(ResponseConstant.ADDRESS).textValue());
		poolMember.setAdmin_state_up(poolMemberNode.path(ResponseConstant.ADMIN_STATE_UP).booleanValue());
		poolMember.setId(poolMemberNode.path(ResponseConstant.ID).textValue());
		poolMember.setProtocol_port(poolMemberNode.path(ResponseConstant.PROTOCOL_PORT).intValue());
		poolMember.setSubnet_id(poolMemberNode.path(ResponseConstant.SUBNET_ID).textValue());
		poolMember.setTenant_id(poolMemberNode.path(ResponseConstant.TENANT_ID).textValue());
		poolMember.setWeight(poolMemberNode.path(ResponseConstant.WEIGHT).intValue());
		poolMember.setStatus(poolMemberNode.path(ResponseConstant.STATUS).textValue());
		poolMember.setStatus_description(poolMemberNode.path(ResponseConstant.STATUS_DESCRIPTION).textValue());
		poolMember.setPool_id(poolMemberNode.path(ResponseConstant.POOL_ID).textValue());

		return poolMember;
	}

	private List<LBPool> getPools(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode poolsNode = rootNode.path(ResponseConstant.POOLS);
		int poolsCount = poolsNode.size();
		if (0 == poolsCount)
			return null;

		List<LBPool> pools = new ArrayList<LBPool>();
		for (int index = 0; index < poolsCount; ++index) {
			LBPool pool = getPoolInfo(poolsNode.get(index));
			if (null == pool)
				continue;
			pools.add(pool);
		}

		return pools;
	}

	private LBPool getPool(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode poolNode = rootNode.path(ResponseConstant.POOL);
		return getPoolInfo(poolNode);
	}

	private LBPool getPoolInfo(JsonNode poolNode) {
		if (null == poolNode)
			return null;
		LBPool pool = new LBPool();

		pool.setStatus(poolNode.path(ResponseConstant.STATUS).textValue());
		pool.setLb_algorithm(poolNode.path(ResponseConstant.LB_ALGORITHM).textValue());
		pool.setProtocol(poolNode.path(ResponseConstant.PROTOCOL).textValue());
		pool.setDescription(poolNode.path(ResponseConstant.DESCRIPTION).textValue());
		pool.setHealth_monitor_id(poolNode.path(ResponseConstant.HEALTH_MONITOR_ID).textValue());
		
		JsonNode loadbalancersNode = poolNode.path(ResponseConstant.LOADBLANCERS);
		if (null != loadbalancersNode) {
			int loadbalancersCount = loadbalancersNode.size();
			List<String> loadbalancersId = new ArrayList<String>();
			for (int index = 0; index < loadbalancersCount; ++index) {
				JsonNode loadbalancerNode = loadbalancersNode.get(index);
				loadbalancersId.add(loadbalancerNode.path(ResponseConstant.ID).textValue());
			}
			pool.setLoadbalancers(loadbalancersId);
			pool.setLoadbalancer_id(Util.listToString(loadbalancersId, ','));
		}
		
		JsonNode listenersNode = poolNode.path(ResponseConstant.LISTENERS);
		if (null != listenersNode) {
			int listenersCount = listenersNode.size();
			List<String> listenersId = new ArrayList<String>();
			for (int index = 0; index < listenersCount; ++index) {
				JsonNode listenerNode = listenersNode.get(index);
				listenersId.add(listenerNode.path(ResponseConstant.ID).textValue());
			}
			pool.setListeners(listenersId);
			pool.setListener_id(Util.listToString(listenersId, ','));
		}
		
		JsonNode membersNode = poolNode.path(ResponseConstant.MEMBERS);
		if (null != membersNode) {
			int membersCount = membersNode.size();
			List<String> membersId = new ArrayList<String>();
			for (int index = 0; index < membersCount; ++index) {
				JsonNode memberNode = membersNode.get(index);
				membersId.add(memberNode.path(ResponseConstant.ID).textValue());
			}
			pool.setMembers(membersId);
			pool.setMembers_id(Util.listToString(membersId, ','));
		}

		pool.setStatus_description(poolNode.path(ResponseConstant.STATUS_DESCRIPTION).textValue());
		pool.setId(poolNode.path(ResponseConstant.ID).textValue());
		pool.setVip_id(poolNode.path(ResponseConstant.VIP_ID).textValue());
		pool.setName(poolNode.path(ResponseConstant.NAME).textValue());
		pool.setAdmin_state_up(poolNode.path(ResponseConstant.ADMIN_STATE_UP).booleanValue());
		pool.setSubnet_id(poolNode.path(ResponseConstant.SUBNET_ID).textValue());
		pool.setTenant_id(poolNode.path(ResponseConstant.TENANT_ID).textValue());
		pool.setProvider(poolNode.path(ResponseConstant.PROVIDER).textValue());

//		JsonNode monitorStatusNode = poolNode.path(ResponseConstant.HEALTH_MONITORS_STATUS);
//		if (null != monitorStatusNode) {
//			int monitorsCount = monitorStatusNode.size();
//			List<LBHealthMonitor> healthMoitors = new ArrayList<LBHealthMonitor>();
//			for (int index = 0; index < monitorsCount; ++index) {
//				LBHealthMonitor healthMonitor = new LBHealthMonitor();
//				healthMonitor.setMonitor_id(monitorStatusNode.get(index).path(ResponseConstant.MONITOR_ID).textValue());
//				healthMonitor.setStatus(monitorStatusNode.get(index).path(ResponseConstant.STATUS).textValue());
//				healthMonitor.setStatus_description(
//						monitorStatusNode.get(index).path(ResponseConstant.STATUS_DESCRIPTION).textValue());
//				healthMoitors.add(healthMonitor);
//			}
//			pool.setHealth_monitors_status(healthMoitors);
//		}

		return pool;
	}

	private Loadbalancer getLoadbalancerInfo(JsonNode loadblancerNode) {
		if (null == loadblancerNode)
			return null;
		Loadbalancer loadblancer = new Loadbalancer();

		loadblancer.setId(loadblancerNode.path(ResponseConstant.ID).textValue());
		loadblancer.setName(loadblancerNode.path(ResponseConstant.NAME).textValue());
		loadblancer.setDescription(loadblancerNode.path(ResponseConstant.DESCRIPTION).textValue());
		loadblancer.setAdmin_state_up(loadblancerNode.path(ResponseConstant.ADMIN_STATE_UP).booleanValue());
		loadblancer.setTenant_id(loadblancerNode.path(ResponseConstant.TENANT_ID).textValue());
		loadblancer.setProvisioning_status(loadblancerNode.path(ResponseConstant.PROVISIONING_STATUS).textValue());
		loadblancer.setVip_address(loadblancerNode.path(ResponseConstant.VIP_ADDRESS).textValue());
		loadblancer.setVip_subnet_id(loadblancerNode.path(ResponseConstant.VIP_SUBNET_ID).textValue());
		loadblancer.setOperating_status(loadblancerNode.path(ResponseConstant.OPERATING_STATUS).textValue());
		loadblancer.setProvider(loadblancerNode.path(ResponseConstant.PROVIDER).textValue());
		setListerenId(loadblancer,loadblancerNode.path(ResponseConstant.LISTENERS));
		setLBPoolId(loadblancer,loadblancerNode.path(ResponseConstant.POOLS));
		return loadblancer;
	}

	private void setListerenId(Loadbalancer loadblancer,JsonNode listenerNodes){
		if(null == listenerNodes)
			return;
		int listenerCount = listenerNodes.size();
		List<String> listenerIds = new ArrayList<String>();
		for(int index = 0; index < listenerCount; ++index){
			listenerIds.add(listenerNodes.get(index).path(ResponseConstant.ID).textValue());
		}
		loadblancer.setListenerIds(Util.listToString(listenerIds, ','));
	}
	
	private void setLBPoolId(Loadbalancer loadblancer,JsonNode poolNodes){
		if(null == poolNodes)
			return;
		int poolCount = poolNodes.size();
		List<String> poolIds = new ArrayList<String>();
		for(int index = 0; index < poolCount; ++index){
			poolIds.add(poolNodes.get(index).path(ResponseConstant.ID).textValue());
		}
		loadblancer.setPoolIds(Util.listToString(poolIds, ','));
	}
	
	private List<Loadbalancer> getLoadbalancers(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode loadblancersNode = rootNode.path(ResponseConstant.LOADBLANCERS);
		int loadblancersCount = loadblancersNode.size();
		if (0 == loadblancersCount)
			return null;

		List<Loadbalancer> loadblancers = new ArrayList<Loadbalancer>();
		for (int index = 0; index < loadblancersCount; ++index) {
			Loadbalancer loadblancer = getLoadbalancerInfo(loadblancersNode.get(index));
			if (null == loadblancer)
				continue;
			loadblancers.add(loadblancer);
		}

		return loadblancers;
	}

	private Loadbalancer getLoadbalancer(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode loadblancerNode = rootNode.path(ResponseConstant.LOADBLANCER);
		return getLoadbalancerInfo(loadblancerNode);
	}

	private Listener getListenerInfo(JsonNode listenerNode) {
		if (null == listenerNode)
			return null;
		Listener listener = new Listener();

		listener.setAdmin_state_up(listenerNode.path(ResponseConstant.ADMIN_STATE_UP).booleanValue());
		listener.setConnection_limit(listenerNode.path(ResponseConstant.CONNECTION_LIMIT).intValue());
		listener.setDefault_pool_id(listenerNode.path(ResponseConstant.DEFAULT_POOL_ID).textValue());
		listener.setDescription(listenerNode.path(ResponseConstant.DESCRIPTION).textValue());
		listener.setId(listenerNode.path(ResponseConstant.ID).textValue());
		listener.setName(listenerNode.path(ResponseConstant.NAME).textValue());
		listener.setProtocol(listenerNode.path(ResponseConstant.PROTOCOL).textValue());
		listener.setProtocol_port(listenerNode.path(ResponseConstant.PROTOCOL_PORT).intValue());
		listener.setDefault_tls_container_ref(
				listenerNode.path(ResponseConstant.DEFAULT_TLS_CONTAINER_REF).textValue());
		listener.setTenant_id(listenerNode.path(ResponseConstant.TENANT_ID).textValue());

		JsonNode loadbalancersNode = listenerNode.path(ResponseConstant.LOADBLANCERS);
		if (null != loadbalancersNode) {
			int loadblancersCount = loadbalancersNode.size();
			List<String> loadblancerIds = new ArrayList<String>();
			for (int index = 0; index < loadblancersCount; ++index) {
				loadblancerIds.add(loadbalancersNode.get(index).path(ResponseConstant.ID).textValue());
			}
			listener.setLoadbalancer_id(Util.listToString(loadblancerIds, ','));
		}

		JsonNode sniContinerNode = listenerNode.path(ResponseConstant.SNI_CONTAINER_REFS);
		if (null != sniContinerNode) {
			int continersCount = sniContinerNode.size();
			List<String> continersRef = new ArrayList<String>();
			for (int index = 0; index < continersCount; ++index) {
				continersRef.add(sniContinerNode.get(index).textValue());
			}
			listener.setSni_container_refs(Util.listToString(continersRef, ','));
		}
		return listener;
	}

	private List<Listener> getListeners(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode listenersNode = rootNode.path(ResponseConstant.LISTENERS);
		int listenersCount = listenersNode.size();
		if (0 == listenersCount)
			return null;

		List<Listener> listeners = new ArrayList<Listener>();
		for (int index = 0; index < listenersCount; ++index) {
			Listener listener = getListenerInfo(listenersNode.get(index));
			if (null == listener)
				continue;
			listeners.add(listener);
		}

		return listeners;
	}

	private Listener getListener(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode listenerNode = rootNode.path(ResponseConstant.LISTENER);
		return getListenerInfo(listenerNode);
	}
	
	private void makeLoadBalancerCreateInfo(TokenOs ostoken,String createBody,Loadbalancer loadbalancerCreateInfo,Listener listenerCreateInfo,LBPool lbPool,List<LBPoolMember> poolMembers,LBHealthMonitor healthMonitor)
			 throws JsonProcessingException, IOException, BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(createBody);
		
		String name = null;
		String description = null;
		if(!rootNode.path(ResponseConstant.NAME).isMissingNode())
		   name = StringHelper.string2Ncr(rootNode.path(ResponseConstant.NAME).textValue());
		if(!rootNode.path(ResponseConstant.DESCRIPTION).isMissingNode())
			description = StringHelper.string2Ncr(rootNode.path(ResponseConstant.DESCRIPTION).textValue());
		if(!rootNode.path(ResponseConstant.ADMIN_STATE_UP).isMissingNode())
			loadbalancerCreateInfo.setAdmin_state_up(Util.string2Boolean(rootNode.path(ResponseConstant.ADMIN_STATE_UP).textValue()));
		if(null == listenerCreateInfo){
			loadbalancerCreateInfo.setName(name);
			loadbalancerCreateInfo.setDescription(description);
			return;
		}
		Integer port = rootNode.path(ResponseConstant.PORT).intValue();
	    String protocol = rootNode.path(ResponseConstant.PROTOCOL).textValue();
	    String lb_algorithm = rootNode.path(ResponseConstant.LB_ALGORITHM).textValue();
	    String type = rootNode.path(ResponseConstant.TYPE).textValue();
	    Integer delay = rootNode.path(ResponseConstant.DELAY).intValue();
	    Integer timeout = rootNode.path(ResponseConstant.TIMEOUT).intValue();
	    Integer retries = rootNode.path(ResponseConstant.MAX_RETRIES).intValue();
	    
	    String httpMethod = rootNode.path(ResponseConstant.HTTP_METHOD).textValue();
	    String urlPath = rootNode.path(ResponseConstant.URL_PATH).textValue();
	    String expectedCodes = rootNode.path(ResponseConstant.EXPECTED_CODES).textValue();
	    
		JsonNode instancesNode = rootNode.path(ResponseConstant.INSTANCES);
	//	List<Instance> instances = new ArrayList<Instance>();
        List<String> ips = new ArrayList<String>();
        List<String> netrowkIds = new ArrayList<String>();
		int instancesCount = instancesNode.size();
		if(instancesCount < 2)
			throw new ResourceBusinessException(Message.CS_LOADBLANCER_INSTANCE_INFO_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));

	//	List<String> instancesId = new ArrayList<String>();
		for (int index = 0; index < instancesCount; ++index) {
			Instance instance = instanceMapper.selectByPrimaryKey(instancesNode.get(index).textValue());
			if(null == instance)
				continue;
			if(!Util.isNullOrEmptyValue(instance.getFixedips())){
				ips.add(instance.getFixedips().split(",")[0]);
				netrowkIds.add(instance.getNetworkIds().split(",")[0]);
			}
			
			LBPoolMember poolMember = new LBPoolMember();
			poolMember.setAddress(instance.getFixedips());
			poolMember.setAdmin_state_up(true);
			poolMember.setProtocol_port(port);
			poolMembers.add(poolMember);
			poolMember.setInstanceId(instance.getId());
		}
		String subnetId = checkSubnetInfo(ostoken,ips,netrowkIds);
		for (LBPoolMember poolMemberWithSubnet : poolMembers) {
			poolMemberWithSubnet.setSubnet_id(subnetId);
		}
		
		loadbalancerCreateInfo.setAdmin_state_up(true);
		loadbalancerCreateInfo.setName(name);
		loadbalancerCreateInfo.setDescription(description);
		loadbalancerCreateInfo.setVip_subnet_id(subnetId);

		listenerCreateInfo.setAdmin_state_up(true);
		listenerCreateInfo.setName(String.format("%s_listener", name));
		listenerCreateInfo.setProtocol_port(port);
		listenerCreateInfo.setProtocol(protocol);
		
		lbPool.setAdmin_state_up(true);
		lbPool.setLb_algorithm(lb_algorithm);
	//	lbPool.setProtocol(type); //maybe change
		lbPool.setProtocol(protocol);
		lbPool.setName(String.format("%s_lbpool", name));
		
		healthMonitor.setType(type); //maybe change
		healthMonitor.setDelay(delay);
		healthMonitor.setTimeout(timeout);
		healthMonitor.setMax_retries(retries);
		healthMonitor.setHttp_method(httpMethod);
		healthMonitor.setUrl_path(urlPath);
		healthMonitor.setExpected_codes(expectedCodes);
		healthMonitor.setName(String.format("%s_healthMonitor", name));
		healthMonitor.setAdmin_state_up(true);
	}
	
	private String checkSubnetInfo(TokenOs ostoken,List<String> ips,List<String> networkIds) throws BusinessException{
		if(Util.isNullOrEmptyList(ips))
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		String subnetId = "";
		for (int index = 0; index < ips.size();++index) {
			Port port = portMapper.selectByIpAndNetworkId(ips.get(index),networkIds.get(index));
			if (null == port) {
				// it should not be null
				portService.refreshPorts(null, null);
				port = portMapper.selectByIpAndNetworkId(ips.get(index),networkIds.get(index));
			}
			if(null == port)
				continue;
			if (Util.isNullOrEmptyValue(subnetId)){
				subnetId = port.getSubnetId();
			}
			else {
				if (!subnetId.equals(port.getSubnetId()))
					throw new ResourceBusinessException(Message.CS_NETID_IS_NOT_SAME_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
			}
		}
		return subnetId;
	}
	
	private String getLoadbalancerCreatedBody(Loadbalancer loadbalancer){
		LoadbalancerJSON createdLoadbalancerJSON = new LoadbalancerJSON(loadbalancer);
		JsonHelper<LoadbalancerJSON, String> jsonHelp = new JsonHelper<LoadbalancerJSON, String>();
		return jsonHelp.generateJsonBodySimple(createdLoadbalancerJSON);
	}
	
	private String getListenerCreatedBody(Listener listener){
		ListenerJSON createdListenerJSON = new ListenerJSON(listener);
		JsonHelper<ListenerJSON, String> jsonHelp = new JsonHelper<ListenerJSON, String>();
		return jsonHelp.generateJsonBodySimple(createdListenerJSON);
	}
	
	private String getPoolCreatedBody(LBPool lbPool){
		LBPoolJSON createdPoolJSON = new LBPoolJSON(lbPool);
		JsonHelper<LBPoolJSON, String> jsonHelp = new JsonHelper<LBPoolJSON, String>();
		return jsonHelp.generateJsonBodySimple(createdPoolJSON);
	}
	
	private String getPoolMemberCreatedBody(LBPoolMember lbPoolMember){
		LBPoolMemberJSON createdPoolMemberJSON = new LBPoolMemberJSON(lbPoolMember);
		JsonHelper<LBPoolMemberJSON, String> jsonHelp = new JsonHelper<LBPoolMemberJSON, String>();
		return jsonHelp.generateJsonBodySimple(createdPoolMemberJSON);
	}
	
	private String getHealthMonitorMemberCreatedBody(LBHealthMonitor lbHealthMonitor){
		LBHealthMonitorJSON createdHealthMonitorJSON = new LBHealthMonitorJSON(lbHealthMonitor);
		JsonHelper<LBHealthMonitorJSON, String> jsonHelp = new JsonHelper<LBHealthMonitorJSON, String>();
		return jsonHelp.generateJsonBodySimple(createdHealthMonitorJSON);
	}
	
	private List<LBPool> getPoolsFromDB(String tenantId,int limitItems){
		List<LBPool> poolsFromDB = null;
		if(-1 == limitItems){
			poolsFromDB = loadbalancerPoolMapper.selectAllByTenantId(tenantId);
		}else{
			poolsFromDB = loadbalancerPoolMapper.selectAllByTenantIdWithLimit(tenantId,limitItems);
		}
		return poolsFromDB;
	}
	
	private void storePoolToDB(LBPool pool){
		if(null == pool)
			return;
		loadbalancerPoolMapper.insertOrUpdate(pool);
//		if(null == loadbalancerPoolMapper.selectByPrimaryKey(pool.getId()))
//			loadbalancerPoolMapper.insertSelective(pool);
//		else
//			loadbalancerPoolMapper.updateByPrimaryKeySelective(pool);
	}
	
	private List<LBPool> storePoolsToDB(List<LBPool> pools){
		if(Util.isNullOrEmptyList(pools))
			return null;
		loadbalancerPoolMapper.insertOrUpdateBatch(pools);
//		for (LBPool pool : pools) {
//			storePoolToDB(pool);
//		}
		return pools;
	}
	
	private List<Loadbalancer> getLoadbalancersFromDB(String tenantId,int limitItems){
		List<Loadbalancer> loadbalancersFromDB = null;
		if(-1 == limitItems){
			loadbalancersFromDB = loadbalancerMapper.selectAllByTenantId(tenantId);
		}else{
			loadbalancersFromDB = loadbalancerMapper.selectAllByTenantIdWithLimit(tenantId,limitItems);
		}
		return loadbalancersFromDB;
	}
	
	private void storeLoadbalancerToDB(Loadbalancer loadbalancer){
		Loadbalancer existingLoadBalancer = loadbalancerMapper.selectByPrimaryKey(loadbalancer.getId());
		if(null != existingLoadBalancer)
			loadbalancer.setMillionSeconds(existingLoadBalancer.getMillionSeconds());
	//		loadbalancer.setCreatedAt(existingLoadBalancer.getCreatedAt());
		loadbalancerMapper.insertOrUpdate(loadbalancer);
//		if(null == existingLoadBalancer)
//			loadbalancerMapper.insertSelective(loadbalancer);
//		else{
//			loadbalancer.setCreatedAt(existingLoadBalancer.getCreatedAt());
//			loadbalancerMapper.updateByPrimaryKeySelective(loadbalancer);
//		}	
		
		//update monitor objs name
		zabbixService.updateMonitorObjName(loadbalancer.getId(), loadbalancer.getName());
	}
	
	private List<Loadbalancer> storeLoadbalancersToDB(List<Loadbalancer> loadbalancers){
		if(Util.isNullOrEmptyList(loadbalancers))
			return null;
		for (Loadbalancer loadbalancer : loadbalancers) {
			storeLoadbalancerToDB(loadbalancer);
		}
		return loadbalancers;
	}
	
	private void storePoolMemberToDB(LBPoolMember poolMember){
		if(null == poolMember)
			return;
		loadbalancerPoolMemberMapper.insertOrUpdate(poolMember);
//		if(null == loadbalancerPoolMemberMapper.selectByPrimaryKey(poolMember.getId()))
//			loadbalancerPoolMemberMapper.insertSelective(poolMember);
//		else
//			loadbalancerPoolMemberMapper.updateByPrimaryKeySelective(poolMember);
	}
	
	private List<LBPoolMember> storeLBPoolMembersToDB(List<LBPoolMember> poolMembers){
		if(Util.isNullOrEmptyList(poolMembers))
			return null;
		loadbalancerPoolMemberMapper.insertOrUpdateBatch(poolMembers);
//		for (LBPoolMember poolMember : poolMembers) {
//			storePoolMemberToDB(poolMember);
//		}
		return poolMembers;
	}
	
	private void makeLoadbalancerInfo(Loadbalancer loadbalancer, TokenOs ostoken)
			throws BusinessException {
		String poolIds = loadbalancer.getPoolIds();
		if (Util.isNullOrEmptyValue(poolIds))
			return;
		List<Instance> instances = new ArrayList<Instance>();
		List<String> ports = new ArrayList<String>();

		String[] poolIdArray = poolIds.split(",");
		for (int index = 0; index < poolIdArray.length; ++index) {
			LBPool pool = getPool(poolIdArray[index], ostoken);
			setLoadbalancerInstanceInfo(pool, ports, instances, ostoken);
		}

		loadbalancer.setInstances(instances);
		loadbalancer.setPorts(ports);
	}
	
	private void setLoadbalancerInstanceInfo(LBPool pool, List<String> ports, List<Instance> instances, TokenOs ostoken) throws BusinessException {
		if (null == pool)
			return;
		String poolMemberIds = pool.getMembers_id();
		if (Util.isNullOrEmptyValue(poolMemberIds))
			return;

		String[] poolmemberIdArray = poolMemberIds.split(",");
		for (int poolMemberindex = 0; poolMemberindex < poolmemberIdArray.length; ++poolMemberindex) {
			LBPoolMember poolMember = getPoolMember(pool.getId(), poolmemberIdArray[poolMemberindex], ostoken);
			int port = poolMember.getProtocol_port();
			if (!ports.contains(Integer.toString(port)))
				ports.add(Integer.toString(port));
			//Instance instance = instanceMapper.selectInstanceByFixedIp(poolMember.getAddress());
			Instance instance = instanceMapper.selectByPrimaryKey(poolMember.getInstanceId());
			if (null != instance) {
				instances.add(instance);
			}
		}
		
		normalInstanceInfo(instances);
	}
	
	private void normalInstanceInfo(List<Instance> instances){
		for (Instance instance : instances) {
			instance.setSystemName(instance.getSourceName());
			List<String> ips = Util.stringToList(instance.getFixedips(),",");
			if(null != ips)
			   instance.setIps(ips);
			ips = Util.stringToList(instance.getFloatingips(),",");
			if(null != ips)
			    instance.setFloatingIps(ips);
		}
	}
	
	private List<Loadbalancer> getLimitLoadbalancerItems(List<Loadbalancer> loadbalancers,int limit){
		if(null == loadbalancers)
			return null;
		if(-1 != limit){
			if(limit <= loadbalancers.size())
				return loadbalancers.subList(0, limit);
		}
		return loadbalancers;
	}
	
	private List<LBPool> getLimitPoolItems(List<LBPool> pools,int limit){
		if(null == pools)
			return null;
		if(-1 != limit){
			if(limit <= pools.size())
				return pools.subList(0, limit);
		}
		return pools;
	}
	
	private void normalLoadbalancerCreatedTime(List<Loadbalancer> loadbalancers){
		if(null == loadbalancers)
			return;
		for(Loadbalancer loadbalancer : loadbalancers){
			if(null != loadbalancer.getMillionSeconds())
				loadbalancer.setCreatedAt(Util.millionSecond2Date(loadbalancer.getMillionSeconds()));
			loadbalancer.setName(StringHelper.ncr2String(loadbalancer.getName()));
			loadbalancer.setDescription(StringHelper.ncr2String(loadbalancer.getDescription()));
		}
	}
	
	private void updateSyncResourceInfo(String tenantId,String id,String status,String type,String region,String relatedResource,String name){
		SyncResource resource = new SyncResource();
		resource.setId(id);
		resource.setType(type);
		resource.setExpectedStatus(status);
		resource.setRegion(region);
		resource.setRelatedResource(relatedResource);
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
	
	private void checkResource(String id, Locale locale) throws BusinessException {
		SyncResource syncResource = syncResourceMapper.selectByPrimaryKey(id);
		if (null != syncResource)
			throw new ResourceBusinessException(Message.CS_RESOURCE_IS_DOING,ParamConstant.BAD_REQUEST_RESPONSE_CODE, locale);
	     return;
	}
	
	private void checkFloatingIP(String id, Locale locale) throws BusinessException {
		Loadbalancer loadbalancer = loadbalancerMapper.selectByPrimaryKey(id);
	    if(null == loadbalancer)
			throw new ResourceBusinessException(Message.CS_RESOURCE_IS_DOING,ParamConstant.BAD_REQUEST_RESPONSE_CODE, locale);
        if(!Util.isNullOrEmptyValue(loadbalancer.getFloatingIp()))
        	throw new ResourceBusinessException(Message.CS_HAVE_RELATED_FLOATINGIP_WITH_LB,ParamConstant.BAD_REQUEST_RESPONSE_CODE, locale);
        return;
	}
	
	private void updateRelatedInstanceInfo(List<String> instancesid,String id){
		List<Instance> instances = null;
		if(null == instancesid){
			instances = instanceMapper.selectListByLbid(id);
			if(Util.isNullOrEmptyList(instances))
				return;
			for(Instance instance : instances){
				instance.setLbid(null);
			}
			
		}else{
			instances = instanceMapper.selectListByInstanceIds(instancesid);
			if(Util.isNullOrEmptyList(instances))
				return;
			for(Instance instance : instances){
				instance.setLbid(id);
			}
		}
		instanceMapper.insertOrUpdateBatch(instances);	
	}
	
	private void checkName(String name,TokenOs ostoken)  throws BusinessException{
		if(Util.isNullOrEmptyValue(name))
			return;
		List<Loadbalancer> lbs = loadbalancerMapper.selectAllByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(lbs))
			return;
		for(Loadbalancer lb : lbs){
			if(name.equals(lb.getName()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_NAME_IS_SAME,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		return;
	}
	
	private void checkLoadbalancer(TokenOs ostoken)  throws BusinessException{
		List<Loadbalancer> lbs = loadbalancerMapper.selectAllByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(lbs))
			return;
		
		for(Loadbalancer lb : lbs){
			if(null != syncResourceMapper.selectByPrimaryKey(lb.getId()))
				throw new ResourceBusinessException(Message.CS_LB_IS_CREATING,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		return;
	}
}
