package com.cloud.cloudapi.service.common;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Topology;

public interface TopologyService {
	public Topology getTopology(TokenOs ostoken) throws BusinessException;
}
