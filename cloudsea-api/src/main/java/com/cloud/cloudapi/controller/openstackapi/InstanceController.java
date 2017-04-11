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
import com.cloud.cloudapi.pojo.openstackapi.forgui.Console;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FloatingIP;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Image;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.InstanceConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.InterfaceAttachment;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Port;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SecurityGroup;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Volume;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeAttachment;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.openstackapi.FloatingIPService;
import com.cloud.cloudapi.service.openstackapi.ImageService;
import com.cloud.cloudapi.service.openstackapi.InstanceService;
import com.cloud.cloudapi.service.openstackapi.KeypairService;
import com.cloud.cloudapi.service.openstackapi.NetworkService;
import com.cloud.cloudapi.service.openstackapi.PortService;
import com.cloud.cloudapi.service.openstackapi.SubnetService;
import com.cloud.cloudapi.service.openstackapi.TenantService;
import com.cloud.cloudapi.service.openstackapi.VolumeService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class InstanceController  extends BaseController {

	@Resource
	private OperationLogService operationLogService;

	@Resource
	private InstanceService instanceService;
	
	@Resource
	private VolumeService volumeService;
	
	@Resource
	private KeypairService keypairService;
	
	@Resource
	private NetworkService networkService;
	
	@Resource
	private SubnetService subnetService;
	
	@Resource
	private ImageService imageService;
	
	@Resource
	private FloatingIPService floatingIPService;
	
	@Resource
	private PortService portService;

	@Resource
	private TenantService tenantServiceImpl;

	private Logger log = LogManager.getLogger(InstanceController.class);
	
	/**
	 * get the instance list by parameter and guitoken
	 * 
	 * @param guiToken
	 *            guitokenid
	 * @param limit
	 *            the now to be show
	 * @param name
	 *            the name of instance
	 * @param status
	 *            the status of instance
	 * @param imageid
	 *            the imageid of instance
	 * @return
	 */

	@RequestMapping(value = "/instances", method = RequestMethod.GET)
	public String getInstancesList(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			@RequestParam(value = ParamConstant.IMAGE_TYPE, defaultValue = "") String imageid,
			HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String, String> paramMap = Util.makeRequestParamInfo(limit, name, status, ParamConstant.IMAGE_TYPE,
					imageid);
			List<Instance> instances = instanceService.getInstanceList(paramMap, ParamConstant.INSTANCE_TYPE, authToken);
			if (Util.isNullOrEmptyList(instances)) {
				// this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
				// authToken.getTenantid(),Message.GET_INSTANCE,ParamConstant.INSTANCE,"",Message.SUCCESSED_FLAG,"");
				JsonHelper<List<Instance>, String> jsonHelp = new JsonHelper<List<Instance>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Instance>());
			}
			normalInstancesInfo(instances,false,false,new Locale(authToken.getLocale()));
			// this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
			// authToken.getTenantid(),Message.GET_INSTANCE,ParamConstant.INSTANCE,getInstancesId(instances),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<Instance>, String> jsonHelp = new JsonHelper<List<Instance>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(instances);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_INSTANCE, ParamConstant.INSTANCE, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_INSTANCE, ParamConstant.INSTANCE, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_INSTANCE, ParamConstant.INSTANCE, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/instances/{id}", method = RequestMethod.GET)
	public String getInstance(
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
			Instance instance = instanceService.getInstance(id, ParamConstant.INSTANCE_TYPE, authToken, true);
			normalInstanceInfo(instance,true,false,new Locale(authToken.getLocale()));
			JsonHelper<Instance, String> jsonHelp = new JsonHelper<Instance, String>();
			return jsonHelp.generateJsonBodyWithEmpty(instance);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_INSTANCE, ParamConstant.INSTANCE, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_INSTANCE, ParamConstant.INSTANCE, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_COMPUTE_INSTANCE_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_INSTANCE, ParamConstant.INSTANCE, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/instances", method = RequestMethod.POST)
	public String createInstance(
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
			this.checkUserPermission(authToken, ParamConstant.INSTANCE_NEW);
			List<Instance> createdInstances = instanceService.createInstance(createBody, ParamConstant.INSTANCE_TYPE,
					authToken);
			normalInstancesInfo(createdInstances,false,false,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_INSTANCE, ParamConstant.INSTANCE,
					getInstancesId(createdInstances), Message.SUCCESSED_FLAG, "");
			JsonHelper<List<Instance>, String> jsonHelp = new JsonHelper<List<Instance>, String>();
			return jsonHelp.generateJsonBodySimple(createdInstances);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_INSTANCE, ParamConstant.INSTANCE, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_INSTANCE, ParamConstant.INSTANCE, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_COMPUTE_INSTANCE_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_INSTANCE, ParamConstant.INSTANCE, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/instances/{id}/volumes", method = RequestMethod.GET)
	public String getInstanceAttachedVolumes(
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
			// InstanceService inService =
			// OsApiServiceFactory.getInstanceService();
			List<Volume> volumes = instanceService.getAttachedVolumes(id, authToken);
			if (Util.isNullOrEmptyList(volumes)) {
				// this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
				// authToken.getTenantid(),Message.GET_VOLUME,ParamConstant.VOLUME,"",Message.SUCCESSED_FLAG,"");
				JsonHelper<List<Volume>, String> jsonHelp = new JsonHelper<List<Volume>, String>();
				return jsonHelp.generateJsonBodySimple(new ArrayList<Volume>());
			}
			normalVolumesInfo(volumes,new Locale(authToken.getLocale()));
			// this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
			// authToken.getTenantid(),Message.GET_VOLUME,ParamConstant.VOLUME,getVolumesId(volumes),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<Volume>, String> jsonHelp = new JsonHelper<List<Volume>, String>();
			return jsonHelp.generateJsonBodySimple(volumes);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_VOLUME, ParamConstant.VOLUME, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_VOLUME, ParamConstant.VOLUME, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_VOLUME, ParamConstant.VOLUME, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/instances/{id}", method = RequestMethod.PUT)
	public String updateInstance(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, @PathVariable String id, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken,ParamConstant.INSTANCE_UPDATE,id);
			Instance updatedInstance = instanceService.updateInstance(id, createBody, authToken);
			if (null == updatedInstance) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
						authToken.getTenantid(), Message.UPDATE_INSTANCE, ParamConstant.INSTANCE, "",
						Message.FAILED_FLAG, message);
				return message;
			}
			if(ParamConstant.VDI_TYPE.equals(updatedInstance.getType()))
				normalInstanceInfo(updatedInstance,true,true,new Locale(authToken.getLocale()));
			else
				normalInstanceInfo(updatedInstance,true,false,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_INSTANCE, ParamConstant.INSTANCE, updatedInstance.getId(),
					Message.SUCCESSED_FLAG, "");
			JsonHelper<Instance, String> jsonHelp = new JsonHelper<Instance, String>();
			return jsonHelp.generateJsonBodyWithEmpty(updatedInstance);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_INSTANCE, ParamConstant.INSTANCE, id, Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_INSTANCE, ParamConstant.INSTANCE, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_COMPUTE_INSTANCE_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_INSTANCE, ParamConstant.INSTANCE, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/instances/{id}/{action}", method = RequestMethod.POST)
	public String actionInstance(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @PathVariable String action, @RequestBody String body,
			HttpServletResponse response) {
		// get ostoken by cuibl
		String messageFailedId = "";
		String messageSuccessedId = "";
		String messageTitle = "";

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			if (ParamConstant.CREATE_IMAGE_ACTION.equals(action)) {
				this.checkUserPermission(authToken, ParamConstant.IMAGE_NEW);
				/* create image by the instance */
				messageFailedId = Message.CS_COMPUTE_INSTANCE_IMAGE_CREATE_FAILED;
				messageTitle = Message.CREATE_INSTANCE_IMAGE;
				Image image = instanceService.createInstanceImage(id, ParamConstant.INSTANCE_TYPE, authToken, body);
				if (null == image) {
					response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
					ResourceBusinessException exception = new ResourceBusinessException(messageFailedId,new Locale(authToken.getLocale()));
					String message = exception.getResponseMessage();
					this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
							authToken.getTenantid(), messageTitle, ParamConstant.INSTANCE, id, Message.FAILED_FLAG, "");
					return message;
				}
				image.normalInfo();
				JsonHelper<Image, String> jsonHelp = new JsonHelper<Image, String>();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
						authToken.getTenantid(), messageTitle, ParamConstant.INSTANCE, id, Message.SUCCESSED_FLAG, "");
				return jsonHelp.generateJsonBodyWithEmpty(image);
			} else if (ParamConstant.LIVE_MIGRATION_ACTION.equals(action)){
			//	this.checkUserPermission(authToken, ParamConstant.IMAGE_NEW);
				/* create image by the instance */
				messageFailedId = Message.CS_COMPUTE_INSTANCE_LIVE_MIGRATION_FAILED;
				messageTitle = Message.LIVE_MIGRATION_INSTANCE;
				Instance instance = instanceService.liveMigrationInstance(id, action, authToken, body);
				if (null == instance) {
					response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
					ResourceBusinessException exception = new ResourceBusinessException(messageFailedId,new Locale(authToken.getLocale()));
					String message = exception.getResponseMessage();
					this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
							authToken.getTenantid(), messageTitle, ParamConstant.INSTANCE, id, Message.FAILED_FLAG, "");
					return message;
				}
				JsonHelper<Instance, String> jsonHelp = new JsonHelper<Instance, String>();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
						authToken.getTenantid(), messageTitle, ParamConstant.INSTANCE, id, Message.SUCCESSED_FLAG, "");
				return jsonHelp.generateJsonBodyWithEmpty(new Instance());	
			} else if (ParamConstant.GET_VNCCONSOLE_ACTION.equals(action)) {
				this.checkUserPermission(authToken, ParamConstant.INSTANCE_CONSOLE, id);
				messageFailedId = Message.CS_COMPUTE_GET_VNC_CONSOLE_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_GET_VNC_CONSOLE_SUCCESSED;
				messageTitle = Message.GET_VNCCONSOLE;
				Console console = instanceService.getInstanceConsole(id, action, authToken, body);
				JsonHelper<Console, String> jsonHelp = new JsonHelper<Console, String>();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
						authToken.getTenantid(), messageTitle, ParamConstant.INSTANCE, id, Message.SUCCESSED_FLAG, "");
				return jsonHelp.generateJsonBodyWithEmpty(console);
			} else if (ParamConstant.GET_SPICECONSOLE_ACTION.equals(action)) {
				this.checkUserPermission(authToken, ParamConstant.INSTANCE_CONSOLE, id);
				messageFailedId = Message.CS_COMPUTE_GET_SPICE_CONSOLE_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_GET_SPICE_CONSOLE_SUCCESSED;
				messageTitle = Message.GET_SPICECONSOLE;
				Console console = instanceService.getInstanceConsole(id, action, authToken, body);
				JsonHelper<Console, String> jsonHelp = new JsonHelper<Console, String>();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
						authToken.getTenantid(), messageTitle, ParamConstant.INSTANCE, id, Message.SUCCESSED_FLAG, "");
				return jsonHelp.generateJsonBodyWithEmpty(console);
			} else if (ParamConstant.GET_SERIALCONSOLE_ACTION.equals(action)) {
				this.checkUserPermission(authToken, ParamConstant.INSTANCE_CONSOLE, id);
				messageFailedId = Message.CS_COMPUTE_GET_SERIAL_CONSOLE_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_GET_SERIAL_CONSOLE_SUCCESSED;
				messageTitle = Message.GET_SERIALCONSOLE;
				Console console = instanceService.getInstanceConsole(id, action, authToken, body);
				JsonHelper<Console, String> jsonHelp = new JsonHelper<Console, String>();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
						authToken.getTenantid(), messageTitle, ParamConstant.INSTANCE, id, Message.SUCCESSED_FLAG, "");
				return jsonHelp.generateJsonBodyWithEmpty(console);
			} else if (ParamConstant.GET_RDPCONSOLE_ACTION.equals(action)) {
				this.checkUserPermission(authToken, ParamConstant.INSTANCE_CONSOLE, id);
				messageFailedId = Message.CS_COMPUTE_GET_RDP_CONSOLE_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_GET_RDP_CONSOLE_SUCCESSED;
				messageTitle = Message.GET_RDPCONSOLE;
				Console console = instanceService.getInstanceConsole(id, action, authToken, body);
				JsonHelper<Console, String> jsonHelp = new JsonHelper<Console, String>();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
						authToken.getTenantid(), messageTitle, ParamConstant.INSTANCE, id, Message.SUCCESSED_FLAG, "");
				return jsonHelp.generateJsonBodyWithEmpty(console);
			} else if (ParamConstant.PAUSE_INSTANCE_ACTION.equals(action)) {
				this.checkUserPermission(authToken, ParamConstant.INSTANCE_PAUSE, id);
				/* pause the instance */
				messageFailedId = Message.CS_COMPUTE_INSTANCE_PAUSE_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_INSTANCE_PAUSE_SUCCESSED;
				messageTitle = Message.PAUSE_INSTANCE;
				instanceService.operateInstance(id, body, action, ParamConstant.INSTANCE_TYPE, authToken);
			} else if (ParamConstant.UNPAUSE_INSTANCE_ACTION.equals(action)) {
				this.checkUserPermission(authToken, ParamConstant.INSTANCE_RECOVER, id);
				/* pause the instance */
				messageFailedId = Message.CS_COMPUTE_INSTANCE_RESTORE_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_INSTANCE_RESTORE_SUCCESSED;
				messageTitle = Message.RESTORE_INSTANCE;
				instanceService.operateInstance(id, body, action, ParamConstant.INSTANCE_TYPE, authToken);
			} else if (ParamConstant.SOFT_REBOOT_INSTANCE_ACTION.equals(action)) {
				this.checkUserPermission(authToken, ParamConstant.INSTANCE_RESTART, id);
				messageFailedId = Message.CS_COMPUTE_INSTANCE_SOFT_REBOOT_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_INSTANCE_SOFT_REBOOT_SUCCESSED;
				messageTitle = Message.SOFT_REBOOT_INSTANCE;
				instanceService.operateInstance(id, body, action, ParamConstant.INSTANCE_TYPE, authToken);
			} else if (ParamConstant.HARD_REBOOT_INSTANCE_ACTION.equals(action)) {
				this.checkUserPermission(authToken, ParamConstant.INSTANCE_FORCE_RESTART, id);
				messageFailedId = Message.CS_COMPUTE_INSTANCE_HARD_REBOOT_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_INSTANCE_HARD_REBOOT_SUCCESSED;
				messageTitle = Message.HARD_REBOOT_INSTANCE;
				instanceService.operateInstance(id, body, action, ParamConstant.INSTANCE_TYPE, authToken);
			} else if (ParamConstant.START_INSTANCE_ACTION.equals(action)) {
				this.checkUserPermission(authToken, ParamConstant.INSTANCE_START, id);
				messageFailedId = Message.CS_COMPUTE_INSTANCE_START_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_INSTANCE_START_SUCCESSED;
				messageTitle = Message.START_INSTANCE;
				instanceService.operateInstance(id, body, action, ParamConstant.INSTANCE_TYPE, authToken);
			} else if (ParamConstant.STOP_INSTANCE_ACTION.equals(action)) {
				this.checkUserPermission(authToken, ParamConstant.INSTANCE_STOP, id);
				messageFailedId = Message.CS_COMPUTE_INSTANCE_STOP_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_INSTANCE_STOP_SUCCESSED;
				messageTitle = Message.STOP_INSTANCE;
				instanceService.operateInstance(id, body, action, ParamConstant.INSTANCE_TYPE, authToken);
			} else if (ParamConstant.SUSPEND_INSTANCE_ACTION.equals(action)) {
				this.checkUserPermission(authToken, ParamConstant.INSTANCE_SUSPEND, id);
				messageFailedId = Message.CS_COMPUTE_INSTANCE_SUSPEND_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_INSTANCE_SUSPEND_SUCCESSED;
				messageTitle = Message.SUSPEND_INSTANCE;
				instanceService.operateInstance(id, body, action, ParamConstant.INSTANCE_TYPE, authToken);
			} else if (ParamConstant.RESUME_INSTANCE_ACTION.equals(action)) {
				this.checkUserPermission(authToken, ParamConstant.INSTANCE_RECOVER, id);
				messageFailedId = Message.CS_COMPUTE_INSTANCE_RESTORE_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_INSTANCE_RESTORE_SUCCESSED;
				messageTitle = Message.RESTORE_INSTANCE;
				instanceService.operateInstance(id, body, action, ParamConstant.INSTANCE_TYPE, authToken);
			}else if (ParamConstant.ADD_SECURITYGROUP_ACTION.equals(action)) {
				this.checkUserPermission(authToken, ParamConstant.INSTANCE_ADD_SECURITYGROUP, id);
				messageFailedId = Message.CS_COMPUTE_INSTANCE_ADD_SECURITY_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_INSTANCE_ADD_SECURITY_SUCCESSED;
				messageTitle = Message.ADD_SECURITYGROUP;
				instanceService.operateInstance(id, body, action, ParamConstant.INSTANCE_TYPE, authToken);
			} else if (ParamConstant.REMOVE_SECURITYGROUP_ACTION.equals(action)) {
				this.checkUserPermission(authToken, ParamConstant.INSTANCE_REMOVE_SECURITYGROUP, id);
				messageFailedId = Message.CS_COMPUTE_INSTANCE_REMOVE_SECURITY_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_INSTANCE_REMOVE_SECURITY_SUCCESSED;
				messageTitle = Message.REMOVE_SECURITYGROUP;
				instanceService.operateInstance(id, body, action, ParamConstant.INSTANCE_TYPE, authToken);
			} else if (ParamConstant.ADD_FLOATINGIP_ACTION.equals(action)) {
				this.checkUserPermission(authToken, ParamConstant.INSTANCE_ADD_FLOATINGIP, id);
				messageFailedId = Message.CS_COMPUTE_INSTANCE_ADD_FLOATINGIP_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_INSTANCE_ADD_FLOATINGIP_SUCCESSED;
				messageTitle = Message.ADD_FLOATINGIP;
				instanceService.operateInstance(id, body, action, ParamConstant.INSTANCE_TYPE, authToken);
			} else if (ParamConstant.REMOVE_FLOATINGIP_ACTION.equals(action)) {
				this.checkUserPermission(authToken, ParamConstant.INSTANCE_REMOVE_FLOATINGIP, id);
				messageFailedId = Message.CS_COMPUTE_INSTANCE_REMOVE_FLOATINGIP_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_INSTANCE_REMOVE_FLOATINGIP_SUCCESSED;
				messageTitle = Message.REMOVE_FLOATINGIP;
				instanceService.operateInstance(id, body, action, ParamConstant.INSTANCE_TYPE, authToken);
			} else if (ParamConstant.ADD_FIXEDIP_ACTION.equals(action)) {
				messageFailedId = Message.CS_COMPUTE_INSTANCE_ADD_FIXEDIP_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_INSTANCE_ADD_FIXEDIP_SUCCESSED;
				messageTitle = Message.ADD_FIXEDIP;
				instanceService.operateInstance(id, body, action, ParamConstant.INSTANCE_TYPE, authToken);
			} else if (ParamConstant.REMOVE_FIXEDIP_ACTION.equals(action)) {
				messageFailedId = Message.CS_COMPUTE_INSTANCE_REMOVE_FIXEDIP_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_INSTANCE_REMOVE_FIXEDIP_SUCCESSED;
				messageTitle = Message.REMOVE_FIXEDIP;
				instanceService.operateInstance(id, body, action, ParamConstant.INSTANCE_TYPE, authToken);
			}else if (ParamConstant.RESIZE_ACTION.equals(action)) {
				this.checkUserPermission(authToken, ParamConstant.INSTANCE_RESIZE, id);
				messageFailedId = Message.CS_COMPUTE_INSTANCE_RESIZE_FAILED;
				messageSuccessedId = Message.CS_COMPUTE_INSTANCE_RESIZE_SUCCESSED;
				messageTitle = Message.RESIZE;
				instanceService.operateInstance(id, body, action, ParamConstant.INSTANCE_TYPE, authToken);
			}

			ResourceBusinessException exception = new ResourceBusinessException(messageSuccessedId,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), messageTitle, ParamConstant.INSTANCE, id, Message.SUCCESSED_FLAG,
					exception.getResponseMessage());
			return exception.getResponseMessage();
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), messageTitle, ParamConstant.INSTANCE, id, Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), messageTitle, ParamConstant.INSTANCE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(messageFailedId,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), messageTitle, ParamConstant.INSTANCE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/instances", method = RequestMethod.DELETE)
	public String deleteInstances(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String deleteBody, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.INSTANCE_DELETE,getInstanceId(deleteBody));
			String id = instanceService.deleteInstances(deleteBody, authToken);
			if (null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			// instanceMapper.deleteByPrimaryKey(id);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_INSTANCE, ParamConstant.INSTANCE, id,
					Message.SUCCESSED_FLAG, "");

			JsonHelper<Instance, String> jsonHelp = new JsonHelper<Instance, String>();
			return jsonHelp.generateJsonBodySimple(new Instance());

		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_INSTANCE, ParamConstant.INSTANCE, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_INSTANCE, ParamConstant.INSTANCE, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_COMPUTE_INSTANCE_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_INSTANCE, ParamConstant.INSTANCE, "", Message.FAILED_FLAG,
					message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/instances/{id}/volumes", method = RequestMethod.POST)
	public String attachVolume(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, @PathVariable String id, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.INSTANCE_ADD_VOLUME,id);
			VolumeAttachment volumeAttachment = instanceService.attachVolume(id, createBody, authToken);
			if (null == volumeAttachment) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
						authToken.getTenantid(), Message.VOLUME_ATTACH, ParamConstant.VOLUME_ATTACHMENT, "",
						Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.VOLUME_ATTACH, ParamConstant.VOLUME_ATTACHMENT,
					volumeAttachment.getId(), Message.SUCCESSED_FLAG, "");
			JsonHelper<VolumeAttachment, String> jsonHelp = new JsonHelper<VolumeAttachment, String>();
			return jsonHelp.generateJsonBodySimple(volumeAttachment);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.VOLUME_ATTACH, ParamConstant.VOLUME_ATTACHMENT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR);
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.VOLUME_ATTACH, ParamConstant.VOLUME_ATTACHMENT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_COMPUTE_INSTANCE_VOLUME_ATTACH_FAILED);
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.VOLUME_ATTACH, ParamConstant.VOLUME_ATTACHMENT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/instances/{id}/snapshots", method = RequestMethod.POST)
	public String createSnapshot(
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
	//		this.checkUserPermission(authToken, ParamConstant.INSTANCE_ADD_VOLUME,id);
			instanceService.createSnapshot(id, authToken);
	
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_INSTANCE_SNAPSHOT, ParamConstant.SNAPSHOT,
					id, Message.SUCCESSED_FLAG, "");
			JsonHelper<Instance, String> jsonHelp = new JsonHelper<Instance, String>();
			return jsonHelp.generateJsonBodySimple(new Instance());
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_INSTANCE_SNAPSHOT, ParamConstant.SNAPSHOT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR);
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_INSTANCE_SNAPSHOT, ParamConstant.SNAPSHOT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_COMPUTE_INSTANCE_SNAPSHOT_CREATE_FAILED);
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_INSTANCE_SNAPSHOT, ParamConstant.SNAPSHOT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/instances/{id}/volumes/{volumeId}", method = RequestMethod.DELETE)
	public String detachVolume(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @PathVariable String volumeId, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.INSTANCE_REMOVE_VOLUME,id);
			instanceService.detachVolume(id, volumeId, authToken);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.VOLUME_DETACH, ParamConstant.VOLUME_ATTACHMENT, volumeId,
					Message.SUCCESSED_FLAG, "");
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_COMPUTE_INSTANCE_VOLUME_DETACH_SUCCESSED,new Locale(authToken.getLocale()));
			return exception.getResponseMessage();
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.VOLUME_DETACH, ParamConstant.VOLUME_ATTACHMENT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.VOLUME_DETACH, ParamConstant.VOLUME_ATTACHMENT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_COMPUTE_INSTANCE_VOLUME_DETACH_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.VOLUME_DETACH, ParamConstant.VOLUME_ATTACHMENT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/instances/{id}/ports", method = RequestMethod.POST)
	public String attachPort(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, @PathVariable String id, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.INSTANCE_ADD_PORT, id);
			InterfaceAttachment interfaceAttachment = instanceService.attachPort(id, ParamConstant.INSTANCE_TYPE,
					createBody, authToken);
			if (null == interfaceAttachment) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
						authToken.getTenantid(), Message.PORT_ATTACH, ParamConstant.INTERFACE_ATTACHMENT, "",
						Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.PORT_ATTACH, ParamConstant.INTERFACE_ATTACHMENT,
					interfaceAttachment.getPort_id(), Message.SUCCESSED_FLAG, "");
			JsonHelper<InterfaceAttachment, String> jsonHelp = new JsonHelper<InterfaceAttachment, String>();
			return jsonHelp.generateJsonBodySimple(interfaceAttachment);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.PORT_ATTACH, ParamConstant.INTERFACE_ATTACHMENT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR);
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.PORT_ATTACH, ParamConstant.INTERFACE_ATTACHMENT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_COMPUTE_INSTANCE_PORT_ATTACH_FAILED);
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.PORT_ATTACH, ParamConstant.INTERFACE_ATTACHMENT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/instances/{id}/ports/{portId}", method = RequestMethod.DELETE)
	public String detachPort(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @PathVariable String portId, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.INSTANCE_REMOVE_PORT, id);
			instanceService.detachPort(id, portId, authToken);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.PORT_DETACH, ParamConstant.INTERFACE_ATTACHMENT, portId,
					Message.SUCCESSED_FLAG, "");
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_COMPUTE_INSTANCE_PORT_DETACH_SUCCESSED,new Locale(authToken.getLocale()));
			return exception.getResponseMessage();
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.PORT_DETACH, ParamConstant.INTERFACE_ATTACHMENT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.PORT_DETACH, ParamConstant.INTERFACE_ATTACHMENT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_COMPUTE_INSTANCE_PORT_DETACH_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.PORT_DETACH, ParamConstant.INTERFACE_ATTACHMENT, "",
					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/instances/config", method = RequestMethod.GET)
	public String getInstanceConfig(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			InstanceConfig instanceConfig = instanceService.getInstanceConfig(ParamConstant.INSTANCE_TYPE, authToken);
			if (null == instanceConfig) {
				response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				// this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
				// authToken.getTenantid(),Message.GET_INSTANCE_CONFIG,ParamConstant.INSTANCECONFIG,"",Message.FAILED_FLAG,message);
				return message;
			}
			// this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
			// authToken.getTenantid(),Message.GET_INSTANCE_CONFIG,ParamConstant.INSTANCECONFIG,instanceConfig.getId(),Message.SUCCESSED_FLAG,"");
			JsonHelper<InstanceConfig, String> jsonHelp = new JsonHelper<InstanceConfig, String>();
			return jsonHelp.generateJsonBodyWithEmpty(instanceConfig);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			// this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
			// authToken.getTenantid(),Message.GET_INSTANCE_CONFIG,ParamConstant.INSTANCECONFIG,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			// this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
			// authToken.getTenantid(),Message.GET_INSTANCE_CONFIG,ParamConstant.INSTANCECONFIG,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_INSTANCE_CONFIG_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			// this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
			// authToken.getTenantid(),Message.GET_INSTANCE_CONFIG,ParamConstant.INSTANCECONFIG,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}

	private String getInstancesId(List<Instance> instances) {
		if (Util.isNullOrEmptyList(instances))
			return "";
		List<String> ids = new ArrayList<String>();
		for (Instance instance : instances) {
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
	
			List<Port> ports = instance.getPorts();
			normalPortsInfo(ports);
			
			List<Image> images = instance.getImages();
			if(Util.isNullOrEmptyList(images))
				instance.setImages(null);
			else{
				for(Image image : images){
					image.normalInfo();
				}
			}
			if (false == normalVDIInfo) {
				List<FloatingIP> floatingIPs = instance.getAttachedFloatingIPs();
				if (Util.isNullOrEmptyList(floatingIPs))
					instance.setAttachedFloatingIPs(null);
				else {
					for (FloatingIP floatingIP : floatingIPs) {
						floatingIP.normalInfo(locale);
					}
					instance.setAttachedFloatingIPs(floatingIPs);
				}

				List<SecurityGroup> securityGroups = instance.getSecurityGroups();
				if (Util.isNullOrEmptyList(securityGroups))
					instance.setSecurityGroups(null);
				else {
					for (SecurityGroup securityGroup : securityGroups) {
						securityGroup.normalInfo(true);
					}
					instance.setSecurityGroups(securityGroups);
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
	
	private void normalPortsInfo(List<Port> ports){
		if(Util.isNullOrEmptyList(ports))
			return;
		else{
			for(Port port : ports){
				port.normalInfo(true);
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
	
	private String getInstanceId(String body){
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(body);
		} catch (Exception e) {
			log.error(e);
		}
		JsonNode idsNode = rootNode.path(ResponseConstant.IDS);
		int idsCount = idsNode.size();
		for (int index = 0; index < idsCount;) {
			return idsNode.get(index).textValue();
		}
		return null;
	}
}
