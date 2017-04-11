package com.cloud.cloudapi.service.openstackapi;

import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeType;

public interface VolumeTypeService {
	public List<VolumeType> getVolumeTypeList(Map<String,String> paramMap,TokenOs ostoken)throws BusinessException;
	public VolumeType createVolumeType(String createBody, TokenOs ostoken)throws BusinessException;
	public VolumeType getVolumeType(String volumeTypeId, TokenOs ostoken)throws BusinessException;
	public VolumeType updateVolumeType(String volumeTypeId, String body,TokenOs ostoken)throws BusinessException;
	public void deleteVolumeType(String volumeTypeId, TokenOs ostoken)throws BusinessException;
}
