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
import org.springframework.beans.factory.annotation.Autowired;
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
import com.cloud.cloudapi.pojo.openstackapi.forgui.NotificationList;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Terminal;
import com.cloud.cloudapi.service.common.NotificationListService;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class NotificationListController  extends BaseController {

	@Autowired
	private NotificationListService notificationListService;

	@Resource
	private OperationLogService operationLogService;

	private Logger log = LogManager.getLogger(NotificationListController.class);
	
	@RequestMapping("/notification-lists")
	public String getNotificationLists(@RequestHeader(value=ParamConstant.AUTH_TOKEN,defaultValue="nownoimpl") String inputToken,
			@RequestParam(value="limit",defaultValue="") String limit,
			@RequestParam(value="name", defaultValue="") String name,
    		@RequestParam(value="terminal", defaultValue="") String terminalId,
    		HttpServletResponse response) {

		
		TokenOs ostoken = null;
		Map<String,String> paramMap=null; 
    	if(!"".equals(limit)){
    		paramMap=new HashMap<String,String>();
    		paramMap.put("limit", limit);
    	}
    	
    	if(!"".equals(name)){
    		if(paramMap==null) paramMap=new HashMap<String,String>();
    		paramMap.put("name", name);
    	}
    	
    	if(!"".equals(terminalId)){		
    		if(paramMap==null) paramMap=new HashMap<String,String>();
    		paramMap.put("terminalId", terminalId);
    	}
    	
		try {
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<NotificationList> notificationLists = notificationListService.getNotificationLists(paramMap, ostoken);
	//		operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_NOTIFICATIONLISTS,ParamConstant.NOTIFICATIONLIST,getNotificationListsId(notificationLists),Message.SUCCESSED_FLAG,"");
			
			if(Util.isNullOrEmptyList(notificationLists)){
				JsonHelper<List<NotificationList>, String> jsonHelp = new JsonHelper<List<NotificationList>, String>();
	    		return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<NotificationList>());
			}
			
			JsonHelper<List<NotificationList>, String> jsonHelp = new JsonHelper<List<NotificationList>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(notificationLists);
			
		}catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_NOTIFICATIONLISTS,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,e.getResponseMessage());
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_NOTIFICATIONLISTS,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATIONLIST_SELECT_0001",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_NOTIFICATIONLISTS,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
	
	@RequestMapping(value="/notification-lists", produces = {"application/json;charset=UTF-8"}, method=RequestMethod.POST)
	public String createNotificationList(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@RequestBody String createBody, HttpServletResponse response) {
		
		TokenOs ostoken = null;
		if (null == createBody || createBody.isEmpty()){
			log.error("createBody is null");
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATIONLIST_CREATE_0003",new Locale(this.getConfig().getSystemDefaultLocale()));
			return exception.getResponseMessage();
		}
		
		ObjectMapper mapper = new ObjectMapper();
		NotificationList notificationList = null;
		try {
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(ostoken, ParamConstant.NOTIFICATION_LIST_NEW);
			notificationList = mapper.readValue(createBody, NotificationList.class);
			notificationListService.insertNotificationList(notificationList, ostoken);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.CREATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,notificationList.getId(),Message.SUCCESSED_FLAG,"");
			return Message.getMessage("CS_NOTIFICATIONLIST_CREATE_0002",new Locale(ostoken.getLocale()));
		}  catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.CREATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,e.getResponseMessage());
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.CREATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}  catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATIONLIST_CREATE_0001",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.CREATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
	
	@RequestMapping(value="/notification-lists/{id}", produces = {"application/json;charset=UTF-8"}, method=RequestMethod.GET)
	public String showNotificationList(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			@PathVariable String id, HttpServletResponse response) {
		
		TokenOs ostoken = null;
		try {
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			NotificationList notificationList = notificationListService.selectNotificationListById(id);
			if(null == notificationList){
				log.error("to get the notificationList is not null; id = " + id);
				throw new ResourceBusinessException("CS_NOTIFICATIONLIST_SHOW_0001",new Locale(ostoken.getLocale()));
			}
			if(!ostoken.getTenantid().equals(notificationList.getTenant_id())){
				log.error("to get the notificationList is not created by this user.current tenant = " 
			              + ostoken.getTenantid() + "; notificaiotnList's tenant id = " + notificationList.getTenant_id());
				throw new ResourceBusinessException("CS_NOTIFICATIONLIST_SHOW_0001",new Locale(ostoken.getLocale()));
			}
			JsonHelper<NotificationList, String> jsonHelp = new JsonHelper<NotificationList, String>();
	//		operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,notificationList.getId(),Message.SUCCESSED_FLAG,"");
			return jsonHelp.generateJsonBodyWithEmpty(notificationList);
			
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,e.getResponseMessage());
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATIONLIST_SHOW_0001",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
		
	}
	
	@RequestMapping(value="/notification-lists/{id}", produces={"application/json;charset=UTF-8"}, method=RequestMethod.DELETE)
	public String deleteNotificationList(@RequestHeader(value=ParamConstant.AUTH_TOKEN, defaultValue="") String inputToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs ostoken = null;
		try {
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
            this.checkUserPermission(ostoken, ParamConstant.NOTIFICATION_LIST_DELETE);
			notificationListService.deleteNotificationListById(id, ostoken);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.DELETE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,id,Message.SUCCESSED_FLAG,"");
			return Message.getMessage("CS_NOTIFICATIONLIST_DELETE_0002",new Locale(ostoken.getLocale()));
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.DELETE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,e.getResponseMessage());
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.DELETE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATIONLIST_DELETE_0001",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.DELETE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
	}
	
	@RequestMapping(value="/notification-lists/{id}", produces={}, method=RequestMethod.PUT)
	public String updateNotificationList(@RequestHeader(value=ParamConstant.AUTH_TOKEN, defaultValue="") String inputToken,
			@PathVariable String id,
			@RequestBody String updateBody, HttpServletResponse response) {
				
		TokenOs ostoken = null;
	
		if (null == updateBody || updateBody.isEmpty()){
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATIONLIST_UPDATE_0001",new Locale(this.getConfig().getSystemDefaultLocale()));
			return exception.getResponseMessage();
		}
		
		ObjectMapper mapper = new ObjectMapper();
		NotificationList notificationList = null;
//		try {
//			ostoken = this.getUserOsToken(inputToken);
//			
//			notificationList = mapper.readValue(updateBody, NotificationList.class);
//		} catch (Exception e) {
//			log.error("transfer updateBody error: " + e.getMessage());
//			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
//			ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATIONLIST_UPDATE_0001",new Locale(ostoken.getLocale()));
//			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,exception.getResponseMessage());
//			return exception.getResponseMessage();
//		}
		
		try {
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(ostoken, ParamConstant.NOTIFICATION_LIST_UPDATE);
			notificationList = mapper.readValue(updateBody, NotificationList.class);
			notificationList.setId(id);
			notificationListService.updateNotificationListById(notificationList, ostoken);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,id,Message.SUCCESSED_FLAG,"");
			return Message.getMessage("CS_NOTIFICATIONLIST_UPDATE_0002",new Locale(ostoken.getLocale()));
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,e.getResponseMessage());
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			// TODO: handle exception
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATIONLIST_UPDATE_0001",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
		
	}
	
	@RequestMapping(value="/notification-lists/{pid}/terminals", produces={"application/json;charset=UTF-8"}, method=RequestMethod.POST)
	public String createTerminal(@RequestHeader(value=ParamConstant.AUTH_TOKEN, defaultValue="") String inputToken,
			@PathVariable(value="pid") String pid, @RequestBody String createBody, HttpServletResponse response) {
		
		TokenOs ostoken = null;
		ObjectMapper mapper = new ObjectMapper();
		Terminal terminal = null;
//		try {
//			terminal = mapper.readValue(createBody, Terminal.class);
//			terminal.setNotificationListId(pid);
//		} catch (Exception e) {
//			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
//			ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATIONLIST_CREATE_0003",new Locale(ostoken.getLocale()));
//			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,exception.getResponseMessage());
//			return exception.getResponseMessage();
//		}
		
		try {
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(ostoken, ParamConstant.NOTIFICATION_ADD_TERMINAL);
			terminal = mapper.readValue(createBody, Terminal.class);
			terminal.setNotificationListId(pid);
			notificationListService.insertTerminal(terminal, ostoken);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,terminal.getNotificationListId(),Message.SUCCESSED_FLAG,"");
			return Message.getMessage("CS_NOTIFICATIONLIST_TERMINAL_CREATE_0002",new Locale(ostoken.getLocale()));
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,e.getResponseMessage());
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			// TODO: handle exception
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATIONLIST_TERMINAL_CREATE_0001",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
		
	}
	
	@RequestMapping(value="/notification-lists/{pid}/terminals/{id}", produces={"application/json;charset=UTF-8"}, method=RequestMethod.DELETE)
	public String deleteTerminal(@RequestHeader(value=ParamConstant.AUTH_TOKEN, defaultValue="") String inputToken,
			@PathVariable(value="pid") String pid, @PathVariable(value="id") String id, HttpServletResponse response) {

		TokenOs ostoken = null;	
		try {
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(ostoken, ParamConstant.NOTIFICATION_DELETE_TERMINAL);
			notificationListService.deleteTerminal(id, ostoken);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,id,Message.SUCCESSED_FLAG,"");
			return Message.getMessage("CS_NOTIFICATIONLIST_TERMINAL_DELETE_0002",new Locale(ostoken.getLocale()));
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,e.getResponseMessage());
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			// TODO: handle exception
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATIONLIST_TERMINAL_DELETE_0001",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}
		
	}
	
	@RequestMapping(value="/notification-lists/terminals/{id}/verify", produces={"application/json;charset=UTF-8"}, method=RequestMethod.POST)
	public String verifyNotificationListTerminal(@RequestHeader(value=ParamConstant.AUTH_TOKEN, defaultValue="") String inputToken,
			@PathVariable(value="id") String id, HttpServletResponse response) {
		
		TokenOs ostoken = null;
		try {
			ostoken = this.getUserOsToken(inputToken);
			if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			notificationListService.verifyNotificationListTerminal(id, ostoken);
			return Message.getMessage("CS_NOTIFICATIONLIST_TERMINAL_VERIFY_0002",new Locale(ostoken.getLocale()));
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,e.getResponseMessage());
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		} catch (Exception e) {
			// TODO: handle exception
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_NOTIFICATIONLIST_TERMINAL_VERIFY_0001",new Locale(ostoken.getLocale()));
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.UPDATE_NOTIFICATIONLIST,ParamConstant.NOTIFICATIONLIST,"",Message.FAILED_FLAG,exception.getResponseMessage());
			log.error(exception.getResponseMessage(),e);
			return exception.getResponseMessage();
		}		
	}
}
