package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeType;

public interface VolumeTypeMapper extends SuperMapper<VolumeType, String> {
	public Integer countNum();
	
	public Integer insertOrUpdate(VolumeType volumeType);
	
	public Integer insertOrUpdateBatch(List<VolumeType> volumeTypes);
	
	public VolumeType selectByName(String name);
	
	public VolumeType selectByBackendName(String backendName);
	
	public List<VolumeType> selectAll();

	public List<VolumeType> selectAllForPage(int start, int end);
	
	public List<VolumeType> selectVolumeTypesById(String[] volumeTypeIds);
}
