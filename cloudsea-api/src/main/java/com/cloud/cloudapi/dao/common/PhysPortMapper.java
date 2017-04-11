package com.cloud.cloudapi.dao.common;

import java.util.List;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PhysPort;

public interface PhysPortMapper extends SuperMapper<PhysPort, String>{

	public Integer insertOrUpdate(PhysPort port);
	public List<PhysPort> selectList();
	public List<PhysPort> selectListWithLimit(int limit);

}
