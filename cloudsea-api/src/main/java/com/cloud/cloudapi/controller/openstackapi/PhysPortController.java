package com.cloud.cloudapi.controller.openstackapi;

import java.util.ArrayList;
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
import com.cloud.cloudapi.pojo.openstackapi.forgui.PhysPort;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.openstackapi.PhysPortService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class PhysPortController {
	@Resource
	private OperationLogService operationLogService;

	@Resource
	private PhysPortService physPortService;

	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(PhysPortController.class);
	
	@RequestMapping(value = "/physports", method = RequestMethod.GET)
	public String getPhysPorts(
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
			Map<String, String> paramMap = Util.makeRequestParamInfo(limit, name, status, "", "");
			List<PhysPort> physPorts = physPortService.getPhysPorts(paramMap, authToken);
			if (Util.isNullOrEmptyList(physPorts)) {
//				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//						Message.GET_PORT, ParamConstant.PORT, "", Message.SUCCESSED_FLAG, "");
				JsonHelper<List<PhysPort>, String> jsonHelp = new JsonHelper<List<PhysPort>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<PhysPort>());
			}
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//					Message.GET_PORT, ParamConstant.PORT, getPhysPortsId(physPorts),
//					Message.SUCCESSED_FLAG, "");
			JsonHelper<List<PhysPort>, String> jsonHelp = new JsonHelper<List<PhysPort>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(physPorts);

		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_PORT, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_PORT, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_PORT_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_PORT, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/physports/{uuid}", method = RequestMethod.GET)
	public String getPhysPort(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String uuid, HttpServletResponse response) {
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
			PhysPort physPort = physPortService.getPhysPort(uuid, authToken);
			if (null == physPort) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
//				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//						Message.GET_PORT_DETAIL, ParamConstant.PORT, "", Message.FAILED_FLAG,
//						message);
				return message;
			}
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//					Message.GET_PORT_DETAIL, ParamConstant.PORT, uuid, Message.SUCCESSED_FLAG, "");
			JsonHelper<PhysPort, String> jsonHelp = new JsonHelper<PhysPort, String>();
			return jsonHelp.generateJsonBodyWithEmpty(physPort);

		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_PORT_DETAIL, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_PORT_DETAIL, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_PORT_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_PORT_DETAIL, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/physports", method = RequestMethod.POST)
	public String createPhysPort(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody,HttpServletResponse response) {
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
			PhysPort physPort = physPortService.createPhysPort(createBody, authToken);
			if (null == physPort) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.CREATE_PORT, ParamConstant.PORT, "", Message.FAILED_FLAG,
						message);
				return message;
			}
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_PORT, ParamConstant.PORT, physPort.getUuid(), Message.SUCCESSED_FLAG, "");
			JsonHelper<PhysPort, String> jsonHelp = new JsonHelper<PhysPort, String>();
			return jsonHelp.generateJsonBodyWithEmpty(physPort);

		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_PORT, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_PORT, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_PORT_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_PORT, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
//	private String getPhysPortsId(List<PhysPort> physPorts) {
//		if (Util.isNullOrEmptyList(physPorts))
//			return "";
//		List<String> ids = new ArrayList<String>();
//		for (PhysPort physPort : physPorts) {
//			ids.add(physPort.getUuid());
//		}
//		return Util.listToString(ids, ',');
//	}
}
