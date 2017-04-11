package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.foros.Role;

public interface RoleMapper  extends SuperMapper<Role,String>{
	public int countNum();
	
	public Integer insertOrUpdate(Role role);
	
	public Integer insertOrUpdateBatch(List<Role> roles);
	
	public List<Role> selectList();
	
	public Role selectByName(String name);
}
