package com.cloud.cloudapi.service.openstackapi;

import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Image;


public interface SnapshotService {
	public List<Image> getSnapshotList(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public Image getSnapshot(String imageId, TokenOs ostoken) throws BusinessException;

}
