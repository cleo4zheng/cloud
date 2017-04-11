package com.cloud.cloudapi.service.openstackapi.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.cloud.cloudapi.dao.common.InstanceMapper;
import com.cloud.cloudapi.dao.common.PhysNodeMapper;
import com.cloud.cloudapi.dao.common.PhysPortMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.InstanceJSON;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.DriverInfo;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Flavor;
import com.cloud.cloudapi.pojo.openstackapi.forgui.HardWare;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Image;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.IronicNode;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Keypair;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Network;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PhysNode;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PhysPort;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Port;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SecurityGroup;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.FlavorService;
import com.cloud.cloudapi.service.openstackapi.ImageService;
import com.cloud.cloudapi.service.openstackapi.KeypairService;
import com.cloud.cloudapi.service.openstackapi.NetworkService;
import com.cloud.cloudapi.service.openstackapi.PhysNodeService;
import com.cloud.cloudapi.service.openstackapi.SecurityGroupService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("physNodeService")
public class PhysNodeServiceImpl  implements PhysNodeService {
    
	@Autowired
	private CloudConfig cloudconfig;
	
	@Resource
	private OSHttpClientUtil client;
	
	@Resource
	private FlavorService flavorService;
	
	@Autowired
	private InstanceMapper instanceMapper;
	
	@Autowired
	private PhysNodeMapper physNodeMapper;
	
	@Autowired
	private PhysPortMapper physPortMapper;
	
	@Resource
	private ImageService imageService;
	
	@Resource
	private KeypairService keypairService;
	
	@Resource
	private NetworkService networkService;
	
	@Resource
	private SecurityGroupService securityGroupService;
	
	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(PhysNodeServiceImpl.class);
	
	@Value("#{ propertyConfigurer['ironic.zone'] }")
	private String zone;
	
	@Override
	public List<PhysNode> getPhysNodes(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException{
		int limitItems = Util.getLimit(paramMap);
		List<PhysNode> physNodes = null;
		if(-1 == limitItems){
			physNodes = physNodeMapper.selectList();
		}else{
			physNodes = physNodeMapper.selectListWithLimit(limitItems);
		}
		if(null == physNodes){
			getPhysNodesFromAPI(ostoken);
			if(-1 == limitItems){
				physNodes = physNodeMapper.selectList();
			}else{
				physNodes = physNodeMapper.selectListWithLimit(limitItems);
			}
		}	
		return physNodes;
	}
	
	private List<PhysNode> getPhysNodesFromAPI(TokenOs ostoken) throws BusinessException{
		String region = ostoken.getCurrentRegion();
		Locale locale = new Locale(ostoken.getLocale());
		TokenOs adminToken = null;
		String url = null;
		try {
			adminToken = authService.createDefaultAdminOsToken();
			url = adminToken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error("error",e);
			return null;
		}
		
//		String url = guiToken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		
		url = RequestUrlHelper.createFullUrl(url + "/v1/nodes/detail", null);

		HashMap<String, String> headers = new HashMap<String, String>();
	//	headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, guiToken.getTokenid());
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
		headers.put(ParamConstant.IRONIC_API_VERSION_STRING, ParamConstant.IRONIC_API_VERSION_VALUE);
		
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		List<PhysNode> physNodes = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				physNodes = getPhysNodes(rs);
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
		    	throw new ResourceBusinessException(Message.CS_NODE_GET_FAILED,httpCode,locale);
			try {
				physNodes = getPhysNodes(rs);
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
			throw new ResourceBusinessException(Message.CS_NODE_GET_FAILED,httpCode,locale);
		}
		//save to DB
		for(PhysNode phynode:physNodes){
			physNodeMapper.insertOrUpdate(phynode);
		}
		return physNodes;
	}
	
	@Override
	public PhysNode getPhysNode(String nodeId,TokenOs ostoken) throws BusinessException{
		PhysNode physNode = physNodeMapper.selectByPrimaryKey(nodeId);
		if(null == physNode){
			physNode = getPhysNodeFromAPI(nodeId, ostoken);
		}
		return physNode;
	}
	
	private PhysNode getPhysNodeFromAPI(String nodeId,TokenOs ostoken) throws BusinessException{
		
		Locale locale = new Locale(ostoken.getLocale());
		TokenOs adminToken = null;
		try {
			adminToken = authService.createDefaultAdminOsToken();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,locale);
		}
		
		String region = ostoken.getCurrentRegion();
	//	String url = guiToken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v1/nodes/");
		sb.append(nodeId);
	
		HashMap<String, String> headers = new HashMap<String, String>();
	//	headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, guiToken.getTokenid());
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
		headers.put(ParamConstant.IRONIC_API_VERSION_STRING, ParamConstant.IRONIC_API_VERSION_VALUE);
		
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		PhysNode physNode = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				physNode = getPhysNode(rs);
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
				throw new ResourceBusinessException(Message.CS_NODE_DETAIL_GET_FAILED,httpCode,locale);
			try {
				physNode = getPhysNode(rs);
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
			throw new ResourceBusinessException(Message.CS_NODE_DETAIL_GET_FAILED,httpCode,locale);
		}
		//save to DB
		physNodeMapper.insertOrUpdate(physNode);
		return physNode;
	}
	
	
	@Override
	public PhysNode createPhysNode(String createBody,TokenOs ostoken) throws BusinessException{
		
		Locale locale = new Locale(ostoken.getLocale());
		TokenOs adminToken = null;
		try {
			adminToken = authService.createDefaultAdminOsToken();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,locale);
		}
		
		String region = ostoken.getCurrentRegion();
	//	String url = guiToken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
	//	headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, guiToken.getTokenid());
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
		headers.put(ParamConstant.IRONIC_API_VERSION_STRING, ParamConstant.IRONIC_API_VERSION_VALUE);
		
		ObjectMapper mapper = new ObjectMapper();
		IronicNode ironicNode = null;
		try {
			ironicNode = mapper.readValue(createBody, IronicNode.class);
		} catch (JsonParseException e1) {
			// TODO Auto-generated catch block
			log.error(e1);
			return null;
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			log.error(e1);
			return null;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			log.error(e1);
			return null;
		}
		ironicNode.getProperties().setCapabilities(ParamConstant.BOOT_LOCAL);
		ironicNode.getProperties().setCpu_arch(ParamConstant.CPU_ARCH_64);
		ironicNode.setDriver(ParamConstant.PXE_IPMITOOL_DRIVER);
		ironicNode.getDriver_info().setDeploy_kernel(cloudconfig.getDepolyKernel());
		ironicNode.getDriver_info().setDeploy_ramdisk(cloudconfig.getDepolyRamdisk());
		ironicNode.getDriver_info().setIpmi_terminal_port(ParamConstant.IPMI_TERMINAL_PORT);
		String address = ironicNode.getExtra().getAddress();
		ironicNode.setExtra(null);
		String jsStr = "" ;
		try {
			jsStr = mapper.writeValueAsString(ironicNode);
		} catch (JsonProcessingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Map<String, String> rs = client.httpDoPost(url+"/v1/nodes", headers,jsStr);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		PhysNode physNode = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				physNode = getPhysNode(rs);
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
			rs =  client.httpDoPost(url+"/v1/nodes", headers,jsStr);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				physNode = getPhysNode(rs);
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
			throw new ResourceBusinessException(Message.CS_NODE_CREATE_FAILED,httpCode,locale);
		}
		String createPort = "{\"node_uuid\":\""+physNode.getUuid()+"\",\"address\":\""+
				address+"\"}";
		createPhysPort(createPort, ostoken);
		
		//add flavor infomation to cloudapi db and openstack
		flavorService.getFlavor(ostoken, ironicNode.getProperties().getCpus(), ironicNode.getProperties().getMemory_mb(), 
				ironicNode.getProperties().getLocal_gb(), ironicNode.getProperties().getCpu_arch(), ParamConstant.BAREMETAL_TYPE);
		
		//TODO record baremetal resource in db
		physNodeMapper.insertSelective(physNode);
		return physNode;
	}
	
	@Override
	public PhysNode updatePhysNode(String nodeId,String updateBody,TokenOs ostoken) throws BusinessException{
		TokenOs adminToken = null;
		Locale locale = new Locale(ostoken.getLocale());
		try {
			adminToken = authService.createDefaultAdminOsToken();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,locale);
		}
		
		String region = ostoken.getCurrentRegion();
	//	String url = guiToken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v1/nodes/");
		sb.append(nodeId);
		HashMap<String, String> headers = new HashMap<String, String>();
	//	headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, guiToken.getTokenid());
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
		headers.put(ParamConstant.IRONIC_API_VERSION_STRING, ParamConstant.IRONIC_API_VERSION_VALUE);
		
		Map<String, String> rs = client.httpDoPatch(sb.toString(), headers,updateBody);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		PhysNode physNode = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				physNode = getPhysNode(rs);
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
			rs =  client.httpDoPatch(sb.toString(), headers,updateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				physNode = getPhysNode(rs);
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
			throw new ResourceBusinessException(Message.CS_NODE_UPDATE_FAILED,httpCode,locale);
		}
		//update database
		physNodeMapper.insertOrUpdate(physNode);
		return physNode;
	}
	
	@Override
	public void changePhysNodePowerStates(String nodeId,String updateBody,TokenOs ostoken) throws BusinessException{
		TokenOs adminToken = null;
		Locale locale = new Locale(ostoken.getLocale());
		try {
			adminToken = authService.createDefaultAdminOsToken();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,locale);
		}
		
		String region = ostoken.getCurrentRegion();
	//	String url = guiToken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v1/nodes/");
		sb.append(nodeId);
		sb.append("/states/power");
		HashMap<String, String> headers = new HashMap<String, String>();
	//	headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, guiToken.getTokenid());
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
		headers.put(ParamConstant.IRONIC_API_VERSION_STRING, ParamConstant.IRONIC_API_VERSION_VALUE);
		
		Map<String, String> rs = client.httpDoPut(sb.toString(), headers,updateBody);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		//PhysNode physNode = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE:{
			break;
		}
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				getPhysNode(rs);
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
			rs = client.httpDoPut(sb.toString(), headers,updateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				getPhysNode(rs);
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
			throw new ResourceBusinessException(Message.CS_NODE_UPDATE_POWER_STATES_FAILED,httpCode,locale);
		}
		//update database
		PhysNode physNode = getPhysNodeFromAPI(nodeId, adminToken);
		physNodeMapper.insertOrUpdate(physNode);
		//return physNode;
	}
	
	@Override
	public String deletePhysNodes(String deleteBody, TokenOs authToken) throws ResourceBusinessException, JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(deleteBody);
		JsonNode idsNode = rootNode.path(ResponseConstant.IDS);
		int idsCount = idsNode.size();
		boolean deletedFailed = false;
		List<String> deletedNodeId = new ArrayList<String>();
		for(int index = 0; index < idsCount; ++index){
			String nodeId = idsNode.get(index).textValue();
			try{
				deletePhysNode(nodeId, authToken);
				deletedNodeId.add(nodeId);
			}catch (Exception e){
				deletedFailed = true;
			}
			//delete success,delete from db
			if(!deletedFailed){
				physNodeMapper.deleteByPrimaryKey(nodeId);
			}
		}
		
		if(true == deletedFailed){
			throw new ResourceBusinessException(Message.CS_NODE_DELETE_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
		}
		return Util.listToString(deletedNodeId, ',');
		
	}

	@Override
	public void deletePhysNode(String nodeId, TokenOs ostoken) throws BusinessException {
		TokenOs adminToken = null;
		Locale locale = new Locale(ostoken.getLocale());
		try {
			adminToken = authService.createDefaultAdminOsToken();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,locale);
		}
		// token should have Regioninfo
		String region = ostoken.getCurrentRegion();
		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v1/nodes/");
		sb.append(nodeId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
		
		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
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
			rs = client.httpDoDelete(sb.toString(), headers);
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			if (httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
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
			throw new ResourceBusinessException(Message.CS_NODE_DELETE_FAILED,httpCode,locale);
		}
	}
	
	private PhysPort createPhysPort(String createBody,TokenOs ostoken) throws BusinessException{
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPost(url+"/v1/ports", headers,createBody);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		PhysPort physPort = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				physPort = getPhysPort(rs);
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
			rs =  client.httpDoPost(url+"/v1/chassis", headers,createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
            if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
            	throw new ResourceBusinessException(Message.CS_PORT_CREATE_FAILED,httpCode,locale);
			try {
				physPort = getPhysPort(rs);
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
			throw new ResourceBusinessException(Message.CS_PORT_CREATE_FAILED,httpCode,locale);
		}
		//save to db
		physPortMapper.insertSelective(physPort);
		return physPort;
	}
	
	private List<PhysNode> getPhysNodes(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode physJsonNodes = rootNode.path(ResponseConstant.NODES);
		int physNodesCount = physJsonNodes.size();
		if (0 == physNodesCount)
			return null; 	
		
		List<PhysNode> physNodes = new ArrayList<PhysNode>();
		for (int index = 0; index < physNodesCount; ++index) {
			PhysNode physNode = getPhysNodeInfo(physJsonNodes.get(index));
			if(null == physNode)
				continue;
			physNodes.add(physNode);
		}
		
		return physNodes;
	}
	
	private PhysNode getPhysNode(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		return getPhysNodeInfo(rootNode);
	}
	
    private PhysNode getPhysNodeInfo(JsonNode physJsonNode){
    	if(null == physJsonNode)
			return null;
    	
    	PhysNode physNode = new PhysNode();
    	physNode.setInstance_uuid(physJsonNode.path(ResponseConstant.INSTANCE_UUID).textValue());
    	physNode.setMaintenance(physJsonNode.path(ResponseConstant.MAINTENANCE).booleanValue());
    	physNode.setName(physJsonNode.path(ResponseConstant.NAME).textValue());
    	physNode.setPower_state(physJsonNode.path(ResponseConstant.POWER_STATE).textValue());
    	//no need to show node uuid
    	physNode.setUuid(physJsonNode.path(ResponseConstant.UUID).textValue());
    	//no console type
    	//physNode.setConsole_enabled(physJsonNode.path(ResponseConstant.CONSOLE_ENABLED).booleanValue());
    	
  //  	physNode.setCreated_at(physJsonNode.path(ResponseConstant.CREATE_AT).textValue());
      	physNode.setCreated_at(physJsonNode.path(ResponseConstant.CREATED_AT).textValue());  	
      	physNode.setMillionSeconds(Util.utc2Millionsecond(physJsonNode.path(ResponseConstant.CREATED_AT).textValue()));
    	//physNode.setDriver(physJsonNode.path(ResponseConstant.DRIVER).textValue());
    	//no inspector now
        //physNode.setInspection_finished_at(physJsonNode.path(ResponseConstant.INSPECTION_FINISHED_AT).textValue());
    	//physNode.setInspection_started_at(physJsonNode.path(ResponseConstant.INSPECTION_STARTED_AT).textValue());
    	// no network interface in node info 
    	//physNode.setNetwork_interface(physJsonNode.path(ResponseConstant.NETWORK_INTERFACE).textValue());
    	//node准备状�?
    	physNode.setProvision_state(physJsonNode.path(ResponseConstant.PROVISION_STATE).textValue());
    	physNode.setProvision_updated_at(physJsonNode.path(ResponseConstant.PROVISION_UPDATED_AT).textValue());
    	//physNode.setRaid_config(physJsonNode.path(ResponseConstant.RAID_CONFIG).textValue());
    	//physNode.setReservation(physJsonNode.path(ResponseConstant.RESERVATION).textValue());
    	//physNode.setResource_class(physJsonNode.path(ResponseConstant.RESOURCE_CLASS).textValue());
    	//physNode.setTarget_power_state(physJsonNode.path(ResponseConstant.TARGET_POWER_STATE).textValue());
    	physNode.setTarget_provision_state(physJsonNode.path(ResponseConstant.TARGET_PROVISION_STATE).textValue());
    	//physNode.setTarget_raid_config(physJsonNode.path(ResponseConstant.TARGET_RAID_CONFIG).textValue());
    	//setDriverInfo(physNode,physJsonNode);
    	// set hardware info
    	JsonNode properties = physJsonNode.path(ResponseConstant.PROPERTIES);
    	physNode.setMemory_mb(properties.path(ResponseConstant.MEMORY).textValue());
    	physNode.setCpu_arch(properties.path(ResponseConstant.CPU_ARCH).textValue());
    	physNode.setLocal_gb(properties.path(ResponseConstant.LOCAL_GB).textValue());
    	physNode.setCpus(properties.path(ResponseConstant.CPUS).textValue());
    	//not sure
    	physNode.setProperties(properties.textValue());
    	
    	// set drive info
    	JsonNode driverJsonInfo = physJsonNode.path(ResponseConstant.DRIVER_INFO);
    	DriverInfo driverInfo = new DriverInfo();
    	driverInfo.setIpmi_address(driverJsonInfo.path(ResponseConstant.IPMI_ADDRESS).textValue());
    	driverInfo.setIpmi_username(driverJsonInfo.path(ResponseConstant.IPMI_USERNAME).textValue());
    	physNode.setDriver_info(driverInfo);
    	physNode.setDriver_info_str(driverJsonInfo.textValue());
		return physNode;
    }
    
//    private void setDriverInfo(PhysNode physNode,JsonNode physJsonNode){
//    	JsonNode dirverInfoNode = physJsonNode.path(ResponseConstant.DRIVER_INFO);
//    	DriverInfo driverInfo = new DriverInfo();
//    	driverInfo.setId(Util.makeUUID());
//    	driverInfo.setIpmi_username(dirverInfoNode.path(ResponseConstant.IPMI_USERNAME).textValue());
//    	driverInfo.setIpmi_password(dirverInfoNode.path(ResponseConstant.IPMI_PASSWORD).textValue());
//    	physNode.setDriver_info(driverInfo);
//    }
    
    private PhysPort getPhysPort(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		return getPhysPortInfo(rootNode);
	}
	
    private PhysPort getPhysPortInfo(JsonNode physJsonNode){
    	if(null == physJsonNode)
			return null;
    	
    	PhysPort physPort = new PhysPort();
    	physPort.setAddress(physJsonNode.path(ResponseConstant.ADDRESS).textValue());
    	physPort.setUuid(physJsonNode.path(ResponseConstant.UUID).textValue());
    	
  //  	JsonNode attrNode = physJsonNode.path(ResponseConstant.CREATE_AT);
      	JsonNode attrNode = physJsonNode.path(ResponseConstant.CREATED_AT);
    	if(null != attrNode && !attrNode.isMissingNode())
    		physPort.setCreated_at(attrNode.textValue());	
    	else
        	physPort.setCreated_at(Util.getCurrentDate());
	    physPort.setMillionSeconds(Util.utc2Millionsecond(physPort.getCreated_at()));
		
    	attrNode = physJsonNode.path(ResponseConstant.NODE_UUID);
    	if(null != attrNode)
    		physPort.setNode_uuid(attrNode.textValue());
    	
    	attrNode = physJsonNode.path(ResponseConstant.PXE_ENABLED);
    	if(null != attrNode)
    		physPort.setPxe_enabled(attrNode.booleanValue());
    	
    	attrNode = physJsonNode.path(ResponseConstant.UPDATE_AT);
    	if(null != attrNode)
    		physPort.setUpdated_at(attrNode.textValue());
    	
		return physPort;
    }
    
    private PhysNode getNodeWithInstance(TokenOs ostoken, String instance_uuid) throws BusinessException {
		TokenOs adminToken = null;
		Locale locale = new Locale(ostoken.getLocale());
		try {
			adminToken = authService.createDefaultAdminOsToken();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
		}
		
		String region = ostoken.getCurrentRegion();
		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
		headers.put(ParamConstant.IRONIC_API_VERSION_STRING, ParamConstant.IRONIC_API_VERSION_VALUE);
		
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v1/nodes/detail?instance_uuid=" + instance_uuid);
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		List<PhysNode> physNodes = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				physNodes = getPhysNodes(rs);
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
				throw new ResourceBusinessException(Message.CS_NODE_GET_FAILED,httpCode,locale);
			try {
				physNodes = getPhysNodes(rs);
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
			throw new ResourceBusinessException(Message.CS_NODE_GET_FAILED,httpCode,locale);
		}
		if(physNodes==null || physNodes.size() == 0 )
			return null;
		return physNodes.get(0);
    }

	@Override
	public List<HardWare> getAvailableSpec(TokenOs ostoken) throws BusinessException {
		// TODO Auto-generated method stub
		TokenOs adminToken = null;
		Locale locale = new Locale(ostoken.getLocale());
		try {
			adminToken = authService.createDefaultAdminOsToken();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
		}
		
		String region = ostoken.getCurrentRegion();
		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
		headers.put(ParamConstant.IRONIC_API_VERSION_STRING, ParamConstant.IRONIC_API_VERSION_VALUE);
		
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v1/nodes/detail?provision_state=available");
		
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		List<PhysNode> physNodes = null;
		List<HardWare> hardWares = new ArrayList<HardWare>();
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				physNodes = getPhysNodes(rs);
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
				throw new ResourceBusinessException(Message.CS_NODE_GET_FAILED,httpCode,locale);
			try {
				physNodes = getPhysNodes(rs);
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
			throw new ResourceBusinessException(Message.CS_NODE_GET_FAILED,httpCode,locale);
		}
		if(physNodes==null || physNodes.size() == 0 )
			return null;
		for (PhysNode physNode : physNodes) {
			HardWare hardWare = new HardWare();
			hardWare.setCpu_arch(physNode.getCpu_arch());
			hardWare.setCpus(physNode.getCpus());
			hardWare.setLocal_gb(physNode.getLocal_gb());
			hardWare.setMemory_mb(physNode.getMemory_mb());
			hardWares.add(hardWare);
		} 

		return hardWares;
	}

	@Override
	//创建物理�?
	public String createInstance(String createBody, TokenOs ostoken)
			throws BusinessException, JsonProcessingException, IOException {
		/*** createBody example
		{
			"name":"testironic",
			"cpus" : "4",
			"memory_mb":"1024",
			"local_gb":"400",
			"cpu_arch":"x86_64",
			"imageRef":"20186195-2db0-4fd5-9384-5b3b702c99a8",
			"password":"testpassword",
			"keypair":"mykeypair"
		} 
		 ***/
		
		//STEP1 get parameter from creatBody
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(createBody);
		String name = rootNode.path(ParamConstant.NAME).asText();
		String flavorid = flavorService.getFlavor(ostoken, rootNode.path(ResponseConstant.CPUS).asText(), rootNode.path(ResponseConstant.MEMORY).asText(), 
				rootNode.path(ResponseConstant.LOCAL_GB).asText(), rootNode.path(ResponseConstant.CPU_ARCH).asText(), 
				ParamConstant.BAREMETAL_TYPE);
		String image_uuid =  rootNode.path(ParamConstant.IMAGEREF).asText();
		String password = rootNode.path(ParamConstant.PASSWORD_CREDENTIAL).asText();
		String key_name = rootNode.path(ParamConstant.KEYPAIR_CREDENTIAL).asText();
		
		
		//STEP2 generate json for openstack request
		InstanceJSON instanceJSON = new InstanceJSON(name, image_uuid, flavorid, key_name, password,zone, 1,
				1);
		instanceJSON.createNetworks(cloudconfig.getSystemIronicNetworkId(), null, null);
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.setSerializationInclusion(Include.NON_EMPTY);
		String jsonStr = mapper.writeValueAsString(instanceJSON);
		
		//STEP3-1 get openstack url
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/servers", null);
		//STEP3-2 insert token in header 
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		//STEP3-3 make Post request to openstack
		Map<String, String> rs = client.httpDoPost(url, headers, jsonStr);		
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String instanceDetail = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE: {
			try {
				instanceDetail = rs.get(ResponseConstant.JSONBODY);
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
			rs = client.httpDoPost(url, headers, jsonStr);
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				instanceDetail = rs.get(ResponseConstant.JSONBODY);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			String errorMessageId = Message.CS_COMPUTE_BAREMETEL_INSTANCE_CREATE_FAILED;
			throw new ResourceBusinessException(errorMessageId,locale);
		}
		//TODO get instance detail info and save to db
		String uuid =  mapper.readTree(instanceDetail).path("server").path("id").textValue();
		getInstance(uuid, ostoken);
		//get node detail and save to db
		PhysNode physNodeWithInstance = getNodeWithInstance(ostoken, uuid);
		physNodeMapper.insertOrUpdate(physNodeWithInstance);
		return instanceDetail;		
	}
	
	public Instance getInstance(String uuid,TokenOs ostoken) throws BusinessException{
		
		Instance instance = null;
		List<Instance> instances = null;  
		//step1 get instance from api db
		List<String> ids = new ArrayList<String>();
		ids.add(uuid);
		instances = instanceMapper.selectListByInstanceIds(ids);
		if(instances != null && instances.size() !=0 ){
			return instances.get(0);
		}
		//step2  if step1 return null, get instance from oepnstack
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/servers/");
		sb.append(uuid);
				
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
			ObjectMapper mapper = new ObjectMapper();
			try {
				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				JsonNode serverNode = rootNode.path(ResponseConstant.INSTANCE);
				mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
				mapper.setSerializationInclusion(Include.NON_NULL);
				mapper.setSerializationInclusion(Include.NON_EMPTY);
				mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
				instance = mapper.readValue(serverNode.toString(), Instance.class);
			} catch (IOException e) {
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
			ObjectMapper mapper = new ObjectMapper();
			try {
				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				JsonNode serverNode = rootNode.path(ResponseConstant.INSTANCE);
				instance = getInstanceDetailInfo(serverNode, ostoken, true, null);
			} catch (IOException e) {
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_GET_FAILED,httpCode,locale);
		}
		
		instance.setTenantId(ostoken.getTenantid());
		instanceMapper.insertSelective(instance);
		return instance;
	}
	
	private Instance getInstanceDetailInfo(JsonNode serverNode, TokenOs ostoken, Boolean details,
			Instance instanceInfoFromDB) throws BusinessException {

		if (null == serverNode)
			return null;
		Instance instanceDetail = new Instance();
		instanceDetail.setId(serverNode.path(ResponseConstant.ID).textValue());
		instanceDetail.setName(serverNode.path(ResponseConstant.NAME).textValue());
		instanceDetail.setStatus(serverNode.path(ResponseConstant.STATUS).textValue());
		instanceDetail.setCreatedAt(serverNode.path(ResponseConstant.CREATED).textValue());
		instanceDetail.setTenantId(serverNode.path(ResponseConstant.TENANT_ID).textValue());
		instanceDetail.setType(serverNode.path(ResponseConstant.OS_EXT_AZ_AVAILABILITY_ZONE).textValue());
		instanceDetail.setAvailabilityZone(serverNode.path(ResponseConstant.OS_EXT_AZ_AVAILABILITY_ZONE).textValue());
		// image
		if (null == instanceInfoFromDB) {
			JsonNode imageNode = serverNode.path(ResponseConstant.IMAGE);
			if (null != imageNode && (null != imageNode.path(ResponseConstant.ID).textValue()
					&& !imageNode.path(ResponseConstant.ID).textValue().isEmpty())) {
				instanceDetail.setSourceId(imageNode.path(ResponseConstant.ID).textValue());
				String imageName = ""; // TODO get the image name
				if (imageName.isEmpty()) {
//					ImageServiceImpl imgService = new ImageServiceImpl();
//					imgService.setCloudconfig(cloudconfig);
//					imgService.setImageMapper(imageMapper);
					Image image = imageService.getImage(instanceDetail.getSourceId(), ostoken);
					instanceDetail.setImage(image);
					instanceDetail.setSourceName(image.getName());
				}
			}
		} else {
			Image image = new Image(instanceInfoFromDB.getSourceId(), instanceInfoFromDB.getSourceName());
			instanceDetail.setImage(image);
			instanceDetail.setSourceId(instanceInfoFromDB.getSourceId());
			instanceDetail.setSourceName(instanceInfoFromDB.getSourceName());
		}
		
		// ip addresses
		if(null == instanceInfoFromDB || (Util.isNullOrEmptyValue(instanceInfoFromDB.getFixedips()) && 
				Util.isNullOrEmptyValue(instanceInfoFromDB.getFloatingips()))){
			setIpInfo(serverNode,instanceDetail);
		}else{
			if(!Util.isNullOrEmptyValue(instanceInfoFromDB.getFixedips())){
				instanceDetail.setIps(Util.stringToList(instanceInfoFromDB.getFixedips(), ","));
				instanceDetail.setFixedips(instanceInfoFromDB.getFixedips());
			}
			if(!Util.isNullOrEmptyValue(instanceInfoFromDB.getFloatingips())){
				instanceDetail.setFloatingIps(Util.stringToList(instanceInfoFromDB.getFloatingips(),","));
				instanceDetail.setFloatingips(instanceInfoFromDB.getFloatingips());
			}
		}
	//	if (false == details)
	//		return instanceDetail;
		
		// flavor
		if (instanceInfoFromDB == null) {
			JsonNode flavorNode = serverNode.path(ResponseConstant.FLAVOR);
			if (null != flavorNode) {
				String flavorId = flavorNode.path(ResponseConstant.ID).textValue();
				Flavor flavor = flavorService.getFlavor(flavorId, ostoken);
				if (null != flavor) {
					instanceDetail.setCore(Integer.toString(flavor.getVcpus()));
					instanceDetail.setRam(Integer.toString(flavor.getRam()));
				}
			}
		} else {
			instanceDetail.setCore(instanceInfoFromDB.getCore());
			instanceDetail.setRam(instanceInfoFromDB.getRam());
		}

		// metadata
		JsonNode metadataNode = serverNode.path(ResponseConstant.METADATA);
		if (null != metadataNode) {
			instanceDetail.setVolumeType(metadataNode.path(ResponseConstant.VOLUME_TYPE).textValue());
		}

		// keypair
		String keypairName = serverNode.path(ResponseConstant.KEY_NAME).textValue();
		if (!Util.isNullOrEmptyValue(keypairName)) {
			Keypair keypair = keypairService.getKeypair(keypairName, ostoken);
			if (null != keypair)
				instanceDetail.addKeypair(keypair);
		}
		instanceDetail.makeKeypairIds();

		// private images
		List<Image> images = null;
		if(null != instanceInfoFromDB && !Util.isNullOrEmptyValue(instanceInfoFromDB.getImageIds())){
			images = instanceInfoFromDB.getImages();
		}else{
			images = imageService.getInstanceImages(instanceDetail.getId());
		}
		if(null != images){
			instanceDetail.setImages(images);
			instanceDetail.makePrivateImageIds();
		}
				
		
		// networks
		List<Network> networks = null;
		if(null != instanceInfoFromDB && !Util.isNullOrEmptyValue(instanceInfoFromDB.getNetworkIds())){
			networks = instanceInfoFromDB.getNetworks();//networkMapper.selectNetworksById(instanceInfoFromDB.getNetworkIds().split(","));
		}else{
		     networks = networkService.getInstanceAttachedNetworks(instanceDetail.getId());
		}
		if(null != networks){
			instanceDetail.setNetworks(networks);
			instanceDetail.makeNetworkIds();
		}
		
		// SecurityGroups
		List<SecurityGroup> securityGroups = null;
		if(null != instanceInfoFromDB && !Util.isNullOrEmptyValue(instanceInfoFromDB.getSecurityGroupIds())){
			securityGroups = instanceInfoFromDB.getSecurityGroups();//securityGroupMapper.selectSecurityGroupsById(instanceInfoFromDB.getSecurityGroupIds().split(","));
		}else{
			 securityGroups = securityGroupService.getInstanceAttachedSecurityGroup(serverNode,ostoken.getTenantid());
		}
		if(null != securityGroups){
			instanceDetail.setSecurityGroups(securityGroups);
			instanceDetail.makeSecurityGroupIds();
		}
		
		// ports
		List<Port> ports = null;
		if(null != instanceInfoFromDB && !Util.isNullOrEmptyValue(instanceInfoFromDB.getPortIds())){
			ports = instanceInfoFromDB.getPorts();//portMapper.selectPortsById(instanceInfoFromDB.getPortIds().split(","));
		}
		if(null != ports){
			instanceDetail.setPorts(ports);
			instanceDetail.setPortIds(instanceInfoFromDB.getPortIds());
		}
		
		return instanceDetail;
	}
	private void setIpInfo(JsonNode serverNode, Instance instanceDetail) {
		List<String> fixedipList = new ArrayList<String>();
		List<String> floatingiplist = new ArrayList<String>();
		JsonNode addressesNode = serverNode.path(ResponseConstant.ADDRESSES);
		Iterator<Entry<String, JsonNode>> elements = addressesNode.fields();
		while (elements.hasNext()) {
			Entry<String, JsonNode> node = elements.next();
			JsonNode networks = node.getValue();
			int size = node.getValue().size();
			for (int index = 0; index < size; ++index) {
				JsonNode network = networks.get(index);
				String ipAddress = network.path(ResponseConstant.ADDR).textValue();
				// String version =
				// network.path(ResponseConstant.VERSION).textValue();
				String ipType = network.path(ResponseConstant.EXT_IP_TYPE).textValue();
				// String mac =
				// network.path(ResponseConstant.MAC_ADDR).textValue();
				if (ParamConstant.FIXED.equals(ipType))
					fixedipList.add(ipAddress);
				else
					floatingiplist.add(ipAddress);
			}
		}
		instanceDetail.setIps(fixedipList);
		instanceDetail.setFloatingIps(floatingiplist);
		instanceDetail.setFixedips(Util.listToString(fixedipList, ','));
		instanceDetail.setFloatingips(Util.listToString(floatingiplist, ','));
	}

	@Override
	public String changeNodespower(String actionBody, String action, TokenOs authToken) throws JsonProcessingException, IOException, ResourceBusinessException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(actionBody);
		JsonNode idsNode = rootNode.path(ResponseConstant.IDS);
		int idsCount = idsNode.size();
		boolean updateFailed = false;
		List<String> updateNodeId = new ArrayList<String>();
		String updateBody = "\"target:\"\"rebooting\"";
		if(!action.equals("rebooting"))
			updateBody = String.format("\"target:\"\"power %s\"", action);
		
		for(int index = 0; index < idsCount; ++index){
			String nodeId = idsNode.get(index).textValue();
			try{
				changePhysNodePowerStates(nodeId, updateBody, authToken);
				updateNodeId.add(nodeId);
			}catch (Exception e){
				updateFailed = true;
			}
		}
		
		if(true == updateFailed){
			throw new ResourceBusinessException(Message.CS_NODE_UPDATE_POWER_STATES_FAILED,ParamConstant.NOT_FOUND_RESPONSE_CODE,new Locale(authToken.getLocale()));
		}
		return Util.listToString(updateNodeId, ',');
	}
}
