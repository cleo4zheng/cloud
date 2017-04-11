package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Port;

public interface PortMapper extends SuperMapper<Port, String> {
	public Integer countNum();
	
	public Port selectByNetworkId(String id);
	
	public Integer insertOrUpdate(Port port);
	
	public int insertOrUpdateBatch(List<Port> ports);
	
	//public Port selectByIp(String ip);
	public List<Port> selectByIp(String ip);
	
	public List<Port> selectPortsBySubetId(String subnetId);
	
	public List<Port> selectPortsBySecurityGroupId(String securityGroupId);
	
	public Port selectByIpAndNetworkId(String ip,String network_id);
	
	public List<Port> selectListByNetworkId(String id);
	
	public List<Port> selectPortsById(String[] portsId);
	
	public List<String> selectIpOfPortsById(String[] portsId);
	
	public List<Port> selectAll();
	
	public List<Port> selectAllByTenantIdWithLimit(String tenantId,int limit);
	
	public List<Port> selectListForPage(int start, int end);

	public List<Port> selectAllByTenantId(String tenantId);
	
	public List<Port> selectListByTenantIds(List<String> ids);

	public List<Port> selectByDeviceId(String device_id);
	
	public Integer deleteByIPAndDeviceId(String ip,String device_id);
}
