package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeSnapshot;

public interface VolumeSnapshotMapper extends SuperMapper<VolumeSnapshot, String> {
	public Integer countNum();

	public Integer insertOrUpdate(VolumeSnapshot snapshot);
	
	public Integer insertOrUpdateBatch(List<VolumeSnapshot> snapshots);
	
	public List<VolumeSnapshot> selectByVolumeId(String volumeId);
	
	public List<VolumeSnapshot> selectAll();
	
	public List<VolumeSnapshot> selectListByTenantId(String tenantId);
	
	public List<VolumeSnapshot> selectByTenantIdAndName(String tenantId,String name);
	
	public List<VolumeSnapshot> selectListForPage(int start, int end);
	
	public List<VolumeSnapshot> selectListByTenantIdAndStatus(String tenantId,String status);
		
	public List<VolumeSnapshot> selectListByTenantIdWithLimit(String tenantId,int limit);
	
	public List<VolumeSnapshot> selectListByTenantIdAndStatusWithLimit(String tenantId,String status,int limit);
	
}
