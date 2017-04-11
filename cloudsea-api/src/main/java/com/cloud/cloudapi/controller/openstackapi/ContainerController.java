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

import com.cloud.cloudapi.controller.common.BaseController;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Container;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ContainerModel;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.openstackapi.ContainerService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class ContainerController extends BaseController{
	
	@Resource
	private OperationLogService operationLogService;

	@Resource
	private ContainerService containerService;

	private Logger log = LogManager.getLogger(ContainerController.class);
	
	//获取container一览信息
	@RequestMapping(value = "/containers", method = RequestMethod.GET)
	public String getContainers(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			HttpServletResponse response) {

		// get ostoken by cuibl
		TokenOs authToken = null;

		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String, String> paramMap = Util.makeRequestParamInfo(limit, name, status, "", "");
			List<Container> containers = containerService.getConstainers(paramMap, authToken);
			if (Util.isNullOrEmptyList(containers)) {
				JsonHelper<List<Container>, String> jsonHelp = new JsonHelper<List<Container>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Container>());
			}
			JsonHelper<List<Container>, String> jsonHelp = new JsonHelper<List<Container>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(containers);

		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CONTAINER, ParamConstant.CONTAINER, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CONTAINER, ParamConstant.CONTAINER, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_CONTAINER_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CONTAINER, ParamConstant.CONTAINER, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	//获取container详细信息
	@RequestMapping(value = "/containers/{id}", method = RequestMethod.GET)
	public String getContainer(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		// get ostoken by cuibl
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Container container = containerService.getContainer(id, authToken);
			JsonHelper<Container, String> jsonHelp = new JsonHelper<Container, String>();
			return jsonHelp.generateJsonBodyWithEmpty(container);

		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CONTAINER_DETAIL, ParamConstant.CONTAINER, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CONTAINER_DETAIL, ParamConstant.CONTAINER, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_CONTAINER_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CONTAINER_DETAIL, ParamConstant.CONTAINER, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	//创建container
	@RequestMapping(value = "/containers", method = RequestMethod.POST)
	public String createContainer(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody,HttpServletResponse response) {
		// get ostoken by cuibl
		TokenOs authToken = null;

		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.CONTAINER_NEW);
			Container container = containerService.createContainerNew(createBody, authToken);
			if (null == container) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.CREATE_CONTAINER, ParamConstant.CONTAINER, "", Message.FAILED_FLAG,
						message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_CONTAINER, ParamConstant.CONTAINER, container.getUuid(), Message.SUCCESSED_FLAG, "");
			JsonHelper<Container, String> jsonHelp = new JsonHelper<Container, String>();
			return jsonHelp.generateJsonBodyWithEmpty(container);

		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_CONTAINER, ParamConstant.CONTAINER, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR);
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_CONTAINER, ParamConstant.CONTAINER, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_CONTAINER_CREATE_FAILED);
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_CONTAINER, ParamConstant.CONTAINER, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	//删除container
	@RequestMapping(value = "/containers/{id}", method = RequestMethod.DELETE)
	public String deleteContainer(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		// get ostoken by cuibl
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.CONTAINER_DELETE);
			containerService.deleteContainer(id, authToken);
			if(null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_CONTAINER_DELETE_SUCCESSED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_CONTAINER, ParamConstant.CONTAINER, id, Message.SUCCESSED_FLAG, message);
			return message;
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_CONTAINER, ParamConstant.CONTAINER, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_CONTAINER, ParamConstant.CONTAINER, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_CONTAINER_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_CONTAINER, ParamConstant.CONTAINER, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	
	//获取container model一览信息
	@RequestMapping(value = "/containerModels", method = RequestMethod.GET)
	public String getContainerModels(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			HttpServletResponse response) {

		// get ostoken by cuibl
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String, String> paramMap = Util.makeRequestParamInfo(limit, name, status, "", "");
			List<ContainerModel> containers = containerService.getContainerModels(paramMap, authToken);
			if (Util.isNullOrEmptyList(containers)) {
				JsonHelper<List<ContainerModel>, String> jsonHelp = new JsonHelper<List<ContainerModel>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<ContainerModel>());
			}
			JsonHelper<List<ContainerModel>, String> jsonHelp = new JsonHelper<List<ContainerModel>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(containers);

		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CONTAINER_MODEL, ParamConstant.CONTAINER_MODEL, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CONTAINER_MODEL, ParamConstant.CONTAINER_MODEL, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_CONTAINER_MODEL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CONTAINER_MODEL, ParamConstant.CONTAINER_MODEL, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	//获取container model详细信息
	@RequestMapping(value = "/containerModels/{id}", method = RequestMethod.GET)
	public String getContainerModel(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		// get ostoken by cuibl
		TokenOs authToken = null;

		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			ContainerModel container = containerService.getContainerModel(id, authToken);
			JsonHelper<ContainerModel, String> jsonHelp = new JsonHelper<ContainerModel, String>();
			return jsonHelp.generateJsonBodyWithEmpty(container);

		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CONTAINER_MODEL_DETAIL, ParamConstant.CONTAINER_MODEL, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CONTAINER_MODEL_DETAIL, ParamConstant.CONTAINER_MODEL, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_CONTAINER_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CONTAINER_MODEL_DETAIL, ParamConstant.CONTAINER_MODEL, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	//新建container model
	@RequestMapping(value = "/containerModels", method = RequestMethod.POST)
	public String createContainerModels(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {

		// get ostoken by cuibl
		TokenOs authToken = null;

		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.CONTAINER_UPDATE);
			ContainerModel container = containerService.createContainerModel(createBody, authToken);
			JsonHelper<ContainerModel, String> jsonHelp = new JsonHelper<ContainerModel, String>();
			return jsonHelp.generateJsonBodyWithEmpty(container);

		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CONTAINER_MODEL_DETAIL, ParamConstant.CONTAINER_MODEL, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CONTAINER_MODEL_DETAIL, ParamConstant.CONTAINER_MODEL, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_CONTAINER_MODEL_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_CONTAINER_MODEL_DETAIL, ParamConstant.CONTAINER_MODEL, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	@RequestMapping(value = "/containerModels/{id}", method = RequestMethod.DELETE)
	public String deleteContainerModel(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		// get ostoken by cuibl
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.CONTAINER_UPDATE);
			containerService.deleteContainerModel(id, authToken);
			if(null != response)
				response.setStatus(ParamConstant.NORMAL_DELETE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_CONTAINER_DELETE_SUCCESSED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_CONTAINER, ParamConstant.CONTAINER, id, Message.SUCCESSED_FLAG, message);
			return message;
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_CONTAINER, ParamConstant.CONTAINER, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_CONTAINER, ParamConstant.CONTAINER, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_CONTAINER_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_CONTAINER, ParamConstant.CONTAINER, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
//	private String getContainersId(List<Container> containers) {
//		if (Util.isNullOrEmptyList(containers))
//			return "";
//		List<String> ids = new ArrayList<String>();
//		for (Container container : containers) {
//			ids.add(container.getUuid());
//		}
//		return Util.listToString(ids, ',');
//	}
	
}
