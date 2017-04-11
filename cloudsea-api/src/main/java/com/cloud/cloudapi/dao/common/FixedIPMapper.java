package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.FixedIP;

public interface FixedIPMapper extends SuperMapper<FixedIP, String> {
	public List<FixedIP> selectAll();
	public int deleteFixedIPsById(String[] ids);
	public List<FixedIP> selectListWithLimit(int limit);
	public List<FixedIP> selectAllForPage(int start, int end);
}
