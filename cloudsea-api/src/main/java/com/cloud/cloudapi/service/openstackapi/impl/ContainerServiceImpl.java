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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.ContainerMapper;
import com.cloud.cloudapi.dao.common.ContainerModelMapper;
import com.cloud.cloudapi.dao.common.ResourceCreateProcessMapper;
import com.cloud.cloudapi.dao.common.ResourceEventMapper;
import com.cloud.cloudapi.dao.common.SyncResourceMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Container;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ContainerModel;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Flavor;
import com.cloud.cloudapi.pojo.openstackapi.forgui.RequestContainer;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceCreateProcess;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceEvent;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SyncResource;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.ContainerService;
import com.cloud.cloudapi.service.openstackapi.FlavorService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("containerService")
public class ContainerServiceImpl implements ContainerService {
	
	@Resource
	private OSHttpClientUtil client;
	
	@Autowired
	private ContainerMapper containerMapper;
	
	@Autowired
	private ContainerModelMapper containerModelMapper;
	
	@Autowired
	private FlavorService flavorService;
	
	@Autowired
	private SyncResourceMapper syncResourceMapper;
	
	@Autowired
	private ResourceCreateProcessMapper resourceCreateProcessMapper;

	@Autowired
	private ResourceEventMapper resourceEventMapper;
	
	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(ContainerServiceImpl.class);
	
	@Value("#{ propertyConfigurer['magnum.coe'] }")
	public String coe;
	
	@Override
	public List<Container> getConstainers(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException{
		
		//get from db
		int limit = Util.getLimit(paramMap);
		List<Container> bays = getContainersFromDB(ostoken.getTenantid(), limit);
		if(!Util.isNullOrEmptyList(bays))
			return bays;
		//get from openstack
		String region = ostoken.getCurrentRegion();
        Locale locale = new Locale(ostoken.getLocale());
        String url = null;
        try{
        	 url = ostoken.getEndPoint(TokenOs.EP_TYPE_CONTAINER, region).getPublicURL();
        }catch (Exception e){
        	log.error("error",e);
        	return null;
        }
		url = RequestUrlHelper.createFullUrl(url + "/bays", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				bays = getBays(rs);
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
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				bays = getBays(rs);
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
			throw new ResourceBusinessException(Message.CS_CONTAINER_GET_FAILED,httpCode,locale);
		}
		if (!Util.isNullOrEmptyList(bays)) {
			for (Container container : bays) {
				container.setTenantId(ostoken.getTenantid());
			}
			saveContainersToDB(bays);
		}		
		return bays;
	}
	
	@Override
	public Container getContainer(String containerId,TokenOs ostoken) throws BusinessException{
		/*
		Container bay = containerMapper.selectByPrimaryKey(containerId);
		if(null != bay)
			return bay;
		*/
		Locale locale = new Locale(ostoken.getLocale());
		Container bay = null;
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_CONTAINER, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/bays/");
		sb.append(containerId);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				bay = getBay(rs);
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
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				bay = getBay(rs);
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
			throw new ResourceBusinessException(Message.CS_CONTAINER_DETAIL_GET_FAILED,httpCode,locale);
		}
		bay.setTenantId(ostoken.getTenantid());
		containerMapper.updateByPrimaryKeySelective(bay);
		return bay;
	}
	
	@Override
	public Container createContainer(String createBody, TokenOs ostoken)
			throws BusinessException {
		Locale locale = new Locale(ostoken.getLocale());
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_CONTAINER, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoPost(url + "/bays", ostoken.getTokenid(), createBody);
		Util.checkResponseBody(rs,locale);
		Container bay = null;
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: 
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE:{
			try {
				bay = getBay(rs);
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
			rs = client.httpDoPost(url + "/bays", ostoken.getTokenid(), createBody);
		    failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			if (httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE || httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				bay = getBay(rs);
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
			throw new ResourceBusinessException(Message.CS_CONTAINER_CREATE_FAILED,httpCode,locale);
		}

		bay.setTenantId(ostoken.getTenantid());
		containerMapper.insertSelective(bay);
		updateSyncResourceInfo(ostoken.getTenantid(),bay.getUuid(),null,ParamConstant.CREATE_COMPLETE, ostoken.getCurrentRegion());
		storeResourceEventInfo(ostoken.getTenantid(),bay.getUuid(),ParamConstant.CONTAINER,null,ParamConstant.CREATE_COMPLETE,Util.getCurrentMillionsecond());

		return bay;
	}

	private void updateSyncResourceInfo(String tenantId,String id,String orgStatus,String expectedStatus, String region)  {
		SyncResource resource = new SyncResource();
		resource.setId(id);
		resource.setType(ParamConstant.CONTAINER);
		resource.setOrgStatus(orgStatus);
		resource.setExpectedStatus(expectedStatus);
		resource.setRegion(region);
		syncResourceMapper.insertSelective(resource);
		
		ResourceCreateProcess createProcess = new ResourceCreateProcess();
		createProcess.setId(id);
		createProcess.setTenantId(tenantId);
		createProcess.setType(ParamConstant.CONTAINER);
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
	
	@Override
	public void deleteContainer(String containerId,TokenOs ostoken) throws BusinessException{
		// token should have Regioninfo
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(containerId, locale);
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_CONTAINER, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/bays/");
		sb.append(containerId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE: 
		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE:{
			break;
		}
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			break;
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
			if (httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE || httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_CONTAINER_DELETE_FAILED,httpCode,locale);
		}
		Container container = containerMapper.selectByPrimaryKey(containerId);
		containerMapper.deleteByPrimaryKey(containerId);
		updateSyncResourceInfo(ostoken.getTenantid(),container.getUuid(),container.getStatus(),ParamConstant.DELETED_STATUS, ostoken.getCurrentRegion());
		storeResourceEventInfo(ostoken.getTenantid(),container.getUuid(),ParamConstant.CONTAINER,container.getStatus(),ParamConstant.DELETED_STATUS,Util.getCurrentMillionsecond());
	}
	
	@Override
	public List<ContainerModel> getContainerModels(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException{
	
		List<ContainerModel> models = containerModelMapper.selectAllByTenantId(ostoken.getTenantid());
		if(!Util.isNullOrEmptyList(models))
			return models;
		
		// todo 1: 閫氳繃guitokenid 鍙栧緱瀹為檯锛岀敤鎴蜂俊鎭�
		// AuthService as = new AuthServiceImpl();
		Locale locale = new Locale(ostoken.getLocale());
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_CONTAINER, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/baymodels", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				models = getBayModels(rs);
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
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				models = getBayModels(rs);
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
			throw new ResourceBusinessException(Message.CS_CONTAINER_MODEL_GET_FAILED,httpCode,locale);
		}
		for (ContainerModel containerModel : models) {
			containerModel.setTenantId(ostoken.getTenantid());
		}
		saveContainerModelsToDB(models);
		return models;
	}

	@Override
	public ContainerModel getContainerModel(String modelId,TokenOs ostoken) throws BusinessException{
		/*
		ContainerModel model = containerModelMapper.selectByPrimaryKey(modelId);
		if(null != model)
			return model;
		*/
		ContainerModel model = null;
		String region = ostoken.getCurrentRegion();
		Locale locale = new Locale(ostoken.getLocale());
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_CONTAINER, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/baymodels/");
		sb.append(modelId);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				model = getBayModel(rs);
				Flavor flavor = flavorService.getFlavor(model.getFlavor_id(), ostoken);
				model.setCore(flavor.getVcpus());
				model.setRam(flavor.getRam());
				model.setLocal_volume_size(flavor.getDisk());
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
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				model = getBayModel(rs);
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
			throw new ResourceBusinessException(Message.CS_CONTAINER_MODEL_DETAIL_GET_FAILED,httpCode,locale);
		}
	
		//containerModelMapper.insertSelective(model);
		return model;
	}
	
	@Override
	public ContainerModel createContainerModel(String createBody,TokenOs ostoken) throws BusinessException, JsonParseException, JsonMappingException, IOException{
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
	
		String region = ostoken.getCurrentRegion();
		// String url=ot.getEndPoint(TokenOs.EP_TYPE_NETWORK,
		// region).getPublicURL();
		// url=url+"/v2.0/networks/" + NetworkId;
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_CONTAINER, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.setSerializationInclusion(Include.NON_EMPTY);
		ContainerModel containerModel = mapper.readValue(createBody, ContainerModel.class);
		containerModel.setCoe(coe);
		containerModel.setTls_disabled(true);
		//TODO set public to true
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPost(url + "/baymodels", ostoken.getTokenid(), mapper.writeValueAsString(containerModel));
		Util.checkResponseBody(rs,locale);
		ContainerModel model = null;
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				model = getBayModel(rs);
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
			rs = client.httpDoPost(url + "/baymodels", ostoken.getTokenid(), createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				model = getBayModel(rs);
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
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,locale);
		default:
			throw new ResourceBusinessException(Message.CS_CONTAINER_MODEL_CREATE_FAILED,httpCode,locale);
		}
		
		model.setTenantId(ostoken.getTokenid());
		containerModelMapper.insertSelective(model);
		return model;
	}
	
	@Override
	public void deleteContainerModel(String uuid,TokenOs ostoken) throws BusinessException{
		// token should have Regioninfo
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_CONTAINER, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/baymodels/");
		sb.append(uuid);

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
		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE:{
			break;
		}
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			break;
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
			if (httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE || httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_CONTAINER_DELETE_FAILED,httpCode,locale);
		}
		
		containerModelMapper.deleteByPrimaryKey(uuid);
	}
	
	@Override
	/* createBody example 
	 {
		"name": "tangbay1",
		"master_count": 1, 
		"node_count": 1, 
		"bay_create_timeout": 0,
		"discovery_url":null,
		"baymodel":{
   			"image_id":"6d14b6fe-d4f7-4a58-ad62-0f210920d7af",
    		"keypair_id":"test",
    		"public":true,
    		"tls_disabled":true,
    		"docker_volume_size":"1",
    		"external_network_id":"CTCC",
    		"core":2,
    		"ram":2048,
    		"local_volume_size":20
    	}
	}
	*/
	public Container createContainerNew(String createBody, TokenOs ostoken)
			throws BusinessException, JsonParseException, JsonMappingException, IOException {
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_CONTAINER, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        RequestContainer container = mapper.readValue(createBody, RequestContainer.class);
		JsonNode baymodelNode = mapper.readTree(createBody).path("baymodel");
		
		ContainerModel containerModel =   mapper.readValue(baymodelNode.toString(), ContainerModel.class);		
		String flavor_id = flavorService.getFlavor(ostoken, containerModel.getCore().toString(), containerModel.getRam().toString(),
				containerModel.getLocal_volume_size().toString(),false,ParamConstant.CONTAINER);
		containerModel.setFlavor_id(flavor_id);
		containerModel.setCoe(coe);
		containerModel.setCore(null);
		containerModel.setRam(null);
		containerModel.setLocal_volume_size(null);
		//TODO　get some baymodel already exists
		ContainerModel createdModel = createContainerModel(mapper.writeValueAsString(containerModel), ostoken);
		
		container.setBaymodel_id(createdModel.getUuid());
		container.setDiscovery_url(null);
		mapper.setSerializationInclusion(Include.ALWAYS);
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPost(url + "/bays", ostoken.getTokenid(), mapper.writeValueAsString(container));
		Util.checkResponseBody(rs,locale);
		Container bay = null;
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE:
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE:{
			try {
				bay = getBay(rs);
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
			rs = client.httpDoPost(url + "/bays", ostoken.getTokenid(), createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE || httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				bay = getBay(rs);
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
			throw new ResourceBusinessException(Message.CS_CONTAINER_CREATE_FAILED,httpCode,locale);
		}

		bay.setCore(containerModel.getCore());
		bay.setRam(containerModel.getRam());
		bay.setSystemVolumeSize(containerModel.getLocal_volume_size());
		bay.setDataVolumeSize(containerModel.getDocker_volume_size());
		//TODO set volume type
		bay.setTenantId(ostoken.getTenantid());
		bay.setMillionSeconds(Util.getCurrentMillionsecond());
		bay.setImageId(containerModel.getImage_id());
		containerMapper.insertSelective(bay);
		return bay;
	}
	
	private List<Container> getBays(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode baysNode = rootNode.path(ResponseConstant.BAYS);
		int size = baysNode.size();
		if (0 == size)
			return null;
		 List<Container> bays = new ArrayList<Container>();
		 
		for(int index = 0; index < size; ++index){
			Container bay = getBayInfo(baysNode.get(index));
			if(null == bay)
				continue;
			bays.add(bay);
		}
		
		return bays;
	}
	
	private Container getBay(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		return getBayInfo(rootNode);
	}
	
	private Container getBayInfo(JsonNode bayNode){
		if(null == bayNode)
			return null;
		Container bay = new Container();
		bay.setUuid(bayNode.path(ResponseConstant.UUID).textValue());
		bay.setStatus(bayNode.path(ResponseConstant.STATUS).textValue());
		bay.setStack_id(bayNode.path(ResponseConstant.STACK_ID).textValue());
		bay.setMaster_count(bayNode.path(ResponseConstant.MASTER_COUNT).intValue());
		bay.setBaymodel_id(bayNode.path(ResponseConstant.BAYMODEL_ID).textValue());
		bay.setNode_count(bayNode.path(ResponseConstant.NODE_COUNT).intValue());
		bay.setBay_create_timeout(bayNode.path(ResponseConstant.BAY_CREATE_TIMEOUT).intValue());
		bay.setName(bayNode.path(ResponseConstant.NAME).textValue());
		
		bay.setCreated_at(bayNode.path(ResponseConstant.CREATED_AT).textValue());
		bay.setApi_address(bayNode.path(ResponseConstant.API_ADDRESS).textValue());
		bay.setDiscovery_url(bayNode.path(ResponseConstant.DISCOVERY_URL).textValue());
		bay.setUpdated_at(bayNode.path(ResponseConstant.UPDATED_AT).textValue());
		bay.setStatus_reason(bayNode.path(ResponseConstant.STATUS_REASON).textValue());
		
		JsonNode masterNodes = bayNode.path(ResponseConstant.MASTER_ADDRESSES);
		if (null != masterNodes) {
			int mastersCount = masterNodes.size();
			if(0 != mastersCount){
				List<String> addresses = new ArrayList<String>();
				for (int index = 0; index < mastersCount; ++index) {
					addresses.add(masterNodes.get(index).textValue());
				}
				bay.setMaster_addresses(addresses);
			}
		}
		
		JsonNode nodeNodes = bayNode.path(ResponseConstant.NODE_ADDRESSES);
		if (null != nodeNodes) {
			int nodesCount = nodeNodes.size();
			if(0 != nodesCount){
				List<String> addresses = new ArrayList<String>();
				for (int index = 0; index < nodesCount; ++index) {
					addresses.add(nodeNodes.get(index).textValue());
				}
				bay.setNode_addresses(addresses);
			}
		}
		
		return bay;
	}
	
	
	private ContainerModel getBayModelInfo(JsonNode modelNode){
		if(null == modelNode)
			return null;
		ContainerModel model = new ContainerModel();
		model.setInsecure_registry(modelNode.path(ResponseConstant.INSECURE_REGISTRY).textValue());
		model.setHttp_proxy(modelNode.path(ResponseConstant.HTTP_PROXY).textValue());
		model.setUpdated_at(modelNode.path(ResponseConstant.UPDATED_AT).textValue());
		model.setFloating_ip_enabled(modelNode.path(ResponseConstant.FLOATING_IP_ENABLED).booleanValue());
		model.setFixed_subnet(modelNode.path(ResponseConstant.FIXED_SUBNET).textValue());
		model.setMaster_flavor_id(modelNode.path(ResponseConstant.MASTER_FLAVOR_ID).textValue());
		model.setUuid(modelNode.path(ResponseConstant.UUID).textValue());
		model.setNo_proxy(modelNode.path(ResponseConstant.NO_PROXY).textValue());
		model.setHttp_proxy(modelNode.path(ResponseConstant.HTTPS_PROXY).textValue());
		model.setTls_disabled(modelNode.path(ResponseConstant.TLS_DISABLED).booleanValue());
		model.setKeypair_id(modelNode.path(ResponseConstant.KEYPAIR_ID).textValue());
		model.setPublicFlag(modelNode.path(ResponseConstant.PUBLIC).booleanValue());
		model.setDocker_volume_size(modelNode.path(ResponseConstant.DOCKER_VOLUME_SIZE).intValue());
		model.setServer_type(modelNode.path(ResponseConstant.SERVER_TYPE).textValue());
		model.setExternal_network_id(modelNode.path(ResponseConstant.EXTERNAL_NETWORK_ID).textValue());
		model.setCluster_distro(modelNode.path(ResponseConstant.CLUSTER_DISTRO).textValue());
		model.setImage_id(modelNode.path(ResponseConstant.IMAGE_ID).textValue());
		model.setVolume_driver(modelNode.path(ResponseConstant.VOLUME_DRIVER).textValue());
		model.setRegistry_enabled(modelNode.path(ResponseConstant.REGISTRY_ENABLED).booleanValue());
		model.setDocker_storage_driver(modelNode.path(ResponseConstant.DOCKER_STORAGE_DRIVER).textValue());
		model.setApiserver_port(modelNode.path(ResponseConstant.APISERVER_PORT).textValue());
		model.setName(modelNode.path(ResponseConstant.NAME).textValue());
	//	model.setCreated_at(modelNode.path(ResponseConstant.CREATE_AT).textValue());
		model.setCreated_at(modelNode.path(ResponseConstant.CREATED_AT).textValue());
		model.setNetwork_driver(modelNode.path(ResponseConstant.NETWORK_DRIVER).textValue());
		model.setFixed_network(modelNode.path(ResponseConstant.FIXED_NETWORK).textValue());
        model.setCoe(modelNode.path(ResponseConstant.CORE).textValue());
        model.setFlavor_id(modelNode.path(ResponseConstant.FLAVOR_ID).textValue());
        model.setMaster_lb_enabled(modelNode.path(ResponseConstant.MASTER_LB_ENABLED).booleanValue());
        model.setDns_nameserver(modelNode.path(ResponseConstant.DNS_NAMESERVER).textValue());
		
		return model;
	}
	
	private ContainerModel getBayModel(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		return getBayModelInfo(rootNode);
	}
	
	private List<ContainerModel> getBayModels(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode modelsNode = rootNode.path(ResponseConstant.BAYMODELS);
		int size = modelsNode.size();
		if (0 == size)
			return null;
		 List<ContainerModel> models = new ArrayList<ContainerModel>();
		 
		for(int index = 0; index < size; ++index){
			ContainerModel model = getBayModelInfo(modelsNode.get(index));
			if(null == model)
				continue;
			models.add(model);
		}
		
		return models;
	}
	
	private void saveContainerModelsToDB(List<ContainerModel> models){
		if(Util.isNullOrEmptyList(models))
			return;
		
		for(ContainerModel model : models){
			if(null == containerModelMapper.selectByPrimaryKey(model.getUuid()))
				containerModelMapper.insertSelective(model);
			else
				containerModelMapper.updateByPrimaryKeySelective(model);
		}
	}
	
	private void saveContainersToDB(List<Container> containers){
		if(Util.isNullOrEmptyList(containers))
			return;
		
		for(Container container : containers){
			if(null == containerMapper.selectByPrimaryKey(container.getUuid()))
				containerMapper.insertSelective(container);
			else
				containerMapper.updateByPrimaryKeySelective(container);
		}
	}
	
	private List<Container> getContainersFromDB(String tenantId, int limit) {
		List<Container> containers = null;
		if (-1 == limit) {
			containers = containerMapper.selectAllByTenantId(tenantId);
		} else {
			containers = containerMapper.selectAllByTenantIdWithLimit(tenantId, limit);
		}
		if (Util.isNullOrEmptyList(containers))
			return null;
		return containers;

	}
	
	private void checkResource(String id, Locale locale) throws BusinessException {
		SyncResource syncResource = syncResourceMapper.selectByPrimaryKey(id);
		if (null != syncResource)
			throw new ResourceBusinessException(Message.CS_RESOURCE_IS_DOING,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE, locale);
	}
}
