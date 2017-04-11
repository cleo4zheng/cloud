package com.cloud.cloudapi.controller.common;

import java.util.Locale;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Topology;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.common.TopologyService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class TopologyController {
	
	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private AuthService authService;
	
	@Resource
	private TopologyService topologyService;
	
	private Logger log = LogManager.getLogger(TopologyController.class);
	
	@RequestMapping(value = "/topology", method = RequestMethod.GET)
	public String getBareMetalPort(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,HttpServletResponse response) {
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
			Topology topology = topologyService.getTopology(authToken);
			if (null == topology) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
//				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//						Message.GET_TOPOLOGY, ParamConstant.TOPOLOGY, "", Message.FAILED_FLAG,
//						message);
				return message;
			}
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//					Message.GET_TOPOLOGY, ParamConstant.TOPOLOGY, topology.getId(), Message.SUCCESSED_FLAG, "");
			JsonHelper<Topology, String> jsonHelp = new JsonHelper<Topology, String>();
			return jsonHelp.generateJsonBodyWithEmpty(topology);

		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_TOPOLOGY, ParamConstant.TOPOLOGY, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_TOPOLOGY, ParamConstant.TOPOLOGY, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_TOPOLOGY_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_TOPOLOGY, ParamConstant.TOPOLOGY, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
}
