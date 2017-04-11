package com.cloud.cloudapi.service.openstackapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Subnet;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface SubnetService {

	public List<Subnet> getSubnetList(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public Subnet getSubnet(String subnetId,TokenOs ostoken) throws BusinessException;	
	public List<Subnet> getSpecialAdminNetwork(TokenOs ostoken) throws BusinessException;
	public List<Subnet> createSubnet(String createBody,String instanceName,TokenOs ostoken,String networkId) throws BusinessException, JsonProcessingException, IOException;
	public List<Subnet> createSubnets(List<Subnet> subnetsCreateInfo, TokenOs ostoken) throws BusinessException;
	public Subnet updateSubnet(String subnetId,String updateBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public void deleteSubnet(String subnetId,TokenOs ostoken) throws BusinessException;
    
	public void connectRouter(String subnetId,String createBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
}
