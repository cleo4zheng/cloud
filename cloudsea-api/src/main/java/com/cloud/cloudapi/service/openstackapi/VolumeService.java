package com.cloud.cloudapi.service.openstackapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Volume;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeSnapshot;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface VolumeService {
	
	public List<Volume> getVolumeList(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public Volume getVolume(String volumeId,TokenOs ostoken) throws BusinessException;
	
	public Volume refreshVolumeInfo(String volumeId, TokenOs ostoken) throws BusinessException;
	public Volume createVolume(String createBody,TokenOs ostoken) throws BusinessException, JsonParseException, JsonMappingException, IOException;
	public Volume updateVolume(String volumeId,String updateBody,TokenOs ostoken) throws BusinessException, JsonParseException, JsonMappingException, IOException;
	public void deleteVolume(String volumeId,TokenOs ostoken) throws BusinessException;

	public String deleteVolumes(String deleteBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public VolumeConfig getVolumeConfig(TokenOs ostoken) throws BusinessException;
	public List<Volume> selectVolumes(String instanceId);
	
	public List<VolumeSnapshot> getSnapshots(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public VolumeSnapshot getSnapshot(String snapshotId,TokenOs ostoken) throws BusinessException;
	public VolumeSnapshot createSnapshot(String createBody,TokenOs ostoken) throws BusinessException;
	public VolumeSnapshot createSnapshotForInstance(String volumeId,TokenOs ostoken) throws BusinessException;
	public VolumeSnapshot updateSnapshot(String snapshotId,String updateBody,TokenOs ostoken) throws BusinessException;
	public void deleteSnapshot(String snapshotId,TokenOs ostoken) throws BusinessException;

	
}
