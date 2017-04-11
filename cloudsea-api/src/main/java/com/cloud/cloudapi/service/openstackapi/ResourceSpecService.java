package com.cloud.cloudapi.service.openstackapi;

import java.util.List;
import java.util.Map;

public interface ResourceSpecService {

	public void updateResourceSpecQuota(String name,String type,int size,Boolean add);
	public void updateTotalResourcesQuota(Map<String,Integer> resourceQuotas,List<String> resourceNames,Boolean add);
}
