package com.cloud.cloudapi.service.openstackapi;

import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PhysPort;

public interface PhysPortService {
	public List<PhysPort> getPhysPorts(Map<String,String> paramMap,TokenOs guiToken) throws BusinessException;
	public PhysPort getPhysPort(String portId,TokenOs guiToken) throws BusinessException;	
	public PhysPort createPhysPort(String createBody,TokenOs guiToken) throws BusinessException;
}
