package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Loadbalancer;

public interface LoadbalancerMapper extends SuperMapper<Loadbalancer,String> {
	public Integer countNum();
	
	public Integer insertOrUpdate(Loadbalancer loadbalancer);
	
	public Loadbalancer selectByFloatingIP(String floatingIp);
	
	public List<Loadbalancer> selectAll();

	public List<Loadbalancer> selectByIds(String[] ids);

	public List<Loadbalancer> selectAllByTenantId(String tenantId);
	
	public List<Loadbalancer> selectListByTenantIds(List<String> ids);

	public List<Loadbalancer> selectListWithLimit(int limit);
	
	public List<Loadbalancer> selectAllByTenantIdWithLimit(String tenantId,int limit);
	
	public List<Loadbalancer> selectAllForPage(int start, int end);
}
