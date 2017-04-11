package com.cloud.cloudapi.controller.openstackapi;

import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloud.cloudapi.controller.common.BaseController;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeType;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.openstackapi.VolumeTypeService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class VolumeTypeController extends BaseController {
	
	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private VolumeTypeService volumeTypeService;

	
	private Logger log = LogManager.getLogger(VolumeTypeController.class);
	
	@RequestMapping(value = "/volumes-type", method = RequestMethod.GET)
	public String getVolumesType(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name, HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			
			Map<String, String> paramMap = new HashMap<String, String>();
			paramMap.put(ParamConstant.NAME, name);

			List<VolumeType> volumeTypes = volumeTypeService.getVolumeTypeList(paramMap, authToken);
			if(Util.isNullOrEmptyList(volumeTypes)){
		//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE,ParamConstant.VOLUMETYPE,"",Message.SUCCESSED_FLAG,"");
				JsonHelper<List<VolumeType>, String> jsonHelp = new JsonHelper<List<VolumeType>, String>();
				return jsonHelp.generateJsonBodySimple(new ArrayList<VolumeType>());
			}
			normalInfo(volumeTypes);
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE,ParamConstant.VOLUMETYPE,getVolumeTypesId(volumeTypes),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<VolumeType>, String> jsonHelp = new JsonHelper<List<VolumeType>, String>();
			return jsonHelp.generateJsonBodySimple(volumeTypes);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
		//	this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.GET_VOLUME_TYPE,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
		//	this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_VOLUME_TYPE_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
		//	this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/volumes-type/{id}", method = RequestMethod.GET)
	public String getVolumeType(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			
			VolumeType volumeType = volumeTypeService.getVolumeType(id, authToken);
			if(null == volumeType){
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
		//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE_DETAIL,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
				return message;
			}
			volumeType.normalInfo();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE_DETAIL,ParamConstant.VOLUMETYPE,volumeType.getId(),Message.SUCCESSED_FLAG,"");
			JsonHelper<VolumeType, String> jsonHelp = new JsonHelper<VolumeType, String>();
			return jsonHelp.generateJsonBodyWithEmpty(volumeType);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE_DETAIL,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE_DETAIL,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_VOLUME_TYPE_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE_DETAIL,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
		
	}

	@RequestMapping(value = "/volumes-type/{id}", method = RequestMethod.DELETE)
	public String deleteVolumeType(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.VOLUMETYPE_NEW);
			volumeTypeService.deleteVolumeType(id, authToken);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_VOLUME_TYPE,ParamConstant.VOLUMETYPE,id,Message.SUCCESSED_FLAG,"");
			JsonHelper<VolumeType, String> jsonHelp = new JsonHelper<VolumeType, String>();
			return jsonHelp.generateJsonBodyWithEmpty(new VolumeType());
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE_DETAIL,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE_DETAIL,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_VOLUME_TYPE_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE_DETAIL,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/volumes-type/{id}", method = RequestMethod.PUT)
	public String updateVolumeType(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String volumeTypeBody,HttpServletResponse response) {
		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.VOLUMETYPE_UPDATE);
			VolumeType volumeType = volumeTypeService.updateVolumeType(id, volumeTypeBody, authToken);
			volumeType.normalInfo();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_VOLUME_TYPE,ParamConstant.VOLUMETYPE,id,Message.SUCCESSED_FLAG,"");
			JsonHelper<VolumeType, String> jsonHelp = new JsonHelper<VolumeType, String>();
			return jsonHelp.generateJsonBodyWithEmpty(volumeType);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE_DETAIL,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE_DETAIL,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_VOLUME_TYPE_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE_DETAIL,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/volumes-type", method = RequestMethod.POST)
	public String createVolumeType(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String volumeTypeBody, HttpServletResponse response) {
		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			
			this.checkUserPermission(authToken, ParamConstant.VOLUMETYPE_NEW);
			VolumeType volumeType = volumeTypeService.createVolumeType(volumeTypeBody, authToken);
			if(null == volumeType){
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
	//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.CREATE_VOLUME_TYPE,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.CREATE_VOLUME_TYPE,ParamConstant.VOLUMETYPE,volumeType.getId(),Message.SUCCESSED_FLAG,"");
			JsonHelper<VolumeType, String> jsonHelp = new JsonHelper<VolumeType, String>();
			return jsonHelp.generateJsonBodyWithEmpty(volumeType);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VOLUME_TYPE,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
		//	this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VOLUME_TYPE,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_VOLUME_TYPE_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
		//	this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VOLUME_TYPE,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}

	private void normalInfo(List<VolumeType> volumeTypes){
		if(Util.isNullOrEmptyList(volumeTypes))
			return;
		for(VolumeType volumeType : volumeTypes)
			volumeType.normalInfo();
	}
}
