package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Subnet;

public interface SubnetMapper extends SuperMapper<Subnet,String>{
	public Integer countNum();
	public List<Subnet> selectAllList();
	public List<Subnet> selectListByTenantId(String tenant_id);
	public List<Subnet> selectListByTenantIdWithLimit(String tenant_id,int limit);
	public List<Subnet> selectListByNetworkId(String networkId);
	public List<Subnet> selectListBySubnetIds(List<String> subnetIds);
	public int insertTenantsSubnets(Subnet subnet);
	public int insertSubnetsBatch(List<Subnet> subnets);
	public int insertOrUpdateBatch(List<Subnet> subnets);
	public int insertOrUpdate(Subnet subnet);
	public int deleteTenantsSubnets(String subnet_id);
	public List<Subnet> selectAllByTenantId(String tenantId);
}
