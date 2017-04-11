package com.cloud.cloudapi.service.openstackapi;

import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.EnvResource;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Host;
import com.cloud.cloudapi.pojo.openstackapi.forgui.HostAggregate;

public interface HostService {
	public List<Host> getHostList(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public Host getHostDetail( String hostName, String zoneName,TokenOs ostoken) throws BusinessException;
	public EnvResource getTotalResource(Map<String, String> paramMap, TokenOs ostoken) throws BusinessException;
	
	public List<HostAggregate> getHostAggregates(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public HostAggregate getHostAggregate(String id,TokenOs ostoken) throws BusinessException;
	public HostAggregate createHostAggregate(String body,TokenOs ostoken) throws BusinessException;
	public void addHostToAggregate(String id,String body,TokenOs ostoken) throws BusinessException;
	public void removeHostFromAggregate(String id,String body,TokenOs ostoken) throws BusinessException;
	public void setHostAggregateMetadate(String id,String name,TokenOs ostoken) throws BusinessException;
	public void deleteHostAggregate(String id,TokenOs ostoken) throws BusinessException;
	public HostAggregate updateHostAggregate(String id,String body,TokenOs ostoken) throws BusinessException;
}
