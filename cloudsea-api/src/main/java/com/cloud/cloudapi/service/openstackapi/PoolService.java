package com.cloud.cloudapi.service.openstackapi;

import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Pool;

public interface PoolService {
	public Pool createPool(Pool poolInfo,TokenOs ostoken);
}
