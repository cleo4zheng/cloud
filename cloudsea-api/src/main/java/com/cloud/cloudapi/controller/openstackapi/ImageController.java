package com.cloud.cloudapi.controller.openstackapi;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloud.cloudapi.controller.common.BaseController;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Image;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeType;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.openstackapi.ImageService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class ImageController  extends BaseController {
	
	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private ImageService imageService;

	private Logger log = LogManager.getLogger(ImageController.class);
	
	@RequestMapping(value = "/images", method = RequestMethod.GET)
	public String getImages(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			@RequestParam(value = ParamConstant.VISIBILITY, defaultValue = "") String visibility,
			HttpServletResponse response) {

		TokenOs authToken=null;
        try{
        	authToken = this.getUserOsToken(guiToken);
        	if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
    		Map<String, String> paramMap = null;
    		if (!"".equals(limit)) {
    			paramMap = new HashMap<String, String>();
    			paramMap.put(ParamConstant.LIMIT, limit);
    		}
    		if (!"".equals(name)) {
    			if (null == paramMap)
    				paramMap = new HashMap<String, String>();
    			paramMap.put(ParamConstant.OWNER, name);
    		}
    		if (!"".equals(status)) {
    			if (null == paramMap)
    				paramMap = new HashMap<String, String>();
    			paramMap.put(ParamConstant.STATUS, status);
    		}
    		if (!"".equals(visibility)) {
    			if (null == paramMap) {
    				paramMap = new HashMap<String, String>();
    				paramMap.put(ParamConstant.VISIBILITY, visibility);
    			}
    		}
    		Map<String,List<Image>> images = imageService.getImageList(paramMap, authToken);
    		normalImages(images);
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_IMAGE,ParamConstant.IMAGE,getImagesId(images),Message.SUCCESSED_FLAG,"");
			JsonHelper<Map<String,List<Image>>, String> jsonHelp = new JsonHelper<Map<String,List<Image>>, String>();
    		return jsonHelp.generateJsonBodyWithEmpty(images);
    		
        } catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_IMAGE,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_IMAGE,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_IMAGE_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_IMAGE,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/private-images", method = RequestMethod.GET)
	public String getPrivateImages(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			@RequestParam(value = ParamConstant.VISIBILITY, defaultValue = "") String visibility,
			HttpServletResponse response) {
		TokenOs authToken=null;
        try{
        	authToken = this.getUserOsToken(guiToken);
        	if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
    		Map<String, String> paramMap = null;
    		if (!"".equals(limit)) {
    			paramMap = new HashMap<String, String>();
    			paramMap.put(ParamConstant.LIMIT, limit);
    		}
    		if (!"".equals(name)) {
    			if (null == paramMap)
    				paramMap = new HashMap<String, String>();
    			paramMap.put(ParamConstant.OWNER, name);
    		}
    		if (!"".equals(status)) {
    			if (null == paramMap)
    				paramMap = new HashMap<String, String>();
    			paramMap.put(ParamConstant.STATUS, status);
    		}
    		if (!"".equals(visibility)) {
    			if (null == paramMap) {
    				paramMap = new HashMap<String, String>();
    				paramMap.put(ParamConstant.VISIBILITY, visibility);
    			}
    		}
    		Map<String,List<Image>> images = imageService.getPrivateImages(paramMap, authToken);
    		normalImages(images);
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_IMAGE,ParamConstant.IMAGE,getImagesId(images),Message.SUCCESSED_FLAG,"");
			JsonHelper<Map<String,List<Image>>, String> jsonHelp = new JsonHelper<Map<String,List<Image>>, String>();
    		return jsonHelp.generateJsonBodyWithEmpty(images);
    		
        } catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_IMAGE,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_PRIVATE_IMAGE_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_IMAGE,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/images/volumeType/{id}", method = RequestMethod.GET)
	public String getImageVolumeType(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs authToken=null;
		try {
        	authToken = this.getUserOsToken(guiToken);
        	if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			VolumeType type = imageService.getImageVolumeType(id, authToken);
			type.setBackendName(null);
			type.setDescription(null);
			type.setDisplayName(null);
			type.normalInfo();
			JsonHelper<VolumeType, String> jsonHelp = new JsonHelper<VolumeType, String>();
	        return jsonHelp.generateJsonBodyWithEmpty(type);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message =  e.getResponseMessage();
			log.error(message,e);
    		return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message =  exception.getResponseMessage();
			log.error(message,e);
    		return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_IMAGE_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message =  exception.getResponseMessage();
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/images/{id}", method = RequestMethod.GET)
	public String getImage(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs authToken=null;
		try {
        	authToken = this.getUserOsToken(guiToken);
        	if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Image image = imageService.getImage(id, authToken);
			if(null == image){
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message =  exception.getResponseMessage();
		//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_IMAGE_DETAIL,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
	    		return message;
			}
			image.setSize(Util.byte2Mega(image.getSize()));
			image.normalInfo();
		//	this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_IMAGE_DETAIL,ParamConstant.IMAGE,image.getId(),Message.SUCCESSED_FLAG,"");
			JsonHelper<Image, String> jsonHelp = new JsonHelper<Image, String>();
    		return jsonHelp.generateJsonBodyWithEmpty(image);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message =  e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_IMAGE_DETAIL,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message =  exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_IMAGE_DETAIL,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_IMAGE_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message =  exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_IMAGE_DETAIL,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/images/{id}", method = RequestMethod.DELETE)
	public String deleteImage(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs authToken=null;
		try {
        	authToken = this.getUserOsToken(guiToken);
        	if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
        	this.checkUserPermission(authToken, ParamConstant.IMAGE_DELETE);
			imageService.deleteImage(id, authToken);
			if(null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_SNAPSHOT,ParamConstant.IMAGE,id,Message.SUCCESSED_FLAG,"");
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_IMAGE_DELETE_SUCCESSED,new Locale(authToken.getLocale()));
			return exception.getResponseMessage();
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message =  e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_SNAPSHOT,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message =  exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_SNAPSHOT,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_IMAGE_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message =  exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_SNAPSHOT,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	private void normalImages(Map<String, List<Image>> allimages) {
		if (null == allimages)
			return;
		for (Map.Entry<String, List<Image>> entry : allimages.entrySet()) {
			List<Image> images = entry.getValue();
			normalImagesInfo(images);
		}
	}
	
	private void normalImagesInfo(List<Image> images){
	    if(Util.isNullOrEmptyList(images))
	    	return;
		for(Image image : images){
			image.normalInfo();
		}
	}
}
