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
import com.cloud.cloudapi.pojo.openstackapi.forgui.LBHealthMonitor;
import com.cloud.cloudapi.pojo.openstackapi.forgui.LBPool;
import com.cloud.cloudapi.pojo.openstackapi.forgui.LBPoolMember;
import com.cloud.cloudapi.pojo.openstackapi.forgui.LBVip;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Listener;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Loadbalancer;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.openstackapi.LoadbalancerService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class LoadblancerController  extends BaseController {
	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private LoadbalancerService loadblanceService;

	private Logger log = LogManager.getLogger(LoadblancerController.class);
	
	@RequestMapping(value = "/loadbalancers", method = RequestMethod.GET)
	public String getLoadblancers(
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
			Map<String, String> paramMap = null;
			if (!"".equals(limit)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.LIMIT, limit);
			}
			if (!"".equals(name)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.NAME, name);
			}

			List<Loadbalancer> loadbalancers = loadblanceService.getLoadbalancers(paramMap, authToken);
			if (Util.isNullOrEmptyList(loadbalancers)) {
				JsonHelper<List<Loadbalancer>, String> jsonHelp = new JsonHelper<List<Loadbalancer>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Loadbalancer>());
			}
			normalLoadbalancerInfo(loadbalancers);
			JsonHelper<List<Loadbalancer>, String> jsonHelp = new JsonHelper<List<Loadbalancer>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(loadbalancers);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_LOADBLANCER_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/loadbalancers/{id}", method = RequestMethod.GET)
	public String getLoadblancer(
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
			Loadbalancer loadbalancer = loadblanceService.getLoadbalancer(id, authToken);
			if (null == loadbalancer) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				return message;
			}
			loadbalancer.normalInfo(false);
			JsonHelper<Loadbalancer, String> jsonHelp = new JsonHelper<Loadbalancer, String>();
			return jsonHelp.generateJsonBodyWithEmpty(loadbalancer);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LOADBLANCER_DETAIL, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LOADBLANCER_DETAIL, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_LOADBLANCER_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LOADBLANCER_DETAIL, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/loadbalancers", method = RequestMethod.POST)
	public String createLoadblancer(
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
			this.checkUserPermission(authToken, ParamConstant.LOADBALANCER_NEW);
			Loadbalancer loadbalancer = loadblanceService.createLoadbalancer(createBody, authToken);
			if (null == loadbalancer) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.CREATE_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_LOADBLANCER, ParamConstant.LOADBALANCER, loadbalancer.getId(), Message.SUCCESSED_FLAG,
					"");
			loadbalancer.normalInfo(true);
			JsonHelper<Loadbalancer, String> jsonHelp = new JsonHelper<Loadbalancer, String>();
			return jsonHelp.generateJsonBodyWithEmpty(loadbalancer);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_LOADBLANCER_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/loadbalancers/{id}", method = RequestMethod.PUT)
	public String updateLoadblancer(
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
			Boolean updateState = this.checkUpdateAdminState(updateBody);
			if(null == updateState){
				this.checkUserPermission(authToken, ParamConstant.LOADBALANCER_UPDATE);
			}else if(true == updateState){
				this.checkUserPermission(authToken, ParamConstant.LOADBALANCER_ENABLE);
			}else{
				this.checkUserPermission(authToken, ParamConstant.LOADBALANCER_DISABLE);
			}
			Loadbalancer loadbalancer = loadblanceService.updateLoadbalancer(id,updateBody, authToken);
			if (null == loadbalancer) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.UPDATE_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_LOADBLANCER, ParamConstant.LOADBALANCER, loadbalancer.getId(), Message.SUCCESSED_FLAG,
					"");
			loadbalancer.normalInfo(false);
			JsonHelper<Loadbalancer, String> jsonHelp = new JsonHelper<Loadbalancer, String>();
			return jsonHelp.generateJsonBodyWithEmpty(loadbalancer);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_LOADBLANCER, ParamConstant.LOADBALANCER, id, Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_LOADBLANCER_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/loadbalancers/{id}/addFloatingIp", method = RequestMethod.PUT)
	public String addLoadblancerFloatingIP(
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
			this.checkUserPermission(authToken, ParamConstant.LOADBALANCER_ADD_FLOATINGIP);
			Loadbalancer loadbalancer = loadblanceService.bindingFloatingIP(id,updateBody, authToken);
			if (null == loadbalancer) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.UPDATE_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_LOADBLANCER, ParamConstant.LOADBALANCER, loadbalancer.getId(), Message.SUCCESSED_FLAG,
					"");
			loadbalancer.normalInfo(false);
			JsonHelper<Loadbalancer, String> jsonHelp = new JsonHelper<Loadbalancer, String>();
			return jsonHelp.generateJsonBodyWithEmpty(loadbalancer);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_LOADBLANCER, ParamConstant.LOADBALANCER, id, Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_LOADBLANCER_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/loadbalancers/{id}/removeFloatingIp", method = RequestMethod.PUT)
	public String removeLoadblancerFloatingIP(
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
			this.checkUserPermission(authToken, ParamConstant.LOADBALANCER_ADD_FLOATINGIP);
			Loadbalancer loadbalancer = loadblanceService.removeFloatingIP(id,updateBody, authToken);
			if (null == loadbalancer) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.UPDATE_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_LOADBLANCER, ParamConstant.LOADBALANCER, loadbalancer.getId(), Message.SUCCESSED_FLAG,
					"");
			loadbalancer.normalInfo(false);
			JsonHelper<Loadbalancer, String> jsonHelp = new JsonHelper<Loadbalancer, String>();
			return jsonHelp.generateJsonBodyWithEmpty(loadbalancer);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_LOADBLANCER, ParamConstant.LOADBALANCER, id, Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_LOADBLANCER_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/loadbalancers/{id}", method = RequestMethod.DELETE)
	public String deleteLoadblancer(
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
			this.checkUserPermission(authToken, ParamConstant.LOADBALANCER_DELETE);
			loadblanceService.deleteLoadbalancer(id, authToken);
			if(null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_LOADBLANCER, ParamConstant.LOADBALANCER, id,
					Message.SUCCESSED_FLAG, "");
			JsonHelper<Loadbalancer, String> jsonHelp = new JsonHelper<Loadbalancer, String>();
			return jsonHelp.generateJsonBodyWithEmpty(new Loadbalancer());
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_LOADBLANCER_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_LOADBLANCER, ParamConstant.LOADBALANCER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/listeners", method = RequestMethod.GET)
	public String getListeners(
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
			Map<String, String> paramMap = null;
			if (!"".equals(limit)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.LIMIT, limit);
			}
			if (!"".equals(name)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.NAME, name);
			}

			List<Listener> listeners = loadblanceService.getListeners(paramMap, authToken);
			if (Util.isNullOrEmptyList(listeners)) {
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.GET_LISTENER, ParamConstant.LISTENER, "", Message.SUCCESSED_FLAG, "");
				JsonHelper<List<Listener>, String> jsonHelp = new JsonHelper<List<Listener>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Listener>());
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LISTENER, ParamConstant.LISTENER, getListenersId(listeners), Message.SUCCESSED_FLAG,
					"");
			JsonHelper<List<Listener>, String> jsonHelp = new JsonHelper<List<Listener>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(listeners);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LISTENER, ParamConstant.LISTENER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LISTENER, ParamConstant.LISTENER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_LISTENER_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LISTENER, ParamConstant.LISTENER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/listeners/{id}", method = RequestMethod.GET)
	public String getListener(
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
			Listener listener = loadblanceService.getListener(id, authToken);
			if (null == listener) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.GET_LISTENER_DETAIL, ParamConstant.LISTENER, "", Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LISTENER_DETAIL, ParamConstant.LISTENER, listener.getId(), Message.SUCCESSED_FLAG, "");
			JsonHelper<Listener, String> jsonHelp = new JsonHelper<Listener, String>();
			return jsonHelp.generateJsonBodyWithEmpty(listener);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LISTENER_DETAIL, ParamConstant.LISTENER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LISTENER_DETAIL, ParamConstant.LISTENER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_LISTENER_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LISTENER_DETAIL, ParamConstant.LISTENER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/listeners", method = RequestMethod.POST)
	public String createListener(
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
			Listener listener = loadblanceService.buildListener(createBody, authToken);
			if (null == listener) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.CREATE_LISTENER, ParamConstant.LISTENER, "", Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_LISTENER, ParamConstant.LISTENER, listener.getId(), Message.SUCCESSED_FLAG, "");
			JsonHelper<Listener, String> jsonHelp = new JsonHelper<Listener, String>();
			return jsonHelp.generateJsonBodyWithEmpty(listener);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_LISTENER, ParamConstant.LISTENER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_LISTENER, ParamConstant.LISTENER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_LISTENER_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_LISTENER, ParamConstant.LISTENER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/lbpools", method = RequestMethod.GET)
	public String getPools(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
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
			Map<String, String> paramMap = null;
			if (!"".equals(limit)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.LIMIT, limit);
			}
			if (!"".equals(name)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.NAME, name);
			}

			List<LBPool> pools = loadblanceService.getPools(paramMap, authToken);
			if (Util.isNullOrEmptyList(pools)) {
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.GET_LBPOOL, ParamConstant.LBPOOL, "", Message.SUCCESSED_FLAG, "");
				JsonHelper<List<Listener>, String> jsonHelp = new JsonHelper<List<Listener>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Listener>());
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LBPOOL, ParamConstant.LBPOOL, getPoolsId(pools), Message.SUCCESSED_FLAG, "");
			JsonHelper<List<LBPool>, String> jsonHelp = new JsonHelper<List<LBPool>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(pools);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LBPOOL, ParamConstant.LBPOOL, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LBPOOL, ParamConstant.LBPOOL, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_POOL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LBPOOL, ParamConstant.LBPOOL, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/lbpools/{id}", method = RequestMethod.GET)
	public String getPool(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			LBPool pool = loadblanceService.getPool(id, authToken);
			if (null == pool) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.GET_LBPOOL_DETAIL, ParamConstant.LBPOOL, "", Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LBPOOL_DETAIL, ParamConstant.LBPOOL, pool.getId(), Message.SUCCESSED_FLAG, "");
			JsonHelper<LBPool, String> jsonHelp = new JsonHelper<LBPool, String>();
			return jsonHelp.generateJsonBodyWithEmpty(pool);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LBPOOL_DETAIL, ParamConstant.LBPOOL, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LBPOOL_DETAIL, ParamConstant.LBPOOL, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_POOL_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LBPOOL_DETAIL, ParamConstant.LBPOOL, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/lbpools", method = RequestMethod.POST)
	public String createPool(
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
			LBPool pool = loadblanceService.buildPool(createBody, authToken);
			if (null == pool) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.CREATE_LB_POOL, ParamConstant.LBPOOL, "", Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_LB_POOL, ParamConstant.LBPOOL, pool.getId(), Message.SUCCESSED_FLAG, "");
			JsonHelper<LBPool, String> jsonHelp = new JsonHelper<LBPool, String>();
			return jsonHelp.generateJsonBodyWithEmpty(pool);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_LB_POOL, ParamConstant.LBPOOL, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_LB_POOL, ParamConstant.LBPOOL, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_POOL_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_LB_POOL, ParamConstant.LBPOOL, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/lbpools/{id}/members", method = RequestMethod.GET)
	public String getPoolMembers(
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
			List<LBPoolMember> poolMembers = loadblanceService.getPoolMembers(id, authToken);
			if (Util.isNullOrEmptyList(poolMembers)) {
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.GET_LBPOOL_MEMBER, ParamConstant.LBPOOL_MEMBER, "", Message.SUCCESSED_FLAG, "");
				JsonHelper<List<Listener>, String> jsonHelp = new JsonHelper<List<Listener>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Listener>());
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LBPOOL_MEMBER, ParamConstant.LBPOOL_MEMBER, getPoolMembersId(poolMembers),
					Message.SUCCESSED_FLAG, "");
			JsonHelper<List<LBPoolMember>, String> jsonHelp = new JsonHelper<List<LBPoolMember>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(poolMembers);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LBPOOL_MEMBER, ParamConstant.LBPOOL_MEMBER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LBPOOL_MEMBER, ParamConstant.LBPOOL_MEMBER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_POOL_MEMBER_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LBPOOL_MEMBER, ParamConstant.LBPOOL_MEMBER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/lbpools/{id}/members/{memberId}", method = RequestMethod.GET)
	public String getPoolMember(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @PathVariable String memberId, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			LBPoolMember poolMember = loadblanceService.getPoolMember(id, memberId, authToken);
			if (null == poolMember) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.GET_LBPOOL_MEMBER_DETAIL, ParamConstant.LBPOOL_MEMBER, "", Message.FAILED_FLAG,
						message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LBPOOL_MEMBER_DETAIL, ParamConstant.LBPOOL_MEMBER, poolMember.getId(),
					Message.SUCCESSED_FLAG, "");
			JsonHelper<LBPoolMember, String> jsonHelp = new JsonHelper<LBPoolMember, String>();
			return jsonHelp.generateJsonBodyWithEmpty(poolMember);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LBPOOL_MEMBER_DETAIL, ParamConstant.LBPOOL_MEMBER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LBPOOL_MEMBER_DETAIL, ParamConstant.LBPOOL_MEMBER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_POOL_MEMBER_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_LBPOOL_MEMBER_DETAIL, ParamConstant.LBPOOL_MEMBER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/lbpools/{id}/members", method = RequestMethod.POST)
	public String addPoolMember(
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
			LBPoolMember poolMember = loadblanceService.addPoolMember(id, createBody, authToken);
			if (null == poolMember) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.CREATE_LB_POOL_MEMBER, ParamConstant.LBPOOL_MEMBER, "", Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_LB_POOL_MEMBER, ParamConstant.LBPOOL_MEMBER, poolMember.getId(),
					Message.SUCCESSED_FLAG, "");
			JsonHelper<LBPoolMember, String> jsonHelp = new JsonHelper<LBPoolMember, String>();
			return jsonHelp.generateJsonBodyWithEmpty(poolMember);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_LB_POOL_MEMBER, ParamConstant.LBPOOL_MEMBER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_LB_POOL_MEMBER, ParamConstant.LBPOOL_MEMBER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_POOL_MEMBER_ADD_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_LB_POOL_MEMBER, ParamConstant.LBPOOL_MEMBER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/lbpools/{id}/members/{memberId}", method = RequestMethod.DELETE)
	public String removePoolMember(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @PathVariable String memberId, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			loadblanceService.removePoolMember(id, memberId, authToken);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_LB_POOL_MEMBER, ParamConstant.LBPOOL_MEMBER, memberId, Message.SUCCESSED_FLAG, "");
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_POOL_MEMBER_REMOVE_SUCCESSED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			return message;
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_LB_POOL_MEMBER, ParamConstant.LBPOOL_MEMBER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_LB_POOL_MEMBER, ParamConstant.LBPOOL_MEMBER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_POOL_MEMBER_REMOVE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_LB_POOL_MEMBER, ParamConstant.LBPOOL_MEMBER, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/healthMonitors", method = RequestMethod.GET)
	public String getHealthMonitors(
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
			Map<String, String> paramMap = null;
			if (!"".equals(limit)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.LIMIT, limit);
			}
			if (!"".equals(name)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.NAME, name);
			}

			List<LBHealthMonitor> healthMonitors = loadblanceService.getHealthMonitors(paramMap, authToken);
			if (Util.isNullOrEmptyList(healthMonitors)) {
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.GET_HEALTH_MONITOR, ParamConstant.HEALTH_MONITOR, "", Message.SUCCESSED_FLAG, "");
				JsonHelper<List<Listener>, String> jsonHelp = new JsonHelper<List<Listener>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Listener>());
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_HEALTH_MONITOR, ParamConstant.HEALTH_MONITOR, getHealthMonitorsId(healthMonitors),
					Message.SUCCESSED_FLAG, "");
			JsonHelper<List<LBHealthMonitor>, String> jsonHelp = new JsonHelper<List<LBHealthMonitor>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(healthMonitors);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_HEALTH_MONITOR, ParamConstant.HEALTH_MONITOR, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_HEALTH_MONITOR, ParamConstant.HEALTH_MONITOR, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_HEALTH_MONITOR_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_HEALTH_MONITOR, ParamConstant.HEALTH_MONITOR, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/healthMonitors/{id}", method = RequestMethod.GET)
	public String getHealthMonitor(
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
			LBHealthMonitor healthMonitor = loadblanceService.getHealthMonitor(id,authToken);
			if (null == healthMonitor) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.GET_HEALTH_MONITOR_DETAIL, ParamConstant.HEALTH_MONITOR, "", Message.FAILED_FLAG,
						message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_HEALTH_MONITOR_DETAIL, ParamConstant.HEALTH_MONITOR, healthMonitor.getId(),
					Message.SUCCESSED_FLAG, "");
			JsonHelper<LBHealthMonitor, String> jsonHelp = new JsonHelper<LBHealthMonitor, String>();
			return jsonHelp.generateJsonBodyWithEmpty(healthMonitor);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_HEALTH_MONITOR_DETAIL, ParamConstant.HEALTH_MONITOR, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_HEALTH_MONITOR_DETAIL, ParamConstant.HEALTH_MONITOR, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_HEALTH_MONITOR_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_HEALTH_MONITOR_DETAIL, ParamConstant.HEALTH_MONITOR, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/healthMonitors", method = RequestMethod.POST)
	public String createHealthMonitor(
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
			LBHealthMonitor healthMonitor = loadblanceService.buildHealthMonitor(createBody, authToken);
			if (null == healthMonitor) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.CREATE_HEALTH_MONITOR, ParamConstant.HEALTH_MONITOR, "", Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_HEALTH_MONITOR, ParamConstant.HEALTH_MONITOR, healthMonitor.getId(),
					Message.SUCCESSED_FLAG, "");
			JsonHelper<LBHealthMonitor, String> jsonHelp = new JsonHelper<LBHealthMonitor, String>();
			return jsonHelp.generateJsonBodyWithEmpty(healthMonitor);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_HEALTH_MONITOR, ParamConstant.HEALTH_MONITOR, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_HEALTH_MONITOR, ParamConstant.HEALTH_MONITOR, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_HEALTH_MONITOR_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_HEALTH_MONITOR, ParamConstant.HEALTH_MONITOR, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/vips", method = RequestMethod.GET)
	public String getVips(
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
			Map<String, String> paramMap = null;
			if (!"".equals(limit)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.LIMIT, limit);
			}
			if (!"".equals(name)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.NAME, name);
			}

			List<LBVip> vips = loadblanceService.getVips(paramMap, authToken);
			if (Util.isNullOrEmptyList(vips)) {
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.GET_VIP, ParamConstant.LBVIP, "", Message.SUCCESSED_FLAG, "");
				JsonHelper<List<LBVip>, String> jsonHelp = new JsonHelper<List<LBVip>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<LBVip>());
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_VIP, ParamConstant.LBVIP, getVipsId(vips),
					Message.SUCCESSED_FLAG, "");
			JsonHelper<List<LBVip>, String> jsonHelp = new JsonHelper<List<LBVip>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(vips);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_VIP, ParamConstant.LBVIP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_VIP, ParamConstant.LBVIP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_VIP_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_VIP, ParamConstant.LBVIP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/vips/{id}", method = RequestMethod.GET)
	public String getVip(
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
			LBVip vip = loadblanceService.getVip(id,authToken);
			if (null == vip) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.GET_VIP_DETAIL, ParamConstant.LBVIP, "", Message.FAILED_FLAG,
						message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_VIP_DETAIL, ParamConstant.LBVIP, vip.getId(),
					Message.SUCCESSED_FLAG, "");
			JsonHelper<LBVip, String> jsonHelp = new JsonHelper<LBVip, String>();
			return jsonHelp.generateJsonBodyWithEmpty(vip);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_VIP_DETAIL, ParamConstant.LBVIP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_VIP_DETAIL, ParamConstant.LBVIP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_VIP_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_VIP_DETAIL, ParamConstant.LBVIP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/vips", method = RequestMethod.POST)
	public String createVip(
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
			LBVip vip = loadblanceService.createVip(createBody, authToken);
			if (null == vip) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.CREATE_VIP, ParamConstant.LBVIP, "", Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_VIP, ParamConstant.LBVIP, vip.getId(),
					Message.SUCCESSED_FLAG, "");
			JsonHelper<LBVip, String> jsonHelp = new JsonHelper<LBVip, String>();
			return jsonHelp.generateJsonBodyWithEmpty(vip);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_VIP, ParamConstant.LBVIP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_VIP, ParamConstant.LBVIP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_VIP_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_VIP, ParamConstant.LBVIP, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	private String getListenersId(List<Listener> listeners) {
		if (Util.isNullOrEmptyList(listeners))
			return "";
		List<String> ids = new ArrayList<String>();
		for (Listener listener : listeners) {
			ids.add(listener.getId());
		}
		return Util.listToString(ids, ',');
	}

	private String getPoolsId(List<LBPool> pools) {
		if (Util.isNullOrEmptyList(pools))
			return "";
		List<String> ids = new ArrayList<String>();
		for (LBPool pool : pools) {
			ids.add(pool.getId());
		}
		return Util.listToString(ids, ',');
	}

	private String getPoolMembersId(List<LBPoolMember> poolMembers) {
		if (Util.isNullOrEmptyList(poolMembers))
			return "";
		List<String> ids = new ArrayList<String>();
		for (LBPoolMember poolMember : poolMembers) {
			ids.add(poolMember.getId());
		}
		return Util.listToString(ids, ',');
	}

	private String getHealthMonitorsId(List<LBHealthMonitor> healthMonitors) {
		if (Util.isNullOrEmptyList(healthMonitors))
			return "";
		List<String> ids = new ArrayList<String>();
		for (LBHealthMonitor healthMonitor : healthMonitors) {
			ids.add(healthMonitor.getId());
		}
		return Util.listToString(ids, ',');
	}
	
	private String getVipsId(List<LBVip> vips) {
		if (Util.isNullOrEmptyList(vips))
			return "";
		List<String> ids = new ArrayList<String>();
		for (LBVip vip : vips) {
			ids.add(vip.getId());
		}
		return Util.listToString(ids, ',');
	}
	
	private void normalLoadbalancerInfo(List<Loadbalancer> loadbalancers){
		if(Util.isNullOrEmptyList(loadbalancers))
			return;
		for(Loadbalancer loadbalancer : loadbalancers){
			loadbalancer.normalInfo(true);
		}
	}
	
}
