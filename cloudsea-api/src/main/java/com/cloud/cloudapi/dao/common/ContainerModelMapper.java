package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.ContainerModel;

public interface ContainerModelMapper extends SuperMapper<ContainerModel, String> {
	public Integer countNum();

	public Integer countNumByInstanceStatus(String status);
	
	public List<ContainerModel> selectAll();
	
	public List<ContainerModel> selectAllByTenantId(String tenantId);
	
	public List<ContainerModel> selectListWithLimit(int limit);
	
	public List<ContainerModel> selectAllByTenantIdWithLimit(String tenantId,int limit);
	
	public List<ContainerModel> selectListForPage(int start, int end);
}
