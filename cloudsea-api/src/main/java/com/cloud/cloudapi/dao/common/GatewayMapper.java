package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Gateway;

public interface GatewayMapper extends SuperMapper<Gateway, String> {
	public List<Gateway> selectAll();
	public List<Gateway> selectListWithLimit(int limit);
	public List<Gateway> selectAllForPage(int start, int end);
}
