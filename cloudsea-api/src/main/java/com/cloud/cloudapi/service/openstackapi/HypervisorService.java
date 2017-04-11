package com.cloud.cloudapi.service.openstackapi;

import java.util.List;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Hypervisor;

public interface HypervisorService {
	List<Hypervisor> listHypervisorDetail(TokenOs ostoken) throws BusinessException;
}
