package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Backup;

public interface BackupMapper extends SuperMapper<Backup, String> {
	public Integer countNum();

	public Integer countNumByInstanceStatus(String status);
	
	public Integer insertOrUpdate(Backup backup);
	
	public Backup selectByInstanceId(String id);
	
	public List<Backup> selectListByInstanceId(String id);
	
	public List<Backup> selectListByTenantIds(List<String> ids);
	   
	public List<Backup> selectAll();
	
	public List<Backup> selectAllByTenantId(String tenantId);
	
	public List<Backup> selectListWithLimit(int limit);
	
	public List<Backup> selectAllByTenantIdWithLimit(String tenantId,int limit);
	
	public List<Backup> selectListForPage(int start, int end);
}
