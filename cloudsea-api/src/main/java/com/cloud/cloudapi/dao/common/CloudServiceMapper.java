package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.CloudService;

public interface CloudServiceMapper extends SuperMapper<CloudService, String> {
	public Integer countNum();
	
	public Integer insertOrUpdate(CloudService service);
	
	public Integer insertOrUpdateBatch(List<CloudService> services);
	
	public List<CloudService> selectAll();
	
	public CloudService selectByType(String type);
	
	public List<CloudService> selectByTypes(List<String> types);
	
	public List<CloudService> selectListForPage(int start, int end);
}
