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
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.OperationLog;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class OperationLogController {

	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(OperationLogController.class);
	
	@RequestMapping(value = "/operations", method = RequestMethod.GET)
	public String getOperatonLogs(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			@RequestParam(value = ParamConstant.RESOURCE, defaultValue = "") String type,
			HttpServletResponse response) {

		Map<String, String> paramMap = null;
		//get ostoken by cuibl
		TokenOs authToken=null;
		try{
		authToken = authService.insertCheckGuiAndOsTokenByEncrypt(guiToken);
		} catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return e.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR).getResponseMessage();
		}
		if (!"".equals(limit)) {
			paramMap = new HashMap<String, String>();
			paramMap.put(ParamConstant.LIMIT, limit);
		}

		if (!"".equals(status)) {
			if (paramMap == null)
				paramMap = new HashMap<String, String>();
			paramMap.put(ParamConstant.STATUS, status);
		}

		if (!"".equals(type)) {
			if (paramMap == null)
				paramMap = new HashMap<String, String>();
			paramMap.put(ParamConstant.RESOURCE, type);
		}

		try {
			List<OperationLog> operationLogs = operationLogService.getOperationLogList(paramMap, authToken);
			if(Util.isNullOrEmptyList(operationLogs)){
				JsonHelper<List<OperationLog>, String> jsonHelp = new JsonHelper<List<OperationLog>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<OperationLog>());
			}
			JsonHelper<List<OperationLog>, String> jsonHelp = new JsonHelper<List<OperationLog>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(operationLogs);
			
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_OPERATION_GET_FAILED,new Locale(authToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
	
	@RequestMapping(value = "/operations", method = RequestMethod.DELETE)
	public String deleteOperatonLogs(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String deleteBody, HttpServletResponse response) {
		//get ostoken by cuibl
		TokenOs authToken=null;
		try{
		authToken = authService.insertCheckGuiAndOsTokenByEncrypt(guiToken);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return e.getResponseMessage();
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR).getResponseMessage();
		}		
		try {
			operationLogService.deleteOperationLogs(deleteBody, authToken);
			JsonHelper<OperationLog, String> jsonHelp = new JsonHelper<OperationLog, String>();
			return jsonHelp.generateJsonBodyWithEmpty(new OperationLog());
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_OPERATION_DELETE_FAILED,new Locale(authToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
	
	@RequestMapping(value = "/operations/{id}", method = RequestMethod.GET)
	public String getOperatonLogDetail(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		//get ostoken by cuibl
		TokenOs authToken=null;
		try{
		authToken = authService.insertCheckGuiAndOsTokenByEncrypt(guiToken);
		} catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return e.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR).getResponseMessage();
		}		
		try {
			OperationLog operationLog = operationLogService.getOperationLogDetail(id, authToken);
			if(null == operationLog){
				JsonHelper<OperationLog, String> jsonHelp = new JsonHelper<OperationLog, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new OperationLog());
			}
			JsonHelper<OperationLog, String> jsonHelp = new JsonHelper<OperationLog, String>();
			return jsonHelp.generateJsonBodyWithEmpty(operationLog);
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_OPERATION_GET_DETAIL_FAILED,new Locale(authToken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}


}
