package com.cloud.cloudapi.controller.common;

import java.util.HashMap;
import java.util.Locale;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.common.TokenGui;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.service.common.CloudRoleService;
import com.cloud.cloudapi.service.common.CloudUserService;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.util.JWTTokenHelper;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.StringHelper;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class AuthController  extends BaseController {
	
	private Logger log = LogManager.getLogger(AuthController.class);

	@Resource
	private QuotaService quotaService;
	
	@Resource
	private CloudUserService cloudUserService;

	@Resource
	private CloudRoleService cloudRoleService; 
	
	@Resource
	private CloudConfig cloudconfig;
	
	@RequestMapping(value = "/auth/tokens", method = RequestMethod.POST)
	public String userLogin(@RequestBody String createBody, HttpServletResponse response) {
		ObjectMapper mapper = new ObjectMapper();
		CloudUser cloudUser = null;
		String rsMessage = "success";
		boolean hasException = true;
		boolean isAdmin = false;
		try {
			log.debug("login:jsonboy-" + createBody);
			cloudUser = mapper.readValue(createBody, CloudUser.class);
			hasException = false;
		} catch (Exception e) {
			hasException = true;
			log.error("login error:", e);
		} 
		if(hasException){
            response.setStatus(ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE);
            return new ResourceBusinessException(Message.CS_AUTH_INFO_ERROR,new Locale(cloudUser.getLocale())).getMessage();
        }
		
		TokenGui guitoken=null;
		try {
			guitoken = this.getAuthService().insertLogin(cloudUser);
			//检查是否为管理员

			cloudUser = cloudUserService.selectUserByGuiTokenId(guitoken.getTokenid());
			isAdmin = cloudUserService.checkIsAdmin(cloudUser.getUserid());
			hasException = false;
		}catch (ResourceBusinessException e) {	
			hasException = true;
			response.setStatus(ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE);
			rsMessage= e.getResponseMessage();
			log.error("login error:", e);
		} catch (Exception e) {
			hasException = true;
			// TODO Auto-generated catch block
			response.setStatus(ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE);
			rsMessage= new ResourceBusinessException(Message.CS_AUTH_ERROR).getMessage();
			log.error("login error:", e);
		}
		
		if(hasException){
            response.setStatus(ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE);
            return rsMessage;
        }
		
		try{
			this.getAuthService().insertOsTokenByGuiId(guitoken.getTokenid());
		}catch (ResourceBusinessException e) {	
			hasException = true;
			response.setStatus(e.getStatusCode());
			rsMessage= e.getResponseMessage();
			log.error("login error:", e);
		} catch (Exception e) {
			hasException = true;
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			rsMessage= new ResourceBusinessException(Message.CS_AUTH_ERROR,new Locale(cloudUser.getLocale())).getMessage();
			log.error("login error:", e);
		}
		
		if(hasException){
            response.setStatus(ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE);
            return rsMessage;
        }
		
        rsMessage= new JsonHelper<String,String>().generateJsonBodySimple(JWTTokenHelper.createEncryptToken(guitoken, cloudUser,isAdmin),"token");
  		return rsMessage;
	}

	/**
     * 检查旧密码
     * @param response
     * @return
     */
    @RequestMapping(value = "/auth/checkOldPass", method = RequestMethod.POST)
    public String checkOldPass(
    		@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
    		@RequestBody String checkBody,
			HttpServletResponse response) {
    	String message = "{}";
    	ObjectMapper mapper = new ObjectMapper();
		try {
			CloudUser cloudUser = mapper.readValue(checkBody, CloudUser.class);
			this.getAuthService().checkOldPassword(cloudUser);
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
		//	message = e.getMessage();
			message = Message.getMessage(Message.CS_CHECK_PASWORD_FAILED, true);
			log.error(e);
		}
    	
    	return message;
    	
    }
    
    /**
     * 修改密码
     * @param response
     * @return
     */
    @RequestMapping(value = "/auth/modify", method = RequestMethod.POST)
    public String modifyPass(
    		@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
    		@RequestBody String modifyBody,
			HttpServletResponse response) {
    	String message = "{}";
    	ObjectMapper mapper = new ObjectMapper();
    	CloudUser cloudUser = null;
		try {
			TokenOs authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.USER_MODIFY_PASSWORD);
			cloudUser = this.getAuthService().getUserByGuiToken(guiToken);
			HashMap map = mapper.readValue(modifyBody, HashMap.class);
			String account = StringHelper.objectToString(map.get("account"));
			String oldPass = StringHelper.objectToString(map.get("oldPass"));
			String newPass = StringHelper.objectToString(map.get("newPass"));
			String reNewPass = StringHelper.objectToString(map.get("reNewPass"));
			if(!newPass.equals(reNewPass)){
				response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
				message = Message.getMessage(Message.CS_USER_PASSWORD_MODIFY_INCONSISTENT, new Locale(cloudUser.getLocale()),true);
			}
			CloudUser user = new CloudUser();
			user.setAccount(account);
			user.setPassword(oldPass);
			//检查密码
			this.getAuthService().checkOldPassword(user);
			//修改密码
			this.getAuthService().modifyPassword(account,newPass);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			message = e.getMessage();
			log.error(e);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			if(null != cloudUser)
				message = Message.getMessage(Message.CS_USER_PASSWORD_MODIFY_FAILED, new Locale(cloudUser.getLocale()),true);
			else
				message = Message.getMessage(Message.CS_USER_PASSWORD_MODIFY_FAILED,true);
			log.error(e);
		}
    	
    	return message;
    	
    }
}
