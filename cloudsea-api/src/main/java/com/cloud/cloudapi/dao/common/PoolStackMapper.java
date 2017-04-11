package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.PoolStack;

public interface PoolStackMapper {
	int deleteByPrimaryKey(String id);

	int insert(PoolStack record);

	int insertSelective(PoolStack record);

	PoolStack selectByPrimaryKey(String id);

	int updateByPrimaryKeySelective(PoolStack record);

	int updateByPrimaryKey(PoolStack record);

	List<PoolStack> selectByPoolId(String poolId);
	
	List<PoolStack> selectByIds(String[] ids);
}