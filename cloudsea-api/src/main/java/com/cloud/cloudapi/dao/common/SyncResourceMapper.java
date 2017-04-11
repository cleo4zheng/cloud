package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.SyncResource;

public interface SyncResourceMapper extends SuperMapper<SyncResource, String> {
	public Integer countNum();

	public Integer insertOrUpdate(SyncResource syncResource);
	
	public List<SyncResource> selectAll();

	public List<SyncResource> selectAllForPage(int start, int end);
}
