package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Tenant;

public interface TenantMapper extends SuperMapper<Tenant,String>{
	public List<Tenant> selectAllList();
	
	public List<Tenant> selectTenantsByIds(List<String> tenantIds);
	
	public List<Tenant> selectListByParentId(String parent_id);
	
	public List<Tenant> selectByQuotaTemplateId(String quota_template_id);
	
	public List<Tenant> selectByMonitorTemplateId(String monitor_template_id);
	
	public Tenant selectByName(String name);
	
	public Integer insertOrUpdate(Tenant tenant);

	public Integer insertOrUpdateBatch(List<Tenant> tenants);

	public List<CloudUser> selectUserListByParentId(String parent_id);
}
