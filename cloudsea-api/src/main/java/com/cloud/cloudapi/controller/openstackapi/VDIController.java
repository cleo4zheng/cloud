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
import com.cloud.cloudapi.pojo.openstackapi.forgui.FloatingIP;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Image;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.InstanceConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SecurityGroup;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Volume;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.openstackapi.InstanceService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class VDIController extends BaseController  {
	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private InstanceService instanceService;

	private Logger log = LogManager.getLogger(VDIController.class);
			
	@RequestMapping(value = "/vdi-instances", method = RequestMethod.GET)
	public String getVDIInstances(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			@RequestParam(value = ParamConstant.IMAGE_TYPE, defaultValue = "") String imageid,
			HttpServletResponse response) {
		
		TokenOs authToken=null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String, String> paramMap = Util.makeRequestParamInfo(limit,name,status,ParamConstant.IMAGE_TYPE,imageid);
			List<Instance> instances = instanceService.getVDIInstanceList(paramMap, ParamConstant.VDI_TYPE,authToken);
			if(Util.isNullOrEmptyList(instances)){
	//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VDI_INSTANCE,ParamConstant.INSTANCE,"",Message.SUCCESSED_FLAG,"");
				JsonHelper<List<Instance>, String> jsonHelp = new JsonHelper<List<Instance>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Instance>());
			}
			normalInstancesInfo(instances,false,false,new Locale(authToken.getLocale()));
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VDI_INSTANCE,ParamConstant.INSTANCE,getInstancesId(instances),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<Instance>, String> jsonHelp = new JsonHelper<List<Instance>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(instances);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VDI_INSTANCE,ParamConstant.INSTANCE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message =  exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VDI_INSTANCE,ParamConstant.INSTANCE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_VDI_INSTANCE_GET_FAILED,new Locale(authToken.getLocale()));
			String message =  exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VDI_INSTANCE,ParamConstant.INSTANCE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/vdi-instances/{id}", method = RequestMethod.GET)
	public String getVDIInstance(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id , HttpServletResponse response) {

		TokenOs authToken=null;
		try{
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
		    Instance instance = instanceService.getInstance(id,ParamConstant.VDI_TYPE,authToken,true);
		    normalInstanceInfo(instance,true,true,new Locale(authToken.getLocale()));
		//    this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VDI_INSTANCE,ParamConstant.INSTANCE,id,Message.SUCCESSED_FLAG,"");
			JsonHelper<Instance, String> jsonHelp = new JsonHelper<Instance, String>();
			return jsonHelp.generateJsonBodySimple(instance);
		}catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VDI_INSTANCE,ParamConstant.INSTANCE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}  catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VDI_INSTANCE,ParamConstant.INSTANCE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_VDI_INSTANCE_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VDI_INSTANCE,ParamConstant.INSTANCE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/vdi-instances", method = RequestMethod.POST)
	public String createInstance(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {

		TokenOs authToken=null;		
		try{
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
		    List<Instance> createdInstances = instanceService.createInstance(createBody, ParamConstant.VDI_TYPE,authToken);
			normalInstancesInfo(createdInstances,false,true,new Locale(authToken.getLocale()));
		    this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VDI_INSTANCE,ParamConstant.INSTANCE,getInstancesId(createdInstances),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<Instance>, String> jsonHelp = new JsonHelper<List<Instance>, String>();
			return jsonHelp.generateJsonBodySimple(createdInstances);
		}catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VDI_INSTANCE,ParamConstant.INSTANCE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}  catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VDI_INSTANCE,ParamConstant.INSTANCE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_VDI_INSTANCE_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VDI_INSTANCE,ParamConstant.INSTANCE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/vdi-instances/config", method = RequestMethod.GET)
	public String getVDIInstanceConfig(
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
			InstanceConfig instanceConfig = instanceService.getInstanceConfig(ParamConstant.VDI_TYPE,authToken);
			if (null == instanceConfig) {
				response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message =  exception.getResponseMessage();
	//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_INSTANCE_CONFIG,ParamConstant.INSTANCECONFIG,"",Message.FAILED_FLAG,message);
				return message;
			}
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_INSTANCE_CONFIG,ParamConstant.INSTANCECONFIG,instanceConfig.getId(),Message.SUCCESSED_FLAG,"");
			JsonHelper<InstanceConfig, String> jsonHelp = new JsonHelper<InstanceConfig, String>();
			return jsonHelp.generateJsonBodyWithEmpty(instanceConfig);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message =  e.getResponseMessage();
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_INSTANCE_CONFIG,ParamConstant.INSTANCECONFIG,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message =  exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_INSTANCE_CONFIG,ParamConstant.INSTANCECONFIG,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_INSTANCE_CONFIG_GET_FAILED,new Locale(authToken.getLocale()));
			String message =  exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_INSTANCE_CONFIG,ParamConstant.INSTANCECONFIG,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	private String getInstancesId(List<Instance> instances){
		if(Util.isNullOrEmptyList(instances))
			return "";
		List<String> ids = new ArrayList<String>();
		for(Instance instance : instances){
			ids.add(instance.getId());
		}
		return Util.listToString(ids, ',');
	}
	
	private void normalInstanceInfo(Instance instance,Boolean normalDetailInfo,Boolean normalVDIInfo,Locale locale){
		instance.setSystemName(instance.getSourceName());
	//	instance.setAvailabilityZoneName(Message.getMessage(instance.getAvailabilityZone().toUpperCase(),locale,false));
		List<String> ips = Util.stringToList(instance.getFixedips(), ",");
		if (null != ips)
			instance.setIps(ips);
		ips = Util.stringToList(instance.getFloatingips(), ",");
		if (null != ips)
			instance.setFloatingIps(ips);
		instance.normalInfo(false);
		if(true == normalDetailInfo){
			List<Volume> volumes = instance.getVolumes();
			normalVolumesInfo(volumes,locale);
	
			List<Image> images = instance.getImages();
			if(Util.isNullOrEmptyList(images))
				instance.setImages(null);
			else{
				for(Image image : images){
					image.normalInfo();
				}
			
				if(false == normalVDIInfo){
					List<FloatingIP> floatingIPs = instance.getAttachedFloatingIPs();
					if(Util.isNullOrEmptyList(floatingIPs))
						instance.setFloatingIps(null);
					else{
						for(FloatingIP floatingIP : floatingIPs){
							floatingIP.normalInfo(locale);
						}
					}
					
					List<SecurityGroup> securityGroups = instance.getSecurityGroups();
					if(Util.isNullOrEmptyList(securityGroups))
						instance.setSecurityGroups(null);
					else{
						for(SecurityGroup securityGroup : securityGroups){
							securityGroup.normalInfo(true);
						}
					}
				}
			}
		}else{
			instance.setVolumes(null);
			instance.setImages(null);
			instance.setAttachedFloatingIPs(null);
			instance.setSecurityGroups(null);
		}
	}
	
	private void normalVolumesInfo(List<Volume> volumes,Locale locale){
		if(Util.isNullOrEmptyList(volumes))
			return;
		else{
			for(Volume volume : volumes){
				volume.normalInfo(true,locale);
			}
		}
	}
	private void normalInstancesInfo(List<Instance> instances,Boolean normalDetailInfo,Boolean normalVDIInfo,Locale locale){
		if(Util.isNullOrEmptyList(instances))
			return;
		for(Instance instance : instances){
			normalInstanceInfo(instance,normalDetailInfo,normalVDIInfo,locale);
		}
	}
}
