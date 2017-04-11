package com.cloud.cloudapi.service.openstackapi;

import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PoolEntity;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PoolStack;

public interface PoolEntityService {
	public PoolEntity createPoolEntity(Map<String, Object> params, TokenOs ostoken) throws BusinessException;

	public List<PoolEntity> listPoolEntity(TokenOs ostoken);

	public PoolEntity getPoolEntityById(String id);

	public void updatePoolEntity(PoolEntity p);
	
	public PoolEntity getPoolEntityByTenantId(String id);

	PoolEntity updatePoolEntity(Map<String, Object> params, String poolId, TokenOs ostoken) throws BusinessException;
	
	List<PoolStack> listPoolStackByPoolId(String poolId);
	
	PoolStack getPoolStack(String id);

	int deletePoolStack(String id);

	int updatePoolStack(PoolStack poolStack);

	int createPoolStack(PoolStack poolStack);
}
