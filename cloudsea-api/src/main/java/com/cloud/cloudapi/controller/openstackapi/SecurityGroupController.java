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
import com.cloud.cloudapi.pojo.openstackapi.forgui.SecurityGroup;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SecurityGroupRule;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.openstackapi.SecurityGroupService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class SecurityGroupController extends BaseController {

	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private SecurityGroupService securityGroupService;

	private Logger log = LogManager.getLogger(SecurityGroupController.class);
	
	@RequestMapping(value = "/security-groups", method = RequestMethod.GET)
	public String getSecurityGroupsList(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			HttpServletResponse response) {

		TokenOs authToken = null;
		try {
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
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.NAME, name);
			}

			if (!"".equals(status)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.STATUS, status);
			}

			List<SecurityGroup> securityGroups = securityGroupService.getSecurityGroupList(paramMap, authToken);
			if (Util.isNullOrEmptyList(securityGroups)) {
				JsonHelper<List<SecurityGroup>, String> jsonHelp = new JsonHelper<List<SecurityGroup>, String>();
//				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//						Message.GET_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.SUCCESSED_FLAG, "");
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<SecurityGroup>());
			}
			normalSecurityGroupsInfo(securityGroups);
			JsonHelper<List<SecurityGroup>, String> jsonHelp = new JsonHelper<List<SecurityGroup>, String>();
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//					Message.GET_SECURITYGROUP, ParamConstant.SECURITYGROUP, getSecurityGroupsId(securityGroups),
//					Message.SUCCESSED_FLAG, "");
			return jsonHelp.generateJsonBodySimple(securityGroups);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NETWORK_SECURITYGROUP_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/security-groups/{id}", method = RequestMethod.GET)
	public String getSecurityGroup(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			SecurityGroup securityGroup = securityGroupService.getSecurityGroup(id, authToken);
			if (null == securityGroup) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
//				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//						Message.GET_SECURITYGROUP_DETAIL, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG,
//						message);
				return message;
			}
			normalSecurityGroupWithRuleInfo(securityGroup);
			JsonHelper<SecurityGroup, String> jsonHelp = new JsonHelper<SecurityGroup, String>();
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//					Message.GET_SECURITYGROUP_DETAIL, ParamConstant.SECURITYGROUP, securityGroup.getId(),
//					Message.SUCCESSED_FLAG, "");
			return jsonHelp.generateJsonBodyWithEmpty(securityGroup);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NETWORK_SECURITYGROUP_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/security-groups", method = RequestMethod.POST)
	public String createSecurityGroup(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.SECURITYGROUP_NEW);
			SecurityGroup securityGroup = securityGroupService.createSecurityGroup(createBody, authToken);
			if (null == securityGroup) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.CREATE_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
				return message;
			}
			normalSecurityGroupWithRuleInfo(securityGroup);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_SECURITYGROUP, ParamConstant.SECURITYGROUP, securityGroup.getId(),
					Message.SUCCESSED_FLAG, "");
			JsonHelper<SecurityGroup, String> jsonHelp = new JsonHelper<SecurityGroup, String>();
			return jsonHelp.generateJsonBodyWithEmpty(securityGroup);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NETWORK_SECURITYGROUP_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	
	@RequestMapping(value = "/security-groups/{id}", method = RequestMethod.PUT)
	public String updateSecurityGroup(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String createBody, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.SECURITYGROUP_UPDATE);
			SecurityGroup securityGroup = securityGroupService.updateSecurityGroup(id,createBody, authToken);
			normalSecurityGroupWithRuleInfo(securityGroup);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_SECURITYGROUP, ParamConstant.SECURITYGROUP, id,
					Message.SUCCESSED_FLAG, "");
			JsonHelper<SecurityGroup, String> jsonHelp = new JsonHelper<SecurityGroup, String>();
			return jsonHelp.generateJsonBodyWithEmpty(securityGroup);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_SECURITYGROUP, ParamConstant.SECURITYGROUP, id, Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NETWORK_SECURITYGROUP_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/security-groups/{id}/rules", method = RequestMethod.PUT)
	public String addSecurityGroupRule(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String createBody, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.SECURITYGROUP_ADD_RULE);
			SecurityGroup securityGroup = securityGroupService.addSecurityGroupRule(id,createBody, authToken);
			normalSecurityGroupWithRuleInfo(securityGroup);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.ADD_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP, id,
					Message.SUCCESSED_FLAG, "");
			JsonHelper<SecurityGroup, String> jsonHelp = new JsonHelper<SecurityGroup, String>();
			return jsonHelp.generateJsonBodyWithEmpty(securityGroup);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.ADD_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.ADD_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NETWORK_SECURITYGROUP_ADD_RULE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.ADD_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/security-groups/{id}/rules/{ruleId}", method = RequestMethod.DELETE)
	public String removeSecurityGroupRule(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id,@PathVariable String ruleId, HttpServletResponse response) {
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.SECURITYGROUP_REMOVE_RULE);
			SecurityGroup securityGroup = securityGroupService.removeSecurityGroupRule(id,ruleId, authToken);
			normalSecurityGroupWithRuleInfo(securityGroup);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.REMOVE_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP, id,
					Message.SUCCESSED_FLAG, "");
			JsonHelper<SecurityGroup, String> jsonHelp = new JsonHelper<SecurityGroup, String>();
			return jsonHelp.generateJsonBodyWithEmpty(securityGroup);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.REMOVE_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.REMOVE_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NETWORK_SECURITYGROUP_REMOVE_RULE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.REMOVE_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/security-groups/{id}/addPort", method = RequestMethod.PUT)
	public String addSecurityGroupToPort(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id,@RequestBody String updateBody, HttpServletResponse response) {
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.SECURITYGROUP_ADD_PORT);
			securityGroupService.addSecurityGroupToPort(id, updateBody, authToken);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_SECURITYGROUP, ParamConstant.SECURITYGROUP, id,
					Message.SUCCESSED_FLAG, "");
			JsonHelper<SecurityGroup, String> jsonHelp = new JsonHelper<SecurityGroup, String>();
			return jsonHelp.generateJsonBodyWithEmpty(new SecurityGroup());
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NETWORK_SECURITYGROUP_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/security-groups/{id}/removePort", method = RequestMethod.PUT)
	public String removeSecurityGroupFromPort(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id,@RequestBody String updateBody, HttpServletResponse response) {
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.SECURITYGROUP_REMOVE_PORT);
			securityGroupService.removeSecurityGroupFromPort(id, updateBody, authToken);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_SECURITYGROUP, ParamConstant.SECURITYGROUP, id,
					Message.SUCCESSED_FLAG, "");
			JsonHelper<SecurityGroup, String> jsonHelp = new JsonHelper<SecurityGroup, String>();
			return jsonHelp.generateJsonBodyWithEmpty(new SecurityGroup());
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NETWORK_SECURITYGROUP_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/security-groups/{id}", method = RequestMethod.DELETE)
	public String deleteSecurityGroup(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.SECURITYGROUP_DELETE);
			securityGroupService.deleteSecurityGroup(id,authToken);
			if(null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NETWORK_SECURITYGROUP_DELETE_SUCCESSED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_SECURITYGROUP, ParamConstant.SECURITYGROUP, id,
					Message.SUCCESSED_FLAG, message);
			return message;
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NETWORK_SECURITYGROUP_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_SECURITYGROUP, ParamConstant.SECURITYGROUP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	private void normalSecurityGroupWithRuleInfo(SecurityGroup securityGroup){
		securityGroup.normalInfo(false);
		List<SecurityGroupRule> rules = securityGroup.getSecurityGroupRules();
		if(Util.isNullOrEmptyList(rules))
			return;
		for(SecurityGroupRule rule : rules){
			rule.setCreatedAt(null);
			rule.setCidr(null);
			rule.setRemote_group_id(null);
			rule.setEthertype(null);
			rule.setRemoteIpPrefix(null);
			rule.setSecurity_group_id(null);
			rule.setTenantId(null);
		}
	}
	
	private void normalSecurityGroupsInfo(List<SecurityGroup> securityGroupsFromDB){
		for(SecurityGroup securityGroup : securityGroupsFromDB){
			securityGroup.normalInfo(true);
		}
	}
	
}
