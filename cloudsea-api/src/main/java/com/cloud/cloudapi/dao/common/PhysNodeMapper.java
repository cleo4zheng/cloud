package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.PhysNode;

public interface PhysNodeMapper extends SuperMapper<PhysNode, String>{

	public Integer insertOrUpdate(PhysNode node);
	public List<PhysNode> selectList();
	public List<PhysNode> selectListWithLimit(int limit);
}
