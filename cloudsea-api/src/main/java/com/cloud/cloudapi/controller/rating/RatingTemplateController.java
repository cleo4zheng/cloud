package com.cloud.cloudapi.controller.rating;

import java.util.List;
import java.util.Locale;

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
import com.cloud.cloudapi.pojo.rating.TemplateField;
import com.cloud.cloudapi.pojo.rating.TemplateService;
import com.cloud.cloudapi.pojo.rating.TemplateTenantMapping;
import com.cloud.cloudapi.pojo.rating.TemplateVersion;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.rating.RatingTemplateService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class RatingTemplateController  extends BaseController {
	
	@Resource
	private RatingTemplateService templateService;
	
	@Resource
	private OperationLogService operationLogService;

	private Logger log = LogManager.getLogger(RatingTemplateController.class);
	
	@RequestMapping(value = "/rating-versions", method = RequestMethod.GET)
	public String getRatingVersions(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			HttpServletResponse response) {
		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<TemplateVersion> versions = templateService.getRatingVersions(null, authToken);
			JsonHelper<List<TemplateVersion>, String> jsonHelp = new JsonHelper<List<TemplateVersion>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(versions);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_VERSION, ParamConstant.RATING_VERSION, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_VERSION, ParamConstant.RATING_VERSION, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_RATING_VERSION_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_VERSION, ParamConstant.RATING_VERSION, "",
					Message.FAILED_FLAG, message);
			return message;
		}
	}

	@RequestMapping(value = "/rating-versions/{id}", method = RequestMethod.GET)
	public String getRatingVersion(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			TemplateVersion version = templateService.getRatingVersion(id, authToken);
			JsonHelper<TemplateVersion, String> jsonHelp = new JsonHelper<TemplateVersion, String>();
			return jsonHelp.generateJsonBodyWithEmpty(version);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_VERSION_DETAIL, ParamConstant.RATING_VERSION, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_VERSION_DETAIL, ParamConstant.RATING_VERSION, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_RATING_VERSION_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_VERSION_DETAIL, ParamConstant.RATING_VERSION, "",
					Message.FAILED_FLAG, message);
			return message;
		}
	}

	@RequestMapping(value = "/rating-versions/{id}", method = RequestMethod.PUT)
	public String updateRatingVersion(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String body, HttpServletResponse response) {
		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			TemplateVersion version = templateService.updateRatingVersion(id, body, authToken);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_RATING_VERSION_UPDATE_SUCCESSED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_RATING_VERSION, ParamConstant.RATING_VERSION, id, Message.SUCCESSED_FLAG,
					exception.getResponseMessage());
			JsonHelper<TemplateVersion, String> jsonHelp = new JsonHelper<TemplateVersion, String>();
			return jsonHelp.generateJsonBodyWithEmpty(version);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_RATING_VERSION, ParamConstant.RATING_VERSION, "", Message.FAILED_FLAG,
					message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_RATING_VERSION, ParamConstant.RATING_VERSION, "", Message.FAILED_FLAG,
					message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_RATING_VERSION_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_RATING_VERSION, ParamConstant.RATING_VERSION, "", Message.FAILED_FLAG,
					message);
			return message;
		}
	}
	
	@RequestMapping(value = "/rating-versions/{id}", method = RequestMethod.DELETE)
	public String deleteRatingVersion(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			templateService.deleteRatingVersion(id, authToken);
			if (null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_RATING_VERSION_DELETE_SUCCESSED);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_RATING_VERSION, ParamConstant.RATING_VERSION, id,
					Message.SUCCESSED_FLAG, exception.getResponseMessage());
			return exception.getResponseMessage();
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_RATING_VERSION, ParamConstant.RATING_VERSION, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_RATING_VERSION, ParamConstant.RATING_VERSION, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_RATING_VERSION_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_RATING_VERSION, ParamConstant.RATING_VERSION, "",
					Message.FAILED_FLAG, message);
			return message;
		}
	}
	
	@RequestMapping(value = "/rating-tenants", method = RequestMethod.GET)
	public String getTenantRatings(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<TemplateTenantMapping> ratings = templateService.getTenantRatings(null, authToken);
			JsonHelper<List<TemplateTenantMapping>, String> jsonHelp = new JsonHelper<List<TemplateTenantMapping>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(ratings);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_TENANT_RATING, ParamConstant.TENANT_RATING, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_TENANT_RATING, ParamConstant.TENANT_RATING, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_TENANT_RATING_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_TENANT_RATING, ParamConstant.TENANT_RATING, "",
					Message.FAILED_FLAG, message);
			return message;
		}
	}

	@RequestMapping(value = "/rating-tenants/{id}", method = RequestMethod.GET)
	public String getTenantRating(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		
		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			TemplateTenantMapping rating = templateService.getTenantRating(id, authToken);
			JsonHelper<TemplateTenantMapping, String> jsonHelp = new JsonHelper<TemplateTenantMapping, String>();
			return jsonHelp.generateJsonBodyWithEmpty(rating);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_TENANT_RATING_DETAIL, ParamConstant.TENANT_RATING, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_TENANT_RATING_DETAIL, ParamConstant.TENANT_RATING, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_TENANT_RATING_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_TENANT_RATING_DETAIL, ParamConstant.TENANT_RATING, "",
					Message.FAILED_FLAG, message);
			return message;
		}
	}

	@RequestMapping(value = "/rating-tenants", method = RequestMethod.POST)
	public String createTenantRating(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.PRICE_APPLY);
			TemplateTenantMapping rating = templateService.addTenantRating(createBody, authToken,true);
			if (null == rating) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
						authToken.getTenantid(), Message.CREATE_TENANT_RATING, ParamConstant.TENANT_RATING, "",
						Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_TENANT_RATING, ParamConstant.TENANT_RATING,
					rating.getTenant_id(), Message.SUCCESSED_FLAG, "");
			JsonHelper<TemplateTenantMapping, String> jsonHelp = new JsonHelper<TemplateTenantMapping, String>();
			return jsonHelp.generateJsonBodyWithEmpty(rating);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_TENANT_RATING, ParamConstant.TENANT_RATING, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_TENANT_RATING, ParamConstant.TENANT_RATING, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_TENANT_RATING_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_TENANT_RATING, ParamConstant.TENANT_RATING, "",
					Message.FAILED_FLAG, message);
			return message;
		}

	}
	
	@RequestMapping(value = "/rating-tenants/{id}", method = RequestMethod.PUT)
	public String updateTenantRating(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String body, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			TemplateTenantMapping rating = templateService.updateTenantRating(id, body, authToken);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_TENANT_RATING_UPDATE_SUCCESSED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_TENANT_RATING, ParamConstant.TENANT_RATING, id, Message.SUCCESSED_FLAG,
					exception.getResponseMessage());
			JsonHelper<TemplateTenantMapping, String> jsonHelp = new JsonHelper<TemplateTenantMapping, String>();
			return jsonHelp.generateJsonBodyWithEmpty(rating);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_TENANT_RATING, ParamConstant.TENANT_RATING, "", Message.FAILED_FLAG,
					message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_TENANT_RATING, ParamConstant.TENANT_RATING, "", Message.FAILED_FLAG,
					message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_TENANT_RATING_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_TENANT_RATING, ParamConstant.TENANT_RATING, "", Message.FAILED_FLAG,
					message);
			return message;
		}
	}
	
	@RequestMapping(value = "/rating-tenants/{id}", method = RequestMethod.DELETE)
	public String deleteTenantRating(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			templateService.deleteTenantRating(id, authToken);
			if (null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_TENANT_RATING_DELETE_SUCCESSED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_TENANT_RATING, ParamConstant.TENANT_RATING, id,
					Message.SUCCESSED_FLAG, exception.getResponseMessage());
			return exception.getResponseMessage();
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_TENANT_RATING, ParamConstant.TENANT_RATING, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_TENANT_RATING, ParamConstant.TENANT_RATING, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_TENANT_RATING_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_TENANT_RATING, ParamConstant.TENANT_RATING, "",
					Message.FAILED_FLAG, message);
			return message;
		}
	}
	
	
	@RequestMapping(value = "/rating-services", method = RequestMethod.GET)
	public String getRatingServices(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<TemplateService> service = templateService.getRatingServices(null, authToken);
			JsonHelper<List<TemplateService>, String> jsonHelp = new JsonHelper<List<TemplateService>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(service);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_SERVICE, ParamConstant.RATING_SERVICE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_SERVICE, ParamConstant.RATING_SERVICE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_RATING_SERVICE_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_SERVICE, ParamConstant.RATING_SERVICE, "",
					Message.FAILED_FLAG, message);
			return message;
		}
	}
	
	@RequestMapping(value = "/rating-services/{id}", method = RequestMethod.GET)
	public String getRatingService(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			TemplateService service = templateService.getRatingService(id, authToken);
			JsonHelper<TemplateService, String> jsonHelp = new JsonHelper<TemplateService, String>();
			return jsonHelp.generateJsonBodyWithEmpty(service);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_SERVICE_DETAIL, ParamConstant.RATING_SERVICE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_SERVICE_DETAIL, ParamConstant.RATING_SERVICE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_RATING_SERVICE_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_SERVICE_DETAIL, ParamConstant.RATING_SERVICE, "",
					Message.FAILED_FLAG, message);
			return message;
		}
	}
	
	@RequestMapping(value = "/rating-services", method = RequestMethod.POST)
	public String createRatingService(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			TemplateService service = templateService.addRatingService(createBody, authToken);
			if (null == service) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
						authToken.getTenantid(), Message.CREATE_RATING_SERVICE, ParamConstant.RATING_SERVICE, "",
						Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_RATING_SERVICE, ParamConstant.RATING_SERVICE,
					service.getService_id(), Message.SUCCESSED_FLAG, "");
			JsonHelper<TemplateService, String> jsonHelp = new JsonHelper<TemplateService, String>();
			return jsonHelp.generateJsonBodyWithEmpty(service);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_RATING_SERVICE, ParamConstant.RATING_SERVICE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_RATING_SERVICE, ParamConstant.RATING_SERVICE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_RATING_SERVICE_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_RATING_SERVICE, ParamConstant.RATING_SERVICE, "",
					Message.FAILED_FLAG, message);
			return message;
		}

	}
	
	@RequestMapping(value = "/rating-policies/{id}/{fieldId}", method = RequestMethod.GET)
	public String getRatingPolicy(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @PathVariable String fieldId, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			TemplateField policy = templateService.getRatingPolicy(id, fieldId, authToken);
			JsonHelper<TemplateField, String> jsonHelp = new JsonHelper<TemplateField, String>();
			return jsonHelp.generateJsonBodyWithEmpty(policy);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_POLICY_DETAIL, ParamConstant.RATING_POLICY, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_POLICY_DETAIL, ParamConstant.RATING_POLICY, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_RATING_SERVICE_FIELD_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_POLICY_DETAIL, ParamConstant.RATING_POLICY, "",
					Message.FAILED_FLAG, message);
			return message;
		}
	}
	
	@RequestMapping(value = "/rating-policies/{id}", method = RequestMethod.POST)
	public String createRatingPolicy(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id,@RequestBody String createBody, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			TemplateField policy = templateService.addRatingPolicy(id, createBody, authToken);
			if (null == policy) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
						authToken.getTenantid(), Message.CREATE_RATING_POLICY, ParamConstant.RATING_POLICY, "",
						Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_RATING_SERVICE, ParamConstant.RATING_POLICY,
					policy.getField_id(), Message.SUCCESSED_FLAG, "");
			JsonHelper<TemplateField, String> jsonHelp = new JsonHelper<TemplateField, String>();
			return jsonHelp.generateJsonBodyWithEmpty(policy);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_RATING_POLICY, ParamConstant.RATING_POLICY, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_RATING_POLICY, ParamConstant.RATING_POLICY, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_RATING_SERVICE_FIELD_ADD_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_RATING_POLICY, ParamConstant.RATING_POLICY, "",
					Message.FAILED_FLAG, message);
			return message;
		}
	}
	
	@RequestMapping(value = "/rating-init", method = RequestMethod.POST)
	public String initRatingTemplate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {
		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			templateService.initRatingTemplate(createBody, authToken);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_ENV_INIT_SUCCESSED,new Locale(authToken.getLocale()));
			return exception.getResponseMessage();
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.INIT_RATING, ParamConstant.RATING_TEMPLATE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.INIT_RATING, ParamConstant.RATING_TEMPLATE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_ENV_INIT_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.INIT_RATING, ParamConstant.RATING_TEMPLATE, "",
					Message.FAILED_FLAG, message);
			return message;
		}

	}
}
