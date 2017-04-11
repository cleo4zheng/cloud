package com.cloud.cloudapi.service.openstackapi;

import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Image;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeType;

public interface ImageService {
	public Map<String,List<Image>> getImageList(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public List<Image> getImages(String type,TokenOs ostoken);
	public List<Image> getRatingImageList(Map<String, String> paramMap,TokenOs ostoken) throws BusinessException;
	public Map<String,List<Image>> getPrivateImages(Map<String,String> paramMap,TokenOs ostoken);
	public Image getImage(String imageId,TokenOs ostoken) throws BusinessException;
	public VolumeType getImageVolumeType(String imageId,TokenOs ostoken) throws BusinessException;
	public void deleteImage(String imageId,TokenOs ostoken) throws BusinessException;
	public List<Image> getInstanceImages(String instanceId);
}
