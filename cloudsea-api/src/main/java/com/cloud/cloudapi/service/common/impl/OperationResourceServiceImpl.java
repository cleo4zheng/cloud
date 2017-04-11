package com.cloud.cloudapi.service.common.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.OperationResourceMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.OperationResource;
import com.cloud.cloudapi.service.common.OperationResourceService;

@Service("operationResourceService")  
public class OperationResourceServiceImpl implements OperationResourceService{
	
	@Autowired  
    private OperationResourceMapper operationResourceMapper;  
	
	@Override
	public List<OperationResource> getOperationResourceList(String resourcesId,TokenOs ostoken) throws BusinessException{
		if(!Util.isNullOrEmptyValue(resourcesId)){
			List<OperationResource> operationResources  = new ArrayList<OperationResource>();
			String[] resourceIdArray = resourcesId.split(",");
			for(int index = 0; index < resourceIdArray.length; ++index){
				OperationResource operationResource = this.operationResourceMapper.selectByPrimaryKey(resourceIdArray[index]);
				if(null == operationResource)
					continue;
				operationResources.add(operationResource);
			}
			
			return operationResources;
		}
		
		return null;
	}
}
