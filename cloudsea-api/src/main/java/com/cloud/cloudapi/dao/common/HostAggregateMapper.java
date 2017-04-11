package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.HostAggregate;

public interface HostAggregateMapper extends SuperMapper<HostAggregate, String> {
	public Integer countNum();

	public Integer insertOrUpdate(HostAggregate aggregate);
	
	public Integer insertOrUpdateBatch(List<HostAggregate> aggregates);
	
	public List<HostAggregate> selectAll();

	public List<HostAggregate> selectByServiceId(String serviceId);
	
	public HostAggregate selectByZoneName(String availabilityZone);
	
	public List<HostAggregate> selectByServiceIds(List<String> serviceIds);
	
	public List<HostAggregate> selectListForPage(int start, int end);
}
