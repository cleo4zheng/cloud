package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Router;

/** 
* @author  wangw
* @create  2016年5月30日 下午4:29:49 
* 
*/
public interface RouterMapper extends SuperMapper<Router,String>{
	public List<Router> selectAll();
	
	public Integer insertOrUpdate(Router router);
	
	public Integer insertOrUpdateBatch(List<Router> routers);
	
	public Integer updateRouterFirewallInfo(Router router);
	
	public Router selectByVPNId(String vpnId);
	
	public List<Router> selectByIds(String[] ids);
	
	public List<Router> selectAllByTenantId(String tenantId);
	
	public List<Router> selectListByTenantIds(List<String> ids);
	
	public List<Router> selectBySubnetId(String subnetId);
	
	public List<Router> selectListWithLimit(int limit);
	
	public List<Router> selectAllByTenantIdWithLimit(String tenantId,int limit);
	
	public List<Router> selectAllForPage(int start, int end);
	
	
}
