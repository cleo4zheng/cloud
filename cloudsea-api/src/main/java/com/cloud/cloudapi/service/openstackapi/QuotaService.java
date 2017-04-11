package com.cloud.cloudapi.service.openstackapi;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Quota;
import com.cloud.cloudapi.pojo.openstackapi.forgui.QuotaDetail;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceSpec;
import com.cloud.cloudapi.pojo.quota.QuotaTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface QuotaService {
	
	public QuotaTemplate createSystemTemplate(TokenOs ostoken) throws BusinessException;
	public List<QuotaTemplate> getQuotaTemplates(TokenOs ostoken) throws BusinessException;
	public QuotaTemplate getQuotaTemplate(String id,TokenOs ostoken) throws BusinessException;
	public QuotaTemplate getQuotaTemplateFields(TokenOs ostoken) throws BusinessException;
	public QuotaTemplate createQuotaTemplate(String createBody,TokenOs ostoken) throws BusinessException;
	public void applyQuotaTemplate(String id,String tenantId,TokenOs ostoken) throws BusinessException;
	public QuotaTemplate updateQuotaTemplate(String id,String body,TokenOs ostoken) throws BusinessException;
	public void deleteQuotaTemplate(String id,TokenOs ostoken) throws BusinessException;
	
	public List<Quota> getHardQuotas(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public List<ResourceSpec> getTenantQuota(TokenOs ostoken,String instanceType,String volumeTypeId,String floatingType) throws BusinessException;
	public void deleteTenantQuota(TokenOs ostoken,String tenantId) throws BusinessException;
	public Boolean setHardQuota(Map<String,String> paramMap,TokenOs ostoken);
	public Quota getStorageQuotas(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public Quota getHardQuota(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public Quota getNetworkQuotas(Map<String, String> paramMap, TokenOs ostoken)throws BusinessException;
	public List<Quota> getQuotas(Map<String, String> paramMap, TokenOs ostoken)throws BusinessException;
	public List<Quota> getDefaultQuotas(TokenOs ostoken) throws BusinessException;
	public Quota updateComputeQuota(String updateBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public Quota updateNetworkQuota(String updateBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public Quota updateStoragrQuota(String updateBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public void updateQuota(String resourceName,TokenOs ostoken,boolean bAdd,int used);
	public void updateTenantResourcesQuota(List<String> resourceQuotaTypes, Map<String,Integer> resourceQuotas,TokenOs ostoken, boolean bAdd);
	public void checkQuota(List<QuotaDetail> quotaDetails,String tenantId,String availabilityZone,Locale locale) throws BusinessException;
	public void checkResource(String tenantId,int coreSize,int ramSize,int diskSize, String volumeBackendName,String availabilityZone,Locale locale) throws ResourceBusinessException;
	public Quota createQuota(TokenOs ostoken,String quotaType,List<String> quotaDetailTypes,List<String> units,List<Integer> totals) throws BusinessException;
	public void createDefaultQuota(TokenOs ostoken) throws BusinessException;
	public void createTenantQuota(String tenantId,Locale locale) throws BusinessException;
//	void checkResource(TokenOs ostoken, String serviceName, List<String> quotaDetailTypes, List<String> units,
//			List<Integer> totals, List<Host> hosts) throws BusinessException;
	public void checkFipResource(String floatingIpType, int count,Locale locale) throws ResourceBusinessException;
	public void checkResourceQuota(String tenantId,String type,int count,Locale locale) throws BusinessException;
	public void updateQuotaTotal(String resourceName, TokenOs ostoken, int total);
	public void setQuotaSharedStatus(String tenantId);
}
