package com.cloud.cloudapi.service.openstackapi.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.CloudServiceMapper;
import com.cloud.cloudapi.dao.common.FirewallMapper;
import com.cloud.cloudapi.dao.common.LoadbalancerMapper;
import com.cloud.cloudapi.dao.common.VPNMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.CloudService;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Loadbalancer;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VPN;
import com.cloud.cloudapi.service.openstackapi.CloudServiceService;
import com.cloud.cloudapi.util.DictConstant;

@Service("serviceService")
public class CloudServiceServiceImpl implements CloudServiceService{
	
	@Autowired
	private CloudConfig cloudconfig;
	
	@Resource
	private VPNMapper vpnMapper;

	@Resource
	private LoadbalancerMapper loadbalancerMapper;
	
	@Resource
	private FirewallMapper firewallMapper;

	@Resource
	private CloudServiceMapper serviceMapper;

	@Override
	public List<CloudService> getServiceList(Map<String, String> paramMap, TokenOs ostoken) throws BusinessException {
		//vpn and loadbalance use ps command to check the service status, firewall,  db is a vm, so monitor db with monitor vm
		String tenantId = ostoken.getTenantid();
		List<VPN> vpnList = vpnMapper.selectAllByTenantId(tenantId);
		List<Loadbalancer> loadbalancerList = loadbalancerMapper.selectAllByTenantId(tenantId);
		//TODO zabbix监控方式未明，目前不监控
//		List<Firewall> firewallList = firewallMapper.selectAllByTenantId(tenantId);
		List<CloudService> cloudServiceList = new ArrayList<CloudService>();
		for(VPN vpn : vpnList){
			CloudService cloudService = new CloudService();
			cloudService.setId(vpn.getId());
			cloudService.setName(vpn.getName());
			cloudService.setType(DictConstant.CLOUDSERVICE_TYPE_VPN);
			cloudService.setStatus(vpn.getStatus());
			cloudService.setTenantId(vpn.getTenant_id());
			cloudServiceList.add(cloudService);
		}
		
		for(Loadbalancer loadbalancer : loadbalancerList){
			CloudService cloudService = new CloudService();
			cloudService.setId(loadbalancer.getId());
			cloudService.setName(loadbalancer.getName());
			cloudService.setType(DictConstant.CLOUDSERVICE_TYPE_LOADBALANCER);
			cloudService.setStatus(loadbalancer.getProvisioning_status());
			cloudService.setTenantId(loadbalancer.getTenant_id());
			cloudService.setCreatedAt(loadbalancer.getCreatedAt());
			cloudServiceList.add(cloudService);
		}
		
//		for(Firewall firewall : firewallList){
//			CloudService cloudService = new CloudService();
//			cloudService.setId(firewall.getId());
//			cloudService.setName(firewall.getName());
//			cloudService.setType(DictConstant.CLOUDSERVICE_TYPE_FIREWALL);
//			cloudService.setStatus(firewall.getStatus());
//			cloudService.setTenantId(firewall.getTenant_id());
//			cloudService.setCreatedAt(firewall.getCreatedAt());
//			cloudServiceList.add(cloudService);
//		}
		
		return cloudServiceList;
	}
	
	@Override
	public CloudService getService(String serviceId, TokenOs ostoken) throws BusinessException {
		VPN vpn = vpnMapper.selectByPrimaryKey(serviceId);
		if(null != vpn){
			CloudService cloudService = new CloudService();
			cloudService.setId(vpn.getId());
			cloudService.setName(vpn.getName());
			cloudService.setType(DictConstant.CLOUDSERVICE_TYPE_VPN);
			cloudService.setStatus(vpn.getStatus());
			cloudService.setTenantId(vpn.getTenant_id());
            return cloudService;			
		}
		
		Loadbalancer loadbalancer = loadbalancerMapper.selectByPrimaryKey(serviceId);
		if(null != loadbalancer){
			CloudService cloudService = new CloudService();
			cloudService.setId(loadbalancer.getId());
			cloudService.setName(loadbalancer.getName());
			cloudService.setType(DictConstant.CLOUDSERVICE_TYPE_LOADBALANCER);
			cloudService.setStatus(loadbalancer.getProvisioning_status());
			cloudService.setTenantId(loadbalancer.getTenant_id());
			cloudService.setCreatedAt(loadbalancer.getCreatedAt());
			return cloudService;
		}
		//@TODO zabbix监控方式未明，目前不监控
//		Firewall firewall = firewallMapper.selectByPrimaryKey(serviceId);
//		if(null != firewall){
//			CloudService cloudService = new CloudService();
//			cloudService.setId(firewall.getId());
//			cloudService.setName(firewall.getName());
//			cloudService.setType(DictConstant.CLOUDSERVICE_TYPE_FIREWALL);
//			cloudService.setStatus(firewall.getStatus());
//			cloudService.setTenantId(firewall.getTenant_id());
//			cloudService.setCreatedAt(firewall.getCreatedAt());
//		}
		
		return null;
	}
	
	
	@Override
	public void initServices(TokenOs ostoken) throws BusinessException{
		String systemInstanceType = cloudconfig.getSystemInstanceType();
		if(Util.isNullOrEmptyValue(systemInstanceType))
			return;
		String[] instanceTypes = systemInstanceType.split(",");
		List<CloudService> services = new ArrayList<CloudService>();
		for(int index = 0; index < instanceTypes.length; ++index){
			CloudService service = new CloudService();
			service.setId(Util.makeUUID());
			service.setType(instanceTypes[index]);
			services.add(service);
		}	
		serviceMapper.insertOrUpdateBatch(services);
	}
	
	@Override
	public List<CloudService> getSystemServiceCapacity(TokenOs ostoken){
		List<CloudService> services = serviceMapper.selectAll();
		return services;
	}
	
	@Override
	public CloudService getSystemServiceByType(String type){
		return serviceMapper.selectByType(type);
	}
}
