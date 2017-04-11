package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.SecurityGroup;

public interface SecurityGroupMapper extends SuperMapper<SecurityGroup, String> {
	public Integer countNum();
	
	public Integer insertOrUpdate(SecurityGroup securityGroup);
	
	public Integer insertOrUpdateBatch(List<SecurityGroup> securityGroups);
	
	public List<SecurityGroup> selectAllList();
	
	public List<SecurityGroup> selectSecurityGroupsById(String[] ids);
	
	public List<SecurityGroup> selectSecurityGroupsByName(List<String> names);
	
	public SecurityGroup selectTenantDefaultSecurityGroup(String tenantId);
	
	public SecurityGroup selectTenantSecurityGroupByName(String name,String tenantId);
	
	public List<SecurityGroup> selectAllByTenantId(String tenantId);
	
	public List<SecurityGroup> selectListByTenantIds(List<String> ids);

	public List<SecurityGroup> selectListWithLimit(int limit);
	
	public List<SecurityGroup> selectAllByTenantIdWithLimit(String tenantId,int limit);
	
	public List<SecurityGroup> selectListForPage(int start, int end);
}
