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
import com.cloud.cloudapi.pojo.openstackapi.forgui.Notification;
import com.cloud.cloudapi.service.common.NotificationService;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class NotificationController extends BaseController  {

	@Resource
	private NotificationService notificationService;

	@Resource
	private OperationLogService operationLogService;
	
	private Logger log = LogManager.getLogger(NotificationController.class);
	
	@RequestMapping(value = "/notifications", method = RequestMethod.GET)
	public String getNotificationList(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.TYPE, defaultValue = "") String type,
			@RequestParam(value = ParamConstant.READ, defaultValue = "") String read,
			HttpServletResponse response) {

		TokenOs ostoken = null;
		try {
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String, String> paramMap = null;
	
			if (!"".equals(limit)) {
				if(paramMap==null) 
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.LIMIT, limit);
			}
	
			if (!"".equals(type)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.TYPE, type);
			}
	
			if (!"".equals(read)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.READ, read);
			}
	
			List<Notification> list = notificationService.getNotificationsPage(paramMap, ostoken);
	//		operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_NOTIFICATIONS,ParamConstant.NOTIFICATION,"",Message.SUCCESSED_FLAG,"");
			
			if(Util.isNullOrEmptyList(list)){
				JsonHelper<List<Notification>, String> jsonHelp = new JsonHelper<List<Notification>, String>();
	    		return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Notification>());
			}
			
			JsonHelper<List<Notification>, String> jsonHelp = new JsonHelper<List<Notification>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(list);
			
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_NOTIFICATIONS,ParamConstant.NOTIFICATION,"",Message.FAILED_FLAG,e.getResponseMessage());
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_NOTIFICATIONS,ParamConstant.NOTIFICATION,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATION_SELECT_0001",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_NOTIFICATIONS,ParamConstant.NOTIFICATION,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}

	}
	
	@RequestMapping(value = "/notifications/{id}", method = RequestMethod.GET)
	public String getNotification(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@PathVariable String id, HttpServletResponse response) {


		TokenOs ostoken = null;	
		try {
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Notification notification = notificationService.getNotification(id);
	//		operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_NOTIFICATION,ParamConstant.NOTIFICATION,id,Message.SUCCESSED_FLAG,"");
			
			if(null == notification || !notification.getTenant_id().equals(ostoken.getTenantid())){
				JsonHelper<Notification, String> jsonHelp = new JsonHelper<Notification, String>();
	    		return jsonHelp.generateJsonBodyWithEmpty(new Notification());
			}
			
			JsonHelper<Notification, String> jsonHelp = new JsonHelper<Notification, String>();
			return jsonHelp.generateJsonBodyWithEmpty(notification);
			
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_NOTIFICATION,ParamConstant.NOTIFICATION,"",Message.FAILED_FLAG,e.getResponseMessage());
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_NOTIFICATION,ParamConstant.NOTIFICATION,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATION_SELECT_0002",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_NOTIFICATION,ParamConstant.NOTIFICATION,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}

	}
	
	@RequestMapping(value = "/notifications/{id}", method = RequestMethod.PUT)
	public String updateNotificationRead(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@RequestBody String createBody, HttpServletResponse response) {
		
		TokenOs ostoken = null;
		try {
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(ostoken, ParamConstant.NOTIFICATION_READ);
			if (Util.isNullOrEmptyValue(createBody)) {
				ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATION_UPDATE_0003",new Locale(ostoken.getLocale())); 																										// change//the/message
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATION,ParamConstant.NOTIFICATION,"",Message.FAILED_FLAG,exception.getResponseMessage());
				return exception.getResponseMessage();
			}
			
			ObjectMapper mapper = new ObjectMapper();
			Notification notificationInfo = null;
			notificationInfo = mapper.readValue(createBody, Notification.class);

			notificationService.updateNotificationRead(notificationInfo.getId(), notificationInfo.getRead());
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATION,ParamConstant.NOTIFICATION,notificationInfo.getId(),Message.SUCCESSED_FLAG,"");
			return Message.getMessage("CS_NOTIFICATION_UPDATE_0002",new Locale(ostoken.getLocale()));
			
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATION,ParamConstant.NOTIFICATION,"",Message.FAILED_FLAG,e.getResponseMessage());
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATION,ParamConstant.NOTIFICATION,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATION_UPDATE_0001",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATION,ParamConstant.NOTIFICATION,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}

	}
	
	@RequestMapping(value = "/notifications/read", method = RequestMethod.PUT)
	public String updateNotificationsRead(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@RequestBody String createBody, HttpServletResponse response) {
		
		TokenOs ostoken = null;
		try {
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(ostoken, ParamConstant.NOTIFICATION_READ);
			if (Util.isNullOrEmptyValue(createBody)) {
				ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATION_UPDATE_0003",new Locale(ostoken.getLocale())); 																										// change//the/message
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATION,ParamConstant.NOTIFICATION,"",Message.FAILED_FLAG,exception.getResponseMessage());
				return exception.getResponseMessage();
			}
			
			ObjectMapper mapper = new ObjectMapper();
			HashMap<String,List> notificationIdH = null;
			notificationIdH = mapper.readValue(createBody, HashMap.class);

			notificationService.updateNotificationsReadStatus(notificationIdH.get("ids"), true);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATION,ParamConstant.NOTIFICATION, notificationIdH.get("ids").toString(), Message.SUCCESSED_FLAG,"");
			return Message.getMessage("CS_NOTIFICATION_UPDATE_0002",new Locale(ostoken.getLocale()));
			
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATION,ParamConstant.NOTIFICATION,"",Message.FAILED_FLAG,e.getResponseMessage());
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATION,ParamConstant.NOTIFICATION,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATION_UPDATE_0001",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATION,ParamConstant.NOTIFICATION,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}

	}
	
	@RequestMapping(value = "/notifications/unread", method = RequestMethod.PUT)
	public String updateNotificationsUnRead(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@RequestBody String createBody, HttpServletResponse response) {
		
	
		TokenOs ostoken = null;
		try {
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(ostoken, ParamConstant.NOTIFICATION_UNREAD);
			if (Util.isNullOrEmptyValue(createBody)) {
				ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATION_UPDATE_0003",new Locale(ostoken.getLocale())); 																										// change//the/message
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATION,ParamConstant.NOTIFICATION,"",Message.FAILED_FLAG,exception.getResponseMessage());
				return exception.getResponseMessage();
			}
			
			ObjectMapper mapper = new ObjectMapper();
			HashMap<String,List> notificationIdH = null;
			notificationIdH = mapper.readValue(createBody, HashMap.class);

			notificationService.updateNotificationsReadStatus(notificationIdH.get("ids"), false);	
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATION,ParamConstant.NOTIFICATION, notificationIdH.get("ids").toString(), Message.SUCCESSED_FLAG,"");
			return Message.getMessage("CS_NOTIFICATION_UPDATE_0002",new Locale(ostoken.getLocale()));
			
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATION,ParamConstant.NOTIFICATION,"",Message.FAILED_FLAG,e.getResponseMessage());
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATION,ParamConstant.NOTIFICATION,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATION_UPDATE_0001",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATION,ParamConstant.NOTIFICATION,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}

	}
	
}
