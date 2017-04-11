package com.cloud.cloudapi.service.openstackapi.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.BackupMapper;
import com.cloud.cloudapi.dao.common.InstanceMapper;
import com.cloud.cloudapi.dao.common.ResourceCreateProcessMapper;
import com.cloud.cloudapi.dao.common.ResourceEventMapper;
import com.cloud.cloudapi.dao.common.SyncResourceMapper;
import com.cloud.cloudapi.dao.common.VolumeMapper;
import com.cloud.cloudapi.dao.common.VolumeSnapshotMapper;
import com.cloud.cloudapi.dao.common.VolumeTypeMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.VolumeJSON;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Backup;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceCreateProcess;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceEvent;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SyncResource;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Volume;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeSnapshot;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeType;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.ConfigService;
import com.cloud.cloudapi.service.openstackapi.InstanceService;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.service.openstackapi.ResourceSpecService;
import com.cloud.cloudapi.service.openstackapi.VolumeService;
import com.cloud.cloudapi.service.openstackapi.VolumeTypeService;
import com.cloud.cloudapi.service.pool.PoolResource;
import com.cloud.cloudapi.service.rating.RatingTemplateService;
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

@Service("volumeService")
public class VolumeServiceImpl implements VolumeService {

	@Resource
	private OSHttpClientUtil client;

	@Autowired
	private VolumeMapper volumeMapper;

	@Autowired
	private VolumeTypeMapper volumeTypeMapper;
	
	@Autowired
	private InstanceMapper instanceMapper;

	@Autowired
	private BackupMapper backupMapper;

	@Autowired
	private SyncResourceMapper syncResourceMapper;
	
	@Autowired
	private ResourceCreateProcessMapper resourceCreateProcessMapper;

	@Autowired
	private ResourceEventMapper resourceEventMapper;
	
	@Autowired
	private VolumeSnapshotMapper volumeSnapshotMapper;
	
	@Autowired
	private CloudConfig cloudconfig;

	@Resource
	private QuotaService quotaService;

	@Resource
	private InstanceService instanceService;

	@Resource
	private VolumeTypeService volumeTypeService;

	@Resource
	private ResourceSpecService resourceSpecService;

	@Resource
	private RatingTemplateService ratingTemplateService;
	
	@Resource
	private PoolResource poolService;
	
	@Resource
	private ConfigService configService;
	
	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(VolumeServiceImpl.class);
	
	public BackupMapper getBackupMapper() {
		return backupMapper;
	}

	public void setBackupMapper(BackupMapper backupMapper) {
		this.backupMapper = backupMapper;
	}

	public VolumeMapper getVolumeMapper() {
		return volumeMapper;
	}

	public void setVolumeMapper(VolumeMapper volumeMapper) {
		this.volumeMapper = volumeMapper;
	}

	public CloudConfig getCloudconfig() {
		return cloudconfig;
	}

	public void setCloudconfig(CloudConfig cloudconfig) {
		this.cloudconfig = cloudconfig;
	}

	public VolumeServiceImpl() {
		super();
	}

	@Override
	public List<Volume> getVolumeList(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {

		int limitItems = Util.getLimit(paramMap);
		Locale locale = new Locale(ostoken.getLocale());
		String status = null != paramMap ? paramMap.get(ParamConstant.STATUS) : null;
		List<Volume> volumesFromDB = getVolumesFromDB(ostoken.getTenantid(), limitItems, status,locale);
		if (!Util.isNullOrEmptyList(volumesFromDB)){
			return volumesFromDB;
		}
			
		// todo 1: 閫氳繃guitokenid 鍙栧緱瀹為檯锛岀敤鎴蜂俊鎭�
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/volumes/detail", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
        
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		List<Volume> volumes = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				volumes = getVolumes(rs,locale);
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
			rs = client.httpDoGet(url, headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				volumes = getVolumes(rs,locale);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_GET_FAILED,httpCode,locale);
		}

		volumes = storeVolumes2DB(volumes, ostoken);
		return getLimitItems(volumes, limitItems, status, ostoken.getTenantid());
	}

	@Override
	public Volume refreshVolumeInfo(String volumeId, TokenOs ostoken) throws BusinessException {
	
		Volume volume = null;
		Locale locale = new Locale(ostoken.getLocale());
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/volumes/");
		sb.append(volumeId);
		url = RequestUrlHelper.createFullUrl(sb.toString(), null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));

		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				volume = getVolume(rs,locale);
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
			rs = client.httpDoGet(url, headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				volume = getVolume(rs,locale);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_DETAIL_GET_FAILED,httpCode,locale);
		}

		Volume volumeFromDB = volumeMapper.selectByPrimaryKey(volumeId);
		if(null != volumeFromDB)
			volume.setMillionSeconds(volumeFromDB.getMillionSeconds());
		else
			volume.setMillionSeconds(Util.getCurrentMillionsecond());
		storeVolume2DB(volume);
		setBackupInfo(volume);
		//volume.setCreatedAt(Util.millionSecond2Date(volume.getMillionSeconds()));
		return volume;
	}
	
	@Override
	public Volume getVolume(String volumeId, TokenOs ostoken) throws BusinessException {
		Volume volume = volumeMapper.selectByPrimaryKey(volumeId);
		if (null != volume) {
			setInstanceInfo(volume,new Locale(ostoken.getLocale()));
			setBackupInfo(volume);
			return volume;
		}
		volume = refreshVolumeInfo(volumeId,ostoken);
		return volume;
	}

	private void storeVolume2DB(Volume volume) {
		if (null == volume)
			return;
		volumeMapper.insertOrUpdate(volume);
		// if (null != volumeMapper.selectByPrimaryKey(volume.getId()))
		// volumeMapper.updateByPrimaryKeySelective(volume);
		// else
		// volumeMapper.insertSelective(volume);
	}

	private List<Volume> storeVolumes2DB(List<Volume> volumes, TokenOs ostoken) throws BusinessException {
		if (Util.isNullOrEmptyList(volumes))
			return null;
		List<Volume> volumesWithInstance = new ArrayList<Volume>();
		for (Volume volume : volumes) {
			storeVolume2DB(volume);
			Instance instance = instanceMapper.selectByPrimaryKey(volume.getInstanceId());
			if (null != instance) {
				volume.addInstance(instance);
			} else {
				try {
					addInstnceInfo2Volume(volume, ostoken);
				} catch (Exception e) {
					log.error(e);// TODO
				}
			}
			setBackupInfo(volume);
			volumesWithInstance.add(volume);
		}
		return volumesWithInstance;
	}

	private void addInstnceInfo2Volume(Volume volume, TokenOs ostoken) throws BusinessException {
		if (null == volume)
			return;
		if (null != volume.getInstanceId() && !volume.getInstanceId().isEmpty()) {
			Instance instance = instanceService.getInstance(volume.getInstanceId(), ParamConstant.INSTANCE_TYPE,
					ostoken, false);
			volume.addInstance(instance);
		}
	}

	@Override
	public Volume createVolume(String createBody, TokenOs ostoken)
			throws BusinessException, JsonParseException, JsonMappingException, IOException {

		Volume volumeInfo = makeVolumeInfo(createBody);
		volumeInfo.setName(StringHelper.string2Ncr(volumeInfo.getName()));
		checkVolumeName(volumeInfo.getName(),ostoken);
		volumeInfo.setDescription(StringHelper.string2Ncr(volumeInfo.getDescription()));
		String volumeCreateBody = getVolumeCreateBody(volumeInfo);
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		url += "/volumes";

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPost(url, headers, volumeCreateBody);
		Util.checkResponseBody(rs, locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		Volume volume = null;

		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE: {
			try {
				volume = getVolume(rs,locale);
			}  catch (Exception e) {
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
			rs = client.httpDoPost(url, headers, volumeCreateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				volume = getVolume(rs,locale);
			}  catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_CREATE_FAILED,httpCode,locale);
		}

		try {
			volume.setTenantId(ostoken.getTenantid());
			volume.setMillionSeconds(Util.getCurrentMillionsecond());
			storeVolume2DB(volume);
			updateSyncResourceInfo(ostoken.getTenantid(),volume.getId(),null,ParamConstant.AVAILABLE_STATUS,ostoken.getCurrentRegion(),volume.getName(),ParamConstant.VOLUME);
			updateVolumeQuota(ostoken, volumeInfo.getSize(), volumeInfo.getVolume_type(), true);
			storeResourceEventInfo(ostoken.getTenantid(),volume.getId(),ParamConstant.VOLUME,null,ParamConstant.AVAILABLE_STATUS.toUpperCase(),volume.getMillionSeconds());
		} catch (Exception e) {
			// TODO
		}
		return volume;
	}
	
	@Override
	public Volume updateVolume(String volumeId, String updateBody, TokenOs ostoken)
			throws BusinessException, JsonParseException, JsonMappingException, IOException {

		Locale locale = new Locale(ostoken.getLocale());
		checkResource(volumeId,false,locale);
		
		Volume volumeInfo = makeVolumeInfo(updateBody);
		volumeInfo.setName(StringHelper.string2Ncr(volumeInfo.getName()));
		checkVolumeName(volumeInfo.getName(),ostoken);
		
		String volumeUpdateBody = getVolumeCreateBody(volumeInfo);
        
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/volumes/");
		sb.append(volumeId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoPut(sb.toString(), headers, volumeUpdateBody);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		Volume volume = null;

		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				volume = getVolume(rs,locale);
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
			rs = client.httpDoPut(sb.toString(), headers, volumeUpdateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				volume = getVolume(rs,locale);
			}  catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_UPDATE_FAILED,httpCode,locale);
		}
		volume.setTenantId(ostoken.getTenantid());
		Volume volumeFromDB = volumeMapper.selectByPrimaryKey(volumeId);
		if(null != volumeFromDB)
			volume.setMillionSeconds(volumeFromDB.getMillionSeconds());
		storeVolume2DB(volume);
		
		return volume;
	}

	@Override
	public List<VolumeSnapshot> getSnapshots(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {

		int limitItems = Util.getLimit(paramMap);
		Locale locale = new Locale(ostoken.getLocale());
		String status = null != paramMap ? paramMap.get(ParamConstant.STATUS) : null;
		List<VolumeSnapshot> snapshots = getSnapshotsFromDB(ostoken.getTenantid(), limitItems, status,locale);
		if (!Util.isNullOrEmptyList(snapshots)){
			makeSnapshotsVolumeInfo(snapshots);
			return snapshots;
		}
			
		// todo 1: 閫氳繃guitokenid 鍙栧緱瀹為檯锛岀敤鎴蜂俊鎭�
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/snapshots/detail", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
        
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				snapshots = getSnapshots(rs,locale);
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
			rs = client.httpDoGet(url, headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				snapshots = getSnapshots(rs,locale);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_SNAPSHOT_GET_FAILED,httpCode,locale);
		}

		storeSnapshots2DB(snapshots, ostoken);
		snapshots = getSnapshotLimitItems(snapshots, limitItems, status, ostoken.getTenantid());
		makeSnapshotsVolumeInfo(snapshots);
		return snapshots;
	}
	
	@Override
	public VolumeSnapshot getSnapshot(String snapshotId, TokenOs ostoken) throws BusinessException {
		VolumeSnapshot snapshot = volumeSnapshotMapper.selectByPrimaryKey(snapshotId);
		makeSnapshotVolumeInfo(snapshot);
		return snapshot;
	}
	
	@Override
	public VolumeSnapshot createSnapshotForInstance(String volumeId,TokenOs ostoken) throws BusinessException{
		
		Volume volume = volumeMapper.selectByPrimaryKey(volumeId);
		if(null == volume)
			return null;
		List<VolumeSnapshot> snapshots = volumeSnapshotMapper.selectByVolumeId(volumeId);
		int index = 1;
		if(!Util.isNullOrEmptyList(snapshots))
			index += snapshots.size();
		
		String name = String.format("snapshot %s for %s",index,volume.getName());
		
		StringBuilder body = new StringBuilder();
		body.append("{");
		body.append("\"");
		body.append(ParamConstant.SNAPSHOT);
		body.append("\":{");
		body.append("\"name\":\"");
		body.append(name);
		body.append("\",");
		body.append("\"volume_id\":\"");
		body.append(volumeId);
		body.append("\"}}");
		
		return createSnapshot(body.toString(),ostoken,new Locale(ostoken.getLocale()));
	}
	
	@Override
	public VolumeSnapshot createSnapshot(String createBody, TokenOs ostoken)
			throws BusinessException {

		Locale locale = new Locale(ostoken.getLocale());
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(createBody);
		} catch (Exception e) {
			log.error("error",e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE, locale);
		}
		String name = StringHelper.string2Ncr(rootNode.path(ResponseConstant.NAME).textValue());
		String description = null;
		if(!rootNode.path(ResponseConstant.DESCRIPTION).isMissingNode())
			description = rootNode.path(ResponseConstant.DESCRIPTION).textValue();
		String volumeId = rootNode.path(ResponseConstant.VOLUMEID).textValue();
		SyncResource syncResource = syncResourceMapper.selectByPrimaryKey(volumeId);
		if (null != syncResource)
			throw new ResourceBusinessException(Message.CS_VOLUME_IS_DOING,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE, locale);

		checkSnapshotName(name,ostoken);
		
		StringBuilder body = new StringBuilder();
		body.append("{");
		body.append("\"");
		body.append(ParamConstant.SNAPSHOT);
		body.append("\":{");
		body.append("\"name\":\"");
		body.append(name);
		body.append("\",");
		if(null != description){
			body.append("\"description\":\"");
			body.append(description);
			body.append("\",");
		}
		body.append("\"volume_id\":\"");
		body.append(volumeId);
		body.append("\"}}");
	
		return createSnapshot(body.toString(),ostoken,locale);
	}
	
	private VolumeSnapshot createSnapshot(String body,TokenOs ostoken,Locale locale) throws BusinessException{
		
		quotaService.checkResourceQuota(ostoken.getTenantid(), ParamConstant.SNAPSHOT, 1, locale);
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		url += "/snapshots";

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		Map<String, String> rs = client.httpDoPost(url, headers, body);
		Util.checkResponseBody(rs,locale);

		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		VolumeSnapshot snapshot = null;
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE: {
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				JsonNode snapshotNode = rootNode.path(ResponseConstant.SNAPSHOT);
				snapshot = getSnapshotInfo(snapshotNode, locale);
			}  catch (Exception e) {
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
			rs = client.httpDoPost(url, headers, body);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				JsonNode snapshotNode = rootNode.path(ResponseConstant.SNAPSHOT);
				snapshot = getSnapshotInfo(snapshotNode, locale);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_SNAPSHOT_CREATE_FAILED,httpCode,locale);
		}
		
		snapshot.setMillionSeconds(Util.getCurrentMillionsecond());
		volumeSnapshotMapper.insertOrUpdate(snapshot);
		updateSnapshotQuota(ostoken,ParamConstant.SNAPSHOT,true);
		updateSyncResourceInfo(ostoken.getTenantid(),snapshot.getId(),null,ParamConstant.AVAILABLE_STATUS,ostoken.getCurrentRegion(),snapshot.getName(),ParamConstant.SNAPSHOT);

		return snapshot;
	}
	
	@Override
	public VolumeSnapshot updateSnapshot(String snapshotId, String updateBody, TokenOs ostoken)
			throws BusinessException{

		Locale locale = new Locale(ostoken.getLocale());
		checkResource(snapshotId,false,locale);
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(updateBody);
		} catch (Exception e) {
			log.error("error",e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE, locale);
		}
		String name = null;
		if(!rootNode.path(ResponseConstant.NAME).isMissingNode())
			name = StringHelper.string2Ncr(rootNode.path(ResponseConstant.NAME).textValue());
		String description = null;
		if(!rootNode.path(ResponseConstant.DESCRIPTION).isMissingNode())
			description = rootNode.path(ResponseConstant.DESCRIPTION).textValue();

		checkSnapshotName(name,ostoken);
		StringBuilder body = new StringBuilder();
		if(null != name && null != description){
			body.append("{");
			body.append("\"");
			body.append(ParamConstant.SNAPSHOT);
			body.append("\":{");
			body.append("\"name\":\"");
			body.append(name);
			body.append("\",");
			if(null != description){
				body.append("\"description\":\"");
				body.append(description);
				body.append("\"");
			}
			body.append("}}");
		}else if(null != name){
			body.append("{");
			body.append("\"");
			body.append(ParamConstant.SNAPSHOT);
			body.append("\":{");
			body.append("\"name\":\"");
			body.append(name);
			body.append("\"}}");
		}else{
			body.append("{");
			body.append("\"");
			body.append(ParamConstant.SNAPSHOT);
			body.append("\":{");
			body.append("\"description\":\"");
			body.append(description);
			body.append("\"}}");
		}
		
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/snapshots/");
		sb.append(snapshotId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoPut(sb.toString(), headers, body.toString());
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
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
			rs = client.httpDoPut(sb.toString(), headers, body.toString());
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_SNAPSHOT_UPDATE_FAILED,httpCode,locale);
		}
		
		VolumeSnapshot snapshot = volumeSnapshotMapper.selectByPrimaryKey(snapshotId);
		if(null != snapshot){
			if(null != name)
				snapshot.setName(name);
			if(null != description)
				snapshot.setDescription(description);
			volumeSnapshotMapper.updateByPrimaryKeySelective(snapshot);
		}
		return snapshot;
	}
	
	@Override
	public void deleteSnapshot(String snapshotId, TokenOs ostoken) throws BusinessException {
		
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(snapshotId,false,locale);
		
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/snapshots/");
		sb.append(snapshotId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoDelete(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE:
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
				log.error(e);
				return;
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoDelete(sb.toString(), ostoken.getTokenid());
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
    		break;
		}
		default:
			break;
		}
		updateSnapshotDBInfoAfterDelete(ostoken,snapshotId);
	}

	private void updateSnapshotQuota(TokenOs ostoken,String type,boolean bAdd){
		quotaService.updateQuota(type,ostoken,bAdd,1);
		resourceSpecService.updateResourceSpecQuota(type,ParamConstant.SNAPSHOT,1,bAdd);
		
	//	Map<String,Integer> resourceQuota = new HashMap<String,Integer>();
	//	resourceQuota.put(type, 1);
	//	poolService.updatePoolQuota(ostoken.getTenantid(), resourceQuota, bAdd);
	}
	
	private void updateSnapshotDBInfoAfterDelete(TokenOs ostoken, String snapshotId) {
		
		VolumeSnapshot snapshot = volumeSnapshotMapper.selectByPrimaryKey(snapshotId);
		if(null == snapshot)
			return;
		updateSnapshotQuota(ostoken,ParamConstant.SNAPSHOT,false);
		volumeSnapshotMapper.deleteByPrimaryKey(snapshotId);
		updateSyncResourceInfo(ostoken.getTenantid(),snapshot.getId(),snapshot.getStatus(),ParamConstant.DELETED_STATUS,ostoken.getCurrentRegion(),snapshot.getName(),ParamConstant.SNAPSHOT);
	}
	
	@Override
	public String deleteVolumes(String deleteBody, TokenOs ostoken)
			throws BusinessException, JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(deleteBody);
		JsonNode idsNode = rootNode.path(ResponseConstant.IDS);
		int idsCount = idsNode.size();
		Locale locale = new Locale(ostoken.getLocale());
		for (int index = 0; index < idsCount; ++index) {
			String volumeId = idsNode.get(index).textValue();
			checkResource(volumeId,true,locale);
			checkSystemVolume(volumeId,locale);
			checkVolumeSnapshot(volumeId,locale);
		}
		
//		boolean deletedFailed = false;
		List<String> deletedVolumeId = new ArrayList<String>();
		for (int index = 0; index < idsCount; ++index) {
			String volumeId = idsNode.get(index).textValue();
	//		checkResource(volumeId,new Locale(ostoken.getLocale()));
			deleteVolume(volumeId, ostoken);
			deletedVolumeId.add(volumeId);
			
		}

//		if (true == deletedFailed) {
//			throw new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_DELETE_FAILED,ParamConstant.NOT_FOUND_RESPONSE_CODE,new Locale(ostoken.getLocale()));
//		}
		updateVolumeDBInfoAfterDelete(ostoken, deletedVolumeId);
		return Util.listToString(deletedVolumeId, ',');
	}

	@Override
	public void deleteVolume(String volumeId, TokenOs ostoken) throws BusinessException {
		
		Locale locale = new Locale(ostoken.getLocale());
	//	checkResource(volumeId,locale);
		
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/volumes/");
		sb.append(volumeId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoDelete(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE:
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
				log.error(e);
				return;
		//		throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoDelete(sb.toString(), ostoken.getTokenid());
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
//			if (httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE
//					|| httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE)
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			break;
		}
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			break;
//			throw new ResourceBusinessException(Message.CS_COMPUTE_VOLUME_DELETE_FAILED,httpCode,locale);
		}
	//	 updateVolumeDBInfoAfterDelete(ostoken,volumeId);
	}

	private void updateVolumeDBInfoAfterDelete(TokenOs ostoken, List<String> volumesId) {
		if (Util.isNullOrEmptyList(volumesId))
			return;
		List<Volume> volumes = volumeMapper.getVolumesByIds(volumesId);
		if(Util.isNullOrEmptyList(volumes))
			return;
		Map<String, Integer> releaseDisk = new HashMap<String, Integer>();

		for (Volume volume : volumes) {
			storeResourceEventInfo(ostoken.getTenantid(), volume.getId(), ParamConstant.VOLUME, volume.getStatus(),
					ParamConstant.DELETED_STATUS, Util.getCurrentMillionsecond());

			if (releaseDisk.containsKey(volume.getVolume_type())) {
				releaseDisk.put(volume.getVolume_type(), releaseDisk.get(volume.getVolume_type()) + volume.getSize());
			} else {
				releaseDisk.put(volume.getVolume_type(), volume.getSize());
			}

			Instance instance = instanceMapper.selectByPrimaryKey(volume.getInstanceId());
			if (null != instance) {
				List<String> volumeIds = Util.getCorrectedIdInfo(instance.getVolumeIds(), volume.getId());
				instance.setVolumeIds(Util.listToString(volumeIds, ','));
				instanceMapper.updateByPrimaryKeySelective(instance);
			}
		}
		volumeMapper.deleteVolumesByIds(volumesId);
		for (Entry<String, Integer> entry : releaseDisk.entrySet()) {
			quotaService.updateQuota(entry.getKey(), ostoken, false, entry.getValue());
			resourceSpecService.updateResourceSpecQuota(entry.getKey(),ParamConstant.DISK, entry.getValue(), false);
		}
		for (Volume volume : volumes) {
			updateSyncResourceInfo(ostoken.getTenantid(),volume.getId(),volume.getStatus(),ParamConstant.DELETED_STATUS,ostoken.getCurrentRegion(),volume.getName(),ParamConstant.VOLUME);
		}
	}

	@Override
	public List<Volume> selectVolumes(String instanceId) {
		return this.volumeMapper.selectListByInstanceId(instanceId);
	}

	private Volume getVolume(Map<String, String> rs,Locale locale) throws JsonProcessingException, IOException, BusinessException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode volumeNode = rootNode.path(ResponseConstant.VOLUME);
		return getVolumeInfo(volumeNode,locale);
	}

	private List<Volume> getVolumes(Map<String, String> rs,Locale locale)
			throws JsonProcessingException, IOException, BusinessException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode volumesNode = rootNode.path(ResponseConstant.VOLUMES);
		int volumesCount = volumesNode.size();
		if (0 == volumesCount)
			return null;

		List<Volume> volumes = new ArrayList<Volume>();
		for (int index = 0; index < volumesCount; ++index) {
			Volume volume = getVolumeInfo(volumesNode.get(index),locale);
			if (volume.getBootable()) // skip system volume
				continue;
			volumes.add(volume);
		}

		return volumes;
	}

	private Volume getVolumeInfo(JsonNode volumeNode,Locale locale) throws BusinessException {
		if (null == volumeNode)
			return null;
		Volume volume = new Volume();
		volume.setId(volumeNode.path(ResponseConstant.ID).textValue());
		volume.setName(volumeNode.path(ResponseConstant.NAME).textValue());
		volume.setTenantId(volumeNode.path(ResponseConstant.OS_VOL_TENANT_ATTR_ID).textValue());
		volume.setVolume_type(volumeNode.path(ResponseConstant.VOLUME_TYPE).textValue());
		if(!Util.isNullOrEmptyValue(volume.getVolume_type())){
			VolumeType volumeType = volumeTypeMapper.selectByName(volume.getVolume_type());
			if(null != volumeType){
				volume.setVolumeTypeName(volumeType.getDisplayName());
			}
		}
			
			//volume.setVolumeTypeName(Message.getMessage(volume.getVolume_type().toUpperCase(),locale,false));
		volume.setMultiattach(volumeNode.path(ResponseConstant.MULTIATTACH).booleanValue());
		//volume.setCreatedAt(volumeNode.path(ResponseConstant.CREATED_AT).textValue());
		//volume.setMillionSeconds(Util.time2Millionsecond(volumeNode.path(ResponseConstant.CREATED_AT).textValue(), ParamConstant.TIME_FORMAT_02));
		volume.setSize(volumeNode.path(ResponseConstant.SIZE).intValue());
		if(!Util.isNullOrEmptyValue(volumeNode.path(ResponseConstant.STATUS).textValue()))
			volume.setStatus(volumeNode.path(ResponseConstant.STATUS).textValue().toUpperCase());
		volume.setDescription(volumeNode.path(ResponseConstant.DESCRIPTION).textValue());
		volume.setVolumeId(volumeNode.path(ResponseConstant.VOLUME_ID).textValue());
		if (ParamConstant.TRUE.equalsIgnoreCase(volumeNode.path(ResponseConstant.BOOTABLE).textValue()))
			volume.setBootable(true);
		else
			volume.setBootable(false);

		JsonNode attachments = volumeNode.path(ResponseConstant.ATTACHMENTS);
		if (null == attachments || 0 == attachments.size())
			return volume;

		JsonNode attachment = attachments.get(0);
		volume.setInstanceId(attachment.path(ResponseConstant.SERVER_ID).textValue());
		volume.setDevice(attachment.path(ResponseConstant.DEVICE).textValue());
		return volume;
	}

	@Override
	public VolumeConfig getVolumeConfig(TokenOs ostoken) throws BusinessException {
		return this.configService.getVolumeConfig(ostoken);
	}

	private List<VolumeSnapshot> getSnapshots(Map<String, String> rs,Locale locale)
			throws JsonProcessingException, IOException, BusinessException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode snapshotsNode = rootNode.path(ResponseConstant.SNAPSHOTS);
		int snapshotsCount = snapshotsNode.size();
		if (0 == snapshotsCount)
			return null;

		List<VolumeSnapshot> snapshots = new ArrayList<VolumeSnapshot>();
		for (int index = 0; index < snapshotsCount; ++index) {
			VolumeSnapshot snapthot = getSnapshotInfo(snapshotsNode.get(index),locale);
			snapshots.add(snapthot);
		}

		return snapshots;
	}
	
	private VolumeSnapshot getSnapshotInfo(JsonNode snapshotNode,Locale locale) throws BusinessException {
		if (null == snapshotNode)
			return null;
		VolumeSnapshot snapshot = new VolumeSnapshot();
		snapshot.setId(snapshotNode.path(ResponseConstant.ID).textValue());
		snapshot.setStatus(snapshotNode.path(ResponseConstant.STATUS).textValue());
		snapshot.setName(snapshotNode.path(ResponseConstant.NAME).textValue());
		snapshot.setVolumeId(snapshotNode.path(ResponseConstant.VOLUME_ID).textValue());
		snapshot.setSize(snapshotNode.path(ResponseConstant.SIZE).intValue());
		snapshot.setDescription(snapshotNode.path(ResponseConstant.DESCRIPTION).textValue());
		return snapshot;
	}

	private void makeSnapshotVolumeInfo(VolumeSnapshot snapshot){
		if(null == snapshot)
			return;
		Volume volume = volumeMapper.selectByPrimaryKey(snapshot.getVolumeId());
		snapshot.setVolume(volume);
	}
	
	private void makeSnapshotsVolumeInfo(List<VolumeSnapshot> snapshots){
		if(Util.isNullOrEmptyList(snapshots))
			return;
		for(VolumeSnapshot snapshot : snapshots)
			makeSnapshotVolumeInfo(snapshot);
	}
	
	private void storeSnapshots2DB(List<VolumeSnapshot> snapshots, TokenOs ostoken) throws BusinessException {
		if (Util.isNullOrEmptyList(snapshots))
			return;
		volumeSnapshotMapper.insertOrUpdateBatch(snapshots);
	}
	
	private Volume makeVolumeInfo(String createBody) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(createBody, Volume.class);
	}

	private String getVolumeCreateBody(Volume volumeInfo) {
		VolumeJSON volumeCreate = new VolumeJSON(volumeInfo);
		JsonHelper<VolumeJSON, String> jsonHelp = new JsonHelper<VolumeJSON, String>();
		return jsonHelp.generateJsonBodySimple(volumeCreate);
	}

	private List<Volume> filterVolumeByStatus(List<Volume> volumes, String status) {
		if (Util.isNullOrEmptyValue(status))
			return volumes;
		List<Volume> filterVolumes = new ArrayList<Volume>();
		for (Volume volume : volumes) {
			if (volume.getBootable()) // skip system volume
				continue;
			if (status.equals(volume.getStatus()))
				filterVolumes.add(volume);

		}
		return filterVolumes;
	}

	private List<Volume> getVolumesFromDB(String tenantId, int limitItems, String status,Locale locale) {
		List<Volume> volumesFromDB = null;
		if (-1 == limitItems) {
			if (Util.isNullOrEmptyValue(status))
				volumesFromDB = volumeMapper.selectListByTenantId(tenantId);
			else
				volumesFromDB = volumeMapper.selectListByTenantIdAndStatus(tenantId, status);
		} else {
			if (Util.isNullOrEmptyValue(status))
				volumesFromDB = volumeMapper.selectListByTenantIdWithLimit(tenantId, limitItems);
			else {
				volumesFromDB = volumeMapper.selectListByTenantIdAndStatus(tenantId, status);
				if (limitItems <= volumesFromDB.size())
					volumesFromDB = volumesFromDB.subList(0, limitItems);
			}
		}

		if (Util.isNullOrEmptyList(volumesFromDB))
			return null;

		Map<String,String> volumeTypeMap = new HashMap<String,String>();
		List<Volume> volumesWithInstance = new ArrayList<Volume>();
		for (Volume volume : volumesFromDB) {
			String attachedInstanceId = volume.getInstanceId();
			if (null != attachedInstanceId && !attachedInstanceId.isEmpty()) {
				Instance instance = instanceMapper.selectByPrimaryKey(attachedInstanceId);
				if(null != instance)
					volume.addInstance(instance);
			}
			volume.setStatus(volume.getStatus().toUpperCase());
			String typeName = volumeTypeMap.get(volume.getVolume_type());
			if(null == typeName){
				VolumeType volumeType = volumeTypeMapper.selectByName(volume.getVolume_type());
				if(null != volumeType){
					volumeTypeMap.put(volume.getVolume_type(), volumeType.getDisplayName());
					typeName = volumeType.getDisplayName();	
				}
			}
			//if(null == typeName && null != volume.getVolume_type())
			//	typeName = Message.getMessage(volume.getVolume_type().toUpperCase(),locale,false);
			volume.setVolumeTypeName(typeName);
			
//			if(volume.getVolume_type() != null)
//				volume.setVolumeTypeName(Message.getMessage(volume.getVolume_type().toUpperCase(),locale,false));
			//setBackupInfo(volume);
			volumesWithInstance.add(volume);
		}
		return volumesWithInstance;
	}

	private List<VolumeSnapshot> getSnapshotsFromDB(String tenantId, int limitItems, String status,Locale locale) {
		List<VolumeSnapshot> snapshotsFromDB = null;
		if (-1 == limitItems) {
			if (Util.isNullOrEmptyValue(status))
				snapshotsFromDB = volumeSnapshotMapper.selectListByTenantId(tenantId);
			else
				snapshotsFromDB = volumeSnapshotMapper.selectListByTenantIdAndStatus(tenantId, status);
		} else {
			if (Util.isNullOrEmptyValue(status))
				snapshotsFromDB = volumeSnapshotMapper.selectListByTenantIdWithLimit(tenantId, limitItems);
			else {
				snapshotsFromDB = volumeSnapshotMapper.selectListByTenantIdAndStatusWithLimit(tenantId, status,limitItems);
				if (limitItems <= snapshotsFromDB.size())
					snapshotsFromDB = snapshotsFromDB.subList(0, limitItems);
			}
		}
		return snapshotsFromDB;
	}
	
	private void setInstanceInfo(Volume volume,Locale locale) {
		if (null == volume)
			return;
		String instanceId = volume.getInstanceId();
		if (Util.isNullOrEmptyValue(instanceId))
			return;
		Instance instance = instanceMapper.selectByPrimaryKey(instanceId);
		volume.addInstance(instance);
		volume.setStatus(volume.getStatus().toUpperCase());
		String volumeTypeName = volume.getVolume_type();
		if(!Util.isNullOrEmptyValue(volumeTypeName)){
			VolumeType volumeType = volumeTypeMapper.selectByName(volumeTypeName);
			if(null != volumeType){
				volume.setVolumeTypeName(volumeType.getDisplayName());
			}
		}
		//volume.setVolumeTypeName(Message.getMessage(volume.getVolume_type().toUpperCase(),locale,false));
	}

	private void setBackupInfo(Volume volume) {
		if (null == volume)
			return;
		String backupId = volume.getBackupId();
		if (Util.isNullOrEmptyValue(backupId))
			return;
		String[] backupsId = backupId.split(",");
		for (int index = 0; index < backupsId.length; ++index) {
			Backup backup = backupMapper.selectByPrimaryKey(backupsId[index]);
			if (null == backup)
				continue; // TODO
			volume.addBackup(backup);
		}
	}

	private List<Volume> getLimitItems(List<Volume> volumes, int limit, String status, String tenantId) {
		if (Util.isNullOrEmptyList(volumes))
			return null;
		volumes = filterVolumeByStatus(volumes, status);
		if (Util.isNullOrEmptyList(volumes))
			return null;

		List<Volume> tenantVolumes = new ArrayList<Volume>();
		for (Volume volume : volumes) {
			if (!tenantId.equals(volume.getTenantId()))
				continue;
			tenantVolumes.add(volume);
		}
		if (-1 != limit) {
			if (limit <= tenantVolumes.size())
				return tenantVolumes.subList(0, limit);
		}
		return tenantVolumes;
	}

	private List<VolumeSnapshot> getSnapshotLimitItems(List<VolumeSnapshot> snapshots, int limit, String status, String tenantId) {
		if (Util.isNullOrEmptyList(snapshots))
			return null;
		

		List<VolumeSnapshot> tenantSnapshots = new ArrayList<VolumeSnapshot>();
		for (VolumeSnapshot snapshot : snapshots) {
			if (!tenantId.equals(snapshot.getTenantId()))
				continue;
			tenantSnapshots.add(snapshot);
		}
		if (-1 != limit) {
			if (limit <= tenantSnapshots.size())
				return tenantSnapshots.subList(0, limit);
		}
		return tenantSnapshots;
	}
	
	private void updateVolumeQuota(TokenOs ostoken, int diskSize, String type, boolean bAdd) {
		// String volumeTypeName = "CS_VOLUME_TYPE_NAME";
		// volumeTypeName = volumeTypeName.replaceFirst("TYPE",
		// type.toUpperCase());
		// quotaService.updateQuota(Message.getMessage(volumeTypeName,
		// false),ostoken,bAdd,diskSize);
		VolumeType volumeTypeFromDB = volumeTypeMapper.selectByPrimaryKey(type);
		if(null == volumeTypeFromDB)
			return;
		
		quotaService.updateQuota(volumeTypeFromDB.getName(), ostoken, bAdd, diskSize);

	//	resourceSpecService.updateResourceSpecQuota(ParamConstant.DISK, type, diskSize, bAdd);
		resourceSpecService.updateResourceSpecQuota(volumeTypeFromDB.getName(), type, diskSize, bAdd);
		
		Map<String,Integer> resourceQuota = new HashMap<String,Integer>();
		resourceQuota.put(volumeTypeFromDB.getName(), diskSize);
		poolService.updatePoolQuota(ostoken.getTenantid(), resourceQuota, bAdd);
	}
	
	private void updateSyncResourceInfo(String tenantId,String id,String orgStatus,String expectedStatus,String region,String name,String type){
		SyncResource resource = new SyncResource();
		resource.setId(id);
		resource.setType(type);
		resource.setOrgStatus(orgStatus);
		resource.setExpectedStatus(expectedStatus);
		resource.setRegion(region);
		syncResourceMapper.insertSelective(resource);
		
		ResourceCreateProcess createProcess = new ResourceCreateProcess();
		createProcess.setId(id);
		createProcess.setName(name);
		createProcess.setTenantId(tenantId);
		createProcess.setType(type);
		createProcess.setBegineSeconds(Util.time2Millionsecond(Util.getCurrentDate(),ParamConstant.TIME_FORMAT_01));
		resourceCreateProcessMapper.insertOrUpdate(createProcess);
	}
	
	private void storeResourceEventInfo(String tenantId,String id,String type,String beginState,String endState,long time){
		ResourceEvent event = new ResourceEvent();
		event.setTenantId(tenantId);
		event.setResourceId(id);
		event.setResourceType(type);
		event.setBeginState(beginState);
		event.setEndState(endState);
		event.setMillionSeconds(time);
		resourceEventMapper.insertSelective(event);
	}
	
	private void checkResource(String id, Boolean checkRelated,Locale locale) throws BusinessException {
		SyncResource syncResource = syncResourceMapper.selectByPrimaryKey(id);
		if (null != syncResource)
			throw new ResourceBusinessException(Message.CS_RESOURCE_IS_DOING,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE, locale);
		
		if(true == checkRelated){
			Instance instance = instanceMapper.selectInstanceByVolumeId(id);
			if(null != instance)
				throw new ResourceBusinessException(Message.CS_HAVE_RELATED_VOLUME_RESOURCE,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);	
		}
		
		return;
	}
	
	private void checkSystemVolume(String id, Locale locale) throws BusinessException {
		Volume volume = volumeMapper.selectByPrimaryKey(id);
		if(null == volume)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		if(volume.getBootable() == true)
			throw new ResourceBusinessException(Message.CS_OPERATE_SYSTEM_VOLUME_FORBIDDEN,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
		return;
	}
	
	private void checkVolumeSnapshot(String id, Locale locale) throws BusinessException {
		if(!Util.isNullOrEmptyList(volumeSnapshotMapper.selectByVolumeId(id)))
			throw new ResourceBusinessException(Message.CS_HAVE_RELATED_PRIVE_IMAGE_RESOURCE,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
		return;
	}
	
	private void checkVolumeName(String name,TokenOs ostoken)  throws BusinessException{
		if(Util.isNullOrEmptyValue(name))
			return;
		List<Volume> volumes = volumeMapper.selectListByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(volumes))
			return;
		for(Volume volume : volumes){
			if(name.equals(volume.getName()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_NAME_IS_SAME,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		return;
	}
	
	private void checkSnapshotName(String name,TokenOs ostoken)  throws BusinessException{
		if(Util.isNullOrEmptyValue(name))
			return;
		List<VolumeSnapshot> snapshots = volumeSnapshotMapper.selectListByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(snapshots))
			return;
		for(VolumeSnapshot snapshot : snapshots){
			if(name.equals(snapshot.getName()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_NAME_IS_SAME,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		return;
	}
}
