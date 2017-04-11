package com.cloud.cloudapi.controller.common;

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
import org.springframework.web.bind.annotation.RestController;

import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudRole;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.service.common.CloudRoleService;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class CloudRoleController  extends BaseController {

	@Resource
	private CloudRoleService cloudRoleService; 
 
	@Resource
	private OperationLogService operationLogService;
	
	private Logger log = LogManager.getLogger(CloudRoleController.class);
	
	@RequestMapping(value = "/roles", method = RequestMethod.GET)
	public String getRoles(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestHeader(value = ParamConstant.TENANT_ID, defaultValue = "") String tenantId,
			@RequestHeader(value = ParamConstant.BILLING_MONTH_UNTIL, defaultValue = "") String billingMonthUntil,
			HttpServletResponse response) {
		
		TokenOs authToken=null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<CloudRole> roles = cloudRoleService.getRoles(authToken);
			normalInfo(roles);
			JsonHelper<List<CloudRole>, String> jsonHelp = new JsonHelper<List<CloudRole>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(roles);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_ROLE_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	
	@RequestMapping(value = "/roles/{name}", method = RequestMethod.GET)
	public String getRoles(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String name,HttpServletResponse response) {
		
		TokenOs authToken=null;

		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<CloudRole> roles = cloudRoleService.getUserRoles(authToken,name);
			normalInfo(roles);
			JsonHelper<List<CloudRole>, String> jsonHelp = new JsonHelper<List<CloudRole>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(roles);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_ROLE_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/roles/{id}", method = RequestMethod.PUT)
	public String updateRole(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id,@RequestBody String body,HttpServletResponse response) {
		
		TokenOs authToken=null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.ROLE_UPDATE);
			CloudRole role = cloudRoleService.updateRole(authToken,id,body);
			role.normalInfo();
			JsonHelper<CloudRole, String> jsonHelp = new JsonHelper<CloudRole, String>();
			return jsonHelp.generateJsonBodyWithEmpty(role);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_ROLE_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
//	@RequestMapping(value = "/add-users/{id}", method = RequestMethod.PUT)
//	public String addRole(
//			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
//			@PathVariable String id, @RequestBody String body, HttpServletResponse response) {
//		
//		TokenOs authToken=null;
//		try {
//			authToken = this.getUserOsToken(guiToken);
//			if(null == authToken){
//				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
//				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
//				return exception.getResponseMessage();
//			}
//			this.checkUserPermission(authToken, ParamConstant.ROLE_BIND_USER);
//			cloudRoleService.addUsers(id, body, authToken);
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.ADD_ROLE,ParamConstant.ROLE,id,Message.SUCCESSED_FLAG,"");
//			ResourceBusinessException message = new ResourceBusinessException(Message.CS_ROLE_ADD_SUCCESSED,new Locale(authToken.getLocale()));
//			return message.getResponseMessage();
//		} catch (ResourceBusinessException e) {
//			log.error(e.getResponseMessage(),e);
//			response.setStatus(e.getStatusCode());
//			String message = e.getResponseMessage();
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.ADD_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
//			return message;
//		} catch (MyBatisSystemException e) {
//			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
//			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
//			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.ADD_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
//			log.error(message,e);
//			return message;
//		} catch (Exception e) {
//			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
//			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_ROLE_ADD_FAILED,new Locale(authToken.getLocale()));
//			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.ADD_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
//			log.error(message,e);
//			return message;
//		}
//	}
	
	@RequestMapping(value = "/roles", method = RequestMethod.POST)
	public String createRole(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {
		
		TokenOs authToken=null;	
        try{
        	authToken = this.getUserOsToken(guiToken);
        	if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
        	this.checkUserPermission(authToken, ParamConstant.ROLE_NEW);
        	CloudRole role = cloudRoleService.createRoles(createBody, authToken);
        	role.normalInfo();
			JsonHelper<CloudRole, String> jsonHelp = new JsonHelper<CloudRole, String>();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_ROLE,ParamConstant.ROLE,role.getId(),Message.SUCCESSED_FLAG,"");
			return jsonHelp.generateJsonBodyWithEmpty(role);
        } catch (ResourceBusinessException e) {
        	log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_ROLE_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.CREATE_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/roles/{roleId}", method = RequestMethod.DELETE)
	public String deleteRole(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String roleId, HttpServletResponse response) {
		
		TokenOs authToken=null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.ROLE_NEW);
			cloudRoleService.deleteRole(roleId, authToken);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_ROLE,ParamConstant.ROLE,roleId,Message.SUCCESSED_FLAG,"");
			JsonHelper<CloudRole, String> jsonHelp = new JsonHelper<CloudRole, String>();
			return jsonHelp.generateJsonBodyWithEmpty(new CloudRole());
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_ROLE_ADD_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	private void normalInfo(List<CloudRole> roles){
		if(Util.isNullOrEmptyList(roles))
			return;
		for(CloudRole role : roles)
			role.normalInfo();
	}
}
