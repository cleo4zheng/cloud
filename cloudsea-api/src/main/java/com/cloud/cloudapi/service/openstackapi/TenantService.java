package com.cloud.cloudapi.service.openstackapi;

import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.common.TokenGui;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Tenant;
import com.cloud.cloudapi.pojo.openstackapi.foros.Project;

public interface TenantService {
	public List<Tenant> getTenantList(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public Tenant getTenant(String id,TokenOs ostoken) throws BusinessException;
	public Tenant createTenant(String body,TokenOs ostoken) throws BusinessException;
	public Tenant updateTenant(String tenantId,String body,TokenOs ostoken) throws BusinessException;
	public List<String> getTenantNamesByUser(CloudUser user);
	
	public void deleteTenant(String tenantId,TokenOs ostoken) throws BusinessException;
	public Tenant addUsersToTenant(String tenantId,String body,TokenOs ostoken) throws BusinessException;
	public Tenant removeUsersFromTenant(String tenantId,String body,TokenOs ostoken) throws BusinessException;
	public void assignUserRoleToTenant(TokenOs ostoken,String tenantId,String body) throws BusinessException;
	public void assignUserRoleToTenant(String userId,String roleId,String tenantId);
	public void removeUserRoleFromTenant(String userId,String roleId,String tenantId);
	
	public Project createProject(Project project,TokenOs ostoken) throws BusinessException;
	public Project getProject(String projectId,TokenOs ot) throws BusinessException;
	public void deleteProject(String projectId,TokenOs ot) throws BusinessException;
	
	public Tenant getCurrentTenant(TokenGui guitoken) throws Exception;
	
	public Tenant getCurrentParentTenant(TokenGui guitoken) throws Exception;
	
	public Tenant getTenant(TokenGui guitoken,String tenantId) throws Exception;
	
	public Tenant getTenant(TokenOs ostoken,String tenantId) throws Exception;
	
	public List<CloudUser> getSubTenantUserList(TokenGui guitoken,String tenantId) throws Exception;
	
	public List<CloudUser> gettCurrentSubTenantUserList(TokenGui guitoken) throws Exception;
	
	public String updateTenantToken(TokenGui guitoken,String locale) throws Exception;
	
	public List<String> getTenantIdsByParentTenant(TokenOs ostoken);
	
}
