package com.cloud.cloudapi.service.common;

import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.OperationLog;

public interface OperationLogService {
	public List<OperationLog> getOperationLogList(Map<String,String> paramMap,TokenOs ostoken);
	public OperationLog getOperationLogDetail(String operationId,TokenOs ostoken);
	public void deleteOperationLogs(String body,TokenOs ostoken);
	public void addOperationLog(String user,String tenantId,String title,String type,String resourcesId,String status,String details);
}
