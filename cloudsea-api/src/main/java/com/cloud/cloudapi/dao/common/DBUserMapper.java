package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.DBUser;

public interface DBUserMapper extends SuperMapper<DBUser,String> {
	
	public Integer insertOrUpdate(DBUser user);
	
	public Integer insertOrUpdateBatch(List<DBUser> user);
	
	public List<DBUser> selectAll();

	public List<DBUser> selectByIds(String[] ids);
	//public List<Database> selectAllByTenantId(String tenantId);
	
	public List<DBUser> selectListWithLimit(int limit);
	
	//public List<Database> selectAllByTenantIdWithLimit(String tenantId,int limit);
	
	public List<DBUser> selectAllForPage(int start, int end);
	
	public List<DBUser> selectByinstanceId(String instanceId);

	public void deleteByInstanceId(String instanceId);
}
