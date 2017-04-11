package com.cloud.cloudapi.service.openstackapi.impl;

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

import com.cloud.cloudapi.dao.common.CurrencyMapper;
import com.cloud.cloudapi.dao.common.HostAggregateMapper;
import com.cloud.cloudapi.dao.common.RatingTemplateMapper;
import com.cloud.cloudapi.dao.common.TemplateFieldMapper;
import com.cloud.cloudapi.dao.common.TemplateFieldRatingMapper;
import com.cloud.cloudapi.dao.common.TemplateServiceMapper;
import com.cloud.cloudapi.dao.common.TemplateTenantMappingMapper;
import com.cloud.cloudapi.dao.common.TemplateVersionMapper;
import com.cloud.cloudapi.dao.common.VolumeTypeMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.HostAggregate;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeType;
import com.cloud.cloudapi.pojo.rating.Currency;
import com.cloud.cloudapi.pojo.rating.RatingTemplate;
import com.cloud.cloudapi.pojo.rating.TemplateField;
import com.cloud.cloudapi.pojo.rating.TemplateFieldRating;
import com.cloud.cloudapi.pojo.rating.TemplateService;
import com.cloud.cloudapi.pojo.rating.TemplateTenantMapping;
import com.cloud.cloudapi.pojo.rating.TemplateVersion;
import com.cloud.cloudapi.service.openstackapi.PriceService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.StringHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("priceService")
public class PriceServiceImpl implements PriceService {
	
	@Resource
	private OSHttpClientUtil client;

	@Resource
	private CurrencyMapper currencyMapper;
	
	@Autowired
	private TemplateVersionMapper templateVersionMapper;
	
	@Autowired
	private TemplateFieldRatingMapper templateFieldRatingMapper;
	
	@Autowired
	private TemplateFieldMapper templateFieldMapper;
	
	@Autowired
	private RatingTemplateMapper ratingTemplateMapper;
	
	@Autowired
	private TemplateServiceMapper templateServiceMapper;

	@Autowired
	private HostAggregateMapper hostAggregateMapper;
	
	@Autowired
	private TemplateTenantMappingMapper templateTenantMapingMapper;

	@Resource
	private VolumeTypeMapper volumeTypeMapper;
	
	private Logger log = LogManager.getLogger(PriceServiceImpl.class);
	
	@Override
	public List<Currency> getCurrencies(){
		List<Currency> currencies = currencyMapper.selectAll();
		return currencies;
	}
	
	@Override
	public Currency getCurrency(String id){
		return currencyMapper.selectByPrimaryKey(id);
	}
	
	@Override
	public void createCurrency(String ccy,String ccyName,String ccyUnit,String ccyUnitName){
		Currency currency = new Currency();
		currency.setCcy_id(Util.makeUUID());
		currency.setCcy(ccy);
		currency.setCcy_name(ccyName);
		currency.setCcy_unit(ccyUnit);
		currency.setCcy_unit_name(ccyUnitName);
		currency.setMillionSeconds(Util.getCurrentMillionsecond());
		currencyMapper.insertSelective(currency);
	}
	
	@Override
	public void createCurrency(String body,TokenOs ostoken) throws BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(body);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_CURRENCY_CREATE_FAILED, ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		int count =rootNode.size();
        if(0 == count)
        	return ;
        List<Currency> currencies = new ArrayList<Currency>();
        for(int index = 0; index < count; ++index){
        	Currency currency = new Currency();
    		currency.setCcy_id(Util.makeUUID());
    		currency.setCcy(rootNode.get(index).path(ResponseConstant.CCY).textValue());
    		currency.setCcy_name(rootNode.get(index).path(ResponseConstant.CCY_NAME).textValue());
    		currency.setCcy_unit(rootNode.get(index).path(ResponseConstant.CCY_UNIT).textValue());
    		currency.setCcy_unit_name(rootNode.get(index).path(ResponseConstant.CCY_UNIT_NAME).textValue());
    		currency.setMillionSeconds(Util.getCurrentMillionsecond());
    		currencies.add(currency);
        }
		currencyMapper.insertOrUpdateBatch(currencies);
	}
	
	@Override
	public void deleteCurrency(String id){
		currencyMapper.deleteByPrimaryKey(id);	
	}
	
	@Override
	public List<RatingTemplate> getTemplates(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException{
		List<RatingTemplate> templates = ratingTemplateMapper.selectAll();
		if(Util.isNullOrEmptyList(templates))
			return templates;
		for(RatingTemplate template : templates){
			List<TemplateVersion> versions = templateVersionMapper.selectByTemplateId(template.getTemplate_id());
			template.setVersions(versions);
		}
		return templates;
	}
	
	@Override
	public RatingTemplate getTemplate(String templateId,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		
		RatingTemplate ratingTemplate = ratingTemplateMapper.selectByPrimaryKey(templateId);
		if(null == ratingTemplate)
			throw new ResourceBusinessException(Message.CS_RATING_TEMPLATE_DETAILE_GET_FAILED,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);

		List<TemplateFieldRating> fields = templateFieldRatingMapper.selectByTemplateAndVersionId(templateId,ratingTemplate.getVersionIds());
		if(Util.isNullOrEmptyList(fields))
			return ratingTemplate;
		
		List<TemplateVersion> versions = new ArrayList<TemplateVersion>();
		TemplateVersion version = templateVersionMapper.selectByPrimaryKey(ratingTemplate.getVersionIds());
		if(null == version)
			throw new ResourceBusinessException(Message.CS_RATING_TEMPLATE_DETAILE_GET_FAILED,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
        
		Currency currency = currencyMapper.selectByPrimaryKey(version.getCcy_id());
		if(null == currency)
			throw new ResourceBusinessException(Message.CS_RATING_TEMPLATE_DETAILE_GET_FAILED,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
        version.setCurrency(currency);
		versions.add(version);
		
	//	String coreType = "CS_CORE_TYPE_NAME";
	//	String ramType  = "CS_RAM_TYPE_NAME";
	//	String volumeType = "CS_VOLUME_TYPE_NAME";
		String networkType = "CS_FLOTINGIP_TYPE_NAME";
		String serviceType = "CS_SERVICE_TYPE_NAME";
		Map<String,String> typeNameMap = new HashMap<String,String>();
		List<HostAggregate> aggregates = hostAggregateMapper.selectAll();
		for(HostAggregate aggregate : aggregates){
			if(Util.isNullOrEmptyValue(aggregate.getServiceId()))
				continue;
			typeNameMap.put(aggregate.getAvailabilityZone(), StringHelper.ncr2String(aggregate.getName()));
		}
		
		List<VolumeType> volumeTypes = volumeTypeMapper.selectAll();
		List<TemplateService> services = templateServiceMapper.selectAll();
		for(TemplateService service : services){
			if(ParamConstant.COMPUTE.equals(service.getService_code())){
        		service.setName(Message.getMessage(Message.CS_QUOTA_COMPUTE_TYPE, locale,false));
        		
        		for(TemplateFieldRating field : fields){
					if(service.getService_id().equals(field.getService_id())){
						String type = field.getCharging_keys().substring(0, field.getCharging_keys().indexOf('_'));
						String  typeName = typeNameMap.get(type);
						if(null == typeName)
							typeName = type;
						if(null != typeName){
							if(field.getCharging_keys().contains(ParamConstant.RAM)){
								if(locale.getLanguage().contains("zh"))
									field.setName(typeName+Message.getMessage(Message.CS_MEMORY_NAME, locale,false));
								else
									field.setName(typeName+" " + Message.getMessage(Message.CS_MEMORY_NAME, locale,false));
							}else{
								if(locale.getLanguage().contains("zh"))
									field.setName(typeName+Message.getMessage(Message.CS_CPU_NAME, locale,false));
								else
									field.setName(typeName+" "+Message.getMessage(Message.CS_CPU_NAME, locale,false));	
							}
						}
						service.addFieldratings(field);
					}
				}	
			}else if(ParamConstant.STORAGE.equals(service.getService_code())){
				service.setName(Message.getMessage(Message.CS_QUOTA_STORAGE_TYPE, locale,false));
				for(TemplateFieldRating field : fields){
					if(service.getService_id().equals(field.getService_id())){
						String typeName = getVolumeTypeVisibleName(volumeTypes,field.getCharging_keys());
						if(Util.isNullOrEmptyValue(typeName))
							typeName = field.getCharging_keys();//Message.getMessage(field.getCharging_keys().toUpperCase(), locale,false);
						
					//	String typeName = volumeType.replaceFirst("TYPE", field.getCharging_keys().toUpperCase());
        			//	field.setName(Message.getMessage(typeName, locale,false));
						field.setName(typeName);
						service.addFieldratings(field);
					}
				}	
			}else if(ParamConstant.NETWORK.equals(service.getService_code())){
				service.setName(Message.getMessage(Message.CS_QUOTA_NETWORK_TYPE, locale,false));
				for(TemplateFieldRating field : fields){
					if(service.getService_id().equals(field.getService_id())){
						String typeName = networkType.replaceFirst("TYPE", field.getCharging_keys().toUpperCase());
        				field.setName(Message.getMessage(typeName, locale,false));
						service.addFieldratings(field);
					}
				}
			}else if(ParamConstant.EXTEND_SERVICE.equals(service.getService_code())){
				service.setName(Message.getMessage(Message.CS_QUOTA_SERVICE_TYPE, locale,false));
				for(TemplateFieldRating field : fields){
					if(service.getService_id().equals(field.getService_id())){
						String typeName = serviceType.replaceFirst("TYPE", field.getCharging_keys().toUpperCase());
        				field.setName(Message.getMessage(typeName, locale,false));
						service.addFieldratings(field);
					}
				}
			}else{
				service.setName(Message.getMessage(Message.CS_QUOTA_IMAGE_TYPE, locale,false));
				for(TemplateFieldRating field : fields){
					if(service.getService_id().equals(field.getService_id())){
        				field.setName(field.getCharging_keys());
						service.addFieldratings(field);
					}
				}
			}
			
		}
		version.setServices(services);
		ratingTemplate.setVersions(versions);
		return ratingTemplate;
	}
	
	@Override
	public RatingTemplate addTemplate(String createBody,TokenOs ostoken) throws BusinessException{
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(createBody);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_RATING_TEMPLATE_CREATE_FAILED, ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		
		RatingTemplate template = new RatingTemplate();
		String templateId = Util.makeUUID();
		template.setTemplate_id(templateId);
		template.setName(rootNode.path(ResponseConstant.NAME).textValue());
		checkName(template.getName(),ostoken);
		template.setDescription(rootNode.path(ResponseConstant.DESCRIPTION).textValue());
		String versionId = Util.makeUUID();
		template.setVersionIds(versionId);
		template.setMillionSeconds(Util.getCurrentMillionsecond());
		ratingTemplateMapper.insertSelective(template);
		
		JsonNode versionsNode = rootNode.path(ResponseConstant.VERSIONS);
		int versionsCount = versionsNode.size();
		if(0 == versionsCount)
			return template;
		for(int index = 0; index < versionsCount; ++index){
			JsonNode versionNode = versionsNode.get(index);
			makeTemplateVersion(versionNode,templateId,versionId);
			JsonNode serviceNode = versionNode.path(ResponseConstant.SERVICES);
			addServiceRatingFields(serviceNode,versionId,templateId);
		}
		return template;
	}
	
	@Override
	public TemplateVersion addTemplateVersion(String templateId,String createBody,TokenOs ostoken) throws BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(createBody);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_RATING_VERSION_CREATE_FAILED, ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		
		String versionId = Util.makeUUID();
		TemplateVersion templateVersion = makeTemplateVersion(rootNode,templateId,versionId);
		
		JsonNode serviceNode = rootNode.path(ResponseConstant.SERVICES);
		addServiceRatingFields(serviceNode,versionId,templateId);
		return templateVersion;
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
	
    private TemplateVersion makeTemplateVersion(JsonNode rootNode,String templateId,String versionId){
    	TemplateVersion templateVersion = new TemplateVersion();
		templateVersion.setTemplate_id(templateId);
		templateVersion.setCcy_id(rootNode.path(ResponseConstant.CCY_ID).textValue());
		templateVersion.setName(Integer.toString(rootNode.path(ResponseConstant.NAME).intValue()));
		templateVersion.setDescription(rootNode.path(ResponseConstant.DESCRIPTION).textValue());
		templateVersion.setVersion_id(versionId);
		templateVersionMapper.insertSelective(templateVersion);
		return templateVersion;
    }
	
	@Override
	public RatingTemplate updateTemplate(String templateId,String updateBody,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		
		String description = null;
		Boolean defaultFlag = null;
		String name = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode templateNode = mapper.readTree(updateBody);
			if(!templateNode.path(ResponseConstant.NAME).isMissingNode())
				name = templateNode.path(ResponseConstant.NAME).textValue();
			if(!templateNode.path(ResponseConstant.VALUE).isMissingNode())
				description = templateNode.path(ResponseConstant.VALUE).textValue();
			if(!templateNode.path(ResponseConstant.FLAG).isMissingNode())
				defaultFlag = templateNode.path(ResponseConstant.FLAG).booleanValue();
		}  catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e);
			throw new ResourceBusinessException(Message.CS_RATING_TEMPLATE_UPDATE_FAILED, ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		
		RatingTemplate template = ratingTemplateMapper.selectByPrimaryKey(templateId);
		if(null == template)
			throw new ResourceBusinessException(Message.CS_RATING_TEMPLATE_UPDATE_FAILED, ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		if(null != description)
			template.setDescription(description);
		if(null != name){
		    checkName(name,ostoken);
			template.setName(name);
		}
			
		if(null != defaultFlag){
			resetTemplatesFlag();
			template.setDefaultFlag(defaultFlag);
		}
		ratingTemplateMapper.updateByPrimaryKeySelective(template);
		return template;
	}
	
    @Override
	public void deleteTemplate(String templateId,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());

		List<TemplateTenantMapping> tenantMappings = templateTenantMapingMapper.selectByTemplateId(templateId);
		if(!Util.isNullOrEmptyList(tenantMappings))
			throw new ResourceBusinessException(Message.CS_RATING_TEMPLATE_IS_DOING,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
        
		ratingTemplateMapper.deleteByPrimaryKey(templateId);
		templateVersionMapper.deleteByTemplateId(templateId);
		templateFieldRatingMapper.deleteByTemplateId(templateId);
	}
    
	private void addServiceRatingFields(JsonNode servicesNode,String versionId,String templateId){
		if(null == servicesNode)
			return ;
		int servicesCount = servicesNode.size();
		if(0 == servicesCount)
			return ;
		
		List<TemplateFieldRating> ratingFields = new ArrayList<TemplateFieldRating>();
		
		for(int index = 0; index < servicesCount; ++index){
			JsonNode serviceNode = servicesNode.get(index);
			String serviceId = serviceNode.path(ResponseConstant.SERVICE_ID).textValue();
			JsonNode fieldsNode = serviceNode.path(ResponseConstant.FIELDRATINGS);
			int fieldsCount = fieldsNode.size();
			if(0 == fieldsCount)
				continue;
			
			for (int fieldIndex = 0; fieldIndex < fieldsCount; ++fieldIndex) {
				JsonNode fieldNode = fieldsNode.get(fieldIndex);
				TemplateFieldRating field = new TemplateFieldRating();
				field.setFieldrating_id(Util.makeUUID());
				field.setField_id(fieldNode.path(ResponseConstant.FIELD_ID).textValue());
				field.setService_id(serviceId);
				field.setVersion_id(versionId);
				field.setTemplate_id(templateId);
                field.setPrice(fieldNode.path(ResponseConstant.PRICE).floatValue());
                field.setC_unit(fieldNode.path(ResponseConstant.C_UNIT).textValue());
                field.setC_unit_name(fieldNode.path(ResponseConstant.C_UNIT_NAME).textValue());
                field.setC_unit_conversion(fieldNode.path(ResponseConstant.C_UNIT_CONVERSION).intValue());
                field.setC_unit_value(fieldNode.path(ResponseConstant.C_UNIT_VALUE).intValue());
                field.setT_unit(fieldNode.path(ResponseConstant.T_UNIT).textValue());
                field.setT_unit_conversion(fieldNode.path(ResponseConstant.T_UNIT_CONVERSION).intValue());
                field.setT_unit_name(fieldNode.path(ResponseConstant.T_UNIT_NAME).textValue());
                field.setT_unit_value(fieldNode.path(ResponseConstant.T_UNIT_VALUE).intValue());
				JsonNode chargingNodes = fieldNode.path(ResponseConstant.CHARGING_KEYS);
				if (!chargingNodes.isMissingNode()) {
					TemplateField templateField = templateFieldMapper.selectByPrimaryKey(field.getField_id());
					field.setCharging_keys(templateField.getField_code());
//					int chargingsCount = chargingNodes.size();
//					List<String> keys = new ArrayList<String>();
//					for (int charingIndex = 0; index < chargingsCount; ++index) {
//						keys.add(chargingNodes.get(charingIndex).textValue());
//					}
//					field.setCharging_keys(Util.listToString(keys, ','));
				}
				ratingFields.add(field);
			}
		}
		templateFieldRatingMapper.insertOrUpdateBatch(ratingFields);
	}
	
	private void checkName(String name,TokenOs ostoken) throws BusinessException{
		List<RatingTemplate> ratingTemplates = ratingTemplateMapper.selectAll();
		if(Util.isNullOrEmptyList(ratingTemplates))
			return;
		for(RatingTemplate ratingTemplate : ratingTemplates){
			if(name.equals(ratingTemplate.getName()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_NAME_IS_SAME,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
	}
	
	private void resetTemplatesFlag(){
		List<RatingTemplate> ratingTemplates = ratingTemplateMapper.selectAll();
		if(Util.isNullOrEmptyList(ratingTemplates))
			return;
		for(RatingTemplate ratingTemplate : ratingTemplates){
			ratingTemplate.setDefaultFlag(false);
		}
		ratingTemplateMapper.insertOrUpdateBatch(ratingTemplates);
	}
}
