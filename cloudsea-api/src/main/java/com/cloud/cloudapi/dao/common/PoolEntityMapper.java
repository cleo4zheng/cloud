package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.PoolEntity;

public interface PoolEntityMapper {
	public int deleteByPrimaryKey(String id);

	public int insert(PoolEntity record);

	public int insertSelective(PoolEntity record);

	public PoolEntity selectByPrimaryKey(String id);
    
	public List<PoolEntity> selectByTenantId(String tenantId);
    
	public List<PoolEntity> selectAll();

    public List<PoolEntity> selectByIds(String[] ids);
    
    public int updateByPrimaryKeySelective(PoolEntity record);

    public int updateByPrimaryKey(PoolEntity record);
}