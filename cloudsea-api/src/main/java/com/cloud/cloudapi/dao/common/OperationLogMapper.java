package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.OperationLog;

public interface OperationLogMapper extends SuperMapper<OperationLog, String>{
	public Integer countNum();

	public Integer countNumByOperationStatus(String status);
	
	public List<OperationLog> selectAll(String tenantId);

	public List<OperationLog> selectListWithLimit(String tenantId,int limit);
	
	public List<OperationLog> selectListForPage(int start, int end);
	
	public Integer deleteByOperationsId(List<String> ids);
}
