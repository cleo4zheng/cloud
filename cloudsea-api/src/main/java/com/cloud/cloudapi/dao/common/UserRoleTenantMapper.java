package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.UserRoleTenant;

public interface UserRoleTenantMapper extends SuperMapper<UserRoleTenant,String>{

	public Integer insertOrUpdate(UserRoleTenant userRoleTenant);
	public Integer insertOrUpdateBatch(List<UserRoleTenant> userRoleTenants);
	public UserRoleTenant selectByUserRoleTenantId(String userId,String roleId,String tenantId);
	public List<UserRoleTenant> selectByUserTenantId(String userId,String tenantId);
	public List<UserRoleTenant> selectByUserId(String userId);
	
	public List<UserRoleTenant> selectByRoleId(String roleId);
	public Integer deleteByUserRoleTenantId(String userId,String roleId,String tenantId);
	public Integer deleteByUserTenantId(String userId,String tenantId);
}
