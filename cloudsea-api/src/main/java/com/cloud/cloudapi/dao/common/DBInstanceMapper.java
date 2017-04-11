package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.DBInstance;;

public interface DBInstanceMapper extends SuperMapper<DBInstance,String> {
	
	public Integer insertOrUpdate(DBInstance dbinstance);
	
	public Integer insertOrUpdateBatch(List<DBInstance> dbinstances);
	
	public List<DBInstance> selectAll();

	public List<DBInstance> selectByIds(String[] ids);
	
	public List<DBInstance> selectAllByTenantId(String tenantId);
	
	public List<DBInstance> selectListWithLimit(int limit);
	
	public List<DBInstance> selectAllByTenantIdWithLimit(String tenantId,int limit);
	
	public List<DBInstance> selectAllForPage(int start, int end);
	
	public List<DBInstance> selectAllByTenantIdAndType(String tenantId, String type);
	
	public List<DBInstance> selectAllByTenantIdAndTypeWithLimit(String tenantId, int limit, String type);
}
