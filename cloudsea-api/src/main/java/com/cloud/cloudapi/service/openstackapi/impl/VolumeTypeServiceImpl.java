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

import com.cloud.cloudapi.dao.common.VolumeMapper;
import com.cloud.cloudapi.dao.common.VolumeTypeMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeType;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.VolumeTypeService;
import com.cloud.cloudapi.service.rating.RatingTemplateService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("volumeTypeService")  
public class VolumeTypeServiceImpl implements VolumeTypeService{

	@Resource
	private OSHttpClientUtil client;
	
	@Autowired
	private CloudConfig cloudconfig;
	
	@Autowired  
    private VolumeMapper volumeMapper;
	
	@Autowired  
    private VolumeTypeMapper volumeTypeMapper;
	
	@Resource
	private RatingTemplateService ratingService;
	
	@Resource
	private AuthService authService;

	private Logger log = LogManager.getLogger(VolumeTypeServiceImpl.class);
	
	public CloudConfig getCloudconfig() {
		return cloudconfig;
	}

	public void setCloudconfig(CloudConfig cloudconfig) {
		this.cloudconfig = cloudconfig;
	}

	public VolumeTypeMapper getVolumeTypeMapper() {
		return volumeTypeMapper;
	}

	public void setVolumeTypeMapper(VolumeTypeMapper volumeTypeMapper) {
		this.volumeTypeMapper = volumeTypeMapper;
	}

	
	@Override
	public List<VolumeType> getVolumeTypeList(Map<String, String> paraMap, TokenOs ostoken)throws BusinessException{
		List<VolumeType> volumeTypesFromDB = getVolumeTypesFromDB();
		if(!Util.isNullOrEmptyList(volumeTypesFromDB))
			return volumeTypesFromDB;
		Locale locale = new Locale(ostoken.getLocale());
		TokenOs adminToken = null;
		try{
			adminToken = authService.createDefaultAdminOsToken();	
		}catch(Exception e){
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,locale);
		}
		
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		String region = ostoken.getCurrentRegion(); 

		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/types?is_public=None", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
       
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		List<VolumeType> volumeTypes = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				volumeTypes = getVolumeTypes(rs);
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
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,locale);
			try {
				volumeTypes = getVolumeTypes(rs);
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
			throw new ResourceBusinessException(Message.CS_VOLUME_TYPE_GET_FAILED,httpCode,locale);
		}

		storeVolumeTypesToDB(volumeTypes);
		return volumeTypes;
	}
	
	@Override
	public VolumeType getVolumeType(String volumeTypeId, TokenOs ostoken)throws BusinessException{
		VolumeType volumeType = volumeTypeMapper.selectByPrimaryKey(volumeTypeId);
		if(null != volumeType)
			return volumeType;
		String region = ostoken.getCurrentRegion();

		// String url=ot.getEndPoint(TokenOs.EP_TYPE_NETWORK,
		// region).getPublicURL();
		// url=url+"/v2.0/networks/" + NetworkId;
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/types/");
		sb.append(volumeTypeId);
		
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
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				volumeType = getVolumeType(rs);
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
			rs =  client.httpDoGet(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				volumeType = getVolumeType(rs);
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
			throw new ResourceBusinessException(Message.CS_VOLUME_TYPE_DETAIL_GET_FAILED,httpCode,locale);
		}

		storeVolumeTyep2DB(volumeType);
		return volumeType;
	}
	
	private String getVolumeTypeBody(String name, String description, String backend, Boolean create) {
		StringBuilder sb = new StringBuilder();

		Boolean haveValue = false;

		sb.append("{\"volume_type\":{");
		if (null != name) {
			sb.append("\"");
			sb.append(ParamConstant.NAME);
			sb.append("\":\"");
			sb.append(name);
			sb.append("\"");
			haveValue = true;
		}

		if (null != description) {
			if (true == haveValue)
				sb.append(",");
			sb.append("\"");
			sb.append(ParamConstant.DESCRIPTION);
			sb.append("\":\"");
			sb.append(description);
			sb.append("\"");
			haveValue = true;
		}
		
		if (true == create) {
			if (true == haveValue)
				sb.append(",");

			sb.append("\"");
			sb.append(ParamConstant.VOLUME_TYPE_PUBLIC_ACCESS);
			sb.append("\":");
			sb.append(true);
			haveValue = true;
		}
		
		if(null != backend){
			if (true == haveValue)
				sb.append(",");
			sb.append("\"");
			sb.append(ParamConstant.EXTRA_SPECS);
			sb.append("\":{\"");
			sb.append(ParamConstant.VOLUME_BACKEND_NAME);
			sb.append("\":\"");
			sb.append(backend);
			sb.append("\"}");
		}
		
		sb.append("}}");
		return sb.toString();
	}

	@Override
	public VolumeType updateVolumeType(String volumeTypeId, String body,TokenOs ostoken)throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(body);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		
		String name = null;
		if(!rootNode.path(ParamConstant.NAME).isMissingNode())
			name = rootNode.path(ParamConstant.NAME).textValue();
		String displayName = null;
		if(!rootNode.path(ParamConstant.DISPLAYNAME).isMissingNode())
			displayName = rootNode.path(ParamConstant.DISPLAYNAME).textValue();
		String description = null;
		if(!rootNode.path(ParamConstant.DESCRIPTION).isMissingNode())
			description = rootNode.path(ParamConstant.DESCRIPTION).textValue();
		checkName(name, displayName, locale);
		VolumeType volumeType = null;
		if(null != name || null != description){
			String updateBody = getVolumeTypeBody(name,description,null,false);
			
			String region = ostoken.getCurrentRegion();
			String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
			StringBuilder sb = new StringBuilder();
			sb.append(url);
			sb.append("/types/");
			sb.append(volumeTypeId);
			
			HashMap<String, String> headers = new HashMap<String, String>();
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
			Map<String, String> rs = client.httpDoPut(sb.toString(), headers,updateBody);
			Util.checkResponseBody(rs,locale);
			String failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			
			int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			switch (httpCode) {
			case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
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
				failedError = Util.getFailedReason(rs);
				if(!Util.isNullOrEmptyValue(failedError))
					log.error(failedError);
				if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
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
				throw new ResourceBusinessException(Message.CS_VOLUME_TYPE_UPDATE_FAILED,httpCode,locale);
			}
		}
		
		volumeType = volumeTypeMapper.selectByPrimaryKey(volumeTypeId);
		if(null != name)
			volumeType.setName(name);
		if(null != displayName)
			volumeType.setDisplayName(displayName);
		if(null != description)
			volumeType.setDescription(description);
		volumeTypeMapper.insertOrUpdate(volumeType);
		return volumeType;
	}
	
	@Override
	public VolumeType createVolumeType(String createBody,TokenOs ostoken) throws BusinessException {

		Locale locale = new Locale(ostoken.getLocale());
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(createBody);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
        
		String name = rootNode.path(ParamConstant.NAME).textValue();
		String displayName = rootNode.path(ParamConstant.DISPLAYNAME).textValue();
		String description = null;
		if(!rootNode.path(ParamConstant.DESCRIPTION).isMissingNode())
			description = rootNode.path(ParamConstant.DESCRIPTION).textValue();
		checkName(name, displayName, locale);
		
		String backendName = null;
		if(!rootNode.path(ParamConstant.BACKEND).isMissingNode())
			backendName = rootNode.path(ParamConstant.BACKEND).textValue();
		
		String body = getVolumeTypeBody(name,description,backendName,true);
		
		String region = ostoken.getCurrentRegion(); // we should get the regioninfo by the guiTokenId
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/types", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoPost(url, headers,body);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		VolumeType volumeType = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				volumeType = getVolumeType(rs);
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
			rs = client.httpDoPost(url, headers,body);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				volumeType = getVolumeType(rs);
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
			throw new ResourceBusinessException(Message.CS_VOLUME_TYPE_CREATE_FAILED,httpCode,locale);
		}
        
		volumeType.setDisplayName(displayName);
		storeVolumeTyep2DB(volumeType);
		
		ratingService.addRatingPolicy(ParamConstant.STORAGE,volumeType.getName(),ostoken); //add the rating template field

		return volumeType;
	}
	
	@Override
	public void deleteVolumeType(String volumeTypeId, TokenOs ostoken)throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(volumeTypeId,locale);
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/types/");
		sb.append(volumeTypeId);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
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
			rs =  client.httpDoDelete(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE)
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
			throw new ResourceBusinessException(Message.CS_VOLUME_TYPE_DELETE_FAILED,httpCode,locale);
		}

		VolumeType volumeType = volumeTypeMapper.selectByPrimaryKey(volumeTypeId);
		if(null != volumeType){
			volumeTypeMapper.deleteByPrimaryKey(volumeTypeId);
			ratingService.deleteRatingPolicy(ParamConstant.STORAGE, volumeType.getName(), ostoken);//delete the rating field	
		}
		return;
	}
	
	private void checkName(String name,String displayName,Locale locale)  throws BusinessException{
		List<VolumeType> types = volumeTypeMapper.selectAll();
		if(null == types)
			return;
		for(VolumeType type : types){
			if(name != null && name.equals(type.getName()))
				throw new ResourceBusinessException(Message.CS_HAVE_SAME_NAME_EXIST,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
			if(displayName != null && displayName.equals(type.getDisplayName()))
				throw new ResourceBusinessException(Message.CS_HAVE_SAME_VISIBLE_NAME_EXIST,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
	}
		
	private List<VolumeType> getVolumeTypesFromDB(){
		List<VolumeType> volumeTypesFromDB = volumeTypeMapper.selectAll();
		return volumeTypesFromDB;
	}
	
	private List<VolumeType> storeVolumeTypesToDB(List<VolumeType> volumeTypes){
		if(Util.isNullOrEmptyList(volumeTypes))
			return volumeTypes;
		volumeTypeMapper.insertOrUpdateBatch(volumeTypes);
		
//		List<VolumeType> volumeTypesToDB = new ArrayList<VolumeType>();
//		for(VolumeType volumeType : volumeTypes){
//			storeVolumeTyep2DB(volumeType);
//		}
		return volumeTypes;
	}
	
	private void storeVolumeTyep2DB(VolumeType volumeType){
		if(null == volumeType)
			return;
		volumeTypeMapper.insertOrUpdate(volumeType);
		
//		if(null != volumeTypeMapper.selectByPrimaryKey(volumeType.getId()))
//			volumeTypeMapper.updateByPrimaryKeySelective(volumeType);
//		else
//			volumeTypeMapper.insertSelective(volumeType);
	}
	
	private Double getVolumeTypePrice(String volumeType){
		return Double.valueOf(cloudconfig.getVolumePrice());
		/*
		if(Util.isNullOrEmptyValue(volumeType))
			return Double.valueOf(cloudconfig.getVolumePrice()); 
		String[] volumeTypes = cloudconfig.getSystemVolumeSpec().split(",");
		String[] volumePrice = cloudconfig.getVolumePrice();

		if (volumeTypes.length != volumePrices.length)
			return Double.valueOf(cloudconfig.getVolumePrice()); 
		for (int index = 0; index < volumeTypes.length; ++index) {
			if(volumeTypes[index].equals(volumeType))
			  return Double.valueOf(volumePrices[index]);
		}
		return Double.valueOf(cloudconfig.getVolumePrice());
		*/ 
	}
	
	private VolumeType getVolumeTypeInfo(JsonNode volumeTypeNode){
		if(null == volumeTypeNode)
			return null;
		VolumeType volumeTypeInfo = new VolumeType();
		volumeTypeInfo.setId(volumeTypeNode.path(ResponseConstant.ID).textValue());
		volumeTypeInfo.setName(volumeTypeNode.path(ResponseConstant.NAME).textValue());
		volumeTypeInfo.setDisplayName(volumeTypeInfo.getName());
		volumeTypeInfo.setDescription(volumeTypeNode.path(ResponseConstant.DESCRIPTION).textValue());
		volumeTypeInfo.setIs_public(volumeTypeNode.path(ResponseConstant.IS_PUBLIC).booleanValue());
		volumeTypeInfo.setUnitPrice(getVolumeTypePrice(volumeTypeInfo.getName()));
		
		JsonNode extraSpecNode = volumeTypeNode.path(ResponseConstant.EXTRA_SPECS);
		if(null != extraSpecNode && !extraSpecNode.isMissingNode()){
			String backendName = extraSpecNode.path(ResponseConstant.VOLUME_BACKEND_NAME).textValue(); 
			volumeTypeInfo.setBackendName(backendName);
//			if(!Util.isNullOrEmptyValue(backendName)){
//				List<String> extraSpecs = new ArrayList<String>();
//				extraSpecs.add(backendName);
//				volumeTypeInfo.setExtra_Specs(extraSpecs);	
//			}
		}
		
		return volumeTypeInfo;
	}
	
	private VolumeType getVolumeType(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode volumeTypeNode = rootNode.path(ResponseConstant.VOLUME_TYPE);
		return getVolumeTypeInfo(volumeTypeNode);
	}
	
	private List<VolumeType> getVolumeTypes(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode volumeTypesNode = rootNode.path(ResponseConstant.VOLUME_TYPES);
		int volumesTypeCount = volumeTypesNode.size();
		if (0 == volumesTypeCount)
			return null;
		List<VolumeType> volumeTypes = new ArrayList<VolumeType>();
		for (int index = 0; index < volumesTypeCount; ++index) {
			VolumeType volumeType = getVolumeTypeInfo(volumeTypesNode.get(index));
			if(null == volumeType)
				continue;
			volumeTypes.add(volumeType);
		}
		return volumeTypes;
	}
	
	private void checkResource(String id,Locale locale)  throws BusinessException {
		VolumeType volumeType = volumeTypeMapper.selectByPrimaryKey(id);
		if(null == volumeType)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
        if(0 != volumeMapper.countNumByVolumeType(volumeType.getName()))
			throw new ResourceBusinessException(Message.CS_RESORCE_IS_USED,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
        return;
	}
}
