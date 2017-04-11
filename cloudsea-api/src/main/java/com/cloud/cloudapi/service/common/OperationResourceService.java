package com.cloud.cloudapi.service.common;

import java.util.List;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.OperationResource;

public interface OperationResourceService {
	public List<OperationResource> getOperationResourceList(String resourcesId,TokenOs ostoken) throws BusinessException;
	
}
