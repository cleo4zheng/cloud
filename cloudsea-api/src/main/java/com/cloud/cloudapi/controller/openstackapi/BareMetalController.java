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
import com.cloud.cloudapi.pojo.openstackapi.forgui.BareMetalPort;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.openstackapi.BareMetalService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
/**
 * 该controller没有被使用，待删除
 * @author tanggc
 *
 */
@RestController
public class BareMetalController {

	@Resource
	private OperationLogService operationLogService;

	@Resource
	private BareMetalService bareMetalService;

	@Resource
	private AuthService authService;

	private Logger log = LogManager.getLogger(BareMetalController.class);
	
	@RequestMapping(value = "/baremetals", method = RequestMethod.GET)
	public String getBareMetals(
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
			List<BareMetalNode> bareMetals = bareMetalService.getBareMetalNodes(paramMap, authToken);
			if (Util.isNullOrEmptyList(bareMetals)) {
//				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//						Message.GET_BAREMETAL_NODE, ParamConstant.BAREMETAL, "", Message.SUCCESSED_FLAG, "");
				JsonHelper<List<BareMetalNode>, String> jsonHelp = new JsonHelper<List<BareMetalNode>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<BareMetalNode>());
			}
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//					Message.GET_BAREMETAL_NODE, ParamConstant.BAREMETAL, getBareMetalNodesId(bareMetals),
//					Message.SUCCESSED_FLAG, "");
			JsonHelper<List<BareMetalNode>, String> jsonHelp = new JsonHelper<List<BareMetalNode>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(bareMetals);

		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_BAREMETAL_NODE, ParamConstant.BAREMETAL, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_BAREMETAL_NODE, ParamConstant.BAREMETAL, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_BAREMETAL_NODE_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_BAREMETAL_NODE, ParamConstant.BAREMETAL, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}

	@RequestMapping(value = "/baremetals/{uuid}", method = RequestMethod.GET)
	public String getBareMetal(
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
			BareMetalNode bareMetal = bareMetalService.getBareMetalNode(uuid, authToken);
			JsonHelper<BareMetalNode, String> jsonHelp = new JsonHelper<BareMetalNode, String>();
			return jsonHelp.generateJsonBodyWithEmpty(bareMetal);

		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_BAREMETAL_NODE_DETAIL, ParamConstant.BAREMETAL, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_BAREMETAL_NODE_DETAIL, ParamConstant.BAREMETAL, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_BAREMETAL_NODE_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_BAREMETAL_NODE_DETAIL, ParamConstant.BAREMETAL, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}

	@RequestMapping(value = "/baremetals", method = RequestMethod.POST)
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
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR).getResponseMessage();
		}

		try {
			BareMetalNode bareMetal = bareMetalService.createBareMetalNode(createBody, authToken);
			if (null == bareMetal) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.CREATE_BAREMETAL_NODE, ParamConstant.BAREMETAL, "", Message.FAILED_FLAG,
						message);
				return message;
			}
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_BAREMETAL_NODE, ParamConstant.BAREMETAL, bareMetal.getId(), Message.SUCCESSED_FLAG, "");
			JsonHelper<BareMetalNode, String> jsonHelp = new JsonHelper<BareMetalNode, String>();
			return jsonHelp.generateJsonBodyWithEmpty(bareMetal);

		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_BAREMETAL_NODE, ParamConstant.BAREMETAL, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_BAREMETAL_NODE, ParamConstant.BAREMETAL, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_BAREMETAL_NODE_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_BAREMETAL_NODE, ParamConstant.BAREMETAL, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	@RequestMapping(value = "/baremetal-ports", method = RequestMethod.GET)
	public String getBareMetalPorts(
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
			List<BareMetalPort> bareMetalPorts = bareMetalService.getBareMetalPorts(paramMap, authToken);
			if (Util.isNullOrEmptyList(bareMetalPorts)) {
//				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//						Message.GET_BAREMETAL_PORT, ParamConstant.BAREMETALPORT, "", Message.SUCCESSED_FLAG, "");
				JsonHelper<List<BareMetalPort>, String> jsonHelp = new JsonHelper<List<BareMetalPort>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<BareMetalPort>());
			}
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//					Message.GET_BAREMETAL_PORT, ParamConstant.BAREMETALPORT, getBareMetalPortsId(bareMetalPorts),
//					Message.SUCCESSED_FLAG, "");
			JsonHelper<List<BareMetalPort>, String> jsonHelp = new JsonHelper<List<BareMetalPort>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(bareMetalPorts);

		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_BAREMETAL_PORT, ParamConstant.BAREMETALPORT, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_BAREMETAL_PORT, ParamConstant.BAREMETALPORT, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_BAREMETAL_PORT_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_BAREMETAL_PORT, ParamConstant.BAREMETALPORT, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}

	@RequestMapping(value = "/baremetal-ports/{portId}", method = RequestMethod.GET)
	public String getBareMetalPort(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String portId, HttpServletResponse response) {
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
			BareMetalPort bareMetalPort = bareMetalService.getBareMetalPort(portId, authToken);
			JsonHelper<BareMetalPort, String> jsonHelp = new JsonHelper<BareMetalPort, String>();
			return jsonHelp.generateJsonBodyWithEmpty(bareMetalPort);

		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_BAREMETAL_PORT_DETAIL, ParamConstant.BAREMETALPORT, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_BAREMETAL_PORT_DETAIL, ParamConstant.BAREMETALPORT, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_BAREMETAL_PORT_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_BAREMETAL_PORT_DETAIL, ParamConstant.BAREMETALPORT, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}

	@RequestMapping(value = "/baremetal-ports", method = RequestMethod.POST)
	public String createBareMetalPort(
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
			BareMetalPort bareMetalPort = bareMetalService.createBareMetalPort(createBody, authToken);
			if (null == bareMetalPort) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.CREATE_BAREMETAL_PORT, ParamConstant.BAREMETALPORT, "", Message.FAILED_FLAG,
						message);
				return message;
			}
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_BAREMETAL_PORT, ParamConstant.BAREMETALPORT, bareMetalPort.getId(), Message.SUCCESSED_FLAG, "");
			JsonHelper<BareMetalPort, String> jsonHelp = new JsonHelper<BareMetalPort, String>();
			return jsonHelp.generateJsonBodyWithEmpty(bareMetalPort);

		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_BAREMETAL_PORT, ParamConstant.BAREMETALPORT, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_BAREMETAL_PORT, ParamConstant.BAREMETALPORT, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_BAREMETAL_PORT_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_BAREMETAL_PORT, ParamConstant.BAREMETALPORT, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
//	private String getBareMetalNodesId(List<BareMetalNode> bareMetalNodes) {
//		if (Util.isNullOrEmptyList(bareMetalNodes))
//			return "";
//		List<String> ids = new ArrayList<String>();
//		for (BareMetalNode bareMetalNode : bareMetalNodes) {
//			ids.add(bareMetalNode.getId());
//		}
//		return Util.listToString(ids, ',');
//	}
	
//	private String getBareMetalPortsId(List<BareMetalPort> bareMetalPorts) {
//		if (Util.isNullOrEmptyList(bareMetalPorts))
//			return "";
//		List<String> ids = new ArrayList<String>();
//		for (BareMetalPort bareMetalPort : bareMetalPorts) {
//			ids.add(bareMetalPort.getId());
//		}
//		return Util.listToString(ids, ',');
//	}
	
}
