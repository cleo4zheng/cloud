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
import com.cloud.cloudapi.dao.common.NetworkMapper;
import com.cloud.cloudapi.dao.common.PortMapper;
import com.cloud.cloudapi.dao.common.ResourceCreateProcessMapper;
import com.cloud.cloudapi.dao.common.ResourceEventMapper;
import com.cloud.cloudapi.dao.common.RouterMapper;
import com.cloud.cloudapi.dao.common.SyncResourceMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.FloatingIPJSON;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FloatingIP;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FloatingIPConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Loadbalancer;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Network;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Range;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceCreateProcess;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceEvent;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Router;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SyncResource;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.ConfigService;
import com.cloud.cloudapi.service.openstackapi.FloatingIPService;
import com.cloud.cloudapi.service.openstackapi.InstanceService;
import com.cloud.cloudapi.service.openstackapi.LoadbalancerService;
import com.cloud.cloudapi.service.openstackapi.NetworkService;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.service.openstackapi.ResourceSpecService;
import com.cloud.cloudapi.service.openstackapi.RouterService;
import com.cloud.cloudapi.service.pool.PoolResource;
import com.cloud.cloudapi.service.rating.RatingTemplateService;
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

@Service("floatingIPService")
public class FloatingIPServiceImpl implements FloatingIPService{
	
	@Autowired
	private FloatingIPMapper floatingIPMapper;
	
	@Autowired
	private RouterMapper routerMapper;
	
	@Autowired
	private InstanceMapper instanceMapper;
	
	@Autowired
	private NetworkMapper networkMapper;
	
	@Autowired
	private PortMapper portMapper;
	
	@Autowired
	private LoadbalancerMapper loadbalancerMapper;
	
	@Autowired
	private ResourceEventMapper resourceEventMapper;

	@Autowired
	private SyncResourceMapper syncResourceMapper;

	@Autowired
	private ResourceCreateProcessMapper resourceCreateProcessMapper;
	
	@Autowired
	private CloudConfig cloudconfig;

	@Resource
	private QuotaService quotaService;
	
	@Resource
	private RouterService routerService;

	@Resource
	private NetworkService networkService;

	@Resource
	private InstanceService instanceService;

	@Resource
	private LoadbalancerService lbService;
	
	@Resource
	private ResourceSpecService resourceSpecService;
		
	@Resource
	private RatingTemplateService ratingTemplateService;
	
    @Resource
    private PoolResource poolService;
    
 	@Resource
 	private AuthService authService;
	
	@Resource
	private ConfigService configService;
	
//	private static final String TYPE_OF_INSTANCE = "instance";

	@Resource
	private OSHttpClientUtil client;
	
 //   private int ERROR_HTTP_CODE = 400;

    private Logger log = LogManager.getLogger(FloatingIPServiceImpl.class);
    
	@Override
	public List<FloatingIP> getFloatingIPList(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException{
		
		int limitItems = Util.getLimit(paramMap);
		Locale locale = new Locale(ostoken.getLocale());
		List<FloatingIP> floatingipsFromDB = getFloatingIPsFromDB(ostoken,limitItems);
		if(!Util.isNullOrEmptyList(floatingipsFromDB)){
			return floatingipsFromDB;
		}
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/floatingips", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
	
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs, locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		List<FloatingIP> floatingIps = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				floatingIps = getFloatingIPs(rs,ostoken);
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
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				floatingIps = getFloatingIPs(rs,ostoken);
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
			throw new ResourceBusinessException(Message.CS_NETWORK_FLOATINGIP_GET_FAILED,httpCode,locale);
		}

		floatingIps =  storeFloatingIPs2DB(floatingIps,ostoken);
		return  getLimitItems(floatingIps,limitItems);
	}
	
	@Override
	public FloatingIP refreshFloatingIP(String floatingIpId, TokenOs ostoken) throws BusinessException{

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/floatingips/");
		sb.append(floatingIpId);
	
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		FloatingIP floatingIp = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				floatingIp = getFloatingIP(rs,ostoken);
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
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				floatingIp = getFloatingIP(rs,ostoken);
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
			throw new ResourceBusinessException(Message.CS_NETWORK_FLOATINGIP_DETAIL_GET_FAILED,httpCode,locale);
		}
		return floatingIp;
	}
	
	@Override
	public FloatingIP getFloatingIP(String floatingIpId, TokenOs ostoken) throws BusinessException{
		FloatingIP floatingipFromDB = getFloatingIPFromDB(floatingIpId,ostoken);
		if(null != floatingipFromDB ){
			return floatingipFromDB;
		}
		floatingipFromDB = refreshFloatingIP(floatingIpId,ostoken);
		storeFloatingIP2DB(floatingipFromDB,ostoken);
		return floatingipFromDB;
	}

	@Override
	public FloatingIP createFloatingIp(String createBody, TokenOs ostoken) throws BusinessException{

		FloatingIP floatingIPCreateInfo = new FloatingIP();
		makeFloatingIPCreateInfo(createBody,floatingIPCreateInfo,ostoken);
		String floatingIPName = floatingIPCreateInfo.getName();
		checkName(floatingIPName,ostoken);
		String type = floatingIPCreateInfo.getType();
		Integer bandwith = floatingIPCreateInfo.getBandwith();
		floatingIPCreateInfo.setName(null);
		floatingIPCreateInfo.setType(null);
		floatingIPCreateInfo.setBandwith(null);
		
		String floatingIPCreateBody = getFloatingIPCreatedBody(floatingIPCreateInfo);
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/floatingips", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoPost(url, ostoken.getTokenid(),floatingIPCreateBody);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		FloatingIP floatingIp = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				floatingIp = getFloatingIP(rs,ostoken);
			}  catch (Exception e) {
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
			rs = client.httpDoPost(url, tokenid,floatingIPCreateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				floatingIp = getFloatingIP(rs,ostoken);
			}  catch (Exception e) {
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
			throw new ResourceBusinessException(Message.CS_NETWORK_FLOATINGIP_CREATE_FAILED,httpCode,locale);
		}
		
		floatingIp.setName(floatingIPName);
		floatingIp.setType(type);
		floatingIp.setBandwith(bandwith);
		floatingIp.setMillionSeconds(Util.getCurrentMillionsecond());
		floatingIp.setAssigned(false);
		storeAndUpdateNetworkInfo(floatingIp);
		updateFloatingIPQuota(ostoken,type,true);
		storeResourceEventInfo(ostoken.getTenantid(),floatingIp.getId(),ParamConstant.FLOATINGIP,null,ParamConstant.ACTIVE_STATUS,floatingIp.getMillionSeconds());
		
		StringBuilder relatedResource = new StringBuilder();
		relatedResource.append(ParamConstant.NAME);
		relatedResource.append(":");
		relatedResource.append(StringHelper.string2Ncr(floatingIPName));

		updateSyncResourceInfo(ostoken.getTenantid(),floatingIp.getId(),ParamConstant.ACTIVE_STATUS,ParamConstant.FLOATINGIP,ostoken.getCurrentRegion(),null,StringHelper.string2Ncr(floatingIPName));
		return floatingIp;
	}
	
	private FloatingIP updateFloatingIPInfo(String floatingIpId, String updateBody,TokenOs ostoken) throws BusinessException{
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/floatingips/");
		sb.append(floatingIpId);
	
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPut(sb.toString(), headers,updateBody);
		Util.checkResponseBody(rs, locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		FloatingIP floatingIp = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				floatingIp = getFloatingIP(rs,ostoken);
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
			rs = client.httpDoPut(sb.toString(), headers,updateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				floatingIp = getFloatingIP(rs,ostoken);
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
			throw new ResourceBusinessException(Message.CS_NETWORK_FLOATINGIP_UPDATE_FAILED,httpCode,locale);
		}
		return floatingIp;
	}
	
	@Override
	public FloatingIP disassociateFloatingIp(String floatingIPId, String lbId,TokenOs ostoken) throws BusinessException{
		
		checkResource(floatingIPId,new Locale(ostoken.getLocale()),false);

		StringBuilder sb = new StringBuilder();
		sb.append("{\"");
		sb.append(ParamConstant.FLOATINGIP);
		sb.append("\":{\"");
		sb.append(ParamConstant.PORT_ID);
		sb.append("\":null}}");
		FloatingIP floatingIp = updateFloatingIPInfo(floatingIPId,sb.toString(),ostoken);
		FloatingIP floatingIPFromDB = floatingIPMapper.selectByPrimaryKey(floatingIp.getId());
		if(null == floatingIPFromDB){
			floatingIPFromDB = floatingIp;
		}
		floatingIPFromDB.setAssigned(false);
		floatingIPFromDB.setPort_id(null);
		if(!Util.isNullOrEmptyValue(lbId))
			floatingIPFromDB.setLoadbalancerId(null);
		floatingIPMapper.insertOrUpdate(floatingIPFromDB);
		updateSyncResourceInfo(ostoken.getTenantid(),floatingIPId,ParamConstant.DOWN_STATUS,ParamConstant.FLOATINGIP,ostoken.getCurrentRegion(),null,floatingIPFromDB.getName());

		return floatingIPFromDB;
	}
	
	@Override
	public FloatingIP associateFloatingIp(String floatingIPId, String lbId,String portId,TokenOs ostoken) throws BusinessException{
		
		checkResource(floatingIPId,new Locale(ostoken.getLocale()),false);

		StringBuilder sb = new StringBuilder();
		sb.append("{\"");
		sb.append(ParamConstant.FLOATINGIP);
		sb.append("\":{\"");
		sb.append(ParamConstant.PORT_ID);
		sb.append("\":\"");
		sb.append(portId);
		sb.append("\"}}");
		
		FloatingIP floatingIp = updateFloatingIPInfo(floatingIPId,sb.toString(),ostoken);
		FloatingIP floatingIPFromDB = floatingIPMapper.selectByPrimaryKey(floatingIp.getId());
		if(null == floatingIPFromDB){
			floatingIPFromDB = floatingIp;
		}
		floatingIPFromDB.setAssigned(true);
		floatingIPFromDB.setPort_id(portId);
		if(!Util.isNullOrEmptyValue(lbId))
			floatingIPFromDB.setLoadbalancerId(lbId);
		floatingIPMapper.insertOrUpdate(floatingIPFromDB);
		updateSyncResourceInfo(ostoken.getTenantid(),floatingIPId,ParamConstant.ACTIVE_STATUS,ParamConstant.FLOATINGIP,ostoken.getCurrentRegion(),null,floatingIPFromDB.getName());

		return floatingIPFromDB;
	}
	
	@Override
	public FloatingIP updateFloatingIP(String floatingIPId, String updateBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException{
		
		checkResource(floatingIPId,new Locale(ostoken.getLocale()),false);
		
		//	String floatingUpdateBody = getUpdateBody(updateBody);
		JsonNode floatingipNode = getUpdateFloatingIPNode(updateBody);
		
		String updatedName = floatingipNode.path(ResponseConstant.NAME).textValue();
		checkName(updatedName,ostoken);
		String floatingUpdateBody = getUpdateBody(floatingipNode);
		
		FloatingIP floatingIp = updateFloatingIPInfo(floatingIPId,floatingUpdateBody,ostoken);
		FloatingIP floatingIPFromDB = floatingIPMapper.selectByPrimaryKey(floatingIp.getId());
		if(null != floatingIPFromDB){
			if(!Util.isNullOrEmptyValue(updatedName))
				floatingIp.setName(updatedName);
			else
				floatingIp.setName(floatingIPFromDB.getName());
			floatingIp.setMillionSeconds(floatingIPFromDB.getMillionSeconds());
			floatingIp.setType(floatingIPFromDB.getType());
			floatingIp.setAssigned(floatingIPFromDB.getAssigned());
			floatingIp.setBandwith(floatingIPFromDB.getBandwith());
		}
		
		storeFloatingIP2DB(floatingIp,ostoken);
		return floatingIp;
	}
	
	@Override
	public void deleteFloatingIP(String floatingIpId, TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(floatingIpId,locale,true);
		
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/floatingips/");
		sb.append(floatingIpId);
	
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE:{
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
			throw new ResourceBusinessException(Message.CS_NETWORK_FLOATINGIP_DELETE_FAILED,httpCode,locale);
		}

		updateFloatingIPDBInfo(ostoken,floatingIpId);

	}
	
	private void updateFloatingIPDBInfo(TokenOs ostoken,String floatingIpId){
		
		FloatingIP floatingIP = floatingIPMapper.selectByPrimaryKey(floatingIpId);
		if(null == floatingIP)
			return;
		
		String portId = floatingIP.getPort_id();
		if(!Util.isNullOrEmptyValue(portId))
			portMapper.deleteByPrimaryKey(portId);
		floatingIPMapper.deleteByPrimaryKey(floatingIpId);
		updateSyncResourceInfo(ostoken.getTenantid(),floatingIpId,ParamConstant.DELETED_STATUS,ParamConstant.FLOATINGIP,ostoken.getCurrentRegion(),null,floatingIP.getName());
		storeResourceEventInfo(ostoken.getTenantid(),floatingIP.getId(),ParamConstant.FLOATINGIP,floatingIP.getStatus(),ParamConstant.DELETED_STATUS,Util.getCurrentMillionsecond());

//		Instance instance = instanceMapper.selectInstanceByFloatingIp(floatingIP.getFixedIpAddress());
//		if(null != instance){
//			List<String> floatingIPAddresses = Util.getCorrectedIdInfo(instance.getFloatingips(), floatingIP.getFixedIpAddress());
//			instance.setFloatingips(Util.listToString(floatingIPAddresses, ','));
//			try{
//				instanceMapper.updateByPrimaryKeySelective(instance);
//			}catch(Exception e){
//				//TODO
//			}
//		}
		
		
		//floatingIPMapper.deleteByPrimaryKey(floatingIpId);
	
		
		updateFloatingIPQuota(ostoken,floatingIP.getType(),false);
	}
	
	private JsonNode getUpdateFloatingIPNode(String updateBody) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(updateBody);
		return rootNode;
	}
	
	private String getUpdateBody(JsonNode rootNode) {
		FloatingIP floatingIP = new FloatingIP();
		floatingIP.setPort_id(rootNode.path(ResponseConstant.PORT_ID).textValue());
		
		FloatingIPJSON floatingIPJSON = new FloatingIPJSON(floatingIP);
		JsonHelper<FloatingIPJSON, String> jsonHelp = new JsonHelper<FloatingIPJSON, String>();
		return jsonHelp.generateJsonBodySimple(floatingIPJSON);
	}
	
	private void updateFloatingIPQuota(TokenOs ostoken,String type,boolean bAdd){
		quotaService.updateQuota(type,ostoken,bAdd,1);
		resourceSpecService.updateResourceSpecQuota(type,ParamConstant.FLOATINGIP,1,bAdd);
		
		Map<String,Integer> resourceQuota = new HashMap<String,Integer>();
		resourceQuota.put(type, 1);
		poolService.updatePoolQuota(ostoken.getTenantid(), resourceQuota, bAdd);
	}
	
	private void storeAndUpdateNetworkInfo(FloatingIP floatingIp){
		floatingIPMapper.insertSelective(floatingIp);
		//update network's portid info
		Network network = networkMapper.selectByPrimaryKey(floatingIp.getNetworkId());
		if(null == network){
			//TODO
		}else{
			String floatingIpId = Util.getIdWithAppendId(floatingIp.getId(),network.getFloatingipId());
			network.setFloatingipId(floatingIpId);;
			networkMapper.updateByPrimaryKeySelective(network);	
		}
	}
	
	private void addAttachedResourceInfo(FloatingIP floatingip,TokenOs ostoken) throws BusinessException{
		if(null == floatingip)
			return;
		String attchedRouterId = floatingip.getRouterId();
		if(null != attchedRouterId && !attchedRouterId.isEmpty()){
			Router router = routerMapper.selectByPrimaryKey(attchedRouterId);
			if(null != router){
				floatingip.addResource(attchedRouterId, router.getName(), ParamConstant.ROUTER);
			}else{
//				RouterServiceImpl routerService = new RouterServiceImpl(); 
//				routerService.setCloudconfig(cloudconfig);
//				routerService.setRouterMapper(routerMapper);
				router = routerService.getRouter(attchedRouterId,ostoken);
				if(null != router){
					//routerMapper.insertSelective(router);
					floatingip.addResource(attchedRouterId, router.getName(), ParamConstant.ROUTER);
				}
			}
		}
		String attachedInstanceId = floatingip.getInstanceId();
		if(null != attachedInstanceId){
			Instance instance = instanceMapper.selectByPrimaryKey(attachedInstanceId);
			if(null != instance){
				floatingip.addResource(attachedInstanceId, instance.getName(), ParamConstant.INSTANCE);
			}else{
				Map<String, String> paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.ID, attachedInstanceId);
				instance = instanceService.getInstance(attachedInstanceId,ParamConstant.INSTANCE_TYPE,ostoken, false);
				//showInstance(attachedInstanceId,ostoken, false, null, response);
				if(null != instance){
					floatingip.addResource(attachedInstanceId, instance.getName(), ParamConstant.INSTANCE);
				}
			}
		}
		
		String attachedLbId = floatingip.getLoadbalancerId();
		if(null != attachedLbId){
			Loadbalancer lb = loadbalancerMapper.selectByPrimaryKey(attachedLbId);
			if(null != lb){
				floatingip.addResource(attachedLbId, lb.getName(), ParamConstant.LOADBALANCER);
			}else{
				Map<String, String> paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.ID, attachedLbId);
				lb = lbService.getLoadbalancer(attachedLbId, ostoken);
				//showInstance(attachedInstanceId,ostoken, false, null, response);
				if(null != lb){
					floatingip.addResource(attachedLbId, lb.getName(), ParamConstant.LOADBALANCER);
				}
			}
		}
	}
	
	private List<FloatingIP> getFloatingIPsFromDB(TokenOs ostoken,int limitItems){
		List<FloatingIP> floatingipsFromDB = null;
		if(-1 == limitItems){
			floatingipsFromDB = floatingIPMapper.selectListByTenantId(ostoken.getTenantid());
		}else{
			floatingipsFromDB = floatingIPMapper.selectListByTenantIdWithLimit(ostoken.getTenantid(),limitItems);
		}
		
		if(!Util.isNullOrEmptyList(floatingipsFromDB)){
			List<FloatingIP> floatingipsWithResource = new ArrayList<FloatingIP>();
			for(FloatingIP floatingip : floatingipsFromDB){
				try {
					addAttachedResourceInfo(floatingip,ostoken);
				} catch (BusinessException e) {
					// TODO Auto-generated catch block
					log.error(e);
				}
				floatingipsWithResource.add(floatingip);
			}
			return floatingipsWithResource;
		}
		return null;
	}
	
	private FloatingIP getFloatingIPFromDB(String id,TokenOs ostoken) throws ResourceBusinessException{
		if(null == floatingIPMapper)
			return null;
		FloatingIP floatingipFromDB = floatingIPMapper.selectByPrimaryKey(id);
		if(null != floatingipFromDB ){
			try {
				addAttachedResourceInfo(floatingipFromDB,ostoken);
			} catch (BusinessException e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw  new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(ostoken.getLocale()));
			}
		}
		return floatingipFromDB;
	}
	
	private List<FloatingIP> storeFloatingIPs2DB(List<FloatingIP> floatingIPS,TokenOs ostoken){
		if(Util.isNullOrEmptyList(floatingIPS))
			return null;
		List<FloatingIP> floatingipsWithResource = new ArrayList<FloatingIP>();
		for(FloatingIP floating : floatingIPS){
			storeFloatingIP2DB(floating,ostoken);
			floatingipsWithResource.add(floating);
		}
		return floatingipsWithResource;
	}
	
	private void storeFloatingIP2DB(FloatingIP floating,TokenOs ostoken){
		try {
			addAttachedResourceInfo(floating,ostoken);
			floatingIPMapper.insertOrUpdate(floating);
		} catch (BusinessException e) {
			// TODO Auto-generated catch block
			log.error(e);
		//	ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_FLOATINGIP_DETAIL_GET_FAILED);
		//	return exception.getResponseMessage(); //TODO
		}
	}
	
	private List<FloatingIP> getFloatingIPs(Map<String, String> rs,TokenOs ostoken) throws JsonProcessingException, IOException, BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode floatingipsNode = rootNode.path(ResponseConstant.FLOATINGIPS);
		int floatingsCount = floatingipsNode.size();
		if (0 == floatingsCount)
			return null;
		
		List<FloatingIP> floatingIPs = new ArrayList<FloatingIP>();
		for (int index = 0; index < floatingsCount; ++index) {
			FloatingIP floatingIP = getFloatingIPInfo(floatingipsNode.get(index),ostoken);
			floatingIPs.add(floatingIP);
		}
		
		return floatingIPs;
	}
	
	private FloatingIP getFloatingIP(Map<String, String> rs,TokenOs ostoken) throws JsonProcessingException, IOException, BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode floatingipNode = rootNode.path(ResponseConstant.FLOATINGIP);
		return getFloatingIPInfo(floatingipNode,ostoken);
	}
	
	private FloatingIP getFloatingIPInfo(JsonNode floatingIPNode,TokenOs ostoken) throws BusinessException{
		if(null == floatingIPNode)
			return null;
		FloatingIP floatingIPInfo = new FloatingIP();

		floatingIPInfo.setNetworkId(floatingIPNode.path(ResponseConstant.FLOATING_NETWORK_ID).textValue());
		floatingIPInfo.setRouterId(floatingIPNode.path(ResponseConstant.ROUTER_ID).textValue());
		if(Util.isNullOrEmptyValue(floatingIPInfo.getRouterId()))
			floatingIPInfo.setAssigned(false);
		else
			floatingIPInfo.setAssigned(true);
		floatingIPInfo.setFixedIpAddress(floatingIPNode.path(ResponseConstant.FIXED_IP_ADDRESS).textValue());
		floatingIPInfo.setFloatingIpAddress(floatingIPNode.path(ResponseConstant.FLOATING_IP_ADDRESS).textValue());
		floatingIPInfo.setTenantId(floatingIPNode.path(ResponseConstant.TENANT_ID).textValue());
		floatingIPInfo.setStatus(floatingIPNode.path(ResponseConstant.STATUS).textValue());
		floatingIPInfo.setId(floatingIPNode.path(ResponseConstant.ID).textValue());
		floatingIPInfo.setPort_id(floatingIPNode.path(ResponseConstant.PORT_ID).textValue());
		
		List<Network> externalNets = networkService.getExternalNetworks(ostoken);
		if(!Util.isNullOrEmptyList(externalNets)){
			for(Network network : externalNets){
				if(network.getId().equals(floatingIPInfo.getNetworkId())){
					floatingIPInfo.setType(network.getName());
					floatingIPInfo.setName(floatingIPInfo.getId()+"_"+network.getName());
					break;
				}
			}
		}
//		Network network = networkMapper.selectByPrimaryKey(floatingIPInfo.getNetworkId());
//		if(null != network)
//			floatingIPInfo.setType(network.getName());
		
		return floatingIPInfo;
	}
	
	
	/**
	 * for api   /floating-ips
	 */
//	public List<FloatingIP> getFloatingIPExtList(Map<String, String> paramMap, TokenOs ostoken) throws BusinessException{
//		
//		//获取所有的instance,组装�?一个hashmap
//		//key=ip, value=resource(HashMap)
//		InstanceService inService= OsApiServiceFactory.getInstanceService();
//		List<Instance> instanceList=inService.getInstanceList(null,ParamConstant.INSTANCE_TYPE,ostoken);
//		HashMap<String,HashMap> instanceInfo =  new HashMap<String, HashMap>();
//		HashMap<String, String> resource = null;
//		List<String> fpList = new ArrayList<String>();
//		for(Instance instance:instanceList){
//			fpList = instance.getFloatingIps();
//		    if(fpList != null && !fpList.isEmpty()){
//		    	for(String fp: fpList){
//		    		resource = new HashMap<String, String>();
//		    		resource.put(ResponseConstant.NAME, instance.getName());
//		    		resource.put(ResponseConstant.ID, instance.getId());
//		    		resource.put(ResponseConstant.RESOURCE_TYPE, TYPE_OF_INSTANCE);
//		    		instanceInfo.put(fp, resource);
//		    	}
//		    }
//		    
//		}
//		
//		//获取所有网络的信息列表组成一个HashMap
//		//key=net_id, value=网络信息
//	   /* NetworkService netService = OsApiServiceFactory.getNetworkService();
//	    List<Network> netList = netService.getNetworkList(paramMap, ostoken);
//	    for(Network network:netList){
//	    	
//	    }*/
//		
//	
//		// TODO
//		// 取得bandwith,line等信�?
//		
//		
//		Map<String, String>  rs = getFloatingIPs(paramMap, ostoken);
//		try {
//				ObjectMapper mapper = new ObjectMapper();
//				JsonNode rootNode = mapper.readTree(rs.get("jsonbody"));
//				JsonNode floatingipsNode=rootNode.path(ResponseConstant.FLOATINGIPS);
//				int floatingipsCount =floatingipsNode.size();
//                if(0 == floatingipsCount)
//                	return null;
//                
//                List<FloatingIP> list= new ArrayList<FloatingIP>();	
//				for(JsonNode floatingIPNode:floatingipsNode){
//					HashMap map = new HashMap();
//					FloatingIP floatingIPExt = new FloatingIP();					
//					floatingIPExt.setId(floatingIPNode.path(ResponseConstant.ID).textValue());
////					floatingIPExt.setName("Fake Name");
////					floatingIPExt.setBandwidth("100 Mbps");
////					floatingIPExt.setLine("中国电信");
//					floatingIPExt.setStatus(floatingIPNode.path(ResponseConstant.STATUS).textValue());
//					floatingIPExt.setFloatingIpAddress(floatingIPNode.path(ResponseConstant.FLOATING_IP_ADDRESS).textValue());
////					floatingIPExt.setCreatedAt("2016-01-01");
////					HashMap re = instanceInfo.get(floatingIPNode.path(ResponseConstant.FLOATING_IP_ADDRESS).textValue())==null?
////							(new HashMap()):instanceInfo.get(floatingIPNode.path(ResponseConstant.FLOATING_IP_ADDRESS).textValue());
////					floatingIPExt.setResource(re);
//					/*if(floatingIPNode.path("port_id").textValue()!=null&&!(floatingIPNode.path("port_id").textValue().equals(""))){
//						
//					}*/
//					
//					list.add(floatingIPExt);
//					
//			}
//				
//			return list;
//		}
//		catch(Exception e){
//			// TODO Auto-generated catch block
//			log.error(e);
//		}
//		return null;
//	}
	
	@Override
	public FloatingIPConfig getFloatingIPConfig(TokenOs ostoken) throws BusinessException{
		return this.configService.getFloatingIPConfig(ostoken);
	}
	
	@Override
	public FloatingIPConfig getFloatingIPConfig2(TokenOs ostoken) throws BusinessException{

		List<Network> externalNetworks = networkService.getExternalNetworks(ostoken);
		if(Util.isNullOrEmptyList(externalNetworks))
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale())); 
		FloatingIPConfig floatingIPConfig = new FloatingIPConfig();
		floatingIPConfig.setUnit(ParamConstant.MBPS);
		String[] range = cloudconfig.getFloatingRange().split(",");
		if(2 != range.length)
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale())); 
		floatingIPConfig.setRange(new Range(Integer.parseInt(range[0]),Integer.parseInt(range[1])));
		
		String[] floatingSpecs = cloudconfig.getSystemFloatingSpec().split(",");
		String[] floatingPrices = cloudconfig.getFloatingPrice().split(",");
		if(floatingSpecs.length != floatingPrices.length  || floatingSpecs.length != externalNetworks.size())
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale())); 
		for(int index = 0 ; index < floatingSpecs.length; ++index){
			String floatingipTypeName = floatingSpecs[index].toUpperCase();
			for(Network network : externalNetworks){
				if(network.getName().matches("(?i)"+floatingSpecs[index]+".*")){
					floatingIPConfig.addResource(network.getId(),floatingipTypeName, Double.valueOf(floatingPrices[index]));
                    break; 
				}
			}
		}
		return floatingIPConfig;
	}
	
	/**
	 * 根据条件查询到所有的floating ip对象
	 * 并且对此对象不做任何处理
	 * @param paramMap
	 * @return
	 */
//	private Map<String, String> getFloatingIPs(Map<String, String> paraMap, TokenOs ostoken){
//		HttpClientForOsBase osClient = new HttpClientForOsBase(cloudconfig);
//		TokenOs ot=osClient.getToken();
//		//todo 1: 通过guitokenid 取得实际，用户信�?
//        //AuthService	as = new AuthServiceImpl();	
//        //as.GetTokenOS(guiTokenId);
//		String region ="RegionOne";
//		String url=ot.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();		
//		url=RequestUrlHelper.createFullUrl(url+"/v2.0/floatingips", paraMap);
//		
//		HashMap<String, String> headers = new HashMap<String, String>();
//		headers.put("X-Auth-Token", ot.getTokenid());
//		
//		Map<String, String>  rs = client.httpDoGet(url, headers);
////		Map<String, String>  rs =client.httpDoGet(url, ot.getTokenid());
//		
//		System.out.println("httpcode:"+rs.get("httpcode")); 
//		System.out.println("jsonbody:"+rs.get("jsonbody")); 
//			
//		if(Integer.parseInt(rs.get("httpcode")) > ERROR_HTTP_CODE){
//			//System.out.println("wo cha:request failed");
//			log.error("wo cha:request failed");
//			return null;
//		}
//		return rs;
//		
//	}
	
	private void makeFloatingIPCreateInfo(String createBody,FloatingIP floatingIPCreateInfo,TokenOs ostoken) throws ResourceBusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(createBody);
		} catch (Exception e) {
			log.error("error",e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));

		} 
		String netId = rootNode.path(ResponseConstant.TYPE).textValue();
		Integer bandwidth = rootNode.path(ResponseConstant.BANDWIDTH).intValue(); //do it later 08/15
		
		Network network = networkMapper.selectByPrimaryKey(netId);
		if(null == network)
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		
		floatingIPCreateInfo.setFloating_network_id(netId);
		floatingIPCreateInfo.setName(rootNode.path(ResponseConstant.NAME).textValue());
		
		floatingIPCreateInfo.setType(network.getName());
		floatingIPCreateInfo.setBandwith(bandwidth);
	}
	
	private String getFloatingIPCreatedBody(FloatingIP floatingIP){
		FloatingIPJSON createdFloatingIPJSON = new FloatingIPJSON(floatingIP);
		JsonHelper<FloatingIPJSON, String> jsonHelp = new JsonHelper<FloatingIPJSON, String>();
		return jsonHelp.generateJsonBodySimple(createdFloatingIPJSON);
	}
	
	/**
	 * 根据floatingIP id获取额外的信�? 如从属的网络、绑定的端口信息�?
	 * @param paraMap
	 * @return
	 */
//	private String getFloatingIPExtInfo(String id){
//		
//		
//		return null;
//	}
	
	private List<FloatingIP> getLimitItems(List<FloatingIP> floatingIps,int limit){
		if(null == floatingIps)
			return null;
		if(-1 != limit){
			if(limit <= floatingIps.size())
				return floatingIps.subList(0, limit);
		}
		return floatingIps;
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
	
	private void checkResource(String id, Locale locale,Boolean checkBinding) throws BusinessException {
		SyncResource resource = syncResourceMapper.selectByPrimaryKey(id);
		if(null != resource)
			throw new ResourceBusinessException(Message.CS_RESOURCE_IS_DOING,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);

		if(true == checkBinding){
			FloatingIP floatingIP = floatingIPMapper.selectByPrimaryKey(id);
			if(null == floatingIP)
				return;
			Instance instance = instanceMapper.selectInstanceByFloatingIp(floatingIP.getFloatingIpAddress());
			if(null != instance)
				throw new ResourceBusinessException(Message.CS_HAVE_RELATED_INSTANCE_RESOURCE,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
			if(null != loadbalancerMapper.selectByFloatingIP(floatingIP.getFloatingIpAddress()))
				throw new ResourceBusinessException(Message.CS_HAVE_RELATED_LB_RESOURCE_WITH_FLOATINGIP,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
		}	
		return;
	}
	
	private void checkName(String name,TokenOs ostoken)  throws BusinessException{
		if(Util.isNullOrEmptyValue(name))
			return;
		List<FloatingIP> floatingIPs = floatingIPMapper.selectListByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(floatingIPs))
			return;
		for(FloatingIP floatingIP : floatingIPs){
			if(name.equals(floatingIP.getName()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_NAME_IS_SAME,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		return;
	}
	

}
