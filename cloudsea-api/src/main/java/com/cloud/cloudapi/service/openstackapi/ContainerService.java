package com.cloud.cloudapi.service.openstackapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Container;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ContainerModel;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface ContainerService {
	public List<Container> getConstainers(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public Container getContainer(String containerId,TokenOs ostoken) throws BusinessException;
	public Container createContainer(String createBody,TokenOs ostoken) throws BusinessException;
	public void deleteContainer(String containerId,TokenOs ostoken) throws BusinessException;
	
	public List<ContainerModel> getContainerModels(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public ContainerModel getContainerModel(String modelId,TokenOs ostoken) throws BusinessException;
	public ContainerModel createContainerModel(String createBody,TokenOs ostoken) throws BusinessException, JsonParseException, JsonMappingException, IOException;
	void deleteContainerModel(String uuid, TokenOs ostoken) throws BusinessException;
	public Container createContainerNew(String createBody, TokenOs ostoken)
			throws BusinessException, JsonParseException, JsonMappingException, IOException;

}
