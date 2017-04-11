package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceCreateProcess;

public interface ResourceCreateProcessMapper extends SuperMapper<ResourceCreateProcess, String>{
	public Integer countNum();

	public Integer insertOrUpdate(ResourceCreateProcess resource);
	
	public List<ResourceCreateProcess> selectAll();

	public List<ResourceCreateProcess> selectAllByTenantId(String tenantId);
	
	public List<ResourceCreateProcess> selectAllForPage(int start, int end);
}
