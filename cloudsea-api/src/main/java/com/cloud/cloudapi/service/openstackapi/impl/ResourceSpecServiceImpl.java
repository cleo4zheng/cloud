package com.cloud.cloudapi.service.openstackapi.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.ResourceMapper;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceSpec;
import com.cloud.cloudapi.service.openstackapi.ResourceSpecService;

@Service("resourceSpec")
public class ResourceSpecServiceImpl implements ResourceSpecService{

	@Autowired
	private ResourceMapper resourceMapper;
	
//	@Autowired
//	private VolumeTypeMapper volumeTypeMapper;
	
	@Override
	public void updateResourceSpecQuota(String name,String type,int size,Boolean add){
		if(false == add)
			size = -size;
		ResourceSpec resourceSpec = resourceMapper.selectByName(name);
		
//		if(ParamConstant.DISK.equals(name)){
//			VolumeType volumeType = volumeTypeMapper.selectByPrimaryKey(type);
//			if(null == volumeType)
//				volumeType = volumeTypeMapper.selectByName(type);
//			if(null != volumeType)
//				resourceSpec = resourceMapper.selectByName(volumeType.getBackendName());
//		}else{
//			resourceSpec = resourceMapper.selectByName(name);
//		}
		if(null != resourceSpec){
			resourceSpec.setUsed(resourceSpec.getUsed()+size);
			resourceMapper.updateByPrimaryKeySelective(resourceSpec);
		}
	}
	
	@Override
	public void updateTotalResourcesQuota(Map<String,Integer> resourceQuotas,List<String> resourceNames,Boolean add){
		List<ResourceSpec> resourceSpecs = resourceMapper.findResourcesByNames(resourceNames);
		if(resourceSpecs.size() != resourceQuotas.size())
			return;
		for(ResourceSpec resourceSpec : resourceSpecs){
			Integer used = resourceQuotas.get(resourceSpec.getName());
			if(null == used)
				continue;
			if (true == add)
				resourceSpec.setUsed(resourceSpec.getUsed() + used);
			else
				resourceSpec.setUsed(resourceSpec.getUsed() - used);
		}
		resourceMapper.insertOrUpdateBatch(resourceSpecs);
	}
}
