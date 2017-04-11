package com.cloud.cloudapi.service.common;

import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.json.forgui.StackConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FloatingIPConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.InstanceConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PoolConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeConfig;
import com.cloud.cloudapi.pojo.rating.TemplateTenantMapping;

public interface ConfigService {
	public String[] getConfigPrice(TemplateTenantMapping tenantRating,String serviceCode, String[] typeList, String[] defaultPrice, TokenOs authToken);
	public Map<String,String> getConfigPrice(TemplateTenantMapping tenantRating, String serviceCode, String[] typeList,Map<String,String> defaultPrices, TokenOs ostoken);
	public Map<String,String> getConfigPrice(TemplateTenantMapping tenantRating, String serviceCode,String defaultPrice,TokenOs ostoken);
	public Map<String,String> getConfigPrice(TemplateTenantMapping tenantRating, String serviceCode,String defaultCpuPrice,String defaultRamPrice,TokenOs ostoken);

	public PoolConfig getPoolConfig(TokenOs authToken) throws BusinessException;
	public StackConfig getStackConfig(TokenOs authToken) throws BusinessException;
	public InstanceConfig getInstanceConfig(String type, TokenOs authToken) throws BusinessException;
	public FloatingIPConfig getFloatingIPConfig(TokenOs ostoken) throws BusinessException;
	public VolumeConfig getVolumeConfig(TokenOs ostoken) throws BusinessException;
}
