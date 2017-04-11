package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.foros.DataStoreVersion;

public interface DataStoreVersionMapper extends SuperMapper<DataStoreVersion,String>{
	
	public Integer insertOrUpdate(DataStoreVersion dsv);
	
	public Integer insertOrUpdateBatch(List<DataStoreVersion> dsv);
	
	public List<DataStoreVersion> selectAll();
	
	public List<DataStoreVersion> selectListWithLimit(int limit);
	
	public List<DataStoreVersion> selectAllForPage(int start, int end);
	
	public List<DataStoreVersion> selectByDataStoreId(String datastoreId);
	
	public List<DataStoreVersion> selectByDataStoreType(String type);

}
