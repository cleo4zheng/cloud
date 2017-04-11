package com.cloud.cloudapi.dao.common;
import java.util.List;

import com.cloud.cloudapi.pojo.common.Region;

public interface RegionMapper extends SuperMapper<Region,String>{

	public int countNum();
	public List<Region> selectList();
}
