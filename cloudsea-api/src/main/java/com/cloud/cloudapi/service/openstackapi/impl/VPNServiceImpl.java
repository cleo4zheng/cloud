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

import com.cloud.cloudapi.dao.common.ResourceCreateProcessMapper;
import com.cloud.cloudapi.dao.common.ResourceEventMapper;
import com.cloud.cloudapi.dao.common.RouterMapper;
import com.cloud.cloudapi.dao.common.SubnetMapper;
import com.cloud.cloudapi.dao.common.SyncResourceMapper;
import com.cloud.cloudapi.dao.common.VPNMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.IPSecPolicyJSON;
import com.cloud.cloudapi.json.forgui.IkePolicyJSON;
import com.cloud.cloudapi.json.forgui.IpsecSiteConnectionJSON;
import com.cloud.cloudapi.json.forgui.VPNJSON;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.IPSecPolicy;
import com.cloud.cloudapi.pojo.openstackapi.forgui.IPSecSiteConnection;
import com.cloud.cloudapi.pojo.openstackapi.forgui.IkePolicy;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Policy;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceCreateProcess;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceEvent;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Router;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SyncResource;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VPN;
import com.cloud.cloudapi.service.businessapi.zabbix.ZabbixService;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.VPNService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("vpnService")
public class VPNServiceImpl implements VPNService {
	@Autowired
	private CloudConfig cloudconfig;

	@Autowired
	private VPNMapper vpnMapper;

	@Autowired
	private SubnetMapper subnetMapper;

	@Autowired
	private RouterMapper routerMapper;
	
	@Autowired
	private SyncResourceMapper syncResourceMapper;
	
	@Autowired
	private ResourceCreateProcessMapper resourceCreateProcessMapper;
	
	@Resource
	private ZabbixService zabbixService;

	@Autowired
	private ResourceEventMapper resourceEventMapper;
	
	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(VPNServiceImpl.class);
	
	public CloudConfig getCloudconfig() {
		return cloudconfig;
	}

	public void setCloudconfig(CloudConfig cloudconfig) {
		this.cloudconfig = cloudconfig;
	}

	@Resource
	private OSHttpClientUtil client;

	
	@Override
	public List<VPN> getVPNs(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
        List<VPN> vpns = vpnMapper.selectAllByTenantId(ostoken.getTenantid());
        if(!Util.isNullOrEmptyList(vpns)){
        	normalVPNsInfo(vpns);
        	return vpns;
        }
        
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/vpn/vpnservices", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
        Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				vpns = getVPNs(rs);
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
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				vpns = getVPNs(rs);
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
			throw new ResourceBusinessException(Message.CS_VPN_GET_FAILED,httpCode,locale);
			*/
		}
		normalVPNsInfo(vpns);
		return vpns;
	}

	@Override
	public VPN getVPN(String vpnId, TokenOs ostoken) throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		VPN vpn = vpnMapper.selectByPrimaryKey(vpnId);
		if(null != vpn){
			normalVPNInfo(vpn);
			return vpn;	
		}
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/vpn/vpnservices/");
		sb.append(vpnId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				vpn = getVPN(rs);
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
			try {
				vpn = getVPN(rs);
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
			throw new ResourceBusinessException(Message.CS_VPN_DETAIL_GET_FAILED,httpCode,locale);
		}
		if(null == vpn.getMillionSeconds())
			vpn.setMillionSeconds(Util.time2Millionsecond(vpn.getCreatedAt(), ParamConstant.TIME_FORMAT_01));
		vpnMapper.insertOrUpdate(vpn);
		//update monitor obj name
		zabbixService.updateMonitorObjName(vpn.getId(), vpn.getName());
		normalVPNInfo(vpn);
		return vpn;
	}

	@Override
	public VPN createVPN(String createBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException{
		
		VPN vpnCreateInfo = new VPN();
		IPSecSiteConnection ipsecSiteCon = new IPSecSiteConnection();
		IkePolicy ikePolicy = new IkePolicy();
		IPSecPolicy ipsecPolicy = new IPSecPolicy();
		Locale locale = new Locale(ostoken.getLocale());

		makeVPNCreateInfo(createBody,false,vpnCreateInfo,ikePolicy,ipsecPolicy,ipsecSiteCon);
		checkName(vpnCreateInfo.getName(),ostoken);
		
		checkRouterResource(vpnCreateInfo.getRouter_id(),vpnCreateInfo.getSubnet_id(),locale);
		
		String vpnCreateBody = getVPNCreatedBody(vpnCreateInfo);
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		if(Util.isNullOrEmptyValue(ikePolicy.getId()))
			ikePolicy = createIkePolicy(null, vpnCreateInfo.getName(),ostoken);
		if(Util.isNullOrEmptyValue(ipsecPolicy.getId()))
			ipsecPolicy = createIPSecPolicy(null, vpnCreateInfo.getName(),ostoken);
		Map<String, String> rs = client.httpDoPost(url+"/v2.0/vpn/vpnservices", headers,vpnCreateBody);
		Util.checkResponseBody(rs,locale);

		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		
		VPN vpn = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));;
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				vpn = getVPN(rs);
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
			rs =  client.httpDoPost(url+"/v2.0/vpn/vpnservices", headers,vpnCreateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				vpn = getVPN(rs);
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
			throw new ResourceBusinessException(Message.CS_VPN_CREATE_FAILED,httpCode,locale);
		}
		
		String ipsecSiteConCreateBody = makeIpsecConnectionBody(null,ostoken.getTenantid(),vpn,ikePolicy,ipsecPolicy,ipsecSiteCon,locale);
		ipsecSiteCon = createIPSecConnection(ipsecSiteConCreateBody, false,ostoken);
		vpn.setMillionSeconds(Util.getCurrentMillionsecond());
		//vpn.setCreatedAt(Util.getCurrentDate());
		vpn.setIkePolicyId(ikePolicy.getId());
		vpn.setIkePolicyName(ikePolicy.getName());
		vpn.setIpsecSiteconId(ipsecSiteCon.getId());
		vpn.setIpsecPolicyId(ipsecPolicy.getId());
		vpn.setIpsecPolicyName(ipsecPolicy.getName());
		storeVPNToDB(vpn);
		
		addRelatedRouterInfo(vpn.getId(),vpnCreateInfo.getRouter_id());
		updateSyncResourceInfo(ostoken.getTenantid(),vpn.getId(),null,ParamConstant.DOWN_STATUS/*ACTIVE_STATUS*/,ostoken.getCurrentRegion(),null,vpn.getName());
		storeResourceEventInfo(ostoken.getTenantid(),vpn.getId(),ParamConstant.VPNAAS,null,ParamConstant.ACTIVE_STATUS,vpn.getMillionSeconds());
		return vpn;
	}
	
	private void storeVPNToDB(VPN vpn){
		if(null == vpn)
			return;
		vpnMapper.insertOrUpdate(vpn);
//		if(null == vpnMapper.selectByPrimaryKey(vpn.getId())){
//			vpnMapper.insertSelective(vpn);
//		}else{
//			vpnMapper.updateByPrimaryKeySelective(vpn);
//		}
		
		//update monitor obj name
		zabbixService.updateMonitorObjName(vpn.getId(), vpn.getName());
	}
	
	@Override
	public VPN updateVPN(String vpnId,String updateBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException{

		Locale locale = new Locale(ostoken.getLocale());
		checkResource(vpnId,locale);
		
		VPN vpnUpdateInfo = new VPN();
		makeVPNCreateInfo(updateBody,true,vpnUpdateInfo,null,null,null);
		checkName(vpnUpdateInfo.getName(),ostoken);
		String vpnUpdateBody = getVPNCreatedBody(vpnUpdateInfo);
		
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/vpn/vpnservices/");
		sb.append(vpnId);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		Map<String, String> rs = client.httpDoPut(sb.toString(), headers,vpnUpdateBody);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		VPN vpn = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				vpn = getVPN(rs);
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
			rs =  client.httpDoPost(sb.toString(), headers,vpnUpdateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				vpn = getVPN(rs);
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
			throw new ResourceBusinessException(Message.CS_VPN_UPDATE_FAILED,httpCode,locale);
		}
	
		VPN vpnFromDB = vpnMapper.selectByPrimaryKey(vpnId);
		normalVPNInfo(vpn);
		if(null == vpnUpdateInfo.getAdmin_state_up() || true == vpnUpdateInfo.getAdmin_state_up()){
			updateSyncResourceInfo(ostoken.getTenantid(),vpn.getId(),vpnFromDB.getStatus(),ParamConstant.ACTIVE_STATUS,ostoken.getCurrentRegion(),"admin_state_up:1",vpn.getName());
			storeResourceEventInfo(ostoken.getTenantid(),vpn.getId(),ParamConstant.VPNAAS,vpnFromDB.getStatus(),ParamConstant.ACTIVE_STATUS,Util.getCurrentMillionsecond());
		}else{
			updateSyncResourceInfo(ostoken.getTenantid(),vpn.getId(),vpnFromDB.getStatus(),ParamConstant.DOWN_STATUS,ostoken.getCurrentRegion(),"admin_state_up:0",vpn.getName());
			storeResourceEventInfo(ostoken.getTenantid(),vpn.getId(),ParamConstant.VPNAAS,vpnFromDB.getStatus(),ParamConstant.STOPPED_STATUS,Util.getCurrentMillionsecond());

		}
		return vpn;
	}
	
	@Override
	public void removeVPN(String vpnId,TokenOs ostoken) throws BusinessException{
		
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(vpnId,locale);
		
		VPN vpn = vpnMapper.selectByPrimaryKey(vpnId);
		if(null != vpn){
			try{
				this.removeIPSecSiteConnection(vpn.getIpsecSiteconId(),ostoken);	
			}catch (Exception e){
				throw new ResourceBusinessException(Message.CS_VPN_DELETE_FAILED,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
			}  
		}
		
		String region = ostoken.getCurrentRegion(); 
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/vpn/vpnservices/");
		sb.append(vpnId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoDelete(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE:
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE:{
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
			rs =  client.httpDoDelete(sb.toString(), ostoken.getTokenid());
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE || httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE)
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
//			throw new ResourceBusinessException(Message.CS_VPN_DELETE_FAILED,httpCode,locale);
		}
		

		try {
			this.removeIPSecPolicy(vpn.getIpsecPolicyId(), ostoken);
			this.removeIkePolicy(vpn.getIkePolicyId(), ostoken);
		} catch (Exception e) {
			log.error(e);
		}
		
		vpnMapper.deleteByPrimaryKey(vpnId);
	
		//delete monitor obj about VPN
		zabbixService.deleteMonitorObj(vpnId);
		removeRelatedupdateRouterInfo(vpnId);
		updateSyncResourceInfo(ostoken.getTenantid(),vpn.getId(),ParamConstant.ACTIVE_STATUS,ParamConstant.DELETED_STATUS,ostoken.getCurrentRegion(),ParamConstant.ROUTER+":"+vpn.getRouter_id(),vpn.getName());
		storeResourceEventInfo(ostoken.getTenantid(),vpn.getId(),ParamConstant.VPNAAS,vpn.getStatus(),ParamConstant.DELETED_STATUS,Util.getCurrentMillionsecond());
	}
	
	@Override
	public List<IkePolicy> getIkePolicies(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException{
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/vpn/ikepolicies", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		List<IkePolicy> ikePolicies = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				ikePolicies = getIkePolicies(rs);
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
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				ikePolicies = getIkePolicies(rs);
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
			throw new ResourceBusinessException(Message.CS_IKEPOLICY_GET_FAILED,httpCode,locale);
		}
		return ikePolicies;
	}
	
	@Override
	public IkePolicy getIkePolicy(String policyId,TokenOs ostoken) throws BusinessException{
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/vpn/ikepolicies/");
		sb.append(policyId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		IkePolicy ikePolicy = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				ikePolicy = getIkePolicy(rs);
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
			try {
				ikePolicy = getIkePolicy(rs);
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
			throw new ResourceBusinessException(Message.CS_IKEPOLICY_DETAIL_GET_FAILED,httpCode,locale);
		}
		return ikePolicy;	
	}
	
	@Override
	public IkePolicy createIkePolicy(String createBody,String vpnName,TokenOs ostoken) throws BusinessException{
		
		Locale locale = new Locale(ostoken.getLocale());
		IkePolicy ikePolicy = makeIkePolicyCreateInfo(createBody,vpnName,ostoken.getTenantid(),locale);
		String ikePolicyCreateBody = getIkePolicyCreatedBody(ikePolicy);
		
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		Map<String, String> rs = client.httpDoPost(url+"/v2.0/vpn/ikepolicies", headers,ikePolicyCreateBody);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
	//	IkePolicy ikePolicy = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				ikePolicy = getIkePolicy(rs);
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
			rs =  client.httpDoPost(url+"/v2.0/vpn/ikepolicies", headers,ikePolicyCreateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				ikePolicy = getIkePolicy(rs);
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
			throw new ResourceBusinessException(Message.CS_IKEPOLICY_CREATE_FAILED,httpCode,locale);
		}
		return ikePolicy;
	}
	
	@Override
	public void removeIkePolicy(String ikePolicyId,TokenOs ostoken) throws BusinessException{
		
		String region = ostoken.getCurrentRegion(); 
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/vpn/ikepolicies/");
		sb.append(ikePolicyId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoDelete(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE:
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE:{
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
			rs =  client.httpDoDelete(sb.toString(), ostoken.getTokenid());
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE || httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE)
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
//			throw new ResourceBusinessException(Message.CS_VPN_DELETE_FAILED,httpCode,locale);
		}

	}
	
	@Override
	public List<IPSecPolicy> getIPSecPolicies(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException{
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/vpn/ipsecpolicies", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		List<IPSecPolicy> ipSecPolicies = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				ipSecPolicies = getIPSecPolicies(rs);
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
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				ipSecPolicies = getIPSecPolicies(rs);
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
			throw new ResourceBusinessException(Message.CS_IPSECPOLICY_GET_FAILED,httpCode,locale);
		}
		return ipSecPolicies;
	}
	
	@Override
	public IPSecPolicy getIPSecPolicy(String policyId,TokenOs ostoken) throws BusinessException{
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/vpn/ipsecpolicies/");
		sb.append(policyId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		IPSecPolicy ipSecPolicy = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				ipSecPolicy = getIPSecPolicy(rs);
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
			try {
				ipSecPolicy = getIPSecPolicy(rs);
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
			throw new ResourceBusinessException(Message.CS_IPSECPOLICY_DETAIL_GET_FAILED,httpCode,locale);
		}
		return ipSecPolicy;	
	}
	
	@Override
	public IPSecPolicy createIPSecPolicy(String createBody,String vpnName,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		IPSecPolicy ipSecPolicy = makeIPSecPolicyCreateInfo(createBody,vpnName,ostoken.getTenantid(),locale);
		String ipsecPolicyCreateBody = getIPSecPolicyCreatedBody(ipSecPolicy);
		
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		Map<String, String> rs = client.httpDoPost(url+"/v2.0/vpn/ipsecpolicies", headers,ipsecPolicyCreateBody);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				ipSecPolicy = getIPSecPolicy(rs);
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
			rs =  client.httpDoPost(url+"/v2.0/vpn/ipsecpolicies", headers,ipsecPolicyCreateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				ipSecPolicy = getIPSecPolicy(rs);
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
			throw new ResourceBusinessException(Message.CS_IPSECPOLICY_CREATE_FAILED,httpCode,locale);
		}
		return ipSecPolicy;
	}
	
	@Override
	public void removeIPSecPolicy(String ipsecPolicyId,TokenOs ostoken) throws BusinessException{
		String region = ostoken.getCurrentRegion(); 
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/vpn/ipsecpolicies/");
		sb.append(ipsecPolicyId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoDelete(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE:
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE:{
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
			rs =  client.httpDoDelete(sb.toString(), ostoken.getTokenid());
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		    failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE || httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE)
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
//			throw new ResourceBusinessException(Message.CS_VPN_DELETE_FAILED,httpCode,locale);
		}
	}
	
	@Override
	public List<IPSecSiteConnection> getIPSecConnections(Map<String, String> paramMap, TokenOs ostoken) throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/ipsec-site-connections", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		List<IPSecSiteConnection> ipSecSiteCons = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				ipSecSiteCons = getIPSecSiteConnections(rs);
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
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				ipSecSiteCons = getIPSecSiteConnections(rs);
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
			throw new ResourceBusinessException(Message.CS_IPSEC_CONNECTION_GET_FAILED,httpCode,locale);
		}
		return ipSecSiteCons;
	}
	
	@Override
	public IPSecSiteConnection getIPSecConnection(String ipsecConnectionId, TokenOs ostoken) throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/vpn/ipsec-site-connections/");
		sb.append(ipsecConnectionId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		IPSecSiteConnection ipSecSiteCon = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				ipSecSiteCon = getIPSecSiteConnection(rs);
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
			try {
				ipSecSiteCon = getIPSecSiteConnection(rs);
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
			throw new ResourceBusinessException(Message.CS_IPSEC_CONNECTION_DETAIL_GET_FAILED,locale);
		}
		return ipSecSiteCon;
	}
	
	@Override
	public IPSecSiteConnection createIPSecConnection(String createBody,Boolean convert,TokenOs ostoken) throws BusinessException, JsonParseException, JsonMappingException, IOException{
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		if(true == convert)
			createBody = makeIpsecConnectionBody(createBody,ostoken.getTenantid(),null,null,null,null,locale);
		Map<String, String> rs = client.httpDoPost(url+"/v2.0/vpn/ipsec-site-connections", headers,createBody);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		IPSecSiteConnection ipSecSiteConnection = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				ipSecSiteConnection = getIPSecSiteConnection(rs);
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
			rs =  client.httpDoPost(url+"/v2.0/vpn/ipsec-site-connections", headers,createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				ipSecSiteConnection = getIPSecSiteConnection(rs);
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
			throw new ResourceBusinessException(Message.CS_IPSEC_CONNECTION_CREATE_FAILED,httpCode,locale);
		}
		return ipSecSiteConnection;
	}
	
	@Override
	public void removeIPSecSiteConnection(String ipsecSiteConId,TokenOs ostoken) throws BusinessException{
		String region = ostoken.getCurrentRegion(); 
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/vpn/ipsec-site-connections/");
		sb.append(ipsecSiteConId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoDelete(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE:
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE:{
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
			rs =  client.httpDoDelete(sb.toString(), ostoken.getTokenid());
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE || httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE)
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
//			throw new ResourceBusinessException(Message.CS_VPN_DELETE_FAILED,httpCode,locale);
		}
	}
	
	private VPN getVPN(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode vpnNode = rootNode.path(ResponseConstant.VPNSERVICE);
		return getVPNInfo(vpnNode);
	}
	
	private List<VPN> getVPNs(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode vpnsNode = rootNode.path(ResponseConstant.VPNSERVICES);
		int vpnsCount = vpnsNode.size();
		if (0 == vpnsCount)
			return null;

		List<VPN> vpns = new ArrayList<VPN>();
		for (int index = 0; index < vpnsCount; ++index) {
			VPN vpn = getVPNInfo(vpnsNode.get(index));
			if (null == vpn)
				continue;
			vpns.add(vpn);
		}
		return vpns;
	}

	private VPN getVPNInfo(JsonNode vpnNode) {
		if (null == vpnNode)
			return null;

		VPN vpn = new VPN();
		vpn.setRouter_id(vpnNode.path(ResponseConstant.ROUTER_ID).textValue());
		vpn.setStatus(vpnNode.path(ResponseConstant.STATUS).textValue());
		vpn.setName(vpnNode.path(ResponseConstant.NAME).textValue());
		vpn.setExternal_v6_ip(vpnNode.path(ResponseConstant.EXTERNAL_V6_IP).textValue());
		vpn.setAdmin_state_up(vpnNode.path(ResponseConstant.ADMIN_STATE_UP).booleanValue());
		vpn.setSubnet_id(vpnNode.path(ResponseConstant.SUBNET_ID).textValue());
		vpn.setTenant_id(vpnNode.path(ResponseConstant.TENANT_ID).textValue());
		vpn.setExternal_v4_ip(vpnNode.path(ResponseConstant.EXTERNAL_V4_IP).textValue());
		vpn.setId(vpnNode.path(ResponseConstant.ID).textValue());
		vpn.setDescription(vpnNode.path(ResponseConstant.DESCRIPTION).textValue());

		return vpn;
	}
	
	private IPSecPolicy getIPSecPolicy(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode ipSecPolicyNode = rootNode.path(ResponseConstant.IPSECPOLICY);
		return getIPSecPolicyInfo(ipSecPolicyNode);
	}
	
	private List<IPSecSiteConnection> getIPSecSiteConnections(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode ipsecSiteConsNode = rootNode.path(ResponseConstant.IPSEC_SITE_CONNECTIONS);
		int ipsecSiteConsCount = ipsecSiteConsNode.size();
		if (0 == ipsecSiteConsCount)
			return null;

		List<IPSecSiteConnection> ipSecSiteCons = new ArrayList<IPSecSiteConnection>();
		for (int index = 0; index < ipsecSiteConsCount; ++index) {
			IPSecSiteConnection ipSecSiteCon = getIPSecSiteConnectionInfo(ipsecSiteConsNode.get(index));
			if (null == ipSecSiteCon)
				continue;
			ipSecSiteCons.add(ipSecSiteCon);
		}
		return ipSecSiteCons;
	}
	
	private IPSecSiteConnection getIPSecSiteConnection(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode ipSecSiteConNode = rootNode.path(ResponseConstant.IPSEC_SITE_CONNECTION);
		return getIPSecSiteConnectionInfo(ipSecSiteConNode);
	}
	
	private IkePolicy getIkePolicy(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode ikePolicyNode = rootNode.path(ResponseConstant.IKEPOLICY);
		return getIkePolicyInfo(ikePolicyNode);
	}
	
	private List<IkePolicy> getIkePolicies(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode ikepoliciesNode = rootNode.path(ResponseConstant.IKEPOLICIES);
		int ikepoliciesCount = ikepoliciesNode.size();
		if (0 == ikepoliciesCount)
			return null;

		List<IkePolicy> ikepolicies = new ArrayList<IkePolicy>();
		for (int index = 0; index < ikepoliciesCount; ++index) {
			IkePolicy ikepolicy = getIkePolicyInfo(ikepoliciesNode.get(index));
			if (null == ikepolicy)
				continue;
			ikepolicies.add(ikepolicy);
		}
		return ikepolicies;
	}

	private List<IPSecPolicy> getIPSecPolicies(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode ipsecpoliciesNode = rootNode.path(ResponseConstant.IPSECPOLICY);
		int ipsecpoliciesCount = ipsecpoliciesNode.size();
		if (0 == ipsecpoliciesCount)
			return null;

		List<IPSecPolicy> ipSecpolicies = new ArrayList<IPSecPolicy>();
		for (int index = 0; index < ipsecpoliciesCount; ++index) {
			IPSecPolicy ipSecpolicy = getIPSecPolicyInfo(ipsecpoliciesNode.get(index));
			if (null == ipSecpolicy)
				continue;
			ipSecpolicies.add(ipSecpolicy);
		}
		return ipSecpolicies;
	}
	
	private IkePolicy getIkePolicyInfo(JsonNode ikePolicyNode) {
		if (null == ikePolicyNode)
			return null;
		IkePolicy ikePolicy = new IkePolicy();
		setPolicyInfo(ikePolicy,ikePolicyNode);
		ikePolicy.setPhase1_negotiation_mode(ikePolicyNode.path(ResponseConstant.PHASE1_NEGOTIATION_MODE).textValue());
		ikePolicy.setIke_version(ikePolicyNode.path(ResponseConstant.IKE_VERSION).textValue());
		return ikePolicy;
	}
	
	private void setPolicyInfo(Policy policy,JsonNode policyNode){
		
		policy.setName(policyNode.path(ResponseConstant.NAME).textValue());
		policy.setTenant_id(policyNode.path(ResponseConstant.TENANT_ID).textValue());
		policy.setAuth_algorithm(policyNode.path(ResponseConstant.AUTH_ALGORITHM).textValue());
		policy.setEncryption_algorithm(policyNode.path(ResponseConstant.ENCRYPTION_ALGORITHM).textValue());
		policy.setPfs(policyNode.path(ResponseConstant.PFS).textValue());
		policy.setId(policyNode.path(ResponseConstant.ID).textValue());
		policy.setDescription(policyNode.path(ResponseConstant.DESCRIPTION).textValue());
		
		JsonNode lifetimeNode = policyNode.path(ResponseConstant.LIFETIME);
		if(null != lifetimeNode){
			policy.makeLifetime(lifetimeNode.path(ResponseConstant.UNITS).textValue(), lifetimeNode.path(ResponseConstant.VALUE).textValue());
		}
	}
	
	private IPSecPolicy getIPSecPolicyInfo(JsonNode ipsecPolicyNode) {
		if (null == ipsecPolicyNode)
			return null;
		IPSecPolicy ipSecPolicy = new IPSecPolicy();
		setPolicyInfo(ipSecPolicy,ipsecPolicyNode);
		ipSecPolicy.setEncapsulation_mode(ipsecPolicyNode.path(ResponseConstant.ENCAPSULATION_MODE).textValue());
		ipSecPolicy.setTransform_protocol(ipsecPolicyNode.path(ResponseConstant.TRANSFORM_PROTOCOL).textValue());
		return ipSecPolicy;
	}
	
	private IPSecSiteConnection getIPSecSiteConnectionInfo(JsonNode ipsecSiteConNode){
		if (null == ipsecSiteConNode)
			return null;
		IPSecSiteConnection ipSecSiteCon = new IPSecSiteConnection();
		ipSecSiteCon.setId(ipsecSiteConNode.path(ResponseConstant.ID).textValue());
		ipSecSiteCon.setPsk(ipsecSiteConNode.path(ResponseConstant.PSK).textValue());
		ipSecSiteCon.setInitiator(ipsecSiteConNode.path(ResponseConstant.INITIATOR).textValue());
		ipSecSiteCon.setName(ipsecSiteConNode.path(ResponseConstant.NAME).textValue());
		ipSecSiteCon.setAdmin_state_up(ipsecSiteConNode.path(ResponseConstant.ADMIN_STATE_UP).booleanValue());
		ipSecSiteCon.setTenant_id(ipsecSiteConNode.path(ResponseConstant.TENANT_ID).textValue());
		ipSecSiteCon.setAuth_mode(ipsecSiteConNode.path(ResponseConstant.AUTH_MODE).textValue());
		ipSecSiteCon.setMtu(ipsecSiteConNode.path(ResponseConstant.MTU).intValue());
		ipSecSiteCon.setPeer_ep_group_id(ipsecSiteConNode.path(ResponseConstant.PEER_EP_GROUP_ID).textValue());
		ipSecSiteCon.setIkepolicy_id(ipsecSiteConNode.path(ResponseConstant.IKEPOLICY_ID).textValue());
		ipSecSiteCon.setVpnservice_id(ipsecSiteConNode.path(ResponseConstant.VPNSERVICE_ID).textValue());
		ipSecSiteCon.setRoute_mode(ipsecSiteConNode.path(ResponseConstant.ROUTE_MODE).textValue());
		ipSecSiteCon.setIpsecpolicy_id(ipsecSiteConNode.path(ResponseConstant.IPSECPOLICY_ID).textValue());
		ipSecSiteCon.setLocal_ep_group_id(ipsecSiteConNode.path(ResponseConstant.LOCAL_EP_GROUP_ID).textValue());
		ipSecSiteCon.setPeer_address(ipsecSiteConNode.path(ResponseConstant.PEER_ADDRESS).textValue());
		ipSecSiteCon.setPeer_id(ipsecSiteConNode.path(ResponseConstant.PEER_ID).textValue());
		ipSecSiteCon.setDescription(ipsecSiteConNode.path(ResponseConstant.DESCRIPTION).textValue());
		
		return ipSecSiteCon;
	}
	
	private String makeIpsecConnectionBody(String createBody,String tenantId,VPN vpn,IkePolicy ikePolicy,IPSecPolicy ipsecPolicy,IPSecSiteConnection ipsecSiteCon,Locale locale) throws ResourceBusinessException {
		if(null == ipsecSiteCon){
			try{
				ObjectMapper mapper = new ObjectMapper();
				ipsecSiteCon = mapper.readValue(createBody, IPSecSiteConnection.class);	
				ipsecSiteCon.setTenant_id(tenantId);
			}catch(Exception e){
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.NOT_FOUND_RESPONSE_CODE,locale);
			}
		}else{
			String name = String.format("%s_vpnConnection", vpn.getName());
			ipsecSiteCon.setName(name);
			ipsecSiteCon.setIkepolicy_id(ikePolicy.getId());
			ipsecSiteCon.setIpsecpolicy_id(ipsecPolicy.getId());
			ipsecSiteCon.setVpnservice_id(vpn.getId());
			ipsecSiteCon.setTenant_id(tenantId);
		}
		IpsecSiteConnectionJSON ipsecSiteConnectionCreate = new IpsecSiteConnectionJSON(ipsecSiteCon);
		JsonHelper<IpsecSiteConnectionJSON, String> jsonHelp = new JsonHelper<IpsecSiteConnectionJSON, String>();
		return jsonHelp.generateJsonBodySimple(ipsecSiteConnectionCreate);
	}
	
	private IPSecPolicy makeIPSecPolicyCreateInfo(String createBody,String vpnName,String tenantId,Locale locale) throws ResourceBusinessException{
		IPSecPolicy ipsecPolicy = null;
		if(Util.isNullOrEmptyValue(createBody)){
			ipsecPolicy = new IPSecPolicy();
			String name = String.format("%s_ipsecPolicy", vpnName);
			ipsecPolicy.setName(name);
			ipsecPolicy.setTenant_id(tenantId);
		}else{
			ObjectMapper mapper = new ObjectMapper();
			try {
				ipsecPolicy = mapper.readValue(createBody, IPSecPolicy.class);
				ipsecPolicy.setTenant_id(tenantId);
			} catch (Exception e) {
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.NOT_FOUND_RESPONSE_CODE,locale);
			}
		}
		return ipsecPolicy;
	}
	
	private String getIPSecPolicyCreatedBody(IPSecPolicy ipsecPolicy){
		IPSecPolicyJSON createdIPSecPolicyJSON = new IPSecPolicyJSON(ipsecPolicy);
		JsonHelper<IPSecPolicyJSON, String> jsonHelp = new JsonHelper<IPSecPolicyJSON, String>();
		return jsonHelp.generateJsonBodySimple(createdIPSecPolicyJSON);
	}
	
	private IkePolicy makeIkePolicyCreateInfo(String createBody,String vpnName,String tenantId,Locale locale) throws ResourceBusinessException{
		IkePolicy ikePolicy = null;
		if(Util.isNullOrEmptyValue(createBody)){
			ikePolicy = new IkePolicy();
			String name = String.format("%s_ikePolicy", vpnName);
			ikePolicy.setName(name);
			ikePolicy.setTenant_id(tenantId);
		}else{
			ObjectMapper mapper = new ObjectMapper();
			try {
				ikePolicy = mapper.readValue(createBody, IkePolicy.class);
				ikePolicy.setTenant_id(tenantId);
			} catch (Exception e) {
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.NOT_FOUND_RESPONSE_CODE,locale);
			}
		}
		return ikePolicy;
	}
	
	private String getIkePolicyCreatedBody(IkePolicy ikePolicy){
		IkePolicyJSON createdIkePolicyJSON = new IkePolicyJSON(ikePolicy);
		JsonHelper<IkePolicyJSON, String> jsonHelp = new JsonHelper<IkePolicyJSON, String>();
		return jsonHelp.generateJsonBodySimple(createdIkePolicyJSON);
	}
	
	private void makeVPNCreateInfo(String createBody,Boolean update,VPN vpnCreateInfo,IkePolicy ikePolicy,IPSecPolicy ipsecPolicy,IPSecSiteConnection ipsecSiteCon)throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(createBody);
		if(!rootNode.path(ResponseConstant.NAME).isMissingNode())
			vpnCreateInfo.setName(rootNode.path(ResponseConstant.NAME).textValue());
		if(!rootNode.path(ResponseConstant.DESCRIPTION).isMissingNode())
			vpnCreateInfo.setDescription(rootNode.path(ResponseConstant.DESCRIPTION).textValue());
		if(!rootNode.path(ResponseConstant.ADMIN_STATE_UP).isMissingNode())
			vpnCreateInfo.setAdmin_state_up(Util.string2Boolean(rootNode.path(ResponseConstant.ADMIN_STATE_UP).textValue()));
		if(false == update){
			//vpnCreateInfo.setAdmin_state_up(true);
			vpnCreateInfo.setSubnet_id(rootNode.path(ResponseConstant.SUBNET_ID).textValue());
			vpnCreateInfo.setRouter_id(rootNode.path(ResponseConstant.ROUTER_ID).textValue());	
		}
		if(null != ipsecSiteCon){
			ipsecSiteCon.setPeer_address(rootNode.path(ResponseConstant.PEER_ADDRESS).textValue());
			ipsecSiteCon.setPeer_id(ipsecSiteCon.getPeer_address());
			List<String> peerCidrs = new ArrayList<String>();
			peerCidrs.add(rootNode.path(ResponseConstant.PEER_CIDR).textValue());
			ipsecSiteCon.setPeer_cidrs(peerCidrs);
			ipsecSiteCon.setPsk("secret");	
		}
		
		if(!rootNode.path(ResponseConstant.IKE_POLICY_NAME).isMissingNode() && null != ikePolicy){
			ikePolicy.setId(rootNode.path(ResponseConstant.IKE_POLICY_NAME).textValue());
		}
		
		if(!rootNode.path(ResponseConstant.IPSEC_POLICY_NAME).isMissingNode() && null != ipsecPolicy){
			ipsecPolicy.setId(rootNode.path(ResponseConstant.IPSEC_POLICY_NAME).textValue());
		}
	}
	
	private String getVPNCreatedBody(VPN vpn){
		VPNJSON createdVPNJSON = new VPNJSON(vpn);
		JsonHelper<VPNJSON, String> jsonHelp = new JsonHelper<VPNJSON, String>();
		return jsonHelp.generateJsonBodySimple(createdVPNJSON);
	}
	
	private void normalVPNsInfo(List<VPN> vpns) {
		if (null == vpns)
			return;
		for (VPN vpn : vpns) {
			normalVPNInfo(vpn);
		}
	}
	
	private void normalVPNInfo(VPN vpn){
		String subnetId = vpn.getSubnet_id();
		String routerId = vpn.getRouter_id();
		if(null != subnetId)
			vpn.setSubnet(subnetMapper.selectByPrimaryKey(subnetId));
		if(null != routerId)
			vpn.setRouter(routerMapper.selectByPrimaryKey(routerId));
//		if(null != vpn.getMillionSeconds())
//			vpn.setCreatedAt(Util.millionSecond2Date(vpn.getMillionSeconds()));
	}
	
	private void updateSyncResourceInfo(String tenantId,String id,String orgStatus,String expectedStatus,String region,String relatedResource,String name){
		SyncResource resource = new SyncResource();
		resource.setId(id);
		resource.setType(ParamConstant.VPN);
		resource.setOrgStatus(orgStatus);
		resource.setExpectedStatus(expectedStatus);
		resource.setRegion(region);
		resource.setRelatedResource(relatedResource);
		syncResourceMapper.insertSelective(resource);
		
		ResourceCreateProcess createProcess = new ResourceCreateProcess();
		createProcess.setId(id);
		createProcess.setName(name);
		createProcess.setTenantId(tenantId);
		createProcess.setType(ParamConstant.VPN);
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
		if (null != syncResource){
			if(!syncResource.getSyncStatus().equalsIgnoreCase(ParamConstant.DOWN_STATUS) && !syncResource.getSyncStatus().equalsIgnoreCase(ParamConstant.ACTIVE_STATUS)){
				throw new ResourceBusinessException(Message.CS_RESOURCE_IS_DOING,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE, locale);
			}
		}
		return;
	}
	
	private void checkRouterResource(String routerId,String subnetId,Locale locale) throws BusinessException{
		VPN vpn  = vpnMapper.selectVPNByRouterId(routerId);
		if(null != vpn)
			throw new ResourceBusinessException(Message.CS_HAVE_RELATED_VPN_RESOURCE_CREATE_WITH_ROUTER,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);	
		
		vpn = vpnMapper.selectVPNBySubetId(subnetId);
		if(null != vpn)
			throw new ResourceBusinessException(Message.CS_HAVE_RELATED_VPN_RESOURCE_CREATE_WITH_SUBNET,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);	

		return;
	}
	
	private void addRelatedRouterInfo(String vpnId,String routerId){
		if(Util.isNullOrEmptyValue(routerId))
			return;
		Router router = routerMapper.selectByPrimaryKey(routerId);
		if(null == router)
			return;
		router.setVpnId(vpnId);
		routerMapper.insertOrUpdate(router);
	}
	
	private void removeRelatedupdateRouterInfo(String vpnId){
		Router router = routerMapper.selectByVPNId(vpnId);
		if(null == router)
			return;
		router.setVpnId(null);
		routerMapper.insertOrUpdate(router);	
	}
	
	private void checkName(String name,TokenOs ostoken)  throws BusinessException{
		if(Util.isNullOrEmptyValue(name))
			return;
		List<VPN> vpns = vpnMapper.selectAllByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(vpns))
			return;
		for(VPN vpn : vpns){
			if(name.equals(vpn.getName()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_NAME_IS_SAME,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		return;
	}
}
