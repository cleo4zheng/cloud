package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Database;

public interface DatabaseMapper extends SuperMapper<Database,String> {
	
	public Integer insertOrUpdate(Database db);
	
	public Integer insertOrUpdateBatch(List<Database> db);
	
	public List<Database> selectAll();

	public List<Database> selectByIds(String[] ids);
	//public List<Database> selectAllByTenantId(String tenantId);
	
	public List<Database> selectListWithLimit(int limit);
	
	//public List<Database> selectAllByTenantIdWithLimit(String tenantId,int limit);
	
	public List<Database> selectAllForPage(int start, int end);
	
	public List<Database> selectByinstanceId(String instanceId);

	public void deleteByInstanceId(String instanceId);
}
