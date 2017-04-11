package com.cloud.cloudapi.service.openstackapi;

import java.util.List;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.foros.Role;

public interface RoleService {
	
	public Role createRole(Role role) throws Exception;
	
	public List<Role> getRoleList() throws Exception;
	
	public boolean  grantRoleToUserOnProject(String role_id,String user_id,String project_id) throws BusinessException;
	
	public void  removeRoleUserFromProject(String role_id,String user_id,String project_id) throws BusinessException;
	
	public Role getRoleByName(String name) throws BusinessException ;
	
	public void setTokenOs(TokenOs token);
	public void clearTokenOs();

}
