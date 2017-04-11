package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceEvent;

public interface ResourceEventMapper extends SuperMapper<ResourceEvent, String>{
	public Integer countNum();
	public List<ResourceEvent> selectAll();
	public int addResourcesBatch(List<ResourceEvent> resources);
	public Integer insertOrUpdateBatch(List<ResourceEvent> resources);
}
