package com.cloud.cloudapi.service.openstackapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.HardWare;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PhysNode;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface PhysNodeService {
	public List<PhysNode> getPhysNodes(Map<String,String> paramMap,TokenOs guiToken) throws BusinessException;
	public PhysNode getPhysNode(String nodeId,TokenOs guiToken) throws BusinessException;	
	public PhysNode createPhysNode(String createBody,TokenOs guiToken) throws BusinessException;
	public PhysNode updatePhysNode(String nodeId,String updateBody,TokenOs guiToken) throws BusinessException;
	public void changePhysNodePowerStates(String nodeId,String updateBody,TokenOs guiToken) throws BusinessException;
	public void deletePhysNode(String nodeId,TokenOs guiToken) throws BusinessException;
	public List<HardWare> getAvailableSpec(TokenOs guiToken) throws BusinessException;
	public String createInstance(String createBody,TokenOs guiToken) throws BusinessException, JsonProcessingException, IOException;
	public String deletePhysNodes(String deleteBody, TokenOs authToken) throws ResourceBusinessException, JsonProcessingException, IOException;
	public String changeNodespower(String actionBody, String action, TokenOs authToken) throws JsonProcessingException, IOException, ResourceBusinessException;
}
