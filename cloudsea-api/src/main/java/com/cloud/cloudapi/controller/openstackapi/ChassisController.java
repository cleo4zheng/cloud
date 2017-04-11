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
import com.cloud.cloudapi.pojo.openstackapi.forgui.BareMetalNode;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Chassis;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.openstackapi.ChassisService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class ChassisController {
	@Resource
	private OperationLogService operationLogService;

	@Resource
	private ChassisService chassisService;

	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(ChassisController.class);
	
	@RequestMapping(value = "/chassis", method = RequestMethod.GET)
	public String getChassises(
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
			log.error("error",e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR).getResponseMessage();
		}

		try {
			Map<String, String> paramMap = Util.makeRequestParamInfo(limit, name, status, "", "");
			List<Chassis> chassises = chassisService.getChassises(paramMap, authToken);
			if (Util.isNullOrEmptyList(chassises)) {
				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.GET_CHASSIS, ParamConstant.CHASSIS, "", Message.SUCCESSED_FLAG, "");
				JsonHelper<List<BareMetalNode>, String> jsonHelp = new JsonHelper<List<BareMetalNode>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<BareMetalNode>());
			}
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CHASSIS, ParamConstant.CHASSIS, getChassisesId(chassises),
					Message.SUCCESSED_FLAG, "");
			JsonHelper<List<Chassis>, String> jsonHelp = new JsonHelper<List<Chassis>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(chassises);

		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CHASSIS, ParamConstant.CHASSIS, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CHASSIS, ParamConstant.CHASSIS, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_CHASSIS_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CHASSIS, ParamConstant.CHASSIS, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	@RequestMapping(value = "/chassis/{uuid}", method = RequestMethod.GET)
	public String getChassis(
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
			log.error("error",e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR).getResponseMessage();
		}

		try {
			Chassis chassis = chassisService.getChassis(uuid, authToken);
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CHASSIS_DETAIL, ParamConstant.CHASSIS, uuid, Message.SUCCESSED_FLAG, "");
			JsonHelper<Chassis, String> jsonHelp = new JsonHelper<Chassis, String>();
			return jsonHelp.generateJsonBodyWithEmpty(chassis);

		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CHASSIS_DETAIL, ParamConstant.CHASSIS, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CHASSIS_DETAIL, ParamConstant.CHASSIS, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_CHASSIS_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CHASSIS_DETAIL, ParamConstant.CHASSIS, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	@RequestMapping(value = "/chassis", method = RequestMethod.POST)
	public String createBareMetal(
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
			log.error("error",e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR).getResponseMessage();
		}

		try {
			Chassis chassis = chassisService.createChassis(createBody, authToken);
			if (null == chassis) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.CREATE_CHASSIS, ParamConstant.CHASSIS, "", Message.FAILED_FLAG,
						message);
				return message;
			}
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_CHASSIS, ParamConstant.CHASSIS, chassis.getUuid(), Message.SUCCESSED_FLAG, "");
			JsonHelper<Chassis, String> jsonHelp = new JsonHelper<Chassis, String>();
			return jsonHelp.generateJsonBodyWithEmpty(chassis);

		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_CHASSIS, ParamConstant.CHASSIS, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_CHASSIS, ParamConstant.CHASSIS, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_CHASSIS_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_CHASSIS, ParamConstant.CHASSIS, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	private String getChassisesId(List<Chassis> chassises) {
		if (Util.isNullOrEmptyList(chassises))
			return "";
		List<String> ids = new ArrayList<String>();
		for (Chassis chassis : chassises) {
			ids.add(chassis.getUuid());
		}
		return Util.listToString(ids, ',');
	}
}
