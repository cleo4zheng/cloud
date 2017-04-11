package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Image;

public interface ImageMapper  extends SuperMapper<Image, String> {
	public Integer countNum();

	public Integer countNumByInstanceStatus(String status);

	public Integer countNumByImageFlag(Boolean privateFlag);
	
	public Integer insertOrUpdate(Image image);
	
	public Image selectByInstanceId(String id);
	
	public List<Image> selectListByInstanceId(String id);
	
	public List<Image> selectImagesById(String[] imageIds);
	
	public List<Image> selectAll();

	public List<Image> selectAllInstanceImages();
	
	public List<Image> selectAllByTenantId(String tenantId);
	
	public List<Image> selectListByTenantIds(List<String> ids);

	public List<Image> selectAllPrivateImages(String tenantId);
	
	public List<Image> selectPrivateImagesWithLimit(String tenantId,int limit);
	
	public List<Image> selectListWithLimit(int limit);
	
	public List<Image> selectInstanceImagesWithLimit(int limit);
	
	public List<Image> selectImagesByType(String type);
	
	public List<Image> selectAllByTenantIdWithLimit(String tenantId,int limit);
	
	public List<Image> selectPrivateImages(String tenantId,boolean privateFlag);
	
	public List<Image> selectInstanceImages(String instanceId,boolean privateFlag);
	
	public List<Image> selectListForPage(int start, int end);
}
