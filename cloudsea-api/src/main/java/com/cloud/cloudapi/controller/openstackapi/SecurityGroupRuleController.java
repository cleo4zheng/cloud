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

import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SecurityGroupRule;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.openstackapi.SecurityGroupRuleService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class SecurityGroupRuleController {

	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private SecurityGroupRuleService securityGroupRuleService;
	
	@Resource
	private AuthService authService;

	private Logger log = LogManager.getLogger(SecurityGroupRuleController.class);
	
	@RequestMapping(value = "/security-group-rules", method = RequestMethod.GET)
	public String getSecurityGroupRulesList(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			HttpServletResponse response) {
		// get ostoken by cuibl
		TokenOs authToken = null;
		try {
			authToken = authService.insertCheckGuiAndOsTokenByEncrypt(guiToken);
		} catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return e.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR).getResponseMessage();
		}
		try {
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

			List<SecurityGroupRule> securityGroupRules = securityGroupRuleService.getSecurityGroupRuleList(paramMap,
					authToken);
			if (null == securityGroupRules) {
//				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//						Message.GET_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP_RULE, "", Message.SUCCESSED_FLAG,
//						"");
				JsonHelper<List<SecurityGroupRule>, String> jsonHelp = new JsonHelper<List<SecurityGroupRule>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<SecurityGroupRule>());
			}
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//					Message.GET_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP_RULE,
//					getSecurityGroupRulesId(securityGroupRules), Message.SUCCESSED_FLAG, "");
			JsonHelper<List<SecurityGroupRule>, String> jsonHelp = new JsonHelper<List<SecurityGroupRule>, String>();
			return jsonHelp.generateJsonBodySimple(securityGroupRules);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP_RULE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP_RULE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NETWORK_SECURITYGROUP_RULE_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP_RULE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/security-group-rules/{id}", method = RequestMethod.GET)
	public String getSecurityGroupRule(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		// get ostoken by cuibl
		TokenOs authToken = null;
		try {
			authToken = authService.insertCheckGuiAndOsTokenByEncrypt(guiToken);
		} catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return e.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR).getResponseMessage();
		}
		try {
			SecurityGroupRule securityGroupRule = securityGroupRuleService.getSecurityGroupRule(id, authToken);
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//					Message.GET_SECURITYGROUP_RULE_DETAIL, ParamConstant.SECURITYGROUP_RULE, id,
//					Message.SUCCESSED_FLAG, "");
			JsonHelper<SecurityGroupRule, String> jsonHelp = new JsonHelper<SecurityGroupRule, String>();
			return jsonHelp.generateJsonBodyWithEmpty(securityGroupRule);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//					Message.GET_SECURITYGROUP_RULE_DETAIL, ParamConstant.SECURITYGROUP_RULE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_SECURITYGROUP_RULE_DETAIL, ParamConstant.SECURITYGROUP_RULE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NETWORK_SECURITYGROUP_RULE_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_SECURITYGROUP_RULE_DETAIL, ParamConstant.SECURITYGROUP_RULE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/security-group-rules", method = RequestMethod.POST)
	public String createSecurityGroupRule(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {
		// @TODO 1. guitoken should has no defaultValue,if there no token ,bad
		// request
		// get ostoken by cuibl
		TokenOs authToken = null;
		try {
			authToken = authService.insertCheckGuiAndOsTokenByEncrypt(guiToken);
		} catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return e.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR).getResponseMessage();
		}
		// @TODO 2. guitoken should be checked, timeout or not

		try {
			SecurityGroupRule securityGroupRule = securityGroupRuleService.createSecurityGroupRule(createBody,
					authToken);
			if (null == securityGroupRule) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.CREATE_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP_RULE, "", Message.FAILED_FLAG,
						message);
				return message;
			}
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP_RULE, securityGroupRule.getId(),
					Message.SUCCESSED_FLAG, "");
			JsonHelper<SecurityGroupRule, String> jsonHelp = new JsonHelper<SecurityGroupRule, String>();
			return jsonHelp.generateJsonBodySimple(securityGroupRule);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP_RULE, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP_RULE, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NETWORK_SECURITYGROUP_RULE_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP_RULE, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/security-group-rules/{id}", method = RequestMethod.DELETE)
	public String deleteSecurityGroupRule(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		// get ostoken by cuibl
		TokenOs authToken = null;
		try {
			authToken = authService.insertCheckGuiAndOsTokenByEncrypt(guiToken);
		} catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return e.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR).getResponseMessage();
		}
		try {
			securityGroupRuleService.deleteSecurityGroupRule(id, authToken);
			if(null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NETWORK_SECURITYGROUP_RULE_DELETE_SUCCESSED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP_RULE, id,
					Message.SUCCESSED_FLAG, message);
			return message;
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP_RULE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP_RULE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NETWORK_SECURITYGROUP_RULE_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_SECURITYGROUP_RULE, ParamConstant.SECURITYGROUP_RULE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
//	private String getSecurityGroupRulesId(List<SecurityGroupRule> securityGroupRules) {
//		if (Util.isNullOrEmptyList(securityGroupRules))
//			return "";
//		List<String> ids = new ArrayList<String>();
//		for (SecurityGroupRule securityGroupRule : securityGroupRules) {
//			ids.add(securityGroupRule.getId());
//		}
//		return Util.listToString(ids, ',');
//	}
}
