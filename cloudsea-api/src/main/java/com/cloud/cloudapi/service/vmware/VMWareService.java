package com.cloud.cloudapi.service.vmware;

import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;

public interface VMWareService {
	public void makeVCenterResources(TokenOs ostoken) throws ResourceBusinessException;

}
