package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.HostDetail;

public interface HostDetailMapper extends SuperMapper<HostDetail, String>{
	public List<HostDetail> selectAll();
	public List<HostDetail> getHostDetailsById(String[] hostDetailIds);
	public Integer insertOrUpdateBatch(List<HostDetail> details);
}
