package com.cloud.cloudapi.service.rating;

import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.rating.RatingTemplate;
import com.cloud.cloudapi.pojo.rating.TemplateField;
import com.cloud.cloudapi.pojo.rating.TemplateService;
import com.cloud.cloudapi.pojo.rating.TemplateTenantMapping;
import com.cloud.cloudapi.pojo.rating.TemplateVersion;

public interface RatingTemplateService {
	
	public void initRatingTemplate(String body,TokenOs ostoken) throws BusinessException;
	
	public TemplateTenantMapping bindTenantRatingTemplate(TokenOs amdintoken, String tenantId) throws BusinessException;
	public RatingTemplate createSystemTemplate(TokenOs ostoken) throws BusinessException;

//	public List<Currency> getCurrencies(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
//	public Currency addCurrency(String createBody,TokenOs ostoken) throws BusinessException;
//	public Currency getCurrency(String currencyId,TokenOs ostoken) throws BusinessException;
//	public Currency updateCurrency(String currencyId,String updateBody,TokenOs ostoken) throws BusinessException;
//	public void deleteCurrency(String currencyId,TokenOs ostoken) throws BusinessException;
	
	public List<TemplateVersion> getRatingVersions(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	//public RatingVersion addRatingVersion(String templateId,String createBody,TokenOs ostoken) throws BusinessException;
	public TemplateVersion getRatingVersion(String versionId,TokenOs ostoken) throws BusinessException;
	public TemplateVersion updateRatingVersion(String versionId,String updateBody,TokenOs ostoken) throws BusinessException;
	public void deleteRatingVersion(String versionId,TokenOs ostoken) throws BusinessException;
	
	public List<TemplateTenantMapping> getTenantRatings(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public TemplateTenantMapping addTenantRating(String createBody,TokenOs ostoken,boolean replace) throws BusinessException;
	public TemplateTenantMapping getTenantRating(String tenantId,TokenOs ostoken) throws BusinessException;
	public TemplateTenantMapping updateTenantRating(String tenantId,String updateBody,TokenOs ostoken) throws BusinessException;
	public void deleteTenantRating(String tenantId,TokenOs ostoken) throws BusinessException;

	
	public List<TemplateService> getTemplateServices();
	public void addRatingService(List<TemplateService> services,TokenOs ostoken) throws BusinessException;
	
	public List<TemplateService> getRatingServices(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public TemplateService addRatingService(String createBody,TokenOs ostoken) throws BusinessException;
	public TemplateService getRatingService(String serviceCode,TokenOs ostoken) throws BusinessException;
	//public RatingVersion updateRatingService(String versionId,String updateBody,TokenOs ostoken,HttpServletResponse response) throws BusinessException;
	//public void deleteRatingService(String versionId,TokenOs ostoken,HttpServletResponse response) throws BusinessException;
	
	
	public TemplateField getRatingPolicy(String serviceId,String fieldId,TokenOs ostoken) throws BusinessException;
	public TemplateField addRatingPolicy(String serviceCode,String fieldCode,TokenOs ostoken) throws BusinessException;
	public void deleteRatingPolicy(String serviceCode,String fieldCode,TokenOs ostoken) throws BusinessException;
	
	
}
