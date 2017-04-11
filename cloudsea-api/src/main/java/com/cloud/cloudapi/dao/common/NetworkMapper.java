package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.InstanceNetworkRel;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Network;

public interface NetworkMapper extends SuperMapper<Network,String>{
	public Integer countNum();
	public List<Network> selectAllList();
	public Integer insertOrUpdate(Network network);
	public Integer insertOrUpdateBatch(List<Network> networks);
	public List<Network> selectExternalNetworks();
	public Network selectTenantBasicNetwork(String tenant_id);
	public List<Network> selectListByTenantId(String tenant_id);
	public List<Network> selectListByTenantIds(List<String> ids);
	public List<Network> selectNetworksById(String[] networksId);
	public List<Network> selectListByPortId(String portId);
	public List<Network> selectBySubnetId(String subnetId);
	public Network selectByName(String name);
	public List<InstanceNetworkRel> selectListByInstanceId(String instanceId);
	public List<Network> selectListByTenantIdWithLimit(String tenant_id,int limit);
	public  int insertInstancesNetworks(Network network);
	public  int insertTenantsNetworks(Network network);
	public  int deleteInstancesNetworks(String network_id);
	public  int deleteTenantsNetworks(String network_id);

}
