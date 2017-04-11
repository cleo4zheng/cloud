package com.cloud.cloudapi.service.openstackapi;

import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.rating.Currency;
import com.cloud.cloudapi.pojo.rating.RatingTemplate;
import com.cloud.cloudapi.pojo.rating.TemplateVersion;

public interface PriceService {
	
	public List<Currency> getCurrencies();
	public Currency getCurrency(String id);
	public void createCurrency(String body,TokenOs ostoken) throws BusinessException;
	public void createCurrency(String ccy,String ccyName,String ccyUnit,String ccyUnitName);
	public void deleteCurrency(String id);
	
	public List<RatingTemplate> getTemplates(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public RatingTemplate getTemplate(String templateId,TokenOs ostoken) throws BusinessException;
	public RatingTemplate addTemplate(String createBody,TokenOs ostoken) throws BusinessException;
	public void deleteTemplate(String templateId,TokenOs ostoken) throws BusinessException;
	public RatingTemplate updateTemplate(String templateId,String updateBody,TokenOs ostoken) throws BusinessException;

	public TemplateVersion addTemplateVersion(String templateId,String createBody,TokenOs ostoken) throws BusinessException;


}
