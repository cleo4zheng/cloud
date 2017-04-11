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

import com.cloud.cloudapi.dao.common.CloudUserMapper;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.common.TokenGui;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.crm.CrmInfo;
import com.cloud.cloudapi.service.common.CloudRoleService;
import com.cloud.cloudapi.service.common.CloudUserService;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.crm.CrmInfoService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class CloudUserController extends BaseController {
	
	@Resource
	private CloudUserService cloudUserService;

	@Resource
	private CloudUserMapper cloudUserMapper;

	@Resource
	private CloudRoleService cloudRoleService; 
	
	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private CrmInfoService crmInfoService;

	private Logger log = LogManager.getLogger(CloudUserController.class);
    /**
     * for get request and json content
     * get data from json content
     * @param name
     * @return
     */
    @RequestMapping("/auth/sign-up")
    public String createUser(@RequestBody String createBody,HttpServletResponse response) {
    	
    	 ObjectMapper mapper = new ObjectMapper();
    	 CloudUser cloudUser = null;
    	 String rsMessage="success";
    	 boolean hasException= true;
         try { 
        	 log.debug("sign-up:jsonboy-"+createBody);
        	 cloudUser= mapper.readValue(createBody,  CloudUser.class);
        	 hasException=false;
         } catch (Exception e) {
        	 hasException=true;
             log.error(e);
         }
         
 		if(hasException){
            response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
            return new ResourceBusinessException(Message.CS_COMPUTE_USER_SIGN_FAILED,new Locale(cloudUser.getLocale())).getResponseMessage();
        }
 		
        try {   	
        	TokenOs adminToken=this.getAuthService().createDefaultAdminOsToken();
			cloudUserService.insertUserAndTenant(cloudUser,adminToken);
		}catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			log.error(e.getResponseMessage(),e);
			return e.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			ResourceBusinessException exception =  new ResourceBusinessException(Message.CS_COMPUTE_USER_SIGN_FAILED,new Locale(cloudUser.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
         rsMessage= new JsonHelper<String,String>().generateJsonBodySimple(rsMessage,"status");          
         return rsMessage;
    }
    
    
    @RequestMapping(value = "/users", method = RequestMethod.POST)
    public String addUser(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
    		@RequestBody String createBody,HttpServletResponse response) {
    	
    	 ObjectMapper mapper = new ObjectMapper();
    	 CloudUser cloudUser = null;
    	 String rsMessage="success";
    	 boolean hasException= true;
         try { 
        	 log.debug("sign-up:jsonboy-"+createBody);
        	 cloudUser= mapper.readValue(createBody,  CloudUser.class);
        	 hasException=false;
         } catch (Exception e) {
        	 hasException=true;
             log.error(e);
             
         } 
 		if(hasException){
            response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
            return new ResourceBusinessException(Message.CS_COMPUTE_USER_SIGN_FAILED,new Locale(cloudUser.getLocale())).getResponseMessage();
        }
 		
        try {   
        	TokenOs authToken = this.getUserOsToken(guiToken);
        	if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
        	this.checkUserPermission(authToken, ParamConstant.USER_NEW);
        	
        	TokenOs adminToken=this.getAuthService().createDefaultAdminOsToken();
			cloudUserService.insertUserAndTenant(cloudUser,adminToken);
			
			hasException=false;
		}catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			log.error(e.getResponseMessage(),e);
			return e.getResponseMessage();
		} catch (Exception e) {
			hasException = true;
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_USER_SIGN_FAILED,new Locale(cloudUser.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
         rsMessage= new JsonHelper<String,String>().generateJsonBodySimple(rsMessage,"status");          
         return rsMessage;
    }
    
    /**
     * 获取系统中所有用�?
     * @param response
     * @return
     */
    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public String getCloudUsersList(
    		@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			HttpServletResponse response) {
    	String message = "[]";
    	TokenOs token = null;
		try {
			//检查是否为管理�?
//			if(!JWTTokenHelper.isAdminFromEncryptToken(guiToken)){
//				response.setStatus(ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE);
//			    return Message.getMessage(Message.CS_USER_AUTHHORIZED_ERROR, true);
//			}
			token = this.getUserOsToken(guiToken);
			if(null == token){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.getAuthService().checkIsAdmin(token);
			List<CloudUser> userList = cloudUserService.getCloudUsersList();
			normalUserInfo(userList);
			JsonHelper<List<CloudUser>, String> jsonHelp = new JsonHelper<List<CloudUser>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(userList);
		} catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			log.error(e.getResponseMessage(),e);
			return e.getResponseMessage();
		} catch (Exception e) {
            response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
            message = Message.getMessage(Message.CS_USERLIST_GET_ERROR,new Locale(token.getLocale()), true);
            log.error(message,e);
		}
    	return message;
    	
    }
    
    /**
     * 获取用户详细信息
     * @param response
     * @return
     */
    @RequestMapping(value = "/users/{id}", method = RequestMethod.GET)
    public String getCloudUserDetail(
    		@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
    		@PathVariable String id,
			HttpServletResponse response) {
    	String message = "{}";
    	TokenOs token = null;
    	CloudUser user = null;
		try {
			//检查是否为管理员用户或者为当前用户
			token = this.getUserOsToken(guiToken);
			if(null == token){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.getAuthService().checkIsAdmin(token);
			user = cloudUserService.getCloudUserDetail(token, id);
			//不显示密�?
			user.normalInfo();
	
			JsonHelper<CloudUser, String> jsonHelp = new JsonHelper<CloudUser, String>();
			return jsonHelp.generateJsonBodyWithEmpty(user);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(token), token.getTenantid(),Message.GET_USER_DETAIL,ParamConstant.USER,"",Message.FAILED_FLAG,message);
			return message;
		}catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(token.getLocale()));
			message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(token), token.getTenantid(),Message.GET_USER_DETAIL,ParamConstant.USER,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			if(null != user)
				message = Message.getMessage(Message.CS_USERDETAIL_GET_ERROR,new Locale(user.getLocale()),true);
			log.error(message,e);
		}
    	
    	return message;
    	
    }
    
    /**
     * 启用与禁用用�?reset用户密码
     * @param response
     * @return
     */
    @RequestMapping(value = "/users/{id}/{action}", method = RequestMethod.PUT)
    public String operateUser(
    		@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
    		@PathVariable String id,
    		@PathVariable String action,
			HttpServletResponse response) {
    	String message = "{}";
    	CloudUser user = null;
    	TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			if(ParamConstant.ENABLE_USER_ACTION.equals(action)){
				this.checkUserPermission(authToken, ParamConstant.USER_ENABLE);
			}else if(ParamConstant.DISABLE_USER_ACTION.equals(action)){
				this.checkUserPermission(authToken, ParamConstant.USER_DISABLE);
			}else if(ParamConstant.RESETPASSWORD_USER_ACTION.equals(action)){
				this.checkUserPermission(authToken, ParamConstant.USER_RESET);
		    }
			
			user = cloudUserService.operateUser(id, action);
			//检查是否为管理�?
//			if(!JWTTokenHelper.isAdminFromEncryptToken(guiToken)){
//				response.setStatus(ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE);
//			    message = Message.getMessage(Message.CS_USER_AUTHHORIZED_ERROR, new Locale(user.getLocale()),true);
//			}
			/*JsonHelper<CloudUser, String> jsonHelp = new JsonHelper<CloudUser, String>();
			return jsonHelp.generateJsonBodyWithEmpty(user);*/
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			message = e.getResponseMessage();
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_USER,ParamConstant.USER,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			message = exception.getResponseMessage();
		//	this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_USER,ParamConstant.USER,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}  catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			if(null != user)
				message = Message.getMessage(Message.CS_USER_UPDATE_ERROR, new Locale(authToken.getLocale()),true);
			log.error(message,e);
		}
    	
    	return message; 
    }
    
    @RequestMapping(value = "/user-tenant/{name}", method = RequestMethod.PUT)
	public String updateUserTenant(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String name,HttpServletResponse response) {
		//get ostoken by cuibl
		TokenOs authToken=null;
		TokenGui guitoken = this.getUserGuiToken(guiToken);
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken || null == guitoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.COMMON_UPDATE_TENANT);
			String token = cloudUserService.updateUserProject(guitoken,name);
			return new JsonHelper<String,String>().generateJsonBodySimple(token,"token");
		}  catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			log.error(message,e);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.UPDATE_USER_PROJECT, ParamConstant.USER, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_UPDATE_ERROR,new Locale(authToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
    
    
    /**
     * 为用户绑定订单号
     * @param cloudUsers
     * 
     * 
     */
    @RequestMapping(value = "/users/{userId}/bind/{ddh}", method = RequestMethod.PUT)
    public String bindddh(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
    		@PathVariable String userId,@PathVariable String ddh,HttpServletResponse response){
    	TokenOs authToken = null;
    	int  bindresult = 0;
    	String message = "{}";
    	try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			//bond
			this.checkUserPermission(authToken, ParamConstant.BOND_DDH);
			bindresult = cloudUserService.bindddh(userId, ddh);
    	} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			message = e.getResponseMessage();
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_USER,ParamConstant.USER,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		}  catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			if(bindresult == 0)
				message = Message.getMessage(Message.CS_USER_UPDATE_ERROR, new Locale(authToken.getLocale()),true);
			log.error(message,e);
		}
    	return message;
    }
    
    @RequestMapping(value = "/ddhs", method = RequestMethod.GET)
    public String getAllDdh(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
    		HttpServletResponse response){
    	String message = "{}";
    	TokenOs authToken = null;
    	try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			//bond
			List<CrmInfo> crms = crmInfoService.getAllavaialeCrmInfo();
			normalCrmInfo(crms);
			JsonHelper<List<CrmInfo>, String> jsonHelp = new JsonHelper<List<CrmInfo>, String>();
			message =  jsonHelp.generateJsonBodyWithEmpty(crms);
    	} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			message = e.getResponseMessage();
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_USER,ParamConstant.USER,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		}  catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			log.error(message,e);
		}
    	return message;
    }
    
//	@RequestMapping(value = "/add-roles/{id}", method = RequestMethod.PUT)
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
//			this.checkUserPermission(authToken, ParamConstant.USER_BIND_ROLE);
//			cloudUserService.addRoles(id, body, authToken);
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.ADD_ROLE,ParamConstant.ROLE,id,Message.SUCCESSED_FLAG,"");
//			ResourceBusinessException message = new ResourceBusinessException(Message.CS_ROLE_ADD_SUCCESSED,new Locale(authToken.getLocale()));
//			return message.getResponseMessage();
//		} catch (ResourceBusinessException e) {
//			response.setStatus(e.getStatusCode());
//			String message = e.getResponseMessage();
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.ADD_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
//			log.error(message,e);
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
	
	
//	@RequestMapping(value = "/remove-roles/{userId}/{roleId}", method = RequestMethod.PUT)
//	public String removeRole(
//			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
//			@PathVariable String userId, @PathVariable String roleId, HttpServletResponse response) {
//		
//		TokenOs authToken=null;
//		try {
//			authToken = this.getUserOsToken(guiToken);
//			if(null == authToken){
//				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
//				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
//				return exception.getResponseMessage();
//			}
//			this.checkUserPermission(authToken, ParamConstant.USER_REMOVE_ROLE);
//			CloudRole role = cloudRoleService.removeRole(roleId, userId, authToken);
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.REMOVE_ROLE,ParamConstant.ROLE,roleId,Message.SUCCESSED_FLAG,"");
//			JsonHelper<CloudRole, String> jsonHelp = new JsonHelper<CloudRole, String>();
//			return jsonHelp.generateJsonBodyWithEmpty(role);
//		} catch (ResourceBusinessException e) {
//			response.setStatus(e.getStatusCode());
//			String message = e.getResponseMessage();
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.REMOVE_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
//			log.error(message,e);
//			return message;
//		} catch (MyBatisSystemException e) {
//			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
//			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
//			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.REMOVE_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
//			log.error(message,e);
//			return message;
//		} catch (Exception e) {
//			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
//			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_ROLE_REMOVE_FAILED,new Locale(authToken.getLocale()));
//			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.REMOVE_ROLE,ParamConstant.ROLE,"",Message.FAILED_FLAG,message);
//			log.error(message,e);
//			return message;
//		}
//	}
	
    private void normalUserInfo(List<CloudUser> cloudUsers){
    	if(Util.isNullOrEmptyList(cloudUsers))
    		return;
    	for(CloudUser cloudUser : cloudUsers){
    		cloudUser.normalInfo();
    	}
    }
    private void normalCrmInfo(List<CrmInfo> crms){
    	if(Util.isNullOrEmptyList(crms)){
    		return ;
    	}
    	for (CrmInfo crm: crms){
    		crm.normalInfo();
    	}
    }
}
