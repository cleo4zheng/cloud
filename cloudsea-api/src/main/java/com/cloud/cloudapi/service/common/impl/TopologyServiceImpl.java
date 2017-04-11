package com.cloud.cloudapi.service.common.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.ImageMapper;
import com.cloud.cloudapi.dao.common.InstanceMapper;
import com.cloud.cloudapi.dao.common.NetworkMapper;
import com.cloud.cloudapi.dao.common.RouterMapper;
import com.cloud.cloudapi.dao.common.SubnetMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Image;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Network;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Router;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Subnet;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Topology;
import com.cloud.cloudapi.service.common.TopologyService;
import com.cloud.cloudapi.util.ParamConstant;

@Service("topologyService")
public class TopologyServiceImpl implements TopologyService {
	
	@Autowired
	private NetworkMapper networkMapper;
	
	@Autowired
	private SubnetMapper subnetMapper;
	
	@Autowired
	private InstanceMapper instanceMapper;
	
	@Autowired
	private ImageMapper imageMapper;
	
	@Autowired
	private RouterMapper routerMapper;
	
	@Override
	public Topology getTopology(TokenOs ostoken) throws BusinessException{
		
		Topology topo = new Topology();
		
		String tenantId = ostoken.getTenantid();
		List<Network> networks = networkMapper.selectListByTenantId(tenantId);
		if(!Util.isNullOrEmptyList(networks)){
			for(Network network : networks){
				if(true == network.getExternal())
					network.setNodeType(ParamConstant.EXTERNAL_NETWORK_TYPE);
				else
					network.setNodeType(ParamConstant.INTERNAL_NETWORK_TYPE);
				List<Instance> instances = instanceMapper.selectByNetworkId(network.getId());
				if(!Util.isNullOrEmptyList(instances)){
					List<String> instanceIds = new ArrayList<String>();
					for(Instance instance : instances){
						instanceIds.add(instance.getId());
					}
					network.setServers(instanceIds);
				}
				
				List<Subnet> subnets = subnetMapper.selectListByNetworkId(network.getId());
				network.setSubnets(subnets);
			}
		}
	
		List<Instance> instances = instanceMapper.selectListByTenantId(tenantId);
		if(!Util.isNullOrEmptyList(instances)){
			for(Instance instance : instances){
				Image image = imageMapper.selectByPrimaryKey(instance.getSourceId());
				instance.setImage(image);
				instance.setIps(Util.stringToList(instance.getFixedips(), ","));
				instance.setFloatingIps(Util.stringToList(instance.getFloatingips(), ","));
				instance.setNodeType(ParamConstant.INSTANCE_TYPE);
				instance.setNetworksId(Util.stringToList(instance.getNetworkIds(), ","));
			}
		}
		
		List<Router> routers = routerMapper.selectAllByTenantId(tenantId);
		if(!Util.isNullOrEmptyList(routers)){
			for(Router router : routers){
				router.setNodeType(ParamConstant.ROUTER_TYPE);
				List<String> subnetIds = Util.stringToList(router.getSubnetIds(), ",");
				if(!Util.isNullOrEmptyList(subnetIds)){
					List<String> networkIds = new ArrayList<String>();
					for(String subnetId : subnetIds){
						Subnet subnet = subnetMapper.selectByPrimaryKey(subnetId);
						if(null == subnet)
							continue;
						if(networkIds.contains(subnet.getNetwork_id()))
							continue;
						networkIds.add(subnet.getNetwork_id());
					}
					router.setNetworks(networkIds);
				}
			}
		}
		topo.setNetworks(networks);
		topo.setServers(instances);
		topo.setRouters(routers);
		topo.setId(Util.makeUUID());
		return topo;
	}
}
