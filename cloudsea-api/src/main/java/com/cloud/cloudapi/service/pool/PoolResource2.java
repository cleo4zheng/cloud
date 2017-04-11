package com.cloud.cloudapi.service.pool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.cloud.cloudapi.dao.common.FirewallMapper;
import com.cloud.cloudapi.dao.common.FirewallRuleMapper;
import com.cloud.cloudapi.dao.common.FloatingIPMapper;
import com.cloud.cloudapi.dao.common.ImageMapper;
import com.cloud.cloudapi.dao.common.InstanceMapper;
import com.cloud.cloudapi.dao.common.LoadbalancerMapper;
import com.cloud.cloudapi.dao.common.LoadbalancerPoolMapper;
import com.cloud.cloudapi.dao.common.LoadbalancerPoolMemberMapper;
import com.cloud.cloudapi.dao.common.NetworkMapper;
import com.cloud.cloudapi.dao.common.PoolStackMapper;
import com.cloud.cloudapi.dao.common.QuotaDetailMapper;
import com.cloud.cloudapi.dao.common.ResourceEventMapper;
import com.cloud.cloudapi.dao.common.RouterMapper;
import com.cloud.cloudapi.dao.common.StackResourceMapper;
import com.cloud.cloudapi.dao.common.SubnetMapper;
import com.cloud.cloudapi.dao.common.SyncResourceMapper;
import com.cloud.cloudapi.dao.common.VolumeMapper;
import com.cloud.cloudapi.dao.common.VolumeTypeMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.StackConfig;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FloatingIPConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Image;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PoolConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PoolEntity;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PoolStack;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceEvent;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceSpec;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Stack;
import com.cloud.cloudapi.pojo.openstackapi.forgui.StackResource;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SyncResource;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Volume;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeType;
import com.cloud.cloudapi.service.businessapi.zabbix.ZabbixService;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.ConfigService;
import com.cloud.cloudapi.service.common.MonitorService;
import com.cloud.cloudapi.service.openstackapi.FirewallService;
import com.cloud.cloudapi.service.openstackapi.FlavorService;
import com.cloud.cloudapi.service.openstackapi.FloatingIPService;
import com.cloud.cloudapi.service.openstackapi.InstanceService;
import com.cloud.cloudapi.service.openstackapi.LoadbalancerService;
import com.cloud.cloudapi.service.openstackapi.NetworkService;
import com.cloud.cloudapi.service.openstackapi.PoolEntityService;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.service.openstackapi.ResourceSpecService;
import com.cloud.cloudapi.service.openstackapi.RouterService;
import com.cloud.cloudapi.service.openstackapi.StackService;
import com.cloud.cloudapi.service.openstackapi.VolumeService;
import com.cloud.cloudapi.service.openstackapi.VolumeTypeService;
import com.cloud.cloudapi.service.pool.resource.NeutronNet;
import com.cloud.cloudapi.service.pool.resource.NeutronSubnet;
import com.cloud.cloudapi.service.pool.resource.NovaServer;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PoolResource2 implements PoolResourceService {
	@Resource
	private PoolEntityService poolEntityService;

	@Resource
	private FlavorService flavorService;

	@Resource
	private StackService stackService;

	@Resource
	private PoolStackMapper poolStackMapper;

	@Resource
	private AuthService authService;

	@Resource
	private QuotaDetailMapper quotaDetailMapper;

	@Resource
	private QuotaService quotaService;

	@Autowired
	private CloudConfig cloudconfig;

	@Resource
	private InstanceService instanceService;

	@Resource
	private VolumeTypeService volumeTypeService;
	
	@Resource
	private InstanceMapper instanceMapper;

	@Resource
	private ZabbixService zabbixService;

	@Resource
	private NetworkService networkService;

	@Resource
	private RouterService routerService;

	@Resource
	private NetworkMapper networkMapper;

	@Resource
	private SubnetMapper subnetMapper;

	@Resource
	private RouterMapper routerMapper;
	
	@Resource
	private FloatingIPService floatingIPService;
	
	@Resource
	private LoadbalancerService loadbalancerService;
	
	@Resource
	private FirewallService firewallService;
	
	@Resource
	private VolumeService volumeService;
	
	@Resource
	private ConfigService configService;
	
	@Resource
	private VolumeMapper volumeMapper;
	
	@Resource
	private LoadbalancerMapper loadbalancerMapper;
	
	@Resource
	private LoadbalancerPoolMapper loadbalancerPoolMapper;
	
	@Resource
	private LoadbalancerPoolMemberMapper loadbalancerPoolMemberMapper;
	
	@Resource
	private FloatingIPMapper floatingIPMapper;
	
	@Resource
	private FirewallMapper firewallMapper;
	
	@Resource
	private FirewallRuleMapper firewallRuleMapper;
	
	@Resource
	private ResourceSpecService resourceSpecService;
	
	@Autowired
    private SyncResourceMapper syncResourceMapper;
	
	@Autowired
    private StackResourceMapper stackResourceMapper;
	
	@Autowired
	private MonitorService monitorService;
	
	@Autowired
	private ImageMapper imageMapper;
	
	@Autowired
	private ResourceEventMapper resourceEventMapper;
	
	@Autowired
	private VolumeTypeMapper volumeTypeMapper;
	
	private Logger log = LogManager.getLogger(PoolResource.class);

	@Override
	@SuppressWarnings("unchecked")
	public Stack create(Map<String, Object> params, TokenOs ostoken, HttpServletResponse response) throws Exception {
		String tenantid = ostoken.getTenantid();
		PoolEntity pool = this.poolEntityService.getPoolEntityByTenantId(tenantid);
		if (pool == null) {
			throw new ResourceBusinessException(Message.CS_POOL_NOT_EXISTS_ERROR, ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		String stackName = (String) params.get("stackName");
		if (!this.validatePoolStackName(stackName, pool.getId())) {
			throw new ResourceBusinessException(Message.CS_TEMPLATE_ALREADY_EXISTS_ERROR, ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		
		Map<String, Object> yamlMap = this.initYAML();
		Map<String, Object> resourcesMap = new LinkedHashMap<String, Object>();

		ObjectMapper mapper = new ObjectMapper();

		Map<String, Integer> tFips = null;
		Map<String, Integer> tVols = null;
		Map<String, Integer> uFips = null;
		Map<String, Integer> uVols = null;
		Map<String, Integer> tCpus = null;
		Map<String, Integer> tMems = null;
		Map<String, Integer> uCpus = null;
		Map<String, Integer> uMems = null;
		try {
			tFips = pool.gettFips() != null ? mapper.readValue(pool.gettFips(), new TypeReference<HashMap<String, Integer>>() {
			}) : null;
			tVols = pool.gettVolumes() != null ? mapper.readValue(pool.gettVolumes(), new TypeReference<HashMap<String, Integer>>() {
			}) : null;
			uFips = pool.getuFips() != null ? mapper.readValue(pool.getuFips(), new TypeReference<HashMap<String, Integer>>() {
			}) : null;
			uVols = pool.getuVolumes() != null ? mapper.readValue(pool.getuVolumes(), new TypeReference<HashMap<String, Integer>>() {
			}) : null;
			tCpus = pool.gettCpus() != null ? mapper.readValue(pool.gettCpus(), new TypeReference<HashMap<String, Integer>>() {
			}) : null;
			tMems = pool.gettMems() != null ? mapper.readValue(pool.gettMems(), new TypeReference<HashMap<String, Integer>>() {
			}) : null;
			uCpus = pool.getuCpus() != null ? mapper.readValue(pool.getuCpus(), new TypeReference<HashMap<String, Integer>>() {
			}) : null;
			uMems = pool.getuMems() != null ? mapper.readValue(pool.getuMems(), new TypeReference<HashMap<String, Integer>>() {
			}) : null;
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE, new Locale(ostoken.getLocale()));
		}

		int cpusTotal = 0;
		int memsTotal = 0;
		Map<String, Integer> fipTotal = new LinkedHashMap<String, Integer>();
		Map<String, Integer> volTotal = new LinkedHashMap<String, Integer>();

		List<Map<String, Object>> netInstance = (List<Map<String, Object>>) params.get("netInstance");
		Map<String, String> instanceNet = new HashMap<String, String>();
		for (Map<String, Object> ni : netInstance) {
			String name = (String) ni.get("name");
			List<String> instanceNames = (List<String>) ni.get("instance_names");
			for(String instance: instanceNames){
				instanceNet.put(instance, name);
			}
		}
		
//		List<Map<String, Object>> netFloatingIp = (List<Map<String, Object>>) params.get("netFloatingIp");
//		Map<String, String> netGateway = new HashMap<String, String>();
//		if (netFloatingIp != null && netFloatingIp.size() != 0) {
//			for (Map<String, Object> nFi : netFloatingIp) {
//				String floatingIpType = (String) nFi.get("floatingIpType");
//				List<String> instanceNames = (List<String>) nFi.get("instance_names");
//				for(String instance: instanceNames){
//					String netname = instanceNet.get(instance);
//					netGateway.put(netname, floatingIpType);
//				}
//			}
//		}

		// *********** NETWORK ***********
		Map<String, NeutronSubnet> netSubnetMap = new HashMap<String, NeutronSubnet>();
//		List<String> routers = new ArrayList<String>();
//		List<String> routerInterfacesList = new ArrayList<String>();
		List<Map<String, Object>> netWork = (List<Map<String, Object>>) params.get("network");
		for (Map<String, Object> net : netWork) {
			String name = (String) net.get("name");
			String cidr = (String) net.get("CIDR");
			String ipVirsion = (String) net.get("ipVersion");
			int ipV = 4;
			if (ipVirsion.equals("IP v6")) {
				ipV = 6;
			}
			String gateway = (String) net.get("gateway");
			Boolean dhcp = (Boolean) net.get("DHCP");
			NeutronNet neutronNet = new NeutronNet(name);
			NeutronSubnet neutronSubnet = new NeutronSubnet(neutronNet.getResourceName(), cidr, gateway, dhcp, ipV);
			resourcesMap.putAll(neutronNet.getResourceMap());
			resourcesMap.putAll(neutronSubnet.getResourceMap());
			// 
			netSubnetMap.put(name, neutronSubnet);
//			String pubNet = null;
//			if (netGateway.containsKey(name)) {
//				pubNet = netGateway.get(name);
//			}
//			String routerName = name + "_router";
//			String[] nameList = {name};
//			NeutronRouter neutronRouter = new NeutronRouter(routerName, Arrays.asList(nameList), pubNet);
//			routers.add(neutronRouter.getResourceName());
//			resourcesMap.putAll(neutronRouter.getResourceMap());
//			routerInterfacesList.addAll(neutronRouter.getRouterInterfaces());
		}
		
		FloatingIPConfig floatingipConfig = this.floatingIPService.getFloatingIPConfig2(ostoken);
		Map<String, String> fipIdName = new HashMap<String, String>();
		for (ResourceSpec ft : floatingipConfig.getTypes()) {
			fipIdName.put(ft.getId(), ft.getName());
		}
		
	
//		Map<String, List<NeutronFloatingIP>> netNovaFloatingIp = new HashMap<String, List<NeutronFloatingIP>>();
//		Map<String, NeutronFloatingIP> instanceFloatingIP = new HashMap<String , NeutronFloatingIP>();
//		int findex = 1;
//		if (netFloatingIp != null && netFloatingIp.size() != 0) {
//			for (Map<String, Object> nFi : netFloatingIp) {
//				String floatingIpType = (String) nFi.get("floatingIpType");
//				List<String> instanceNames = (List<String>) nFi.get("instance_names");
//				int count = instanceNames.size();
//				
//				List<NeutronFloatingIP> nfis = new ArrayList<NeutronFloatingIP>();
//				for (int i = 0; i < count; i++) {
//					String ftype = fipIdName.get(floatingIpType);
//					String instanceName = instanceNames.get(i);
//					NeutronFloatingIP nfi = new NeutronFloatingIP(findex, instanceName, ftype, floatingIpType,
//							routerInterfacesList);
//					nfis.add(nfi);
//					if (fipTotal.containsKey(ftype)) {
//						int fcount = fipTotal.get(ftype);
//						fipTotal.put(ftype, fcount + 1);
//					} else {
//						fipTotal.put(ftype, 1);
//					}
//					instanceFloatingIP.put(instanceName, nfi);
//					findex++;
//				}
//
//			}
//		}
		// *********** BULK SERVERS ***********
		int count = 0;
		
//		String sourceType = (String) params.get("sourceType");
		String image = (String) params.get("source");
		int cpus = (int) params.get("core");
		int mems_mb = (int) params.get("ram");
		int mems = mems_mb / 1024;
		int volume_size = (int) params.get("volumeSize");
//		String volume_type_id = (String) params.get("volumeType");
		//VolumeType vt = this.volumeTypeService.getVolumeType(volume_type_id, ostoken, response);
		//String volume_type = vt.getName();
		String volume_type = (String) params.get("volumeType");
		
		if (!tVols.containsKey(volume_type)) {
			throw new ResourceBusinessException(Message.CS_VOLTYPE_NOT_EXISTS_ERROR, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
//		String flavor = this.getFlavor(ostoken, cpus, mems_mb, response);
		Map<String, List<NovaServer>> netServerMap = new LinkedHashMap<String, List<NovaServer>>();
		int i_index = 1;
		//List<Map<String, Object>> netInstance = (List<Map<String, Object>>) params.get("netInstance");
//		List<NovaFloatingIpAssociation> nfiaList = new ArrayList<NovaFloatingIpAssociation>();
		String az = (String) params.get("availabilityZone");
		String keyName = (String) params.get("keypair");
		String username = (String) params.get("userName");
		String password = (String) params.get("password");
		String ostype = NovaServer.OS_LINUX;
		if (username.equals("Administrator")) {
			ostype = NovaServer.OS_WINDOWS;
		}
		String flavor = null;
		if (az.equals(cloudconfig.getSystemVmwareZone())) {
			flavor = this.flavorService.getFlavor(ostoken, cpus + "", mems_mb + "", volume_size + "", true);
		} else {
			flavor = this.flavorService.getFlavor(ostoken, cpus + "", mems_mb + "", volume_size + "", false);
		}
		for (Map<String, Object> ni : netInstance) {
			String name = (String) ni.get("name");
			List<String> instanceNames = (List<String>) ni.get("instance_names");
			int number = instanceNames.size();
			//int number = (int) ni.get("number");
			boolean vol_enable = true;
			if (az.contains("vmware")) {
				vol_enable = false;
			}
			List<NovaServer> serverList = new ArrayList<NovaServer>();
//			List<NeutronFloatingIP> nfis = netNovaFloatingIp.get(name);
			for (int i = 0; i < number; i++) {
//				NovaServer server = new NovaServer(i_index, name + "_server", flavor, name, image,
//						Integer.toString(volume_size), volume_type, az, vol_enable, keyName);
				String instanceName = instanceNames.get(i);
				NovaServer server = new NovaServer(i_index, instanceName, flavor, name, image,
				Integer.toString(volume_size), volume_type, az, vol_enable, keyName, ostype, username, password);
				serverList.add(server);
//				if(instanceFloatingIP.containsKey(instanceName)){
//					NeutronFloatingIP nfi = instanceFloatingIP.get(instanceName);
//					NovaFloatingIpAssociation nfia = new NovaFloatingIpAssociation(server.getResourceName(),
//							nfi.getResourceName());
//					resourcesMap.putAll(nfi.getResourceMap());
//					resourcesMap.putAll(nfia.getResourceMap());
//					nfiaList.add(nfia);
//				}
				i_index++;
				count++;
			}
			netServerMap.put(name, serverList);
		}

		cpusTotal += count * cpus;
		memsTotal += count * mems;
		volTotal.put(volume_type, volume_size * count);
		
		if (tCpus.get(az) < (uCpus.get(az) + cpusTotal)) {
			throw new ResourceBusinessException(Message.CS_CPU_EXCEEDS_POOL_LIMIT_ERROR, ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		
		if (tMems.get(az) < (uMems.get(az) + memsTotal)) {
			throw new ResourceBusinessException(Message.CS_MEM_EXCEEDS_POOL_LIMIT_ERROR, ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}

		// *********** update pool ***********

		for (Map.Entry<String, Integer> entry : fipTotal.entrySet()) {
			String fipname = entry.getKey();
			if ((entry.getValue() + uFips.get(fipname)) > tFips.get(fipname)) {
				throw new ResourceBusinessException(Message.CS_FIP_EXCEEDS_POOL_LIMIT_ERROR, ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
			}
			uFips.put(fipname, uFips.get(fipname) + entry.getValue());
		}

		for (Map.Entry<String, Integer> entry : volTotal.entrySet()) {
			String volname = entry.getKey();
			if ((entry.getValue() + uVols.get(volname)) > tVols.get(volname)) {
				throw new ResourceBusinessException(Message.CS_VOL_EXCEEDS_POOL_LIMIT_ERROR, ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
			}
			uVols.put(volname, uVols.get(volname) + entry.getValue());
		}

		uCpus.put(az, uCpus.get(az) + cpusTotal);
		uMems.put(az, uMems.get(az) + memsTotal);
		String totalVols = null;
		String totalFips = null;
		try {
			String ufips = mapper.writeValueAsString(uFips);
			String uvols = mapper.writeValueAsString(uVols);
			totalVols = mapper.writeValueAsString(volTotal);
			totalFips = mapper.writeValueAsString(fipTotal);
			String ucpus = mapper.writeValueAsString(uCpus);
			String umems = mapper.writeValueAsString(uMems);
			pool.setuFips(ufips);
			pool.setuVolumes(uvols);
			pool.setuCpus(ucpus);
			pool.setuMems(umems);
		} catch (Exception e) {
			log.error(e);
		}

		// *********** FWaas ***********
//		Boolean firewallEnable = (Boolean) params.get("firewallEnable");
//		if (firewallEnable) {
//			int rIndex = 1;
//			int fwIndex = 1;
//			List<Map<String, Object>> fwaas = (List<Map<String, Object>>) params.get("fwaas");
//			for (Map<String, Object> fw : fwaas) {
//
//				List<Map<String, Object>> fwRules = (List<Map<String, Object>>) fw.get("firewall_rules");
//				List<String> fwRuleList = new ArrayList<String>();
//				for (Map<String, Object> fwRule : fwRules) {
//					String destIp = (String) fwRule.get("destination_ip_address");
//					String destPort = (String) fwRule.get("destination_port");
//					String sourceIp = (String) fwRule.get("source_ip_address");
//					String sourcePort = (String) fwRule.get("source_port");
//					String protocol = ((String) fwRule.get("protocol")).toLowerCase();
//					String action = ((String) fwRule.get("action")).toLowerCase();
//					NeutronFirewallRule rule = new NeutronFirewallRule(rIndex, protocol, "4", sourceIp, sourcePort,
//							destIp, destPort, action, true);
//					fwRuleList.add(rule.getResourceName());
//					resourcesMap.putAll(rule.getResourceMap());
//					rIndex++;
//				}
//				NeutronFirewallPolicy fwPolicy = new NeutronFirewallPolicy(fwIndex, fwRuleList);
//				
//				List<String> fwNetworks = (List<String>) fw.get("networks");
//				List<String> fwRouters = new ArrayList<String>();
//				for (String net : fwNetworks) {
//					String routerName = net + "_router";
//					fwRouters.add(routerName);
//				}
//				NeutronFirewall fwFirewall = new NeutronFirewall(fwIndex, fwPolicy.getResourceName(),
//						fwRouters.toArray(new String[fwRouters.size()]));
//				resourcesMap.putAll(fwPolicy.getResourceMap());
//				resourcesMap.putAll(fwFirewall.getResourceMap());
//				fwIndex++;
//			}
//		}

		// *********** LBaas ***********
//		Boolean loadBalancerEnable = (Boolean) params.get("loadBalancerEnable");
//
//		if (loadBalancerEnable) {
//			List<Map<String, Object>> lbaas = (List<Map<String, Object>>) params.get("lbaas");
//			int lbIndex = 0;
//			for (Map<String, Object> lb : lbaas) {
//				String name = (String) lb.get("name");
//				String protocol = ((String) lb.get("protocol")).toUpperCase();
//				int port = (int) lb.get("port");
//				String lb_algorithm = (String) lb.get("lb_algorithm");
//				String net = (String) lb.get("net");
//				List<String> instanceNames = (List<String>) lb.get("instance_names"); 
//				int instance_count = instanceNames.size();
//				//int instance_count = (int) lb.get("instance_count");
//				String type = (String) lb.get("type");
//				int delay = (int) lb.get("delay");
//				int max_retries = (int) lb.get("max_retries");
//				int timeout = (int) lb.get("timeout");
//
//				NeutronSubnet lbsubnet = netSubnetMap.get(net);
//				NeutronLBaaSLoadBalancer lbaasLb = new NeutronLBaaSLoadBalancer(name, lbsubnet.getResourceName());
//				NeutronLBaaSListener lbaasListener = new NeutronLBaaSListener(name, lbaasLb.getResourceName(), protocol,
//						port);
//				NeutronLBaasPool lbaasPool = new NeutronLBaasPool(name, protocol, lb_algorithm,
//						lbaasListener.getResourceName());
//				NeutronLBaaSHealthMonitor lbaasMonitor = new NeutronLBaaSHealthMonitor(name, type,
//						lbaasPool.getResourceName(), delay, timeout, max_retries);
//				resourcesMap.putAll(lbaasLb.getResourceMap());
//				resourcesMap.putAll(lbaasListener.getResourceMap());
//				resourcesMap.putAll(lbaasPool.getResourceMap());
//				resourcesMap.putAll(lbaasMonitor.getResourceMap());
//
//				List<NovaServer> serverList = netServerMap.get(net);
//				List<String> thisNetServernames = new ArrayList<String>();
//				for (NovaServer ns : serverList) {
//					String rname = ns.getResourceName();
//					thisNetServernames.add(rname);
//				}
//				for (String instanceName : instanceNames) {
//					if (!thisNetServernames.contains(instanceName)) {
//						throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE, new Locale(ostoken.getLocale()));
//					}
//				}
//
//				for (int srvIndex = 0; srvIndex < instance_count; srvIndex++) {
//					String instanceName = instanceNames.get(srvIndex);
//					NeutronLBaaSPoolMember member = new NeutronLBaaSPoolMember(name, srvIndex,
//							lbaasPool.getResourceName(), instanceName, port, lbsubnet.getResourceName());
//					resourcesMap.putAll(member.getResourceMap());
//				}
//				lbIndex++;
//			}
//		}

		// add all server to yaml
		for (Map.Entry<String, List<NovaServer>> entry : netServerMap.entrySet()) {
			for (NovaServer srv : entry.getValue()) {
				resourcesMap.putAll(srv.getResourceMap());
			}
		}

		// *********** DBaas ***********
//		Boolean databasesEnable = (Boolean) params.get("databasesEnable");
//		//String troveNet = this.cloudconfig.getTroveNetId();
//		if (databasesEnable) {
//			List<Map<String, Object>> dbaas = (List<Map<String, Object>>) params.get("dbaas");
//			for (Map<String, Object> db : dbaas) {
//				String name = (String) db.get("name");
//				String dbflavor = (String) db.get("flavor");
//				int dbvolume_size = (int) db.get("volume_size");
//				String datastore = (String) db.get("datastore");
//				String datastore_version = (String) db.get("datastore_version");
//				String network = (String) db.get("network");
//				String adminUser = (String) db.get("adminUser");
//				String dbpassword = (String) db.get("password");
//				String initialDatabase = (String) db.get("initialDatabase");
//				TroveInstance trove = new TroveInstance(name, dbflavor, datastore, datastore_version, network,
//						dbvolume_size, initialDatabase, adminUser, dbpassword);
//				resourcesMap.putAll(trove.getResourceMap());
//			}
//		}
		
		// *********** VPNaas ***********
//		Boolean VPNEnable = (Boolean) params.get("VPNEnable");
//		if (VPNEnable) {
//			List<Map<String, Object>> vpnaas = (List<Map<String, Object>>) params.get("vpnaas");
//			for (Map<String, Object> vpn : vpnaas) {
//				String name = (String) vpn.get("name");
//				String network = (String) vpn.get("network");
//				String subnetName = netSubnetMap.get(network).getResourceName();
//				String routerName = network + "_router";
//				String peer_address = (String) vpn.get("peer_address");
//				String peer_cidrs = (String) vpn.get("peer_cidrs");
//
//				NeutronVPNService vpnService= new NeutronVPNService(name, routerName, subnetName, routerInterfacesList);
//				NeutronIPsecPolicy IPsecPolicy = new NeutronIPsecPolicy(name);
//				NeutronIKEPolicy IKEPolicy = new NeutronIKEPolicy(name);
//				NeutronIPsecSiteConnection connection = new NeutronIPsecSiteConnection(name, peer_address, peer_cidrs,
//						"secret", IKEPolicy.getResourceName(), IPsecPolicy.getResourceName(),
//						vpnService.getResourceName());
//				resourcesMap.putAll(vpnService.getResourceMap());
//				resourcesMap.putAll(IPsecPolicy.getResourceMap());
//				resourcesMap.putAll(IKEPolicy.getResourceMap());
//				resourcesMap.putAll(connection.getResourceMap());
//			}
//		}
		
		// *********** maas ***********
//		Boolean monitorEnable = (Boolean) params.get("monitorEnable");
//		if (monitorEnable) {
//			Map<String, Object> maas = (Map<String, Object>) params.get("maas");
//			String name = (String) maas.get("name");
//			String type = "instance";
//			List<Map<String, Object>> resources = (List<Map<String, Object>>) maas.get("resources");
//			List<Map<String, Object>> rules = (List<Map<String, Object>>) maas.get("rules");
//			List<Map<String, Object>> notificationObjs = (List<Map<String, Object>>) maas.get("notificationObjs");
//			ServerMonitorRules smr = new ServerMonitorRules(name, type, resources, rules, notificationObjs);
//			resourcesMap.putAll(smr.getResourceMap());
//		}
		// *********** BUILD YAML AND CREATE ***********
		yamlMap.put("resources", resourcesMap);
		String yamlTemplate = BasicResource.convertMAP2YAML(yamlMap);
		System.out.println(yamlTemplate);
		//String template_name = this.genStackName(pool);
		String genName = this.genStackName(ostoken);

		Stack created = this.stackService.createStack(genName, null, yamlTemplate, null, null, ostoken);
		Stack stack = this.stackService.getStack(created.getId(), ostoken);
		
		if (stack != null) {
			Map<String, String> curStack = new LinkedHashMap<String, String>();
			curStack.put("id", stack.getId());
			curStack.put("name", stack.getName());
			curStack.put("core", Integer.toString(cpusTotal));
			curStack.put("ram", Integer.toString(memsTotal));
			curStack.put("volume_type", volume_type);
			curStack.put("volume_size", Integer.toString(volume_size * count));
	//		String stacks = null;
			List<Map<String, String>> stackList = null;
			try {
				stackList = mapper.readValue(pool.getStacks(), new TypeReference<List<Map<String, String>>>() {
				});
				stackList.add(curStack);
	//			stacks = mapper.writeValueAsString(stackList);
			} catch (Exception e) {
			}
			this.poolEntityService.updatePoolEntity(pool);
			RefreshStackStatusThread rss = new RefreshStackStatusThread(stack.getId(), pool.getId(), this.stackService,
					this.poolEntityService, this, params, ostoken, null);
			Thread t = new Thread(rss);
			t.start();
			PoolStack poolStack = new PoolStack();
			poolStack.setDisplayName(stackName);
			poolStack.setId(stack.getId());
			poolStack.setName(stack.getName());
			poolStack.setPoolId(pool.getId());
			poolStack.setStatus(stack.getStatus());
			poolStack.setCore(cpusTotal);
			poolStack.setRam(memsTotal);
			poolStack.setFip(totalFips);
			poolStack.setVolume(totalVols);
			long ms = Util.utc2Millionsecond(stack.getCreatedAt() + "Z");
			poolStack.setCreateAt(Util.millionSecond2Date(ms));
			poolStack.setUpdateAt(stack.getUpdatedAt());
			poolStack.setAz(az);
//			poolStack.setFwaas(String.valueOf(firewallEnable));
//			poolStack.setLbaas(String.valueOf(loadBalancerEnable));
//			poolStack.setDbaas(String.valueOf(databasesEnable));
//			poolStack.setMaas(String.valueOf(monitorEnable));
//			poolStack.setVpnaas(String.valueOf(VPNEnable));
			poolStack.setFwaas("false");
			poolStack.setLbaas("false");
			poolStack.setDbaas("false");
			poolStack.setMaas("false");
			poolStack.setVpnaas("false");
			poolStack.setMillionSeconds(ms);
			this.updateQuotaUsage(ostoken, az, cpusTotal, memsTotal, volTotal, fipTotal, true);
			this.poolEntityService.createPoolStack(poolStack);
			this.updateSyncResourceInfo(stack.getId(), "CREATE_COMPLETE");
		} else {

		}
		return stack;
	}
	

	private void updateQuotaUsage(TokenOs ostoken, String az, int cpusTotal, int memsTotal, Map<String, Integer> volTotal,
			Map<String, Integer> fipTotal, Boolean add) {
		this.quotaService.updateQuota(az + "_" + ParamConstant.CORE, ostoken, add, cpusTotal);
		this.quotaService.updateQuota(az + "_" + ParamConstant.RAM, ostoken, add, memsTotal);
		
//		this.resourceSpecService.updateResourceSpecQuota(ParamConstant.CORE, null, cpusTotal, add);
//		this.resourceSpecService.updateResourceSpecQuota(ParamConstant.RAM, null, memsTotal, add);
		
		for (Map.Entry<String, Integer> entry : volTotal.entrySet()) {
			this.quotaService.updateQuota(entry.getKey(), ostoken, add, entry.getValue());
//			this.resourceSpecService.updateResourceSpecQuota(ParamConstant.DISK, entry.getKey(), entry.getValue(), add);
		}
		for (Map.Entry<String, Integer> entry : fipTotal.entrySet()) {
			this.quotaService.updateQuota(entry.getKey(), ostoken, add, entry.getValue());
//			this.resourceSpecService.updateResourceSpecQuota(entry.getKey(), null, entry.getValue(), add);
		}
	}

	@Override
	public PoolConfig getPoolConfig(TokenOs authToken) throws BusinessException {
		return this.configService.getPoolConfig(authToken);
	}

	@Override
	public List<PoolStack> getStackList(String stackID, TokenOs ostoken) throws ResourceBusinessException {
		String tenantid = ostoken.getTenantid();
		PoolEntity pool = this.poolEntityService.getPoolEntityByTenantId(tenantid);
		if (pool == null) {
			return null;
		}

		List<PoolStack> psl = this.poolStackMapper.selectByPoolId(pool.getId());
		List<PoolStack> rt = new ArrayList<PoolStack>();
		for (PoolStack ps : psl) {
			if (ps.getStatus().equals("CREATE_FAILED")) {
				continue;
			}
			rt.add(ps);
		}
		return rt;
	}

//	private List<String> getStackIdList(PoolEntity pool) throws ResourceBusinessException {
//		List<PoolStack> psl = this.poolStackMapper.selectByPoolId(pool.getId());
//		List<String> sl = new ArrayList<String>();
//		for (PoolStack ps : psl) {
//			sl.add(ps.getId());
//		}
//		return sl;
//	}

	@Override
	public List<StackResource> getResources(String stackID, TokenOs ostoken) throws ResourceBusinessException {
		String tenantid = ostoken.getTenantid();
		PoolEntity pool = this.poolEntityService.getPoolEntityByTenantId(tenantid);
		if (pool == null) {
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		PoolStack stack = this.poolStackMapper.selectByPrimaryKey(stackID);
		if (stack == null) {
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		if (stack.getStatus().equals("CREATE_FAILED")) {
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		//List<StackResource> list = this.stackService.getStackResourceList(stack.getName(), stack.getId(), ostoken);
		List<StackResource> list = this.stackResourceMapper.selectByStackId(stack.getId());
//		for (StackResource s : list) {
//			String rname = s.getName();
//			StackResource cs = this.stackService.getStackResource(stack.getName(), stack.getId(), rname, ostoken);
//			s.setAttributes(cs.getAttributes());
//		}
		return list;
	}

	@Override
	public void delete(String stackID, TokenOs ostoken) throws ResourceBusinessException {
		String tenantid = ostoken.getTenantid();
		PoolEntity pool = this.poolEntityService.getPoolEntityByTenantId(tenantid);
		if (pool == null) {
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		PoolStack stack = null;
		List<PoolStack> psl = this.poolStackMapper.selectByPoolId(pool.getId());
		boolean stackExist = false;
		for (PoolStack ps : psl) {
			if (ps.getId().equals(stackID)) {
				stackExist = true;
				stack = ps;
				break;
			}
		}
		if (!stackExist) {
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		if (stack.getStatus().equals("CREATE_COMPLETE") || stack.getStatus().equals("DELETE_FAILED")|| stack.getStatus().equals("DELETE_IN_PROGRESS")) {
			try {
				List<StackResource> resources = this.stackService.getStackResourceList(stack.getName(), stack.getId(), ostoken);
				this.stackService.deleteStack(stack.getName(), stack.getId(), ostoken);
				PoolStack poolStack = new PoolStack();
				poolStack.setId(stack.getId());
				poolStack.setStatus("DELETE_IN_PROGRESS");
				this.poolEntityService.updatePoolStack(poolStack);
				RefreshStackStatusThread rss = new RefreshStackStatusThread(stack.getId(), pool.getId(), this.stackService,
						this.poolEntityService, this, null, ostoken, resources);
				Thread t = new Thread(rss);
				t.start();
			} catch (Exception e) {

			}
			//this.revertStackUsage(ostoken, stackID, pool.getId());
		}
		if (stack.getStatus().equals("ROLLBACK_FAILED")) {
			try {
				this.stackService.deleteStack(stack.getName(), stack.getId(), ostoken);
				PoolStack poolStack = new PoolStack();
				poolStack.setId(stack.getId());
				poolStack.setStatus("DELETE_IN_PROGRESS");
				this.poolEntityService.updatePoolStack(poolStack);
				RefreshStackStatusThread rss = new RefreshStackStatusThread(stack.getId(), pool.getId(), this.stackService,
						this.poolEntityService, this, null, ostoken, null);
				Thread t = new Thread(rss);
				t.start();
			} catch (Exception e) {

			}
			//this.revertStackUsage(ostoken, stackID, pool.getId());
		}
		if (stack.getStatus().equals("CREATE_FAILED")) {
			this.poolEntityService.deletePoolStack(stackID);
		}
		if (stack.getStatus().equals("CREATE_IN_PROGRESS")) {
//			try {
//				this.stackService.deleteStack(stack.getName(), stack.getId(), ostoken);
//				PoolStack poolStack = new PoolStack();
//				poolStack.setId(stack.getId());
//				poolStack.setStatus("DELETE_IN_PROGRESS");
//				this.poolEntityService.updatePoolStack(poolStack);
//				RefreshStackStatusThread rss = new RefreshStackStatusThread(stack.getId(), pool.getId(), this.stackService,
//						this.poolEntityService, this, null, ostoken, null);
//				Thread t = new Thread(rss);
//				t.start();
//			} catch (Exception e) {
//
//			}
			throw new ResourceBusinessException(Message.CS_DEPLOY_POOL_TEMPLATE_DELETE_DISALLOWED, ParamConstant.BAD_REQUEST_RESPONSE_CODE,
					new Locale(ostoken.getLocale()));
		}
		//this.poolEntityService.deletePoolStack(stackID);
	}

	@Override
	public void updatePoolQuota(String tenantId,Map<String,Integer> resourceQuotas,Boolean add){
		PoolEntity pool = this.poolEntityService.getPoolEntityByTenantId(tenantId);
		if (pool == null) {
			return;
		}
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Integer> uFips = null;
		Map<String, Integer> uVols = null;
		Map<String, Integer> uCpus = null;
		Map<String, Integer> uMems = null;
		try {
			uFips = mapper.readValue(pool.getuFips(), new TypeReference<HashMap<String, Integer>>() {
			});
			uVols = mapper.readValue(pool.getuVolumes(), new TypeReference<HashMap<String, Integer>>() {
			});
			uCpus = mapper.readValue(pool.getuCpus(), new TypeReference<HashMap<String, Integer>>() {
			});
			uMems = mapper.readValue(pool.getuMems(), new TypeReference<HashMap<String, Integer>>() {
			});
		} catch (Exception e) {
			log.error(e);
			return;
		}
		
		if(true == add){
			for(Map.Entry<String, Integer> entry : uCpus.entrySet()){    
			    if(resourceQuotas.containsKey(entry.getKey()+"_"+ParamConstant.CORE)){
			    	uCpus.put(entry.getKey(), entry.getValue()+resourceQuotas.get(entry.getKey()+"_"+ParamConstant.CORE));    	
			    }
			}
			
			for(Map.Entry<String, Integer> entry : uMems.entrySet()){    
				 if(resourceQuotas.containsKey(entry.getKey()+"_"+ParamConstant.RAM)){
					 uMems.put(entry.getKey(), entry.getValue()+resourceQuotas.get(entry.getKey()+"_"+ParamConstant.RAM));  
				 }  
			}   
			
			for(Map.Entry<String, Integer> entry : uFips.entrySet()){    
				 if(resourceQuotas.containsKey(entry.getKey())){
					uFips.put(entry.getKey(), entry.getValue()+resourceQuotas.get(entry.getKey()));
				 }
			}   
			
			for(Map.Entry<String, Integer> entry : uVols.entrySet()){    
				 if(resourceQuotas.containsKey(entry.getKey())){
					 uVols.put(entry.getKey(), entry.getValue()+resourceQuotas.get(entry.getKey()));
				 }
			}
		}else{
			for(Map.Entry<String, Integer> entry : uCpus.entrySet()){    
			    if(resourceQuotas.containsKey(entry.getKey()+"_"+ParamConstant.CORE)){
			    	uCpus.put(entry.getKey(), entry.getValue()-resourceQuotas.get(entry.getKey()+"_"+ParamConstant.CORE));    	
			    }
			}
			
			for(Map.Entry<String, Integer> entry : uMems.entrySet()){    
				 if(resourceQuotas.containsKey(entry.getKey()+"_"+ParamConstant.RAM)){
					 uMems.put(entry.getKey(), entry.getValue()-resourceQuotas.get(entry.getKey()+"_"+ParamConstant.RAM));  
				 }  
			}   
			
			for(Map.Entry<String, Integer> entry : uFips.entrySet()){    
				 if(resourceQuotas.containsKey(entry.getKey())){
					uFips.put(entry.getKey(), entry.getValue()-resourceQuotas.get(entry.getKey()));
				 }
			}   
			
			for(Map.Entry<String, Integer> entry : uVols.entrySet()){    
				 if(resourceQuotas.containsKey(entry.getKey())){
					 uVols.put(entry.getKey(), entry.getValue()-resourceQuotas.get(entry.getKey()));
				 }
			}
		}
		   

		try {
			String ufips = mapper.writeValueAsString(uFips);
			String uvols = mapper.writeValueAsString(uVols);
			String ucpus = mapper.writeValueAsString(uCpus);
			String urams = mapper.writeValueAsString(uMems);
			pool.setuFips(ufips);
			pool.setuVolumes(uvols);
			pool.setuCpus(ucpus);
			pool.setuMems(urams);
		} catch (Exception e) {
			log.error(e);
		}
		
		this.poolEntityService.updatePoolEntity(pool);
	}
	
	@Override
	public void revertStackUsage(TokenOs ostoken, PoolStack	stack, String poolId) {
		//PoolStack stack = this.poolStackMapper.selectByPrimaryKey(stackId);
		PoolEntity pool = this.poolEntityService.getPoolEntityById(poolId);

		ObjectMapper mapper = new ObjectMapper();

		Map<String, Integer> sFips = null;
		Map<String, Integer> sVols = null;
		Map<String, Integer> uFips = null;
		Map<String, Integer> uVols = null;
		Map<String, Integer> uCpus = null;
		Map<String, Integer> uMems = null;
		try {
			sFips = mapper.readValue(stack.getFip(), new TypeReference<HashMap<String, Integer>>() {
			});
			sVols = mapper.readValue(stack.getVolume(), new TypeReference<HashMap<String, Integer>>() {
			});
			uFips = mapper.readValue(pool.getuFips(), new TypeReference<HashMap<String, Integer>>() {
			});
			uVols = mapper.readValue(pool.getuVolumes(), new TypeReference<HashMap<String, Integer>>() {
			});
			uCpus = mapper.readValue(pool.getuCpus(), new TypeReference<HashMap<String, Integer>>() {
			});
			uMems = mapper.readValue(pool.getuMems(), new TypeReference<HashMap<String, Integer>>() {
			});
		} catch (Exception e) {
			log.error(e);
		}

		for (Map.Entry<String, Integer> entry : sFips.entrySet()) {
			String fipType = entry.getKey();
			int count = uFips.get(fipType) - entry.getValue();
			uFips.put(fipType, count);
		}

		for (Map.Entry<String, Integer> entry : sVols.entrySet()) {
			String volType = entry.getKey();
			int count = uVols.get(volType) - entry.getValue();
			uVols.put(volType, count);
		}
		String az = stack.getAz();
		uCpus.put(az, uCpus.get(az) - stack.getCore());
		uMems.put(az, uMems.get(az) - stack.getRam());
//		pool.setuCpus(pool.getuCpus() - stack.getCore());
//		pool.setuMems(pool.getuMems() - stack.getRam());
		try {
			String ufips = mapper.writeValueAsString(uFips);
			String uvols = mapper.writeValueAsString(uVols);
			String ucpus = mapper.writeValueAsString(uCpus);
			String umems = mapper.writeValueAsString(uMems);
			pool.setuFips(ufips);
			pool.setuVolumes(uvols);
			pool.setuCpus(ucpus);
			pool.setuMems(umems);
		} catch (JsonProcessingException e) {
			log.error(e);
		}
		this.updateQuotaUsage(ostoken, stack.getAz(), stack.getCore(), stack.getRam(), sVols, sFips, false);
		this.poolEntityService.updatePoolEntity(pool);
	}

	public String update(Map<String, String> params, TokenOs ostoken) {
		return null;
	}

	private Map<String, Object> initYAML() {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		//map.put("heat_template_version", "2013-05-23");
		map.put("heat_template_version", "2015-04-30");
		return map;
	}

	private void updateSyncResourceInfo(String id, String status) {
		try {
			SyncResource resource = new SyncResource();
			resource.setId(id);
			resource.setType(ParamConstant.STACK);
			resource.setExpectedStatus(status);
			syncResourceMapper.insertSelective(resource);
		} catch (Exception e) {
			log.error(e);
		}
	}

//	private String genStackName(PoolEntity p) {
//		List<PoolStack> ps = this.poolStackMapper.selectByPoolId(p.getId());
//		int no;
//		if (ps.size() == 0) {
//			no = 0;
//		} else {
//			String[] names = new String[ps.size()];
//			for (int i = 0; i < ps.size(); i++) {
//				names[i] = ps.get(i).getName();
//			}
//			Arrays.sort(names);
//			String last = names[ps.size() - 1];
//			no = Integer.parseInt(last.substring(last.length() - 1, last.length()));
//		}
//		return "pool-" + p.getId() + "_" + (no + 1);
//	}

	private String genStackName(TokenOs ostoken) {
		while(true){
			String random = UUID.randomUUID().toString().substring(0, 8);
			String name = "pool_" + ostoken.getTenantname() + "_" + random;
			if (this.validateName(name, ostoken)) {
				return name;
			}
		}
	}
	
	
	private boolean validateName(String name, TokenOs ostoken) {
		if (name == null || name.isEmpty()) {
			return false;
		}
		List<Stack> stacks = this.stackService.getStackList(null, ostoken);
		if (stacks == null || stacks.size() == 0){
			return true;
		}
		for (Stack stack : stacks) {
			String stackName = stack.getName();
			if (stackName.equals(name)) {
				return false;
			}
		}
		return true;
	}
	
	private boolean validatePoolStackName(String name, String poolId) {
		if (name == null || name.isEmpty()) {
			return false;
		}
		List<PoolStack> psl = this.poolStackMapper.selectByPoolId(poolId);
		if (psl == null || psl.size() == 0){
			return true;
		}
		for (PoolStack stack : psl) {
			if (stack.getStatus().equals("CREATE_FAILED")) {
				continue;
			}
			String stackName = stack.getDisplayName();
			if (name.equals(stackName)) {
				return false;
			}
		}
		return true;
	}
	
//	private String getFlavor(TokenOs ostoken, int flavor_vcpus, int flavor_ram)
//			throws BusinessException {
//		int vcpus = flavor_vcpus;
//		int ram = flavor_ram;
//		int id = 0;
//		List<Flavor> list = this.flavorService.getFlavorList(null, ostoken);
//		if (null != list) {
//			for (Flavor flavor : list) {
//				if (flavor.getRam() == ram && flavor.getVcpus() == vcpus) {
//					return flavor.getId();
//				}
//				int flavor_id = Integer.parseInt(flavor.getId());
//				if (id < flavor_id)
//					id = flavor_id;
//			}
//		}
//		String name = String.format("%s_%s_Flavor", vcpus, ram);
//
//		Flavor flavorCreate = new Flavor();
//		flavorCreate.setId(Integer.toString(++id));
//		flavorCreate.setName(name);
//		flavorCreate.setDisk(100);
//		flavorCreate.setRam(ram);
//		flavorCreate.setVcpus(vcpus);
//		FlavorJSON flavorJson = new FlavorJSON(flavorCreate);
//		TokenOs adminToken = null;
//		try {
//			adminToken = this.authService.createDefaultAdminOsToken();
//		} catch (Exception e) {
//			log.error(e);
//		}
//		JsonHelper<FlavorJSON, String> jsonHelp = new JsonHelper<FlavorJSON, String>();
//		Flavor decidedFlavor = flavorService.createFlavor(jsonHelp.generateJsonBodyWithEmpty(flavorJson), adminToken,ParamConstant.INSTANCE_TYPE);
//		if (null == decidedFlavor)
//			return null;
//		return decidedFlavor.getId();
//	}

	@SuppressWarnings("unchecked")
	@Override
	public void storeStackResourcesToDB(String stackId, Map<String, Object> params, TokenOs ostoken)
			throws BusinessException {
		PoolStack poolStack = this.poolStackMapper.selectByPrimaryKey(stackId);
		List<StackResource> resources = this.stackService.getStackResourceList(poolStack.getName(), poolStack.getId(),
				ostoken);
		
		Map<String, StackResource> resMap = new HashMap<String, StackResource>();
		for (StackResource r : resources) {
			StackResource cur = this.stackService.getStackResource(poolStack.getName(), poolStack.getId(), r.getName(), ostoken);
			cur.setStackId(stackId);
			this.stackResourceMapper.insert(cur);
			resMap.put(cur.getName(), cur);
		}
		
		List<String> instanceIds = new ArrayList<String>();
		String az = (String) params.get("availabilityZone");
		String instanceType = ParamConstant.INSTANCE_TYPE;
		if (az.contains("vdi")) {
			instanceType = ParamConstant.VDI_TYPE;
		}
		ObjectMapper mapper = new ObjectMapper();
		Map<String, String> serverNetId = new HashMap<String, String>();
		for (StackResource resource : resources) {
			if (resource.getResourceType().equals("OS::Neutron::Net")) {
				String netId = resource.getPhysicalResourceId();
				String[] rqbs = resource.getRequiredBy();
				for (String rqb : rqbs) {
					if(rqb.contains("server")){
						serverNetId.put(rqb, netId);
					}
				}
			}
		}
		
		for (StackResource resource : resources) {
			if (resource.getResourceType().equals("OS::Nova::Server")) {
				String instanceId = resource.getPhysicalResourceId();
				Instance instance = this.instanceService.getInstance(instanceId, instanceType, ostoken, false);
				StackResource insRes = resMap.get(resource.getName());
				String attr = insRes.getAttributes();
				String imageId = null;
				String imageName = null;
				String volId = null;
				String volTypeId = null;
				String volSize = null;
				try {
					Map<String, Object> attrobj = mapper.readValue(attr, new TypeReference<HashMap<String, Object>>() {
					});
					Map<String, Object> metadata = (Map<String, Object>) attrobj.get("metadata");
					imageId = (String) metadata.get("image_id");
					Image image = this.imageMapper.selectByPrimaryKey(imageId);
					imageName = image.getName();
					
					List<Map<String, String>> vols = (List<Map<String, String>>) attrobj
							.get("os-extended-volumes:volumes_attached");
					volId = vols.get(0).get("id");
					Volume v = this.volumeService.getVolume(volId, ostoken);
					volSize = v.getSize() + "";
					String vType = v.getVolume_type();
					VolumeType vt = this.volumeTypeMapper.selectByName(vType);
					volTypeId = vt.getId();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				instance.setSourceId(imageId);
				instance.setSourceName(imageName);
				instance.setNetworkIds(serverNetId.get(resource.getName()));
				instance.setVolumeType(volTypeId);
				instance.setVolumeSize(volSize);
				this.instanceMapper.updateByPrimaryKeySelective(instance);
				instanceIds.add(instanceId);
				this.storeResourceEventInfo(ostoken.getTenantid(), instance.getId(), ParamConstant.INSTANCE, null,
						ParamConstant.ACTIVE_STATUS, Util.getCurrentMillionsecond());
				continue;
			}
			if (resource.getResourceType().equals("OS::Cinder::Volume")) {
				String volId = resource.getPhysicalResourceId();
				this.volumeService.getVolume(volId, ostoken);
				continue;
			}
			if (resource.getResourceType().equals("OS::Neutron::Net")) {
				String netId = resource.getPhysicalResourceId();
				try {
					this.networkService.getNetwork(netId, ostoken);
				} catch (Exception e) {
					e.printStackTrace();
				}
				continue;
			}
			if (resource.getResourceType().equals("OS::Neutron::Router")) {
				String routerId = resource.getPhysicalResourceId();
				this.routerService.getRouter(routerId, ostoken);
				continue;
			}
			if (resource.getResourceType().equals("OS::Neutron::LBaaS::LoadBalancer")) {
				String lbId = resource.getPhysicalResourceId();
				this.loadbalancerService.getLoadbalancer(lbId, ostoken);
				continue;
			}
			if (resource.getResourceType().equals("OS::Neutron::LBaaS::Pool")) {
				String poolId = resource.getPhysicalResourceId();
				this.loadbalancerService.getPool(poolId, ostoken);
				for (StackResource rs : resources) {
					if (rs.getResourceType().equals("OS::Neutron::LBaaS::PoolMember")) {
						String poolMemberId = rs.getPhysicalResourceId();
						this.loadbalancerService.getPoolMember(poolId, poolMemberId, ostoken);
					}
				}
				continue;
			}
			if (resource.getResourceType().equals("OS::Neutron::FloatingIP")) {
	//			String fipId = resource.getPhysicalResourceId();
				//this.floatingIPService.getFloatingIP(fipId, ostoken, null);
				this.floatingIPService.getFloatingIPList(null, ostoken);
				continue;
			}
			if (resource.getResourceType().equals("OS::Neutron::Firewall")) {
				String fwId = resource.getPhysicalResourceId();
				this.firewallService.getFirewall(fwId, ostoken);
				continue;
			}
			if (resource.getResourceType().equals("OS::Neutron::FirewallRule")) {
				String fwrId = resource.getPhysicalResourceId();
				this.firewallService.getFirewallRule(fwrId, ostoken);
				continue;
			}
		}
		
		
		Boolean monitorEnable = (Boolean) params.get("monitorEnable");
		if (monitorEnable) {
			StackResource sr = this.getStackResourceByName(poolStack.getId(), "monitor_rule");
			String attr = sr.getAttributes();
			System.out.println(attr);
			Map<String, Object> outputobj = null;
			try {
				Map<String, Object> attrobj = mapper.readValue(attr, new TypeReference<HashMap<String, Object>>() {
				});
				String output = (String) attrobj.get("output");
				outputobj = mapper.readValue(output, new TypeReference<HashMap<String, Object>>() {
				});
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			String name = (String) outputobj.get("name");
			List<Map<String, String>> nameResources = (List<Map<String, String>>) outputobj.get("resources");
			List<Map<String, Object>> rules = (List<Map<String, Object>>) outputobj.get("rules");
			String type = (String) outputobj.get("type");
			List<Map<String, String>> notificationObjs = (List<Map<String, String>>) outputobj.get("notificationObjs");
			String description = poolStack.getDisplayName()+ " monitor";
			List<Map<String, String>> idResources = new ArrayList<Map<String, String>>();
			for (Map<String, String> nameR : nameResources) {
				String serverName = nameR.get("name");
				StackResource serverR = this.getStackResourceByName(poolStack.getId(), serverName);
				String id = serverR.getPhysicalResourceId();
				Map<String, String> idR = new HashMap<String, String>();
				idR.put("id", id);
				idResources.add(idR);
			}

			Map<String, Object> bodyobj = new LinkedHashMap<String, Object>();
			bodyobj.put("name", name);
			bodyobj.put("description", description);
			bodyobj.put("type", type);
			bodyobj.put("resources", idResources);
			bodyobj.put("rules", rules);
			bodyobj.put("notificationObjs", notificationObjs);
			String body = null;
			try {
				body = mapper.writeValueAsString(bodyobj);
				this.monitorService.createMonitor(body, ostoken);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private StackResource getStackResourceByName(String stackId, String rname) {
		List<StackResource> list = this.stackResourceMapper.selectByStackId(stackId);
		for (StackResource r : list) {
			if (r.getName().equals(rname)) {
				return r;
			}
		}
		return null;
	}

	@Override
	public void removeStackResourcesfromDB(PoolStack poolStack,  List<StackResource> resources, TokenOs ostoken) throws BusinessException {
//		if (poolStack.getMaas().equals("true")) {
//			
//		}
		List<String> instanceIds = new ArrayList<String>();
		for (StackResource resource : resources) {
			if (resource.getResourceType().equals("OS::Nova::Server")) {
				String instanceId = resource.getPhysicalResourceId();
				if(instanceId == null || instanceId.isEmpty()){
					continue;
				}
				try {
					this.instanceMapper.deleteByPrimaryKey(instanceId);
				} catch (Exception e) {
					log.error(e);
				}
				instanceIds.add(instanceId);
				
				this.storeResourceEventInfo(ostoken.getTenantid(), instanceId, ParamConstant.INSTANCE, ParamConstant.ACTIVE_STATUS,
						ParamConstant.DELETED_STATUS, Util.getCurrentMillionsecond());
				continue;
			}
			if (resource.getResourceType().equals("OS::Neutron::Net")) {
				String netId = resource.getPhysicalResourceId();
				if(netId == null || netId.isEmpty()){
					continue;
				}
				try {
					this.networkMapper.deleteByPrimaryKey(netId);
				} catch (Exception e) {
					log.error(e);
				}
				continue;
			}
			if (resource.getResourceType().equals("OS::Neutron::Subnet")) {
				String subnetId = resource.getPhysicalResourceId();
				if(subnetId == null || subnetId.isEmpty()){
					continue;
				}
				try {
					this.subnetMapper.deleteByPrimaryKey(subnetId);
				} catch (Exception e) {
					log.error(e);
				}
				continue;
			}
			if (resource.getResourceType().equals("OS::Neutron::Router")) {
				String routerId = resource.getPhysicalResourceId();
				if(routerId == null || routerId.isEmpty()){
					continue;
				}
				try {
					this.routerMapper.deleteByPrimaryKey(routerId);
				} catch (Exception e) {
					log.error(e);
				}
				continue;
			}
			if (resource.getResourceType().equals("OS::Cinder::Volume")) {
				String volId = resource.getPhysicalResourceId();
				if(volId == null || volId.isEmpty()){
					continue;
				}
				try {
					this.volumeMapper.deleteByPrimaryKey(volId);
				} catch (Exception e) {
					log.error(e);
				}
				continue;
			}
			if (resource.getResourceType().equals("OS::Neutron::LBaaS::LoadBalancer")) {
				String lbId = resource.getPhysicalResourceId();
				if(lbId == null || lbId.isEmpty()){
					continue;
				}
				try {
					this.loadbalancerMapper.deleteByPrimaryKey(lbId);
				} catch (Exception e) {
					log.error(e);
				}
				continue;
			}
			if (resource.getResourceType().equals("OS::Neutron::LBaaS::Pool")) {
				String poolId = resource.getPhysicalResourceId();
				if(poolId == null || poolId.isEmpty()){
					continue;
				}
				try {
					this.loadbalancerPoolMapper.deleteByPrimaryKey(poolId);
				} catch (Exception e) {
					log.error(e);
				}
				continue;
			}
			if (resource.getResourceType().equals("OS::Neutron::LBaaS::PoolMember")) {
				String poolMemberId = resource.getPhysicalResourceId();
				if(poolMemberId == null || poolMemberId.isEmpty()){
					continue;
				}
				try {
					this.loadbalancerPoolMemberMapper.deleteByPrimaryKey(poolMemberId);
				} catch (Exception e) {
					log.error(e);
				}
			}
			if (resource.getResourceType().equals("OS::Neutron::FloatingIP")) {
				String fipId = resource.getPhysicalResourceId();
				if(fipId == null || fipId.isEmpty()){
					continue;
				}
				try {
					this.floatingIPMapper.deleteByPrimaryKey(fipId);
				} catch (Exception e) {
					log.error(e);
				}
				continue;
			}
			if (resource.getResourceType().equals("OS::Neutron::Firewall")) {
				String fwId = resource.getPhysicalResourceId();
				if(fwId == null || fwId.isEmpty()){
					continue;
				}
				try {
					this.firewallMapper.deleteByPrimaryKey(fwId);
				} catch (Exception e) {
					log.error(e);
				}
				continue;
			}
			if (resource.getResourceType().equals("OS::Neutron::FirewallRule")) {
				String fwrId = resource.getPhysicalResourceId();
				if(fwrId == null || fwrId.isEmpty()){
					continue;
				}
				try {
					this.firewallRuleMapper.deleteByPrimaryKey(fwrId);
				} catch (Exception e) {
					log.error(e);
				}
				continue;
			}
		}
		if (poolStack.getMaas().equals("true")) {
			try {
				this.zabbixService.deleteMonitorObjs(instanceIds);
			} catch (Exception e) {
			}
		}
		this.stackResourceMapper.deleteByStackId(poolStack.getId());
	}

	@Override
	public StackConfig getStackConfig(TokenOs authToken) throws BusinessException{
		return this.configService.getStackConfig(authToken);
	}
	
	private void storeResourceEventInfo(String tenantId, String id, String type, String beginState, String endState,
			long time) {
		ResourceEvent event = new ResourceEvent();
		event.setTenantId(tenantId);
		event.setResourceId(id);
		event.setResourceType(type);
		event.setBeginState(beginState);
		event.setEndState(endState);
		event.setMillionSeconds(time);
		resourceEventMapper.insertSelective(event);
	}
}

