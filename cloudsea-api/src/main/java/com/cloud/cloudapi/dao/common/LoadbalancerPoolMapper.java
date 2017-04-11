package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.LBPool;

public interface LoadbalancerPoolMapper extends SuperMapper<LBPool,String> {
	
	public Integer insertOrUpdate(LBPool pool);
	
	public Integer insertOrUpdateBatch(List<LBPool> pools);
	
	public List<LBPool> selectAll();

	public List<LBPool> selectAllByTenantId(String tenantId);

	public List<LBPool> selectListWithLimit(int limit);

	public List<LBPool> selectAllByTenantIdWithLimit(String tenantId,int limit);
	
	public List<LBPool> selectAllForPage(int start, int end);
}