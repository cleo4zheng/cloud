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
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.BareMetalNode;
import com.cloud.cloudapi.pojo.openstackapi.forgui.BareMetalPort;
import com.cloud.cloudapi.pojo.openstackapi.forgui.BareMetalPropertyInfo;
import com.cloud.cloudapi.pojo.openstackapi.forgui.DriverInfo;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.BareMetalService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("bareMetalService")
public class BareMetalServiceImpl implements BareMetalService {	
	@Resource
	private OSHttpClientUtil client;

	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(BareMetalServiceImpl.class);
	
	@Override
	public List<BareMetalNode> getBareMetalNodes(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException{
		
		Locale locale = new Locale(ostoken.getLocale());
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v1/nodes/detail", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		List<BareMetalNode> bareMetalNodes = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				bareMetalNodes = getBareMetalNodes(rs);
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
			} catch (Exception e1) {
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
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode, locale);
			try {
				bareMetalNodes = getBareMetalNodes(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE, locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode, locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode, locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode, locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode, locale);
		default:
			throw new ResourceBusinessException(Message.CS_BAREMETAL_NODE_GET_FAILED,httpCode, locale);
		}
		return bareMetalNodes;
	}
	
	@Override
	public BareMetalNode getBareMetalNode(String bareMetalId,TokenOs ostoken) throws BusinessException{
		
		Locale locale = new Locale(ostoken.getLocale());
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v1/nodes/");
		sb.append(bareMetalId);
	
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		BareMetalNode bareMetalNode = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				bareMetalNode = getBareMetalNode(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE, locale);
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
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode, locale);
			try {
				bareMetalNode = getBareMetalNode(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE, locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode, locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode, locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode, locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode, locale);
		default:
			throw new ResourceBusinessException(Message.CS_BAREMETAL_NODE_DETAIL_GET_FAILED,httpCode, locale);
		}
		return bareMetalNode;
	}
	
	@Override
	public BareMetalNode createBareMetalNode(String createBody,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		Map<String, String> rs = client.httpDoPost(url+"/v1/nodes", headers,createBody);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		BareMetalNode bareMetalNode = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				bareMetalNode = getBareMetalNode(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE, locale);
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
			rs =  client.httpDoPost(url+"/v1/nodes", headers,createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode, locale);
			try {
				bareMetalNode = getBareMetalNode(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE, locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode, locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode, locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode, locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode, locale);
		default:
			throw new ResourceBusinessException(Message.CS_BAREMETAL_NODE_CREATE_FAILED,httpCode, locale);
		}
		return bareMetalNode;
	}
	
	@Override
	public List<BareMetalPort> getBareMetalPorts(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v1/ports/detail", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		List<BareMetalPort> bareMetalPorts = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				bareMetalPorts = getBareMetalPorts(rs);
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
			rs = client.httpDoGet(url + "/v1/ports/detail", headers);
		    httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		    failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
		    if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode, locale);
			try {
				bareMetalPorts = getBareMetalPorts(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode, locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_BAREMETAL_PORT_GET_FAILED,httpCode,locale);
		}
		return bareMetalPorts;
	}
	
	@Override
	public BareMetalPort getBareMetalPort(String portId,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v1/ports/");
		sb.append(portId);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		BareMetalPort bareMetalPort = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				bareMetalPort = getBareMetalPort(rs);
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
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode, locale);
			try {
				bareMetalPort = getBareMetalPort(rs);
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
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode, locale);
		default:
			throw new ResourceBusinessException(Message.CS_BAREMETAL_PORT_DETAIL_GET_FAILED,httpCode,locale);
		}
		return bareMetalPort;
	}
	
	@Override
	public BareMetalPort createBareMetalPort(String createBody,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_IRONIC, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		Map<String, String> rs = client.httpDoPost(url+"/v1/ports", headers,createBody);
		Util.checkResponseBody(rs,locale);

		BareMetalPort bareMetalPort = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				bareMetalPort = getBareMetalPort(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE, locale);
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
			rs =  client.httpDoPost(url+"/v1/ports", headers,createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		    if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode, locale);
			try {
				bareMetalPort = getBareMetalPort(rs);
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
			throw new ResourceBusinessException(Message.CS_BAREMETAL_PORT_CREATE_FAILED,httpCode,locale);
		}
		return bareMetalPort;
	}
	
	private BareMetalPort getBareMetalPortInfo(JsonNode bareMetalPortNode){
		if(null == bareMetalPortNode)
			return null;
		BareMetalPort bareMetalPort = new BareMetalPort();
		bareMetalPort.setNode_uuid(bareMetalPortNode.path(ResponseConstant.NODE_UUID).textValue());
		bareMetalPort.setUpdated_at(bareMetalPortNode.path(ResponseConstant.UPDATE_AT).textValue());
		bareMetalPort.setUuid(bareMetalPortNode.path(ResponseConstant.UUID).textValue());
		bareMetalPort.setId(bareMetalPort.getUuid());
		bareMetalPort.setAddress(bareMetalPortNode.path(ResponseConstant.ADDRESSES).textValue());
		bareMetalPort.setCreated_at(bareMetalPortNode.path(ResponseConstant.CREATED_AT).textValue());
		return bareMetalPort;
	}
	
	private BareMetalPort getBareMetalPort(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		return getBareMetalPortInfo(rootNode);
	}
	
	private List<BareMetalPort> getBareMetalPorts(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode bareMetalPortsNode = rootNode.path(ResponseConstant.PORTS);
		int bareMetalPortsCount = bareMetalPortsNode.size();
		if (0 == bareMetalPortsCount)
			return null;
		
		List<BareMetalPort> bareMetalPorts = new ArrayList<BareMetalPort>();
		for (int index = 0; index < bareMetalPortsCount; ++index) {
			BareMetalPort bareMetalPort = getBareMetalPortInfo(bareMetalPortsNode.get(index));
			if(null == bareMetalPort)
				continue;
			bareMetalPorts.add(bareMetalPort);
		}
		return bareMetalPorts;
	}
	
	private BareMetalNode getBareMetalNode(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		return getBareMetalNodeInfo(rootNode);
	}
	
	private List<BareMetalNode> getBareMetalNodes(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode bareMetalsNode = rootNode.path(ResponseConstant.NODES);
		int bareMetalsCount = bareMetalsNode.size();
		if (0 == bareMetalsCount)
			return null;
		
		List<BareMetalNode> bareMetalNodes = new ArrayList<BareMetalNode>();
		for (int index = 0; index < bareMetalsCount; ++index) {
			BareMetalNode bareMetalNode = getBareMetalNodeInfo(bareMetalsNode.get(index));
			if(null == bareMetalNode)
				continue;
			bareMetalNodes.add(bareMetalNode);
		}
		
		return bareMetalNodes;
	}
	
	private BareMetalNode getBareMetalNodeInfo(JsonNode bareMetalNode){
		if(null == bareMetalNode)
			return null;
		BareMetalNode bareMetal = new BareMetalNode();
		bareMetal.setReservation(bareMetalNode.path(ResponseConstant.RESERVATION).textValue());
		bareMetal.setDriver(bareMetalNode.path(ResponseConstant.DRIVER).textValue());
		bareMetal.setUuid(bareMetalNode.path(ResponseConstant.UUID).textValue());
		bareMetal.setId(bareMetal.getUuid());
		bareMetal.setProvision_updated_at(bareMetalNode.path(ResponseConstant.PROVISION_UPDATED_AT).textValue());
		bareMetal.setPower_state(bareMetalNode.path(ResponseConstant.PROVISION_STATE).textValue());
		bareMetal.setMaintenance(bareMetalNode.path(ResponseConstant.MAINTENANCE).booleanValue());
		bareMetal.setConsole_enabled(bareMetalNode.path(ResponseConstant.CONSOLE_ENABLED).booleanValue());
		bareMetal.setInspection_started_at(bareMetalNode.path(ResponseConstant.INSPECTION_STARTED_AT).textValue());
		bareMetal.setLast_error(bareMetalNode.path(ResponseConstant.LAST_ERROR).textValue());
		bareMetal.setName(bareMetalNode.path(ResponseConstant.NAME).textValue());
		bareMetal.setCreated_at(bareMetalNode.path(ResponseConstant.CREATED_AT).textValue());
		bareMetal.setUpdated_at(bareMetalNode.path(ResponseConstant.UPDATED_AT).textValue());
		bareMetal.setMaintenance_reason(bareMetalNode.path(ResponseConstant.MAINTENANCE_REASON).textValue());
		bareMetal.setInspection_finished_at(bareMetalNode.path(ResponseConstant.INSPECTION_FINISHED_AT).textValue());
		bareMetal.setInstance_uuid(bareMetalNode.path(ResponseConstant.INSTANCE_UUID).textValue());
		bareMetal.setPower_state(bareMetalNode.path(ResponseConstant.POWER_STATE).textValue());
		bareMetal.setTarget_power_state(bareMetalNode.path(ResponseConstant.TARGET_POWER_STATE).textValue());
		bareMetal.setTarget_provision_state(bareMetalNode.path(ResponseConstant.TARGET_PROVISION_STATE).textValue());
		
		setBareMetalProertiesInfo(bareMetal,bareMetalNode.path(ResponseConstant.PROPERTIES));
		setBareMetalDriverInfo(bareMetal,bareMetalNode.path(ResponseConstant.DRIVER_INFO));
		
		return bareMetal;
	}
	
	private void setBareMetalProertiesInfo(BareMetalNode bareMetal,JsonNode proertiesNode){
		if(null == proertiesNode)
			return;
		BareMetalPropertyInfo bareMetalPropertyInfo = new BareMetalPropertyInfo();
		bareMetalPropertyInfo.setCpus(proertiesNode.path(ResponseConstant.CPUS).intValue());
		bareMetalPropertyInfo.setMemory_mb(proertiesNode.path(ResponseConstant.MEMORY).intValue());
		bareMetalPropertyInfo.setLocal_gb(proertiesNode.path(ResponseConstant.LOCAL_GB).intValue());
		bareMetalPropertyInfo.setCpu_arch(proertiesNode.path(ResponseConstant.CPU_ARCH).textValue());
		bareMetal.setProperties(bareMetalPropertyInfo);
	}
	
	private void setBareMetalDriverInfo(BareMetalNode bareMetal,JsonNode driverNode){
		if(null == driverNode)
			return;
		DriverInfo driverInfo = new DriverInfo();
		driverInfo.setDeploy_kernel(driverNode.path(ResponseConstant.DEPLOY_KERNEL).textValue());
		driverInfo.setIpmi_address(driverNode.path(ResponseConstant.IPMI_ADDRESS).textValue());
		driverInfo.setDeploy_ramdisk(driverNode.path(ResponseConstant.DEPLOY_RAMDISK).textValue());
		driverInfo.setIpmi_password(driverNode.path(ResponseConstant.IPMI_PASSWORD).textValue());
		driverInfo.setIpmi_username(driverNode.path(ResponseConstant.IPMI_USERNAME).textValue());
		bareMetal.setDriver_info(driverInfo);;
	}
}
