package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Firewall;

public interface FirewallMapper extends SuperMapper<Firewall,String> {
	public Integer countNum();
	
	public Integer insertOrUpdate(Firewall firewall);
	
	public Integer insertOrUpdateBatch(List<Firewall> firewalls);
	
	public List<Firewall> selectAll();

	public List<Firewall> selectAllByTenantId(String tenantId);
	
	public List<Firewall> selectListByTenantIds(List<String> ids);
	
	public List<Firewall> selectListWithLimit(int limit);
	
	public List<Firewall> selectAllByTenantIdWithLimit(String tenantId,int limit);
	
	public List<Firewall> selectByIds(String[] ids);

	public List<Firewall> selectAllForPage(int start, int end);
}
