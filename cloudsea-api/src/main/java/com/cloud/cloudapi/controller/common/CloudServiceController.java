package com.cloud.cloudapi.controller.common;

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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.cloud.cloudapi.controll.monitor.MonitorController;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.CloudService;
import com.cloud.cloudapi.service.common.CloudUserService;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.openstackapi.CloudServiceService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class CloudServiceController  extends BaseController {

	@Resource
	private CloudUserService cloudUserService;

	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private CloudServiceService serviceService;
	
	private Logger log = LogManager.getLogger(MonitorController.class);
	
	@RequestMapping(value = "/services", method = RequestMethod.GET)
	public String getServiceList(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			HttpServletResponse response) {		
		TokenOs ostoken = null;
		try {
			ostoken = this.getUserOsToken(inputToken);
        	if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
        	
			Map<String, String> paramMap = new HashMap<String, String>();
			List<CloudService> serviceList =  serviceService.getServiceList(paramMap, ostoken);
			
			if(Util.isNullOrEmptyList(serviceList)){
	//			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_SERVICES,ParamConstant.SERVICE,"",Message.FAILED_FLAG,"");
				JsonHelper<List<CloudService>, String> jsonHelp = new JsonHelper<List<CloudService>, String>();
	    		return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<CloudService>());
			}
			
	//		operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_SERVICES,ParamConstant.SERVICE,getServicesId(serviceList),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<CloudService>, String> jsonHelp = new JsonHelper<List<CloudService>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(serviceList);
			
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			String message = e.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_SERVICES,ParamConstant.SERVICE,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			String message = exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_SERVICES,ParamConstant.SERVICE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_SERVICE_SELECT_0001",new Locale(ostoken.getLocale()));
			String message = exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_SERVICES,ParamConstant.SERVICE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/service-types", method = RequestMethod.GET)
	public String getServiceTypes(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String inputToken,
			HttpServletResponse response) {		
		TokenOs ostoken = null;
	
		try {
			ostoken = this.getUserOsToken(inputToken);
        	if(null == ostoken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
        	this.getAuthService().checkIsAdmin(ostoken);
			List<CloudService> services = serviceService.getSystemServiceCapacity(ostoken);
			normalServices(services,new Locale(ostoken.getLocale()));
		//	operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_SERVICES,ParamConstant.SERVICE,getServicesId(services),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<CloudService>, String> jsonHelp = new JsonHelper<List<CloudService>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(services);
		}  catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message =e.getResponseMessage();
			return message;
		}  catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_DB_00041",new Locale(ostoken.getLocale()));
			String message = exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_SERVICES,ParamConstant.SERVICE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException("CS_SERVICE_SELECT_0001",new Locale(ostoken.getLocale()));
			String message = exception.getResponseMessage();
			operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(ostoken), ostoken.getTenantid(),Message.GET_SERVICES,ParamConstant.SERVICE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	private void normalServices(List<CloudService> services,Locale locale){
		if(null == services)
			return;
		for(CloudService service : services)
			service.normalInfo(locale);
	}
	
//	private String getServicesId(List<CloudService> services){
//		if(Util.isNullOrEmptyList(services))
//			return "";
//		List<String> ids = new ArrayList<String>();
//		for(CloudService service : services){
//			ids.add(service.getId());
//		}
//		return Util.listToString(ids, ',');
//	}
}
