package com.cloud.cloudapi.controll.monitor;

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
import com.cloud.cloudapi.pojo.monitor.MonitorTemplate;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Monitor;
import com.cloud.cloudapi.service.common.CloudUserService;
import com.cloud.cloudapi.service.common.MonitorService;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class MonitorController  extends BaseController {
	
	@Resource
	private MonitorService monitorService;

	@Resource
	private CloudUserService cloudUserService;
	
	@Resource
	private OperationLogService operationLogService;
	
	private Logger log = LogManager.getLogger(MonitorController.class);

		
	@RequestMapping(value = "/alarms", method = RequestMethod.GET)
	public String getMonitorList(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			@RequestParam(value = ParamConstant.TYPE, defaultValue = "") String type,
			@RequestParam(value = ParamConstant.RESOURCE, defaultValue = "") String resource,
			HttpServletResponse response) {		

		TokenOs ostoken = null;
		try {
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String, String> paramMap = Util.makeRequestParamInfo(limit,name,status,ParamConstant.RESOURCE,resource);
			
			if (!"".equals(type)){
				paramMap.put(ParamConstant.TYPE, type);
			}
			
			List<Monitor> monitorList =  monitorService.getMonitorListPage(paramMap, ostoken);
	//		operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_MONITORS,ParamConstant.MONITOR,getMonitorsId(monitorList),Message.SUCCESSED_FLAG,"");
			
			if(Util.isNullOrEmptyList(monitorList)){
				JsonHelper<List<Monitor>, String> jsonHelp = new JsonHelper<List<Monitor>, String>();
	    		return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Monitor>());
			}
			
			JsonHelper<List<Monitor>, String> jsonHelp = new JsonHelper<List<Monitor>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(monitorList);
			
		} catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			String message = e.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_MONITORS,ParamConstant.MONITOR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			String message = exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_MONITORS,ParamConstant.MONITOR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_SELECT_0001",new Locale(ostoken.getLocale()));
			String message = exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_MONITORS,ParamConstant.MONITOR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/alarms/{id}", method = RequestMethod.GET)
	public String getMonitor(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@PathVariable String id,
			HttpServletResponse response) {

		TokenOs ostoken = null;
		try {
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Monitor monitor =  monitorService.getMonitorById(id,ostoken);
			if(null == monitor){
				ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_SHOW_0002",new Locale(ostoken.getLocale()));
				throw exception;
			}
	//		operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_MONITOR,ParamConstant.MONITOR,monitor.getId(),Message.SUCCESSED_FLAG,"");
			JsonHelper<Monitor, String> jsonHelp = new JsonHelper<Monitor, String>();
			return jsonHelp.generateJsonBodyWithEmpty(monitor);
			
		} catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			String message = e.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_MONITOR,ParamConstant.MONITOR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			String message = exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_MONITOR,ParamConstant.MONITOR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_SHOW_0001",new Locale(ostoken.getLocale()));
			String message = exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_MONITOR,ParamConstant.MONITOR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/alarms", method = RequestMethod.POST)
	public String createMonitor(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@RequestBody String createBody, HttpServletResponse response){
		
		TokenOs ostoken = null;
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(ostoken, ParamConstant.ALARM_NEW);
			monitorService.createMonitor(createBody, ostoken);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.CREATE_MONITOR,ParamConstant.MONITOR,"",Message.SUCCESSED_FLAG,"");
			return Message.getMessage("CS_MONITOR_CREATE_0002",new Locale(ostoken.getLocale()));
			
		}catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message =  e.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.CREATE_MONITOR,ParamConstant.MONITOR,"",Message.FAILED_FLAG,"");
			log.error(message,e);
			return message;
		}  catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.CREATE_MONITOR,ParamConstant.MONITOR,"",Message.FAILED_FLAG,"");
			log.error(message,e);
			return message;
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_CREATE_0001",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.CREATE_MONITOR,ParamConstant.MONITOR,"",Message.FAILED_FLAG,"");
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/alarms/{id}", method = RequestMethod.PUT)
	public String updateMonitor(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@PathVariable String id,
			@RequestBody String updateBody, HttpServletResponse response){
		
		TokenOs ostoken = null;
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(ostoken, ParamConstant.ALARM_UPDATE);
			monitorService.updateMonitor(updateBody, ostoken);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR,id,Message.SUCCESSED_FLAG,"");
            return Message.getMessage("CS_MONITOR_UPDATE_0002",new Locale(ostoken.getLocale()));
			
		}catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			String message =  e.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR,id,Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR,id,Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_UPDATE_0001",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR,id,Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}

	}
	
	@RequestMapping(value = "/alarms/{monitorId}/resources", method = RequestMethod.POST)
	public String addResource(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@PathVariable String monitorId,
			@RequestBody String addBody, HttpServletResponse response){
		
		TokenOs ostoken = null;
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(ostoken, ParamConstant.ALARM_ADD_RESOURCE);
			monitorService.addResource(addBody, ostoken);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.SUCCESSED_FLAG,"");
            return Message.getMessage("CS_MONITOR_UPDATE_0002",new Locale(ostoken.getLocale()));	
		}catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			String message =  e.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}  catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_UPDATE_0001",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}

	}
	
	@RequestMapping(value = "/alarms/{monitorId}/resources/{resourceId}", method = RequestMethod.DELETE)
	public String deleteResource(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@PathVariable String monitorId,
			@PathVariable String resourceId, HttpServletResponse response){
		
		TokenOs ostoken = null;
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(ostoken, ParamConstant.ALARM_REMOVE_RESOURCE);
			monitorService.deleteResource(monitorId, resourceId, ostoken);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.DELETE_MONITOR,ParamConstant.MONITOR, monitorId, Message.SUCCESSED_FLAG,"");
            return Message.getMessage("CS_MONITOR_UPDATE_0002",new Locale(ostoken.getLocale()));
			
		}catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			String message =  e.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}  catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_UPDATE_0001",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/alarms/{monitorId}/rules", method = RequestMethod.POST)
	public String addRules(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@PathVariable String monitorId,
			@RequestBody String addBody, HttpServletResponse response){
		
		TokenOs ostoken = null;
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(ostoken, ParamConstant.ALARM_ADD_RULE);
			monitorService.addRule(monitorId, addBody, ostoken);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.SUCCESSED_FLAG,"");
            return Message.getMessage("CS_MONITOR_UPDATE_0002",new Locale(ostoken.getLocale()));
			
		}catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			String message =  e.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}  catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_UPDATE_0001",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}

	}
	
	@RequestMapping(value = "/alarms/{monitorId}/rules/{ruleId}", method = RequestMethod.DELETE)
	public String deleteRules(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@PathVariable String monitorId,@PathVariable String ruleId,
			HttpServletResponse response){
		
		TokenOs ostoken = null;
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(ostoken, ParamConstant.ALARM_REMOVE_RULE);
			monitorService.deleteRule(ruleId, monitorId, ostoken);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.SUCCESSED_FLAG,"");
            return Message.getMessage("CS_MONITOR_UPDATE_0002",new Locale(ostoken.getLocale()));
			
		}catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			String message =  e.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}  catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_UPDATE_0001",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/alarms/{monitorId}/notification-list", method = RequestMethod.POST)
	public String addNotificationList(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@PathVariable String monitorId,
			@RequestBody String addBody, HttpServletResponse response){
		
		TokenOs ostoken = null;
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(ostoken, ParamConstant.ALARM_ADD_NOTIFICATION);
			monitorService.addNotificationList(addBody, ostoken);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.SUCCESSED_FLAG,"");
            return Message.getMessage("CS_MONITOR_UPDATE_0002",new Locale(ostoken.getLocale()));
			
		}catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			String message =  e.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}  catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_UPDATE_0001",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/alarms/{monitorId}/notification-list/{notificationObjId}", method = RequestMethod.DELETE)
	public String deleteNotificationObj(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@PathVariable String monitorId,@PathVariable String notificationObjId,
			HttpServletResponse response){

		TokenOs ostoken = null;
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(ostoken, ParamConstant.ALARM_REMOVE_NOTIFICATION);
			monitorService.deleteNotificationObj(notificationObjId, monitorId, ostoken);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.SUCCESSED_FLAG,"");
            return Message.getMessage("CS_MONITOR_UPDATE_0002",new Locale(ostoken.getLocale()));
		}catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			String message =  e.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}  catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_UPDATE_0001",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, monitorId, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/alarms/{id}", method = RequestMethod.DELETE)
	public String deleteMonitor(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@PathVariable String id,
			HttpServletResponse response){
		
		TokenOs ostoken = null;
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(ostoken, ParamConstant.ALARM_DELETE);
			monitorService.deleteMonitor(id, ostoken);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.DELETE_MONITOR,ParamConstant.MONITOR, id, Message.SUCCESSED_FLAG,"");
			return Message.getMessage("CS_MONITOR_DELETE_0002",new Locale(ostoken.getLocale()));	
		}catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			String message =  e.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.DELETE_MONITOR,ParamConstant.MONITOR, id, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}  catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.DELETE_MONITOR,ParamConstant.MONITOR, id, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_DELETE_0001",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.DELETE_MONITOR,ParamConstant.MONITOR, id, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/alarms/{id}/action", method = RequestMethod.PUT)
	public String actionMonitor(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@PathVariable String id,
			@RequestBody String actionBody, HttpServletResponse response){
		
		TokenOs ostoken = null;
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			if(true == this.checkAlarmEnableState(actionBody))
				this.checkUserPermission(ostoken, ParamConstant.ALARM_ENABLE);
			else
				this.checkUserPermission(ostoken, ParamConstant.ALARM_DISABLE);
			monitorService.actionMonitor(id,actionBody, ostoken);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, id, Message.SUCCESSED_FLAG,"");
			return Message.getMessage("CS_MONITOR_ACTION_0002",new Locale(ostoken.getLocale()));
		}catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			String message =  e.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, id, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}  catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, id, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_ACTION_0001",new Locale(ostoken.getLocale()));
			String message =  exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_MONITOR,ParamConstant.MONITOR, id, Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/alarms/config", method = RequestMethod.GET)
	public String getConfig(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			HttpServletResponse response){
		TokenOs ostoken = null;	
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}	
			String config = monitorService.getMonitorNewConfigInfo();
			return config;
		}catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			return e.getResponseMessage();
		}catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_GET_CONFIG_001",new Locale(ostoken.getLocale()));
			return exception.getResponseMessage();
		}
	}
		
	@RequestMapping(value = "/alarms/get-vm-cpu-usage-statics", method = RequestMethod.GET)
	public String getVMCpuUsageStatics(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@RequestParam(value = ParamConstant.INSTANCE_ID, defaultValue = "") String instance_id,
			@RequestParam(value = ParamConstant.DURATION, defaultValue = "") String duration,
			HttpServletResponse response){
		TokenOs ostoken = null;	
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String,Object> paramMap = new HashMap<String, Object>();
			paramMap.put(ParamConstant.INSTANCE_ID, instance_id);
			paramMap.put(ParamConstant.DURATION, duration);
			String result = monitorService.getVMCpuUsageStatics(paramMap,new Locale(ostoken.getLocale()));
			if(null == result)
				result = "[]";
			return result;
		}catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			return e.getResponseMessage();
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_GET_STATIC_001",new Locale(ostoken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
	
	@RequestMapping(value = "/alarms/get-vm-memory-usage-statics", method = RequestMethod.GET)
	public String getVMMemoryUsageStatics(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@RequestParam(value = ParamConstant.INSTANCE_ID, defaultValue = "") String instance_id,
			@RequestParam(value = ParamConstant.DURATION, defaultValue = "") String duration,
			HttpServletResponse response){
		TokenOs ostoken = null;	
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String,Object> paramMap = new HashMap<String, Object>();
			paramMap.put(ParamConstant.INSTANCE_ID, instance_id);
			paramMap.put(ParamConstant.DURATION, duration);
			String result = monitorService.getVMMemoryUsageStatics(paramMap,new Locale(ostoken.getLocale()));
			if(null == result)
				result = "[]";
			return result;
		}catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			return e.getResponseMessage();
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_GET_STATIC_001",new Locale(ostoken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
	
	@RequestMapping(value = "/alarms/get-vm-disk-usage-statics", method = RequestMethod.GET)
	public String getVMDiskUsageStatics(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@RequestParam(value = ParamConstant.INSTANCE_ID, defaultValue = "") String instance_id,
			@RequestParam(value = ParamConstant.DURATION, defaultValue = "") String duration,
			HttpServletResponse response){
		TokenOs ostoken = null;
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String,Object> paramMap = new HashMap<String, Object>();
			paramMap.put(ParamConstant.INSTANCE_ID, instance_id);
			paramMap.put(ParamConstant.DURATION, duration);
			String result = monitorService.getVMDiskUsageStatics(paramMap,new Locale(ostoken.getLocale()));
			if(null == result)
				result = "[]";
			return result;
		}catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			return e.getResponseMessage();
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_GET_STATIC_001",new Locale(ostoken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
	
	@RequestMapping(value = "/alarms/get-vm-network-usage-statics", method = RequestMethod.GET)
	public String getVMNetworkUsageStatics(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@RequestParam(value = ParamConstant.INSTANCE_ID, defaultValue = "") String instance_id,
			@RequestParam(value = ParamConstant.DURATION, defaultValue = "") String duration,
			HttpServletResponse response){
		TokenOs ostoken = null;
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String,Object> paramMap = new HashMap<String, Object>();
			paramMap.put(ParamConstant.INSTANCE_ID, instance_id);
			paramMap.put(ParamConstant.DURATION, duration);
			String result = monitorService.getVMNetworkUsageStatics(paramMap,new Locale(ostoken.getLocale()));
			if(null == result)
				result = "[]";
			return result;
		}catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			return e.getResponseMessage();
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_GET_STATIC_001",new Locale(ostoken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
	
	@RequestMapping(value = "/alarms/get-physical-cpu-usage-statics", method = RequestMethod.GET)
	public String getPhysicalCpuUsageStatics(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@RequestParam(value = ParamConstant.PHY_HOST, defaultValue = "") String phy_host,
			@RequestParam(value = ParamConstant.DURATION, defaultValue = "") String duration,
			HttpServletResponse response){
		TokenOs ostoken = null;	
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String,Object> paramMap = new HashMap<String, Object>();
			paramMap.put(ParamConstant.PHY_HOST, phy_host);
			paramMap.put(ParamConstant.DURATION, duration);
			String result = monitorService.getPhysicalCpuUsageStatics(paramMap,new Locale(ostoken.getLocale()));
			if(null == result)
				result = "[]";
			return result;
		}catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			return e.getResponseMessage();
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_GET_STATIC_001",new Locale(ostoken.getLocale()));
			log.error(exception,e);
			return exception.getResponseMessage();
		}
	}
	
	@RequestMapping(value = "/alarms/get-physical-memory-usage-statics", method = RequestMethod.GET)
	public String getPhysicalMemoryUsageStatics(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@RequestParam(value = ParamConstant.PHY_HOST, defaultValue = "") String phy_host,
			@RequestParam(value = ParamConstant.DURATION, defaultValue = "") String duration,
			HttpServletResponse response){
		TokenOs ostoken = null;
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String,Object> paramMap = new HashMap<String, Object>();
			paramMap.put(ParamConstant.PHY_HOST, phy_host);
			paramMap.put(ParamConstant.DURATION, duration);
			String result = monitorService.getPhysicalMemoryUsageStatics(paramMap,new Locale(ostoken.getLocale()));
			if(null == result)
				result = "[]";
			return result;
		}catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			return e.getResponseMessage();
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_GET_STATIC_001",new Locale(ostoken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
	
	@RequestMapping(value = "/alarms/get-physical-disk-usage-statics", method = RequestMethod.GET)
	public String getPhysicalDiskUsageStatics(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@RequestParam(value = ParamConstant.PHY_HOST, defaultValue = "") String phy_host,
			@RequestParam(value = ParamConstant.DURATION, defaultValue = "") String duration,
			HttpServletResponse response){
		TokenOs ostoken = null;	
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String,Object> paramMap = new HashMap<String, Object>();
			paramMap.put(ParamConstant.PHY_HOST, phy_host);
			paramMap.put(ParamConstant.DURATION, duration);
			String result = monitorService.getPhysicalDiskUsageStatics(paramMap,new Locale(ostoken.getLocale()));
			if(null == result)
				result = "[]";
			return result;
		}catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			return e.getResponseMessage();
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_GET_STATIC_001",new Locale(ostoken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
	
	@RequestMapping(value = "/alarms/get-physical-network-usage-statics", method = RequestMethod.GET)
	public String getPhysicalNetworkUsageStatics(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@RequestParam(value = ParamConstant.PHY_HOST, defaultValue = "") String phy_host,
			@RequestParam(value = ParamConstant.DURATION, defaultValue = "") String duration,
			HttpServletResponse response){
	
		TokenOs ostoken = null;	
		try{
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String,Object> paramMap = new HashMap<String, Object>();
			paramMap.put(ParamConstant.PHY_HOST, phy_host);
			paramMap.put(ParamConstant.DURATION, duration);
			String result = monitorService.getPhysicalNetworkUsageStatics(paramMap,new Locale(ostoken.getLocale()));
			if(null == result)
				result = "[]";
			return result;
		}catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			return e.getResponseMessage();
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_MONITOR_GET_STATIC_001",new Locale(ostoken.getLocale()));
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
	
	@RequestMapping(value = "/monitors", method = RequestMethod.GET)
	public String getMonitorTemplates(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			HttpServletResponse response) {

		TokenOs ostoken = null;
		try {
			ostoken = this.getUserOsToken(inputToken);
			if (null == ostoken) {
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,
						new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}

			List<MonitorTemplate> templates = monitorService.getMonitorTemplates(null, ostoken);
			normalMonitoremplateInfo(templates);
			JsonHelper<List<MonitorTemplate>, String> jsonHelp = new JsonHelper<List<MonitorTemplate>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(templates);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			log.error(message, e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,
					new Locale(ostoken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message, e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_MONITOR_TEMPLATE_GET_FAILED,
					new Locale(ostoken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message, e);
			return message;
		}
	}
	
	
	@RequestMapping(value = "/monitors/{id}", method = RequestMethod.GET)
	public String getMonitorTemplate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response){

		TokenOs ostoken = null;
		try {
			ostoken = this.getUserOsToken(guiToken);
			if (null == ostoken) {
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,
						new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}

			MonitorTemplate template = monitorService.getMonitorTemplate(id, ostoken);
			template.normalInfo();
			JsonHelper<MonitorTemplate, String> jsonHelp = new JsonHelper<MonitorTemplate, String>();
			return jsonHelp.generateJsonBodyWithEmpty(template);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			log.error(message, e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,
					new Locale(ostoken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message, e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_MONITOR_TEMPLATE_DETAIL_GET_FAILED,
					new Locale(ostoken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message, e);
			return message;
		}
	}
	
	@RequestMapping(value = "/monitors", method = RequestMethod.POST)
	public String createMonitorTemplate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			 authToken = this.getUserOsToken(guiToken);
			 MonitorTemplate template  = monitorService.createMonitorTemplate(createBody, authToken);
			 template.normalInfo();
			 JsonHelper<MonitorTemplate, String> jsonHelp = new JsonHelper<MonitorTemplate, String>();
		     return jsonHelp.generateJsonBodyWithEmpty(template);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_MONITOR_TEMPLATE_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/monitors/{id}", method = RequestMethod.DELETE)
	public String deleteMonitorTemplate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs ostoken=null;
		try {
			ostoken = this.getUserOsToken(guiToken);
			if (null == ostoken) {
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,
						new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
		
			 monitorService.deleteMonitorTemplate(id, ostoken);
			 JsonHelper<MonitorTemplate, String> jsonHelp = new JsonHelper<MonitorTemplate, String>();
		     return jsonHelp.generateJsonBodyWithEmpty(new MonitorTemplate());
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(ostoken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_MONITOR_TEMPLATE_DELETE_FAILED,new Locale(ostoken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		}
	}
	
	
	@RequestMapping(value = "/monitors/{id}", method = RequestMethod.PUT)
	public String updateMonitorTemplate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String updateBody, HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			 authToken = this.getUserOsToken(guiToken);
			 MonitorTemplate template = monitorService.updateMonitorTemplate(id,updateBody, authToken);
			 template.normalInfo();
			 JsonHelper<MonitorTemplate, String> jsonHelp = new JsonHelper<MonitorTemplate, String>();
		     return jsonHelp.generateJsonBodyWithEmpty(template);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_MONITOR_TEMPLATE_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/monitor/{id}/{tenantId}", method = RequestMethod.PUT)
	public String applyMonitorTemplate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @PathVariable String tenantId, HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			 authToken = this.getUserOsToken(guiToken);
			 monitorService.applyMonitorTemplate(id,tenantId, authToken);
			 JsonHelper<MonitorTemplate, String> jsonHelp = new JsonHelper<MonitorTemplate, String>();
		     return jsonHelp.generateJsonBodyWithEmpty(new MonitorTemplate());
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_MONITOR_TEMPLATE_APPLY_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		}
	}
	
	private void normalMonitoremplateInfo(List<MonitorTemplate> templates){
		if(Util.isNullOrEmptyList(templates))
			return;
		for(MonitorTemplate template : templates)
			template.normalInfo();
	}
}
