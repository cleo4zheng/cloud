package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Host;

public interface HostMapper extends SuperMapper<Host,String>{
	public Integer countNum();

	public List<Host> selectAll();
	
	public Integer insertOrUpdateBatch(List<Host> hosts);
	
	public List<Host> selectByHostNames(List<String> hostNames);
	
	public Host selectByHostName(String hostName);
	
	public List<Host> selectByServiceName(String serviceName);
	
	public List<Host> selectListForPage(int start, int end);
	
	public List<Host> selectByIds(String[] ids);
	
}
