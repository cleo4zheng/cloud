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

import com.cloud.cloudapi.dao.common.ImageMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Image;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.SnapshotService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SnapshotServiceImpl  implements SnapshotService {
	
	@Resource
	private CloudConfig cloudconfig;
	
	@Resource
	private ImageMapper imageMapper;
	
	@Resource
	private OSHttpClientUtil oSHttpClientUtil;
	
	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(SnapshotServiceImpl.class);
	
	private List<Image> getSnapshotsFromDB(String tenantId){
		List<Image> snapshots = imageMapper.selectPrivateImages(tenantId, true);
		return snapshots;
	}
	
	private List<Image> storeImages2DB(List<Image> images){
		if(Util.isNullOrEmptyList(images))
			return null;
		
		List<Image> imagesToResponse = new ArrayList<Image>();
		for(Image image : images){
			storeImage2DB(image);
		//	image.setSize(Util.byte2Mega(image.getSize()));
			imagesToResponse.add(image);
		}	
		return imagesToResponse;
	}
	
	private void storeImage2DB(Image image){
		if(null == image)
			return;
		if(null != imageMapper.selectByPrimaryKey(image.getId()))
		    imageMapper.updateByPrimaryKeySelective(image);
		else
			imageMapper.insertSelective(image);
		image.setSize(Util.byte2Mega(image.getSize()));
	}
	
	@Override
	public List<Image> getSnapshotList(Map<String, String> paramMap, TokenOs ostoken) throws BusinessException {
		
		List<Image> snapshots = getSnapshotsFromDB(ostoken.getTenantid());
		if(!Util.isNullOrEmptyList(snapshots))
			return snapshots;

		String region = ostoken.getCurrentRegion();
		
		String url=ostoken.getEndPoint(TokenOs.EP_TYPE_IMAGE, region).getPublicURL();
		if(paramMap==null)
			paramMap = new HashMap<String,String>();
	    paramMap.put("image_type",ParamConstant.SNAPSHOT_TYPE_IMAGE );
		url=RequestUrlHelper.createFullUrl(url+"/v2/images", paramMap);
		HashMap<String, String> headers = new HashMap<String, String>();
		//headers.put("X-Auth-Token" ,ot.getTokenid());
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN ,ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String>  rs = oSHttpClientUtil.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		List<Image> images = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				images = getImages(rs);
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
			rs = oSHttpClientUtil.httpDoGet(url, headers);
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				images = getImages(rs);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_SNAPSHOT_GET_FAILED,httpCode,locale);
		}
		return storeImages2DB(images);
	}
	
	@Override
	public Image getSnapshot(String imageId, TokenOs ostoken) throws BusinessException {
		Image image = imageMapper.selectByPrimaryKey(imageId);
		if(null != image)
			return image;

		
		String region = ostoken.getCurrentRegion();
		
		String url=ostoken.getEndPoint(TokenOs.EP_TYPE_IMAGE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2/images/");
		sb.append(imageId);
		
		url=RequestUrlHelper.createFullUrl(sb.toString(), null);
		HashMap<String, String> headers = new HashMap<String, String>();
		//headers.put("X-Auth-Token" ,ot.getTokenid());
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN ,ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String>  rs = oSHttpClientUtil.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode imageNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				image = getImageInfo(imageNode);
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
			rs = oSHttpClientUtil.httpDoGet(url, headers);
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode imageNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				image = getImageInfo(imageNode);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_SNAPSHOT_DETAIL_GET_FAILED,httpCode,locale);
		}
		
		storeImage2DB(image);
		return image;
	}
	
	private List<Image> getImages(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode imagesNode = rootNode.path(ResponseConstant.IMAGES);
		int imagesCount = imagesNode.size();
		if (0 == imagesCount)
			return null;
		List<Image> images = new ArrayList<Image>();
		for (int index = 0; index < imagesCount; ++index) {
			Image image = getImageInfo(imagesNode.get(index));
			images.add(image);
		}
		return images;
	}
	
	private Image getImageInfo(JsonNode imageNode){
		if(null == imageNode)
			return null;
		Image image = new Image();
		image.setId(imageNode.path(ResponseConstant.ID).textValue());
		image.setName(imageNode.path(ResponseConstant.NAME).textValue());
		image.setStatus(imageNode.path(ResponseConstant.STATUS).textValue());
		image.setSize(imageNode.path(ResponseConstant.SIZE).longValue());
		image.setCreatedAt(imageNode.path(ResponseConstant.CREATED_AT).textValue());
		//image.setCreatedAt(DateHelper.getDateByString(oneSnapshot.path(ResponseConstant.CREATED_AT).textValue()).toString());
		
		return image;
	}

}
