package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Container;

public interface ContainerMapper extends SuperMapper<Container, String> {
	public Integer countNum();

	public Integer countNumByInstanceStatus(String status);
	
	public List<Container> selectAll();
	
	public List<Container> selectByIds(String[] ids);
	
	public List<Container> selectAllByTenantId(String tenantId);
	
	public List<Container> selectListWithLimit(int limit);
	
	public List<Container> selectAllByTenantIdWithLimit(String tenantId,int limit);
	
	public List<Container> selectListForPage(int start, int end);
}
