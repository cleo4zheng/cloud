package com.cloud.cloudapi.service.common.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.CloudUserMapper;
import com.cloud.cloudapi.dao.common.ContainerMapper;
import com.cloud.cloudapi.dao.common.DBInstanceMapper;
import com.cloud.cloudapi.dao.common.DBUserMapper;
import com.cloud.cloudapi.dao.common.DatabaseMapper;
import com.cloud.cloudapi.dao.common.FirewallMapper;
import com.cloud.cloudapi.dao.common.FlavorMapper;
import com.cloud.cloudapi.dao.common.FloatingIPMapper;
import com.cloud.cloudapi.dao.common.HostMapper;
import com.cloud.cloudapi.dao.common.ImageMapper;
import com.cloud.cloudapi.dao.common.InstanceMapper;
import com.cloud.cloudapi.dao.common.KeypairMapper;
import com.cloud.cloudapi.dao.common.LoadbalancerMapper;
import com.cloud.cloudapi.dao.common.NetworkMapper;
import com.cloud.cloudapi.dao.common.NotificationListMapper;
import com.cloud.cloudapi.dao.common.NotificationMapper;
import com.cloud.cloudapi.dao.common.OperationLogMapper;
import com.cloud.cloudapi.dao.common.OperationResourceMapper;
import com.cloud.cloudapi.dao.common.PoolEntityMapper;
import com.cloud.cloudapi.dao.common.PoolStackMapper;
import com.cloud.cloudapi.dao.common.PortMapper;
import com.cloud.cloudapi.dao.common.RouterMapper;
import com.cloud.cloudapi.dao.common.SecurityGroupMapper;
import com.cloud.cloudapi.dao.common.SubnetMapper;
import com.cloud.cloudapi.dao.common.VPNMapper;
import com.cloud.cloudapi.dao.common.VolumeMapper;
import com.cloud.cloudapi.dao.common.VolumeTypeMapper;
import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Container;
import com.cloud.cloudapi.pojo.openstackapi.forgui.DBInstance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.DBUser;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Database;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Firewall;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Flavor;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FloatingIP;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Host;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Image;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Keypair;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Loadbalancer;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Network;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Notification;
import com.cloud.cloudapi.pojo.openstackapi.forgui.NotificationList;
import com.cloud.cloudapi.pojo.openstackapi.forgui.OperationLog;
import com.cloud.cloudapi.pojo.openstackapi.forgui.OperationResource;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PoolEntity;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PoolStack;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Port;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Router;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SecurityGroup;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Subnet;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VPN;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Volume;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeType;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.StringHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("operationLogService")
public class OperationLogServiceImpl implements OperationLogService {

	@Autowired
	private OperationLogMapper operationLogMapper;

	@Autowired
	private OperationResourceMapper operationResourceMapper;
	
	@Autowired
	private InstanceMapper instanceMapper;
	
	@Autowired
	private ImageMapper imageMapper;
	
	@Autowired
	private VolumeMapper volumeMapper;
	
	@Autowired
	private VolumeTypeMapper volumeTypeMapper;
	
	@Autowired
	private NetworkMapper networkMapper;
	
	@Autowired
	private SubnetMapper subnetMapper;
	
	@Autowired
	private FloatingIPMapper floatingIPMapper;
	
	@Autowired
	private RouterMapper routerMapper;
	
	@Autowired
	private HostMapper hostMapper;
	
	@Autowired
	private KeypairMapper keypairMapper;
	
	@Autowired
	private FlavorMapper flavorMapper;
	
	@Autowired
	private SecurityGroupMapper securityGroupMapper;
	
	@Autowired
	private FirewallMapper firewallMapper;
	
	@Autowired
	private PortMapper portMapper;
	
	@Autowired
	private LoadbalancerMapper loadbalancerMapper;
	
	@Autowired
	private VPNMapper vpnMapper;

	@Resource
	private NotificationMapper notificationMapper;
	
	@Autowired
	private NotificationListMapper notificationListMapper;
	
	@Resource
	private CloudUserMapper cloudUserMapper;
	
	@Resource
	private DatabaseMapper dbMapper;
	
	@Resource
	private DBUserMapper dbUserMapper;
	
	@Resource
	private DBInstanceMapper dbInstanceMapper;
	
	@Resource
	private ContainerMapper containerMapper;
	
	@Resource
	private PoolStackMapper poolStackMapper;
	
	@Resource
	private PoolEntityMapper poolEntityMapper;
	
	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(OperationLogServiceImpl.class);
	
	@Override
	public List<OperationLog> getOperationLogList(Map<String, String> paramMap, TokenOs ostoken) {

		int limitItems = Util.getLimit(paramMap);
		List<OperationLog> operations = getOperationLogsFromDB(ostoken.getTenantid(),limitItems);
		if(Util.isNullOrEmptyList(operations))
			return null;
		Locale locale = new Locale(ostoken.getLocale());
		List<OperationLog> operationsWithResource = new ArrayList<OperationLog>();
		for(OperationLog operationLog : operations){
			try{
				//List<OperationResource> resources = operationResourceMapper.selectResourcesByOperationId(operationLog.getId());
				appendOperationResources(operationLog);
				operationLog.setTimestamp(Util.millionSecond2Date(operationLog.getMillionSeconds()));
				//operationLog.setResources(resources);
				operationLog.setTitle(Message.getMessage(operationLog.getTitle(),locale,false));
				operationLog.setStatus(Message.getMessage(operationLog.getStatus(),locale,false));	
				operationLog.setTenantId(null);
				operationsWithResource.add(operationLog);
			}catch(Exception e){
				log.error(e);
			}
		}
		return operationsWithResource;
	}

	@Override
	public void deleteOperationLogs(String body,TokenOs ostoken){
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode;
		try {
			rootNode = mapper.readTree(body);
		} catch (Exception e) {
			log.error(e);
			return;
		} 
		JsonNode idsNode = rootNode.path(ResponseConstant.IDS);
		int idsCount = idsNode.size();
		List<String> ids = new ArrayList<String>();
		for (int index = 0; index < idsCount; ++index) {
			String id = idsNode.get(index).textValue();
			ids.add(id);
		}
		operationResourceMapper.deleteByOperationsId(ids);
		operationLogMapper.deleteByOperationsId(ids);
	}
	
	@Override
	public OperationLog getOperationLogDetail(String operationId,TokenOs ostoken){
		OperationLog operation = this.operationLogMapper.selectByPrimaryKey(operationId);
		if(null == operation)
			return null;
		Locale locale = new Locale(ostoken.getLocale());
		operation.setTitle(Message.getMessage(operation.getTitle(),locale,false));
		operation.setStatus(Message.getMessage(operation.getStatus(),locale,false));
		operation.setTimestamp(Util.millionSecond2Date(operation.getMillionSeconds()));
		appendOperationResources(operation);
		//List<OperationResource> resources = operationResourceMapper.selectResourcesByOperationId(operationId);
		//operation.setResources(resources);
		return operation;
	}
	
	private void appendOperationResources(OperationLog operationLog){
		String resourceId = operationLog.getResourcesId();
		if(Util.isNullOrEmptyValue(resourceId))
			return;
		switch (operationLog.getResourceType()) {
		case ParamConstant.INSTANCE:{
			List<String> ids = Util.stringToList(resourceId, ",");
			List<Instance> instances = instanceMapper.selectListByInstanceIds(ids);
			if(null == instances)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(Instance instance : instances){
				OperationResource resource = new OperationResource();
				resource.setId(instance.getId());
				resource.setName(StringHelper.ncr2String(instance.getName()));
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.IMAGE:{
			List<Image> images = imageMapper.selectImagesById(resourceId.split(","));
			if(null == images)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(Image image : images){
				OperationResource resource = new OperationResource();
				resource.setId(image.getId());
				resource.setName(StringHelper.ncr2String(image.getName()));
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.VOLUME:{
			List<Volume> volumes = volumeMapper.selectVolumesById(resourceId.split(","));
			if(null == volumes)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(Volume volume : volumes){
				OperationResource resource = new OperationResource();
				resource.setId(volume.getId());
				resource.setName(StringHelper.ncr2String(volume.getName()));
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.VOLUMETYPE:{
			List<VolumeType> volumeTypes = volumeTypeMapper.selectVolumeTypesById(resourceId.split(","));
			if(null == volumeTypes)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(VolumeType volumeType : volumeTypes){
				OperationResource resource = new OperationResource();
				resource.setId(volumeType.getId());
				resource.setName(volumeType.getName());
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;	
		}
		case ParamConstant.NETWORK:{
			List<Network> networks = networkMapper.selectNetworksById(resourceId.split(","));
			if(null == networks)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(Network network : networks){
				OperationResource resource = new OperationResource();
				resource.setId(network.getId());
				resource.setName(StringHelper.ncr2String(network.getName()));
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;	
		}
		case ParamConstant.SUBNET:{
			List<String> ids = Util.stringToList(resourceId, ",");
			List<Subnet> subnets = subnetMapper.selectListBySubnetIds(ids);
			if(null == subnets)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(Subnet subnet : subnets){
				OperationResource resource = new OperationResource();
				resource.setId(subnet.getId());
				resource.setName(StringHelper.ncr2String(subnet.getName()));
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.FLOATINGIP:{
			List<FloatingIP> ips = floatingIPMapper.selectByIds(resourceId.split(","));
			if(null == ips)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(FloatingIP ip : ips){
				OperationResource resource = new OperationResource();
				resource.setId(ip.getId());
				resource.setName(StringHelper.ncr2String(ip.getName()));
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.ROUTER:{
			List<Router> routers = routerMapper.selectByIds(resourceId.split(","));
			if(null == routers)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(Router router : routers){
				OperationResource resource = new OperationResource();
				resource.setId(router.getId());
				resource.setName(StringHelper.ncr2String(router.getName()));
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.PORT:{
			List<Port> ports = portMapper.selectPortsById(resourceId.split(","));
			if(null == ports)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(Port port : ports){
				OperationResource resource = new OperationResource();
				resource.setId(port.getId());
				resource.setName(StringHelper.ncr2String(port.getName()));
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.HOST:{
			List<Host> hosts = hostMapper.selectByIds(resourceId.split(","));
			if(null == hosts)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(Host host : hosts){
				OperationResource resource = new OperationResource();
				resource.setId(host.getId());
				resource.setName(host.getHostName());
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.KEYPAIR:{
			//List<Keypair> keypairs = keypairMapper.selectKeypairsById(resourceId.split(","));
			List<Keypair> keypairs = keypairMapper.selectKeypairsByName(resourceId.split(","));
			if(null == keypairs)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(Keypair keypair : keypairs){
				OperationResource resource = new OperationResource();
				resource.setId(keypair.getId());
				resource.setName(keypair.getName());
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.FLAVOR:{
			List<Flavor> flavors = flavorMapper.selectByIds(resourceId.split(","));
			if(null == flavors)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(Flavor flavor : flavors){
				OperationResource resource = new OperationResource();
				resource.setId(flavor.getId());
				resource.setName(flavor.getName());
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.SECURITYGROUP:{
			List<SecurityGroup> sgs = securityGroupMapper.selectSecurityGroupsById(resourceId.split(","));
			if(null == sgs)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(SecurityGroup sg : sgs){
				OperationResource resource = new OperationResource();
				resource.setId(sg.getId());
				resource.setName(StringHelper.ncr2String(sg.getName()));
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.FIREWALL:{
			List<Firewall> fws = firewallMapper.selectByIds(resourceId.split(","));
			if(null == fws)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(Firewall fw : fws){
				OperationResource resource = new OperationResource();
				resource.setId(fw.getId());
				resource.setName(StringHelper.ncr2String(fw.getName()));
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.LOADBALANCER:{
			List<Loadbalancer> lbs = loadbalancerMapper.selectByIds(resourceId.split(","));
			if(null == lbs)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(Loadbalancer lb : lbs){
				OperationResource resource = new OperationResource();
				resource.setId(lb.getId());
				resource.setName(StringHelper.ncr2String(lb.getName()));
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.VPN:{
			List<VPN> vpns = vpnMapper.selectByIds(resourceId.split(","));
			if(null == vpns)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(VPN vpn : vpns){
				OperationResource resource = new OperationResource();
				resource.setId(vpn.getId());
				resource.setName(StringHelper.ncr2String(vpn.getName()));
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.NOTIFICATIONLIST:{
			List<NotificationList> notifications = notificationListMapper.selectByIds(resourceId.split(","));
			if(null == notifications)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(NotificationList notification : notifications){
				OperationResource resource = new OperationResource();
				resource.setId(notification.getId());
				resource.setName(notification.getName());
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.NOTIFICATION:{
			List<Notification> notifications = notificationMapper.selectByIds(resourceId.split(","));
			if(null == notifications)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(Notification notification : notifications){
				OperationResource resource = new OperationResource();
				resource.setId(notification.getId());
				resource.setName(notification.getName());
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.CONTAINER:{
			List<Container> containers = containerMapper.selectByIds(resourceId.split(","));
			if(null == containers)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(Container container : containers){
				OperationResource resource = new OperationResource();
				resource.setId(container.getUuid());
				resource.setName(container.getName());
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.DATABASEINSTANCE:{
			List<DBInstance> instances = dbInstanceMapper.selectByIds(resourceId.split(","));
			if(null == instances)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(DBInstance instance : instances){
				OperationResource resource = new OperationResource();
				resource.setId(instance.getId());
				resource.setName(StringHelper.ncr2String(instance.getName()));
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.DATABASEINSTANCE_USER:{
			List<DBUser> users = dbUserMapper.selectByIds(resourceId.split(","));
			if(null == users)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(DBUser user : users){
				OperationResource resource = new OperationResource();
				resource.setId(user.getId());
				resource.setName(user.getName());
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.DATABASE:{
			List<Database> dbs = dbMapper.selectByIds(resourceId.split(","));
			if(null == dbs)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(Database db : dbs){
				OperationResource resource = new OperationResource();
				resource.setId(db.getId());
				resource.setName(StringHelper.ncr2String(db.getName()));
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.STACK:{
			List<PoolStack> stacks = poolStackMapper.selectByIds(resourceId.split(","));
			if(null == stacks)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(PoolStack stack : stacks){
				OperationResource resource = new OperationResource();
				resource.setId(stack.getId());
				resource.setName(StringHelper.ncr2String(stack.getName()));
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		case ParamConstant.POOL:{
			List<PoolEntity> pools = poolEntityMapper.selectByIds(resourceId.split(","));
			if(null == pools)
				break;
			List<OperationResource> resources = new ArrayList<OperationResource>();
			for(PoolEntity pool : pools){
				OperationResource resource = new OperationResource();
				resource.setId(pool.getId());
				resource.setName(StringHelper.ncr2String(pool.getName()));
				resource.setType(operationLog.getResourceType());
				resources.add(resource);
			}
			operationLog.setResources(resources);
			break;
		}
		default:
			break;
		}
	}
	
//	private void addInstancesResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			Instance instance = instanceMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == instance)
//				continue; //TODO get it from openstack
//			OperationResource opetationResource = new OperationResource(instance.getId(),StringHelper.ncr2String(instance.getName()),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//		}
//	}
//	
//	private void addImagesResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			Image image = imageMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == image)
//				continue; //TODO get it from openstack
//			OperationResource opetationResource = new OperationResource(image.getId(),StringHelper.ncr2String(image.getName()),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);	
//		}
//	}
//	
//	private void addVolumesResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			Volume volume = volumeMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == volume)
//				continue; //TODO get it from openstack
//			OperationResource opetationResource = new OperationResource(volume.getId(),StringHelper.ncr2String(volume.getName()),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//			
//		}
//	}
//	
//	private void addVolumeTypesResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			VolumeType volumeType = volumeTypeMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == volumeType)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(volumeType.getId(),volumeType.getName(),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//			
//		}
//	}
//	
//	private void addNetworksResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			Network network = networkMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == network)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(network.getId(),StringHelper.ncr2String(network.getName()),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//			
//		}
//	}
//	
//	private void addSubnetsResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			Subnet subnet = subnetMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == subnet)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(subnet.getId(),StringHelper.ncr2String(subnet.getName()),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//			
//		}
//	}
//	
//	private void addFloatingIPsResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			FloatingIP floatingIP = floatingIPMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == floatingIP)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(floatingIP.getId(),StringHelper.ncr2String(floatingIP.getName()),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//			
//		}
//	}
//	
//	private void addRoutersResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			Router router = routerMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == router)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(router.getId(),StringHelper.ncr2String(router.getName()),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//			
//		}
//	}
//	
//	private void addHostsResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			Host host = hostMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == host)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(host.getId(),host.getHostName(),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//			
//		}
//	}
//	
//	private void addKeypairsResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			Keypair keypair = keypairMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == keypair)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(keypair.getId(),keypair.getName(),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//			
//		}
//	}
//	
//	private void addFlavorsResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			Flavor flavor = flavorMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == flavor)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(flavor.getId(),flavor.getName(),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//			
//		}
//	}
//	
//	private void addSecurityGroupsResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			SecurityGroup securityGroup = securityGroupMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == securityGroup)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(securityGroup.getId(),StringHelper.ncr2String(securityGroup.getName()),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//		}
//	}
//	
//	private void addFirewallsResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			Firewall firewall = firewallMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == firewall)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(firewall.getId(),StringHelper.ncr2String(firewall.getName()),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//		}
//	}
//	
//	private void addPortsResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			Port port = portMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == port)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(port.getId(),StringHelper.ncr2String(port.getName()),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//		}
//	}
//	
//	private void addLoadbalancersResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			Loadbalancer loadbalancer = loadbalancerMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == loadbalancer)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(loadbalancer.getId(),StringHelper.ncr2String(loadbalancer.getName()),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//		}
//	}
//	
//	private void addVPNsResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			VPN vpn = vpnMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == vpn)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(vpn.getId(),StringHelper.ncr2String(vpn.getName()),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//		}
//	}
//	
//	private void addNotificationsResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			NotificationList notificationlist = notificationListMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == notificationlist)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(notificationlist.getId(),notificationlist.getName(),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//		}
//	}
//	
//	private void addNotificationListsResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			Notification notification = notificationMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == notification)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(notification.getId(),notification.getName(),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//		}
//	}
//	
//	private void addDatabaseResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			Database notification = dbMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == notification)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(notification.getId(),StringHelper.ncr2String(notification.getName()),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//		}
//	}
//	
//	private void addDBUserResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			DBUser notification = dbUserMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == notification)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(notification.getId(),notification.getName(),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//		}
//	}
//	
//	private void addDBInstanceResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			DBInstance notification = dbInstanceMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == notification)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(notification.getId(),StringHelper.ncr2String(notification.getName()),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//		}
//	}
//	
//	private void addContainerResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			Container notification = containerMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == notification)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(notification.getUuid(),StringHelper.ncr2String(notification.getName()),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//		}
//	}
//	
//	private void addStacksResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			PoolStack stack = poolStackMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == stack)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(stack.getId(),stack.getName(),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//		}
//	}
//	
//	private void addPoolsResource(String resourcesId,String type,String operationId){
//		String[] resourceIdArray = resourcesId.split(",");
//		for(int i = 0; i < resourceIdArray.length; ++i){
//			PoolEntity pool = poolEntityMapper.selectByPrimaryKey(resourceIdArray[i]);
//			if(null == pool)
//				continue; //TODO gei it from openstack
//			OperationResource opetationResource = new OperationResource(pool.getId(),pool.getName(),type,operationId);
//			operationResourceMapper.insertOrUpdate(opetationResource);
//		}
//	}
	
//	private void addOperationResource(String type, String resourcesId,String operationId) {
//		switch (type) {
//		case ParamConstant.INSTANCE:
//            addInstancesResource(resourcesId,type,operationId);
//			break;
//		case ParamConstant.IMAGE:
//			addImagesResource(resourcesId,type,operationId);
//			break;
//		case ParamConstant.VOLUME:
//			addVolumesResource(resourcesId,type,operationId);
//			break;
//		case ParamConstant.VOLUMETYPE:
//			addVolumeTypesResource(resourcesId,type,operationId);
//			break;	
//		case ParamConstant.NETWORK:
//			addNetworksResource(resourcesId,type,operationId);
//			break;
//		case ParamConstant.SUBNET:
//			addSubnetsResource(resourcesId,type,operationId);
//			break;
//		case ParamConstant.FLOATINGIP:
//			addFloatingIPsResource(resourcesId,type,operationId);
//			break;
//		case ParamConstant.ROUTER:
//			addRoutersResource(resourcesId,type,operationId);
//			break;
//		case ParamConstant.PORT:
//			addPortsResource(resourcesId,type,operationId);
//			break;
//		case ParamConstant.HOST:
//			addHostsResource(resourcesId,type,operationId);
//			break;
//		case ParamConstant.KEYPAIR:
//			addKeypairsResource(resourcesId,type,operationId);
//			break;
//		case ParamConstant.FLAVOR:
//			addFlavorsResource(resourcesId,type,operationId);
//			break;
//		case ParamConstant.SECURITYGROUP:
//			addSecurityGroupsResource(resourcesId,type,operationId);
//			break;
//		case ParamConstant.FIREWALL:
//			addFirewallsResource(resourcesId,type,operationId);
//			break;
//		case ParamConstant.LOADBALANCER:
//			addLoadbalancersResource(resourcesId,type,operationId);
//			break;
//		case ParamConstant.VPN:
//			addVPNsResource(resourcesId,type,operationId);
//			break;
//		case ParamConstant.NOTIFICATION:
//			addNotificationsResource(resourcesId,type,operationId);
//			break;
//		case ParamConstant.NOTIFICATIONLIST:
//			addNotificationListsResource(resourcesId,type,operationId);
//			break;
//		case ParamConstant.CONTAINER:
//			addContainerResource(resourcesId, type, operationId);
//			break;
//		case ParamConstant.DATABASEINSTANCE:
//			addDBInstanceResource(resourcesId, type, operationId);
//			break;
//		case ParamConstant.DATABASEINSTANCE_USER:
//			addDBUserResource(resourcesId, type, operationId);
//			break;
//		case ParamConstant.DATABASE:
//			addDatabaseResource(resourcesId, type, operationId);
//			break;
//		case ParamConstant.STACK:
//			addStacksResource(resourcesId, type, operationId);
//			break;
//		case ParamConstant.POOL:
//			addPoolsResource(resourcesId, type, operationId);
//			break;
//		default:
//			break;
//		}
//	}

	@Override
	public void addOperationLog(String user, String tenantId,String title, String type, String resourcesId, String status,
			String details){
		OperationLog operationLog = new OperationLog();
		CloudUser cloudUser = cloudUserMapper.selectByAccount(user);
		if(null == cloudUser)
			return;
		operationLog.setUser(user);
		operationLog.setId(Util.makeUUID());
		operationLog.setTenantId(tenantId);
		operationLog.setTimestamp(Util.getCurrentDate());
		operationLog.setMillionSeconds(System.currentTimeMillis());
		operationLog.setResourceType(type);
		operationLog.setTitle(title);
		operationLog.setStatus(status);
	//	operationLog.setTitle(Message.getMessage(title,locale,false));
	//	operationLog.setStatus(Message.getMessage(status,locale,false));
		operationLog.setResourcesId(resourcesId);
		operationLog.setDetails(details);
	//	addOperationResource(type, resourcesId,operationLog.getId());
		this.operationLogMapper.insertSelective(operationLog);
	}
	
	private List<OperationLog> getOperationLogsFromDB(String tenantId,int limitItems){
		List<OperationLog> operationLogsFromDB = null;
		if(-1 == limitItems){
			operationLogsFromDB = operationLogMapper.selectAll(tenantId);
		}else{
			operationLogsFromDB = operationLogMapper.selectListWithLimit(tenantId,limitItems);
		}
		return operationLogsFromDB;
	}
	
//	private List<OperationLog> getLimitItems(List<OperationLog> operationLogs,int limit){
//		if(null == operationLogs)
//			return null;
//		if(-1 != limit){
//			if(limit <= operationLogs.size())
//				return operationLogs.subList(0, limit);
//		}
//		return operationLogs;
//	}
}
