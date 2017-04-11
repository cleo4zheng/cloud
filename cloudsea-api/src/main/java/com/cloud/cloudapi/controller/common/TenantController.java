package com.cloud.cloudapi.controller.common;

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

import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenGui;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Tenant;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.openstackapi.TenantService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class TenantController  extends BaseController {
	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private TenantService tenantService;

	
	private Logger log = LogManager.getLogger(TenantController.class);
	
	@RequestMapping(value = "/tenants", method = RequestMethod.GET)
	public String getTenantList(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name, HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
	//		this.getAuthService().checkIsAdmin(authToken);
			Map<String, String> paramMap = null;

			if (!"".equals(name)) {
				if (null == paramMap)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.NAME, name);
			}
			List<Tenant> tenants = tenantService.getTenantList(paramMap, authToken);
			if (null == tenants) {
				JsonHelper<List<Tenant>, String> jsonHelp = new JsonHelper<List<Tenant>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Tenant>());
			}
			normalInfo(tenants);
			JsonHelper<List<Tenant>, String> jsonHelp = new JsonHelper<List<Tenant>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(tenants);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.GET_TENANT, ParamConstant.TENANT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_TENANT, ParamConstant.TENANT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_TENANT_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_TENANT, ParamConstant.TENANT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/tenants/{id}", method = RequestMethod.GET)
	public String getTenant(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id,HttpServletResponse response) {
		//get ostoken by cuibl
		TokenOs osToken=null;
		try {
			osToken = this.getUserOsToken(guiToken);
			if(null == osToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Tenant tenant = tenantService.getTenant(id, osToken);
			tenant.normalInfo();
			JsonHelper<Tenant, String> jsonHelp = new JsonHelper<Tenant, String>();
			return jsonHelp.generateJsonBodyWithEmpty(tenant);
		}  catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(osToken),osToken.getTenantid(), Message.UPDATE_TENANT_LOCALE, ParamConstant.TENANT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(osToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_TENANT_DETAIL_GET_FAILED,new Locale(osToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
	
	@RequestMapping(value = "/tenants", method = RequestMethod.POST)
	public String createTenant(	@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {
		
		TokenOs osToken = null;
		try {
			osToken = this.getUserOsToken(guiToken);
			if(null == osToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.getAuthService().checkIsAdmin(osToken);
			this.checkUserPermission(osToken, ParamConstant.TENANT_NEW);
			Tenant tenant = tenantService.createTenant(createBody, osToken);
			tenant.normalInfo();
			JsonHelper<Tenant, String> jsonHelp = new JsonHelper<Tenant, String>();
			return jsonHelp.generateJsonBodyWithEmpty(tenant);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			log.error(e.getResponseMessage(),e);
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(osToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			ResourceBusinessException exception =  new ResourceBusinessException(Message.CS_TENANT_CREATE_FAILED, new Locale(osToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
		    return exception.getResponseMessage();
		}
	}
	
	@RequestMapping(value = "/tenants/{id}", method = RequestMethod.DELETE)
	public String deleteTenant(	@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		
		TokenOs osToken = null;
		try {
			osToken = this.getUserOsToken(guiToken);
			if(null == osToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.getAuthService().checkIsAdmin(osToken);
			this.checkUserPermission(osToken, ParamConstant.TENANT_DELETE);
			tenantService.deleteTenant(id, osToken);
			JsonHelper<Tenant, String> jsonHelp = new JsonHelper<Tenant, String>();
			return jsonHelp.generateJsonBodyWithEmpty(new Tenant());
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			log.error(e.getResponseMessage(),e);
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(osToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			ResourceBusinessException exception =  new ResourceBusinessException(Message.CS_TENANT_DELETE_FAILED, new Locale(osToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
	
	@RequestMapping(value = "/tenants/{id}", method = RequestMethod.PUT)
	public String updateTenant(	@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String body, HttpServletResponse response) {
		
		TokenOs osToken = null;
		try {
			osToken = this.getUserOsToken(guiToken);
			if(null == osToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			//this.getAuthService().checkIsAdmin(osToken);
			this.checkUserPermission(osToken, ParamConstant.TENANT_UPDATE);
			Tenant tenant = tenantService.updateTenant(id, body, osToken);
			tenant.normalInfo();
			JsonHelper<Tenant, String> jsonHelp = new JsonHelper<Tenant, String>();
			return jsonHelp.generateJsonBodyWithEmpty(tenant);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			log.error(e.getResponseMessage(),e);
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(osToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			ResourceBusinessException exception =  new ResourceBusinessException(Message.CS_TENANT_UPDATE_FAILED, new Locale(osToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
	
	@RequestMapping(value = "/tenants/{id}/add", method = RequestMethod.PUT)
	public String appendUserToTenant(	@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String updateBody, HttpServletResponse response) {
		
		TokenOs osToken = null;
		try {
			osToken = this.getUserOsToken(guiToken);
			if(null == osToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			//this.getAuthService().checkIsAdmin(osToken);
			this.checkUserPermission(osToken, ParamConstant.TENANT_ADD_USER);
			Tenant tenant = tenantService.addUsersToTenant(id, updateBody, osToken);
			tenant.normalInfo();
			JsonHelper<Tenant, String> jsonHelp = new JsonHelper<Tenant, String>();
			return jsonHelp.generateJsonBodyWithEmpty(tenant);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			log.error(e.getResponseMessage(),e);
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(osToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			ResourceBusinessException exception =  new ResourceBusinessException(Message.CS_TENANT_UPDATE_FAILED, new Locale(osToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
	
	@RequestMapping(value = "/tenants/{id}/addRole", method = RequestMethod.PUT)
	public String appendUserRoleToTenant(	@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String updateBody, HttpServletResponse response) {
		
		TokenOs osToken = null;
		try {
			osToken = this.getUserOsToken(guiToken);
			if(null == osToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			//this.getAuthService().checkIsAdmin(osToken);
			this.checkUserPermission(osToken, ParamConstant.TENANT_UPDATE_USERROLE);
			tenantService.assignUserRoleToTenant(osToken, id, updateBody);
			JsonHelper<Tenant, String> jsonHelp = new JsonHelper<Tenant, String>();
			return jsonHelp.generateJsonBodyWithEmpty(new Tenant());
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			log.error(e.getResponseMessage(),e);
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(osToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			ResourceBusinessException exception =  new ResourceBusinessException(Message.CS_TENANT_UPDATE_FAILED, new Locale(osToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
	
	
	@RequestMapping(value = "/tenants/{id}/remove", method = RequestMethod.PUT)
	public String removeUserFromTenant(	@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String updateBody, HttpServletResponse response) {
		
		TokenOs osToken = null;
		try {
			osToken = this.getUserOsToken(guiToken);
			if(null == osToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			//this.getAuthService().checkIsAdmin(osToken);
			this.checkUserPermission(osToken, ParamConstant.TENANT_REMOVE_USER);
			Tenant tenant = tenantService.removeUsersFromTenant(id, updateBody, osToken);
			tenant.normalInfo();
			JsonHelper<Tenant, String> jsonHelp = new JsonHelper<Tenant, String>();
			return jsonHelp.generateJsonBodyWithEmpty(tenant);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			log.error(e.getResponseMessage(),e);
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(osToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			ResourceBusinessException exception =  new ResourceBusinessException(Message.CS_TENANT_UPDATE_FAILED, new Locale(osToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
//	@RequestMapping(value = "/tenants_test", method = RequestMethod.GET)
//	public String testTenantList(
//			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
//			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name, HttpServletResponse response) {
//
//		TokenOs authToken=null;
//	    TokenGui tokenGui=null;
//		try{
//			authToken = this.getUserOsToken(guiToken);
//			if(null == authToken){
//				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
//				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
//				return exception.getResponseMessage();
//			}
//			tokenGui = this.getAuthService().selectCheckGuiByEncrypt(guiToken);
//			} catch (ResourceBusinessException e) {
//				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
//				return e.getResponseMessage();
//			} catch (Exception e) {
//				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
//				return new ResourceBusinessException(Message.CS_AUTH_ERROR).getResponseMessage();
//		}		
//		
//		try {
//			
//			Tenant crut=tenantService.getCurrentTenant(tokenGui);
//			JsonHelper<Tenant, String> jsonHelp = new JsonHelper<Tenant, String>();
//			log.debug("tenant:"+jsonHelp.generateJsonBodySimple(crut));
//			
//			Tenant crut_p=tenantService.getCurrentParentTenant(tokenGui);
//			log.debug("tenant:"+jsonHelp.generateJsonBodySimple(crut_p));
//			
//			List<CloudUser> listUser=tenantService.gettCurrentSubTenantUserList(tokenGui);
//			JsonHelper<List<CloudUser>, String> jsonHelp2 = new JsonHelper<List<CloudUser>, String>();
//			log.debug("SubtenantUser:"+jsonHelp2.generateJsonBodySimple(listUser));
//			
//			List<CloudUser> listUser2= tenantService.getSubTenantUserList(tokenGui,crut_p.getId());
//			log.debug("SubtenantUser2:"+jsonHelp2.generateJsonBodySimple(listUser2));
//	
//			return jsonHelp2.generateJsonBodySimple(listUser);
//	    } catch (ResourceBusinessException e) {
//			log.error(e);
//			response.setStatus(e.getStatusCode());
//			String message = e.getResponseMessage();
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.GET_TENANT, ParamConstant.TENANT, "",
//					Message.FAILED_FLAG, message);
//			return message;
//		} catch (MyBatisSystemException e) {
//			log.error(e);
//			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
//			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
//			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.GET_TENANT, ParamConstant.TENANT, "",
//					Message.FAILED_FLAG, message);
//			return message;
//		} catch (Exception e) {
//			// TODO
//			log.error(e);
//			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
//			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_TENANT_GET_FAILED,new Locale(authToken.getLocale()));
//			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_TENANT, ParamConstant.TENANT, "",
//					Message.FAILED_FLAG, message);
//			return message;
//		}
//	}
//	
	@RequestMapping(value = "/tenant-language/{locale}", method = RequestMethod.PUT)
	public String updateTenant(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String locale,HttpServletResponse response) {
		//get ostoken by cuibl
		TokenGui authToken=null;
		TokenOs osToken=null;
		try {
			authToken = this.getUserGuiToken(guiToken);
			osToken = this.getUserOsToken(guiToken);
			if(null == osToken || null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(osToken, ParamConstant.COMMON_UPDATE_LOCALE);
			String token = tenantService.updateTenantToken(authToken, locale);
			return new JsonHelper<String,String>().generateJsonBodySimple(token,"token");
		}  catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			log.error(message,e);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(osToken),authToken.getTenantid(), Message.UPDATE_TENANT_LOCALE, ParamConstant.TENANT, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_TENANT_UPDATE_FAILED,new Locale(authToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
	
	private void normalInfo(List<Tenant> tenants){
		if(Util.isNullOrEmptyList(tenants))
			return;
		for(Tenant tenant : tenants)
			tenant.normalInfo();
	}
}
