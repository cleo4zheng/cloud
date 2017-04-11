package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.FloatingIP;

/** 
* @author  wangw 
* @create  2016年5月31日 上午11:52:51 
* 
*/
public interface FloatingIPMapper extends SuperMapper<FloatingIP,String> {
	
	public Integer countNum();

	public Integer countNumByInstanceStatus(String status);
	
	public Integer insertOrUpdate(FloatingIP floatingIP);
	
	public Integer insertOrUpdateBatch(List<FloatingIP> floatingIPs);
	
	public FloatingIP selectByInstanceId(String id);
	
	public FloatingIP selectByName(String name);
	
	public FloatingIP selectByPortId(String port_id);
	
	public FloatingIP selectByRouterId(String id);
	
	public FloatingIP selectByAddress(String floatingIpAddress);
	
	public List<FloatingIP> selectListByFloatingIps(List<String> floatingIps);
	
	public List<FloatingIP> selectByIds(String[] ids);
	
	public FloatingIP selectByFloatingIp(String floatingIp);
	
	public List<FloatingIP> selectListByInstanceId(String id);
	
	public List<FloatingIP> selectListByRouterId(String id);
	
	public List<FloatingIP> selectListByTenantId(String tenantId);
	
	public List<FloatingIP> selectListByTenantIds(List<String> ids);
	
	public List<FloatingIP> selectListByTenantIdAndNetId(String tenantId,String networkId);
	
	public List<FloatingIP> selectListByTenantIdWithLimit(String tenantId,int limit);
	
	public List<FloatingIP> selectAll();

	public List<FloatingIP> selectAllForPage(int start, int end);

}
