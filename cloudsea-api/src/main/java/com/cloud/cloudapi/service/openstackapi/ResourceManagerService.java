package com.cloud.cloudapi.service.openstackapi;

import java.util.List;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.AggregationInfo;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceCreateProcess;

public interface ResourceManagerService {
	public List<ResourceCreateProcess> getResourceCreateProcesses(TokenOs ostoken);
	public List<AggregationInfo> getAggregationInfos(TokenOs ostoken) throws BusinessException;
}
