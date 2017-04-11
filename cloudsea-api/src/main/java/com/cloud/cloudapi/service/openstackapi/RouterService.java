package com.cloud.cloudapi.service.openstackapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Router;
import com.fasterxml.jackson.core.JsonProcessingException;


public interface RouterService {
	public List<Router> getRouterList(Map<String, String> paramMap, TokenOs ostoken) throws BusinessException;
	public Router getRouter(String routerId, TokenOs ostoken) throws BusinessException;
	public Router createRouter(String createBody, TokenOs ostoken) throws BusinessException;
	public Router updateRouter(String routerId,String updateBody, TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public void attachPort(String routerId,String updateBody,TokenOs ostoken) throws BusinessException;
	public void detachPort(String routerId,String portId,TokenOs ostoken) throws BusinessException;
	public void deleteRouter(String routerId, TokenOs ostoken) throws BusinessException;
	public Router addInterfaceToRouter(String routerId,String body, TokenOs ostoken) throws BusinessException;
	public Router removeInterfaceFromRouter(String routerId,String subnetId, TokenOs ostoken) throws BusinessException;
	public Router setExternalGateway(String routerId, String updateBody, TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public Router clearExternalGateway(String routerId, TokenOs ostoken) throws BusinessException;
}
