package com.cloud.cloudapi.controller.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.AggregationInfo;
import com.cloud.cloudapi.pojo.openstackapi.forgui.EnvResource;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceCreateProcess;
import com.cloud.cloudapi.service.openstackapi.HostService;
import com.cloud.cloudapi.service.openstackapi.ResourceManagerService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class ResourceManagerController  extends BaseController {

	@Resource
	private ResourceManagerService resourceManagerService;
	
	@Resource
	private HostService hostService;
	
	private Logger log = LogManager.getLogger(ResourceManagerController.class);
	
	@RequestMapping(value = "/aggregation-info", method = RequestMethod.GET)
	public String getAggregationInfos(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			HttpServletResponse response) {
		
		//get ostoken by cuibl
		TokenOs authToken=null;
		try{
		authToken = getAuthService().insertCheckGuiAndOsTokenByEncrypt(guiToken);
		} catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return e.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR).getResponseMessage();
		}
		
		try {
			List<AggregationInfo> aggregationInfos = resourceManagerService.getAggregationInfos(authToken);
			if (Util.isNullOrEmptyList(aggregationInfos)) {
	//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_AGGREGATION,ParamConstant.AGGREGATION,"",Message.SUCCESSED_FLAG,"");
				JsonHelper<List<AggregationInfo>, String> jsonHelp = new JsonHelper<List<AggregationInfo>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<AggregationInfo>());
			}
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_AGGREGATION,ParamConstant.AGGREGATION,getAggregationInfosId(aggregationInfos),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<AggregationInfo>, String> jsonHelp = new JsonHelper<List<AggregationInfo>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(aggregationInfos);

		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
		//	this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.GET_AGGREGATION,ParamConstant.AGGREGATION,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_AGGREGATION,ParamConstant.AGGREGATION,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_AGGREGATION_INFO_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_AGGREGATION,ParamConstant.AGGREGATION,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	@RequestMapping(value = "/total-resources", method = RequestMethod.GET)
	public String getEnvTotalResource(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name, HttpServletResponse response) {
		
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			EnvResource envResource = hostService.getTotalResource(null, authToken);
			if (null == envResource) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
	//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_ENV_RESOURCE,ParamConstant.RESOURCE,"",Message.FAILED_FLAG,message);
				return message;
			}
			envResource.normalInfo(new Locale(authToken.getLocale()));
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_ENV_RESOURCE,ParamConstant.RESOURCE,envResource.getId(),Message.SUCCESSED_FLAG,"");
			JsonHelper<EnvResource, String> jsonHelp = new JsonHelper<EnvResource, String>();
			return jsonHelp.generateJsonBodyWithEmpty(envResource);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message =e.getResponseMessage();
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.GET_ENV_RESOURCE,ParamConstant.RESOURCE,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message =exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_ENV_RESOURCE,ParamConstant.RESOURCE,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_ENV_RESOURCE_GET_FAILED,new Locale(authToken.getLocale()));
			String message =exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_ENV_RESOURCE,ParamConstant.RESOURCE,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	@RequestMapping(value = "/resource-state", method = RequestMethod.GET)
	public String getResourceCreateProcessInfo(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			HttpServletResponse response) {
		// get ostoken by cuibl
		TokenOs authToken = null;
		try {
			authToken = getAuthService().insertCheckGuiAndOsTokenByEncrypt(guiToken);
		} catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return e.getResponseMessage();
		} catch (Exception e) {
			log.error("error",e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR).getResponseMessage();
		}
		try {
			List<ResourceCreateProcess> resources = resourceManagerService.getResourceCreateProcesses(authToken);
			if (Util.isNullOrEmptyList(resources)) {
				JsonHelper<List<ResourceCreateProcess>, String> jsonHelp = new JsonHelper<List<ResourceCreateProcess>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<ResourceCreateProcess>());
			}
			JsonHelper<List<ResourceCreateProcess>, String> jsonHelp = new JsonHelper<List<ResourceCreateProcess>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(resources);
		} catch (Exception e) {
			log.error("error",e);
			JsonHelper<List<ResourceCreateProcess>, String> jsonHelp = new JsonHelper<List<ResourceCreateProcess>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<ResourceCreateProcess>());
		}
	}

}
