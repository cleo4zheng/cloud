package com.cloud.cloudapi.service.openstackapi.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.BackupMapper;
import com.cloud.cloudapi.dao.common.InstanceMapper;
import com.cloud.cloudapi.dao.common.ResourceCreateProcessMapper;
import com.cloud.cloudapi.dao.common.SyncResourceMapper;
import com.cloud.cloudapi.dao.common.VolumeMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.BackupJSON;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Backup;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceCreateProcess;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SyncResource;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Volume;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.BackupService;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.service.openstackapi.ResourceSpecService;
import com.cloud.cloudapi.service.openstackapi.VolumeService;
import com.cloud.cloudapi.service.pool.PoolResource;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.StringHelper;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("backupService")
public class BackupServiceImpl implements BackupService {
	
	@Autowired
	private BackupMapper backupMapper;
	
	@Autowired
	private VolumeMapper volumeMapper;
	
	@Autowired
	private InstanceMapper instanceMapper;

	@Resource
	private OSHttpClientUtil client;

	@Resource
	private VolumeService volumeService;
	
	@Resource
	private QuotaService quotaService;

	@Autowired
	private SyncResourceMapper syncResourceMapper;
	
	@Autowired
	private ResourceCreateProcessMapper resourceCreateProcessMapper;
	
	@Resource
	private ResourceSpecService resourceSpecService;
	
	@Resource
	private PoolResource poolService;
	
	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(BackupServiceImpl.class);
	
	@Override
	public List<Backup> getBackupList(Map<String, String> paramMap, TokenOs ostoken) throws BusinessException{
		
		int limitItems = Util.getLimit(paramMap);
		Locale locale = new Locale(ostoken.getLocale());
		List<Backup> backupsFromDB = getBackupsFromDB(ostoken.getTenantid(),limitItems);
		if(!Util.isNullOrEmptyList(backupsFromDB)){
			return backupsFromDB;
		}

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/backups/detail", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);

		List<Backup> backups = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				backups = getBackups(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoGet(url, headers);
		    httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
		    if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				backups = getBackups(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, httpCode,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING, httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN, httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, httpCode, locale);
		default:
			throw new ResourceBusinessException(Message.CS_VOLUME_BACKUP_GET_FAILED, httpCode, locale);
		}
		
		backups = storeBackups2DB(backups);
		return getLimitItems(backups,ostoken.getTenantid(),limitItems);
	}
	
	@Override
	public Backup getBackup(String backupId,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		Backup backup = backupMapper.selectByPrimaryKey(backupId);
		if(null != backup){
		   setVolumeInfo2Backup(backup);
		   return backup;
		}
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/backups/");
		sb.append(backupId);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs, locale);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				backup = getBackup(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoGet(sb.toString(), headers);
		    httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
		    if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				backup = getBackup(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_VOLUME_BACKUP_DETAIL_GET_FAILED,httpCode,locale);
		}
		
		backup.setTenantId(ostoken.getTenantid());
		backup.setMillionSeconds(Util.getCurrentMillionsecond());
		setVolumeInfo2Backup(backup);
		storeBackup2DB(backup);
		return backup;
	}
	
	@Override
	public Volume createBackup(String createBody,TokenOs ostoken) throws BusinessException, JsonParseException, JsonMappingException, IOException{
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		Locale locale = new Locale(ostoken.getLocale());

		quotaService.checkResourceQuota(ostoken.getTenantid(), ParamConstant.BACKUP, 1, locale);

		String backupCreateBody = makeVolumeBackBody(createBody,ostoken);

		String region = ostoken.getCurrentRegion();
		// String url=ot.getEndPoint(TokenOs.EP_TYPE_NETWORK,
		// region).getPublicURL();
		// url=url+"/v2.0/networks/" + NetworkId;
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoPost(url + "/backups", ostoken.getTokenid(), backupCreateBody);
		Util.checkResponseBody(rs, locale);

		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		
		Backup backup = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE: {
			try {
				backup = getBackup(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoGet(url + "/backups", headers);
		    httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		    failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
		    if(httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				backup = getBackup(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode, locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode, locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode, locale);
		default:
			throw new ResourceBusinessException(Message.CS_VOLUME_BACKUP_CREATE_FAILED,httpCode, locale);
		}
		
		backup = getBackup(backup.getId(), ostoken);
	//	backup.setTenantId(ostoken.getTenantid());
	//	storeBackup2DB(backup);
		
		Volume volume = updateVolumeInfo(backup,true);
		updateVolumeQuota(ostoken, backup.getSize(), volume.getVolume_type(), true);
		updateBackupQuota(ostoken,ParamConstant.BACKUP,true);

		updateSyncResourceInfo(ostoken.getTenantid(),backup.getId(),null,ParamConstant.AVAILABLE,ParamConstant.BACKUP,ostoken.getCurrentRegion());
		return volume;
	}

	@Override
	public void deleteBackup(String backupId,TokenOs ostoken) throws BusinessException{
		
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(backupId,locale);
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/backups/");
		sb.append(backupId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs, locale);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE:
		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE:{
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoGet(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE || httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode, locale);
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode, locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode, locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode, locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode, locale);
		default:
			throw new ResourceBusinessException(Message.CS_VOLUME_BACKUP_DELETE_FAILED,httpCode, locale);
		}
		
		Backup deletedBackup = backupMapper.selectByPrimaryKey(backupId);
		backupMapper.deleteByPrimaryKey(backupId);
		Volume volume = updateVolumeInfo(deletedBackup,false);
		updateVolumeQuota(ostoken, deletedBackup.getSize(), volume.getVolume_type(), false);
		updateBackupQuota(ostoken,ParamConstant.BACKUP,false);
		updateSyncResourceInfo(ostoken.getTenantid(),backupId,deletedBackup.getStatus(),ParamConstant.DELETED_STATUS,ParamConstant.BACKUP,ostoken.getCurrentRegion());
		
		return;
	}
	
	@Override
	public Volume restoreBackup(String restoreBody,String backupId,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException{
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(backupId,locale);
		
		Volume restoreVolume = new Volume();
		String backupRestoreBody = makeVolumeRestoreBody(restoreVolume,restoreBody);

		String region = ostoken.getCurrentRegion();
		// String url=ot.getEndPoint(TokenOs.EP_TYPE_NETWORK,
		// region).getPublicURL();
		// url=url+"/v2.0/networks/" + NetworkId;
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/backups/");
		sb.append(backupId);
		sb.append("/restore");
		
		Map<String, String> rs = client.httpDoPost(sb.toString(), ostoken.getTokenid(), backupRestoreBody);
		Util.checkResponseBody(rs, locale);

		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE: {
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			rs = client.httpDoPost(sb.toString(), tokenid, backupRestoreBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode, locale);
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode, locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode, locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode, locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode, locale);
		default:
			throw new ResourceBusinessException(Message.CS_VOLUME_BACKUP_RESTORE_FAILED,httpCode, locale);
		}
		
		Backup restoreBackup = backupMapper.selectByPrimaryKey(backupId);
		if(null != restoreBackup){
			updateSyncResourceInfo(ostoken.getTenantid(),backupId,restoreBackup.getStatus(),ParamConstant.AVAILABLE_STATUS,ParamConstant.BACKUP,ostoken.getCurrentRegion());
		//	restoreBackup.setStatus(ParamConstant.RESTORING);
		//	backupMapper.updateByPrimaryKeySelective(restoreBackup);
		}
		Volume orgVolume = volumeMapper.selectByPrimaryKey(restoreVolume.getId());
		if(null == orgVolume){
			orgVolume = volumeService.refreshVolumeInfo(restoreVolume.getId(), ostoken);
		}
		updateSyncResourceInfo(ostoken.getTenantid(),restoreVolume.getId(),orgVolume.getStatus(),ParamConstant.AVAILABLE_STATUS,ParamConstant.VOLUME,ostoken.getCurrentRegion());
		return orgVolume;
	}
	
	private Volume updateVolumeInfo(Backup backup,Boolean add){
		Volume volume = null;
		if(true == add){
			volume = volumeMapper.selectByPrimaryKey(backup.getVolume_id());
			String backupId = Util.getIdWithAppendId(backup.getId(), volume.getBackupId());
			volume.setBackupId(backupId);
			volumeMapper.updateByPrimaryKeySelective(volume);
			volume.addBackup(backup);
			String instanceId = volume.getInstanceId();
			Instance instance = instanceMapper.selectByPrimaryKey(instanceId);
			if (null != instance)
				volume.addInstance(instance);		
		}else{
			volume = volumeMapper.selectByPrimaryKey(backup.getVolume_id());
			String backupId = Util.listToString(Util.getCorrectedIdInfo(volume.getBackupId(), backup.getId()),',');
			volume.setBackupId(backupId);
			volumeMapper.updateByPrimaryKeySelective(volume);
		}
		return volume;
	}
	

	
	private void setVolumeInfo2Backup(Backup backup){
		String attchedVolumeId = backup.getVolume_id();
		Volume volume = volumeMapper.selectByPrimaryKey(attchedVolumeId);
		if(null == volume)
			return;
		// volume.setBackupId(Util.getIdWithAppendId(attchedVolumeId,volume.getBackupId()));
		// volumeMapper.updateByPrimaryKeySelective(volume);
		backup.setVolume(volume);
		backup.setVolume_type(volume.getVolume_type());
	}
	
	private List<Backup> getBackupsFromDB(String tenantId,int limitItems){
		List<Backup> backupsFromDB = null;
		if(-1 == limitItems){
			backupsFromDB = backupMapper.selectAllByTenantId(tenantId);
		}else{
			backupsFromDB = backupMapper.selectAllByTenantIdWithLimit(tenantId,limitItems);
		}
		if(Util.isNullOrEmptyList(backupsFromDB))
			return null;

		List<Backup> backupsWithVolume = new ArrayList<Backup>();
		for (Backup backup : backupsFromDB) {
			setVolumeInfo2Backup(backup);
			backupsWithVolume.add(backup);
		}
		return backupsWithVolume;
	}
	
	private void storeBackup2DB(Backup backup){
		if(null == backup)
			return;
		backupMapper.insertOrUpdate(backup);
	}
	
	private List<Backup> storeBackups2DB(List<Backup> backups){
		if(Util.isNullOrEmptyList(backups))
			return null;
		List<Backup> backupsWithVolume = new ArrayList<Backup>();
		for (Backup backup : backups) {
			storeBackup2DB(backup);
			setVolumeInfo2Backup(backup);
			backupsWithVolume.add(backup);
		}
		return backupsWithVolume;
	}
	
	private Backup getBackup(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode backupNode = rootNode.path(ResponseConstant.BACKUP);
		return getBackupInfo(backupNode);
	}
	
	private List<Backup> getBackups(Map<String, String> rs) throws JsonProcessingException, IOException, BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode backupsNode = rootNode.path(ResponseConstant.BACKUPS);
		int backupsCount = backupsNode.size();
		if (0 == backupsCount)
			return null;
		
		List<Backup> backups = new ArrayList<Backup>();
		for (int index = 0; index < backupsCount; ++index) {
			Backup backup = getBackupInfo(backupsNode.get(index));
			if(null == backup)
				continue;
			backups.add(backup);
		}
		
		return backups;
	}
	
	private String makeVolumeBackBody(String createBody,TokenOs ostoken) throws JsonParseException, JsonMappingException, IOException, BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(createBody);
		JsonNode backupNode = rootNode.path(ResponseConstant.BACKUP);
		Backup backupInfo = new Backup();
		backupInfo.setName(StringHelper.string2Ncr(backupNode.path(ResponseConstant.NAME).textValue()));
		backupInfo.setVolume_id(backupNode.path(ResponseConstant.VOLUME_ID).textValue());
		checkName(backupInfo.getName(),ostoken);
		Volume volume = volumeMapper.selectByPrimaryKey(backupInfo.getVolume_id());
		if(volume != null && volume.getStatus().equalsIgnoreCase(ParamConstant.INUSE_STATUS))
			backupInfo.setForce(true);
		else
			backupInfo.setForce(false);
//		Backup backupInfo = mapper.readValue(createBody, Backup.class);
		backupInfo.setContainer(new String());
	//	backupInfo.setIncremental(true); //may change it it is related to driver
		BackupJSON backupCreate = new BackupJSON(backupInfo);
		JsonHelper<BackupJSON, String> jsonHelp = new JsonHelper<BackupJSON, String>();
		return jsonHelp.generateJsonBodyWithEmpty(backupCreate);
	}
	
	private String makeVolumeRestoreBody(Volume restoreVolume,String restoreBody) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(restoreBody);
		StringBuilder sb = new StringBuilder();
		sb.append("{\"restore\":{");
		sb.append("\"volume_id\":\"");
		sb.append(rootNode.path(ResponseConstant.VOLUME_ID).textValue());
		sb.append("\"}}");
		restoreVolume.setId(rootNode.path(ResponseConstant.VOLUME_ID).textValue());
		return sb.toString();
	}
	
	private Backup getBackupInfo(JsonNode backupNode){
		if(null == backupNode)
			return null;
		Backup backup = new Backup();
		backup.setId(backupNode.path(ResponseConstant.ID).textValue());
		backup.setName(backupNode.path(ResponseConstant.NAME).textValue()); 
//		if(!Util.isNullOrEmptyValue(backupNode.path(ResponseConstant.CREATED_AT).textValue())){
//			backup.setCreatedAt(backupNode.path(ResponseConstant.CREATED_AT).textValue());
//			backup.setMillionSeconds(Util.time2Millionsecond(backup.getCreatedAt(),ParamConstant.TIME_FORMAT_02));
//		}else{
//			backup.setMillionSeconds(Util.time2Millionsecond(Util.getCurrentDate(),ParamConstant.TIME_FORMAT_01));
//		}
		backup.setSize(backupNode.path(ResponseConstant.SIZE).intValue());
		backup.setStatus(backupNode.path(ResponseConstant.STATUS).textValue().toUpperCase());
		backup.setVolume_id(backupNode.path(ResponseConstant.VOLUME_ID).textValue());
		
		return backup;
		
//		backup.setAvailabilityZone(backupNode.path(ResponseConstant.AVAILABILITY_ZONE).textValue());
//		backup.setContainer(backupNode.path(ResponseConstant.CONTAINER).textValue());
//		backup.setDescription(backupNode.path(ResponseConstant.DESCRIPTION).textValue());
//		backup.setFailReason(backupNode.path(ResponseConstant.FAIL_RESON).textValue());
//		backup.setObjectCount(backupNode.path(ResponseConstant.OBJECT_COUNT).intValue());
//		backup.setIsIncremental(backupNode.path(ResponseConstant.IS_INCREMENTAL).booleanValue());
//		backup.setHasDependentBackups(backupNode.path(ResponseConstant.HAS_DEPENDENT_BACKUPS).booleanValue());
		
	}
	

	private List<Backup> getLimitItems(List<Backup> backups,String tenantId,int limit){
		if(Util.isNullOrEmptyList(backups))
			return null;
		List<Backup> tenantBackups = new ArrayList<Backup>();
		for(Backup backup : backups){
			if(!tenantId.equals(backup.getTenantId()))
				continue;
			tenantBackups.add(backup);
		}
		if(-1 != limit){
			if(limit <= tenantBackups.size())
				return tenantBackups.subList(0, limit);
		}
		return tenantBackups;
	}
	
	private void updateVolumeQuota(TokenOs ostoken, int diskSize, String type, boolean bAdd) {
		quotaService.updateQuota(type, ostoken, bAdd, diskSize);
		resourceSpecService.updateResourceSpecQuota(type, ParamConstant.DISK, diskSize, bAdd);
		
		Map<String,Integer> resourceQuota = new HashMap<String,Integer>();
		resourceQuota.put(type, diskSize);
		poolService.updatePoolQuota(ostoken.getTenantid(), resourceQuota, bAdd);
	}
	
	private void updateSyncResourceInfo(String tenantId,String id,String orgStatus,String expectedStatus,String type,String region){
		SyncResource resource = new SyncResource();
		resource.setId(id);
		resource.setType(type);
		resource.setOrgStatus(orgStatus);
		resource.setExpectedStatus(expectedStatus);
		resource.setRegion(region);
		syncResourceMapper.insertSelective(resource);
		
		ResourceCreateProcess createProcess = new ResourceCreateProcess();
		createProcess.setId(id);
		createProcess.setTenantId(tenantId);
		createProcess.setType(type);
		createProcess.setBegineSeconds(Util.time2Millionsecond(Util.getCurrentDate(),ParamConstant.TIME_FORMAT_01));
		resourceCreateProcessMapper.insertOrUpdate(createProcess);
	}
	
	private void checkResource(String id, Locale locale) throws BusinessException {
		SyncResource syncResource = syncResourceMapper.selectByPrimaryKey(id);
		if (null != syncResource)
			throw new ResourceBusinessException(Message.CS_RESOURCE_IS_DOING,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE, locale);
	}
	
	private void checkName(String name,TokenOs ostoken)  throws BusinessException{
		if(Util.isNullOrEmptyValue(name))
			return;
		List<Backup> backups = backupMapper.selectAllByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(backups))
			return;
		for(Backup backup : backups){
			if(name.equals(backup.getName()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_NAME_IS_SAME,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		return;
	}
	
	
	private void updateBackupQuota(TokenOs ostoken,String type,boolean bAdd){
		quotaService.updateQuota(type,ostoken,bAdd,1);
		resourceSpecService.updateResourceSpecQuota(type,ParamConstant.BACKUP,1,bAdd);
	}
}
