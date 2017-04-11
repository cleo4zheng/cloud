package com.cloud.cloudapi.service.openstackapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Network;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface NetworkService {
	public List<Network> getNetworkList(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public List<Network> getExternalNetworks(TokenOs ostoken) throws BusinessException;
	public Network getNetwork(String networkId,TokenOs ostoken) throws BusinessException;	
	public Network createNetwork(String createBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public Network updateNetwork(String networkId,String updateBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public void deleteNetwork(String networkId,TokenOs ostoken) throws BusinessException;
	public List<Network> getInstanceAttachedNetworks(String instanceId);
	public String getExternalNetworkId(String name,TokenOs ostoken)  throws BusinessException;
}
