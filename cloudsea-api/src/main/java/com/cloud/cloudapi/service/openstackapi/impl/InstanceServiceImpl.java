package com.cloud.cloudapi.service.openstackapi.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

import com.cloud.cloudapi.dao.common.FloatingIPMapper;
import com.cloud.cloudapi.dao.common.GatewayMapper;
import com.cloud.cloudapi.dao.common.HostAggregateMapper;
import com.cloud.cloudapi.dao.common.ImageMapper;
import com.cloud.cloudapi.dao.common.InstanceMapper;
import com.cloud.cloudapi.dao.common.KeypairMapper;
import com.cloud.cloudapi.dao.common.NetworkMapper;
import com.cloud.cloudapi.dao.common.PortMapper;
import com.cloud.cloudapi.dao.common.ResourceCreateProcessMapper;
import com.cloud.cloudapi.dao.common.ResourceEventMapper;
import com.cloud.cloudapi.dao.common.RouterMapper;
import com.cloud.cloudapi.dao.common.SecurityGroupMapper;
import com.cloud.cloudapi.dao.common.SubnetMapper;
import com.cloud.cloudapi.dao.common.SyncResourceMapper;
import com.cloud.cloudapi.dao.common.VolumeMapper;
import com.cloud.cloudapi.dao.common.VolumeSnapshotMapper;
import com.cloud.cloudapi.dao.common.VolumeTypeMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.InstanceJSON;
import com.cloud.cloudapi.json.forgui.InterfaceAttachmentJSON;
import com.cloud.cloudapi.json.forgui.VolumeAttachmentJSON;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Console;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Flavor;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FloatingIP;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Gateway;
import com.cloud.cloudapi.pojo.openstackapi.forgui.HostAggregate;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Image;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.InstanceConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.InterfaceAttachment;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Keypair;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Network;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Port;
import com.cloud.cloudapi.pojo.openstackapi.forgui.QuotaDetail;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceCreateProcess;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceEvent;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Router;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SecurityGroup;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Subnet;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SyncResource;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Volume;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeAttachment;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeSnapshot;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeType;
import com.cloud.cloudapi.service.businessapi.zabbix.ZabbixService;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.ConfigService;
import com.cloud.cloudapi.service.openstackapi.CloudServiceService;
import com.cloud.cloudapi.service.openstackapi.FlavorService;
import com.cloud.cloudapi.service.openstackapi.ImageService;
import com.cloud.cloudapi.service.openstackapi.InstanceService;
import com.cloud.cloudapi.service.openstackapi.KeypairService;
import com.cloud.cloudapi.service.openstackapi.NetworkService;
import com.cloud.cloudapi.service.openstackapi.PortService;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.service.openstackapi.ResourceSpecService;
import com.cloud.cloudapi.service.openstackapi.SecurityGroupService;
import com.cloud.cloudapi.service.openstackapi.SubnetService;
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
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import sun.misc.BASE64Encoder;

@Service("instanceService")
public class InstanceServiceImpl implements InstanceService {

	@Resource
	private OSHttpClientUtil client;

	@Autowired
	private InstanceMapper instanceMapper;

	@Autowired
	private VolumeMapper volumeMapper;

	@Autowired
	private VolumeTypeMapper volumeTypeMapper;

	@Autowired
	private KeypairMapper keypairMapper;

	@Autowired
	private NetworkMapper networkMapper;

	@Autowired
	private SubnetMapper subnetMapper;

	@Autowired
	private ImageMapper imageMapper;

	@Autowired
	private PortMapper portMapper;

	@Autowired
	private RouterMapper routerMapper;

	@Autowired
	private GatewayMapper gatewayMapper;
	
	@Autowired
	private SyncResourceMapper syncResourceMapper;

	@Autowired
	private SecurityGroupMapper securityGroupMapper;

	@Autowired
	private VolumeSnapshotMapper volumeSnapshotMapper;
	
	@Autowired
	private FloatingIPMapper floatingIpMapper;

	@Autowired
	private HostAggregateMapper hostAggregateMapper;
	
	@Autowired
	private ResourceCreateProcessMapper resourceCreateProcessMapper;

	@Autowired
	private ResourceEventMapper resourceEventMapper;

	@Autowired
	private CloudConfig cloudconfig;

	@Resource
	private ZabbixService zabbixService;

	@Resource
	private NetworkService networkService;

	@Resource
	private QuotaService quotaService;

	@Resource
	private ImageService imageService;

	@Resource
	private SubnetService subnetService;

	@Resource
	private VolumeService volumeService;

	@Resource
	private VolumeTypeService volumeTypeService;

	@Resource
	private KeypairService keypairService;

	@Resource
	private PortService portService;

	@Resource
	private FlavorService flavorService;

	@Resource
	private ResourceSpecService resourceSpecService;

	@Resource
	private SecurityGroupService securityGroupService;

	@Resource
	private RatingTemplateService ratingTemplateService;
	
	@Resource
	private PoolResource poolService;

	@Resource
	private CloudServiceService serviceService;
	
	@Resource
	private AuthService authService;
	
	@Resource
	private ConfigService configService;
	
	private Logger log = LogManager.getLogger(InstanceServiceImpl.class);

	public InstanceMapper getInstanceMapper() {
		return instanceMapper;
	}

	public void setInstanceMapper(InstanceMapper instanceMapper) {
		this.instanceMapper = instanceMapper;
	}

	public VolumeMapper getVolumeMapper() {
		return volumeMapper;
	}

	public void setVolumeMapper(VolumeMapper volumeMapper) {
		this.volumeMapper = volumeMapper;
	}

	public KeypairMapper getKeypairMapper() {
		return keypairMapper;
	}

	public void setKeypairMapper(KeypairMapper keypairMapper) {
		this.keypairMapper = keypairMapper;
	}

	public NetworkMapper getNetworkMapper() {
		return networkMapper;
	}

	public void setNetworkMapper(NetworkMapper networkMapper) {
		this.networkMapper = networkMapper;
	}

	public SubnetMapper getSubnetMapper() {
		return subnetMapper;
	}

	public void setSubnetMapper(SubnetMapper subnetMapper) {
		this.subnetMapper = subnetMapper;
	}

	public ImageMapper getImageMapper() {
		return imageMapper;
	}

	public void setImageMapper(ImageMapper imageMapper) {
		this.imageMapper = imageMapper;
	}

	public PortMapper getPortMapper() {
		return portMapper;
	}

	public void setPortMapper(PortMapper portMapper) {
		this.portMapper = portMapper;
	}

	public FloatingIPMapper getFloatingIpMapper() {
		return floatingIpMapper;
	}

	public void setFloatingIpMapper(FloatingIPMapper floatingIpMapper) {
		this.floatingIpMapper = floatingIpMapper;
	}

	public CloudConfig getCloudconfig() {
		return cloudconfig;
	}

	public void setCloudconfig(CloudConfig cloudconfig) {
		this.cloudconfig = cloudconfig;
	}


	@Override
	public List<Instance> getInstanceList(Map<String, String> paramMap, String type, TokenOs ostoken) throws BusinessException {
		int limitItems = Util.getLimit(paramMap);
		List<Instance> instancesFromDB = null;
		if (-1 == limitItems) {
			instancesFromDB = instanceMapper.selectListByTenantIdWithType(ostoken.getTenantid(), type);
		} else {
			instancesFromDB = instanceMapper.selectListByTenantIdWithTypeAndLimit(ostoken.getTenantid(), type,
					limitItems);
		}
		if (!Util.isNullOrEmptyList(instancesFromDB)) {
			for(Instance instance : instancesFromDB)
				normalInstanceZoneName(instance,ostoken);
			return instancesFromDB;
		}
		return instancesFromDB;
		
		// todo 1: 閫氳繃guitokenid 鍙栧緱瀹為檯锛岀敤鎴蜂俊鎭�
		// AuthService as = new AuthServiceImpl();
//		String region = ostoken.getCurrentRegion();
//
//		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
//		url = RequestUrlHelper.createFullUrl(url + "/servers/detail", paramMap);
//
//		String errorMessage = Message.CS_COMPUTE_INSTANCE_GET_FAILED;
//		if (type.equals(ParamConstant.VDI_TYPE))
//			errorMessage = Message.CS_COMPUTE_VDI_INSTANCE_GET_FAILED;
//		HashMap<String, String> headers = new HashMap<String, String>();
//		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
//		Map<String, String> rs = client.httpDoGet(url, headers);
//		Util.checkResponseBody(rs, null);
//		
//		String failedMessage = Util.getFailedReason(rs);
//		if (!Util.isNullOrEmptyValue(failedMessage))
//			log.error(failedMessage);
//		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
//		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//		
//		List<Instance> instances = new ArrayList<Instance>();
//
//		switch (httpCode) {
//		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
//			try {
//				instances = getInstances(rs, type, ostoken, false);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
//			}
//			break;
//		}
//		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
//			String tokenid = "";// TODO reget the token id
//			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
//			rs = client.httpDoGet(url, headers);
//			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//
//			failedMessage = Util.getFailedReason(rs);
//			if (!Util.isNullOrEmptyValue(failedMessage))
//				log.error(failedMessage);
//			try {
//				instances = getInstances(rs, type, ostoken, false);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
//			}
//			break;
//		}
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE);
//		default:
//			throw new ResourceBusinessException(errorMessage);
//		}
//
//		storeInstances2DB(instances);
//		return instances;
	}

	@Override
	public List<Instance> getVDIInstanceList(Map<String, String> paramMap, String type, TokenOs ostoken) throws BusinessException {

		int limitItems = Util.getLimit(paramMap);
		List<Instance> instancesFromDB = null;
		if (-1 == limitItems) {
			instancesFromDB = instanceMapper.selectListByTenantIdWithType(ostoken.getTenantid(), type);
		} else {
			instancesFromDB = instanceMapper.selectListByTenantIdWithTypeAndLimit(ostoken.getTenantid(), type,
					limitItems);
		}
		if (!Util.isNullOrEmptyList(instancesFromDB)) {
			for(Instance instance : instancesFromDB)
				normalInstanceZoneName(instance,ostoken);
			return instancesFromDB;
		//	return normalInstanceInfo(instancesFromDB,ostoken);
		}
		return null;
	}

	@Override
	public Instance getInstance(String instanceId, String type, TokenOs ostoken, Boolean details) throws BusinessException {
		
		checkVMWareEnv(instanceId,new Locale(ostoken.getLocale()));
		
		Instance instance =  instanceMapper.selectByPrimaryKey(instanceId);
		boolean bNotGet = buildInstanceInfoFromDB(instance, instanceId);
		if (true == bNotGet) {
			instance = showInstance(instanceId, ostoken, type, details, instance);
		}
		storeInfo2DB(instance);
		instance.setType(type);
		normalInstanceZoneName(instance,ostoken);
	//	instance.setAvailabilityZoneName(Message.getMessage(instance.getAvailabilityZone().toUpperCase(), new Locale(ostoken.getLocale()),false));
	//	instance.setCreatedAt(Util.millionSecond2Date(instance.getMillionSeconds()));
		instanceMapper.insertOrUpdate(instance);
	//	normaleVolumeTypeInfo(instance.getVolumes(),new Locale(ostoken.getLocale()));
	
		// updateByPrimaryKeySelective(instance);
		return instance;
	}

	@Override
	public String deleteInstances(String deleteBody, TokenOs ostoken)
			throws BusinessException, JsonProcessingException, IOException {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(deleteBody);
		JsonNode idsNode = rootNode.path(ResponseConstant.IDS);
		int idsCount = idsNode.size();
		
		Locale locale = new Locale(ostoken.getLocale());
		for (int index = 0; index < idsCount; ++index) {
			String instanceId = idsNode.get(index).textValue();
			checkVMWareEnv(instanceId,locale);
			checkResource(instanceId,locale);
			checkRelatedResource(instanceId,locale);
		}
		
//		boolean deletedFailed = false;
		List<String> deletedInstanceId = new ArrayList<String>();
		for (int index = 0; index < idsCount; ++index) {
			String instanceId = idsNode.get(index).textValue();
//			checkResource(instanceId,new Locale(ostoken.getLocale()));
//			checkRelatedResource(instanceId,new Locale(ostoken.getLocale()));
			deleteInstance(instanceId, ostoken);
			deletedInstanceId.add(instanceId);
		}

		updateInstanceDBInfo(deletedInstanceId, ostoken);
//		if (true == deletedFailed) {
//			throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_DELETE_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
//		}
		return Util.listToString(deletedInstanceId, ',');
	}

	private void deleteInstance(String id, TokenOs ostoken) throws BusinessException {

		Locale locale = new Locale(ostoken.getLocale());
		//checkRelatedResource(id,locale);
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/servers/");
		sb.append(id);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
       
		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE: {
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
			//	throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoDelete(url.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
//			if(httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
//				throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_DELETE_FAILED,httpCode,locale);
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
//			throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_DELETE_FAILED,httpCode,locale);
		}
		
		
		//delete monitor obj(instance)
		zabbixService.deleteMonitorObj(id);
	
		// updateInstanceDBInfo(id,ostoken);
		/*
		Instance instanceFromDB = instanceMapper.selectByPrimaryKey(id);	
		Volume systemVolume = volumeMapper.selectSystemVolumeByInstanceId(id);
		StringBuilder relatedResource = new StringBuilder();
		relatedResource.append(ParamConstant.VOLUME);
		relatedResource.append(":");
		relatedResource.append(systemVolume.getId());
		updateSyncResourceInfo(ostoken.getTenantid(),id,relatedResource.toString(),instanceFromDB.getStatus(),ParamConstant.DELETED_STATUS,ParamConstant.INSTANCE,ostoken.getCurrentRegion(),instanceFromDB.getName());
		*/
		return;
	}

	private void updateInstanceDBInfo(List<String> deletedInstanceId, TokenOs ostoken) {
		if (Util.isNullOrEmptyList(deletedInstanceId))
			return;
		List<Instance> instances = instanceMapper.selectListByInstanceIds(deletedInstanceId);
		if (Util.isNullOrEmptyList(instances))
			return;
		
		Map<String, Integer> releaseTenantResource = new HashMap<String, Integer>();
		Map<String, Integer> releaseTotalResource = new HashMap<String, Integer>();
		Map<String,String> volumesType = new  HashMap<String,String>();
		List<String> resourcesType = new ArrayList<String>();
		
		for (Instance instance : instances) {
			updateRelatedKeypairInfo(instance);
			storeResourceEventInfo(ostoken.getTenantid(),instance.getId(),ParamConstant.INSTANCE,instance.getStatus(),ParamConstant.DELETED_STATUS,Util.getCurrentMillionsecond());
//			String volumeIds = instance.getVolumeIds();
//			List<Volume> volumes = null;
//			if (Util.isNullOrEmptyValue(volumeIds))
//				volumes = volumeMapper.selectListByInstanceId(instance.getId());
//			else
//				volumes = volumeMapper.selectVolumesById(volumeIds.split(","));
//			if (!Util.isNullOrEmptyList(volumes)) {
//				for (Volume volume : volumes) {
//					volume.setInstanceId(null);
//					try {
//						volumeMapper.updateByPrimaryKeySelective(volume);
//					} catch (Exception e) {
//						// TODO
//					}
//				}
//			}
			
			if (releaseTenantResource.containsKey(instance.getAvailabilityZone() + "_" + ParamConstant.CORE)) {
				releaseTenantResource.put(instance.getAvailabilityZone() + "_" + ParamConstant.CORE,
						releaseTenantResource.get(instance.getAvailabilityZone() + "_" + ParamConstant.CORE) + Integer.parseInt(instance.getCore()));
				
				releaseTotalResource.put(instance.getAvailabilityZone() + "_" + ParamConstant.CORE,
						releaseTotalResource.get(instance.getAvailabilityZone() + "_" + ParamConstant.CORE) + Integer.parseInt(instance.getCore()));

				
			}else{
				releaseTenantResource.put(instance.getAvailabilityZone() + "_" + ParamConstant.CORE,Integer.parseInt(instance.getCore()));
				releaseTotalResource.put(instance.getAvailabilityZone() + "_" + ParamConstant.CORE,Integer.parseInt(instance.getCore()));
				resourcesType.add(instance.getAvailabilityZone() + "_" + ParamConstant.CORE);
			}
			
			if (releaseTenantResource.containsKey(instance.getAvailabilityZone() + "_" + ParamConstant.RAM)) {
				releaseTenantResource.put(instance.getAvailabilityZone() + "_" + ParamConstant.RAM,
						releaseTenantResource.get(instance.getAvailabilityZone() + "_" + ParamConstant.RAM) + Integer.parseInt(instance.getRam())/ParamConstant.MB);
				
				releaseTotalResource.put(instance.getAvailabilityZone() + "_" + ParamConstant.RAM,
						releaseTotalResource.get(instance.getAvailabilityZone() + "_" + ParamConstant.RAM) + Integer.parseInt(instance.getRam()));

				
			}else{
				releaseTenantResource.put(instance.getAvailabilityZone() + "_" + ParamConstant.RAM,Integer.parseInt(instance.getRam())/ParamConstant.MB);
				releaseTotalResource.put(instance.getAvailabilityZone() + "_" + ParamConstant.RAM,Integer.parseInt(instance.getRam()));
				resourcesType.add(instance.getAvailabilityZone() + "_" + ParamConstant.RAM);
			}
			
			if(volumesType.containsKey(instance.getVolumeType())){
				volumesType.get(instance.getVolumeType());
				releaseTenantResource.put(volumesType.get(instance.getVolumeType()),
						releaseTenantResource.get(volumesType.get(instance.getVolumeType())) + Integer.parseInt(instance.getVolumeSize()));
				
				releaseTotalResource.put(volumesType.get(instance.getVolumeType()),
						releaseTotalResource.get(volumesType.get(instance.getVolumeType())) + Integer.parseInt(instance.getVolumeSize()));


			}else{
				VolumeType volumeTypeFromDB = volumeTypeMapper.selectByPrimaryKey(instance.getVolumeType());
				if (null != volumeTypeFromDB) {
					volumesType.put(instance.getVolumeType(),volumeTypeFromDB.getName());
					releaseTenantResource.put(volumeTypeFromDB.getName(), Integer.parseInt(instance.getVolumeSize()));
					releaseTotalResource.put(volumeTypeFromDB.getName(), Integer.parseInt(instance.getVolumeSize()));
					resourcesType.add(volumeTypeFromDB.getName());
				}
			}

		}
		// quotaService.updateQuota(ParamConstant.INSTANCE,ostoken,false,instances.size());

		quotaService.updateTenantResourcesQuota(resourcesType, releaseTenantResource, ostoken, false);
		resourceSpecService.updateTotalResourcesQuota(releaseTotalResource, resourcesType, false);
		poolService.updatePoolQuota(ostoken.getTenantid(), releaseTenantResource, false);
		instanceMapper.deleteByInstanceIds(deletedInstanceId);
		for (Instance instance : instances){
			Volume systemVolume = volumeMapper.selectSystemVolumeByInstanceId(instance.getId());
			StringBuilder relatedResource = new StringBuilder();
			if(null != systemVolume){
				relatedResource.append(ParamConstant.VOLUME);
				relatedResource.append(":");
				relatedResource.append(systemVolume.getId());	
			}
			updateSyncResourceInfo(ostoken.getTenantid(),instance.getId(),relatedResource.toString(),instance.getStatus(),ParamConstant.DELETED_STATUS,ParamConstant.INSTANCE,ostoken.getCurrentRegion(),instance.getName());
		}
	}

	@Override
	public List<Volume> getAttachedVolumes(String instanceId, TokenOs ostoken)
			throws BusinessException {
		Locale locale = new Locale(ostoken.getLocale());
		checkVMWareEnv(instanceId,locale);
		
		List<Volume> volumesFromDB = volumeMapper.selectListByInstanceId(instanceId);
		if (!Util.isNullOrEmptyList(volumesFromDB))
			return volumesFromDB;
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/servers/");
		sb.append(instanceId);
		sb.append("/os-volume_attachments");

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		List<Volume> volumes = new ArrayList<Volume>();
		Volume volume = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			ObjectMapper mapper = new ObjectMapper();
			try {
				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				JsonNode volumesNode = rootNode.path(ResponseConstant.VOLUMEATTACHMENTS);
				int size = volumesNode.size();
				for (int i = 0; i < size; ++i) {
					if (Util.isSystemVolume(volumesNode.get(i).path(ResponseConstant.DEVICE).textValue()))
						continue;
					volume = getVolumeInfo(volumesNode.get(i), ostoken);
					if (null == volume)
						continue;
					volumes.add(volume);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			ObjectMapper mapper = new ObjectMapper();
			try {
				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				JsonNode volumesNode = rootNode.path(ResponseConstant.VOLUMEATTACHMENTS);
				int size = volumesNode.size();
				for (int i = 0; i < size; ++i) {
					volume = getVolumeInfo(volumesNode.get(i), ostoken);
					volumes.add(volume);
				}
			}  catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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

		return storeVolumes2DB(volumes, instanceId);
	}

	@Override
	public List<Instance> createInstance(String createBody, String type, TokenOs ostoken)
			throws BusinessException, JsonParseException, JsonMappingException, IOException {

		Instance instanceCreateInfo = getCreateInstanceInfo(createBody);
		int instancesCount = 0;
		if (Util.isNullOrEmptyValue(instanceCreateInfo.getQuantity()))
			instancesCount = 1;
		else
			instancesCount = Integer.parseInt(instanceCreateInfo.getQuantity());
		
		checkSystemDiskSize(instanceCreateInfo,ostoken);
		
		List<Instance> createdInstances = new ArrayList<Instance>();
		Keypair keypairFromDB = keypairMapper.selectByPrimaryKey(instanceCreateInfo.getKeypairId());
		if (null != keypairFromDB) {
			instanceCreateInfo.addKeypair(keypairFromDB);
		}

		List<QuotaDetail> leastQuota = getCreatedInstancesQuota(instancesCount, instanceCreateInfo);
		// QuotaServiceImpl quotaService = new QuotaServiceImpl();
		// quotaService.setQuotaMapper(quotaMapper);
		// quotaService.setQuotaDetailMapper(quotaDetailMapper);
		// quotaService.getQuotas(null, ostoken, response);//need to delete it
		// 08/05
		quotaService.checkQuota(leastQuota, ostoken.getTenantid(), instanceCreateInfo.getAvailabilityZone(),new Locale(ostoken.getLocale()));

		String externalNetId = instanceCreateInfo.getExternalNetId();
		String orgInstanceName = instanceCreateInfo.getName();
		String orgNetworkIds = instanceCreateInfo.getNetworkIds();
		String orgKeypairIds = instanceCreateInfo.getKeypairIds();
		String orgFixedips = instanceCreateInfo.getFixedips();
		String orgFloatingips = instanceCreateInfo.getFloatingips();
		String orgVolumeIds = instanceCreateInfo.getVolumeIds();
		String orgImageIds = instanceCreateInfo.getImageIds();
		String orgSecurityGroupIds = instanceCreateInfo.getSecurityGroupIds();

		for (int i = 0; i < instancesCount; ++i) {
			if (instancesCount > 1) {
				instanceCreateInfo.setName(String.format(orgInstanceName + "_%s", i + 1));
				instanceCreateInfo.setNetworkIds(orgNetworkIds);
				instanceCreateInfo.setKeypairIds(orgKeypairIds);
				instanceCreateInfo.setFixedips(orgFixedips);
				instanceCreateInfo.setFloatingips(orgFloatingips);
				instanceCreateInfo.setVolumeIds(orgVolumeIds);
				instanceCreateInfo.setImageIds(orgImageIds);
				instanceCreateInfo.setSecurityGroupIds(orgSecurityGroupIds);

				instanceCreateInfo.setNetworks(new ArrayList<Network>());
				instanceCreateInfo.setIps(new ArrayList<String>());
				instanceCreateInfo.setFloatingIps(new ArrayList<String>());
				instanceCreateInfo.setVolumes(new ArrayList<Volume>());
				instanceCreateInfo.setKeypairs(new ArrayList<Keypair>());
				instanceCreateInfo.setImages(new ArrayList<Image>());
				instanceCreateInfo.setSecurityGroups(new ArrayList<SecurityGroup>());
				instanceCreateInfo.setAttachedFloatingIPs(new ArrayList<FloatingIP>());
			}
			Instance instanceDetail = createSingleInstance(instanceCreateInfo, type, ostoken);
			if (null == instanceDetail)
				continue;
			instanceDetail.setType(type);
		//	instanceDetail.setSourceName(instanceCreateInfo.getSourceName());
			instanceDetail.setSystemName(instanceDetail.getSourceName());
			instanceDetail.setSourceId(instanceCreateInfo.getSourceId());
			instanceDetail.setCore(instanceCreateInfo.getCore());
			//For 天津项目:  栁E��开发阶�?development,test,production)
			instanceDetail.setTag(instanceCreateInfo.getTag());
			// keypairs
			storeKeypair2DB(instanceCreateInfo, instanceDetail);
			// networks
			List<String> networksId = storeNetwork2DB(instanceCreateInfo);
			instanceDetail.setNetworkIds(Util.listToString(networksId, ','));
			instanceDetail.setSource(ParamConstant.OPENSTACK_ZONE);
			createdInstances.add(instanceDetail);
		}

		for(Instance instance : createdInstances){
			updateSyncResourceInfo(ostoken.getTenantid(),instance.getId(),null,ParamConstant.ACTIVE_STATUS,ParamConstant.INSTANCE,ostoken.getCurrentRegion(),instance.getName());	
			storeResourceEventInfo(ostoken.getTenantid(),instance.getId(),ParamConstant.INSTANCE,null,ParamConstant.ACTIVE_STATUS,instance.getMillionSeconds());
		}
		updateInstanceQuota(ostoken, true, leastQuota.get(0).getTotal(), leastQuota.get(1).getTotal(),
				leastQuota.get(2).getTotal(), instancesCount, instanceCreateInfo.getAvailabilityZone(),
				instanceCreateInfo.getVolumeType());
		updateFloatingIPQuota(ostoken,externalNetId,instancesCount);
		storeInstances2DB(createdInstances);
		return createdInstances;

	}

	@Override
	public Instance updateInstance(String instanceId, String updateBody, TokenOs ostoken)
			throws BusinessException, JsonParseException, JsonMappingException, IOException {
		
		Locale locale = new Locale(ostoken.getLocale());
		checkVMWareEnv(instanceId,locale);
		checkResource(instanceId,locale);
		Instance updatedInstanceInfo = getUpdatedInstanceInfo(updateBody);
		checkName(updatedInstanceInfo.getName(),ostoken);
		String instanceUpdateBody = "";
		if (null != updatedInstanceInfo) {
			instanceUpdateBody = getUpdatedInstanceBody(updatedInstanceInfo);
			if (Util.isNullOrEmptyValue(instanceUpdateBody))
				return null;
		}
		// token should have Regioninfo
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/servers/");
		sb.append(instanceId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		Map<String, String> rs = client.httpDoPut(sb.toString(), headers, instanceUpdateBody);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
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
			rs = client.httpDoPut(sb.toString(), headers, instanceUpdateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_UPDATE_FAILED,httpCode,locale);
		}

		Instance instance = instanceMapper.selectByPrimaryKey(instanceId);
		buildInstanceInfoFromDB(instance, instanceId);
		updateInstance2DB(updatedInstanceInfo, instance);
	
		return instance;
	}

	@Override
	public VolumeAttachment attachVolume(String instanceId, String attachmentBody, TokenOs ostoken)
			throws BusinessException, JsonParseException, JsonMappingException, IOException {
		Locale locale = new Locale(ostoken.getLocale());
		checkVMWareEnv(instanceId,locale);
		
		VolumeAttachment volumeAttachment = getVolumeAttachmentInfo(attachmentBody);
		String volumeId = volumeAttachment.getVolumeId();
		checkResource(volumeId,locale);
		checkSystemVolume(volumeId,locale);
		String volumeAttachmentBody = "";
		if (null != volumeAttachment) {
			volumeAttachmentBody = getVolumeAttachmentBody(volumeAttachment);
			if (Util.isNullOrEmptyValue(volumeAttachmentBody))
				return null;
		}

		// token should have Regioninfo
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/servers/");
		sb.append(instanceId);
		sb.append("/os-volume_attachments");

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		Map<String, String> rs = client.httpDoPost(sb.toString(), headers, volumeAttachmentBody);
		Util.checkResponseBody(rs,locale);
		VolumeAttachment attachedVolume = new VolumeAttachment();

		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				attachedVolume = getVolumeAttachment(rs);
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
			rs = client.httpDoPost(sb.toString(), headers, volumeAttachmentBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				attachedVolume = getVolumeAttachment(rs);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_VOLUME_ATTACH_FAILED,httpCode,locale);
		}

		updateAttachVolumeInfo(instanceId, volumeId, ostoken);
		return attachedVolume;
	}

	@Override
	public void detachVolume(String instanceId, String volumeId, TokenOs ostoken)
			throws BusinessException {
		Locale locale = new Locale(ostoken.getLocale());
		checkVMWareEnv(instanceId,locale);
		checkResource(volumeId,locale);
		checkSystemVolume(volumeId,locale);
		// token should have Regioninfo
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/servers/");
		sb.append(instanceId);
		sb.append("/os-volume_attachments/");
		sb.append(volumeId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
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
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoDelete(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_VOLUME_DETACH_FAILED,httpCode,locale);
		}
		updateDetachVolumeInfo(instanceId, volumeId, ostoken);
	}

	@Override
	public InterfaceAttachment attachPort(String instanceId, String type, String attachmentBody, TokenOs ostoken)
			throws BusinessException, JsonParseException, JsonMappingException, IOException {

		Locale locale = new Locale(ostoken.getLocale());
		checkVMWareEnv(instanceId,locale);
		InterfaceAttachment interfaceAttachment = getInterfaceAttachmentInfo(attachmentBody,locale);
		String portAttachmentBody = getInterfaceAttachmentBody(interfaceAttachment);
		if (Util.isNullOrEmptyValue(portAttachmentBody))
			return null;
		checkPortResource(interfaceAttachment.getPort_id(),instanceId,true,locale);
		// token should have Regioninfo
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/servers/");
		sb.append(instanceId);
		sb.append("/os-interface");

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoPost(sb.toString(), headers, portAttachmentBody);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				interfaceAttachment = getInterfaceAttachment(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			rs = client.httpDoPost(sb.toString(), headers, portAttachmentBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				interfaceAttachment = getInterfaceAttachment(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_PORT_ATTACH_FAILED,httpCode,locale);
		}

		updateAttachPortInfo(instanceId, type, interfaceAttachment, ostoken);
		return interfaceAttachment;
	}

//	private String getCorrectedId(String ids, String id) {
//		if(Util.isNullOrEmptyValue(id))
//			return ids;
//		String newIds = "";
//		if (!Util.isNullOrEmptyValue(ids)) {
//			if (!ids.contains(id)) {
//				newIds += ",";
//				newIds += id;
//			}
//		} else {
//			newIds = id;
//		}
//		return newIds;
//	}

	private void updateAttachPortInfo(String instanceId, String type, InterfaceAttachment interfaceAttachment,
			TokenOs ostoken) throws BusinessException {
		Instance instance = instanceMapper.selectByPrimaryKey(instanceId);
		if (null == instance) {
			instance = getInstance(instanceId, type, ostoken, false);
		}
	
		StringBuilder sb = new StringBuilder();
		
		String networkIds = Util.getAppendedIds(instance.getNetworkIds(), interfaceAttachment.getNet_id());
	    if(!Util.isSame(networkIds, instance.getNetworkIds())){
	    	sb.append(ParamConstant.NETWORK);
	    	sb.append(":");
	    	sb.append(interfaceAttachment.getNet_id());
	    	sb.append(";");
	    	sb.append(ParamConstant.INSTANCE);
	    	sb.append(":");
	    	sb.append(instanceId);
	    }
	    
		instance.setNetworkIds(networkIds);
		
		// instanceMapper.updateByPrimaryKeySelective(instance);

		Network network = networkMapper.selectByPrimaryKey(interfaceAttachment.getNet_id());
		if (null == network) {
			network = networkService.getNetwork(interfaceAttachment.getNet_id(), ostoken);
		}

		String instanceIds = Util.getAppendedIds(network.getInstance_id(), instanceId);//getCorrectedId(network.getInstance_id(), instanceId);
		String subnetIds = Util.getAppendedIds(network.getSubnetId(), interfaceAttachment.getSubnet_id());//getCorrectedId(network.getSubnetId(), interfaceAttachment.getSubnet_id());
		String portIds = Util.getAppendedIds(network.getPortId(), interfaceAttachment.getPort_id());//getCorrectedId(network.getPortId(), interfaceAttachment.getPort_id());

		network.setInstance_id(instanceIds);
		network.setSubnetId(subnetIds);
		network.setPortId(portIds);
		networkMapper.updateByPrimaryKeySelective(network);

		
		Port port = convertInterface2Port(interfaceAttachment, ostoken);
		port.setDevice_id(instanceId);
		portMapper.insertOrUpdate(port);
		
		String fixedIps = instance.getFixedips();
		port.getIp();
		instance.setFixedips(Util.getAppendedIds(fixedIps, port.getIp()));
//		if (null == portMapper.selectByPrimaryKey(port.getId()))
//			portMapper.insertSelective(port);

		portIds = Util.getAppendedIds(instance.getPortIds(), port.getId());//getCorrectedId(instance.getPortIds(), port.getId());
		instance.setPortIds(portIds);
		instanceMapper.updateByPrimaryKeySelective(instance);
		
		updateSyncResourceInfo(ostoken.getTenantid(),interfaceAttachment.getPort_id(),sb.toString(),port.getStatus(),ParamConstant.ACTIVE_STATUS,ParamConstant.PORT,ostoken.getCurrentRegion(),port.getName());	
	}

	@Override
	public Port detachPort(String instanceId, String portId, TokenOs ostoken)
			throws BusinessException, JsonProcessingException, IOException {
		Locale locale = new Locale(ostoken.getLocale());
		checkVMWareEnv(instanceId,locale);
		checkPortResource(portId,instanceId,false,locale);
		
//		String portId = getDetachedPortId(detachBody, ostoken);
//		if (Util.isNullOrEmptyValue(portId)) {
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
//		}
		// token should have Regioninfo
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/servers/");
		sb.append(instanceId);
		sb.append("/os-interface/");
		sb.append(portId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);

		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
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
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoDelete(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_PORT_DETACH_FAILED,httpCode,locale);
		}

		Port port = portMapper.selectByPrimaryKey(portId);
		updateInstancePortInfo(instanceId, port);
       
        port.setDevice_id(null);
        portMapper.insertOrUpdate(port);
        
        StringBuilder instanceInfo = new StringBuilder();
        instanceInfo.append(ParamConstant.INSTANCE);
        instanceInfo.append(":");
        instanceInfo.append(instanceId);
	
        updateSyncResourceInfo(ostoken.getTenantid(),portId,instanceInfo.toString(),port.getStatus(),ParamConstant.DOWN_STATUS,ParamConstant.PORT,ostoken.getCurrentRegion(),port.getName());	
        return port;
		// portMapper.deleteByPrimaryKey(portId);
	//	return portId;
	}

	private void updateInstancePortInfo(String instanceId, Port port) {
		if(null == port)
			return;
		Instance instance = instanceMapper.selectByPrimaryKey(instanceId);
		if (null == instance)
			return;
		String portId = port.getId();
		String ip = port.getIp();
		List<String> portIds = Util.getCorrectedIdInfo(instance.getPortIds(), portId);
		List<String> fixedIps = Util.getCorrectedIdInfo(instance.getFixedips(), ip);
		List<String> floatingIps = Util.getCorrectedIdInfo(instance.getFloatingips(), ip);
		instance.setPortIds(Util.listToString(portIds, ','));
		instance.setFixedips(Util.listToString(fixedIps, ','));
		instance.setFloatingips(Util.listToString(floatingIps, ','));
		instanceMapper.updateByPrimaryKeySelective(instance);
	}

	@Override
	public Image createInstanceImage(String instanceId, String type, TokenOs ostoken, String body) throws BusinessException, JsonProcessingException, IOException {
		Locale locale = new Locale(ostoken.getLocale());
		checkVMWareEnv(instanceId,locale);
		
		quotaService.checkResourceQuota(ostoken.getTenantid(), ParamConstant.IMAGE, 1, locale);

		checkResource(instanceId,locale);
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/servers/");
		sb.append(instanceId);
		sb.append("/action");

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		Map<String, String> rs = client.httpDoPost(sb.toString(), headers, body);
		Util.checkResponseBody(rs,locale);

		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String location = rs.get(ResponseConstant.LOCATION);
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
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoPost(sb.toString(), headers, body);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (ParamConstant.NORMAL_ASYNC_RESPONSE_CODE != httpCode)
				throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_IMAGE_CREATE_FAILED,httpCode,locale);
			location = rs.get(ResponseConstant.LOCATION);
			break;
		}
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_IMAGE_CREATE_FAILED,httpCode,locale);
		}

		Image image = storeImage2DB(instanceId, Util.getImageIdFromLocation(location), type, ostoken, body);
		updatePrivateImageQuota(ostoken,ParamConstant.IMAGE,true);
		return image;
	}

	@Override
	public Instance liveMigrationInstance(String instanceId, String action,TokenOs ostoken,String body) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		checkVMWareEnv(instanceId,locale);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(body);
		} catch (Exception e) {
			log.error("error",e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		String hostName = rootNode.path(ResponseConstant.NAME).textValue();
		String migrationBody = getLiveMigrationBody(hostName,action);
		
		checkResource(instanceId,locale);
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/servers/");
		sb.append(instanceId);
		sb.append("/action");
		
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		Map<String, String> rs = client.httpDoPost(sb.toString(), headers, migrationBody);
		Util.checkResponseBody(rs,locale);

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
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoPost(sb.toString(), headers, migrationBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (ParamConstant.NORMAL_ASYNC_RESPONSE_CODE != httpCode)
				throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_LIVE_MIGRATION_FAILED,httpCode,locale);
			break;
		}
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_LIVE_MIGRATION_FAILED,httpCode,locale);
		}

		Instance instance = instanceMapper.selectByPrimaryKey(instanceId);
		StringBuilder relatedResource = new StringBuilder();
		relatedResource.append(ParamConstant.LIVE_MIGRATION_ACTION);
		relatedResource.append(":");
		relatedResource.append(instanceId);
		updateSyncResourceInfo(ostoken.getTenantid(),instanceId,relatedResource.toString(),instance.getStatus(),ParamConstant.ACTIVE_STATUS,ParamConstant.INSTANCE,ostoken.getCurrentRegion(),instance.getName());

		instance.setHostName(hostName);
		instanceMapper.updateByPrimaryKeySelective(instance);
		return instance;
	}
	
	private String getConsoleBody(String body, String action)
			throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		Console console = mapper.readValue(body, Console.class);
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"");
		sb.append(action);
		sb.append("\":{");
		sb.append("\"type\":");
		sb.append("\"");
		sb.append(console.getType());
		sb.append("\"");
		sb.append("}");
		sb.append("}");
		return sb.toString();
	}
	
	private String getLiveMigrationBody(String name, String action){
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"");
		sb.append(action);
		sb.append("\":{");
		sb.append("\"host\":\"");
		sb.append(name);
		sb.append("\",\"");
		sb.append(ParamConstant.BLOCK_MIGRATION);
		sb.append("\":\"auto\",\"force\":");
		sb.append(false);
		sb.append("}}");
		return sb.toString();
	}

	private Console getConsole(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode consoleNode = rootNode.path(ResponseConstant.CONSOLE);
		Console console = new Console();
		console.setType(consoleNode.path(ResponseConstant.TYPE).textValue());
		console.setUrl(consoleNode.path(ResponseConstant.URL).textValue());
		return console;
	}

	@Override
	public Console getInstanceConsole(String instanceId, String action, TokenOs ostoken, String body) throws BusinessException, JsonProcessingException, IOException {
		
		Locale locale = new Locale(ostoken.getLocale());
		checkVMWareEnv(instanceId,locale);
		checkResource(instanceId,locale);
		
		String consoleBody = getConsoleBody(body, action);
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/servers/");
		sb.append(instanceId);
		sb.append("/action");

		String messageId = "";
		if (ParamConstant.GET_VNCCONSOLE_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_GET_VNC_CONSOLE_FAILED;
		} else if (ParamConstant.GET_SPICECONSOLE_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_GET_SPICE_CONSOLE_FAILED;
		} else if (ParamConstant.GET_SERIALCONSOLE_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_GET_SERIAL_CONSOLE_FAILED;
		} else if (ParamConstant.GET_RDPCONSOLE_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_GET_RDP_CONSOLE_FAILED;
		}

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoPost(sb.toString(), headers, consoleBody);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		Console console = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				console = getConsole(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			rs = client.httpDoPost(sb.toString(), headers, consoleBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (ParamConstant.NORMAL_SYNC_RESPONSE_CODE != httpCode)
				throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_IMAGE_CREATE_FAILED,httpCode,locale);
			try {
				console = getConsole(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(messageId,httpCode,locale);
		}

		return console;
	}

	@Override
	public void createSnapshot(String instanceId,TokenOs ostoken) throws BusinessException{
		
		Locale locale = new Locale(ostoken.getLocale());
		checkVMWareEnv(instanceId,locale);
		//check instance sync resource
		SyncResource syncResource = syncResourceMapper.selectByPrimaryKey(instanceId);
		if(null != syncResource)
			throw new ResourceBusinessException(Message.CS_RESOURCE_IS_DOING,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);

        Instance instance = instanceMapper.selectByPrimaryKey(instanceId);
        if(null == instance)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
        
        String[] volumeIds = instance.getVolumeIds().split(",");
        for(int index = 0; index < volumeIds.length; ++index){
        	volumeService.createSnapshotForInstance(volumeIds[index], ostoken);
        }
	}
	
	@Override
	public void operateInstance(String instanceId, String body, String action, String type, TokenOs ostoken) throws BusinessException {
		
		Locale locale = new Locale(ostoken.getLocale());
		checkVMWareEnv(instanceId,locale);
		checkResource(instanceId,locale);
		checkBindedResource(action,body,locale);
		if(ParamConstant.ADD_FLOATINGIP_ACTION.equals(action)){
			checkInfoForAddFloatingIP(instanceId,body,locale);	
		}
		
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/servers/");
		sb.append(instanceId);
		sb.append("/action");

		String messageId = "";
		if (ParamConstant.PAUSE_INSTANCE_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_INSTANCE_PAUSE_FAILED;
		} else if (ParamConstant.UNPAUSE_INSTANCE_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_INSTANCE_RESTORE_FAILED;
		} else if (ParamConstant.START_INSTANCE_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_INSTANCE_START_FAILED;
		} else if (ParamConstant.STOP_INSTANCE_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_INSTANCE_STOP_FAILED;
		} else if (ParamConstant.SOFT_REBOOT_INSTANCE_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_INSTANCE_SOFT_REBOOT_FAILED;
		} else if (ParamConstant.HARD_REBOOT_INSTANCE_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_INSTANCE_HARD_REBOOT_FAILED;
		} else if (ParamConstant.SUSPEND_INSTANCE_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_INSTANCE_SUSPEND_FAILED;
		} else if (ParamConstant.RESUME_INSTANCE_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_INSTANCE_RESTORE_FAILED;
		} else if (ParamConstant.ADD_SECURITYGROUP_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_INSTANCE_ADD_SECURITY_FAILED;
		} else if (ParamConstant.ADD_FLOATINGIP_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_INSTANCE_ADD_FLOATINGIP_FAILED;
		} else if (ParamConstant.REMOVE_SECURITYGROUP_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_INSTANCE_REMOVE_SECURITY_FAILED;
		} else if (ParamConstant.REMOVE_FLOATINGIP_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_INSTANCE_REMOVE_FLOATINGIP_FAILED;
		} else if (ParamConstant.ADD_FIXEDIP_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_INSTANCE_ADD_FIXEDIP_FAILED;
		} else if (ParamConstant.REMOVE_FIXEDIP_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_INSTANCE_REMOVE_FIXEDIP_FAILED;
		} else if (ParamConstant.RESIZE_ACTION.equals(action)) {
			messageId = Message.CS_COMPUTE_INSTANCE_RESIZE_FAILED;
		}

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		Map<String, String> rs = client.httpDoPost(sb.toString(), headers, body);
		Util.checkResponseBody(rs,locale);

		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));

		String errorMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);

		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE: {
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoPost(sb.toString(), headers, body);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			errorMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if (ParamConstant.NORMAL_ASYNC_RESPONSE_CODE != httpCode)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			break;
		}
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(messageId,httpCode,locale);
		}

		updateInstanceInfoAfterAction(instanceId,action,body,ostoken);
	//	updateInstanceStatusInfo(instanceId, type, ostoken);
	}

	private void updateInstanceInfoAfterAction(String instanceId,String action,String body,TokenOs ostoken){
		Instance updatedInstance = instanceMapper.selectByPrimaryKey(instanceId);
		String endState = ParamConstant.ACTIVE_STATUS;
		if (ParamConstant.ADD_SECURITYGROUP_ACTION.equals(action)) {
			ObjectMapper mapper = new ObjectMapper();
			String securityGroupId = "";
			try{
				JsonNode rootNode = mapper.readTree(body);
				JsonNode floatingNode = rootNode.get(ParamConstant.ADD_SECURITYGROUP_ACTION);
				if(!floatingNode.isMissingNode()){
					String securityGroupName = floatingNode.path(ResponseConstant.NAME).textValue();
					SecurityGroup securityGroup = securityGroupService.getSecurityGroupByName(securityGroupName, ostoken);
					securityGroupId = securityGroup != null ? securityGroup.getId() : null;
				}
			}catch(Exception e){	
				log.error(e);
			}
		//	Instance updatedInstance = instanceMapper.selectByPrimaryKey(instanceId);
			updatedInstance.setSecurityGroupIds(Util.getAppendedIds(updatedInstance.getSecurityGroupIds(), Util.stringToList(securityGroupId, ",")));
			instanceMapper.updateByPrimaryKeySelective(updatedInstance);
			updateSyncResourceInfo(ostoken.getTenantid(),instanceId,updatedInstance.getStatus(),ParamConstant.ACTIVE_STATUS,ParamConstant.INSTANCE,ostoken.getCurrentRegion(),updatedInstance.getName());
		} else if (ParamConstant.ADD_FLOATINGIP_ACTION.equals(action)) {
			ObjectMapper mapper = new ObjectMapper();
			String ip = "";
			try{
				JsonNode rootNode = mapper.readTree(body);
				JsonNode floatingNode = rootNode.get(ParamConstant.ADD_FLOATINGIP_ACTION);
				if(!floatingNode.isMissingNode()){
					ip = floatingNode.path(ResponseConstant.ADDRESS).textValue();
					FloatingIP floatingIP = floatingIpMapper.selectByFloatingIp(ip);
					if(null != floatingIP){
						floatingIP.setAssigned(true);
						floatingIP.setInstanceId(instanceId);
						floatingIpMapper.insertOrUpdate(floatingIP);
						
						StringBuilder sb = new StringBuilder();
						sb.append(ParamConstant.INSTANCE);
						sb.append(":");
						sb.append(instanceId);
						updateSyncResourceInfo(ostoken.getTenantid(),floatingIP.getId(),sb.toString(),floatingIP.getStatus(),ParamConstant.ACTIVE_STATUS,ParamConstant.FLOATINGIP,ostoken.getCurrentRegion(),floatingIP.getName());
					}

				}
			}catch(Exception e){	
				log.error(e);
			}
		//	Instance updatedInstance = instanceMapper.selectByPrimaryKey(instanceId);
			updatedInstance.setFloatingips(Util.getAppendedIds(updatedInstance.getFloatingips(), Util.stringToList(ip, ",")));
			instanceMapper.updateByPrimaryKeySelective(updatedInstance);
			updateSyncResourceInfo(ostoken.getTenantid(),instanceId,updatedInstance.getStatus(),ParamConstant.ACTIVE_STATUS,ParamConstant.INSTANCE,ostoken.getCurrentRegion(),updatedInstance.getName());

		} else if (ParamConstant.ADD_FIXEDIP_ACTION.equals(action)) {
//			Instance updatedInstance = instanceMapper.selectByPrimaryKey(instanceId);
//			updatedInstance.setFixedips(null);
//			instanceMapper.updateByPrimaryKeySelective(updatedInstance);
		}
		if (ParamConstant.REMOVE_SECURITYGROUP_ACTION.equals(action)) {
			ObjectMapper mapper = new ObjectMapper();
			String securityGroupId = "";
			try{
				JsonNode rootNode = mapper.readTree(body);
				JsonNode floatingNode = rootNode.get(ParamConstant.REMOVE_SECURITYGROUP_ACTION);
				if(!floatingNode.isMissingNode()){
					String securityGroupName = floatingNode.path(ResponseConstant.NAME).textValue();
					SecurityGroup securityGroup = securityGroupService.getSecurityGroupByName(securityGroupName, ostoken);
					securityGroupId = securityGroup != null ? securityGroup.getId() : null;
				}
			}catch(Exception e){
				log.error(e);
			}
	//		Instance updatedInstance = instanceMapper.selectByPrimaryKey(instanceId);
			updatedInstance.setSecurityGroupIds(Util.listToString(Util.getCorrectedIdInfo(updatedInstance.getSecurityGroupIds(), securityGroupId), ','));
			instanceMapper.updateByPrimaryKeySelective(updatedInstance);
			updateSyncResourceInfo(ostoken.getTenantid(),instanceId,updatedInstance.getStatus(),ParamConstant.ACTIVE_STATUS,ParamConstant.INSTANCE,ostoken.getCurrentRegion(),updatedInstance.getName());

		} else if (ParamConstant.REMOVE_FLOATINGIP_ACTION.equals(action)) {
			ObjectMapper mapper = new ObjectMapper();
			String ip = "";
			try{
				JsonNode rootNode = mapper.readTree(body);
				JsonNode floatingNode = rootNode.get(ParamConstant.REMOVE_FLOATINGIP_ACTION);
				if(!floatingNode.isMissingNode()){
					ip = floatingNode.path(ResponseConstant.ADDRESS).textValue();
					FloatingIP floatingIP = floatingIpMapper.selectByFloatingIp(ip);
					floatingIP.setInstanceId(null);
					floatingIP.setAssigned(false);
					floatingIpMapper.insertOrUpdate(floatingIP);
					StringBuilder sb = new StringBuilder();
					sb.append(ParamConstant.INSTANCE);
					sb.append(":");
					sb.append(instanceId);
					updateSyncResourceInfo(ostoken.getTenantid(),floatingIP.getId(),sb.toString(),floatingIP.getStatus(),ParamConstant.DOWN_STATUS,ParamConstant.FLOATINGIP,ostoken.getCurrentRegion(),floatingIP.getName());
				}
			}catch(Exception e){	
				log.error(e);
			}
	//		Instance updatedInstance = instanceMapper.selectByPrimaryKey(instanceId);
			updatedInstance.setFloatingips(Util.listToString(Util.getCorrectedIdInfo(updatedInstance.getFloatingips(), ip), ','));
			instanceMapper.updateByPrimaryKeySelective(updatedInstance);
			updateSyncResourceInfo(ostoken.getTenantid(),instanceId,updatedInstance.getStatus(),ParamConstant.ACTIVE_STATUS,ParamConstant.INSTANCE,ostoken.getCurrentRegion(),updatedInstance.getName());
		} else if (ParamConstant.REMOVE_FIXEDIP_ACTION.equals(action)) {
//			Instance updatedInstance = instanceMapper.selectByPrimaryKey(instanceId);
//			updatedInstance.setFixedips(null);
//			instanceMapper.updateByPrimaryKeySelective(updatedInstance);
		} else if (ParamConstant.START_INSTANCE_ACTION.equals(action)) {
			updateSyncResourceInfo(ostoken.getTenantid(),instanceId,updatedInstance.getStatus(),ParamConstant.ACTIVE_STATUS,ParamConstant.INSTANCE,ostoken.getCurrentRegion(),updatedInstance.getName());
		} else if (ParamConstant.STOP_INSTANCE_ACTION.equals(action)) {
			endState = ParamConstant.STOPPED_STATUS;
			updateSyncResourceInfo(ostoken.getTenantid(),instanceId,updatedInstance.getStatus(),ParamConstant.STOPPED_STATUS,ParamConstant.INSTANCE,ostoken.getCurrentRegion(),updatedInstance.getName());
		} else if (ParamConstant.SOFT_REBOOT_INSTANCE_ACTION.equals(action)) {
			updateSyncResourceInfo(ostoken.getTenantid(),instanceId,updatedInstance.getStatus(),ParamConstant.ACTIVE_STATUS,ParamConstant.INSTANCE,ostoken.getCurrentRegion(),updatedInstance.getName());
		} else if (ParamConstant.HARD_REBOOT_INSTANCE_ACTION.equals(action)) {
			updateSyncResourceInfo(ostoken.getTenantid(),instanceId,updatedInstance.getStatus(),ParamConstant.ACTIVE_STATUS,ParamConstant.INSTANCE,ostoken.getCurrentRegion(),updatedInstance.getName());
		} else if (ParamConstant.SUSPEND_INSTANCE_ACTION.equals(action)){
			endState = ParamConstant.SUSPENDED_STATUS;
			updateSyncResourceInfo(ostoken.getTenantid(),instanceId,updatedInstance.getStatus(),ParamConstant.SUSPENDED_STATUS,ParamConstant.INSTANCE,ostoken.getCurrentRegion(),updatedInstance.getName());
		} else if (ParamConstant.RESUME_INSTANCE_ACTION.equals(action)){
			endState = ParamConstant.ACTIVE_STATUS;
			updateSyncResourceInfo(ostoken.getTenantid(),instanceId,updatedInstance.getStatus(),ParamConstant.ACTIVE_STATUS,ParamConstant.INSTANCE,ostoken.getCurrentRegion(),updatedInstance.getName());
		} else if (ParamConstant.PAUSE_INSTANCE_ACTION.equals(action)) {
			endState = ParamConstant.PAUSED_STATUS;
			updateSyncResourceInfo(ostoken.getTenantid(),instanceId,updatedInstance.getStatus(),ParamConstant.PAUSED_STATUS,ParamConstant.INSTANCE,ostoken.getCurrentRegion(),updatedInstance.getName());
		} else if (ParamConstant.UNPAUSE_INSTANCE_ACTION.equals(action)) {
			endState = ParamConstant.ACTIVE_STATUS;
			updateSyncResourceInfo(ostoken.getTenantid(),instanceId,updatedInstance.getStatus(),ParamConstant.ACTIVE_STATUS,ParamConstant.INSTANCE,ostoken.getCurrentRegion(),updatedInstance.getName());
		}else if (ParamConstant.RESIZE_ACTION.equals(action)) {		
			endState = ParamConstant.RESIZE_STATUS;
			updateSyncResourceInfo(ostoken.getTenantid(),instanceId,updatedInstance.getStatus(),ParamConstant.RESIZE_STATUS,ParamConstant.INSTANCE,ostoken.getCurrentRegion(),updatedInstance.getName());
		}
		
		storeResourceEventInfo(ostoken.getTenantid(),updatedInstance.getId(),ParamConstant.INSTANCE,updatedInstance.getStatus(),endState,Util.getCurrentMillionsecond());

	}
	
//	private void updateInstanceStatusInfo(String instanceId, String type, TokenOs ostoken)
//			throws BusinessException {
//		try {
//			Instance instance = showInstance(instanceId, ostoken, type, true, null);
//			if (null == instance)
//				return;
//			Instance instanceFromDB = instanceMapper.selectByPrimaryKey(instanceId);
//			if (null == instanceFromDB)
//				return;
//			instanceFromDB.setStatus(instance.getStatus());
//			instanceMapper.updateByPrimaryKeySelective(instanceFromDB);
//		} catch (Exception e) {
//			// TODO
//		}
//	}

	private Instance showInstance(String instanceId, TokenOs ostoken, String type, Boolean details,
			Instance instanceInfoFromDB) throws BusinessException {
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/servers/");
		sb.append(instanceId);

		String errorMessageId = Message.CS_COMPUTE_INSTANCE_GET_FAILED;
		if (type.equals(ParamConstant.VDI_TYPE))
			errorMessageId = Message.CS_COMPUTE_VDI_INSTANCE_DETAIL_GET_FAILED;

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);

		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		Instance instanceDetail = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			ObjectMapper mapper = new ObjectMapper();
			try {
				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				JsonNode serverNode = rootNode.path(ResponseConstant.INSTANCE);
				instanceDetail = getInstanceDetailInfo(serverNode, ostoken, details, instanceInfoFromDB);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			if (ParamConstant.NORMAL_SYNC_RESPONSE_CODE != httpCode)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			ObjectMapper mapper = new ObjectMapper();
			try {
				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				JsonNode serverNode = rootNode.path(ResponseConstant.INSTANCE);
				instanceDetail = getInstanceDetailInfo(serverNode, ostoken, details, instanceInfoFromDB);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(errorMessageId,httpCode,locale);
		}
		return instanceDetail;

	}

	private Instance createSingleInstance(Instance instanceCreateInfo, String type, TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException {
		// todo 1: 閫氳繃guitokenid 鍙栧緱瀹為檯锛岀敤鎴蜂俊鎭�E�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		checkName(instanceCreateInfo.getName(),ostoken);

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/servers", null);

		Map<String, Map<String, String>> instanceParamMap = buildInstanceParam(instanceCreateInfo, type, ostoken);

		List<String> metadataValues = null; // TODO
		if (null != instanceCreateInfo.getVolumeType() && !instanceCreateInfo.getVolumeType().isEmpty()) {
			metadataValues = new ArrayList<String>();
			metadataValues.add(instanceCreateInfo.getVolumeType());
			//TODO add metadata here is not a good idea
			if(ParamConstant.PASSWORD_CREDENTIAL.equals(instanceCreateInfo.getCredentialType())){
				Image image = imageMapper.selectByPrimaryKey(instanceCreateInfo.getSourceId());
				if(ParamConstant.WINDOWS.equalsIgnoreCase(image.getSystemType())){
					metadataValues.add(instanceCreateInfo.getPassword());
				}
			}
		}

		String errorMessageId = Message.CS_COMPUTE_INSTANCE_CREATE_FAILED;
		if (type.equals(ParamConstant.VDI_TYPE))
			errorMessageId = Message.CS_COMPUTE_VDI_INSTANCE_CREATE_FAILED;

		List<String> names = new ArrayList<String>();
		String createBoay = generateBody(instanceParamMap, names, metadataValues);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPost(url, headers, createBoay);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		Instance instanceDetail = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE: {
			try {
				instanceDetail = getInstance(rs, ostoken, type, true);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			rs = client.httpDoPost(url, headers, createBoay);
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (ParamConstant.NORMAL_SYNC_RESPONSE_CODE != httpCode)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				instanceDetail = getInstance(rs, ostoken, type, true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(errorMessageId,httpCode,locale);
		}

		instanceDetail.setMillionSeconds(Util.getCurrentMillionsecond());
		instanceDetail.setCore(instanceCreateInfo.getCore());
		instanceDetail.setRam(instanceCreateInfo.getRam());
		instanceDetail.setVolumeSize(instanceCreateInfo.getVolumeSize());
		instanceDetail.setVolumeType(instanceCreateInfo.getVolumeType());
		instanceDetail.setNetworkIds(instanceCreateInfo.getNetworkIds());
		
//		updateSyncResourceInfo(ostoken.getTenantid(),instanceDetail.getId(),null,ParamConstant.ACTIVE,ParamConstant.INSTANCE,ostoken.getCurrentRegion(),instanceDetail.getName());	
//		storeResourceEventInfo(ostoken.getTenantid(),instanceDetail.getId(),ParamConstant.INSTANCE,null,ParamConstant.ACTIVE_STATUS,instanceDetail.getMillionSeconds());
		return instanceDetail;
	}

	private List<Volume> storeVolumes2DB(List<Volume> volumes, String instanceId) {
		if (Util.isNullOrEmptyList(volumes))
			return null;
		List<Volume> volumesWithInstanceId = new ArrayList<Volume>();

		for (Volume volume : volumes) {
			if (Util.isSystemVolume(volume.getDevice()))
				continue;
			if (null == volumeMapper.selectByPrimaryKey(volume.getId()))
				volumeMapper.insertSelective(volume);
			else
				volumeMapper.updateByPrimaryKeySelective(volume);
			volume.setInstanceId(instanceId);
			volumesWithInstanceId.add(volume);
		}
		return volumesWithInstanceId;
	}

	private Volume getVolumeInfo(JsonNode volumeNode, TokenOs ostoken) throws BusinessException {
		if (null == volumeNode)
			return null;
		Volume volumeDetail = volumeService.getVolume(volumeNode.path(ResponseConstant.ID).textValue(), ostoken);
		if (null == volumeDetail)
			return null;

		volumeDetail.setInstanceId(volumeNode.path(ResponseConstant.SERVERID).textValue());
		volumeDetail.setDevice(volumeNode.path(ResponseConstant.DEVICE).textValue());
		return volumeDetail;
	}

//	private void setIpInfo(JsonNode serverNode, Instance instanceDetail) {
//		List<String> fixedipList = new ArrayList<String>();
//		List<String> floatingiplist = new ArrayList<String>();
//		JsonNode addressesNode = serverNode.path(ResponseConstant.ADDRESSES);
//		Iterator<Entry<String, JsonNode>> elements = addressesNode.fields();
//		while (elements.hasNext()) {
//			Entry<String, JsonNode> node = elements.next();
//			JsonNode networks = node.getValue();
//			int size = node.getValue().size();
//			for (int index = 0; index < size; ++index) {
//				JsonNode network = networks.get(index);
//				String ipAddress = network.path(ResponseConstant.ADDR).textValue();
//				// String version =
//				// network.path(ResponseConstant.VERSION).textValue();
//				String ipType = network.path(ResponseConstant.EXT_IP_TYPE).textValue();
//				// String mac =
//				// network.path(ResponseConstant.MAC_ADDR).textValue();
//				if (ParamConstant.FIXED.equals(ipType))
//					fixedipList.add(ipAddress);
//				else
//					floatingiplist.add(ipAddress);
//			}
//		}
//		instanceDetail.setIps(fixedipList);
//		instanceDetail.setFloatingIps(floatingiplist);
//		instanceDetail.setFixedips(Util.listToString(fixedipList, ','));
//		instanceDetail.setFloatingips(Util.listToString(floatingiplist, ','));
//	}

	private List<Volume> setVolumeInfo(JsonNode serverNode, Instance instanceDetail, TokenOs ostoken)
			throws BusinessException {
		JsonNode volumesNode = serverNode.path(ResponseConstant.OS_EXT_VOLUMES_VOLUME_ATTACHED);
		int volumesCount = volumesNode.size();
		List<Volume> volumes = new ArrayList<Volume>();
		for (int index = 0; index < volumesCount; ++index) {
			String volumeId = volumesNode.get(index).path(ResponseConstant.ID).textValue();
			Volume volume = volumeMapper.selectByPrimaryKey(volumeId);
			if (null == volume) {
				volume = volumeService.getVolume(volumeId, ostoken);
				if (volume.getBootable()) // skip system volume
					continue;
			}
			volumes.add(volume);
		}
		return volumes;
	}

	private Instance getInstanceDetailInfo(JsonNode serverNode, TokenOs ostoken, Boolean details,
			Instance instanceInfoFromDB) throws BusinessException {

		if (null == serverNode)
			return null;
		Instance instanceDetail = new Instance();
		instanceDetail.setId(serverNode.path(ResponseConstant.ID).textValue());
		instanceDetail.setName(serverNode.path(ResponseConstant.NAME).textValue());
		instanceDetail.setStatus(serverNode.path(ResponseConstant.STATUS).textValue().toUpperCase());
	//	instanceDetail.setMillionSeconds(Util.utc2Millionsecond(serverNode.path(ResponseConstant.CREATED).textValue()));
	//	instanceDetail.setCreatedAt(serverNode.path(ResponseConstant.CREATED).textValue());
		instanceDetail.setTenantId(serverNode.path(ResponseConstant.TENANT_ID).textValue());
		instanceDetail.setType(serverNode.path(ResponseConstant.OS_EXT_AZ_AVAILABILITY_ZONE).textValue());
		instanceDetail.setAvailabilityZone(serverNode.path(ResponseConstant.OS_EXT_AZ_AVAILABILITY_ZONE).textValue());
		// image
		if (null == instanceInfoFromDB) {
			JsonNode imageNode = serverNode.path(ResponseConstant.IMAGE);
			if (null != imageNode && !imageNode.path(ResponseConstant.ID).isMissingNode()) {
				instanceDetail.setSourceId(imageNode.path(ResponseConstant.ID).textValue());
				String imageName = ""; // TODO get the image name
				if (imageName.isEmpty()) {
					// ImageServiceImpl imgService = new ImageServiceImpl();
					// imgService.setCloudconfig(cloudconfig);
					// imgService.setImageMapper(imageMapper);
					Image image = imageService.getImage(instanceDetail.getSourceId(), ostoken);
					instanceDetail.setImage(image);
					instanceDetail.setSourceName(image.getName());
				}
			}
		} else {
			Image image = new Image(instanceInfoFromDB.getSourceId(), instanceInfoFromDB.getSourceName());
			instanceDetail.setImage(image);
			instanceDetail.setSourceId(instanceInfoFromDB.getSourceId());
			instanceDetail.setSourceName(instanceInfoFromDB.getSourceName());
		}

		// ip addresses
		if (null == instanceInfoFromDB || (Util.isNullOrEmptyValue(instanceInfoFromDB.getFixedips())
				&& Util.isNullOrEmptyValue(instanceInfoFromDB.getFloatingips()))) {
			//setIpInfo(serverNode, instanceDetail);
		} else {
			if (!Util.isNullOrEmptyValue(instanceInfoFromDB.getFixedips())) {
				instanceDetail.setIps(Util.stringToList(instanceInfoFromDB.getFixedips(), ","));
				instanceDetail.setFixedips(instanceInfoFromDB.getFixedips());
			}
			if (!Util.isNullOrEmptyValue(instanceInfoFromDB.getFloatingips())) {
				instanceDetail.setFloatingIps(Util.stringToList(instanceInfoFromDB.getFloatingips(), ","));
				instanceDetail.setFloatingips(instanceInfoFromDB.getFloatingips());
				List<FloatingIP> floatingIPs = floatingIpMapper.selectListByFloatingIps(Util.stringToList(instanceInfoFromDB.getFloatingips(), ","));
				instanceDetail.setAttachedFloatingIPs(floatingIPs);
			}
		}
		// if (false == details)
		// return instanceDetail;

		// flavor
		if (instanceInfoFromDB == null) {
			JsonNode flavorNode = serverNode.path(ResponseConstant.FLAVOR);
			if (null != flavorNode) {
				String flavorId = flavorNode.path(ResponseConstant.ID).textValue();
				Flavor flavor = flavorService.getFlavor(flavorId, ostoken);
				if (null != flavor) {
					instanceDetail.setCore(Integer.toString(flavor.getVcpus()));
					instanceDetail.setRam(Integer.toString(flavor.getRam()));
				}
			}
		} else {
			instanceDetail.setCore(instanceInfoFromDB.getCore());
			instanceDetail.setRam(instanceInfoFromDB.getRam());
		}

		// metadata
		JsonNode metadataNode = serverNode.path(ResponseConstant.METADATA);
		if (null != metadataNode) {
			instanceDetail.setVolumeType(metadataNode.path(ResponseConstant.VOLUME_TYPE).textValue());
		}

		// keypair
		String keypairName = serverNode.path(ResponseConstant.KEY_NAME).textValue();
		if (!Util.isNullOrEmptyValue(keypairName)) {
			Keypair keypair = keypairService.getKeypair(keypairName, ostoken);
			if (null != keypair)
				instanceDetail.addKeypair(keypair);
		}
		instanceDetail.makeKeypairIds();

		// private images
		List<Image> images = null;
		if (null != instanceInfoFromDB && !Util.isNullOrEmptyValue(instanceInfoFromDB.getImageIds())) {
			images = instanceInfoFromDB.getImages();
		} else {
			images = imageService.getInstanceImages(instanceDetail.getId());
		}
		if (null != images) {
			instanceDetail.setImages(images);
			instanceDetail.makePrivateImageIds();
		}

		// volumes
		List<Volume> volumes = null;
		if (null != instanceInfoFromDB && !Util.isNullOrEmptyValue(instanceInfoFromDB.getVolumeIds())) {
			volumes = instanceInfoFromDB.getVolumes();// volumeMapper.selectVolumesById(instanceInfoFromDB.getVolumeIds().split(","));
		} else {
			volumes = setVolumeInfo(serverNode, instanceDetail, ostoken);
		}
		if (null != volumes) {
			makeVolumeSnapshotInfo(volumes);
			instanceDetail.setVolumes(volumes);
			instanceDetail.makeVolumeIds();
		}

		// networks
		List<Network> networks = null;
		if (null != instanceInfoFromDB && !Util.isNullOrEmptyValue(instanceInfoFromDB.getNetworkIds())) {
			networks = instanceInfoFromDB.getNetworks();// networkMapper.selectNetworksById(instanceInfoFromDB.getNetworkIds().split(","));
		} else {
		//	networks = networkService.getInstanceAttachedNetworks(instanceDetail.getId());
		}
		if (null != networks) {
			instanceDetail.setNetworks(networks);
			instanceDetail.makeNetworkIds();
		}

		// SecurityGroups
		List<SecurityGroup> securityGroups = null;
		if (null != instanceInfoFromDB && !Util.isNullOrEmptyValue(instanceInfoFromDB.getSecurityGroupIds())) {
			securityGroups = instanceInfoFromDB.getSecurityGroups();// securityGroupMapper.selectSecurityGroupsById(instanceInfoFromDB.getSecurityGroupIds().split(","));
		} else {
			securityGroups = securityGroupService.getInstanceAttachedSecurityGroup(serverNode, ostoken.getTenantid());
		}
		if (null != securityGroups) {
			instanceDetail.setSecurityGroups(securityGroups);
			instanceDetail.makeSecurityGroupIds();
		}

	    //ports
		List<Port> ports = null;
		if (null != instanceInfoFromDB && !Util.isNullOrEmptyValue(instanceInfoFromDB.getPortIds())) {
			ports =  portMapper.selectPortsById(instanceInfoFromDB.getPortIds().split(","));
		}
		if (null != ports) {
			instanceDetail.setPorts(ports);
			instanceDetail.setPortIds(instanceInfoFromDB.getPortIds());
			updatePortSubnetIndo(ports);
		}

		return instanceDetail;
	}

	private void updateInstanceQuota(TokenOs ostoken, boolean bAdd, int core, int ram, int disk, int instance,
			String instanceType, String volumeType) {

		Map<String,Integer> resourceQuota = new HashMap<String,Integer>();
		Map<String,Integer> totalResourceQuota = new HashMap<String,Integer>();
		List<String> resourceType = new ArrayList<String>();
//		
		resourceQuota.put(instanceType + "_" + ParamConstant.CORE, core);
		totalResourceQuota.put(instanceType + "_" + ParamConstant.CORE, core);
		
		resourceQuota.put(instanceType + "_" + ParamConstant.RAM, ram / ParamConstant.MB);
		totalResourceQuota.put(instanceType + "_" + ParamConstant.RAM, ram);
		
		resourceType.add(instanceType + "_" + ParamConstant.CORE);
		resourceType.add(instanceType + "_" + ParamConstant.RAM);
//		quotaService.updateQuota(instanceType + "_" + ParamConstant.CORE, ostoken, bAdd, core);
//		quotaService.updateQuota(instanceType + "_" + ParamConstant.RAM, ostoken, bAdd, ram / ParamConstant.MB);

		VolumeType volumeTypeFromDB = volumeTypeMapper.selectByPrimaryKey(volumeType);
		if (null != volumeTypeFromDB) {
//			quotaService.updateQuota(volumeTypeFromDB.getName(), ostoken, bAdd, disk);
			resourceQuota.put(volumeTypeFromDB.getName(), disk);
			totalResourceQuota.put(volumeTypeFromDB.getName(), disk);
			
			resourceType.add(volumeTypeFromDB.getName());
		}
		// quotaService.updateQuota(Message.getMessage(volumeTypeName, false),
		// ostoken, bAdd, disk);

		// quotaService.updateQuota(volumeType, ostoken, bAdd, disk);
		
		quotaService.updateTenantResourcesQuota(resourceType,resourceQuota, ostoken, bAdd);
		resourceSpecService.updateTotalResourcesQuota(totalResourceQuota, resourceType, bAdd);
		poolService.updatePoolQuota(ostoken.getTenantid(), resourceQuota, bAdd);
//		resourceSpecService.updateResourceSpecQuota(ParamConstant.CORE, null, core, bAdd);
//		resourceSpecService.updateResourceSpecQuota(ParamConstant.RAM, null, ram / ParamConstant.MB, bAdd);
//		if(null != resourceSpecService){
//			resourceSpecService.updateResourceSpecQuota(volumeTypeFromDB.getName(), null, ram / ParamConstant.MB, bAdd);
//		}
		//resourceSpecService.updateResourceSpecQuota(ParamConstant.DISK, volumeType, disk, bAdd);
	}

//	private List<Instance> getInstances(Map<String, String> rs, String type, TokenOs ostoken, Boolean details)
//			throws JsonProcessingException, IOException, BusinessException {
//		ObjectMapper mapper = new ObjectMapper();
//		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
//		JsonNode serversNode = rootNode.path(ResponseConstant.INSTANCES);
//		int size = serversNode.size();
//		if (0 == size)
//			return null;
//		List<Instance> instances = new ArrayList<Instance>();
//		for (int i = 0; i < size; ++i) {
//			Instance instance = getInstanceInfo(serversNode.get(i), type, ostoken, details);
//			if (null == instance)
//				continue;
//			instances.add(instance);
//		}
//		return instances;
//	}

	private Instance getInstance(Map<String, String> rs, TokenOs ostoken, String type, Boolean details)
			throws JsonProcessingException, IOException, BusinessException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode instanceNode = rootNode.path(ResponseConstant.INSTANCE);
		return getInstanceInfo(instanceNode, type, ostoken, details);
	}

	private Instance getInstanceInfo(JsonNode instanceNode, String type, TokenOs ostoken, Boolean details)
			throws BusinessException {
		try {
			if (null == instanceNode)
				return null;
			// if (false == details) {
			// Instance instanceDetail = new Instance();
			// instanceDetail.setId(instanceNode.path(ResponseConstant.ID).textValue());
			// instanceDetail.setName(instanceNode.path(ResponseConstant.NAME).textValue());
			// instanceDetail.setStatus(instanceNode.path(ResponseConstant.STATUS).textValue());
			// instanceDetail.setCreatedAt(instanceNode.path(ResponseConstant.CREATED).textValue());
			// instanceDetail.setType(instanceNode.path(ResponseConstant.OS_EXT_AZ_AVAILABILITY_ZONE).textValue());
			// instanceDetail.setTenantId(instanceNode.path(ResponseConstant.TENANT_ID).textValue());
			// instanceDetail.setAvailabilityZone(instanceDetail.getType());
			// // image
			// JsonNode imageNode = instanceNode.path(ResponseConstant.IMAGE);
			// if (null != imageNode) {
			// String imageId = imageNode.path(ResponseConstant.ID).textValue();
			// if (Util.isNullOrEmptyValue(imageId))
			// return instanceDetail;
			// instanceDetail.setSourceId(imageId);
			// ImageServiceImpl imgService = new ImageServiceImpl();
			// imgService.setCloudconfig(cloudconfig);
			// imgService.setImageMapper(imageMapper);
			// Image image = imgService.getImage(instanceDetail.getSourceId(),
			// ostoken, null);
			// instanceDetail.setImage(image);
			// instanceDetail.setSourceName(image.getName());
			// }
			// return instanceDetail;
			// }
			return showInstance(instanceNode.path(ResponseConstant.ID).textValue(), ostoken, type, false, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		return null;
	}

	private List<QuotaDetail> getCreatedInstancesQuota(int count, Instance instanceInfo) {
		int cores = Integer.parseInt(instanceInfo.getCore());
		int ram = Integer.parseInt(instanceInfo.getRam());
		int disk = Integer.parseInt(instanceInfo.getVolumeSize());

		cores *= count;
		ram *= count;
		disk *= count;
		List<QuotaDetail> leastQuota = new ArrayList<QuotaDetail>();

		QuotaDetail cpuQuotaDetail = new QuotaDetail();
		cpuQuotaDetail.setTotal(cores);
		leastQuota.add(cpuQuotaDetail);

		QuotaDetail ramQuotaDetail = new QuotaDetail();
		ramQuotaDetail.setTotal(ram);
		leastQuota.add(ramQuotaDetail);

		QuotaDetail diskQuotaDetail = new QuotaDetail();
		diskQuotaDetail.setTotal(disk);
		diskQuotaDetail.setType(instanceInfo.getVolumeType());
		leastQuota.add(diskQuotaDetail);

		return leastQuota;
	}

	private Instance getCreateInstanceInfo(String createBody)
			throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(createBody, Instance.class);
	}

	private Port convertInterface2Port(InterfaceAttachment interfaceAttachment,TokenOs ostoken) throws BusinessException {
		Port port = portMapper.selectByPrimaryKey(interfaceAttachment.getPort_id());
		if (null == port)
			port = portService.getPort(interfaceAttachment.getPort_id(), ostoken, false);
		else
			return port;
		port.setId(interfaceAttachment.getPort_id());
		//port.setCreatedAt(Util.getCurrentDate());
		port.setMillionSeconds(Util.getCurrentMillionsecond());
		port.setMacAddress(interfaceAttachment.getMac_addr());
		port.setSubnetId(interfaceAttachment.getSubnet_id());
		port.setTenantId(ostoken.getTenantid());
		port.setStatus(interfaceAttachment.getPort_state());
		port.setNetwork_id(interfaceAttachment.getNet_id());
		return port;
	}

//	private String getDetachedPortId(String detachBody, TokenOs ostoken)
//			throws BusinessException, JsonProcessingException, IOException {
//		ObjectMapper mapper = new ObjectMapper();
//		JsonNode rootNode = mapper.readTree(detachBody);
//		// String networkId =
//		// rootNode.path(ResponseConstant.NETWORK_ID).textValue();
//		String subnetId = rootNode.path(ResponseConstant.SUBNET_ID).textValue();
//		String fixedIp = rootNode.path(ResponseConstant.FIXED_IP).textValue();
//		String floatingIp = rootNode.path(ResponseConstant.FLOATING_IP).textValue();
//		String ip = !Util.isNullOrEmptyValue(fixedIp) ? fixedIp : floatingIp;
//		List<Port> ports = portMapper.selectAllByTenantId(ostoken.getTenantid());
//		if (Util.isNullOrEmptyList(ports)) {
//			// PortServiceImpl portService = new PortServiceImpl();
//			ports = portService.getPortList(null, ostoken, true);
//			if (Util.isNullOrEmptyList(ports))
//				return null;
//		}
//		String portId = "";
//		if (Util.isNullOrEmptyValue(ip)) {
//			Subnet subnet = subnetMapper.selectByPrimaryKey(subnetId);
//			if (null == subnet) {
//				subnet = subnetService.getSubnet(subnetId, ostoken);
//			}
//			return portId;
//		} else {
//			for (Port port : ports) {
//				if (ip.equals(port.getIp()) && subnetId.equals(port.getSubnetId())) {
//					portId = port.getId();
//					break;
//				}
//			}
//		}
//		if (Util.isNullOrEmptyValue(portId))
//			return null;
//
//		return portId;
//	}

	private InterfaceAttachment getInterfaceAttachment(Map<String, String> rs)
			throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode interfaceAttachmentNode = rootNode.path(ResponseConstant.INTERFACEATTACHMENT);
		if (null == interfaceAttachmentNode)
			return null;
		InterfaceAttachment interfaceAttachment = new InterfaceAttachment();
		interfaceAttachment.setMac_addr(interfaceAttachmentNode.path(ResponseConstant.MAC_ADDR).textValue());
		interfaceAttachment.setNet_id(interfaceAttachmentNode.path(ResponseConstant.NET_ID).textValue());
		interfaceAttachment.setPort_id(interfaceAttachmentNode.path(ResponseConstant.PORT_ID).textValue());
		interfaceAttachment.setPort_state(interfaceAttachmentNode.path(ResponseConstant.PORT_STATE).textValue());

		JsonNode fixedIpNode = interfaceAttachmentNode.path(ResponseConstant.FIXED_IPS);
		return setFixedIPInfo(interfaceAttachment, fixedIpNode);

	}

	private InterfaceAttachment setFixedIPInfo(InterfaceAttachment interfaceAttachment, JsonNode fixedIpNode) {
		if (null == fixedIpNode)
			return interfaceAttachment;
		int fixedIPCount = fixedIpNode.size();
		for (int index = 0; index < fixedIPCount; ++index) {
			interfaceAttachment.addFixedIP(fixedIpNode.get(index).path(ResponseConstant.IP_ADDRESS).textValue(),
					fixedIpNode.get(index).path(ResponseConstant.SUBNET_ID).textValue());
		}
		interfaceAttachment.makeSubnetId();
		return interfaceAttachment;
	}

	private VolumeAttachment getVolumeAttachment(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode volumeAttachmentNode = rootNode.path(ResponseConstant.VOLUMEATTACHMENT);
		if (null == volumeAttachmentNode)
			return null;
		VolumeAttachment volumeAttachment = new VolumeAttachment();
		volumeAttachment.setId(volumeAttachmentNode.path(ResponseConstant.ID).textValue());
		volumeAttachment.setDevice(volumeAttachmentNode.path(ResponseConstant.DEVICE).textValue());
		volumeAttachment.setServerId(volumeAttachmentNode.path(ResponseConstant.SERVERID).textValue());
		volumeAttachment.setVolumeId(volumeAttachmentNode.path(ResponseConstant.VOLUMEID).textValue());

		return volumeAttachment;
	}

	private void updateInstance2DB(Instance updatedInstanceInfo, Instance instance) {
		if (!Util.isNullOrEmptyValue(updatedInstanceInfo.getName())) {
			instance.setName(updatedInstanceInfo.getName());
			//For 天津项目:  栁E��开发阶�?development,test,production)
			instance.setTag(updatedInstanceInfo.getTag());
			zabbixService.updateMonitorObjName(instance.getId(), instance.getName());
		}

		if (!Util.isNullOrEmptyValue(updatedInstanceInfo.getDescription()))
			instance.setDescription(updatedInstanceInfo.getDescription());
		instanceMapper.updateByPrimaryKeySelective(instance);
	}

	private Boolean buildInstanceInfoFromDB(Instance instance, String instanceId) {
		if (null == instance) {
			return true;
		}

		boolean bNotGet = false;
		boolean bHit = false;
		if (!Util.isNullOrEmptyValue(instance.getImageIds())) {
			bHit = true;
			List<Image> images = imageMapper.selectImagesById(instance.getImageIds().split(","));
			if (null != images && 0 != images.size()) {
				normalImageTime(images);
				instance.setImages(images);
			} else {
				bNotGet = true;
				// TODO
			}
		}

		// get volumes
		// List<Volume> volumes =
		// volumeMapper.selectListByInstanceId(instance.getId());
		if (!Util.isNullOrEmptyValue(instance.getVolumeIds())) {
			bHit = true;
			List<Volume> volumes = volumeMapper.selectVolumesById(instance.getVolumeIds().split(","));
			if (null != volumes && 0 != volumes.size()) {
				makeVolumeSnapshotInfo(volumes);
				instance.setVolumes(volumes);
			} else {
				bNotGet = true;
				// TODO
			}
		}

		// get networks
		if (!Util.isNullOrEmptyValue(instance.getNetworkIds())) {
			bHit = true;
			List<Network> networks = networkMapper.selectNetworksById(instance.getNetworkIds().split(","));
			if (null != networks && 0 != networks.size()) {
				instance.setNetworks(networks);
//				for (Network network : networks) {
//					addSubnetInfo(network);
//					addPortInfo(network);
//					addSecurityInfo(network);
//					addFloatingIpInfo(network);
//					instance.addNetwork(network);
//				}
			} else {
				bNotGet = true;
			}
		}

		// get keypairs
		if (!Util.isNullOrEmptyValue(instance.getKeypairIds())) {
			bHit = true;
			List<Keypair> keypairs = keypairMapper.selectKeypairsById(instance.getKeypairIds().split(","));
			if (null != keypairs && 0 != keypairs.size()) {
				instance.setKeypairs(keypairs);
			} else {
				bNotGet = true;
				// TODO
			}
		}

		// get securitygroup
		if (!Util.isNullOrEmptyValue(instance.getSecurityGroupIds())) {
			bHit = true;
			List<SecurityGroup> securityGroups = securityGroupMapper
					.selectSecurityGroupsById(instance.getSecurityGroupIds().split(","));
			if (null != securityGroups && 0 != securityGroups.size()) {
			//	normalSecurityGroupTime(securityGroups);
				instance.setSecurityGroups(securityGroups);
			} else {
				bNotGet = true;
				// TODO
			}
		}

		// get floatingip
		if (!Util.isNullOrEmptyValue(instance.getFloatingips())) {
			bHit = true;
			List<FloatingIP> floatingIPs = floatingIpMapper.selectListByFloatingIps(Util.stringToList(instance.getFloatingips(), ","));
			if (null != floatingIPs && 0 != floatingIPs.size()) {
				instance.setAttachedFloatingIPs(floatingIPs);
			} else {
				bNotGet = true;
				// TODO
			}
		}
		
		// get port
		if (!Util.isNullOrEmptyValue(instance.getPortIds())) {
			bHit = true;
			List<Port> ports = portMapper.selectPortsById(instance.getPortIds().split(","));
			if (null != ports && 0 != ports.size()) {
				instance.setPorts(ports);
				updatePortSubnetIndo(ports);
			} else {
				bNotGet = true;
				// TODO
			}
		}

		if (false == bHit)
			return true;

		return bNotGet;
	}

	private void updatePortSubnetIndo(List<Port> ports){
		if(Util.isNullOrEmptyList(ports))
			return;
		for(Port port : ports){
			port.setSubnet(subnetMapper.selectByPrimaryKey(port.getSubnetId()));
		}
	}
	
//	private void normalSecurityGroupTime(List<SecurityGroup> securityGroupsFromDB){
//		for(SecurityGroup securityGroup : securityGroupsFromDB){
//			securityGroup.setCreatedAt(Util.millionSecond2Date(securityGroup.getMillionSeconds()));
//		}
//	}
//	
	private void normalImageTime(List<Image> imagesFromDB){
		for(Image image : imagesFromDB){
			image.setCreatedAt(Util.millionSecond2Date(image.getMillionSeconds()));
		}
	}
	
	private InterfaceAttachment getInterfaceAttachmentInfo(String attachBody,Locale locale)
			throws JsonParseException, JsonMappingException, IOException, ResourceBusinessException {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(attachBody);
		JsonNode portIdsNode = rootNode.path(ResponseConstant.IDS);
		int portIdsCount = portIdsNode.size();
		if(1 != portIdsCount)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);

//		List<String> netIds = new ArrayList<String>();
//		for (int index = 0; index < netIdsCount; ++index) {
//			netIds.add(netIdsNode.get(index).textValue());
//		}
        
		InterfaceAttachment interfaceAttachmentInfo = new InterfaceAttachment();
		interfaceAttachmentInfo.setPort_id(portIdsNode.get(0).textValue());
	//	interfaceAttachmentInfo.setNet_id(Util.listToString(netIds, ','));
		return interfaceAttachmentInfo;
	}

	private String getInterfaceAttachmentBody(InterfaceAttachment interfaceAttachmentInfo) {

		InterfaceAttachmentJSON interfaceAttachmentJson = new InterfaceAttachmentJSON(interfaceAttachmentInfo);
		JsonHelper<InterfaceAttachmentJSON, String> jsonHelp = new JsonHelper<InterfaceAttachmentJSON, String>();
		return jsonHelp.generateJsonBodySimple(interfaceAttachmentJson);
	}

	private VolumeAttachment getVolumeAttachmentInfo(String attachBody)
			throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(attachBody);
		JsonNode volumeIdsNode = rootNode.path(ResponseConstant.IDS);
		int volumeIdsCount = volumeIdsNode.size();
		List<String> volumeIds = new ArrayList<String>();
		for (int index = 0; index < volumeIdsCount; ++index) {
			volumeIds.add(volumeIdsNode.get(index).textValue());
		}
		VolumeAttachment volumeAttachmentInfo = new VolumeAttachment();
		volumeAttachmentInfo.setVolumeId(Util.listToString(volumeIds, ','));
		return volumeAttachmentInfo;
	}

	private String getVolumeAttachmentBody(VolumeAttachment volumeAttachmentInfo) {
		VolumeAttachmentJSON volumeAttachmentJson = new VolumeAttachmentJSON(volumeAttachmentInfo);
		JsonHelper<VolumeAttachmentJSON, String> jsonHelp = new JsonHelper<VolumeAttachmentJSON, String>();
		return jsonHelp.generateJsonBodySimple(volumeAttachmentJson);
	}

	private Instance getUpdatedInstanceInfo(String updateBody)
			throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		Instance instanceInfo = mapper.readValue(updateBody, Instance.class);
		return instanceInfo;
	}

	private String getUpdatedInstanceBody(Instance updatedInstanceInfo) {
		InstanceJSON instanceJson = new InstanceJSON();
		instanceJson.updateInstanceInfo(updatedInstanceInfo.getName(), updatedInstanceInfo.getDescription());
		JsonHelper<InstanceJSON, String> jsonHelp = new JsonHelper<InstanceJSON, String>();
		return jsonHelp.generateJsonBodySimple(instanceJson);
	}

	private void storeInfo2DB(Instance instance) {
		instance.setSystemName(instance.getSourceName());
		List<Volume> volumes = instance.getVolumes();
		if (null != volumes) {
			for (Volume volume : volumes) {
				if (null == volume || Util.isSystemVolume(volume.getDevice()))
					continue;
				if (null == volumeMapper.selectByPrimaryKey(volume.getId()))
					volumeMapper.insertSelective(volume);
			}
		}
		List<Keypair> keypairs = instance.getKeypairs();
		if (null != keypairs) {
			for (Keypair keypair : keypairs) {
				if (null == keypair)
					continue;
				if (null == keypairMapper.selectByName(keypair.getName()))
					keypairMapper.insertSelective(keypair);
			}
		}

		if (null == instance.getNetworks() || 0 == instance.getNetworks().size()) {
			Network network = networkMapper.selectByPrimaryKey(instance.getId());
			if (null != network) {
				addSubnetInfo(network);
				addPortInfo(network);
				addSecurityInfo(network);
				addFloatingIpInfo(network);
				instance.addNetwork(network);
			}
		}

	}

	private void addSubnetInfo(Network network) {
		if (null == network)
			return;
		String subnetId = network.getSubnetId();
		if (Util.isNullOrEmptyValue(subnetId))
			return;
		String[] subnetsId = subnetId.split(",");
		for (int index = 0; index < subnetsId.length; ++index) {
			Subnet subnet = subnetMapper.selectByPrimaryKey(subnetsId[index]);
			if (null == subnet)
				continue; // TODO
			network.addSubnet(subnet);
		}
	}

	private void addPortInfo(Network network) {
		if (null == network)
			return;
		String portId = network.getPortId();
		if (Util.isNullOrEmptyValue(portId))
			return;
		String[] portsId = portId.split(",");
		for (int index = 0; index < portsId.length; ++index) {
			Port port = portMapper.selectByPrimaryKey(portsId[index]);
			if (null == port)
				continue; // TODO
			network.addPort(port);
		}
	}

	private void addSecurityInfo(Network network) {
		if (null == network)
			return;
		List<Port> ports = portMapper.selectListByNetworkId(network.getId());
		if (Util.isNullOrEmptyList(ports))
			return;
		for (Port port : ports) {
			if (Util.isNullOrEmptyValue(port.getSecurityGroupId()))
				continue;
			String[] securityGroupsId = port.getSecurityGroupId().split(",");
			for (int index = 0; index < securityGroupsId.length; ++index) {
				SecurityGroup securityGroup = securityGroupMapper.selectByPrimaryKey(securityGroupsId[index]);
				if (null == securityGroup)
					continue; // TODO
				network.addSecurityGroup(securityGroup);
			}
		}
	}

	private void addFloatingIpInfo(Network network) {
		if (null == network)
			return;
		String floatingIpId = network.getFloatingipId();
		if (Util.isNullOrEmptyValue(floatingIpId))
			return;
		String[] floatingIpsId = floatingIpId.split(",");
		for (int index = 0; index < floatingIpsId.length; ++index) {
			FloatingIP floatingIp = floatingIpMapper.selectByPrimaryKey(floatingIpsId[index]);
			if (null == floatingIp)
				continue; // TODO
			network.addFloatingIP(floatingIp);
		}
	}

	private void storeKeypair2DB(Instance instanceInfo, Instance createdInstance) {
		List<Keypair> keypairs = instanceInfo.getKeypairs();
		if (!Util.isNullOrEmptyList(keypairs)) {
			for (Keypair keypair : keypairs) {
				String instanceId = Util.getIdWithAppendId(createdInstance.getId(), keypair.getInstanceId());
				keypair.setInstanceId(instanceId);
				
//				keypairFromDB = keypairMapper.selectByPrimaryKey(keypair.getId());
//				if (null != keypairFromDB) {
//					String instanceId = Util.getIdWithAppendId(createdInstance.getId(), keypairFromDB.getInstanceId());
//					keypair.setInstanceId(instanceId);
//					keypairMapper.updateByPrimaryKeySelective(keypair);
//				} else {
//					keypair.setInstanceId(createdInstance.getId());
//					keypairMapper.insertSelective(keypair);
//				}
			}
			keypairMapper.insertOrUpdateBatch(keypairs);
		}
		
	}

	private List<String> storeNetwork2DB(Instance instanceInfo) {
		List<Network> networks = instanceInfo.getNetworks();
		List<String> networksId = new ArrayList<String>();
		if (null != networks) {
			for (Network network : networks) {
				networksId.add(network.getId());
				if (null == networkMapper.selectByPrimaryKey(network.getId()))
					networkMapper.insertSelective(network);
				List<Subnet> subnets = network.getSubnets();
				for (Subnet subnet : subnets) {
					if (null != subnetMapper.selectByPrimaryKey(subnet.getId()))
						subnetMapper.updateByPrimaryKeySelective(subnet);
					else
						subnetMapper.insertSelective(subnet);
				}
			}
		}
		return networksId;
	}

	private void storeInstances2DB(List<Instance> instances) throws BusinessException {
		if (Util.isNullOrEmptyList(instances))
			return;
		try {
			instanceMapper.insertOrUpdateBatch(instances);
		} catch (Exception e) {
			log.error(e);
		}
	}


	private Map<String, Map<String, String>> buildInstanceParam(Instance instanceInfo, String type, TokenOs ostoken)
			throws BusinessException, JsonProcessingException, IOException {

		Map<String, Map<String, String>> instanceParamMap = new HashMap<String, Map<String, String>>();
		String name = instanceInfo.getName();
		String source_type = instanceInfo.getSourceType();
		String source_id = instanceInfo.getSourceId();
		String vcpus = instanceInfo.getCore(); // TODO change it later
		String ram = instanceInfo.getRam();
		String disk = instanceInfo.getVolumeSize();
		String network_type = instanceInfo.getNetworkType();
		String subnet_id = "";
		Boolean bBasicNetwork = network_type == null ? null : false; // TODO change it later
		Boolean vmwareZone = false;
	//	String volume_type = instanceInfo.getVolumeType();
		if (ParamConstant.VDI_TYPE.equals(type)) {
			instanceInfo.setAvailabilityZone(cloudconfig.getSystemVdiSpec());
		}
        
		if (instanceInfo.getAvailabilityZone().equals(cloudconfig.getSystemVmwareZone())) {
			subnet_id = instanceInfo.getSubnet();
			vmwareZone = true;
		} else {
			if (ParamConstant.BASIC_NET.equals(network_type))
				bBasicNetwork = true;
			else
				subnet_id = instanceInfo.getSubnet();
		}
		// String volumeType =
		// Util.getVolumeType(instanceInfo.getVolumeType(),cloudconfig.getSystemVolumeSpec().split(","));
		// instanceInfo.setVolumeType(volumeType);

		// String min_count = "1";
		// String max_count = instanceInfo.getQuantity();
	
		String credential_type = instanceInfo.getCredentialType();
		// String username = null;
	//	String password = null;
		String keypairName = null;
		String userdata = null;
		Image image = imageMapper.selectByPrimaryKey(source_id);
		if(null != image && true == image.getPrivateFlag()){
			Image baseImage = imageMapper.selectByPrimaryKey(image.getBaseImageId());
			if(null != baseImage && null != image.getMinDisk()){
				disk = Integer.toString(image.getMinDisk());
				instanceInfo.setVolumeSize(disk);	
			}
		}
		
		if (ParamConstant.PASSWORD_CREDENTIAL.equals(credential_type)) {
			// username = instanceInfo.getUsername();
//			String password = instanceInfo.getPassword();
//			if(Util.isNullOrEmptyValue(password))
//				password = 
			if(null == image)
				userdata = getUserdata(null,instanceInfo.getUsername(),instanceInfo.getPassword());
			else
				userdata = getUserdata(image.getSystemType(),instanceInfo.getUsername(),instanceInfo.getPassword());
			if(null == userdata)
				throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,new Locale(ostoken.getLocale()));
			
		//	password = instanceInfo.getPassword();
		} else {
			keypairName = instanceInfo.getKeypairName();
			Keypair keypair = keypairService.getKeypair(keypairName, ostoken);
			instanceInfo.addKeypair(keypair);
		}

		String flavorRef = flavorService.getFlavor(ostoken, vcpus, ram, null,vmwareZone);
		if (null == flavorRef)
			return null; // TODO maybe throw exception

		instanceParamMap.put(ParamConstant.SERVER, new HashMap<String, String>());
		instanceParamMap.get(ParamConstant.SERVER).put(ParamConstant.NAME, name);
		instanceParamMap.get(ParamConstant.SERVER).put(ParamConstant.FLAVORREF, flavorRef);
		instanceParamMap.get(ParamConstant.SERVER).put(ParamConstant.IMAGEREF, source_id);
		instanceParamMap.get(ParamConstant.SERVER).put(ParamConstant.KEY_NAME, keypairName);
		instanceParamMap.get(ParamConstant.SERVER).put(ParamConstant.USER_DATA, userdata);
		if (!Util.isNullOrEmptyValue(instanceInfo.getAvailabilityZone()))
			instanceParamMap.get(ParamConstant.SERVER).put(ParamConstant.AVAILABILITY_ZONE,
					instanceInfo.getAvailabilityZone());
		// instanceParamMap.get(ParamConstant.SERVER).put(ParamConstant.AVAILABILITY_ZONE,
		// availability_zone);
		// instanceParamMap.get(ParamConstant.SERVER).put(ParamConstant.MIN_COUNT,
		// min_count);
		// instanceParamMap.get(ParamConstant.SERVER).put(ParamConstant.MAX_COUNT,
		// max_count);

		Map<String, String> networkInfo = null;
		if(null != bBasicNetwork){
			networkInfo = getNetInfo(bBasicNetwork, subnet_id, ostoken, instanceInfo);
		}else{
			networkInfo = new HashMap<String, String>();
		}
		if(!Util.isNullOrEmptyValue(instanceInfo.getExternalNetId()))
			networkInfo.put(ParamConstant.EXTERNAL_NET_UUID, instanceInfo.getExternalNetId());
		instanceParamMap.put(ParamConstant.NETWORKS, networkInfo);

		// List<String> metadataValues = null; // TODO
		// if (null != volume_type && !volume_type.isEmpty()) {
		// metadataValues = new ArrayList<String>();
		// metadataValues.add(volume_type);
		// }

		Map<String, String> blockDeviceInfo = getBlockDeviceInfo(source_type, source_id, instanceInfo.getAvailabilityZone(),disk, "0",new Locale(ostoken.getLocale()));
		if (null != blockDeviceInfo)
			instanceParamMap.put(ParamConstant.BLOCK_DEVICE_MAPPING_V2, blockDeviceInfo);

		return instanceParamMap;
	}

	private Image storeImage2DB(String instanceId, String imageId, String type, TokenOs ostoken, String body)
			throws BusinessException, JsonProcessingException, IOException {
		Image image = imageService.getImage(imageId, ostoken);
		if(null == image)
			throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_IMAGE_CREATE_FAILED,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		
		image.setMillionSeconds(Util.getCurrentMillionsecond());
		image.setInstanceId(instanceId);
//		image.setId(imageId);
		Instance instance = instanceMapper.selectByPrimaryKey(instanceId);
		if (null == instance) {
			instance = showInstance(instanceId, ostoken, type, true, null);
			if (null != instance)
				image.setSystemName(instance.getSourceName());
		} else {
			image.setSystemName(instance.getSourceName());
		}
		List<String> appendImageId = new ArrayList<String>();
		appendImageId.add(imageId);
		instance.setImageIds(Util.getAppendedIds(instance.getImageIds(), appendImageId));
		instanceMapper.insertOrUpdate(instance);
		
		String baseImageId = instance.getSourceId();
		Image baseImage = imageMapper.selectByPrimaryKey(baseImageId);
		if(null != baseImage){
			image.setSystemType(baseImage.getSystemType());
			image.setObjectType(baseImage.getObjectType());
			image.setMinDisk(baseImage.getMinDisk());
		}
		
//		ObjectMapper mapper = new ObjectMapper();
//		JsonNode rootNode = mapper.readTree(body);
//		JsonNode imageNode = rootNode.path(ParamConstant.CREATE_IMAGE_ACTION);
//		String imageName = imageNode.path(ParamConstant.NAME).textValue();
//		image.setName(imageName);
//		image.setMillionSeconds(Util.time2Millionsecond(Util.getCurrentDate(), ParamConstant.TIME_FORMAT_01));
	//	image.setCreatedAt(Util.getCurrentDate());
		image.setPrivateFlag(true);
		image.setTenantId(ostoken.getTenantid());
		// imageMapper.insertSelective(image);
		imageMapper.insertOrUpdate(image); //need to update the image status
//		if (null == imageMapper.selectByPrimaryKey(image.getId()))
//			imageMapper.insertSelective(image);
//		else
//			imageMapper.updateByPrimaryKeySelective(image);
		StringBuilder sb = new StringBuilder();
		sb.append(ParamConstant.INSTANCE);
		sb.append(":");
		sb.append(instanceId);
		
		updateSyncResourceInfo(ostoken.getTenantid(),image.getId(),sb.toString(),null,ParamConstant.ACTIVE_STATUS,ParamConstant.IMAGE,ostoken.getCurrentRegion(),image.getName());
		
		return image;
	}

	private String generateBody(Map<String, Map<String, String>> paramMap, List<String> names,
			List<String> metadataValues) {
		if (null == paramMap || 0 == paramMap.size())
			return "";

		Map<String, String> serverMap = paramMap.get(ParamConstant.SERVER);
		if (null == serverMap)
			return "";

		String name = serverMap.get(ParamConstant.NAME);
		String imageRef = serverMap.get(ParamConstant.IMAGEREF);
		String flavorRef = serverMap.get(ParamConstant.FLAVORREF);
		String key_name = serverMap.get(ParamConstant.KEY_NAME);
	//	String adminPass = serverMap.get(ParamConstant.ADMINPASS);
		String userdata = serverMap.get(ParamConstant.USER_DATA);
		String availability_zone = serverMap.get(ParamConstant.AVAILABILITY_ZONE);
		Integer min_count = null != serverMap.get(ParamConstant.MIN_COUNT)
				? Integer.parseInt(serverMap.get(ParamConstant.MIN_COUNT)) : 1;
		Integer max_count = null != serverMap.get(ParamConstant.MAX_COUNT)
				? Integer.parseInt(serverMap.get(ParamConstant.MAX_COUNT)) : 1;
//		InstanceJSON instanceJSON = new InstanceJSON(name, imageRef, flavorRef, key_name, adminPass, availability_zone,
//				min_count, max_count);
		InstanceJSON instanceJSON = new InstanceJSON(name, imageRef, flavorRef, key_name, userdata, availability_zone,
				min_count, max_count);

		Map<String, String> blockDeviceMap = paramMap.get(ParamConstant.BLOCK_DEVICE_MAPPING_V2);
		if (null != blockDeviceMap) {
			String uuid = blockDeviceMap.get(ParamConstant.UUID);
			String source_type = blockDeviceMap.get(ParamConstant.SOURCE_TYPE);
			String destination_type = blockDeviceMap.get(ParamConstant.DESTINATION);
			Integer boot_index = null != blockDeviceMap.get(ParamConstant.BOOT_INDEX)
					? Integer.parseInt(blockDeviceMap.get(ParamConstant.BOOT_INDEX)) : null;
			Integer size = null != blockDeviceMap.get(ParamConstant.VOLUME_SIZE)
					? Integer.parseInt(blockDeviceMap.get(ParamConstant.VOLUME_SIZE)) : null;
			Boolean delete_on_termination = null != blockDeviceMap.get(ParamConstant.DELETE_ON_TERMINATION)
					? Boolean.parseBoolean(blockDeviceMap.get(ParamConstant.DELETE_ON_TERMINATION)) : false;
			instanceJSON.createBlock_device_mapping_v2(uuid, source_type, destination_type, boot_index, size,
					delete_on_termination);
		}

		Map<String, String> netMap = paramMap.get(ParamConstant.NETWORKS);
		if (null != netMap) {
			for (Map.Entry<String, String> entry : netMap.entrySet()) {
				String net_uid = entry.getValue();
				instanceJSON.createNetworks(net_uid, null, null);
			}
		//	String net_uid = netMap.get(ParamConstant.UUID);
		//	String fixed_ip = netMap.get(ParamConstant.FIXED_IP);
		//	String port = netMap.get(ParamConstant.PORT);
		//	instanceJSON.createNetworks(net_uid, null, null);
		}

		if (null != metadataValues) {
			instanceJSON.createMetadata(metadataValues);
		}

		instanceJSON.createSecurity_groups(names);

		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.setSerializationInclusion(Include.NON_EMPTY);
		String jsonStr = "";
		try {
			jsonStr = mapper.writeValueAsString(instanceJSON);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		return jsonStr;
	}

	private Map<String, String> getNetInfo(Boolean bBasicNetwork, String subnet_id, TokenOs ostoken,
			Instance instanceInfo) throws BusinessException, JsonProcessingException, IOException {

		if (true == bBasicNetwork) {
			Network network = networkMapper.selectTenantBasicNetwork(ostoken.getTenantid());
			if(null == network){
				network = networkService.createNetwork(null,ostoken);
				if(null != network){
					network.setBasic(true);
					networkMapper.updateByPrimaryKeySelective(network);
				}
			}
			if (null == network)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));

			String network_id = network.getId();
			instanceInfo.setNetworkIds(network_id);
			
			String subnetsId = network.getSubnetId();
			Subnet subnet = null;
			if(Util.isNullOrEmptyValue(subnetsId)){
				List<Subnet> subnets = subnetService.createSubnet(null, instanceInfo.getName(), ostoken, network_id);
				if (Util.isNullOrEmptyList(subnets))
					throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
				subnet = subnets.get(0);
			}else{
				subnet = subnetMapper.selectByPrimaryKey(subnetsId.split(",")[0]);
				if(null == subnet)
					throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
			}
			network.addSubnet(subnet);
			instanceInfo.addNetwork(network);
			Map<String, String> netParamMap = new HashMap<String, String>();
			netParamMap.put(ParamConstant.UUID, network_id);
			return netParamMap;
		} else {
			if (Util.isNullOrEmptyValue(subnet_id))
				return null;
			Subnet subnet = subnetService.getSubnet(subnet_id, ostoken);
			if (null == subnet)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
			// TODO save the subnet to local db
			Network network = new Network();
			network.setId(subnet.getNetwork_id());
			network.addSubnet(subnet);
			instanceInfo.addNetwork(network);
			instanceInfo.setNetworkIds(subnet.getNetwork_id());
			Map<String, String> netParamMap = new HashMap<String, String>();
			// netParamMap.put(ParamConstant.NETWORKS, new
			// HashMap<String,String>());
			netParamMap.put(ParamConstant.UUID, subnet.getNetwork_id());

			return netParamMap;
		}
	}

	private Map<String, String> getBlockDeviceInfo(String source_type, String sourceId, String availabilityZone,String diskSize,
			String bootIndex,Locale locale) throws BusinessException {

		if (null == source_type || source_type.isEmpty())
			throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_CREATE_FAILED,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);

		Map<String, String> blodkDeviceParamMap = new HashMap<String, String>();
		blodkDeviceParamMap.put(ParamConstant.UUID, sourceId);
		blodkDeviceParamMap.put(ParamConstant.SOURCE_TYPE, source_type);
		if (availabilityZone.equals(cloudconfig.getSystemVmwareZone())) {
			blodkDeviceParamMap.put(ParamConstant.DESTINATION, ParamConstant.LOCAL);
		} else {
			blodkDeviceParamMap.put(ParamConstant.DESTINATION, ParamConstant.VOLUME);	
		}
		blodkDeviceParamMap.put(ParamConstant.VOLUME_SIZE, diskSize);
		blodkDeviceParamMap.put(ParamConstant.BOOT_INDEX, bootIndex);
		blodkDeviceParamMap.put(ParamConstant.DELETE_ON_TERMINATION, "true");
		return blodkDeviceParamMap;
	}

	@Override
	public InstanceConfig getInstanceConfig(String type, TokenOs authToken) throws BusinessException {
		return this.configService.getInstanceConfig(type, authToken);
//		// read from DB later
//				List<String> volumeTypeId = new ArrayList<String>();
//				List<String> volumeTypeName = new ArrayList<String>();
//				List<VolumeType> volumeTypes = volumeTypeService.getVolumeTypeList(null, authToken);
//				if (Util.isNullOrEmptyList(volumeTypes)) {
//					throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
//				} else {
//					for (VolumeType volumeType : volumeTypes) {
//						volumeTypeId.add(volumeType.getId());
//						volumeTypeName.add(volumeType.getName());
//					}
//				}
//				
//				TemplateTenantMapping tenantReating = null;
//				try{
//					tenantReating = ratingTemplateService.getTenantRating(authToken.getTenantid(),authToken);
//				}catch (Exception e){
//					log.error(e);
//				}
//				
//				VolumeConfig volumeConfig = new VolumeConfig();
//				volumeConfig.setSize(Integer.parseInt(cloudconfig.getSystemVolumeSize()));
//			    volumeConfig.setWindowsSystemVolumeSize(Integer.parseInt(cloudconfig.getSystemWindowsVolumeSize()));
//			    volumeConfig.setLinuxSystemVolumeSize(Integer.parseInt(cloudconfig.getSystemLinuxVolumeSize()));
//			    
//				String[] volumePrices = cloudconfig.getVolumePrice().split(",");
//
//				String[] curVolumePrices = this.ratingTemplateService.getConfigPrice(tenantReating,ParamConstant.STORAGE, volumeTypeName.toArray(new String[volumeTypes.size()]), volumePrices, authToken);
//
//				Locale locale = new Locale(authToken.getLocale());
//				if (volumeTypeName.size() != curVolumePrices.length)
//					throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
//				for (int index = 0; index < curVolumePrices.length; ++index) {
//					// volumeConfig.addType(volumeTypes[index],
//					// Double.valueOf(volumePrices[index]));
//					volumeConfig.addType(volumeTypeId.get(index),
//							Message.getMessage(volumeTypeName.get(index).toUpperCase(),locale,false),
//							Double.valueOf(curVolumePrices[index]));
//				}
//
//				String[] volumeRange = cloudconfig.getVolumeRange().split(",");
//				if (2 != volumeRange.length)
//					throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
//				volumeConfig.setRange(new Range(Integer.parseInt(volumeRange[0]), Integer.parseInt(volumeRange[1])));
//
//				InstanceConfig instanceConfig = new InstanceConfig();
//				instanceConfig.setVolume(volumeConfig);
//
//				String[] instanceTypes = null;
////				String[] corePrices = null;
////				String[] ramPrices = null;
//
//				Map<String,String> corePriceMap = getSystemCorePrice();
//				Map<String,String> ramPriceMap  = getSystemRamPrice();
//				
//				List<HostAggregate> aggregates = null;
//				if (type.equals(ParamConstant.VDI_TYPE)) {
//					CloudService service = serviceService.getSystemServiceByType(ParamConstant.VDI_TYPE);
//					if(null != service){
//						aggregates = hostAggregateMapper.selectByServiceId(service.getId());
//					}
//					instanceTypes = cloudconfig.getSystemVdiSpec().split(",");
//			//		corePrices = cloudconfig.getVdiCorePrice().split(",");
//			//		ramPrices = cloudconfig.getVdiRamPrice().split(",");
//				} else {
//					CloudService kvmService = serviceService.getSystemServiceByType(ParamConstant.INSTANCE_TYPE);
//					CloudService vmwareService = serviceService.getSystemServiceByType(ParamConstant.VMWARE_TYPE);
//		            if(null != kvmService){
//		            	aggregates = hostAggregateMapper.selectByServiceId(kvmService.getId());
//		            	if(null != vmwareService){
//		            		if(null != aggregates){
//		            			List<HostAggregate> vmwareAggregate = hostAggregateMapper.selectByServiceId(vmwareService.getId());
//		            			if(null != vmwareAggregate)
//		            				aggregates.addAll(vmwareAggregate);
//		            		}
//		            		else
//		            			aggregates = hostAggregateMapper.selectByServiceId(vmwareService.getId());	
//		            	}
//		            }else if(null != vmwareService){
//		            	aggregates = hostAggregateMapper.selectByServiceId(vmwareService.getId());	
//		            }
//		            instanceTypes = cloudconfig.getSystemInstanceSpec().split(",");
//				//	corePrices = cloudconfig.getCorePrice().split(",");
//				//	ramPrices = cloudconfig.getRamPrice().split(",");
//				}
//				
//		        if(null != aggregates)
//		        	instanceTypes = getInstanceTypes(aggregates).split(",");
//
//				String[] coreInstanceTypes = new String[instanceTypes.length];
//				String[] ramInstanceTypes = new String[instanceTypes.length];
//				for (int i = 0; i < instanceTypes.length; i++) {
//					coreInstanceTypes[i] = instanceTypes[i] + "_core";
//					ramInstanceTypes[i] = instanceTypes[i] + "_ram";
//				}
//				
////				String[] curCorePrices = this.ratingTemplateService.getConfigPrice(tenantReating,ParamConstant.COMPUTE, coreInstanceTypes,
////						corePrices, authToken);
////				String[] curRamPrices = this.ratingTemplateService.getConfigPrice(tenantReating,ParamConstant.COMPUTE, ramInstanceTypes,
////						ramPrices, authToken);
//
//				Map<String,String> curCorePrices = this.ratingTemplateService.getSystemInstanceTypePrice(tenantReating,ParamConstant.COMPUTE, coreInstanceTypes,
//						corePriceMap, authToken);
//				Map<String,String> curRamPrices = this.ratingTemplateService.getSystemInstanceTypePrice(tenantReating,ParamConstant.COMPUTE, ramInstanceTypes,
//						ramPriceMap, authToken);
//
//				
//			//	if (instanceTypes.length != curCorePrices.length || curCorePrices.length != curRamPrices.length)
//			//		throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
//				String[] coreSize = cloudconfig.getSystemCpuSpec().split(",");
//				String[] ramSize = cloudconfig.getSystemRamSpec().split(",");
//				for (int index = 0; index < instanceTypes.length; ++index) {
//					InstanceType instanceType = new InstanceType();
//					// instanceType.setName(instanceTypes[index]);
//					instanceType.setId(instanceTypes[index]);
//			//		instanceType.setName(Message.getMessage(instanceTypes[index].toUpperCase(),locale,false));
//					instanceType.setName(getInstanceTypeName(aggregates, instanceTypes[index], locale));
//					String price = curCorePrices.get(instanceTypes[index]);
//					if(null == price)
//						price = curCorePrices.get(ParamConstant.GENERAL_CORE);
//				//	instanceType.addCore(coreSize, Double.valueOf(curCorePrices[index]));
//					instanceType.addCore(coreSize, Double.valueOf(price));
//					price = curRamPrices.get(instanceTypes[index]);
//					if(null == price)
//						price = curRamPrices.get(ParamConstant.GENERAL_RAM);
//				//	instanceType.addRam(ramSize, Double.valueOf(curRamPrices[index]));
//					instanceType.addRam(ramSize, Double.valueOf(price));
//					instanceConfig.addInstanceType(instanceType);
//				}
//				
//				if(!Util.isNullOrEmptyValue(cloudconfig.getImagePrice())){
//					ImageConfig imageConfig = new ImageConfig();
//					String[] imagePrices = cloudconfig.getImagePrice().split(",");
//					String[] imageTypes = cloudconfig.getSystemImageSpec().split(",");
//					if (imagePrices.length != imageTypes.length)
//						throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(authToken.getLocale()));
//							
//					String[] curImagePrices = this.ratingTemplateService.getConfigPrice(tenantReating,ParamConstant.IMAGE, imageTypes,
//							imagePrices, authToken);
//					for (int index = 0; index < curImagePrices.length; ++index) {
//						ResourceSpec imageType = new ResourceSpec();
//						// instanceType.setName(instanceTypes[index]);
//						imageType.setName(imageTypes[index]);
//						imageType.setUnitPrice(Double.valueOf(curImagePrices[index]));
//						imageConfig.addImageType(imageType);
//					}
//					instanceConfig.setImage(imageConfig);
//				}
//				return instanceConfig;
	}

	private void updateAttachVolumeInfo(String instanceId, String volumeId, TokenOs ostoken) throws BusinessException {
//		Volume volume =  volumeService.refreshVolumeInfo(volumeId, ostoken, response);
		Volume volume = volumeMapper.selectByPrimaryKey(volumeId);
		if(null == volume)
			volume =  volumeService.refreshVolumeInfo(volumeId, ostoken);
		volume.setInstanceId(instanceId);
//		Volume volume = volumeMapper.selectByPrimaryKey(volumeId);
//		if (null != volume) {
//			volume.setInstanceId(instanceId);
//			volume.setStatus(ParamConstant.IN_USE);
//		} else {
//			volume = volumeService.getVolume(volumeId, ostoken, response);
//		}
		volumeMapper.updateByPrimaryKeySelective(volume);
		
		StringBuilder sb = new StringBuilder();
		sb.append(ParamConstant.INSTANCE);
		sb.append(":");
		sb.append(instanceId);
		updateSyncResourceInfo(ostoken.getTenantid(),volumeId,sb.toString(),volume.getStatus(),ParamConstant.IN_USE,ParamConstant.VOLUME,ostoken.getCurrentRegion(),volume.getName());

		Instance instance = instanceMapper.selectByPrimaryKey(instanceId);
		
		String volumeIds = instance.getVolumeIds();
		List<String> appendId = new ArrayList<String>();
		appendId.add(volumeId);
//		if (!Util.isNullOrEmptyValue(volumeIds)) {
//			volumeIds += ",";
//			volumeIds += volumeId;
//		} else {
//			volumeIds = volumeId;
//		}
		instance.setVolumeIds(Util.getAppendedIds(volumeIds, appendId));
		instanceMapper.updateByPrimaryKeySelective(instance);
	}

	private void updateDetachVolumeInfo(String instanceId, String volumeId, TokenOs ostoken) throws BusinessException {
		Volume volume = volumeMapper.selectByPrimaryKey(volumeId);
		if(null == volume)
			volume = volumeService.refreshVolumeInfo(volumeId, ostoken);
		volume.setInstanceId(null);
//		if (null != volume) {
//			volume.setInstanceId(null);
//		} else {
//			volume = volumeService.getVolume(volumeId, ostoken, response);
//		}
		volumeMapper.updateByPrimaryKeySelective(volume);
		
		StringBuilder sb = new StringBuilder();
		sb.append(ParamConstant.INSTANCE);
		sb.append(":");
		sb.append(instanceId);
		updateSyncResourceInfo(ostoken.getTenantid(),volume.getId(),sb.toString(),volume.getStatus(),ParamConstant.AVAILABLE,ParamConstant.VOLUME,ostoken.getCurrentRegion(),volume.getName());
		
		Instance instance = instanceMapper.selectByPrimaryKey(instanceId);
		String volumeIds = instance.getVolumeIds();
		instance.setVolumeIds(Util.listToString(Util.getCorrectedIdInfo(volumeIds, volumeId),','));
		instanceMapper.updateByPrimaryKeySelective(instance);
//		if (!Util.isNullOrEmptyValue(volumeIds)) {
//			List<String> volumeIdList = Util.stringToList(volumeIds, ",");
//			List<String> volumeIdListWithoutDetach = new ArrayList<String>();
//			for (String id : volumeIdList) {
//				if (id.equals(volumeId))
//					continue;
//				volumeIdListWithoutDetach.add(id);
//			}
//			instance.setVolumeIds(Util.listToString(volumeIdListWithoutDetach, ','));
//			instanceMapper.updateByPrimaryKeySelective(instance);
//		}
	}

	private void updateSyncResourceInfo(String tenantId,String id,String orgStatus,String expectedStatus,String type,String region,String name){
		updateSyncResourceInfo(tenantId,id,null,orgStatus,expectedStatus,type,region,name);
//		SyncResource resource = new SyncResource();
//		resource.setId(id);
//		resource.setType(type);
//		resource.setExpectedStatus(status);
//		syncResourceMapper.insertSelective(resource);
	}
	
	private void updateSyncResourceInfo(String tenantId,String id,String relatedResource,String orgStatus,String expectedStatus,String type,String region,String name){
		SyncResource resource = new SyncResource();
		resource.setId(id);
		resource.setType(type);
		resource.setOrgStatus(orgStatus);
		resource.setExpectedStatus(expectedStatus);
		resource.setRelatedResource(relatedResource);
		resource.setRegion(region);
		syncResourceMapper.insertSelective(resource);
		
		updateResourceCreateProcessInfo(tenantId,id,type,name);
	}
	
	private void updateResourceCreateProcessInfo(String tenantId,String id,String type,String name){
		ResourceCreateProcess createProcess = new ResourceCreateProcess();
		createProcess.setId(id);
		createProcess.setTenantId(tenantId);
		createProcess.setType(type);
		createProcess.setName(name);
		createProcess.setBegineSeconds(Util.getCurrentMillionsecond());
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
//	private void normaleVolumeTypeInfo(List<Volume> volumes,Locale locale){
//		for(Volume volume : volumes){
//			volume.setVolumeTypeName(Message.getMessage(volume.getVolume_type().toUpperCase(),locale,false));
//		}
//	}
	
	private String getUserdata(String systemType, String username,String password){
		StringBuilder sb = new StringBuilder();
		sb.append("#cloud-config\n");
		sb.append("chpasswd:\n");
		sb.append(" list: |\n");
		sb.append("   ");
		sb.append(username);
		sb.append(":");
		sb.append(password);
		sb.append("\n");
		sb.append(" expire: False\n");
		sb.append("runcmd:\n");
		sb.append("- sed -i 's/PasswordAuthentication no/PasswordAuthentication yes/g' /etc/ssh/sshd_config \n");
		if (ParamConstant.UBUNTU.equalsIgnoreCase(systemType)){
			sb.append("- sed -i 's/PermitRootLogin without-password/PermitRootLogin yes/g' /etc/ssh/sshd_config \n");
			sb.append("- cp -f /home/ubuntu/.ssh/authorized_keys /root/.ssh/ \n");
			sb.append("- service ssh restart \n");
		}else if(ParamConstant.CENTOS.equalsIgnoreCase(systemType)){
			sb.append("- systemctl restart sshd \n");
		}else{
			sb.append("- systemctl restart sshd \n");
		}
		String userdata = sb.toString();
		try {
			byte[] b = userdata.getBytes("utf-8");
			userdata = new BASE64Encoder().encode(b);
		} catch (UnsupportedEncodingException e) {
			userdata = null;
			// TODO Auto-generated catch block
			log.error(e);
		}  
		return userdata;
	}
	
	
	private void checkRelatedResource(String id,Locale locale) throws BusinessException{
		Instance instanceFromDB = instanceMapper.selectByPrimaryKey(id);
		if(null == instanceFromDB)
			return;
		String volumesId = instanceFromDB.getVolumeIds();
		if(!Util.isNullOrEmptyValue(volumesId))
			throw new ResourceBusinessException(Message.CS_HAVE_RELATED_VOLUME_RESOURCE,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);

		String lbId = instanceFromDB.getLbid();
		if(!Util.isNullOrEmptyValue(lbId))
			throw new ResourceBusinessException(Message.CS_HAVE_RELATED_LB_RESOURCE,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
		
		String imageId = instanceFromDB.getImageIds();
		if(!Util.isNullOrEmptyValue(imageId))
			throw new ResourceBusinessException(Message.CS_HAVE_RELATED_IMAGE_RESOURCE,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
		
		String portId = instanceFromDB.getPortIds();
		if(!Util.isNullOrEmptyValue(portId))
			throw new ResourceBusinessException(Message.CS_HAVE_RELATED_PORT_RESOURCE,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);

//		String securityGroupId = instanceFromDB.getSecurityGroupIds();
//		if(!Util.isNullOrEmptyValue(securityGroupId))
//			throw new ResourceBusinessException(Message.CS_HAVE_RELATED_SECURITYGROUP_RESOURCE,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);

		return;
	}
	
	private void checkBindedResource(String action, String body, Locale locale) throws BusinessException {
		if (action.equals(ParamConstant.ADD_FLOATINGIP_ACTION)) {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = null;
			try {
				rootNode = mapper.readTree(body);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_ADD_FLOATINGIP_FAILED,
						ParamConstant.BAD_REQUEST_RESPONSE_CODE, locale);
			}
			JsonNode floatingIPNode = rootNode.path(ParamConstant.ADD_FLOATINGIP_ACTION);
			String floatingIPAddress = floatingIPNode.path(ResponseConstant.ADDRESS).textValue();
			FloatingIP floatingIP = floatingIpMapper.selectByFloatingIp(floatingIPAddress);
			if (null != syncResourceMapper.selectByPrimaryKey(floatingIP.getId()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_IS_DOING,
						ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE, locale);
		} else if (action.equals(ParamConstant.REMOVE_FLOATINGIP_ACTION)) {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = null;
			try {
				rootNode = mapper.readTree(body);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_ADD_FLOATINGIP_FAILED,
						ParamConstant.BAD_REQUEST_RESPONSE_CODE, locale);
			}
			JsonNode floatingIPNode = rootNode.path(ParamConstant.REMOVE_FLOATINGIP_ACTION);
			String floatingIPAddress = floatingIPNode.path(ResponseConstant.ADDRESS).textValue();
			FloatingIP floatingIP = floatingIpMapper.selectByFloatingIp(floatingIPAddress);
			if (null != syncResourceMapper.selectByPrimaryKey(floatingIP.getId()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_IS_DOING,
						ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE, locale);
		} 
	}
	
	private void checkResource(String id,Locale locale) throws BusinessException{
		//check instance sync resource
		SyncResource syncResource = syncResourceMapper.selectByPrimaryKey(id);
		if(null != syncResource)
			throw new ResourceBusinessException(Message.CS_RESOURCE_IS_DOING,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
		
		Instance instanceFromDB = instanceMapper.selectByPrimaryKey(id);
		if(null == instanceFromDB)
			return;
		String volumesId = instanceFromDB.getVolumeIds();
		if(!Util.isNullOrEmptyValue(volumesId)){
			//check volume sync resource
			String[] ids = volumesId.split(",");
			for(int index = 0; index < ids.length; ++index){
				if(null != syncResourceMapper.selectByPrimaryKey(id))
					throw new ResourceBusinessException(Message.CS_VOLUME_IS_DOING,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
			}	
		}
		
		String portsId = instanceFromDB.getPortIds();
		if(!Util.isNullOrEmptyValue(portsId)){
			//check port sync resource
			String[] ids = portsId.split(",");
			for(int index = 0; index < ids.length; ++index){
				if(null != syncResourceMapper.selectByPrimaryKey(id))
					throw new ResourceBusinessException(Message.CS_PORT_IS_DOING,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
			}	
		}
		
		String floatingIps = instanceFromDB.getFloatingips();
		if(!Util.isNullOrEmptyValue(floatingIps)){
			//check port sync resource
			List<FloatingIP> floatingsIP = floatingIpMapper.selectListByFloatingIps(Util.stringToList(floatingIps, ","));
			for(FloatingIP floatingIP : floatingsIP){
				if(null != syncResourceMapper.selectByPrimaryKey(floatingIP.getId()))
					throw new ResourceBusinessException(Message.CS_FLOATING_IS_DOING,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
			}	
		}
		
		String imagesId = instanceFromDB.getImageIds();
		if(!Util.isNullOrEmptyValue(imagesId)){
			//check image sync resource
			String[] ids = imagesId.split(",");
			for(int index = 0; index < ids.length; ++index){
				if(null != syncResourceMapper.selectByPrimaryKey(id))
					throw new ResourceBusinessException(Message.CS_IMAGE_IS_DOING,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
			}	
		}
	}
	
	private void checkPortResource(String portId,String instanceId,Boolean attach,Locale locale) throws BusinessException{
		//check instance sync resource
		SyncResource syncResource = syncResourceMapper.selectByPrimaryKey(portId);
		if(null != syncResource)
			throw new ResourceBusinessException(Message.CS_RESOURCE_IS_DOING,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
		
		Port port = portMapper.selectByPrimaryKey(portId);
	    if(null == port)
	    	return;
	    if(false == attach){
	    	String deviceId = port.getDevice_id();
		    Router router = routerMapper.selectByPrimaryKey(deviceId);
		    if(null != router){
				throw new ResourceBusinessException(Message.CS_HAVE_RELATED_ROUTER_RESOURCE_ATTACH_WITH_PORT,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);   	
		    }
		    return;
	    }
	    
	    String networkId = port.getNetwork_id();
		if(Util.isNullOrEmptyValue(networkId))
			return;
		Instance instance = instanceMapper.selectByPrimaryKey(instanceId);
		if(null == instance)
			return;
//		if(null != instance.getPortIds() && instance.getPortIds().contains(portId))
//			throw new ResourceBusinessException(Message.CS_HAVE_RELATED_INSTANCE_RESOURCE_ATTACH_WITH_PORT,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
		if(null != instance.getNetworkIds() && !instance.getNetworkIds().contains(networkId))
			throw new ResourceBusinessException(Message.CS_NETID_IS_NOT_SAME,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
		return;
	}
	
	private void updateRelatedKeypairInfo(Instance instance){
		String keypairsId = instance.getKeypairIds();
		if(Util.isNullOrEmptyValue(keypairsId))
			return;
		List<Keypair> keypairs = keypairMapper.selectKeypairsById(keypairsId.split(","));
		if(Util.isNullOrEmptyList(keypairs))
			return;
		for(Keypair keypair : keypairs){
			keypair.setInstanceId(Util.listToString(Util.getCorrectedIdInfo(keypair.getInstanceId(), instance.getId()), ','));
		}
		keypairMapper.insertOrUpdateBatch(keypairs);
	}
	
	private void checkName(String name,TokenOs ostoken)  throws BusinessException{
		if(Util.isNullOrEmptyValue(name))
			return;
		List<Instance> instances = instanceMapper.selectListByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(instances))
			return;
		for(Instance instance : instances){
			if(name.equals(instance.getName()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_NAME_IS_SAME,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		return;
	}
	
	private void checkInfoForAddFloatingIP(String instanceId,String body,Locale locale) throws BusinessException{
		Instance instance = instanceMapper.selectByPrimaryKey(instanceId);
		
		if(!Util.isNullOrEmptyValue(instance.getFloatingips())){
			throw new ResourceBusinessException(Message.CS_INSTANCE_HAVE_FLOATINGIP,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		
		String networksId = instance.getNetworkIds();
		if(Util.isNullOrEmptyValue(networksId))
			throw new ResourceBusinessException(Message.CS_INSTANCE_WITHOUT_NET,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
        List<Network> networks = networkMapper.selectNetworksById(networksId.split(",")); 
		List<Router> routers = new ArrayList<Router>();
		
        for(Network network : networks){
			String subnetsId = network.getSubnetId();
			if(Util.isNullOrEmptyValue(subnetsId))
				continue;
			String[] ids = subnetsId.split(",");
			for(int index = 0; index < ids.length; ++index){
				List<Router> bindingRouters = routerMapper.selectBySubnetId(ids[index]);
				if(!Util.isNullOrEmptyList(bindingRouters))
					routers.addAll(bindingRouters);
			}
		}
        if(Util.isNullOrEmptyList(routers))
			throw new ResourceBusinessException(Message.CS_INSTANCE_WITHOUT_ROUTER,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
        
        ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(body);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e);
			throw new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_ADD_FLOATINGIP_FAILED,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);

		} 
		JsonNode floatingIPNode = rootNode.path(ParamConstant.ADD_FLOATINGIP_ACTION);
		String floatingIPAddress = floatingIPNode.path(ResponseConstant.ADDRESS).textValue();
		
        Boolean externalGateway = false;
        Boolean sameExternalNet = false;
        
        for(Router router : routers){
        	if(Util.isNullOrEmptyValue(router.getGatewayId()))
        		continue;
        	Gateway gateway =  gatewayMapper.selectByPrimaryKey(router.getGatewayId());
        	FloatingIP floatingIP = floatingIpMapper.selectByFloatingIp(floatingIPAddress);
        	if(gateway.getNetwork_id().equals(floatingIP.getNetworkId())){
        		sameExternalNet = true;
        		externalGateway = true;	
        		break;
        	}
        	externalGateway = true;	
        }
        
        if(false == externalGateway)
			throw new ResourceBusinessException(Message.CS_INSTANCE_WITHOUT_ROUTER_EXTERNAL_GATEWAY,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);

        if(false == sameExternalNet)
			throw new ResourceBusinessException(Message.CS_EXTERNAL_NET_NOT_SAME,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
   
        return; 
	}
	
//	private void makeInstanceDefaultSecurityGroup(List<Instance> instancesFromDB,TokenOs ostoken){
//		try{
//			if(false == securityGroupService.hasDefaultSecurityGroup(ostoken.getTenantid())){
//				for(Instance instance : instancesFromDB){
//					if(ParamConstant.ACTIVE_STATUS.equalsIgnoreCase(instance.getStatus())){
//						String sgIds = instance.getSecurityGroupIds();
//						if(Util.isNullOrEmptyValue(sgIds))
//							continue;
//						if(-1 != sgIds.indexOf(",")) //it is not a new created instance
//							continue;
//						securityGroupService.makeTenantDefaultSecurityGroup(sgIds, ostoken);
//					}
//				}
//			}		
//		}catch (Exception e){
//			log.error(e);
//		}
//	}
	
	private void checkSystemDiskSize(Instance instanceCreateInfo,TokenOs ostoken) throws BusinessException{
		Image image = imageMapper.selectByPrimaryKey(instanceCreateInfo.getSourceId());
		if(null == image)
			throw new ResourceBusinessException(Message.CS_IMAGE_IS_MISSING,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		Integer minDisk = image.getMinDisk();
		if(null == minDisk)
			return;
		Integer diskSize = Integer.parseInt(instanceCreateInfo.getVolumeSize());
		if(diskSize < minDisk)
			throw new ResourceBusinessException(Message.CS_DISK_SIZE_IS_SMALL,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
	}
	
	private void checkSystemVolume(String id, Locale locale) throws BusinessException {
		Volume volume = volumeMapper.selectByPrimaryKey(id);
		if(null == volume)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		if(volume.getBootable() == true)
			throw new ResourceBusinessException(Message.CS_OPERATE_SYSTEM_VOLUME_FORBIDDEN,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
		return;
	}
	
	private void checkVMWareEnv(String id, Locale locale) throws BusinessException {
		Instance instance = instanceMapper.selectByPrimaryKey(id);
		if(null == instance)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		if(Util.isNullOrEmptyValue(instance.getSource()))
			return;
		if(!instance.getSource().equals(ParamConstant.OPENSTACK_ZONE))
			throw new ResourceBusinessException(Message.CS_OPERATE_VMWAREZONE_IS_FORBIDDEN,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
		return;
	}
	
	private void updateFloatingIPQuota(TokenOs ostoken,String externalNetId,Integer count){
		if(Util.isNullOrEmptyValue(externalNetId))
			return;
		Network network = networkMapper.selectByPrimaryKey(externalNetId);
		if(null == network)
			return;
		quotaService.updateQuota(network.getName().toUpperCase(),ostoken,true,count);
		resourceSpecService.updateResourceSpecQuota(network.getName().toUpperCase(),ParamConstant.FLOATINGIP,count,true);
		
		Map<String,Integer> resourceQuota = new HashMap<String,Integer>();
		resourceQuota.put(network.getName().toUpperCase(), count);
		poolService.updatePoolQuota(ostoken.getTenantid(), resourceQuota, true);
	}
	
	private void normalInstanceZoneName(Instance instance,TokenOs ostoken){
		String zone = instance.getAvailabilityZone();
		HostAggregate aggregate = hostAggregateMapper.selectByZoneName(zone);
		if(null != aggregate){
			instance.setAvailabilityZoneName(StringHelper.ncr2String(aggregate.getName()));
		}else{
			instance.setAvailabilityZoneName(Message.getMessage(instance.getAvailabilityZone().toUpperCase(), new Locale(ostoken.getLocale()),false));
		}
	}
	
	private void updatePrivateImageQuota(TokenOs ostoken,String type,boolean bAdd){
		quotaService.updateQuota(type,ostoken,bAdd,1);
		resourceSpecService.updateResourceSpecQuota(type,ParamConstant.IMAGE,1,bAdd);
	}
	
	private void makeVolumeSnapshotInfo(List<Volume> volumes){
		if(Util.isNullOrEmptyList(volumes))
			return;
		for(Volume volume : volumes){
			List<VolumeSnapshot> snapshots = volumeSnapshotMapper.selectByVolumeId(volume.getId());
			volume.setSnapshots(snapshots);
		}
	}
}
