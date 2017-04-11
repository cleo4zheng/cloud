package com.cloud.cloudapi.service.openstackapi.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.HostAggregateMapper;
import com.cloud.cloudapi.dao.common.QuotaDetailMapper;
import com.cloud.cloudapi.dao.common.QuotaFieldMapper;
import com.cloud.cloudapi.dao.common.QuotaMapper;
import com.cloud.cloudapi.dao.common.QuotaTemplateMapper;
import com.cloud.cloudapi.dao.common.ResourceMapper;
import com.cloud.cloudapi.dao.common.TemplateFieldMapper;
import com.cloud.cloudapi.dao.common.TemplateServiceMapper;
import com.cloud.cloudapi.dao.common.TenantMapper;
import com.cloud.cloudapi.dao.common.VolumeTypeMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.ComputeQuotaJSON;
import com.cloud.cloudapi.json.forgui.NetworkQuotaJSON;
import com.cloud.cloudapi.json.forgui.QuotaJSON;
import com.cloud.cloudapi.json.forgui.StorageQuotaJSON;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ComputeQuota;
import com.cloud.cloudapi.pojo.openstackapi.forgui.HostAggregate;
import com.cloud.cloudapi.pojo.openstackapi.forgui.NetworkQuota;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Pool;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Quota;
import com.cloud.cloudapi.pojo.openstackapi.forgui.QuotaDetail;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceSpec;
import com.cloud.cloudapi.pojo.openstackapi.forgui.StorageQuota;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Tenant;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeType;
import com.cloud.cloudapi.pojo.quota.QuotaField;
import com.cloud.cloudapi.pojo.quota.QuotaTemplate;
import com.cloud.cloudapi.pojo.rating.TemplateField;
import com.cloud.cloudapi.pojo.rating.TemplateService;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.impl.AuthServiceImpl;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.StringHelper;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("quotaService")
public class QuotaServiceImpl implements QuotaService {
	@Resource
	private OSHttpClientUtil httpClient;

	@Autowired
	private CloudConfig cloudconfig;

	@Autowired
	private TenantMapper tenantMapper;

	@Autowired
	private QuotaMapper quotaMapper;

	@Autowired
	private QuotaDetailMapper quotaDetailMapper;

	@Autowired
	private ResourceMapper resourceMapper;
	
	@Autowired
	private VolumeTypeMapper volumeTypeMapper;

	@Autowired
	private HostAggregateMapper hostAggregateMapper;
	
	@Autowired
	private TemplateServiceMapper templateServiceMapper;
	
	@Autowired
	private TemplateFieldMapper templateFieldMapper;
	
	@Autowired
	private QuotaTemplateMapper quotaTemplateMapper;

	@Autowired
	private QuotaFieldMapper quotaFieldMapper;
	
	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(QuotaServiceImpl.class);
	
	public CloudConfig getCloudconfig() {
		return cloudconfig;
	}

	public void setCloudconfig(CloudConfig cloudconfig) {
		this.cloudconfig = cloudconfig;
	}

	public TenantMapper getTenantMapper() {
		return tenantMapper;
	}

	public void setTenantMapper(TenantMapper tenantMapper) {
		this.tenantMapper = tenantMapper;
	}

	public QuotaMapper getQuotaMapper() {
		return quotaMapper;
	}

	public void setQuotaMapper(QuotaMapper quotaMapper) {
		this.quotaMapper = quotaMapper;
	}

	public QuotaDetailMapper getQuotaDetailMapper() {
		return quotaDetailMapper;
	}

	public void setQuotaDetailMapper(QuotaDetailMapper quotaDetailMapper) {
		this.quotaDetailMapper = quotaDetailMapper;
	}


	@Override
	public List<Quota> getQuotas(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {
		List<Quota> quotas = getQuotasFromDB(ostoken.getTenantid(),new Locale(ostoken.getLocale()));
		return quotas;
//		if (!Util.isNullOrEmptyList(quotas))
//			return quotas;

//		quotas = getHardQuotas(null, ostoken, response);
//		Quota networkQuoto = getNetworkQuotas(null, ostoken, response);
//		Quota storageQuoto = getStorageQuotas(null, ostoken, response);
//		quotas = mergeQuotas(quotas, networkQuoto, storageQuoto);
//
//		if(Util.isNullOrEmptyList(quotas)){
//			quotas = getDefaultQuotas(ostoken,response);
//		}
//		storeQuotasToDB(quotas);
		
	}
	
	@Override
	public List<ResourceSpec> getTenantQuota(TokenOs ostoken,String instanceType,String volumeTypeId,String floatingType) throws BusinessException{
		List<Quota> tenantQuotas = quotaMapper.selectAllByTenantId(ostoken.getTenantid());
		if (Util.isNullOrEmptyList(tenantQuotas))
			return null;
		TokenOs adminToken = null;
		Boolean adminCheck = false;
		try{
			adminToken = authService.createDefaultAdminOsToken();	
			if(ostoken.getTenantid().equals(adminToken.getTenantid()))
				adminCheck = true;
		}catch(Exception e){
			log.error(e);
			//throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
		}
		
		Locale locale = new Locale(ostoken.getLocale());
		List<ResourceSpec> resourceSpecs = new ArrayList<ResourceSpec>();
		ResourceSpec cpuResourceSpec = null;
		ResourceSpec ramResourceSpec = null;
		String coreTypeName = null;
		String ramTypeName = null;
		if(!Util.isNullOrEmptyValue(instanceType)){
			String ramType = "CS_RAM_TYPE_NAME";
			String coreType = "CS_CORE_TYPE_NAME";
			coreTypeName = coreType.replaceFirst("TYPE", instanceType.toUpperCase());
			ramTypeName = ramType.replaceFirst("TYPE", instanceType.toUpperCase());
			cpuResourceSpec = new ResourceSpec();
			ramResourceSpec = new ResourceSpec();
			
			HostAggregate aggregate = hostAggregateMapper.selectByZoneName(instanceType);
			if(null != aggregate){
				if(locale.getLanguage().contains("zh")){
					cpuResourceSpec.setName(StringHelper.ncr2String(aggregate.getName()) + Message.getMessage(Message.CS_CPU_NAME,locale,false));	
					ramResourceSpec.setName(StringHelper.ncr2String(aggregate.getName()) + Message.getMessage(Message.CS_MEMORY_NAME,locale,false));
				}else{
					cpuResourceSpec.setName(StringHelper.ncr2String(aggregate.getName()) + " "+ Message.getMessage(Message.CS_CPU_NAME,locale,false));	
					ramResourceSpec.setName(StringHelper.ncr2String(aggregate.getName()) + " "+ Message.getMessage(Message.CS_MEMORY_NAME,locale,false));
				}
			}else{
				cpuResourceSpec.setName(Message.getMessage(coreTypeName,locale,false));
				ramResourceSpec.setName(Message.getMessage(ramTypeName,locale,false));	
			}
			
			cpuResourceSpec.setTotal(0);
			cpuResourceSpec.setUsed(0);
			ramResourceSpec.setTotal(0);
			ramResourceSpec.setUsed(0);
		}
		ResourceSpec diskResourceSpec = null;
		String volumeType = null;
		String volumeTypeName = null;
		String floatingipTypeName = null;
		if(!Util.isNullOrEmptyValue(volumeTypeId)){
			diskResourceSpec = new ResourceSpec();
			diskResourceSpec.setName(Message.getMessage(Message.CS_DISK_NAME,locale,false));
			VolumeType volumetype = volumeTypeMapper.selectByPrimaryKey(volumeTypeId);
			if(null == volumetype)
				throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			volumeType = volumetype.getName();
			volumeTypeName = volumetype.getDisplayName();
			if(null == volumeTypeName)
				volumeTypeName = Message.getMessage(volumeType.toUpperCase(),locale,false);
		//	volumeTypeName = "CS_VOLUME_TYPE_NAME".replaceFirst("TYPE",volumeType.toUpperCase());
			diskResourceSpec.setTotal(0);
			diskResourceSpec.setUsed(0);
	//		volumeBackendName = volumeType.getBackendName();
		}
		ResourceSpec floatingResourceSpec = null;
        if(!Util.isNullOrEmptyValue(floatingType)){
        	floatingResourceSpec = new ResourceSpec();
        	String floatingipType = "CS_FLOTINGIP_TYPE_NAME";
			floatingipTypeName = floatingipType.replaceFirst("TYPE", floatingType.toUpperCase());
       // 	floatingResourceSpec.setName(Message.getMessage(Message.CS_FLOATINGIP_NAME,locale,false));
			floatingResourceSpec.setName(Message.getMessage(floatingipTypeName,locale,false));
			floatingResourceSpec.setTotal(0);
			floatingResourceSpec.setUsed(0);
        }
        
        
		if(true == adminCheck){
			if(null != cpuResourceSpec){
			//	cpuResourceSpec = resourceMapper.selectByName(instanceType.toLowerCase()+"_core");
			//	cpuResourceSpec.setName(Message.getMessage(coreTypeName,locale,false));
				
			//	ramResourceSpec = resourceMapper.selectByName(instanceType.toLowerCase()+"_ram");
			//	ramResourceSpec.setName(Message.getMessage(ramTypeName,locale,false));
			}
			if(null != diskResourceSpec){
				diskResourceSpec = resourceMapper.selectByName(volumeType);
				//diskResourceSpec.setName(Message.getMessage(volumeTypeName,locale,false));
				diskResourceSpec.setName(volumeTypeName);
			}
			if(null != floatingResourceSpec){
				floatingResourceSpec = resourceMapper.selectByName(floatingType);
				floatingResourceSpec.setName(Message.getMessage(floatingipTypeName,locale,false));
			}
		}else{
			for (Quota quota : tenantQuotas) {
				if (null != cpuResourceSpec && ParamConstant.COMPUTE.equalsIgnoreCase(quota.getQuotaType())) {
					List<QuotaDetail> quotaDetails = quotaDetailMapper
							.getQuotaDetailsById(quota.getQuotaDetailsId().split(","));
					for (QuotaDetail quotaDetail : quotaDetails) {
						if ((instanceType.toLowerCase() + "_" + ParamConstant.CORE).equals(quotaDetail.getType())) {
							cpuResourceSpec.setTotal(quotaDetail.getTotal() + cpuResourceSpec.getTotal());
							cpuResourceSpec.setUsed(quotaDetail.getUsed() + cpuResourceSpec.getUsed());
						} else if ((instanceType.toLowerCase() + "_" + ParamConstant.RAM).equals(quotaDetail.getType())) {
							ramResourceSpec.setTotal(quotaDetail.getTotal() + ramResourceSpec.getTotal());
							ramResourceSpec.setUsed(quotaDetail.getUsed() + ramResourceSpec.getUsed());
						}
					}
				} else if (null != diskResourceSpec && ParamConstant.STORAGE.equalsIgnoreCase(quota.getQuotaType())) {
					List<QuotaDetail> quotaDetails = quotaDetailMapper
							.getQuotaDetailsById(quota.getQuotaDetailsId().split(","));
					for (QuotaDetail quotaDetail : quotaDetails) {
						if (quotaDetail.getType().equals(volumeType)) {
							diskResourceSpec.setTotal(quotaDetail.getTotal() + diskResourceSpec.getTotal());
							diskResourceSpec.setUsed(quotaDetail.getUsed() + diskResourceSpec.getUsed());
						}
					}
				} else if (null != floatingResourceSpec && ParamConstant.NETWORK.equalsIgnoreCase(quota.getQuotaType())) {
					List<QuotaDetail> quotaDetails = quotaDetailMapper
							.getQuotaDetailsById(quota.getQuotaDetailsId().split(","));
					for (QuotaDetail quotaDetail : quotaDetails) {
						if (quotaDetail.getType().equals(floatingType)) {
							floatingResourceSpec.setTotal(quotaDetail.getTotal() + floatingResourceSpec.getTotal());
							floatingResourceSpec.setUsed(quotaDetail.getUsed() + floatingResourceSpec.getUsed());
						}
					}
				}
			}
		}
		if(null != cpuResourceSpec){
			cpuResourceSpec.setFree(cpuResourceSpec.getTotal()-cpuResourceSpec.getUsed());
			resourceSpecs.add(cpuResourceSpec);
			ramResourceSpec.setFree(ramResourceSpec.getTotal()-ramResourceSpec.getUsed());
			resourceSpecs.add(ramResourceSpec);
		}
		if(null != diskResourceSpec){
			diskResourceSpec.setFree(diskResourceSpec.getTotal()-diskResourceSpec.getUsed());	
			resourceSpecs.add(diskResourceSpec);
		}
	    if(null != floatingResourceSpec){
	    	floatingResourceSpec.setFree(floatingResourceSpec.getTotal()-floatingResourceSpec.getUsed());
	    	resourceSpecs.add(floatingResourceSpec);
	    }
		return resourceSpecs;
	}
	
	@Override
	public void deleteTenantQuota(TokenOs ostoken,String tenantId) throws BusinessException{
		List<Quota> quotas = quotaMapper.selectAllByTenantId(tenantId);
		if(Util.isNullOrEmptyList(quotas))
			return;
		List<String> quotaDetailsId = new ArrayList<String>();
		List<String> quotasId = new ArrayList<String>();

		for(Quota quota : quotas){
			quotasId.add(quota.getId());
			String quotaDetailId = quota.getQuotaDetailsId();
			if(Util.isNullOrEmptyValue(quotaDetailId))
				continue;
			quotaDetailsId.addAll(Util.stringToList(quotaDetailId, ","));
		}
		if(0 != quotaDetailsId.size())
			quotaDetailMapper.deleteByIds(quotaDetailsId);
		quotaMapper.deleteByIds(quotasId);
	}
	
	@Override
	public Quota updateComputeQuota(String updateBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException{

		Locale locale = new Locale(ostoken.getLocale());
		String computeQuotaUpdateBody = makeComputeQuotaUpdateBody(updateBody,locale);
		AuthServiceImpl authService =  new AuthServiceImpl();
		authService.setCloudconfig(cloudconfig);
		TokenOs adminToken = null;
		try {
			adminToken = authService.createDefaultAdminOsToken();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e);
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
		}
		
		String region = ostoken.getCurrentRegion();
		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/os-quota-sets/");
		sb.append(ostoken.getTenantid());

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
       
		Map<String, String> rs = httpClient.httpDoPut(sb.toString(), headers, computeQuotaUpdateBody);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		Quota quota = new Quota();
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			quota = getComputeQuota(rs,ostoken.getTenantid(),locale);
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = httpClient.httpDoPut(sb.toString(), headers, computeQuotaUpdateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			quota = getComputeQuota(rs,ostoken.getTenantid(),locale);
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_COMPUTE_QUOTA_UPDATE_FAILED,httpCode,locale);
		}

		storeQuotaToDB(quota);
		return quota;
		
	}
	
	@Override
	public Quota updateNetworkQuota(String updateBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException{
		Locale locale = new Locale(ostoken.getLocale());
		String networkQuotaUpdateBody = makeNetworkQuotaUpdateBody(updateBody,locale);
		AuthServiceImpl authService =  new AuthServiceImpl();
		authService.setCloudconfig(cloudconfig);
		TokenOs adminToken = null;
		try {
			adminToken = authService.createDefaultAdminOsToken();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
		}
		
		String region = ostoken.getCurrentRegion();
		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/os-quota-sets/");
		sb.append(ostoken.getTenantid());

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		Map<String, String> rs = httpClient.httpDoPut(sb.toString(), headers, networkQuotaUpdateBody);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		Quota quota = new Quota();
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			quota = getNetworkQuota(rs,ostoken.getTenantid(),locale);
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = httpClient.httpDoPut(sb.toString(), headers, networkQuotaUpdateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			quota = getNetworkQuota(rs,ostoken.getTenantid(),locale);
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_NETWORK_QUOTA_UPDATE_FAILED,httpCode,locale);
		}

		storeQuotaToDB(quota);
		return quota;
		
	}
	
	@Override
	public Quota updateStoragrQuota(String updateBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException{
		Locale locale = new Locale(ostoken.getLocale());
		String storageQuotaUpdateBody = makeStorageQuotaUpdateBody(updateBody,locale);
		AuthServiceImpl authService =  new AuthServiceImpl();
		authService.setCloudconfig(cloudconfig);
		TokenOs adminToken = null;
		try {
			adminToken = authService.createDefaultAdminOsToken();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
		}
		
		String region = ostoken.getCurrentRegion();
		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/os-quota-sets/");
		sb.append(ostoken.getTenantid());

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		Map<String, String> rs = httpClient.httpDoPut(sb.toString(), headers, storageQuotaUpdateBody);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		Quota quota = new Quota();
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			quota = getStorageQuota(rs,ostoken.getTenantid(),locale);
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = httpClient.httpDoPut(sb.toString(), headers, storageQuotaUpdateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		    failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			quota = getStorageQuota(rs,ostoken.getTenantid(),locale);
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_STORAGE_QUOTA_UPDATE_FAILED,httpCode,locale);
		}
		storeQuotaToDB(quota);
		return quota;
		
	}
	
	@Override
	public void setQuotaSharedStatus(String tenantId) {

		List<Quota> quotas = quotaMapper.selectAllByTenantId(tenantId);
		if(Util.isNullOrEmptyList(quotas))
			return;
		for(Quota quota : quotas){
			quota.setShared(false);
		}
		quotaMapper.insertOrUpdateBatch(quotas);
	}
	
	@Override
	public void updateQuota(String resourceType, TokenOs ostoken, boolean bAdd, int used) {
	    if(0 == used)
	    	return;
		String tenantId = ostoken.getTenantid();
//		QuotaDetail quotaDetail = quotaDetailMapper.selectByResourceType(tenantId, resourceName);
		QuotaDetail quotaDetail = quotaDetailMapper.selectByResourceType(tenantId, resourceType);
		if (null != quotaDetail) {
			if (true == bAdd)
				quotaDetail.setUsed(quotaDetail.getUsed() + used);
			else
				quotaDetail.setUsed(quotaDetail.getUsed() - used);
			quotaDetailMapper.updateByPrimaryKeySelective(quotaDetail);
		}
	}
	
	@Override
	public void updateTenantResourcesQuota(List<String> resourceQuotaTypes, Map<String,Integer> resourceQuotas,TokenOs ostoken, boolean bAdd) {
		if(Util.isNullOrEmptyList(resourceQuotaTypes))
			return;
		String tenantId = ostoken.getTenantid();
		List<QuotaDetail> quotaDetails = quotaDetailMapper.findQuotaDetailsByTypes(tenantId, resourceQuotaTypes);
		if(quotaDetails.size() != resourceQuotas.size())
			return;
		for(QuotaDetail quotaDetail : quotaDetails){
			Integer used = resourceQuotas.get(quotaDetail.getType());
			if(null == used)
				continue;
			if (true == bAdd)
				quotaDetail.setUsed(quotaDetail.getUsed() + used);
			else
				quotaDetail.setUsed(quotaDetail.getUsed() - used);
		}
		quotaDetailMapper.insertOrUpdateBatch(quotaDetails);
	}
	

	@Override
	public void updateQuotaTotal(String resourceType, TokenOs ostoken, int total) {
		String tenantId = ostoken.getTenantid();
		QuotaDetail quotaDetail = quotaDetailMapper.selectByResourceType(tenantId, resourceType);
		if (null != quotaDetail) {
			quotaDetail.setTotal(total);
			quotaDetailMapper.updateByPrimaryKeySelective(quotaDetail);
		}
	}

	@Override
	public Quota createQuota(TokenOs ostoken, String quotaType, List<String> quotaDetailTypes, List<String> units,
			List<Integer> totals) throws BusinessException {

		if (quotaDetailTypes.size() != units.size() || quotaDetailTypes.size() != totals.size())
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));

		List<QuotaDetail> quotaDetails = new ArrayList<QuotaDetail>();
		List<String> quotaDetailIDs = new ArrayList<String>();
		for (int index = 0; index < quotaDetailTypes.size(); ++index) {
			QuotaDetail quotaDetail = new QuotaDetail();
			quotaDetail.setId(Util.makeUUID());
			quotaDetail.setTenantId(ostoken.getTenantid());
			quotaDetail.setTotal(totals.get(index));
			quotaDetail.setUnit(units.get(index));
			quotaDetail.setType(quotaDetailTypes.get(index));
			quotaDetails.add(quotaDetail);
			quotaDetailIDs.add(quotaDetail.getId());
		}

		Quota quota = quotaMapper.selectQuota(ostoken.getTenantid(), quotaType);
		if (null == quota) {
			quota = new Quota();
			quota.setId(Util.makeUUID());
			quota.setTenantId(ostoken.getTenantid());
			quota.setQuotaType(quotaType);
			quota.setData(quotaDetails);
			quota.makeQuotaDetailsId();
			quotaMapper.insertSelective(quota);
			quotaDetailMapper.addQuotaDetailsBatch(quotaDetails);
		} else {
			// update quota detail
            List<QuotaDetail> newCreatedQuotaDetails = new ArrayList<QuotaDetail>();
//          quota.setData(quotaDetails);
//			quota.makeQuotaDetailsId();
			for (QuotaDetail quotaDetail : quotaDetails) {
				QuotaDetail existedQuotaDetail = quotaDetailMapper.selectByResourceType(quotaDetail.getTenantId(),
						quotaDetail.getType());
				if (null != existedQuotaDetail) {
					quotaDetail.setUsed(existedQuotaDetail.getUsed());
					quotaDetailMapper.updateByPrimaryKeySelective(quotaDetail);
				} else {
					quotaDetailMapper.insertSelective(quotaDetail);
					newCreatedQuotaDetails.add(quotaDetail);
				}
			}
			quotaDetails.addAll(newCreatedQuotaDetails);
			quota.setData(quotaDetails);
			quota.makeQuotaDetailsId();
			quotaMapper.updateByPrimaryKeySelective(quota);
		}
		return quota;
	}

	@Override
	public void createTenantQuota(String tenantId,Locale locale) throws BusinessException{

		String computeType = Message.getMessage(Message.CS_QUOTA_COMPUTE_TYPE, locale,false);
		String networkType = Message.getMessage(Message.CS_QUOTA_NETWORK_TYPE, locale,false);
		String storageType = Message.getMessage(Message.CS_QUOTA_STORAGE_TYPE, locale,false);
		
		QuotaTemplate quotaTemplate = null;
		Tenant tenant = tenantMapper.selectByPrimaryKey(tenantId);
		if(null == tenant)
			quotaTemplate = quotaTemplateMapper.selectDefaultTemplate();
		else{
			String templateId = tenant.getQuota_template_id();
			if(Util.isNullOrEmptyValue(templateId))
				quotaTemplate = quotaTemplateMapper.selectDefaultTemplate();
			else
				quotaTemplate = quotaTemplateMapper.selectByPrimaryKey(templateId);
		}
		if(null == quotaTemplate)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
		Quota computeQuota = new Quota();
		computeQuota.setId(Util.makeUUID());
		computeQuota.setQuotaType(ParamConstant.COMPUTE);
		computeQuota.setQuotaTypeName(computeType);
		computeQuota.setTenantId(tenantId);
		computeQuota.setShared(true);
		createComputeQuota(quotaTemplate,computeQuota,tenantId,locale);

		Quota networkQuota = new Quota();
		networkQuota.setId(Util.makeUUID());
		networkQuota.setQuotaType(ParamConstant.NETWORK);
		networkQuota.setQuotaTypeName(networkType);
		networkQuota.setTenantId(tenantId);
		networkQuota.setShared(true);
		createNetworkQuota(quotaTemplate,networkQuota,tenantId,locale);
		
		Quota storageQuota = new Quota();
		storageQuota.setId(Util.makeUUID());
		storageQuota.setQuotaType(ParamConstant.STORAGE);
		storageQuota.setQuotaTypeName(storageType);
		storageQuota.setTenantId(tenantId);
		storageQuota.setShared(true);
		createStorageQuota(quotaTemplate,storageQuota,tenantId,locale);
	}
	
	@Override
	public void createDefaultQuota(TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		String computeType = Message.getMessage(Message.CS_QUOTA_COMPUTE_TYPE, locale,false);
		String networkType = Message.getMessage(Message.CS_QUOTA_NETWORK_TYPE, locale,false);
		String storageType = Message.getMessage(Message.CS_QUOTA_STORAGE_TYPE, locale,false);
		
		String tenantId = ostoken.getTenantid();
		QuotaTemplate quotaTemplate = quotaTemplateMapper.selectDefaultTemplate();
	//	Quota computeQuota = quotaMapper.selectQuota(tenantId, computeType);
		Quota computeQuota = quotaMapper.selectQuota(tenantId, ParamConstant.COMPUTE);
		if(null == computeQuota){
			computeQuota = new Quota();
			computeQuota.setId(Util.makeUUID());
			computeQuota.setQuotaType(ParamConstant.COMPUTE);
			computeQuota.setQuotaTypeName(computeType);
			computeQuota.setTenantId(tenantId);
			computeQuota.setShared(true);
			createComputeQuota(quotaTemplate,computeQuota,tenantId,locale);
			/*
			if(false == createComputeQuota(quotaTemplate,computeQuota,tenantId,locale)){
				createDefaultComputeQuota(computeQuota,tenantId,locale);
			}*/
		}
		
		Quota networkQuota = quotaMapper.selectQuota(tenantId, ParamConstant.NETWORK);
		if(null == networkQuota){
			networkQuota = new Quota();
			networkQuota.setId(Util.makeUUID());
			networkQuota.setQuotaType(ParamConstant.NETWORK);
			networkQuota.setQuotaTypeName(networkType);
			networkQuota.setTenantId(tenantId);
			networkQuota.setShared(true);
			// floatingip Quota
			createNetworkQuota(quotaTemplate,networkQuota,tenantId,locale);
			/*
			if(false == createNetworkQuota(quotaTemplate,networkQuota,tenantId,locale)){
				createDefaultNetworkQuota(networkQuota,tenantId,locale);
			}*/
		
		}
		
		Quota storageQuota = quotaMapper.selectQuota(tenantId, ParamConstant.STORAGE);
		if (null == storageQuota) {
			storageQuota = new Quota();
			storageQuota.setId(Util.makeUUID());
			storageQuota.setQuotaType(ParamConstant.STORAGE);
			storageQuota.setQuotaTypeName(storageType);
			storageQuota.setTenantId(tenantId);
			storageQuota.setShared(true);
			// volume Quota
			createStorageQuota(quotaTemplate,storageQuota,tenantId,locale);
			/*
			if(false == createStorageQuota(quotaTemplate,storageQuota,tenantId,locale)){
				this.createDefaultStorageQuota(storageQuota, tenantId, locale);
			}
			*/
		}
	}
	
	@Override
	public List<QuotaTemplate> getQuotaTemplates(TokenOs ostoken) throws BusinessException{
		return quotaTemplateMapper.selectAll();
	}
	
	@Override
	public QuotaTemplate getQuotaTemplate(String id,TokenOs ostoken) throws BusinessException{
		QuotaTemplate template = quotaTemplateMapper.selectByPrimaryKey(id);
		List<Tenant> tenants = tenantMapper.selectByQuotaTemplateId(id);
		template.setTenants(tenants);
		
		List<TemplateService> services = templateServiceMapper.selectAll();
		if(Util.isNullOrEmptyList(services))
			return template;
		
		List<QuotaField> computeQuotaFields = new ArrayList<QuotaField>();
		List<QuotaField> storageQuotaFields = new ArrayList<QuotaField>();
		List<QuotaField> networkQuotaFields = new ArrayList<QuotaField>();
		for(TemplateService service : services){
			if(service.getService_code().equals(ParamConstant.COMPUTE))
				computeQuotaFields = quotaFieldMapper.selectByServiceIdAndTemplateId(service.getService_id(), id);
			else if(service.getService_code().equals(ParamConstant.STORAGE))
				storageQuotaFields = quotaFieldMapper.selectByServiceIdAndTemplateId(service.getService_id(), id);
			else if(service.getService_code().equals(ParamConstant.NETWORK))
				networkQuotaFields = quotaFieldMapper.selectByServiceIdAndTemplateId(service.getService_id(), id);
		}
		
		List<String> computeFiledIds = new ArrayList<String>();
		Map<String,QuotaField> computeFieldMap = new HashMap<String,QuotaField>();
		for(QuotaField field : computeQuotaFields){
			computeFiledIds.add(field.getField_id());
			computeFieldMap.put(field.getField_id(), field);
		}
		List<TemplateField> computeFields = null;
		if(!Util.isNullOrEmptyList(computeFiledIds))
			computeFields = templateFieldMapper.selectListByIds(computeFiledIds);
		
		List<String> storageFiledIds = new ArrayList<String>();
		Map<String,QuotaField> storageFieldMap = new HashMap<String,QuotaField>();
		for(QuotaField field : storageQuotaFields){
			storageFiledIds.add(field.getField_id());
			storageFieldMap.put(field.getField_id(), field);
		}
		List<TemplateField> storageFields = null;
		if(!Util.isNullOrEmptyList(storageFiledIds))
			storageFields = templateFieldMapper.selectListByIds(storageFiledIds);
		
		List<String> networkFiledIds = new ArrayList<String>();
		Map<String,QuotaField> networkFieldMap = new HashMap<String,QuotaField>();
		for(QuotaField field : networkQuotaFields){
			networkFiledIds.add(field.getField_id());
			networkFieldMap.put(field.getField_id(), field);
		}
		List<TemplateField> networkFields = null;
		if(!Util.isNullOrEmptyList(networkFiledIds))
			networkFields = templateFieldMapper.selectListByIds(networkFiledIds);
		
		Locale locale = new Locale(ostoken.getLocale());
		makeComputeFieldsName(computeFields,locale);
		makeStorgaeFieldsName(storageFields,locale);
		makeNetworkFieldsName(networkFields,locale);
		
		if(!Util.isNullOrEmptyList(computeFields)){
			for(TemplateField field : computeFields){
				field.setMax(computeFieldMap.get(field.getField_id()).getMax());
			}	
		}
		
		if(!Util.isNullOrEmptyList(storageFields)){
			for(TemplateField field : storageFields){
				field.setMax(storageFieldMap.get(field.getField_id()).getMax());
			}
		}
		
		if(!Util.isNullOrEmptyList(networkFields)){
			for(TemplateField field : networkFields){
				field.setMax(networkFieldMap.get(field.getField_id()).getMax());
			}	
		}
		
		template.setComputeFields(computeFields);
		template.setStorageFields(storageFields);
		template.setNetworkFields(networkFields);
		
		return template;
	}
	
	@Override
	public QuotaTemplate getQuotaTemplateFields(TokenOs ostoken) throws BusinessException{
		List<TemplateService> services = templateServiceMapper.selectAll();
		if(Util.isNullOrEmptyList(services))
			return new QuotaTemplate();
	
		List<TemplateField> computeFields = new ArrayList<TemplateField>();
		List<TemplateField> storageFields = new ArrayList<TemplateField>();
		List<TemplateField> networkFields = new ArrayList<TemplateField>();
		for(TemplateService service : services){
			if(service.getService_code().equals(ParamConstant.COMPUTE))
				computeFields = templateFieldMapper.selectFiledsByServiceId(service.getService_id());
			else if(service.getService_code().equals(ParamConstant.STORAGE))
				storageFields = templateFieldMapper.selectFiledsByServiceId(service.getService_id());
			else if(service.getService_code().equals(ParamConstant.NETWORK))
				networkFields = templateFieldMapper.selectFiledsByServiceId(service.getService_id());
		}
		
		Locale locale = new Locale(ostoken.getLocale());
		makeComputeFieldsName(computeFields,locale);
		makeStorgaeFieldsName(storageFields,locale);
		makeNetworkFieldsName(networkFields,locale);
		QuotaTemplate template = new QuotaTemplate();
		template.setComputeFields(computeFields);
		template.setStorageFields(storageFields);
		template.setNetworkFields(networkFields);
		return template;
	}
	
	private void makeComputeFieldsName(List<TemplateField> computeFields,Locale locale){
		if(Util.isNullOrEmptyList(computeFields))
			return;
		Map<String,String> typeNameMap = new HashMap<String,String>();
		List<HostAggregate> aggregates = hostAggregateMapper.selectAll();
		for(HostAggregate aggregate : aggregates){
			if(Util.isNullOrEmptyValue(aggregate.getServiceId()))
				continue;
			typeNameMap.put(aggregate.getAvailabilityZone(), StringHelper.ncr2String(aggregate.getName()));
		}
		String coreType = "CS_CORE_TYPE_NAME";
		String ramType  = "CS_RAM_TYPE_NAME";
		for (TemplateField field : computeFields) {
			String type = field.getField_code().substring(0, field.getField_code().indexOf('_'));
			String name = typeNameMap.get(type);
			if (null != name) {
				if (field.getField_code().contains(ParamConstant.RAM)) {
					if (locale.getLanguage().contains("zh"))
						field.setName(name + Message.getMessage(Message.CS_MEMORY_NAME, locale, false));
					else
						field.setName(name + " " + Message.getMessage(Message.CS_MEMORY_NAME, locale, false));
				} else {
					if (locale.getLanguage().contains("zh"))
						field.setName(name + Message.getMessage(Message.CS_CPU_NAME, locale, false));
					else
						field.setName(name + " " + Message.getMessage(Message.CS_CPU_NAME, locale, false));
				}
			} else {
				if (field.getField_code().contains(ParamConstant.CORE)) {
					String typeName = coreType.replaceFirst("TYPE", type.toUpperCase());
					field.setName(Message.getMessage(typeName, locale, false));
				} else {
					String typeName = ramType.replaceFirst("TYPE", type.toUpperCase());
					field.setName(Message.getMessage(typeName, locale, false));
				}
			}
		}
	}
	
	private void makeStorgaeFieldsName(List<TemplateField> storageFields,Locale locale){
		if(Util.isNullOrEmptyList(storageFields))
			return;
		List<VolumeType> volumeTypes = volumeTypeMapper.selectAll();
		for (TemplateField field : storageFields) {
			if (false == field.getRating()) {
				if (field.getField_code().equals(ParamConstant.SNAPSHOT))
					field.setName(Message.getMessage(Message.CS_VOLUME_SNAPSHOT_NAME, locale, false));
				else if (field.getField_code().equals(ParamConstant.IMAGE)) {
					field.setName(Message.getMessage(Message.CS_PRIVATE_IMAGE_NAME, locale, false));
				} else if (field.getField_code().equals(ParamConstant.BACKUP)) {
					field.setName(Message.getMessage(Message.CS_VOLUME_BACKUP_NAME, locale, false));
				} else {
					field.setName(field.getField_code());
				}
			} else {
				String typeName = getVolumeTypeVisibleName(volumeTypes, field.getField_code());
				if (Util.isNullOrEmptyValue(typeName))
					typeName = field.getField_code();
				field.setName(typeName);
			}

		}
	}
	
	private void makeNetworkFieldsName(List<TemplateField> networkFields,Locale locale){
		if(Util.isNullOrEmptyList(networkFields))
			return;
		String networkType = "CS_FLOTINGIP_TYPE_NAME";
		for(TemplateField field : networkFields){
			String typeName = networkType.replaceFirst("TYPE", field.getField_code().toUpperCase());
			field.setName(Message.getMessage(typeName, locale,false));
		}
	}
	
	
	
	@Override
	public QuotaTemplate createQuotaTemplate(String createBody,TokenOs ostoken) throws BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(createBody);
		} catch (Exception e) {
		    log.error("error",e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		} 
		
		QuotaTemplate template = new QuotaTemplate();
		String name = rootNode.path(ResponseConstant.NAME).textValue();
		String description = rootNode.path(ResponseConstant.DESCRIPTION).textValue();
		
		String templateId = Util.makeUUID();
		template.setId(templateId);
		template.setName(name);
		template.setDescription(description);
		template.setDefaultFlag(false);
		template.setMillionSeconds(Util.getCurrentMillionsecond());
		JsonNode fieldNodes = rootNode.path(ResponseConstant.FIELDS);
		List<QuotaField> quotaFields = new ArrayList<QuotaField>();
		for(int index = 0; index < fieldNodes.size(); ++index){
			JsonNode fieldNode = fieldNodes.get(index);
			QuotaField quotaField = new QuotaField();
			quotaField.setId(Util.makeUUID());
			quotaField.setField_id(fieldNode.path(ResponseConstant.FIELD_ID).textValue());
			quotaField.setService_id(fieldNode.path(ResponseConstant.SERVICE_ID).textValue());
			quotaField.setMax(fieldNode.path(ResponseConstant.MAX).intValue());
			quotaField.setTemplate_id(templateId);
			quotaField.setUsed(0);
			quotaFields.add(quotaField);
		}
		quotaFieldMapper.insertOrUpdateBatch(quotaFields);
		quotaTemplateMapper.insertSelective(template);
		return template;
	}
	
	@Override
	public void applyQuotaTemplate(String id,String tenantId,TokenOs ostoken) throws BusinessException{
		Tenant tenant = tenantMapper.selectByPrimaryKey(tenantId);
		if(null == tenant)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		tenant.setQuota_template_id(id);
		tenantMapper.insertOrUpdate(tenant);
		
		List<QuotaField> fields = quotaFieldMapper.selectByTemplateId(id);
		if(Util.isNullOrEmptyList(fields))
			return;
		List<TemplateService> services = templateServiceMapper.selectAll(); 
		String computeServiceId = null;
		String storageServiceId = null;
		String networkServiceId = null;
		for(TemplateService service : services){
			if(service.getService_code().equals(ParamConstant.COMPUTE))
				computeServiceId = service.getService_id();
			else if(service.getService_code().equals(ParamConstant.NETWORK))
				networkServiceId = service.getService_id();
			else if(service.getService_code().equals(ParamConstant.STORAGE))
				storageServiceId = service.getService_id();
		}
		
		List<QuotaDetail> computeQuotas = new ArrayList<QuotaDetail>();
		List<QuotaDetail> storageQuotas = new ArrayList<QuotaDetail>();
		List<QuotaDetail> networkQuotas = new ArrayList<QuotaDetail>();
		Quota computeQuota = null;
		Quota storageQuota = null;
		Quota networkQuota = null;
		
		List<Quota> quotas = quotaMapper.selectAllByTenantId(tenantId);
		for(Quota quota : quotas){
		    if(quota.getQuotaType().equals(ParamConstant.COMPUTE)) {
		    	computeQuota = quota;
		    	computeQuotas = quotaDetailMapper.getQuotaDetailsById(quota.getQuotaDetailsId().split(","));
		    }
		    else if(quota.getQuotaType().equals(ParamConstant.STORAGE)){
		    	storageQuota = quota;
		    	storageQuotas = quotaDetailMapper.getQuotaDetailsById(quota.getQuotaDetailsId().split(","));
		    }
		    else if(quota.getQuotaType().equals(ParamConstant.NETWORK)){
		    	networkQuota = quota;
		    	networkQuotas = quotaDetailMapper.getQuotaDetailsById(quota.getQuotaDetailsId().split(","));
		    }   	
		}
		
		Locale locale = new Locale(ostoken.getLocale());
		List<QuotaDetail> appendQuotas = new ArrayList<QuotaDetail>();
		List<String> appendComputeQuotaIds = new ArrayList<String>();
		List<String> appendStorageQuotaIds = new ArrayList<String>();
		List<String> appendNetworkQuotaIds = new ArrayList<String>();
		for(QuotaField field : fields){
			if(true == setExistingDetailQuota(field,computeQuotas,locale))
				continue;
			if(true == setExistingDetailQuota(field,storageQuotas,locale))
				continue;
			if(true == setExistingDetailQuota(field,networkQuotas,locale))
				continue;
			if(field.getService_id().equals(computeServiceId)){
				TemplateField templateField = templateFieldMapper.selectByPrimaryKey(field.getField_id());
				if(null == templateField)
					continue;
				QuotaDetail quota = new QuotaDetail();
				quota.setId(Util.makeUUID());
				quota.setTenantId(tenantId);
				quota.setTotal(field.getMax());
				quota.setType(templateField.getField_code());
				quota.setFieldId(field.getField_id());
				quota.setUsed(0);
				if(templateField.getField_code().contains(ParamConstant.CORE))
					quota.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, locale,false));
				else
					quota.setUnit(Message.getMessage(Message.CS_CAPACITY_UNIT,locale, false));
				appendQuotas.add(quota);
				appendComputeQuotaIds.add(quota.getId());
			}else if(field.getService_id().equals(networkServiceId)){
				TemplateField templateField = templateFieldMapper.selectByPrimaryKey(field.getField_id());
				if(null == templateField)
					continue;
				QuotaDetail quota = new QuotaDetail();
				quota.setId(Util.makeUUID());
				quota.setTenantId(tenantId);
				quota.setTotal(field.getMax());
				quota.setUsed(0);
				quota.setType(templateField.getField_code());
				quota.setUnit(Message.getMessage(Message.CS_COUNT_UNIT,locale, false));
				quota.setFieldId(field.getField_id());
				appendQuotas.add(quota);
				appendNetworkQuotaIds.add(quota.getId());
			}else if(field.getService_id().equals(storageServiceId)){
				TemplateField templateField = templateFieldMapper.selectByPrimaryKey(field.getField_id());
				if(null == templateField)
					continue;
				QuotaDetail quota = new QuotaDetail();
				quota.setId(Util.makeUUID());
				quota.setTenantId(tenantId);
				quota.setTotal(field.getMax());
				quota.setUsed(0);
				quota.setType(templateField.getField_code());
				if(templateField.getField_code().equals(ParamConstant.SNAPSHOT) || templateField.getField_code().equals(ParamConstant.IMAGE) || templateField.getField_code().equals(ParamConstant.BACKUP))
					quota.setUnit(Message.getMessage(Message.CS_COUNT_UNIT,locale, false));
				else 
					quota.setUnit(Message.getMessage(Message.CS_CAPACITY_UNIT,locale, false));
				quota.setFieldId(field.getField_id());
				appendQuotas.add(quota);
				appendStorageQuotaIds.add(quota.getId());
			}
		}

		if(!Util.isNullOrEmptyList(appendComputeQuotaIds)){
			if(null != computeQuota){
				computeQuota.setQuotaDetailsId(Util.getAppendedIds(computeQuota.getQuotaDetailsId(), appendComputeQuotaIds));
				quotaMapper.insertOrUpdate(computeQuota);
			}else{
				Quota newComputeQuota = new Quota();
				String computeType = Message.getMessage(Message.CS_QUOTA_COMPUTE_TYPE, locale,false);
				newComputeQuota.setId(Util.makeUUID());
				newComputeQuota.setQuotaType(ParamConstant.COMPUTE);
				newComputeQuota.setQuotaTypeName(computeType);
				newComputeQuota.setTenantId(tenantId);
				newComputeQuota.setShared(true);
				newComputeQuota.setQuotaDetailsId(Util.listToString(appendComputeQuotaIds,','));
				quotaMapper.insertSelective(newComputeQuota);
			}
		}
		
         if(!Util.isNullOrEmptyList(appendStorageQuotaIds)){
        	 if(null != storageQuota){
        		 storageQuota.setQuotaDetailsId(Util.getAppendedIds(storageQuota.getQuotaDetailsId(), appendStorageQuotaIds));
 				 quotaMapper.insertOrUpdate(storageQuota);
 			}else{
 				Quota newStorageQuota = new Quota();
 				String storageType = Message.getMessage(Message.CS_QUOTA_STORAGE_TYPE,locale, false);
 				newStorageQuota.setId(Util.makeUUID());
 				newStorageQuota.setQuotaType(ParamConstant.STORAGE);
 				newStorageQuota.setQuotaTypeName(storageType);
 				newStorageQuota.setTenantId(tenantId);
 				newStorageQuota.setShared(true);
 				newStorageQuota.setQuotaDetailsId(Util.listToString(appendStorageQuotaIds, ','));
 				quotaMapper.insertSelective(newStorageQuota);
 			}
		}

        if(!Util.isNullOrEmptyList(appendNetworkQuotaIds)){
        	if(null != networkQuota){
        		networkQuota.setQuotaDetailsId(Util.getAppendedIds(networkQuota.getQuotaDetailsId(), appendNetworkQuotaIds));
				quotaMapper.insertOrUpdate(networkQuota);
			}else{
				String networkType = Message.getMessage(Message.CS_QUOTA_NETWORK_TYPE, locale,false);
				Quota newNetworkQuota = new Quota();
				newNetworkQuota.setId(Util.makeUUID());
				newNetworkQuota.setQuotaType(ParamConstant.NETWORK);
				newNetworkQuota.setQuotaTypeName(networkType);
				newNetworkQuota.setTenantId(tenantId);
				newNetworkQuota.setShared(true);
				newNetworkQuota.setQuotaDetailsId(Util.listToString(appendNetworkQuotaIds, ','));
				quotaMapper.insertSelective(newNetworkQuota);
			}
        }
        if(!Util.isNullOrEmptyList(appendQuotas))
        	quotaDetailMapper.insertOrUpdateBatch(appendQuotas);
	}
	
	private Boolean setExistingDetailQuota(QuotaField field,List<QuotaDetail> details,Locale locale)  throws BusinessException{
		if(Util.isNullOrEmptyList(details))
			return false;
		for(QuotaDetail detail : details){
			if(detail.getFieldId().equals(field.getField_id())){
				 if(field.getMax() <= detail.getUsed())
					throw new ResourceBusinessException(Message.CS_QUOTA_TEMPLATE_IS_INVALID,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
				detail.setTotal(field.getMax());
				return true;
			}
		}
		return false;
	}
	
	@Override
	public QuotaTemplate updateQuotaTemplate(String id,String body,TokenOs ostoken) throws BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(body);
		} catch (Exception e) {
		    log.error("error",e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		} 

		String name = null;
		String description = null;
		Boolean defaultFlag = null;
		if(!rootNode.path(ResponseConstant.NAME).isMissingNode())
			name = rootNode.path(ResponseConstant.NAME).textValue();
		if(!rootNode.path(ResponseConstant.DESCRIPTION).isMissingNode())
			description = rootNode.path(ResponseConstant.DESCRIPTION).textValue();
		if(!rootNode.path(ResponseConstant.FLAG).isMissingNode())
			defaultFlag = rootNode.path(ResponseConstant.FLAG).booleanValue();
		
		QuotaTemplate template = quotaTemplateMapper.selectByPrimaryKey(id);
		if(null == template)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));

		if(null != name){
			template.setName(name);
			checkName(name,ostoken);
		}
		if(null != description)
			template.setDescription(description);
		if(null != defaultFlag){
			resetTemplatesFlag();
			template.setDefaultFlag(defaultFlag);
		}
		quotaTemplateMapper.insertOrUpdate(template);
		
		return template;
	}
	
	private void checkName(String name, TokenOs ostoken) throws BusinessException {
		List<QuotaTemplate> quotaTemplates = quotaTemplateMapper.selectAll();
		if (Util.isNullOrEmptyList(quotaTemplates))
			return;
		for (QuotaTemplate quotaTemplate : quotaTemplates) {
			if (name.equals(quotaTemplate.getName()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_NAME_IS_SAME,ParamConstant.BAD_REQUEST_RESPONSE_CODE, new Locale(ostoken.getLocale()));
		}
	}
	
	private void resetTemplatesFlag(){
		List<QuotaTemplate> quotaTemplates = quotaTemplateMapper.selectAll();
		if (Util.isNullOrEmptyList(quotaTemplates))
			return;
		for (QuotaTemplate quotaTemplate : quotaTemplates){
			quotaTemplate.setDefaultFlag(false);
		}
		quotaTemplateMapper.insertOrUpdateBatch(quotaTemplates);
	}
	
	@Override
	public void deleteQuotaTemplate(String id,TokenOs ostoken) throws BusinessException{
		QuotaTemplate template = quotaTemplateMapper.selectByPrimaryKey(id);
		if(null == template)
			return;
		if(true == template.getDefaultFlag())
			throw new ResourceBusinessException(Message.CS_TEMPLATE_IS_DEFAULT,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
        List<Tenant> tenants = tenantMapper.selectByQuotaTemplateId(id);
        if(null != tenants && 0 != tenants.size())
			throw new ResourceBusinessException(Message.CS_TEMPLATE_IS_USING,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
        quotaFieldMapper.deleteByTemplateId(id);
        quotaTemplateMapper.deleteByPrimaryKey(id);
	}
	
	@Override
	public QuotaTemplate createSystemTemplate(TokenOs ostoken) throws BusinessException{
		QuotaTemplate template  = new QuotaTemplate();
		template.setName(cloudconfig.getSystemDefaultPriceName());
		template.setDefaultFlag(true);
		template.setId(Util.makeUUID());
		template.setMillionSeconds(Util.getCurrentMillionsecond());
		
		List<TemplateService> services = templateServiceMapper.selectAll();
		if(Util.isNullOrEmptyList(services))
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		
		
		List<QuotaField> quotaFields = new ArrayList<QuotaField>();
		for (TemplateService service : services) {
			if (service.getService_code().equals(ParamConstant.EXTEND_SERVICE))
				continue;
			if (service.getService_code().equals(ParamConstant.IMAGE))
				continue;
			TemplateService quotaService = new TemplateService();
			quotaService.setService_id(service.getService_id());
			List<TemplateField> fields = templateFieldMapper.selectFiledsByServiceId(service.getService_id());
			if (service.getService_code().equals(ParamConstant.COMPUTE)) {
				for (TemplateField field : fields) {
					quotaFields.add(makeComputeQuotaFieldInfo(template.getId(),field));
				}
			} else if (service.getService_code().equals(ParamConstant.STORAGE)) {
				for (TemplateField field : fields) {
					quotaFields.add(makeStorageQuotaFieldInfo(template.getId(),field));
				}
			} else if (service.getService_code().equals(ParamConstant.NETWORK)) {
				for (TemplateField field : fields) {
					quotaFields.add(makeNetworkQuotaFieldInfo(template.getId(),field));
				}
			}
		}
		quotaFieldMapper.insertOrUpdateBatch(quotaFields);
		quotaTemplateMapper.insertOrUpdate(template);
		Tenant adminTenant = tenantMapper.selectByPrimaryKey(cloudconfig.getOs_authtenantid());
		if(null != adminTenant){
			adminTenant.setQuota_template_id(template.getId());
			tenantMapper.updateByPrimaryKeySelective(adminTenant);
		}
		return template;
	}
	
	private QuotaField makeComputeQuotaFieldInfo(String templateId,TemplateField field){
		QuotaField quota = new QuotaField();
		quota.setField_id(field.getField_id());
		quota.setService_id(field.getService_id());
		quota.setId(Util.makeUUID());
		quota.setTemplate_id(templateId);
		quota.setUsed(0);
		if(field.getField_code().contains(ParamConstant.CORE)){
			quota.setMax(Integer.parseInt(cloudconfig.getCoreQuota()));
		}else{
			quota.setMax(Integer.parseInt(cloudconfig.getRamQuota()));
		}
		return quota;
	}
	
	private QuotaField makeStorageQuotaFieldInfo(String templateId,TemplateField field){
		QuotaField quota = new QuotaField();
		quota.setField_id(field.getField_id());
		quota.setService_id(field.getService_id());
		quota.setId(Util.makeUUID());
		quota.setTemplate_id(templateId);
		quota.setUsed(0);
		quota.setMax(Integer.parseInt(cloudconfig.getSystemVolumeQuota()));
		return quota;
	}
	
	private QuotaField makeNetworkQuotaFieldInfo(String templateId,TemplateField field){
		QuotaField quota = new QuotaField();
		quota.setField_id(field.getField_id());
		quota.setService_id(field.getService_id());
		quota.setId(Util.makeUUID());
		quota.setTemplate_id(templateId);
		quota.setUsed(0);
		quota.setMax(Integer.parseInt(cloudconfig.getSystemFloatingIPQuota()));
		return quota;
	}
	
	
	private String getVolumeTypeVisibleName(List<VolumeType> volumeTypes,String type){
		if(Util.isNullOrEmptyList(volumeTypes))
			return null;
		for(VolumeType volumeType : volumeTypes){
			if(type.equals(volumeType.getName()))
				return volumeType.getDisplayName();
		}
		return null;
	}
	
	@Override
	public List<Quota> getHardQuotas(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		// HttpClientForOsBase osClient = new HttpClientForOsBase(cloudconfig);
		// TokenOs ot = osClient.getToken();
		// token should have Regioninfo

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		// url=RequestUrlHelper.createFullUrl(url+"/os-hosts", null);

		// HashMap<String, String> headers = new HashMap<String, String>();
		// headers.put("X-Auth-Token", ot.getTokenid());

		String tenantId = ostoken.getTenantid();
		StringBuffer tenantUrl = new StringBuffer();
		tenantUrl.append(url);
		tenantUrl.append("/os-quota-sets/");
		tenantUrl.append(tenantId);
		tenantUrl.append("/detail");
		// tenantUrl.append("/detail"); //maybe we need to change it on MITAKA

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		// get tenant quota
		Map<String, String> rs = httpClient.httpDoGet(tenantUrl.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		List<Quota> quotas = null;
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				JsonNode quotaNode = rootNode.path(ResponseConstant.QUOTA_SET);
				quotas = getQuotaInfo(quotaNode, tenantId,locale);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = httpClient.httpDoGet(tenantUrl.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				JsonNode quotaNode = rootNode.path(ResponseConstant.QUOTA_SET);
				quotas = getQuotaInfo(quotaNode, tenantId,locale);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_HARD_QUOTA_GET_FAILED,httpCode,locale);
		}

		return quotas;
	}

	@Override
	public Quota getHardQuota(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {
		Locale locale = new Locale(ostoken.getLocale());
	//	String type = Message.getMessage(Message.CS_QUOTA_COMPUTE_TYPE, locale,false);
		Quota quota = quotaMapper.selectQuota(ostoken.getTenantid(),ParamConstant.COMPUTE);
		if (null == quota) {
			quota = new Quota();
			quota.setId(Util.makeUUID());
			quota.setQuotaType(ParamConstant.COMPUTE);
			quota.setTenantId(ostoken.getTenantid());
			createDefaultComputeQuota(quota,ostoken.getTenantid(),locale);
		}
		return quota;
	}

	@Override
	public List<Quota> getDefaultQuotas(TokenOs ostoken) throws BusinessException{
		List<Quota> quotas = getDefaultHardQuotas(ostoken);
		Quota networkQuota = getNetworkQuotas(null,ostoken);
		quotas.add(networkQuota);
		storeQuotasToDB(quotas);
		return quotas;
	}
	
	private List<Quota> getDefaultHardQuotas(TokenOs ostoken)
			throws BusinessException {
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		String tenantId = ostoken.getTenantid(); 
		StringBuffer tenantUrl = new StringBuffer();
		tenantUrl.append(url);
		tenantUrl.append("/os-quota-sets/");
		tenantUrl.append(tenantId);
		tenantUrl.append("/defaults");

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		// get tenant quota
		Map<String, String> rs = httpClient.httpDoGet(tenantUrl.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		List<Quota> quotas = null;
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				quotas = getQuotas(rs, tenantId,locale);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = httpClient.httpDoGet(tenantUrl.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_HARD_QUOTA_GET_FAILED,httpCode,locale);
			try {
				quotas = getQuotas(rs, tenantId,locale);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_HARD_QUOTA_GET_FAILED,httpCode,locale);
		}
	
		return quotas;
	}
	
	@Override
	public Quota getNetworkQuotas(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		// url=RequestUrlHelper.createFullUrl(url+"/os-hosts", null);

		// HashMap<String, String> headers = new HashMap<String, String>();
		// headers.put("X-Auth-Token", ot.getTokenid());
		String tenantId = ostoken.getTenantid(); // TODO
		StringBuffer tenantUrl = new StringBuffer();
		tenantUrl.append(url);
		tenantUrl.append("/v2.0/quotas/");
		tenantUrl.append(tenantId);
		// tenantUrl.append("/detail"); //maybe we need to change it on MITAKA

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		// get tenant quota
		Map<String, String> rs = httpClient.httpDoGet(tenantUrl.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		Quota quota = null;
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				quota = getNetworkQuota(rs,tenantId,locale);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = httpClient.httpDoGet(tenantUrl.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_NETWORK_QUOTA_GET_FAILED,httpCode,locale);
			try {
				quota = getNetworkQuota(rs,tenantId,locale);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_NETWORK_QUOTA_GET_FAILED,httpCode,locale);
		}
		return quota;
	}

	@Override
	public Quota getStorageQuotas(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUME, region).getPublicURL();
		// url=RequestUrlHelper.createFullUrl(url+"/os-hosts", null);

		// HashMap<String, String> headers = new HashMap<String, String>();
		// headers.put("X-Auth-Token", ot.getTokenid());

//		String admTokenId = ""; // TODO to get the admin token id
		String tenantId = ostoken.getTenantid();
		StringBuffer tenantUrl = new StringBuffer();
		tenantUrl.append(url);
		tenantUrl.append("/os-quota-sets/");
		tenantUrl.append(ostoken.getTenantid());
		// tenantUrl.append("/detail"); //maybe we need to change it on MITAKA

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		// get tenant quota
		Map<String, String> rs = httpClient.httpDoGet(tenantUrl.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		Quota quota = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				quota = getStorageQuotaInfo(rs, tenantId,locale);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = httpClient.httpDoGet(tenantUrl.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_STORAGE_QUOTA_GET_FAILED,httpCode,locale);
			try {
				quota = getStorageQuotaInfo(rs, tenantId,locale);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_STORAGE_QUOTA_GET_FAILED,httpCode,locale);
		}

		return quota;
	}

	@Override
	public Boolean setHardQuota(Map<String, String> paramMap, TokenOs ostoken) {
		Pool pool = new Pool(); // TODO to build pool from paramMap

		// if(null == pool)
		// return false;

		Integer floating_ips = null;
		if (null != pool.getFloatingIPNumbers()) {
			int temFloatingIps = 0;
			boolean bHasValue = false;
			for (Map.Entry<String, Integer> entry : pool.getFloatingIPNumbers().entrySet()) {
				if (null != entry.getValue()) {
					bHasValue = true;
					temFloatingIps += entry.getValue();
				}
			}
			if (bHasValue)
				floating_ips = temFloatingIps;
		}

		Integer cores = null != pool.getCores() ? pool.getCores() : null;
		Integer ram = null != pool.getRamSize() ? pool.getRamSize() : null;

		// ram = 1;
		if (null == floating_ips && null == cores && null == ram)
			return true;

		String region = ostoken.getCurrentRegion();// we should get the
													// regioninfo by the
		// guiTokenId
		// userTenantId TODO to get the userTenantId by the database
		/// v2.1/​{admin_tenant_id}�?os-quota-sets/​{tenant_id}�?

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("X-Auth-Token", ostoken.getTokenid());

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuffer urlBuffer = new StringBuffer();
		urlBuffer.append(url);
		urlBuffer.append("/os-quota-sets/");
		// urlBuffer.append(userTenantId);
		urlBuffer.append(ostoken.getTokenid());
		url = RequestUrlHelper.createFullUrl(urlBuffer.toString(), null);

		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.setSerializationInclusion(Include.NON_EMPTY);
		QuotaJSON quotaBody = new QuotaJSON(true, ram, floating_ips, cores);
		String jsonStr = "";
		try {
			jsonStr = mapper.writeValueAsString(quotaBody);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}

		// set hard quota
		System.out.println(ostoken.getTokenid());
		System.out.println(url.toString());
		Map<String, String> rs = httpClient.httpDoPost(url.toString(), headers, jsonStr);

		System.out.println("httpcode:" + rs.get("httpcode"));
		System.out.println("jsonbody:" + rs.get("jsonbody"));
		if (Integer.parseInt(rs.get("httpcode")) != ParamConstant.NORMAL_SYNC_RESPONSE_CODE) {
			System.out.println("wo cha:request failed");
			return false;
		}

		return true;
	}

	private Quota makeComputeQuota(JsonNode quotaNode, String tenantId,Locale locale) {
		Quota quotaCompute = new Quota();
		quotaCompute.setId(Util.makeUUID());
		quotaCompute.setTenantId(tenantId);
		quotaCompute.setQuotaType(Message.CS_QUOTA_COMPUTE_TYPE);
		quotaCompute.setQuotaTypeName(Message.getMessage(quotaCompute.getQuotaType(),locale, false));
		// instance Quota
//		QuotaDetail quotaInstance = new QuotaDetail();
//		JsonNode instanceNode = quotaNode.path(ResponseConstant.INSTANCES_QUOTA);
//		quotaInstance.setId(Util.makeUUID());
//		quotaInstance.setTenantId(tenantId);
//		quotaInstance.setName(ParamConstant.INSTANCE);
//		quotaInstance.setTotal(instanceNode.path(ResponseConstant.LIMIT).intValue());
//		quotaInstance.setUsed(instanceNode.path(ResponseConstant.IN_USE).intValue());
//		quotaInstance.setReserved(instanceNode.path(ResponseConstant.RESERVED).intValue());
//		quotaInstance.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//		quotaInstance.setType(Message.getMessage(Message.CS_INSTANCE_NAME, false));
//		quotaCompute.addQuotaDetail(quotaInstance);

		// vcpu Quota
		QuotaDetail quotaCPU = new QuotaDetail();
		JsonNode cpuNode = quotaNode.path(ResponseConstant.CORES);
		quotaCPU.setId(Util.makeUUID());
		quotaCPU.setTenantId(tenantId);
	//	quotaCPU.setName(ParamConstant.CORE);
		quotaCPU.setTotal(cpuNode.path(ResponseConstant.LIMIT).intValue());
		quotaCPU.setUsed(cpuNode.path(ResponseConstant.IN_USE).intValue());
		quotaCPU.setReserved(cpuNode.path(ResponseConstant.RESERVED).intValue());
		quotaCPU.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, locale,false));
		quotaCPU.setType(ParamConstant.VCPUS);
		quotaCPU.setTypeName(Message.getMessage(quotaCPU.getType(),locale, false));
		quotaCompute.addQuotaDetail(quotaCPU);

		// ram Quota
		QuotaDetail quotaRam = new QuotaDetail();
		JsonNode ramNode = quotaNode.path(ResponseConstant.RAM);
		quotaRam.setId(Util.makeUUID());
		quotaRam.setTenantId(tenantId);
//		quotaRam.setName(ParamConstant.RAM);
		quotaRam.setTotal(ramNode.path(ResponseConstant.LIMIT).intValue());
		quotaRam.setUsed(ramNode.path(ResponseConstant.IN_USE).intValue());
		quotaRam.setReserved(ramNode.path(ResponseConstant.RESERVED).intValue());
		quotaRam.setUnit(Message.getMessage(Message.CS_CAPACITY_UNIT,locale,false));
		quotaRam.setType(Message.CS_MEMORY_NAME);
		quotaRam.setTypeName(Message.getMessage(quotaRam.getType(),locale,false));
		quotaCompute.addQuotaDetail(quotaRam);

		//keypair Quota
//		QuotaDetail quotaKeypair = new QuotaDetail();
//		JsonNode keypairNode = quotaNode.path(ResponseConstant.KEY_PAIRS);
//		quotaKeypair.setId(Util.makeUUID());
//		quotaKeypair.setTenantId(tenantId);
//		quotaKeypair.setName(ParamConstant.KEYPAIR);
//		quotaKeypair.setTotal(keypairNode.path(ResponseConstant.LIMIT).intValue());
//		quotaKeypair.setUsed(keypairNode.path(ResponseConstant.IN_USE).intValue());
//		quotaKeypair.setReserved(keypairNode.path(ResponseConstant.RESERVED).intValue());
//		quotaKeypair.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//		quotaKeypair.setType(Message.getMessage(Message.CS_KEYPAIR_NAME, false));
//		quotaCompute.addQuotaDetail(quotaKeypair);
		
		quotaCompute.makeQuotaDetailsId();
		return quotaCompute;
	}


//	private Quota makeNetworkeQuota(JsonNode quotaNode, String tenantId) {
//		Quota quotaNetwork = new Quota();
//		quotaNetwork.setId(Util.makeUUID());
//		quotaNetwork.setTenantId(tenantId);
//		quotaNetwork.setQuotaType(Message.getMessage(Message.CS_QUOTA_NETWORK_TYPE, false));
//
//		// floating_ip Quota
//		QuotaDetail quotaFloatingIP = new QuotaDetail();
//		JsonNode floatingIPNode = quotaNode.path(ResponseConstant.FLOATING_IPS);
//		quotaFloatingIP.setId(Util.makeUUID());
//		quotaFloatingIP.setTenantId(tenantId);
//		quotaFloatingIP.setTotal(floatingIPNode.path(ResponseConstant.LIMIT).intValue());
//		quotaFloatingIP.setUsed(floatingIPNode.path(ResponseConstant.IN_USE).intValue());
//		quotaFloatingIP.setReserved(floatingIPNode.path(ResponseConstant.RESERVED).intValue());
//		quotaFloatingIP.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//		quotaFloatingIP.setType(Message.getMessage(Message.CS_FLOATINGIP_NAME, false));
//		quotaNetwork.addQuotaDetail(quotaFloatingIP);
//		
//		QuotaDetail quotaSecurityGroup = new QuotaDetail();
//		JsonNode securityGroupNode = quotaNode.path(ResponseConstant.SECURITY_GROUPS);
//		quotaSecurityGroup.setId(Util.makeUUID());
//		quotaSecurityGroup.setTenantId(tenantId);
//		quotaSecurityGroup.setTotal(securityGroupNode.path(ResponseConstant.LIMIT).intValue());
//		quotaSecurityGroup.setUsed(securityGroupNode.path(ResponseConstant.IN_USE).intValue());
//		quotaSecurityGroup.setReserved(securityGroupNode.path(ResponseConstant.RESERVED).intValue());
//		quotaSecurityGroup.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//		quotaSecurityGroup.setType(Message.getMessage(Message.CS_SECURITY_GROUP_NAME, false));
//		quotaNetwork.addQuotaDetail(quotaSecurityGroup);
//		
//		quotaNetwork.makeQuotaDetailsId();
//		return quotaNetwork;
//	}

//	private Quota makeOtherQuota(JsonNode quotaNode, String tenantId) {
//		Quota quotaOther = new Quota();
//		quotaOther.setId(Util.makeUUID());
//		quotaOther.setTenantId(tenantId);
//		quotaOther.setQuotaType(Message.getMessage(Message.CS_QUOTA_OTHER_TYPE, false));
//		// security group Quota
//		QuotaDetail quotaSecurityGroup = new QuotaDetail();
//		JsonNode securityGroupNode = quotaNode.path(ResponseConstant.SECURITY_GROUPS);
//		quotaSecurityGroup.setId(Util.makeUUID());
//		quotaSecurityGroup.setTenantId(tenantId);
//		quotaSecurityGroup.setTotal(securityGroupNode.path(ResponseConstant.LIMIT).intValue());
//		quotaSecurityGroup.setUsed(securityGroupNode.path(ResponseConstant.IN_USE).intValue());
//		quotaSecurityGroup.setReserved(securityGroupNode.path(ResponseConstant.RESERVED).intValue());
//		quotaSecurityGroup.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//		quotaSecurityGroup.setType(Message.getMessage(Message.CS_SECURITY_GROUP_NAME, false));
//		quotaOther.addQuotaDetail(quotaSecurityGroup);
//
//		// keypair Quota
//		QuotaDetail quotaKeypair = new QuotaDetail();
//		JsonNode keypairNode = quotaNode.path(ResponseConstant.KEY_PAIRS);
//		quotaKeypair.setId(Util.makeUUID());
//		quotaKeypair.setTenantId(tenantId);
//		quotaKeypair.setTotal(keypairNode.path(ResponseConstant.LIMIT).intValue());
//		quotaKeypair.setUsed(keypairNode.path(ResponseConstant.IN_USE).intValue());
//		quotaKeypair.setReserved(keypairNode.path(ResponseConstant.RESERVED).intValue());
//		quotaKeypair.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//		quotaKeypair.setType(Message.getMessage(Message.CS_KEYPAIR_NAME, false));
//		quotaOther.addQuotaDetail(quotaKeypair);
//		quotaOther.makeQuotaDetailsId();
//		return quotaOther;
//	}

	private List<Quota> getQuotas(Map<String, String> rs,String tenantId,Locale locale) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode quotaNode = rootNode.path(ResponseConstant.QUOTA_SET);
		return getDefaultQuotaInfo(quotaNode,tenantId,locale);
	}
	
	private List<Quota> getDefaultQuotaInfo(JsonNode quotaNode, String tenantId,Locale locale) {
		if (null == quotaNode)
			return null;
		List<Quota> quotas = new ArrayList<Quota>();

		Quota quotaCompute = new Quota();
		quotaCompute.setId(Util.makeUUID());
		quotaCompute.setTenantId(tenantId);
		quotaCompute.setQuotaType(ParamConstant.COMPUTE);
		quotaCompute.setQuotaTypeName(Message.getMessage(Message.CS_QUOTA_COMPUTE_TYPE,locale, false));
		/*instances*/
//		QuotaDetail quotaInstance = new QuotaDetail();
//		quotaInstance.setId(Util.makeUUID());
//		quotaInstance.setTenantId(tenantId);
//		quotaInstance.setTotal(quotaNode.path(ResponseConstant.INSTANCES).intValue());
//		quotaInstance.setUsed(0);
//		quotaInstance.setReserved(0);
//		quotaInstance.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//		quotaInstance.setType(Message.getMessage(Message.CS_INSTANCE_NAME, false));
//		quotaCompute.addQuotaDetail(quotaInstance);
		/*core*/
		QuotaDetail quotaCPU = new QuotaDetail();
		quotaCPU.setId(Util.makeUUID());
		quotaCPU.setTenantId(tenantId);
		quotaCPU.setTotal(quotaNode.path(ResponseConstant.CORES).intValue());
		quotaCPU.setUsed(0);
		quotaCPU.setReserved(0);
		quotaCPU.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, locale,false));
		quotaCPU.setType(ParamConstant.VCPUS);
		quotaCPU.setTypeName(Message.getMessage(Message.CS_CPU_NAME, locale,false));
		quotaCompute.addQuotaDetail(quotaCPU);
		/*ram*/
		QuotaDetail quotaRam = new QuotaDetail();
		quotaRam.setId(Util.makeUUID());
		quotaRam.setTenantId(tenantId);
		quotaRam.setTotal(quotaNode.path(ResponseConstant.RAM).intValue() / ParamConstant.MB);
		quotaRam.setUsed(0);
		quotaRam.setReserved(0);
		quotaRam.setUnit(Message.getMessage(Message.CS_CAPACITY_UNIT, locale,false));
		quotaRam.setType(ParamConstant.RAM);
		quotaRam.setTypeName(Message.getMessage(Message.CS_MEMORY_NAME,locale, false));
		quotaCompute.addQuotaDetail(quotaRam);
		quotas.add(quotaCompute);
		
		
//		Quota quotaNetwork = new Quota();
//		quotaNetwork.setId(Util.makeUUID());
//		quotaNetwork.setTenantId(tenantId);
//		quotaNetwork.setQuotaType(Message.getMessage(Message.CS_QUOTA_NETWORK_TYPE, false));
//		// floating_ip Quota
//		QuotaDetail quotaFloatingIP = new QuotaDetail();
//		quotaFloatingIP.setId(Util.makeUUID());
//		quotaFloatingIP.setTenantId(tenantId);
//		quotaFloatingIP.setTotal(quotaNode.path(ResponseConstant.FLOATING_IPS).intValue());
//		quotaFloatingIP.setUsed(0);
//		quotaFloatingIP.setReserved(0);
//		quotaFloatingIP.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//		quotaFloatingIP.setType(Message.getMessage(Message.CS_FLOATINGIP_NAME, false));
//		quotaNetwork.addQuotaDetail(quotaFloatingIP);
//		quotaNetwork.makeQuotaDetailsId();
//		quotas.add(quotaNetwork);
		

//		Quota quotaOther = new Quota();
//		quotaOther.setId(Util.makeUUID());
//		quotaOther.setTenantId(tenantId);
//		quotaOther.setQuotaType(Message.getMessage(Message.CS_QUOTA_OTHER_TYPE, false));
//		// security group Quota
//		QuotaDetail quotaSecurityGroup = new QuotaDetail();
//		quotaSecurityGroup.setId(Util.makeUUID());
//		quotaSecurityGroup.setTenantId(tenantId);
//		quotaSecurityGroup.setTotal(quotaNode.path(ResponseConstant.SECURITY_GROUPS).intValue());
//		quotaSecurityGroup.setUsed(0);
//		quotaSecurityGroup.setReserved(0);
//		quotaSecurityGroup.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//		quotaSecurityGroup.setType(Message.getMessage(Message.CS_SECURITY_GROUP_NAME, false));
//		quotaOther.addQuotaDetail(quotaSecurityGroup);
//		// keypair Quota
//		QuotaDetail quotaKeypair = new QuotaDetail();
//		quotaKeypair.setId(Util.makeUUID());
//		quotaKeypair.setTenantId(tenantId);
//		quotaKeypair.setTotal(quotaNode.path(ResponseConstant.KEY_PAIRS).intValue());
//		quotaKeypair.setUsed(0);
//		quotaKeypair.setReserved(0);
//		quotaKeypair.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//		quotaKeypair.setType(Message.getMessage(Message.CS_KEYPAIR_NAME, false));
//		quotaOther.addQuotaDetail(quotaKeypair);
//		quotaOther.makeQuotaDetailsId();
//		quotas.add(quotaOther);
		

		return quotas;
	}
	
	private List<Quota> getQuotaInfo(JsonNode quotaNode, String tenantId,Locale locale) {
		if (null == quotaNode)
			return null;
		List<Quota> quotas = new ArrayList<Quota>();

		quotas.add(makeComputeQuota(quotaNode, tenantId,locale));
	//	quotas.add(makeNetworkeQuota(quotaNode, tenantId));
	//	quotas.add(makeOtherQuota(quotaNode, tenantId));

		return quotas;
	}

	private Quota getStorageQuotaInfo(Map<String, String> rs, String tenantId,Locale locale)
			throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode quotaNode = rootNode.path(ResponseConstant.QUOTA_SET);

		Quota quotaStorage = new Quota();
		quotaStorage.setId(Util.makeUUID());
		quotaStorage.setTenantId(tenantId);
		quotaStorage.setQuotaType(Message.getMessage(Message.CS_QUOTA_STORAGE_TYPE, locale,false));

		// volume snapshot
//		QuotaDetail quotaSnapshot = new QuotaDetail();
//		quotaSnapshot.setId(Util.makeUUID());
//		quotaSnapshot.setTotal(quotaNode.path(ResponseConstant.SNAPSHOTS).intValue());
//		quotaSnapshot.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//		quotaSnapshot.setType(Message.getMessage(Message.CS_CAPACITY_VOLUME_SNAPSHOT_NAME, false)); // TODO
//		quotaStorage.addQuotaDetail(quotaSnapshot);

		// volumes
		QuotaDetail quotaVolume = new QuotaDetail();
		quotaVolume.setId(Util.makeUUID());
		quotaVolume.setTenantId(tenantId);
	//	quotaVolume.setName(ParamConstant.VOLUME);
		quotaVolume.setTotal(quotaNode.path(ResponseConstant.VOLUMES).intValue());
		quotaVolume.setUsed(0);
		quotaVolume.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, locale,false));
		quotaVolume.setType(ParamConstant.STORAGE);
		quotaVolume.setTypeName(Message.getMessage(Message.CS_VOLUME_COUNT,locale, false));
		quotaVolume.setNotDisplay(true);
		quotaStorage.addQuotaDetail(quotaVolume);
		
		// backups
//		QuotaDetail quotaBackup = new QuotaDetail();
//		quotaBackup.setId(Util.makeUUID());
//		quotaBackup.setTenantId(tenantId);
//		quotaBackup.setName(ParamConstant.BACKUP);
//		quotaBackup.setTotal(quotaNode.path(ResponseConstant.BACKUPS).intValue());
//		quotaBackup.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//		quotaBackup.setType(Message.getMessage(Message.CS_BACKUP_COUNT, false));
//		quotaBackup.setNotDisplay(true);
//		quotaStorage.addQuotaDetail(quotaBackup);
		
		quotaStorage.makeQuotaDetailsId();
		return quotaStorage;

	}

//	private Quota getTenantVolumeQuota(String tenantId, String region, String url, HttpServletResponse response)
//			throws BusinessException {
//
//		/// ​{admin_tenant_id}�?os-quota-sets/​{tenant_id}�?detail
//		HttpClientForOsBase osClient = new HttpClientForOsBase(cloudconfig);
//		TokenOs ot = osClient.getToken();
//		StringBuffer tenantUrl = new StringBuffer();
//		tenantUrl.append(url);
//		tenantUrl.append("/os-quota-sets/");
//		tenantUrl.append(ot.getTenantid());
//		// tenantUrl.append("/detail"); //maybe we need to change it on MITAKA
//
//		HashMap<String, String> headers = new HashMap<String, String>();
//		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ot.getTokenid());
//
//		// get tenant quota
//		Map<String, String> rs = httpClient.httpDoGet(tenantUrl.toString(), headers);
//		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//		if (null != response)
//			response.setStatus(httpCode);
//
//		Quota quota = null;
//		switch (httpCode) {
//		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
//			try {
//				quota = getStorageQuotaInfo(rs, tenantId);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
//			}
//			break;
//		}
//		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
//			String tokenid = "";// TODO reget the token id
//			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
//			rs = httpClient.httpDoGet(tenantUrl.toString(), headers);
//			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//			if (null != response)
//				response.setStatus(httpCode);
//			try {
//				quota = getStorageQuotaInfo(rs, tenantId);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
//			}
//			break;
//		}
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE);
//		default:
//			throw new ResourceBusinessException(Message.CS_STORAGE_QUOTA_GET_FAILED);
//		}
//
//		return quota;
//	}

//	private List<Quota> getTenantHardQuota(String tenantId, String region, String url, HttpServletResponse response)
//			throws BusinessException {
//
//		/// ​{admin_tenant_id}�?os-quota-sets/​{tenant_id}�?detail
//		HttpClientForOsBase osClient = new HttpClientForOsBase(cloudconfig);
//		TokenOs ot = osClient.getToken();
//		StringBuffer tenantUrl = new StringBuffer();
//		tenantUrl.append(url);
//		tenantUrl.append("/os-quota-sets/");
//		tenantUrl.append(ot.getTenantid());
//		// tenantUrl.append("/detail"); //maybe we need to change it on MITAKA
//
//		HashMap<String, String> headers = new HashMap<String, String>();
//		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ot.getTokenid());
//
//		// get tenant quota
//		Map<String, String> rs = httpClient.httpDoGet(tenantUrl.toString(), headers);
//		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//		if (null != response)
//			response.setStatus(httpCode);
//
//		List<Quota> quotas = null;
//		switch (httpCode) {
//		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
//			try {
//				ObjectMapper mapper = new ObjectMapper();
//				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
//				JsonNode quotaNode = rootNode.path(ResponseConstant.QUOTA_SET);
//				quotas = getQuotaInfo(quotaNode, tenantId);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
//			}
//			break;
//		}
//		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
//			String tokenid = "";// TODO reget the token id
//			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
//			rs = httpClient.httpDoGet(tenantUrl.toString(), headers);
//			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//			if (null != response)
//				response.setStatus(httpCode);
//			try {
//				ObjectMapper mapper = new ObjectMapper();
//				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
//				JsonNode quotaNode = rootNode.path(ResponseConstant.QUOTA_SET);
//				quotas = getQuotaInfo(quotaNode, tenantId);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
//			}
//			break;
//		}
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE);
//		default:
//			throw new ResourceBusinessException(Message.CS_HARD_QUOTA_GET_FAILED);
//		}
//
//		return quotas;
//	}

	@Override
	public void checkQuota(List<QuotaDetail> leastQuotaDetails, String tenantId,String availabilityZone,Locale locale) throws BusinessException {
		if (Util.isNullOrEmptyList(leastQuotaDetails))
			return;
		int leastCore = leastQuotaDetails.get(0).getTotal();
		int leastRam = leastQuotaDetails.get(1).getTotal();
		int leastdisk = leastQuotaDetails.get(2).getTotal();
		String volumeType = leastQuotaDetails.get(2).getType();
		List<Quota> tenantQuotas = quotaMapper.selectAllByTenantId(tenantId);
		if (Util.isNullOrEmptyList(tenantQuotas))
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
		VolumeType volumeTypeFromDB = volumeTypeMapper.selectByPrimaryKey(volumeType);
		if(null == volumeTypeFromDB)
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
		int availabledCore = -1;
		int availabledRam = -1;
		int availabledDisk = -1;
		Boolean shared = true;
		for (Quota quota : tenantQuotas) {
			shared = quota.getShared();
		//	if (Message.getMessage(Message.CS_QUOTA_COMPUTE_TYPE, false).equals(quota.getQuotaType()) ||
		//			Message.getMessage(Message.CS_QUOTA_STORAGE_TYPE, false).equals(quota.getQuotaType())) {
			if (ParamConstant.COMPUTE.equals(quota.getQuotaType()) || ParamConstant.STORAGE.equals(quota.getQuotaType())) {
			List<QuotaDetail> quotaDetails = quotaDetailMapper
						.getQuotaDetailsById(quota.getQuotaDetailsId().split(","));
				for (QuotaDetail quotaDetail : quotaDetails) {
					if ((availabilityZone+"_"+ParamConstant.CORE).equalsIgnoreCase(quotaDetail.getType())) {
						availabledCore += (quotaDetail.getTotal() - quotaDetail.getUsed());
					} else if ((availabilityZone+"_"+ParamConstant.RAM).equalsIgnoreCase(quotaDetail.getType())){
						availabledRam += (quotaDetail.getTotal() - quotaDetail.getUsed());
					}else{
//						String volumeTypeName = "capacity";
//						if(null != volumeTypeFromDB)
						String volumeTypeName = volumeTypeFromDB.getName();
						if(volumeTypeName.equalsIgnoreCase(quotaDetail.getType())){
							availabledDisk += (quotaDetail.getTotal() - quotaDetail.getUsed());
						}
					}
				}
			}
		}
		if (leastCore > availabledCore || leastRam > availabledRam*ParamConstant.MB || leastdisk > availabledDisk){
			if(false == shared){
				throw new ResourceBusinessException(Message.CS_RESOURCE_OVER_QUOTA,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
			}else{
				throw new ResourceBusinessException(Message.CS_QUOTA_INSUFFICIENT,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);	
			}
		}
		checkResource(tenantId,leastCore,leastRam,leastdisk,volumeTypeFromDB.getName(),availabilityZone,locale);

	}

	@Override
	public void checkResource(String tenantId,int coreSize,int ramSize,int diskSize, String backendName,String availabilityZone,Locale locale) throws ResourceBusinessException{
		List<String> resourceNames = new ArrayList<String>();
		String coreName = null;
		String ramName = null;
		if(!Util.isNullOrEmptyValue(availabilityZone)){
			coreName = availabilityZone+"_"+ParamConstant.CORE;
			ramName = availabilityZone+"_"+ParamConstant.RAM;
			resourceNames.add(coreName);
			resourceNames.add(ramName);
		}
		if(!Util.isNullOrEmptyValue(backendName))
			resourceNames.add(backendName);
	//	ResourceSpec cpuResource = resourceMapper.selectByName(ParamConstant.CORE);
	//	ResourceSpec ramResource = resourceMapper.selectByName(ParamConstant.RAM);
	//	ResourceSpec diskResource = resourceMapper.selectByName(backendName);
        
		List<ResourceSpec> resources = resourceMapper.findResourcesByNames(resourceNames);
		if(Util.isNullOrEmptyList(resources))
			return;
		for(ResourceSpec resource : resources){
			if(resource.getName().equals(coreName)){
				if(coreSize > (resource.getTotal()-resource.getUsed()))
					throw new ResourceBusinessException(Message.CS_ENV_RESOURCE_INSUFFICIENT,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
			}else if(resource.getName().equals(ramName)){
				if(ramSize > (resource.getTotal() - resource.getUsed()))
					throw new ResourceBusinessException(Message.CS_ENV_RESOURCE_INSUFFICIENT,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
			}else{
				if(diskSize > (resource.getTotal() - resource.getUsed()))
					throw new ResourceBusinessException(Message.CS_ENV_RESOURCE_INSUFFICIENT,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
			}
		}
	}
	
	@Override
	public void checkFipResource(String floatingIpType, int count,Locale locale) throws ResourceBusinessException{
		ResourceSpec fipResource = resourceMapper.selectByName(floatingIpType);
		if (null != fipResource) {
			if (count > (fipResource.getTotal() - fipResource.getUsed()))
				throw new ResourceBusinessException(Message.CS_ENV_RESOURCE_INSUFFICIENT,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
	}
	
	@Override
	public void checkResourceQuota(String tenantId,String type,int count,Locale locale) throws BusinessException {
		List<Quota> tenantQuotas = quotaMapper.selectAllByTenantId(tenantId);
		if (Util.isNullOrEmptyList(tenantQuotas))
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
		
		int availabledSnapshot = 0;
		for (Quota quota : tenantQuotas) {
			if(!ParamConstant.STORAGE.equals(quota.getQuotaType()))
				continue;
			List<QuotaDetail> quotaDetails = quotaDetailMapper.getQuotaDetailsById(quota.getQuotaDetailsId().split(","));
			for (QuotaDetail quotaDetail : quotaDetails) {
				if(!quotaDetail.getType().equals(type))
					continue;
				availabledSnapshot += (quotaDetail.getTotal() - quotaDetail.getUsed());
				break;	
			}
		}
		if (count > availabledSnapshot){
			throw new ResourceBusinessException(Message.CS_QUOTA_INSUFFICIENT,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);	
		}
	}
	
//	@Override
//	public void checkResource(TokenOs ostoken, String serviceName, List<String> quotaDetailTypes, List<String> units,
//			List<Integer> totals, List<Host> hosts) throws BusinessException {
//		List<QuotaDetail> qds = this.quotaDetailMapper.selectAll();
//         
//		if (serviceName.equals("compute")) {
//			int usedCPU = 0;
//			int usedMem = 0;
//			for (QuotaDetail qd : qds) {
//				//equals(Message.getMessage(Message.CS_CPU_NAME, false))) 
//				if (qd.getType().contains(ParamConstant.CORE)){
//					usedCPU += qd.getUsed();
//					continue;
//				}
//				//.equals(Message.getMessage(Message.CS_MEMORY_NAME, false))
//				if (qd.getType().contains(ParamConstant.RAM)) {
//					usedMem += qd.getUsed();
//					continue;
//				}
//			}
//			Locale locale = new Locale(ostoken.getLocale());
//			int totalCPU = this.countTotalResource(hosts, "compute", Message.getMessage(Message.CS_CPU_NAME, locale,false));
//			int cpuIndex = quotaDetailTypes.indexOf(Message.getMessage(Message.CS_CPU_NAME, locale, false));
//			int reqCPU = totals.get(cpuIndex);
//			
//			if (reqCPU > (totalCPU - usedCPU)) {
//				throw new ResourceBusinessException(Message.CS_RESOURCE_INSUFFICIENT,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
//			}
//			
//			int totalMem = this.countTotalResource(hosts, "compute", Message.getMessage(Message.CS_MEMORY_NAME, locale, false));
//			int memIndex = quotaDetailTypes.indexOf(Message.getMessage(Message.CS_MEMORY_NAME, locale, false));
//			int reqMem = totals.get(memIndex);
//			if (reqMem > (totalMem - usedMem)) {
//				throw new ResourceBusinessException(Message.CS_RESOURCE_INSUFFICIENT,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
//			}
//		}
//		
//		if (serviceName.equals(ParamConstant.STORAGE)) {
//			for (int index = 0; index < quotaDetailTypes.size(); index++) {
//				int usedVol = 0;
//				int totalVol = this.countTotalResource(hosts, ParamConstant.STORAGE, quotaDetailTypes.get(index));
//				for (QuotaDetail qd : qds) {
//					if (qd.getType().equals(quotaDetailTypes.get(index))) {
//						usedVol += qd.getUsed();
//					}
//				}
//				int reqVol = totals.get(index);
//				if (reqVol > (totalVol - usedVol)) {
//					// throw new ResourceBusinessException(Message.CS_RESOURCE_INSUFFICIENT);
//				}
//			}
//		}
//		
//		if (serviceName.equals(ParamConstant.FLOATINGIP)) {
//			for (int index = 0; index < quotaDetailTypes.size(); index++) {
//				int usedFip = 0;
//				int totalFip = this.countTotalResource(hosts, ParamConstant.FLOATINGIP, quotaDetailTypes.get(index));
//				for (QuotaDetail qd : qds) {
//					if (qd.getType().equals(quotaDetailTypes.get(index))) {
//						usedFip += qd.getUsed();
//					}
//				}
//				int reqFip = totals.get(index);
//				if (reqFip > (totalFip - usedFip)) {
//					// throw new ResourceBusinessException(Message.CS_RESOURCE_INSUFFICIENT);
//				}
//			}
//		}
//		
//	}

//	private int countTotalResource(List<Host> hosts, String serviceName, String type) {
//		int count = 0;
//		for (Host h : hosts) {
//			if (h.getServiceName().equals(serviceName)) {
//				List<HostDetail> hds = h.getHostDetails();
//				for (HostDetail hd : hds) {
//					if (hd.getType().equals(type)) {
//						count += hd.getTotal();
//					}
//				}
//
//			}
//		}
//		return count;
//	}

//	private List<Quota> mergeQuotas(List<Quota> quotos, Quota networkQuota, Quota storageQuota) {
//		List<Quota> mergedQuotas = new ArrayList<Quota>();
//		if (!Util.isNullOrEmptyList(quotos))
//			mergedQuotas = quotos;
//		if (null != networkQuota)
//			mergedQuotas.add(networkQuota);
//
//		if (null != storageQuota)
//			mergedQuotas.add(storageQuota);
//
//		return mergedQuotas;
//	}

	private void storeQuotasToDB(List<Quota> quotas) {
		if (Util.isNullOrEmptyList(quotas))
			return;
		for (Quota quota : quotas) {
			storeQuotaToDB(quota);
		}
	}
	
	private void storeQuotaToDB(Quota quota) {
		List<QuotaDetail> details = quota.getData();
		if (!Util.isNullOrEmptyList(details)) {
			quotaDetailMapper.insertOrUpdateBatch(details);
			List<String> quotaDetailIds = new ArrayList<String>();
			for (QuotaDetail quotaDetail : details) {
				quotaDetailIds.add(quotaDetail.getId());
			//	quotaDetailMapper.insertOrUpdate(quotaDetail);
			}
			quota.setQuotaDetailsId(Util.listToString(quotaDetailIds, ','));
		}
		
		quotaMapper.insertOrUpdate(quota);
//		if (null != quotaMapper.selectByPrimaryKey(quota.getId()))
//			quotaMapper.updateByPrimaryKeySelective(quota);
//		else
//			quotaMapper.insertSelective(quota);
	}

	private void normalQuotaTypeInfo(Quota quota,String computeTypeName,String networkTypeName,String storageTypeName,Locale locale){
		String[] quotaDetailIds = quota.getQuotaDetailsId().split(",");
		if (null == quotaDetailIds || 0 == quotaDetailIds.length)
			return;
		
		List<QuotaDetail> quotaDetails = quotaDetailMapper.getQuotaDetailsById(quotaDetailIds);
		if(quota.getQuotaType().equalsIgnoreCase(ParamConstant.COMPUTE)){
			quota.setQuotaTypeName(computeTypeName);
			if(!Util.isNullOrEmptyList(quotaDetails)){
				String ramType = "CS_RAM_TYPE_NAME";
				String coreType = "CS_CORE_TYPE_NAME";
				for(QuotaDetail quotaDetail : quotaDetails){
					String type = quotaDetail.getType().substring(0, quotaDetail.getType().indexOf('_'));
					HostAggregate aggregate = hostAggregateMapper.selectByZoneName(type);
					if(null != aggregate){
						if(quotaDetail.getType().contains(ParamConstant.RAM)){
							if(locale.getLanguage().contains("zh"))
								quotaDetail.setTypeName(StringHelper.ncr2String(aggregate.getName())+Message.getMessage(Message.CS_MEMORY_NAME, locale,false));
							else
								quotaDetail.setTypeName(StringHelper.ncr2String(aggregate.getName())+" " + Message.getMessage(Message.CS_MEMORY_NAME, locale,false));
						}else{
							if(locale.getLanguage().contains("zh"))
								quotaDetail.setTypeName(StringHelper.ncr2String(aggregate.getName())+Message.getMessage(Message.CS_CPU_NAME, locale,false));
							else
								quotaDetail.setTypeName(StringHelper.ncr2String(aggregate.getName())+" "+Message.getMessage(Message.CS_CPU_NAME, locale,false));	
						}
					}else{
						if(quotaDetail.getType().contains(ParamConstant.RAM)){
							String ramTypeName = ramType.replaceFirst("TYPE", type.toUpperCase());
							quotaDetail.setTypeName(Message.getMessage(ramTypeName, locale,false));
						}else{
							String coreTypeName = coreType.replaceFirst("TYPE", type.toUpperCase());
							quotaDetail.setTypeName(Message.getMessage(coreTypeName, locale,false));
						}	
					}
					
				}
			}
		}else if(quota.getQuotaType().equalsIgnoreCase(ParamConstant.STORAGE)){
			quota.setQuotaTypeName(storageTypeName);
			if(!Util.isNullOrEmptyList(quotaDetails)){
				List<VolumeType> volumeTypes = volumeTypeMapper.selectAll();
			//	String volumeType = "CS_VOLUME_TYPE_NAME";
				for(QuotaDetail quotaDetail : quotaDetails){
					String typeName = null;
					if(quotaDetail.getType().equals(ParamConstant.SNAPSHOT)){
						typeName = Message.getMessage(Message.CS_VOLUME_SNAPSHOT_NAME,locale, false);
					}else if(quotaDetail.getType().equals(ParamConstant.IMAGE)){
						typeName = Message.getMessage(Message.CS_PRIVATE_IMAGE_NAME,locale, false);
				    }else if(quotaDetail.getType().equals(ParamConstant.BACKUP)){
						typeName = Message.getMessage(Message.CS_VOLUME_BACKUP_NAME,locale, false);
				    }else{
				    	typeName = getVolumeTypeVisibleName(volumeTypes,quotaDetail.getType());	
				    }
					if(Util.isNullOrEmptyValue(typeName))
						typeName = quotaDetail.getType();//Message.getMessage(quotaDetail.getType().toUpperCase(), locale,false);
					quotaDetail.setTypeName(typeName);
				//	String volumeTypeName = volumeType.replaceFirst("TYPE", quotaDetail.getType().toUpperCase());
				//	quotaDetail.setTypeName(Message.getMessage(volumeTypeName, locale,false));
				}
			}	
		}else{
			quota.setQuotaTypeName(networkTypeName);
			if(!Util.isNullOrEmptyList(quotaDetails)){
				String floatingipType = "CS_FLOTINGIP_TYPE_NAME";
				for(QuotaDetail quotaDetail : quotaDetails){
					String floatingipTypeName = floatingipType.replaceFirst("TYPE", quotaDetail.getType().toUpperCase());
					quotaDetail.setTypeName(Message.getMessage(floatingipTypeName,locale, false));
				}
			}
		}	
		quota.addQuotaDetail(quotaDetails);
	}
	
	private List<Quota> getQuotasFromDB(String tenantId,Locale locale) {
		List<Quota> quotas = quotaMapper.selectAllByTenantId(tenantId);
		if (Util.isNullOrEmptyList(quotas))
			return null;
		String computeTypeName = Message.getMessage(Message.CS_QUOTA_COMPUTE_TYPE,locale, false);
		String networkTypeName = Message.getMessage(Message.CS_QUOTA_NETWORK_TYPE, locale,false);
		String storageTypeName = Message.getMessage(Message.CS_QUOTA_STORAGE_TYPE,locale, false);
		
		List<Quota> quotasFromDB = new ArrayList<Quota>();
		for (Quota quota : quotas) {
			normalQuotaTypeInfo(quota,computeTypeName,networkTypeName,storageTypeName,locale);
			quotasFromDB.add(quota);
		}
		return quotasFromDB;
	}
	
	private String makeStorageQuotaUpdateBody(String updateBody,Locale locale) throws ResourceBusinessException{
		ObjectMapper mapper = new ObjectMapper();
		StorageQuota quotaInfo = null;
		try {
			quotaInfo = mapper.readValue(updateBody, StorageQuota.class);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.NOT_FOUND_RESPONSE_CODE,locale);
		}
		StorageQuotaJSON storageJSON = new StorageQuotaJSON(quotaInfo);
		JsonHelper<StorageQuotaJSON, String> jsonHelp = new JsonHelper<StorageQuotaJSON, String>();
		return jsonHelp.generateJsonBodyWithoutDefaultValue(storageJSON);
	}
	
	private Quota getStorageQuota(Map<String, String> rs,String tenantId,Locale locale) throws JsonProcessingException, IOException, BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode quotaNode = rootNode.path(ResponseConstant.QUOTA_SET);
		return getStorageQuotaInfo(quotaNode,tenantId,locale);
	}
	
	private Quota getStorageQuotaInfo(JsonNode quotaNode,String tenantId,Locale locale){
		if(null == quotaNode)
			return null;
		
		String type = Message.getMessage(Message.CS_QUOTA_STORAGE_TYPE, locale,false);
		String volumeType = Message.getMessage(Message.CS_VOLUME_COUNT, locale,false);
		String backupType = Message.getMessage(Message.CS_BACKUP_COUNT, locale,false);

		Quota quota = quotaMapper.selectQuota(tenantId, type);
		if(null == quota){
			quota = new Quota();
			quota.setId(Util.makeUUID());
			quota.setTenantId(tenantId);
			quota.setQuotaType(type);
			
			//volumes
			QuotaDetail quotaVolume = new QuotaDetail();
			quotaVolume.setId(Util.makeUUID());
			quotaVolume.setTenantId(tenantId);
		//	quotaVolume.setName(ParamConstant.VOLUME);
			quotaVolume.setTotal(quotaNode.path(ResponseConstant.VOLUMES).intValue());
			quotaVolume.setUnit(Message.getMessage(Message.CS_COUNT_UNIT,locale,false));
			quotaVolume.setType(ParamConstant.STORAGE);
			quotaVolume.setTypeName(volumeType);
			quota.addQuotaDetail(quotaVolume);
			
			//backups
//			QuotaDetail quotaBackup = new QuotaDetail();
//			quotaBackup.setId(Util.makeUUID());
//			quotaBackup.setTenantId(tenantId);
//			quotaBackup.setName(ParamConstant.NAME);
//			quotaBackup.setTotal(quotaNode.path(ResponseConstant.BACKUPS).intValue());
//			quotaBackup.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//			quotaBackup.setType(backupType);
//			quota.addQuotaDetail(quotaBackup);
			
			quota.makeQuotaDetailsId();
			
		}else{
			String[] quotaDetailIds = quota.getQuotaDetailsId().split(",");
			List<QuotaDetail> quotaDetails = quotaDetailMapper.getQuotaDetailsById(quotaDetailIds);
			for(QuotaDetail quotaDetail : quotaDetails){
				if(quotaDetail.getType().equals(volumeType)){
					quotaDetail.setTotal(quotaNode.path(ResponseConstant.VOLUMES).intValue());
				}else if(quotaDetail.getType().equals(backupType)){
					quotaDetail.setTotal(quotaNode.path(ResponseConstant.BACKUPS).intValue());
				}
				quota.addQuotaDetail(quotaDetail);
			}
		}
		return quota;
	}
	
	private String makeNetworkQuotaUpdateBody(String updateBody,Locale locale) throws ResourceBusinessException{
		ObjectMapper mapper = new ObjectMapper();
		NetworkQuota quotaInfo = null;
		try {
			quotaInfo = mapper.readValue(updateBody, NetworkQuota.class);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.NOT_FOUND_RESPONSE_CODE,locale);
		}
		NetworkQuotaJSON networkJSON = new NetworkQuotaJSON(quotaInfo);
		JsonHelper<NetworkQuotaJSON, String> jsonHelp = new JsonHelper<NetworkQuotaJSON, String>();
		return jsonHelp.generateJsonBodyWithoutDefaultValue(networkJSON);
	}
	
	private Quota getNetworkQuota(Map<String, String> rs,String tenantId,Locale locale) throws JsonProcessingException, IOException, BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode quotaNode = rootNode.path(ResponseConstant.QUOTA);
		return getNetworkQuotaInfo(quotaNode,tenantId,locale);
	}
	
	private Quota getNetworkQuotaInfo(JsonNode quotaNode,String tenantId,Locale locale){
		if(null == quotaNode)
			return null;
		
		String type = Message.getMessage(Message.CS_QUOTA_NETWORK_TYPE,locale,false);
		String networkType = Message.getMessage(Message.CS_NETWORK_NAME,locale, false);
		String subnetType = Message.getMessage(Message.CS_SUBNET_NAME, locale,false);
		String routerType = Message.getMessage(Message.CS_ROUTER_NAME, locale,false);
		String portType = Message.getMessage(Message.CS_PORT_NAME,locale, false);
		String securityGroupType = Message.getMessage(Message.CS_SECURITY_GROUP_NAME,locale, false);
		String floatingIPType = Message.getMessage(Message.CS_FLOATINGIP_NAME,locale, false);
	
		Quota quota = quotaMapper.selectQuota(tenantId, type);
		if(null == quota){
			quota = new Quota();
			quota.setId(Util.makeUUID());
			quota.setTenantId(tenantId);
			quota.setQuotaType(type);
			
			//network
//			QuotaDetail quotaNetwork = new QuotaDetail();
//			quotaNetwork.setId(Util.makeUUID());
//			quotaNetwork.setTenantId(tenantId);
//			quotaNetwork.setTotal(quotaNode.path(ResponseConstant.NETWORK).intValue());
//			quotaNetwork.setUsed(0);
//			quotaNetwork.setReserved(0);
//			quotaNetwork.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//			quotaNetwork.setType(networkType);
//			quota.addQuotaDetail(quotaNetwork);
//			
//			//subnet
//			QuotaDetail quotaSubnet = new QuotaDetail();
//			quotaSubnet.setId(Util.makeUUID());
//			quotaSubnet.setTenantId(tenantId);
//			quotaSubnet.setTotal(quotaNode.path(ResponseConstant.SUBNET).intValue());
//			quotaSubnet.setUsed(0);
//			quotaSubnet.setReserved(0);
//			quotaSubnet.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//			quotaSubnet.setType(subnetType);
//			quota.addQuotaDetail(quotaSubnet);
//			
//			//router
//			QuotaDetail quotaRouter = new QuotaDetail();
//			quotaRouter.setId(Util.makeUUID());
//			quotaRouter.setTenantId(tenantId);
//			quotaRouter.setTotal(quotaNode.path(ResponseConstant.ROUTER).intValue());
//			quotaRouter.setUsed(0);
//			quotaRouter.setReserved(0);
//			quotaRouter.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//			quotaRouter.setType(routerType);
//			quota.addQuotaDetail(quotaRouter);
//			
//			//port
//			QuotaDetail quotaPort = new QuotaDetail();
//			quotaPort.setId(Util.makeUUID());
//			quotaPort.setTenantId(tenantId);
//			quotaPort.setTotal(quotaNode.path(ResponseConstant.PORT).intValue());
//			quotaPort.setUsed(0);
//			quotaPort.setReserved(0);
//			quotaPort.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//			quotaPort.setType(portType);
//			quota.addQuotaDetail(quotaPort);
//			
//			//securityGroup
//			QuotaDetail quotaSecurityGroup = new QuotaDetail();
//			quotaSecurityGroup.setId(Util.makeUUID());
//			quotaSecurityGroup.setTenantId(tenantId);
//			quotaSecurityGroup.setTotal(quotaNode.path(ResponseConstant.SECURITY_GROUP).intValue());
//			quotaSecurityGroup.setUsed(0);
//			quotaSecurityGroup.setReserved(0);
//			quotaSecurityGroup.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//			quotaSecurityGroup.setType(securityGroupType);
//			quota.addQuotaDetail(quotaSecurityGroup);
			
			//floatingip
			QuotaDetail quotaFloatingIP = new QuotaDetail();
			quotaFloatingIP.setId(Util.makeUUID());
			quotaFloatingIP.setTenantId(tenantId);
			quotaFloatingIP.setTotal(quotaNode.path(ResponseConstant.FLOATINGIP).intValue());
			quotaFloatingIP.setUsed(0);
			quotaFloatingIP.setReserved(0);
			quotaFloatingIP.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, locale,false));
			quotaFloatingIP.setType(floatingIPType);
			quotaFloatingIP.setNotDisplay(true);
			quota.addQuotaDetail(quotaFloatingIP);
			quota.makeQuotaDetailsId();
			
		}else{
			String[] quotaDetailIds = quota.getQuotaDetailsId().split(",");
			List<QuotaDetail> quotaDetails = quotaDetailMapper.getQuotaDetailsById(quotaDetailIds);
			for(QuotaDetail quotaDetail : quotaDetails){
				if(quotaDetail.getType().equals(networkType)){
					quotaDetail.setTotal(quotaNode.path(ResponseConstant.NETWORK).intValue());
				}else if(quotaDetail.getType().equals(subnetType)){
					quotaDetail.setTotal(quotaNode.path(ResponseConstant.SUBNET).intValue());
				}else if(quotaDetail.getType().equals(routerType)){
					quotaDetail.setTotal(quotaNode.path(ResponseConstant.ROUTER).intValue());
				}else if(quotaDetail.getType().equals(portType)){
					quotaDetail.setTotal(quotaNode.path(ResponseConstant.PORT).intValue());
				}else if(quotaDetail.getType().equals(securityGroupType)){
					quotaDetail.setTotal(quotaNode.path(ResponseConstant.SECURITY_GROUP).intValue());
				}else if(quotaDetail.getType().equals(floatingIPType)){
					quotaDetail.setTotal(quotaNode.path(ResponseConstant.FLOATINGIP).intValue());
				}
				quota.addQuotaDetail(quotaDetail);
			}
		}
		return quota;
	}
	
	private String makeComputeQuotaUpdateBody(String updateBody,Locale locale) throws ResourceBusinessException{
		ObjectMapper mapper = new ObjectMapper();
		ComputeQuota quotaInfo = null;
		try {
			quotaInfo = mapper.readValue(updateBody, ComputeQuota.class);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.NOT_FOUND_RESPONSE_CODE,locale);
		}
		ComputeQuotaJSON computeJSON = new ComputeQuotaJSON(quotaInfo);
		JsonHelper<ComputeQuotaJSON, String> jsonHelp = new JsonHelper<ComputeQuotaJSON, String>();
		return jsonHelp.generateJsonBodyWithoutDefaultValue(computeJSON);
	}
	

	private Quota getComputeQuota(Map<String, String> rs,String tenantId,Locale locale) throws JsonProcessingException, IOException, BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode quotaNode = rootNode.path(ResponseConstant.QUOTA_SET);
		return getComputeQuotaInfo(quotaNode,tenantId,locale);
	}
	
	private Quota getComputeQuotaInfo(JsonNode quotaNode,String tenantId,Locale locale){
		if(null == quotaNode)
			return null;
		
		String type = Message.getMessage(Message.CS_QUOTA_COMPUTE_TYPE, locale,false);
		String instanceType = Message.getMessage(Message.CS_INSTANCE_NAME,locale, false);
		String cpuType = Message.getMessage(Message.CS_CPU_NAME, locale,false);
		String ramType = Message.getMessage(Message.CS_MEMORY_NAME,locale, false);
		String keypairType = Message.getMessage(Message.CS_KEYPAIR_NAME,locale, false);
		
		Quota quota = quotaMapper.selectQuota(tenantId, type);
		if(null == quota){
			quota = new Quota();
			quota.setId(Util.makeUUID());
			quota.setTenantId(tenantId);
			quota.setQuotaType(type);
			
//			QuotaDetail quotaInstance = new QuotaDetail();
//			int instanceQuota = quotaNode.path(ResponseConstant.INSTANCES).intValue();
//			quotaInstance.setId(Util.makeUUID());
//			quotaInstance.setTenantId(tenantId);
//			quotaInstance.setTotal(instanceQuota);
//			quotaInstance.setUsed(0);
//			quotaInstance.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//			quotaInstance.setType(instanceType);
//			quota.addQuotaDetail(quotaInstance);
			
			QuotaDetail quotaCPU = new QuotaDetail();
			int cpuQuota = quotaNode.path(ResponseConstant.CORES).intValue();
			quotaCPU.setId(Util.makeUUID());
			quotaCPU.setTenantId(tenantId);
			quotaCPU.setTotal(cpuQuota);
			quotaCPU.setUsed(0);
			quotaCPU.setUnit(Message.getMessage(Message.CS_COUNT_UNIT,locale, false));
			quotaCPU.setTypeName(cpuType);
			quotaCPU.setType(ParamConstant.VCPUS);
			quota.addQuotaDetail(quotaCPU);
			
			QuotaDetail quotaRam = new QuotaDetail();
			int ramQuota = quotaNode.path(ResponseConstant.RAM).intValue();
			quotaRam.setId(Util.makeUUID());
			quotaRam.setTenantId(tenantId);
			quotaRam.setTotal(ramQuota);
			quotaRam.setUsed(0);
			quotaRam.setUnit(Message.getMessage(Message.CS_CAPACITY_UNIT,locale, false));
			quotaRam.setType(ramType);
			quota.addQuotaDetail(quotaRam);
			
//			QuotaDetail quotaKeypair = new QuotaDetail();
//			int keypairQuota = quotaNode.path(ResponseConstant.KEY_PAIRS).intValue();
//			quotaKeypair.setId(Util.makeUUID());
//			quotaKeypair.setTenantId(tenantId);
//			quotaKeypair.setTotal(keypairQuota);
//			quotaKeypair.setUsed(0);
//			quotaKeypair.setUnit(Message.getMessage(Message.CS_CAPACITY_UNIT, false));
//			quotaKeypair.setType(keypairType);
//			quota.addQuotaDetail(quotaKeypair);
			
			quota.makeQuotaDetailsId();
			
		}else{
			String[] quotaDetailIds = quota.getQuotaDetailsId().split(",");
			List<QuotaDetail> quotaDetails = quotaDetailMapper.getQuotaDetailsById(quotaDetailIds);
			for(QuotaDetail quotaDetail : quotaDetails){
				if(quotaDetail.getType().equals(instanceType)){
					quotaDetail.setTotal(quotaNode.path(ResponseConstant.INSTANCES).intValue());
				}else if(quotaDetail.getType().equals(cpuType)){
					quotaDetail.setTotal(quotaNode.path(ResponseConstant.CORES).intValue());
				}else if(quotaDetail.getType().equals(ramType)){
					quotaDetail.setTotal(quotaNode.path(ResponseConstant.RAM).intValue());
				}else if(quotaDetail.getType().equals(keypairType)){
					quotaDetail.setTotal(quotaNode.path(ResponseConstant.KEY_PAIRS).intValue());
				}
				quota.addQuotaDetail(quotaDetail);
			}
		}
		return quota;
	}
	
	private Boolean createComputeQuota(QuotaTemplate quotaTemplate,Quota computeQuota,String tenantId,Locale locale){
		if(null == quotaTemplate)
			return false;
		TemplateService service = templateServiceMapper.selectByServiceCode(ParamConstant.COMPUTE);
		if(null == service)
			return false;
		List<TemplateField> templateFields = templateFieldMapper.selectFiledsByServiceId(service.getService_id());
		List<QuotaField> fields = quotaFieldMapper.selectByServiceIdAndTemplateId(service.getService_id(),quotaTemplate.getId());
		if(Util.isNullOrEmptyList(fields) || Util.isNullOrEmptyList(templateFields))
			return false;
		Map<String,String> fieldCodes = new HashMap<String,String>();
		for(TemplateField templateField : templateFields){
			fieldCodes.put(templateField.getField_id(), templateField.getField_code());
		}
		for(QuotaField field : fields){
			String fieldCode = fieldCodes.get(field.getField_id());
			QuotaDetail quota = new QuotaDetail();
			quota.setId(Util.makeUUID());
			quota.setTenantId(tenantId);
			quota.setTotal(field.getMax());
			quota.setType(fieldCode);
			quota.setFieldId(field.getField_id());
			quota.setUsed(0);
			if(fieldCode.contains(ParamConstant.CORE)){
				quota.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, locale,false));
				//String coreType = "CS_CORE_TYPE_NAME";
				String type = fieldCode.substring(0, fieldCode.indexOf('_'));
				HostAggregate aggregate = hostAggregateMapper.selectByZoneName(type);
				if(null == aggregate)
					continue;
				String name = aggregate.getName();
				if(null == name)
					name = aggregate.getAvailabilityZone();
				if(null != aggregate){
					if(locale.getLanguage().contains("zh")){
						quota.setTypeName(StringHelper.ncr2String(name) + Message.getMessage(Message.CS_CPU_NAME,locale,false));
					}else{
						quota.setTypeName(StringHelper.ncr2String(name) + " "+ Message.getMessage(Message.CS_CPU_NAME,locale,false));
					}
				}/*else{
					String coreTypeName = coreType.replaceFirst("TYPE", type.toUpperCase());
					quota.setTypeName(Message.getMessage(coreTypeName, locale,false));
				}*/
			}else{
				quota.setUnit(Message.getMessage(Message.CS_CAPACITY_UNIT,locale, false));
				//String ramType = "CS_RAM_TYPE_NAME";
				String type = fieldCode.substring(0, fieldCode.indexOf('_'));
				HostAggregate aggregate = hostAggregateMapper.selectByZoneName(type);
				if(null == aggregate)
					continue;
				String name = aggregate.getName();
				if(null == name)
					name = aggregate.getAvailabilityZone();
				if(null != aggregate){
					if(locale.getLanguage().contains("zh")){
						quota.setTypeName(StringHelper.ncr2String(name) + Message.getMessage(Message.CS_MEMORY_NAME,locale,false));
					}else{
						quota.setTypeName(StringHelper.ncr2String(name) + " "+ Message.getMessage(Message.CS_MEMORY_NAME,locale,false));
					}
				}/*else{
					String ramTypeName = ramType.replaceFirst("TYPE", type.toUpperCase());
					quota.setTypeName(Message.getMessage(ramTypeName, locale,false));
				}*/
			}
			computeQuota.addQuotaDetail(quota);
		}
		computeQuota.makeQuotaDetailsId();
		storeQuotaToDB(computeQuota);
		return true;
	}
	
	
	private Boolean createNetworkQuota(QuotaTemplate quotaTemplate,Quota networkQuota,String tenantId,Locale locale){
		if(null == quotaTemplate)
			return null;
		TemplateService service = templateServiceMapper.selectByServiceCode(ParamConstant.NETWORK);
		if(null == service)
			return false;
		List<TemplateField> templateFields = templateFieldMapper.selectFiledsByServiceId(service.getService_id());
		List<QuotaField> fields = quotaFieldMapper.selectByServiceIdAndTemplateId(service.getService_id(),quotaTemplate.getId());
		if(Util.isNullOrEmptyList(fields) || Util.isNullOrEmptyList(templateFields))
			return false;
		Map<String,String> fieldCodes = new HashMap<String,String>();
		for(TemplateField templateField : templateFields){
			fieldCodes.put(templateField.getField_id(), templateField.getField_code());
		}
		for(QuotaField field : fields){
			QuotaDetail quota = new QuotaDetail();
			quota.setId(Util.makeUUID());
			quota.setTenantId(tenantId);
			quota.setTotal(field.getMax());
			quota.setUsed(0);
			quota.setUnit(Message.getMessage(Message.CS_COUNT_UNIT,locale, false));
			quota.setFieldId(field.getField_id());
			String type = fieldCodes.get(field.getField_id());
			String floatingipTypeName = "CS_FLOTINGIP_TYPE_NAME";
			floatingipTypeName = floatingipTypeName.replaceFirst("TYPE", type.toUpperCase());
			quota.setTypeName(Message.getMessage(floatingipTypeName, locale,false));
			quota.setType(type);
			networkQuota.addQuotaDetail(quota);
		}
		networkQuota.makeQuotaDetailsId();
		storeQuotaToDB(networkQuota);
		return true;
	}
	
	
	private Boolean createStorageQuota(QuotaTemplate quotaTemplate,Quota storageQuota,String tenantId,Locale locale){
		if(null == quotaTemplate)
			return null;
		TemplateService service = templateServiceMapper.selectByServiceCode(ParamConstant.STORAGE);
		if(null == service)
			return false;
		List<TemplateField> templateFields = templateFieldMapper.selectFiledsByServiceId(service.getService_id());
		List<QuotaField> fields = quotaFieldMapper.selectByServiceIdAndTemplateId(service.getService_id(),quotaTemplate.getId());
		if(Util.isNullOrEmptyList(fields) || Util.isNullOrEmptyList(templateFields))
			return false;
		Map<String,String> fieldCodes = new HashMap<String,String>();
		for(TemplateField templateField : templateFields){
			fieldCodes.put(templateField.getField_id(), templateField.getField_code());
		}
		List<VolumeType> volumeTypes = volumeTypeMapper.selectAll();
		for(QuotaField field : fields){
			QuotaDetail quota = new QuotaDetail();
			quota.setId(Util.makeUUID());
			quota.setTenantId(tenantId);
			quota.setTotal(field.getMax());
			quota.setUsed(0);
			String type = fieldCodes.get(field.getField_id());
			String typeName = null;
			if(type.equals(ParamConstant.SNAPSHOT)){
				quota.setUnit(Message.getMessage(Message.CS_COUNT_UNIT,locale, false));
				typeName = Message.getMessage(Message.CS_VOLUME_SNAPSHOT_NAME,locale, false);
			} else if(type.equals(ParamConstant.IMAGE)){
				quota.setUnit(Message.getMessage(Message.CS_COUNT_UNIT,locale, false));
				typeName = Message.getMessage(Message.CS_PRIVATE_IMAGE_NAME,locale, false);
		    } else if(type.equals(ParamConstant.BACKUP)){
				quota.setUnit(Message.getMessage(Message.CS_COUNT_UNIT,locale, false));
				typeName = Message.getMessage(Message.CS_VOLUME_BACKUP_NAME,locale, false);
		    } else{
				quota.setUnit(Message.getMessage(Message.CS_CAPACITY_UNIT,locale, false));
				typeName = getVolumeTypeVisibleName(volumeTypes,type);
				if(Util.isNullOrEmptyValue(typeName))
					typeName = type;
			}
			quota.setFieldId(field.getField_id());
			quota.setTypeName(typeName);
			quota.setType(type);
			storageQuota.addQuotaDetail(quota);
		}
		storageQuota.makeQuotaDetailsId();
		storeQuotaToDB(storageQuota);
		return true;
	}
	
	private void createDefaultComputeQuota(Quota computeQuota,String tenantId,Locale locale){
		// instance Quota
//		QuotaDetail quotaInstance = new QuotaDetail();
//		quotaInstance.setId(Util.makeUUID());
//		quotaInstance.setTenantId(tenantId);
//		quotaInstance.setTotal(Integer.parseInt(cloudconfig.getInstanceQuota()));
//		quotaInstance.setUsed(0);
//		quotaInstance.setName(ParamConstant.INSTANCE);
//		quotaInstance.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//		quotaInstance.setType(Message.getMessage(Message.CS_INSTANCE_NAME, false));
//		computeQuota.addQuotaDetail(quotaInstance);

		String[] instanceTypes = cloudconfig.getSystemInstanceSpec().split(",");
		for(int index = 0; index < instanceTypes.length; ++index){
			// vcpu Quota
			QuotaDetail quotaCPU = new QuotaDetail();
			quotaCPU.setId(Util.makeUUID());
			quotaCPU.setTenantId(tenantId);
			quotaCPU.setTotal(Integer.parseInt(cloudconfig.getCoreQuota()));
			quotaCPU.setUsed(0);
			quotaCPU.setType(instanceTypes[index] + "_" +ParamConstant.CORE);
			quotaCPU.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, locale,false));
			String coreType = "CS_CORE_TYPE_NAME";
			HostAggregate aggregate = hostAggregateMapper.selectByZoneName(instanceTypes[index]);
			if(null != aggregate){
				if(locale.getLanguage().contains("zh")){
					quotaCPU.setTypeName(StringHelper.ncr2String(aggregate.getName()) + Message.getMessage(Message.CS_CPU_NAME,locale,false));
				}else{
					quotaCPU.setTypeName(StringHelper.ncr2String(aggregate.getName()) + " "+ Message.getMessage(Message.CS_CPU_NAME,locale,false));
				}
			}else{
				String coreTypeName = coreType.replaceFirst("TYPE", instanceTypes[index].toUpperCase());
				quotaCPU.setTypeName(Message.getMessage(coreTypeName, locale,false));
			}
			computeQuota.addQuotaDetail(quotaCPU);
		
			// ram Quota
			QuotaDetail quotaRam = new QuotaDetail();
			quotaRam.setId(Util.makeUUID());
			quotaRam.setTenantId(tenantId);
			quotaRam.setTotal(Integer.parseInt(cloudconfig.getRamQuota()));
			quotaRam.setUsed(0);
			quotaRam.setType(instanceTypes[index] + "_" +ParamConstant.RAM);
			quotaRam.setUnit(Message.getMessage(Message.CS_CAPACITY_UNIT,locale, false));
			String ramType = "CS_RAM_TYPE_NAME";
			
			if(null != aggregate){
				if(locale.getLanguage().contains("zh")){
					quotaRam.setTypeName(StringHelper.ncr2String(aggregate.getName()) + Message.getMessage(Message.CS_MEMORY_NAME,locale,false));
				}else{
					quotaRam.setTypeName(StringHelper.ncr2String(aggregate.getName()) + " "+ Message.getMessage(Message.CS_MEMORY_NAME,locale,false));
				}
			}else{
				String ramTypeName = ramType.replaceFirst("TYPE", instanceTypes[index].toUpperCase());
				quotaRam.setTypeName(Message.getMessage(ramTypeName, locale,false));
			}
			computeQuota.addQuotaDetail(quotaRam);
		}
		

		String[] vdiTypes = cloudconfig.getSystemVdiSpec().split(",");
		for(int index = 0; index < vdiTypes.length; ++index){
			// vcpu Quota
			QuotaDetail quotaCPU = new QuotaDetail();
			quotaCPU.setId(Util.makeUUID());
			quotaCPU.setTenantId(tenantId);
			quotaCPU.setTotal(Integer.parseInt(cloudconfig.getVdiCoreQuota()));
			quotaCPU.setUsed(0);
			quotaCPU.setType(vdiTypes[index] + "_" +ParamConstant.CORE);
			quotaCPU.setUnit(Message.getMessage(Message.CS_COUNT_UNIT,locale, false));
			String coreType = "CS_CORE_TYPE_NAME";
			HostAggregate aggregate = hostAggregateMapper.selectByZoneName(instanceTypes[index]);
			if(null != aggregate){
				if(locale.getLanguage().contains("zh")){
					quotaCPU.setTypeName(StringHelper.ncr2String(aggregate.getName()) + Message.getMessage(Message.CS_CPU_NAME,locale,false));
				}else{
					quotaCPU.setTypeName(StringHelper.ncr2String(aggregate.getName()) + " "+ Message.getMessage(Message.CS_CPU_NAME,locale,false));
				}
			}else{
				String coreTypeName = coreType.replaceFirst("TYPE", vdiTypes[index].toUpperCase());
				quotaCPU.setTypeName(Message.getMessage(coreTypeName, locale,false));
			}
			computeQuota.addQuotaDetail(quotaCPU);
		
			// ram Quota
			QuotaDetail quotaRam = new QuotaDetail();
			quotaRam.setId(Util.makeUUID());
			quotaRam.setTenantId(tenantId);
			quotaRam.setTotal(Integer.parseInt(cloudconfig.getVdiRamQuota()));
			quotaRam.setUsed(0);
			quotaRam.setType(vdiTypes[index] + "_" +ParamConstant.RAM);
			quotaRam.setUnit(Message.getMessage(Message.CS_CAPACITY_UNIT,locale, false));
			String ramType = "CS_RAM_TYPE_NAME";
			if(null != aggregate){
				if(locale.getLanguage().contains("zh")){
					quotaRam.setTypeName(StringHelper.ncr2String(aggregate.getName()) + Message.getMessage(Message.CS_MEMORY_NAME,locale,false));
				}else{
					quotaRam.setTypeName(StringHelper.ncr2String(aggregate.getName()) + " "+ Message.getMessage(Message.CS_MEMORY_NAME,locale,false));
				}
			}else{
				String ramTypeName = ramType.replaceFirst("TYPE", vdiTypes[index].toUpperCase());
				quotaRam.setTypeName(Message.getMessage(ramTypeName,locale, false));
			}
			computeQuota.addQuotaDetail(quotaRam);
		}
		
		// keypair Quota
//		QuotaDetail quotaKeypair = new QuotaDetail();
//		quotaKeypair.setId(Util.makeUUID());
//		quotaKeypair.setTenantId(tenantId);
//		quotaKeypair.setTotal(Integer.parseInt(cloudconfig.getKeypairQuota()));
//		quotaKeypair.setUsed(0);
//		quotaKeypair.setName(ParamConstant.KEYPAIR);
//		quotaKeypair.setUnit(Message.getMessage(Message.CS_COUNT_UNIT, false));
//		quotaKeypair.setType(Message.getMessage(Message.CS_KEYPAIR_NAME, false));
//		computeQuota.addQuotaDetail(quotaKeypair);
		computeQuota.makeQuotaDetailsId();

		storeQuotaToDB(computeQuota);
	}
	
	/*
	private void createDefaultNetworkQuota(Quota networkQuota,String tenantId,Locale locale) throws ResourceBusinessException{
		String[] floatingipSpec = cloudconfig.getSystemFloatingSpec().split(",");
		String[] floatingipQuota = cloudconfig.getFloatingipQuota().split(",");
		if(floatingipSpec.length != floatingipQuota.length)
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
		
		for(int index = 0; index < floatingipSpec.length; ++index){
			QuotaDetail quotaFloatingIP = new QuotaDetail();
			quotaFloatingIP.setId(Util.makeUUID());
			quotaFloatingIP.setTenantId(tenantId);
			quotaFloatingIP.setTotal(Integer.parseInt(floatingipQuota[index]));
			quotaFloatingIP.setUsed(0);
			quotaFloatingIP.setUnit(Message.getMessage(Message.CS_COUNT_UNIT,locale, false));
			String floatingipTypeName = "CS_FLOTINGIP_TYPE_NAME";
			floatingipTypeName = floatingipTypeName.replaceFirst("TYPE", floatingipSpec[index].toUpperCase());
			quotaFloatingIP.setTypeName(Message.getMessage(floatingipTypeName, locale,false));
			quotaFloatingIP.setType(floatingipSpec[index]);
			networkQuota.addQuotaDetail(quotaFloatingIP);
		}
		networkQuota.makeQuotaDetailsId();
		storeQuotaToDB(networkQuota);
	}
	*/
	/*
	private void createDefaultStorageQuota(Quota storageQuota,String tenantId,Locale locale) throws ResourceBusinessException{
		String[] volumeSpec = cloudconfig.getSystemVolumeSpec().split(",");
		String[] volumeQuota = cloudconfig.getVolumeQuota().split(",");
		if (volumeSpec.length != volumeQuota.length)
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
		List<VolumeType> volumeTypes = volumeTypeMapper.selectAll();
		for (int index = 0; index < volumeSpec.length; ++index) {
			QuotaDetail quotaVolume = new QuotaDetail();
			quotaVolume.setId(Util.makeUUID());
			quotaVolume.setTenantId(tenantId);
			quotaVolume.setTotal(Integer.parseInt(volumeQuota[index]));
			quotaVolume.setUsed(0);
			quotaVolume.setUnit(Message.getMessage(Message.CS_CAPACITY_UNIT,locale, false));
			
			String typeName = getVolumeTypeVisibleName(volumeTypes,volumeSpec[index]);
			if(Util.isNullOrEmptyValue(typeName))
				typeName = volumeSpec[index];//Message.getMessage(volumeSpec[index].toUpperCase(), locale,false);
			
		
			quotaVolume.setTypeName(typeName);
			quotaVolume.setType(volumeSpec[index]);
			storageQuota.addQuotaDetail(quotaVolume);
		}
		storageQuota.makeQuotaDetailsId();
		storeQuotaToDB(storageQuota);
	}
	*/
}
