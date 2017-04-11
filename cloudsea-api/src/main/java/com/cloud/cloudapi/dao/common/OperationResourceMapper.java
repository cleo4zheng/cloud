package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.OperationResource;

public interface OperationResourceMapper extends SuperMapper<OperationResource, String>{
	public Integer countNum();

	public Integer insertOrUpdate(OperationResource resource);
	
	public List<OperationResource> selectAll();

	public List<OperationResource> selectResourcesByOperationId(String operationId);
	
	public List<OperationResource> selectListForPage(int start, int end);
	
	public Integer deleteByOperationsId(List<String> ids);
}
