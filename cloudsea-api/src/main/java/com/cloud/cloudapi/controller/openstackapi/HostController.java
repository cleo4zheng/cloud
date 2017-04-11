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
import com.cloud.cloudapi.pojo.openstackapi.forgui.Host;
import com.cloud.cloudapi.pojo.openstackapi.forgui.HostAggregate;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.openstackapi.HostService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class HostController  extends BaseController {

	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private HostService hostService;

	private Logger log = LogManager.getLogger(HostController.class);
	
	@RequestMapping(value = "/hosts", method = RequestMethod.GET)
	public String getHostList(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name, HttpServletResponse response) {

		TokenOs authToken=null;
		try{
		    authToken = this.getUserOsToken(guiToken);
		    if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
		    return getHostInfo(limit,name,authToken,response);
		}  catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR,new Locale(this.getConfig().getSystemDefaultLocale())).getResponseMessage();
		}
	}

	@RequestMapping(value = "/host-aggregates", method = RequestMethod.GET)
	public String getHostAggregates(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name, HttpServletResponse response) {

		TokenOs authToken=null;
		try{
		    authToken = this.getUserOsToken(guiToken);
		    if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
		    this.getAuthService().checkIsAdmin(authToken);
		    List<HostAggregate> aggregates = hostService.getHostAggregates(null, authToken);
			if (Util.isNullOrEmptyList(aggregates)) {
				JsonHelper<List<HostAggregate>, String> jsonHelp = new JsonHelper<List<HostAggregate>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<HostAggregate>());
			}
			normalHostAggregatesInfo(aggregates);
			JsonHelper<List<HostAggregate>, String> jsonHelp = new JsonHelper<List<HostAggregate>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(aggregates);
		}  catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			return e.getResponseMessage();
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_HOST_AGGREGATE_GET_FAILED,new Locale(authToken.getLocale()));
			String message =exception.getResponseMessage();
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	@RequestMapping(value = "/host-aggregates/{id}", method = RequestMethod.GET)
	public String getHostAggregate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs authToken=null;
		try{
		    authToken = this.getUserOsToken(guiToken);
		    if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
		    this.getAuthService().checkIsAdmin(authToken);
		    HostAggregate aggregate = hostService.getHostAggregate(id, authToken);
			if (null == aggregate) {
				JsonHelper<HostAggregate, String> jsonHelp = new JsonHelper<HostAggregate, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new HostAggregate());
			}
			aggregate.normalInfo();
			JsonHelper<HostAggregate, String> jsonHelp = new JsonHelper<HostAggregate, String>();
			return jsonHelp.generateJsonBodyWithEmpty(aggregate);
		}  catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message =e.getResponseMessage();
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message =exception.getResponseMessage();
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_HOST_AGGREGATE_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message =exception.getResponseMessage();
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	@RequestMapping(value = "/host-aggregates/{id}", method = RequestMethod.PUT)
	public String updateHostAggregate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String body, HttpServletResponse response) {

		TokenOs authToken=null;
		try{
		    authToken = this.getUserOsToken(guiToken);
		    if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
		    this.checkUserPermission(authToken, ParamConstant.AGGREGATE_UPDATE);
		    this.getAuthService().checkIsAdmin(authToken);
		    HostAggregate aggregate = hostService.updateHostAggregate(id, body,authToken);
			if (null == aggregate) {
				JsonHelper<HostAggregate, String> jsonHelp = new JsonHelper<HostAggregate, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new HostAggregate());
			}
			aggregate.normalInfo();
			JsonHelper<HostAggregate, String> jsonHelp = new JsonHelper<HostAggregate, String>();
			return jsonHelp.generateJsonBodyWithEmpty(aggregate);
		}  catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message =e.getResponseMessage();
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message =exception.getResponseMessage();
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_HOST_AGGREGATE_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message =exception.getResponseMessage();
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	@RequestMapping(value = "/host-aggregates", method = RequestMethod.POST)
	public String createHostAggregate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {
		
		//get ostoken by cuibl
		TokenOs authToken=null;
        try{
        	authToken = getUserOsToken(guiToken);
        	if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
        	this.checkUserPermission(authToken, ParamConstant.AGGREGATE_NEW);
        	HostAggregate aggregate = hostService.createHostAggregate(createBody, authToken);
			if (null == aggregate) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_HOST_AGGREGATE,ParamConstant.HOST_AGGREGATE,"",Message.FAILED_FLAG,message);
				return message;
			}

			JsonHelper<HostAggregate, String> jsonHelp = new JsonHelper<HostAggregate, String>();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_HOST_AGGREGATE,ParamConstant.HOST_AGGREGATE,aggregate.getId(),Message.SUCCESSED_FLAG,"");
			return jsonHelp.generateJsonBodyWithEmpty(aggregate);
        } catch (ResourceBusinessException e) {
        	log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_BACKUP_DETAIL,ParamConstant.BACKUP,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_BACKUP_DETAIL,ParamConstant.BACKUP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_HOST_AGGREGATE_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.GET_VOLUME_BACKUP_DETAIL,ParamConstant.BACKUP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	@RequestMapping(value = "/host-aggregates/{id}/add", method = RequestMethod.PUT)
	public String addHostToAggregate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String createBody, HttpServletResponse response) {
		
		TokenOs authToken=null;
        try{
        	authToken = getUserOsToken(guiToken);
        	if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
        	this.checkUserPermission(authToken, ParamConstant.AGGREGATE_ADD_HOST);
        	hostService.addHostToAggregate(id, createBody, authToken);
			JsonHelper<HostAggregate, String> jsonHelp = new JsonHelper<HostAggregate, String>();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_HOST_AGGREGATE,ParamConstant.HOST_AGGREGATE,id,Message.SUCCESSED_FLAG,"");
			return jsonHelp.generateJsonBodyWithEmpty(new HostAggregate());
        } catch (ResourceBusinessException e) {
        	log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_HOST_AGGREGATE,ParamConstant.HOST_AGGREGATE,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_HOST_AGGREGATE,ParamConstant.HOST_AGGREGATE,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_HOST_AGGREGATE_ADD_HOST_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.UPDATE_HOST_AGGREGATE,ParamConstant.HOST_AGGREGATE,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	
	@RequestMapping(value = "/host-aggregates/{id}/remove", method = RequestMethod.PUT)
	public String removeHostFromAggregate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String createBody, HttpServletResponse response) {
		
		TokenOs authToken=null;
        try{
        	authToken = getUserOsToken(guiToken);
        	if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.AGGREGATE_REMOVE_HOST);
        	hostService.removeHostFromAggregate(id, createBody, authToken);
			JsonHelper<HostAggregate, String> jsonHelp = new JsonHelper<HostAggregate, String>();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_HOST_AGGREGATE,ParamConstant.HOST_AGGREGATE,id,Message.SUCCESSED_FLAG,"");
			return jsonHelp.generateJsonBodyWithEmpty(new HostAggregate());
        } catch (ResourceBusinessException e) {
        	log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_HOST_AGGREGATE,ParamConstant.HOST_AGGREGATE,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_HOST_AGGREGATE,ParamConstant.HOST_AGGREGATE,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_HOST_AGGREGATE_REMOVE_HOST_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.UPDATE_HOST_AGGREGATE,ParamConstant.HOST_AGGREGATE,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	@RequestMapping(value = "/host-aggregates/{id}", method = RequestMethod.DELETE)
	public String deleteHostAggregate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		
		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.AGGREGATE_DELETE);
			hostService.deleteHostAggregate(id, authToken);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_VOLUME_BACKUP_DELETE_SUCCESSED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_HOST_AGGREGATE,ParamConstant.HOST_AGGREGATE,id,Message.SUCCESSED_FLAG,message);
			return message;
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_HOST_AGGREGATE,ParamConstant.HOST_AGGREGATE,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_HOST_AGGREGATE,ParamConstant.HOST_AGGREGATE,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_HOST_AGGREGATE_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_HOST_AGGREGATE,ParamConstant.HOST_AGGREGATE,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	@RequestMapping(value = "/host-quotas", method = RequestMethod.GET)
	public String getHostQuota(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name, HttpServletResponse response) {

		TokenOs authToken=null;
		TokenOs adminToken = null;
		try{
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			adminToken = this.getAuthService().createDefaultAdminOsToken();
		} catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return e.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR,new Locale(this.getConfig().getSystemDefaultLocale())).getResponseMessage();
		}
		adminToken.setLocale(authToken.getLocale());
        return getHostInfo(limit,name,adminToken,response);
	}
	
	@RequestMapping(value = "/hosts/{host_name}", method = RequestMethod.GET)
	public String getHost(@PathVariable(ParamConstant.HOST_NAME) String hostName,
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Host host = hostService.getHostDetail(hostName, null,authToken);
			if (null == host) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
	//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_HOST_DETAIL,ParamConstant.HOST,"",Message.FAILED_FLAG,message);
				return message;
			}
			host.normalInfo(new Locale(authToken.getLocale()),false);
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_HOST_DETAIL,ParamConstant.HOST,host.getId(),Message.SUCCESSED_FLAG,"");
			JsonHelper<Host, String> jsonHelp = new JsonHelper<Host, String>();
			return jsonHelp.generateJsonBodyWithEmpty(host);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message =e.getResponseMessage();
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.GET_HOST_DETAIL,ParamConstant.HOST,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message =exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_HOST_DETAIL,ParamConstant.HOST,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_HOST_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message =exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_HOST_DETAIL,ParamConstant.HOST,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	private String getHostInfo(String limit,String name,TokenOs authToken, HttpServletResponse response){
		try {
			Map<String, String> paramMap = null;

			if (!"".equals(limit)) {
				paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.LIMIT, limit);
			}

			if (!"".equals(name)) {
				if (null == paramMap)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.OWNER, name);
			}
			
			List<Host> hosts = hostService.getHostList(paramMap, authToken);
            if(Util.isNullOrEmptyList(hosts)){
	//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_HOST,ParamConstant.HOST,"",Message.SUCCESSED_FLAG,"");
				JsonHelper<List<Host>, String> jsonHelp = new JsonHelper<List<Host>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Host>());
            }
            hosts = normalHostsInfo(hosts,new Locale(authToken.getLocale()));
  //          this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_HOST,ParamConstant.HOST,getHostsId(hosts),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<Host>, String> jsonHelp = new JsonHelper<List<Host>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(hosts);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_HOST,ParamConstant.HOST,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_HOST,ParamConstant.HOST,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_HOST_GET_FAILED,new Locale(authToken.getLocale()));
			String message =exception.getResponseMessage();
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_HOST,ParamConstant.HOST,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	private List<Host> normalHostsInfo(List<Host> hosts,Locale locale){
		 if(null == hosts)
			 return null;
		 List<Host> normalHosts = new ArrayList<Host>();
		 for(Host host : hosts){
			 if(Util.isNullOrEmptyValue(host.getHostName()))
				 continue;
			 host.normalInfo(locale,false);
			 normalHosts.add(host);
		 }
		 return normalHosts;
	}
	
	private void normalHostAggregatesInfo(List<HostAggregate> hostAggregates){
		 if(null == hostAggregates)
			 return;
		 for(HostAggregate aggregate : hostAggregates)
			 aggregate.normalInfo();
	}
}
