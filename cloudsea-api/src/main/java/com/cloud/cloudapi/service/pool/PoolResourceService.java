package com.cloud.cloudapi.service.pool;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.StackConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PoolConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PoolStack;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Stack;
import com.cloud.cloudapi.pojo.openstackapi.forgui.StackResource;

public interface PoolResourceService {
	
	public Stack create(Map<String, Object> params, TokenOs ostoken, HttpServletResponse response) throws Exception;

	public List<StackResource> getResources(String stackId, TokenOs ostoken) throws ResourceBusinessException;

	public List<PoolStack> getStackList(String stackID, TokenOs ostoken) throws ResourceBusinessException;

	public PoolConfig getPoolConfig(TokenOs authToken) throws BusinessException;
	
	public void delete(String stackID, TokenOs ostoken) throws ResourceBusinessException;

	void storeStackResourcesToDB(String stackId, Map<String, Object> params, TokenOs ostoken) throws BusinessException;
	
	public StackConfig getStackConfig(TokenOs authToken) throws BusinessException;

	void revertStackUsage(TokenOs ostoken, PoolStack stack, String poolId);

	//void removeStackResourcesfromDB(PoolStack poolStack, TokenOs ostoken) throws BusinessException;

	void removeStackResourcesfromDB(PoolStack poolStack, List<StackResource> resources, TokenOs ostoken)
			throws BusinessException;
	
	public void updatePoolQuota(String tenantId,Map<String,Integer> resourceQuotas,Boolean add);
}
