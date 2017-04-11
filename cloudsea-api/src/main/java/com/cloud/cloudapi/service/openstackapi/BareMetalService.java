package com.cloud.cloudapi.service.openstackapi;

import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.BareMetalNode;
import com.cloud.cloudapi.pojo.openstackapi.forgui.BareMetalPort;

public interface BareMetalService {
	public List<BareMetalNode> getBareMetalNodes(Map<String,String> paramMap,TokenOs guiToken) throws BusinessException;
	public BareMetalNode getBareMetalNode(String bareMetalId,TokenOs guiToken) throws BusinessException;
	public BareMetalNode createBareMetalNode(String createBody,TokenOs guiToken) throws BusinessException;
	
	public List<BareMetalPort> getBareMetalPorts(Map<String,String> paramMap,TokenOs guiToken) throws BusinessException;
	public BareMetalPort getBareMetalPort(String portId,TokenOs guiToken) throws BusinessException;
	public BareMetalPort createBareMetalPort(String createBody,TokenOs guiToken) throws BusinessException;
	
}
