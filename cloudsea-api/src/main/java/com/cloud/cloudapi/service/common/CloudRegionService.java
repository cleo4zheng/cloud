package com.cloud.cloudapi.service.common;

import java.util.List;

import com.cloud.cloudapi.pojo.common.Region;

public interface CloudRegionService extends SuperDaoService<Region,String>{
	
	public int countNum()throws Exception;
	public List<Region> selectList()throws Exception;

}
