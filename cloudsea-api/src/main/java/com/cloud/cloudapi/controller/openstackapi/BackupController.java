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
import com.cloud.cloudapi.pojo.openstackapi.forgui.Volume;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.openstackapi.BackupService;
import com.cloud.cloudapi.service.openstackapi.VolumeService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class BackupController extends BaseController{

	@Resource
	private OperationLogService operationLogService;
	@Resource
	private OperationResourceService operationResourceService;
	@Resource
	private BackupService backupService;
	@Resource
	private VolumeService volumeService;
 
	private Logger log = LogManager.getLogger(BackupController.class);
	
	@RequestMapping(value = "/backups", method = RequestMethod.GET)
	public String getBackupsList(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			@RequestParam(value = ParamConstant.VOLUME, defaultValue = "") String volumeId,
			HttpServletResponse response) {
		
		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String, String> paramMap = Util.makeRequestParamInfo(limit, name, status, ParamConstant.VOLUME,volumeId);
			List<Backup> backups = backupService.getBackupList(paramMap, authToken);
			if (Util.isNullOrEmptyList(backups)) {
	//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_BACKUP,ParamConstant.BACKUP,"",Message.SUCCESSED_FLAG,"");
				JsonHelper<List<Backup>, String> jsonHelp = new JsonHelper<List<Backup>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Backup>());
			}
			normalBackupsInfo(backups,new Locale(authToken.getLocale()));
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_BACKUP,ParamConstant.BACKUP,getBackupsId(backups),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<Backup>, String> jsonHelp = new JsonHelper<List<Backup>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(backups);

		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_BACKUP,ParamConstant.BACKUP,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_BACKUP,ParamConstant.BACKUP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_VOLUME_BACKUP_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_BACKUP,ParamConstant.BACKUP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}

	@RequestMapping(value = "/backups/{id}", method = RequestMethod.GET)
	public String getBackup(
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
			Backup backup = backupService.getBackup(id, authToken);
			if (null == backup) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
		//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_BACKUP_DETAIL,ParamConstant.BACKUP,"",Message.FAILED_FLAG,message);
				return message;
			}
			normalBackupInfo(backup,new Locale(authToken.getLocale()));
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_BACKUP_DETAIL,ParamConstant.BACKUP,backup.getId(),Message.SUCCESSED_FLAG,"");
			JsonHelper<Backup, String> jsonHelp = new JsonHelper<Backup, String>();
			return jsonHelp.generateJsonBodyWithEmpty(backup);
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
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_VOLUME_BACKUP_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VOLUME_BACKUP_DETAIL,ParamConstant.BACKUP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}

	@RequestMapping(value = "/backups/{id}", method = RequestMethod.DELETE)
	public String deleteBackup(
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
			this.checkUserPermission(authToken, ParamConstant.BACKUP_DELETE);
			backupService.deleteBackup(id, authToken);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_VOLUME_BACKUP_DELETE_SUCCESSED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_VOLUME_BACKUP,ParamConstant.BACKUP,id,Message.SUCCESSED_FLAG,message);
			return message;
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_VOLUME_BACKUP,ParamConstant.BACKUP,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_VOLUME_BACKUP,ParamConstant.BACKUP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_VOLUME_BACKUP_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_VOLUME_BACKUP,ParamConstant.BACKUP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	@RequestMapping(value = "/backups", method = RequestMethod.POST)
	public String createBackup(
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
        	this.checkUserPermission(authToken, ParamConstant.BACKUP_NEW);
        	Volume volume = backupService.createBackup(createBody, authToken);
			if (null == volume) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VOLUME_BACKUP,ParamConstant.BACKUP,"",Message.FAILED_FLAG,message);
				return message;
			}
			volume.normalInfo(true, new Locale(authToken.getLocale()));
			JsonHelper<Volume, String> jsonHelp = new JsonHelper<Volume, String>();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VOLUME_BACKUP,ParamConstant.BACKUP,volume.getId(),Message.SUCCESSED_FLAG,"");
			return jsonHelp.generateJsonBodyWithEmpty(volume);
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
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_VOLUME_BACKUP_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.GET_VOLUME_BACKUP_DETAIL,ParamConstant.BACKUP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}

	@RequestMapping(value = "/backups/{id}", method = RequestMethod.POST)
	public String restoreBackup(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String restoreBody, HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
        	this.checkUserPermission(authToken, ParamConstant.VOLUME_RESTORE);

			Volume volume = backupService.restoreBackup(restoreBody, id, authToken);
			volume.normalInfo(true, new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_VOLUME_BACKUP_RESTORE,ParamConstant.BACKUP,id,Message.SUCCESSED_FLAG,"");
			JsonHelper<Volume, String> jsonHelp = new JsonHelper<Volume, String>();
			return jsonHelp.generateJsonBodyWithEmpty(volume);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_VOLUME_BACKUP_RESTORE,ParamConstant.BACKUP,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_VOLUME_BACKUP_RESTORE,ParamConstant.BACKUP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_VOLUME_BACKUP_RESTORE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_VOLUME_BACKUP_RESTORE,ParamConstant.BACKUP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	private void normalBackupInfo(Backup backup,Locale locale){
        backup.normalInfo(false);
		Volume volume = backup.getVolume();
		if(null != volume){
			volume.normalInfo(true, locale);
			backup.setVolume(volume);
		}
	}
	
	private void normalBackupsInfo(List<Backup> backups,Locale locale){
		if(Util.isNullOrEmptyList(backups))
			return;
		for(Backup backup : backups){
			normalBackupInfo(backup,locale);
		}
	}
}
