package com.cloud.cloudapi.service.openstackapi;

import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Flavor;

public interface FlavorService {
	public List<Flavor> getFlavorList(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public Flavor createFlavor(String createBody,TokenOs ostoken,String type) throws BusinessException;
	public Flavor getFlavor(String flavorId,TokenOs ostoken) throws BusinessException;
	public String getFlavor(TokenOs ostoken, String flavor_vcpus, String flavor_ram, String flavor_disk,Boolean vmwareZone) throws BusinessException;
	public String getFlavor(TokenOs ostoken, String flavor_vcpus, String flavor_ram, String flavor_disk,Boolean vmwareZone,String type) throws BusinessException;
	public Flavor createFlavorExtraSpecs(String flavorId,String createBody,TokenOs ostoken) throws BusinessException;
	public String getFlavor(TokenOs ostoken, String flavor_vcpus, String flavor_ram, String flavor_disk, String cpu_arch,
			String type) throws BusinessException;
}
