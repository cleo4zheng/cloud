package com.cloud.cloudapi.controller.common;

import java.util.Locale;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.cloud.cloudapi.controller.openstackapi.BackupController;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenGui;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.CloudRoleService;
import com.cloud.cloudapi.service.common.CloudUserService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BaseController {
	@Resource
	private CloudRoleService roleService;  
	
	@Resource
	private CloudConfig cloudconfig;
	
	@Resource
	private AuthService authService;
	
	@Resource
	private CloudUserService cloudUserService;
	
	private Logger log = LogManager.getLogger(BackupController.class);
	
	protected void checkUserPermission(TokenOs ostoken,String operation) throws ResourceBusinessException{
		if(!cloudconfig.isPermissionEnabled())
			return;
		roleService.checkUserPermission(ostoken, operation);
	}
	
	protected void checkUserPermission(TokenOs ostoken,String operation,String instanceId) throws ResourceBusinessException{
		if(!cloudconfig.isPermissionEnabled())
			return;
		roleService.checkUserPermission(ostoken,operation,instanceId);
	}
	
	protected AuthService getAuthService(){
		return this.authService;
	}
	
	protected CloudConfig getConfig(){
		return this.cloudconfig;
	}
	
	protected TokenOs getUserOsToken(String token){
		TokenOs authToken=null;
		try{
		authToken = authService.insertCheckGuiAndOsTokenByEncrypt(token);
		} catch (Exception e) {
			log.error(e);
			return null;
 		} 	
		return authToken;
	}
	
	protected TokenGui getUserGuiToken(String token){
		TokenGui authToken=null;
		try{
		authToken = authService.selectCheckGuiByEncrypt(token);
		} catch (Exception e) {
			log.error(e);
			return null;
		} 
		return authToken;
	}
	
	protected Boolean checkUpdateAdminState(String body){
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(body);
		} catch (Exception e) {
			log.error(e);
			return null;
		}
		if(rootNode.path(ResponseConstant.ADMIN_STATE_UP).isMissingNode())
			return null;
		if(rootNode.path(ResponseConstant.ADMIN_STATE_UP).textValue().equalsIgnoreCase(ParamConstant.TRUE))
			return true;
		return false;
	}
	
	protected Boolean checkAlarmEnableState(String body){
		try{
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(body);
			if(rootNode.path(ResponseConstant.ENABLE).textValue().equalsIgnoreCase(ParamConstant.TRUE))
				return true;
		}catch (Exception e) {
			log.error(e);
			return true;
		}
		return false;
	}
}
