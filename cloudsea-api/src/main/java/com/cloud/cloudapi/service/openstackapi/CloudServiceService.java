package com.cloud.cloudapi.service.openstackapi;

import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.CloudService;

public interface CloudServiceService {	
	public List<CloudService> getServiceList(Map<String, String> paramMap, TokenOs ostoken) throws BusinessException;
	public CloudService getService(String serviceId, TokenOs ostoken) throws BusinessException;
	public void initServices(TokenOs ostoken) throws BusinessException;
	public List<CloudService> getSystemServiceCapacity(TokenOs ostoken);
	public CloudService getSystemServiceByType(String type);
}
