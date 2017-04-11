package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;

public interface InstanceMapper extends SuperMapper<Instance, String> {
	public Integer countNum();

	public Integer insertOrUpdate(Instance instance);
	
	public Integer insertOrUpdateBatch(List<Instance> instances);
	
	public List<Instance> selectListByInstanceIds(List<String> instanceIds);
	
	public List<Instance> selectListByLbid(String lbid);
	
	public Integer deleteByInstanceIds(List<String> instanceIds);
	
	public Integer countNumByInstanceStatus(String status);
	
	public Integer countNumByTenantId(String tenantId);
	
	public Instance selectInstanceByPortId(String portIds);
	
	public Instance selectInstanceByFixedIp(String fixedIp);
	
	public Instance selectInstanceByImageId(String imageIds);
	
	public Instance selectInstanceByVolumeId(String volumeIds);
	
	public List<Instance> selectInstanceBySecurityGroupId(String securityGroupIds);
	
	public Instance selectInstanceByFixedIpAndNetwork(String fixedIp,String networkId);
	
	public Instance selectInstanceByFloatingIp(String floatingIp);
	
	public List<Instance> selectList();

	public List<Instance> selectByNetworkId(String networkIds);
	
	public List<Instance> selectListByTenantId(String tenantId);
	
	public List<Instance> selectListByTenantIds(List<String> ids);
	
	public List<Instance> selectListByTenantIdWithType(String tenantId,String type);
	
	public List<Instance> selectListByTenantIdWithLimit(String tenantId,int limit);

	public List<Instance> selectListByTenantIdWithTypeAndLimit(String tenantId,String type,int limit);
	
	public List<Instance> selectListForPage(int start, int end);
	
	
}
