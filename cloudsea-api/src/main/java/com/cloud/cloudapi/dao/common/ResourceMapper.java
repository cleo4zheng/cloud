package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceSpec;

public interface ResourceMapper extends SuperMapper<ResourceSpec, String> {
	public Integer countNum();
	public List<ResourceSpec> selectAll();
	public int addResourcesBatch(List<ResourceSpec> resources);
	public Integer insertOrUpdateBatch(List<ResourceSpec> resources);
	public List<ResourceSpec> selectByType(String type);
	public List<ResourceSpec> findResourcesByNames(List<String> names);
	public ResourceSpec selectByName(String name);
}
