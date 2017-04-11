package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Volume;

public interface VolumeMapper extends SuperMapper<Volume, String> {
	public Integer countNum();

	public Integer countNumByInstanceStatus(String status);
	
	public Integer countNumByVolumeType(String volume_type);
	
	public Integer insertOrUpdate(Volume volume);
	
	public List<Volume> selectByInstanceId(String id);
	
	public Volume selectSystemVolumeByInstanceId(String id);
	
	public List<Volume> selectVolumesById(String[] volumeIds);
	
	public List<Volume> getVolumesByIds(List<String> volumeIds);
	
	public int deleteVolumesByIds(List<String> volumeIds);
	
	public List<Volume> selectListByInstanceId(String id);
	
	public List<Volume> selectListByTenantId(String tenantId);
	
	public List<Volume> selectListByTenantIds(List<String> ids);
	
	public List<Volume> selectListByTenantIdAndStatus(String tenantId,String status);
	
	public List<Volume> selectDataVolumesByTenantId(String tenantId);
	
	public List<Volume> selectDataVolumesByTenantIdAndStatus(String tenantId,String status);
	
	public List<Volume> selectDataVolumesByTenantIdWithLimit(String tenantId,int limit);
	
	public List<Volume> selectListByTenantIdWithLimit(String tenantId,int limit);
	
	public List<Volume> selectAll();

	public List<Volume> selectAllForPage(int start, int end);
}
