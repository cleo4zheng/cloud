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
import com.cloud.cloudapi.pojo.openstackapi.forgui.Backup;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Volume;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeSnapshot;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.openstackapi.BackupService;
import com.cloud.cloudapi.service.openstackapi.InstanceService;
import com.cloud.cloudapi.service.openstackapi.VolumeService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.StringHelper;

@RestController
public class VolumeController extends BaseController{
	
	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private InstanceService instanceService;
	
	@Resource
	private VolumeService volumeService;
	
	@Resource
	private BackupService backupService;

	private Logger log = LogManager.getLogger(VolumeController.class);
	
	@RequestMapping(value = "/volumes", method = RequestMethod.GET)
	public String getVolumesList(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			@RequestParam(value = ParamConstant.INSTANCE, defaultValue = "") String instanceId,
			HttpServletResponse response) {
		//get ostoken by cuibl
		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String, String> paramMap = Util.makeRequestParamInfo(limit, StringHelper.string2Ncr(name), status, ParamConstant.INSTANCE,
					instanceId);
			List<Volume> volumes = volumeService.getVolumeList(paramMap, authToken);
			if (null == volumes) {
				JsonHelper<List<Volume>, String> jsonHelp = new JsonHelper<List<Volume>, String>();
	//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME,ParamConstant.VOLUME,"",Message.SUCCESSED_FLAG,"");
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Volume>());
			}
			normalVolumesInfo(volumes,true,new Locale(authToken.getLocale()));
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME,ParamConstant.VOLUME,getVolumesId(volumes),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<Volume>, String> jsonHelp = new JsonHelper<List<Volume>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(volumes);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME,ParamConstant.VOLUME,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME,ParamConstant.VOLUME,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME,ParamConstant.VOLUME,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/volumes/{id}", method = RequestMethod.GET)
	public String getVolume(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		//get ostoken by cuibl
		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Volume volume = volumeService.getVolume(id, authToken);
			if (null == volume) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
		//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_DETAIL,ParamConstant.VOLUME,"",Message.FAILED_FLAG,message);
				return message;
			}
			normalVolumeInfo(volume,true,new Locale(authToken.getLocale()));
		//	this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_DETAIL,ParamConstant.VOLUME,volume.getId(),Message.SUCCESSED_FLAG,"");
			JsonHelper<Volume, String> jsonHelp = new JsonHelper<Volume, String>();
			return StringHelper.ncr2String(jsonHelp.generateJsonBodyWithEmpty(volume));
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_DETAIL,ParamConstant.VOLUME,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_DETAIL,ParamConstant.VOLUME,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_DETAIL,ParamConstant.VOLUME,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/volumes", method = RequestMethod.POST)
	public String createVolume(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {
		//get ostoken by cuibl
		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.VOLUME_NEW);
			Volume volume = volumeService.createVolume(createBody, authToken);
			if (null == volume) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VOLUME,ParamConstant.VOLUME,"",Message.FAILED_FLAG,message);
				return message;
			}
			normalVolumeInfo(volume,true,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VOLUME,ParamConstant.VOLUME,volume.getId(),Message.SUCCESSED_FLAG,"");
			JsonHelper<Volume, String> jsonHelp = new JsonHelper<Volume, String>();
			return StringHelper.ncr2String(jsonHelp.generateJsonBodyWithEmpty(volume));
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VOLUME,ParamConstant.VOLUME,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VOLUME,ParamConstant.VOLUME,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VOLUME,ParamConstant.VOLUME,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/volumes/{id}", method = RequestMethod.PUT)
	public String updateVolume(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String updateBody, @PathVariable String id, HttpServletResponse response) {
		// get ostoken by cuibl
		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.VOLUME_UPDATE);
			Volume volume = volumeService.updateVolume(id, updateBody, authToken);
			if (null == volume) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.UPDATE_VOLUME, ParamConstant.VOLUME, "", Message.FAILED_FLAG, message);
				return message;
			}
			normalVolumeInfo(volume,true,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_VOLUME, ParamConstant.VOLUME, id, Message.SUCCESSED_FLAG, "");
			JsonHelper<Volume, String> jsonHelp = new JsonHelper<Volume, String>();
			return StringHelper.ncr2String(jsonHelp.generateJsonBodyWithEmpty(volume));
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_VOLUME, ParamConstant.VOLUME, id, Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_VOLUME, ParamConstant.VOLUME, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_COMPUTE_VOLUME_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_VOLUME, ParamConstant.VOLUME, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/volumes", method = RequestMethod.DELETE)
	public String deleteVolumes(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String deleteBody, HttpServletResponse response) {
		//get ostoken by cuibl
		TokenOs authToken=null;
		// Firstly get the instance from local db
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.VOLUME_DELETE);
	//		Map<String, String> paramMap = new HashMap<String, String>();
	//		paramMap.put(ParamConstant.ID, id);
	//		InstanceService inService = OsApiServiceFactory.getInstanceService();
			String id = volumeService.deleteVolumes(deleteBody, authToken);
			if(null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
	//		instanceMapper.deleteByPrimaryKey(id);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_INSTANCE,ParamConstant.INSTANCE,id,Message.SUCCESSED_FLAG,"");
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_DELETE_SUCCESSED,new Locale(authToken.getLocale()));
			return exception.getResponseMessage();

		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_INSTANCE,ParamConstant.INSTANCE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_INSTANCE,ParamConstant.INSTANCE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_INSTANCE,ParamConstant.INSTANCE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/volumes/{id}", method = RequestMethod.DELETE)
	public String deleteVolume(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		//get ostoken by cuibl
		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.VOLUME_DELETE);
			volumeService.deleteVolume(id, authToken);
			if(null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_VOLUME,ParamConstant.VOLUME,id,Message.SUCCESSED_FLAG,"");
			JsonHelper<Volume, String> jsonHelp = new JsonHelper<Volume, String>();
			return jsonHelp.generateJsonBodyWithEmpty(new Volume());
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_VOLUME,ParamConstant.VOLUME,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_VOLUME,ParamConstant.VOLUME,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_VOLUME,ParamConstant.VOLUME,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/volumes/config", method = RequestMethod.GET)
	public String getVolumeConfig(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			HttpServletResponse response) {
		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			VolumeConfig volumeConfig = volumeService.getVolumeConfig(authToken);
			if (null == volumeConfig) {
				response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message =  exception.getResponseMessage();
		//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
				return message;
			}
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE,ParamConstant.VOLUMETYPE,"",Message.SUCCESSED_FLAG,"");
			JsonHelper<VolumeConfig, String> jsonHelp = new JsonHelper<VolumeConfig, String>();
			return jsonHelp.generateJsonBodyWithEmpty(volumeConfig);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message =  e.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message =  exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_VOLUME_TYPE_GET_FAILED,new Locale(authToken.getLocale()));
			String message =  exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_TYPE,ParamConstant.VOLUMETYPE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	
	@RequestMapping(value = "/volume/snapshots", method = RequestMethod.GET)
	public String getVolumeSnapshots(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			HttpServletResponse response) {
		//get ostoken by cuibl
		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<VolumeSnapshot> snapshots = volumeService.getSnapshots(null, authToken);
			if (null == snapshots) {
				JsonHelper<List<VolumeSnapshot>, String> jsonHelp = new JsonHelper<List<VolumeSnapshot>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<VolumeSnapshot>());
			}
			normalSnapshotsInfo(snapshots);
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME,ParamConstant.VOLUME,getVolumesId(volumes),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<VolumeSnapshot>, String> jsonHelp = new JsonHelper<List<VolumeSnapshot>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(snapshots);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_SNAPSHOT,ParamConstant.SNAPSHOT,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_SNAPSHOT,ParamConstant.SNAPSHOT,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_SNAPSHOT_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_SNAPSHOT,ParamConstant.SNAPSHOT,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	
	@RequestMapping(value = "/volume/snapshots/{id}", method = RequestMethod.GET)
	public String getSnapsohot(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		//get ostoken by cuibl
		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			VolumeSnapshot snapshot = volumeService.getSnapshot(id, authToken);
			if (null == snapshot) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				return message;
			}
			snapshot.normalInfo(true);
			JsonHelper<VolumeSnapshot, String> jsonHelp = new JsonHelper<VolumeSnapshot, String>();
			return StringHelper.ncr2String(jsonHelp.generateJsonBodyWithEmpty(snapshot));
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_SNAPSHOT_DETAIL,ParamConstant.SNAPSHOT,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_SNAPSHOT_DETAIL,ParamConstant.SNAPSHOT,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_SNAPSHOT_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_SNAPSHOT_DETAIL,ParamConstant.SNAPSHOT,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/volume/snapshots", method = RequestMethod.POST)
	public String createVolumeSnapshot(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {
		//get ostoken by cuibl
		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			VolumeSnapshot snapshot = volumeService.createSnapshot(createBody, authToken);
			if (null == snapshot) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VOLUME_SNAPSHOT,ParamConstant.SNAPSHOT,"",Message.FAILED_FLAG,message);
				return message;
			}
			snapshot.normalInfo(true);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VOLUME_SNAPSHOT,ParamConstant.SNAPSHOT,"",Message.SUCCESSED_FLAG,"");
			JsonHelper<VolumeSnapshot, String> jsonHelp = new JsonHelper<VolumeSnapshot, String>();
			return jsonHelp.generateJsonBodyWithEmpty(snapshot);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VOLUME_SNAPSHOT,ParamConstant.SNAPSHOT,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VOLUME_SNAPSHOT,ParamConstant.SNAPSHOT,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_SNAPSHOT_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VOLUME_SNAPSHOT,ParamConstant.SNAPSHOT,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/volume/snapshots/{id}", method = RequestMethod.DELETE)
	public String deleteSnapshot(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		//get ostoken by cuibl
		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
		//	this.checkUserPermission(authToken, ParamConstant.VOLUME_DELETE);
			volumeService.deleteSnapshot(id, authToken);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_VOLUME_SNAPSHOT,ParamConstant.SNAPSHOT,id,Message.SUCCESSED_FLAG,"");
			JsonHelper<Volume, String> jsonHelp = new JsonHelper<Volume, String>();
			return jsonHelp.generateJsonBodyWithEmpty(new Volume());
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_VOLUME_SNAPSHOT,ParamConstant.SNAPSHOT,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_VOLUME_SNAPSHOT,ParamConstant.SNAPSHOT,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_SNAPSHOT_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_VOLUME_SNAPSHOT,ParamConstant.SNAPSHOT,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	
	@RequestMapping(value = "/volume/snapshots/{id}", method = RequestMethod.PUT)
	public String updateSnapshot(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String updateBody, @PathVariable String id, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
	//		this.checkUserPermission(authToken, ParamConstant.VOLUME_UPDATE);
			VolumeSnapshot snapshot = volumeService.updateSnapshot(id, updateBody, authToken);
			if (null == snapshot) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.UPDATE_VOLUME_SNAPSHOT, ParamConstant.SNAPSHOT, "", Message.FAILED_FLAG, message);
				return message;
			}
			snapshot.normalInfo(true);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_VOLUME_SNAPSHOT, ParamConstant.SNAPSHOT, id, Message.SUCCESSED_FLAG, "");
			JsonHelper<VolumeSnapshot, String> jsonHelp = new JsonHelper<VolumeSnapshot, String>();
			return StringHelper.ncr2String(jsonHelp.generateJsonBodyWithEmpty(snapshot));
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_VOLUME_SNAPSHOT, ParamConstant.SNAPSHOT, id, Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_VOLUME_SNAPSHOT, ParamConstant.SNAPSHOT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_COMPUTE_VOLUME_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_VOLUME_SNAPSHOT, ParamConstant.SNAPSHOT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	private void normalVolumeInfo(Volume volume,Boolean normalVolumeType,Locale locale){
		volume.normalInfo(false, locale);
		List<Instance> instances = volume.getInstances();
		if(!Util.isNullOrEmptyList(instances)){
			for(Instance instance : instances){
				instance.normalInfo(true);
			}
		}

		List<Backup> backups = volume.getBackups();
		if(Util.isNullOrEmptyList(backups))
			return;
		for(Backup backup : backups){
			backup.normalInfo(true);
		}
	}
	
	private void normalVolumesInfo(List<Volume> volumes,Boolean normalVolumeType,Locale locale){
		if(Util.isNullOrEmptyList(volumes))
			return;
		for(Volume volume : volumes){
			normalVolumeInfo(volume,normalVolumeType,locale);
		}
	}
	
	private void normalSnapshotsInfo(List<VolumeSnapshot> snapshots){
		if(Util.isNullOrEmptyList(snapshots))
			return;
		for(VolumeSnapshot snapshot : snapshots){
			snapshot.normalInfo(true);
		}
	}

}
