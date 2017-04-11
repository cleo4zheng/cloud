package com.cloud.cloudapi.service.rating.impl;

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

import com.cloud.cloudapi.dao.common.CloudUserMapper;
import com.cloud.cloudapi.dao.common.DomainTenantUserMapper;
import com.cloud.cloudapi.dao.common.HostAggregateMapper;
import com.cloud.cloudapi.dao.common.NetworkMapper;
import com.cloud.cloudapi.dao.common.RatingTemplateMapper;
import com.cloud.cloudapi.dao.common.TemplateFieldMapper;
import com.cloud.cloudapi.dao.common.TemplateFieldRatingMapper;
import com.cloud.cloudapi.dao.common.TemplateServiceMapper;
import com.cloud.cloudapi.dao.common.TemplateTenantMappingMapper;
import com.cloud.cloudapi.dao.common.TemplateVersionMapper;
import com.cloud.cloudapi.dao.common.VolumeTypeMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.common.DomainTenantUser;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.HostAggregate;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Image;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Network;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeType;
import com.cloud.cloudapi.pojo.rating.Currency;
import com.cloud.cloudapi.pojo.rating.RatingTemplate;
import com.cloud.cloudapi.pojo.rating.TemplateField;
import com.cloud.cloudapi.pojo.rating.TemplateFieldRating;
import com.cloud.cloudapi.pojo.rating.TemplateService;
import com.cloud.cloudapi.pojo.rating.TemplateTenantMapping;
import com.cloud.cloudapi.pojo.rating.TemplateVersion;
import com.cloud.cloudapi.service.openstackapi.ImageService;
import com.cloud.cloudapi.service.openstackapi.PriceService;
import com.cloud.cloudapi.service.rating.RatingTemplateService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.StringHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("ratingTemplateService")
public class RatingTemplateServiceImpl implements RatingTemplateService {
	@Resource
	private OSHttpClientUtil httpClient;
	
	@Autowired
	private CloudConfig cloudconfig;
    
	@Resource
    private DomainTenantUserMapper domainTenantUserMapper;
    
	@Resource
    private CloudUserMapper cloudUserMapper;

	@Autowired
	private ImageService imageService;

	@Autowired
	private PriceService priceService;
	
	@Autowired
	private RatingTemplateMapper ratingTemplateMapper;
	
	@Autowired
	private TemplateVersionMapper templateVersionMapper;
	
	@Autowired
	private TemplateServiceMapper templateServiceMapper;
	
	@Autowired
	private TemplateFieldRatingMapper templateFieldRatingMapper;
	
	@Autowired
	private TemplateTenantMappingMapper templateTenantMappingMapper;

	@Autowired
	private HostAggregateMapper hostAggregateMapper;

	@Autowired
	private NetworkMapper networkMapper;
	
	@Autowired
	private TemplateFieldMapper templateFieldMapper;
	
	@Autowired
	private VolumeTypeMapper volumeTypeMapper;
	
	private Logger log = LogManager.getLogger(RatingTemplateServiceImpl.class);
	
	@Override
	public void initRatingTemplate(String body,TokenOs ostoken) throws BusinessException{
		if(!Util.isNullOrEmptyList(priceService.getCurrencies()))
			return; //have initialized env
		
		String[] systemCcy = cloudconfig.getSystemCcy().split(",");
		String[] systemCcyName = cloudconfig.getSystemCcyName().split(",");
		String[] systemCcyUnit = cloudconfig.getSystemCcyUnit().split(",");
		String[] systemCcyUnitName = cloudconfig.getSystemCcyUnitName().split(",");
		Locale locale = new Locale(ostoken.getLocale());
		//init currency
		if(systemCcy.length != systemCcyName.length || systemCcy.length != systemCcyUnit.length 
		   || systemCcy.length != systemCcyUnitName.length)
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
		for(int index = 0; index < systemCcy.length; ++index){
			priceService.createCurrency(systemCcy[index],systemCcyName[index],systemCcyUnit[index],systemCcyUnitName[index]);
		}
		
		//init serviceString
		TemplateService compute = new TemplateService();
		compute.setService_code(ParamConstant.COMPUTE);
		List<TemplateField> computePolicies = new ArrayList<TemplateField>();
		String instanceTypeSpec = getInstanceTypes();
		
		if(!Util.isNullOrEmptyValue(instanceTypeSpec)){
			String[] instanceTypes = instanceTypeSpec.split(",");
			for(int index = 0; index < instanceTypes.length; ++index){
				computePolicies.add(createDefaultTemplateField(instanceTypes[index],ParamConstant.CORE,null,true));
				computePolicies.add(createDefaultTemplateField(instanceTypes[index],ParamConstant.RAM,null,true));
			}
			compute.setFields(computePolicies);
		}
		
		String volumeTypeSpec = getVolumeTypes();
		TemplateService storage = new TemplateService();
		storage.setService_code(ParamConstant.STORAGE);
		List<TemplateField> storagePolicies = new ArrayList<TemplateField>();
		if(!Util.isNullOrEmptyValue(volumeTypeSpec)){
			String[] volumeTypes = volumeTypeSpec.split(",");
		//	storage.setName(Message.getMessage(Message.CS_QUOTA_STORAGE_TYPE,  locale,false));
			// volume type field
			for(int index = 0; index < volumeTypes.length; ++index){
				storagePolicies.add(createDefaultTemplateField(volumeTypes[index],ParamConstant.DISK,volumeTypes[index],true));
			}	
		}
		storagePolicies.add(createDefaultTemplateField(ParamConstant.SNAPSHOT,ParamConstant.SNAPSHOT,ParamConstant.SNAPSHOT,false));
		storagePolicies.add(createDefaultTemplateField(ParamConstant.IMAGE,ParamConstant.IMAGE,ParamConstant.IMAGE,false));
		storagePolicies.add(createDefaultTemplateField(ParamConstant.BACKUP,ParamConstant.BACKUP,ParamConstant.BACKUP,false));
		storage.setFields(storagePolicies);
		
		TemplateService network = new TemplateService();
		network.setService_code(ParamConstant.NETWORK);
	//	network.setName(Message.getMessage(Message.CS_QUOTA_NETWORK_TYPE, locale, false));
		List<TemplateField> networkPolicies = new ArrayList<TemplateField>();
	//	String[] floatingipSpec = cloudconfig.getSystemFloatingSpec().split(",");
		List<Network> externalNetworks = networkMapper.selectExternalNetworks();
		if(!Util.isNullOrEmptyList(externalNetworks)){
           for(Network externalNetwork : externalNetworks){
   			networkPolicies.add(createDefaultTemplateField(externalNetwork.getName(),ParamConstant.FLOATINGIP,externalNetwork.getName(),true));
           }
           network.setFields(networkPolicies);
		}
		/*
		for(int index = 0; index < floatingipSpec.length; ++index){
			networkPolicies.add(createDefaultTemplateField(floatingipSpec[index],"CS_FLOTINGIP_TYPE_NAME",ParamConstant.FLOATINGIP,floatingipSpec[index],locale));
		}*/
		
		TemplateService image = new TemplateService();
		image.setService_code(ParamConstant.IMAGE);
	//	image.setName(Message.getMessage(Message.CS_QUOTA_IMAGE_TYPE, locale, false));
		List<TemplateField> imagePolicies = new ArrayList<TemplateField>();
		List<Image> ratingImages = imageService.getRatingImageList(null, ostoken);
		if(!Util.isNullOrEmptyList(ratingImages)){
			for(Image ratingImage : ratingImages){
				TemplateField policy = new TemplateField();
				policy.setField_code(ratingImage.getName());
				policy.setName(ratingImage.getName());
				policy.setDefault_chargekey(ratingImage.getName());
				imagePolicies.add(policy);
			}	
		}
		image.setFields(imagePolicies);
		
		TemplateService service = new TemplateService();
		service.setService_code(ParamConstant.EXTEND_SERVICE);
	//	service.setName(Message.getMessage(Message.CS_QUOTA_SERVICE_TYPE, locale, false));
		List<TemplateField> servicePolicies = new ArrayList<TemplateField>();
		String[] serviceSpec = cloudconfig.getSystemServiceSpec().split(",");
		for(int index = 0; index < serviceSpec.length; ++index){
			servicePolicies.add(createDefaultTemplateField(serviceSpec[index],ParamConstant.SERVICE,serviceSpec[index],true));
		}
		service.setFields(servicePolicies);
		
		List<TemplateService> services = new ArrayList<TemplateService>();
		services.add(compute);
		services.add(storage);
		services.add(network);
		services.add(image);
		services.add(service);
		addRatingService(services, ostoken);
	}
	
	@Override
	public TemplateTenantMapping bindTenantRatingTemplate(TokenOs amdintoken, String tenantId) throws BusinessException {
	//	RatingTemplate template = ratingTemplateMapper.selectByName(cloudconfig.getSystemDefaultPriceName());
		RatingTemplate template = ratingTemplateMapper.selectDefaultTemplate();
		if (null != template) {
			TemplateTenantMapping tenantMapping = new TemplateTenantMapping();
			tenantMapping.setTenant_mapping_id(Util.makeUUID());
			tenantMapping.setTenant_id(tenantId);
			tenantMapping.setTemplate_id(template.getTemplate_id());
			tenantMapping.setVersion_id(template.getVersionIds());
			tenantMapping.setTenant_discount(new Float(1));
			templateTenantMappingMapper.insertSelective(tenantMapping);
			return tenantMapping;
//			template = getTemplate(template.getTemplate_id(), amdintoken);
//			RatingVersion version = new RatingVersion();
//			version.setVersion_id(template.getVersions().get(0).getVersion_id());
//			List<String> tenantIds = new ArrayList<String>();
//			tenantIds.add(tenantId);
//			version.setTenant_ids(tenantIds);
//			version.setTemplate_id(template.getTemplate_id());
//			List<RatingService> services = template.getVersions().get(0).getServices();
//			List<RatingService> ratingServices = new ArrayList<RatingService>();
//			for (RatingService service : services) {
//				RatingService ratingService = new RatingService();
//				ratingService.setService_id(service.getService_id());
//				ratingService.setService_code(service.getService_code());
//				List<RatingPolicy> fieldRatings = service.getFieldratings();
//				List<RatingPolicy> fields = new ArrayList<RatingPolicy>();
//				for (RatingPolicy fieldRating : fieldRatings) {
//					RatingPolicy field = new RatingPolicy();
//					field.setField_id(fieldRating.getField_id());
//					field.setUsethreshold(false);
//					field.setField_code(fieldRating.getField_code());
//					field.setTenant_discount(new Float(1));
//					field.setName(fieldRating.getName());
//					fields.add(field);
//				}
//				ratingService.setFields(fields);
//				ratingServices.add(ratingService);
//			}
//			version.setServices(ratingServices);
//			JsonHelper<RatingVersion, String> jsonHelp = new JsonHelper<RatingVersion, String>();
//			String ratingCreateBody = jsonHelp.generateJsonBodySimple(version);
//			addTenantRating(ratingCreateBody, amdintoken,false);
		}
		return null;
	}
	
	@Override
	public RatingTemplate createSystemTemplate(TokenOs ostoken) throws BusinessException{
		List<Currency> currencies = priceService.getCurrencies();
        if(Util.isNullOrEmptyList(currencies))
        	throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
        String defaultCurrencyId = null;
        for(Currency currency : currencies){
        	if(currency.getCcy().equals(cloudconfig.getSystemDefaultCurrencyName()))
        			defaultCurrencyId = currency.getCcy_id();
        }
        if(null == defaultCurrencyId)
        	throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
        
        Locale locale = new Locale(ostoken.getLocale());
        
		RatingTemplate template  = new RatingTemplate();
		
		template.setName(cloudconfig.getSystemDefaultPriceName());
		TemplateVersion templateVersion = new TemplateVersion();
		templateVersion.setName("1");
		templateVersion.setCcy_id(defaultCurrencyId);
		
//		TemplateVersion version = new TemplateVersion();
//		version.setName("1");
//		version.setCcy_id(defaultCurrencyId);
		List<TemplateService> services = getTemplateServices();//getRatingServices(null, ostoken);
		if(Util.isNullOrEmptyList(services))
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		
		String corePrice = cloudconfig.getCorePrice();
		String ramPrice = cloudconfig.getRamPrice();
		
		/*
		String[] vdiCorePrices = cloudconfig.getVdiCorePrice().split(",");
		String[] vdiRamPrices = cloudconfig.getVdiCorePrice().split(",");
		
		String[] baremetalCorePrices = cloudconfig.getBaremetalCorePrice().split(",");
		String[] baremetalRamPrices = cloudconfig.getBaremetalRamPrice().split(",");
		
		String[] generalCorePrices = cloudconfig.getGeneralCorePrice().split(",");
		String[] generalRamPrices = cloudconfig.getGeneralRamPrice().split(",");
		*/
		String floatingPrice = cloudconfig.getFloatingPrice();
        String imagePrice = cloudconfig.getImagePrice();
        String servicePrice = cloudconfig.getServicePrice();
		String volumePrice = cloudconfig.getVolumePrice();

	
		List<TemplateService> ratingServices = new ArrayList<TemplateService>();
		for(TemplateService service : services){
			TemplateService ratingService = new TemplateService();
			ratingService.setService_id(service.getService_id());
			List<TemplateField> fields = this.getTemplateServiceFileds(service.getService_id());
			if(Util.isNullOrEmptyList(fields))
				continue;
			List<TemplateFieldRating> ratingFields = new ArrayList<TemplateFieldRating>();
			if(service.getService_code().equals(ParamConstant.COMPUTE)){
				for(TemplateField field : fields){
					TemplateFieldRating rating = new TemplateFieldRating();
					rating.setField_id(field.getField_id());
					rating.setCharging_keys(field.getField_code());
					if(field.getField_code().contains(ParamConstant.CORE)){
						setRatingUnitPrice(rating,Float.valueOf(corePrice),ParamConstant.CORE_UNIT,ParamConstant.CORE_UNIT, 1,ParamConstant.HOUR_UNIT,3600,Message.getMessage("CS_UNIT_0011",locale,false),1);
					}else{
						setRatingUnitPrice(rating,Float.valueOf(ramPrice),ParamConstant.MB_UNIT,ParamConstant.MB_UNIT, 1,ParamConstant.HOUR_UNIT,3600,Message.getMessage("CS_UNIT_0011",locale,false),1);
					}
					ratingFields.add(rating);
				}
				ratingService.setFieldratings(ratingFields);
	            ratingServices.add(ratingService);
			}else if(service.getService_code().equals(ParamConstant.STORAGE)){
				for(TemplateField field : fields){
					TemplateFieldRating rating = new TemplateFieldRating();
					rating.setField_id(field.getField_id());
					rating.setCharging_keys(field.getField_code());
					setRatingUnitPrice(rating,Float.valueOf(volumePrice),ParamConstant.GB_UNIT,ParamConstant.GB_UNIT, 1,ParamConstant.HOUR_UNIT,3600,Message.getMessage("CS_UNIT_0011",locale,false),1);
					ratingFields.add(rating);
				}
				ratingService.setFieldratings(ratingFields);
	            ratingServices.add(ratingService);
			}else if(service.getService_code().equals(ParamConstant.NETWORK)){
				for(TemplateField field : fields){
					TemplateFieldRating rating = new TemplateFieldRating();
					rating.setField_id(field.getField_id());
					rating.setCharging_keys(field.getField_code());
					setRatingUnitPrice(rating,Float.valueOf(floatingPrice),ParamConstant.IP_UNIT,Message.getMessage("CS_UNIT_0001",locale,false), 1,ParamConstant.HOUR_UNIT,3600,Message.getMessage("CS_UNIT_0011",locale,false),1);
					ratingFields.add(rating);
				}
				ratingService.setFieldratings(ratingFields);
	            ratingServices.add(ratingService);
			}else if(service.getService_code().equals(ParamConstant.IMAGE)){
				for(TemplateField field : fields){
					TemplateFieldRating rating = new TemplateFieldRating();
					rating.setField_id(field.getField_id());
					rating.setCharging_keys(field.getField_code());
					setRatingUnitPrice(rating,Float.valueOf(imagePrice),ParamConstant.IMAGE_UNIT,Message.getMessage("CS_UNIT_0001",locale,false), 1,"",0,"",0);
					ratingFields.add(rating);
				}
				ratingService.setFieldratings(ratingFields);
	            ratingServices.add(ratingService);
			}else if(service.getService_code().equals(ParamConstant.EXTEND_SERVICE)){
				for(TemplateField field : fields){
					TemplateFieldRating rating = new TemplateFieldRating();
					rating.setField_id(field.getField_id());
					rating.setCharging_keys(field.getField_code());
					setRatingUnitPrice(rating,Float.valueOf(servicePrice),"","", 0,ParamConstant.HOUR_UNIT,3600,Message.getMessage("CS_UNIT_0011",locale,false),1);
					ratingFields.add(rating);
				}
				ratingService.setFieldratings(ratingFields);
	            ratingServices.add(ratingService);
			}else{
				continue;
			}
		}
		templateVersion.setServices(ratingServices);
        List<TemplateVersion> versions = new ArrayList<TemplateVersion>();
        versions.add(templateVersion);
        template.setVersions(versions);
		template.setDefaultFlag(true);
        addTemplate(template);
		
		bindTenantRatingTemplate(ostoken,ostoken.getTenantid());
		return null;
	}
	
	private void setRatingUnitPrice(TemplateFieldRating rating,float price,String unit,String unitName,Integer cunitConversion,String tunit,Integer tunitConversion,String tunitName,Integer tunitValue){
		rating.setPrice(price);
		rating.setC_unit(unit);
		rating.setC_unit_name(unitName);
		rating.setC_unit_conversion(cunitConversion);
		rating.setC_unit_value(cunitConversion);
		rating.setT_unit(tunit);
		rating.setT_unit_conversion(tunitConversion);
		rating.setT_unit_name(tunitName);
		rating.setT_unit_value(tunitValue);
	}

//	private RatingPolicy createDefaultRatingPolicy(String policyType,String message,String policyName,String defalutName,Locale locale){
//		RatingPolicy policy = new RatingPolicy();
//		if(Util.isNullOrEmptyValue(defalutName))
//			defalutName = policyType + "_" +policyName;
//		policy.setField_code(defalutName);
//		String typeName = message.replaceFirst("TYPE", policyType.toUpperCase());
//		policy.setName(Message.getMessage(typeName, locale,false));
//		policy.setDefault_chargekey(defalutName);
//		return policy;
//	}
	
	private TemplateField createDefaultTemplateField(String policyType,String policyName,String defalutName,Boolean ratingFlag){
		TemplateField policy = new TemplateField();
		if(Util.isNullOrEmptyValue(defalutName))
			defalutName = policyType + "_" +policyName;
		policy.setField_code(defalutName);
	//	String typeName = message.replaceFirst("TYPE", policyType.toUpperCase());
	//	policy.setName(Message.getMessage(typeName, locale,false));
		policy.setDefault_chargekey(defalutName);
		policy.setRating(ratingFlag);
		return policy;
	}
	
	
	private void addTemplate(RatingTemplate ratingTemplate){
		if(null == ratingTemplate)
			return;
		TemplateVersion version = ratingTemplate.getVersions().get(0);
		version.setVersion_id(Util.makeUUID());
		
		RatingTemplate template = new RatingTemplate();
		template.setTemplate_id(Util.makeUUID());
		template.setName(ratingTemplate.getName());
		template.setDescription(ratingTemplate.getDescription());
		template.setMillionSeconds(Util.getCurrentMillionsecond());
		template.setVersionIds(version.getVersion_id());
		template.setDefaultFlag(ratingTemplate.getDefaultFlag());
		ratingTemplateMapper.insertSelective(template);
		
		version.setTemplate_id(template.getTemplate_id());
		templateVersionMapper.insertSelective(version);
		
		List<TemplateService> services = version.getServices();
		for(TemplateService service : services){
			List<TemplateFieldRating> fileds = service.getFieldratings();
			if(Util.isNullOrEmptyList(fileds))
				continue;
			for(TemplateFieldRating field : fileds){
				field.setFieldrating_id(Util.makeUUID());
				field.setService_id(service.getService_id());
				field.setVersion_id(version.getVersion_id());
				field.setTemplate_id(template.getTemplate_id());
			}
			templateFieldRatingMapper.insertOrUpdateBatch(fileds);
		}
	}
	
//	@Override
//	public RatingTemplate addTemplate(String createBody,TokenOs ostoken) throws BusinessException{
//		Locale locale = new Locale(ostoken.getLocale());
//		TokenOs adminToken = null;
//		try{
//			adminToken = authService.createDefaultAdminOsToken();
//		}catch(Exception e){
//			throw new ResourceBusinessException(Message.CS_RATING_TEMPLATE_CREATE_FAILED,locale);
//		}
//		
//		String region = ostoken.getCurrentRegion(); // we should get the regioninfo by the guiTokenId
//		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_RATING, region).getPublicURL();
//		url = RequestUrlHelper.createFullUrl(url+"/v1/rating/module_config/template", null);
//
//		HashMap<String, String> headers = new HashMap<String, String>();
//		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
//		
//		Map<String, String> rs = httpClient.httpDoPost(url, headers,createBody);
//		Util.checkResponseBody(rs,locale);
//		String failedMessage = Util.getFailedReason(rs);
//		if (!Util.isNullOrEmptyValue(failedMessage))
//			log.error(failedMessage);
//		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//		RatingTemplate template = null;
//		switch (httpCode) {
//		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: 
//		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE:{
//			try {
//				ObjectMapper mapper = new ObjectMapper();
//				JsonNode templateNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
//				template = getTemplateInfo(templateNode);
//			}  catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			break;
//		}
//		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
//			String tokenid = "";// TODO reget the token id
//			try {
//				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
//				tokenid = newToken.getTokenid();
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
//			rs = httpClient.httpDoPost(url, headers,createBody);
//			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//			failedMessage = Util.getFailedReason(rs);
//			if (!Util.isNullOrEmptyValue(failedMessage))
//				log.error(failedMessage);
//			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE || httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//			try {
//				ObjectMapper mapper = new ObjectMapper();
//				JsonNode templateNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
//				template = getTemplateInfo(templateNode);
//			}  catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			break;
//		}
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
//		case ParamConstant.ALREADLAY_EXIST_RESPONSE_CODE:{
//			template = getExistingTemplate(cloudconfig.getSystemDefaultPriceName(),adminToken);
//			if(null == template)
//				throw new ResourceBusinessException(Message.CS_RATING_TEMPLATE_CREATE_FAILED,httpCode,locale);
//			break;
//		}
//		default:
//			throw new ResourceBusinessException(Message.CS_RATING_TEMPLATE_CREATE_FAILED,httpCode,locale);
//		}
//		
//		template.setMillionSeconds(Util.time2Millionsecond(Util.getCurrentDate(), ParamConstant.TIME_FORMAT_01));
//		//ratingTemplateMapper.insertSelective(template);
//		ratingTemplateMapper.insertOrUpdate(template);
//		return template;
//	}
	
//	@Override
//	public List<Currency> getCurrencies(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException{
//		Locale locale = new Locale(ostoken.getLocale());
//		TokenOs adminToken = null;
//		try{
//			adminToken = authService.createDefaultAdminOsToken();
//		}catch(Exception e){
//			throw new ResourceBusinessException(Message.CS_CURRENCY_GET_FAILED,locale);
//		}
//		
//		String region = ostoken.getCurrentRegion(); 
//		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_RATING, region).getPublicURL();
//		url = RequestUrlHelper.createFullUrl(url + "/v1/rating/module_config/template/currency", null);
//
//		Map<String, String> rs = httpClient.httpDoGet(url, adminToken.getTokenid());
//		Util.checkResponseBody(rs,locale);
//		String failedMessage = Util.getFailedReason(rs);
//		if (!Util.isNullOrEmptyValue(failedMessage))
//			log.error(failedMessage);
//		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//        List<Currency> currencies = null;
//		switch (httpCode) {
//		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
//			try {
//				currencies = getCurrencies(rs);
//			}  catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			break;
//		}
//		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
//			String tokenid = "";// TODO reget the token id
//			try {
//				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
//				tokenid = newToken.getTokenid();
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			rs = httpClient.httpDoGet(url, tokenid);
//			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//			failedMessage = Util.getFailedReason(rs);
//			if (!Util.isNullOrEmptyValue(failedMessage))
//				log.error(failedMessage);
//			try {
//				currencies = getCurrencies(rs);
//			}  catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			break;
//		}
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
//		default:
//			throw new ResourceBusinessException(Message.CS_CURRENCY_GET_FAILED,httpCode,locale);
//		}
//		
//		return currencies;
//	}
//	
//	@Override
//	public Currency addCurrency(String createBody,TokenOs ostoken) throws BusinessException{
//		Locale locale = new Locale(ostoken.getLocale());
//		TokenOs adminToken = null;
//		try{
//			adminToken = authService.createDefaultAdminOsToken();
//		}catch(Exception e){
//			throw new ResourceBusinessException(Message.CS_CURRENCY_CREATE_FAILED,locale);
//		}
//		
//		String region = ostoken.getCurrentRegion(); // we should get the regioninfo by the guiTokenId
//		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_RATING, region).getPublicURL();
//		url = RequestUrlHelper.createFullUrl(url+"/v1/rating/module_config/template/currency", null);
//
//		HashMap<String, String> headers = new HashMap<String, String>();
//		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
//
//		Map<String, String> rs = httpClient.httpDoPost(url, headers,createBody);
//		Util.checkResponseBody(rs,locale);
//		String failedMessage = Util.getFailedReason(rs);
//		if (!Util.isNullOrEmptyValue(failedMessage))
//			log.error(failedMessage);
//		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//		Currency currency = null;
//		switch (httpCode) {
//		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: 
//		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE:{
//			try {
//				ObjectMapper mapper = new ObjectMapper();
//				JsonNode currencyNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
//				currency = getCurrency(currencyNode);
//			}  catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			break;
//		}
//		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
//			String tokenid = "";// TODO reget the token id
//			try {
//				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
//				tokenid = newToken.getTokenid();
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
//			rs = httpClient.httpDoPost(url, headers,createBody);
//			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//			failedMessage = Util.getFailedReason(rs);
//			if (!Util.isNullOrEmptyValue(failedMessage))
//				log.error(failedMessage);
//			try {
//				ObjectMapper mapper = new ObjectMapper();
//				JsonNode currencyNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
//				currency = getCurrency(currencyNode);
//			}  catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			break;
//		}
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
//		default:
//			throw new ResourceBusinessException(Message.CS_CURRENCY_CREATE_FAILED,httpCode,locale);
//		}
//		
//		return currency;
//	}
//	
//	@Override
//	public Currency getCurrency(String currencyId,TokenOs ostoken) throws BusinessException{
//		Locale locale = new Locale(ostoken.getLocale());
//		TokenOs adminToken = null;
//		try{
//			adminToken = authService.createDefaultAdminOsToken();
//		}catch(Exception e){
//			throw new ResourceBusinessException(Message.CS_CURRENCY_DETAIL_GET_FAILED,locale);
//		}
//		
//		String region = ostoken.getCurrentRegion(); // we should get the regioninfo by the guiTokenId
//		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_RATING, region).getPublicURL();
//		StringBuilder sb = new StringBuilder();
//		sb.append(url);
//		sb.append("/v1/rating/module_config/template/currency/");
//		sb.append(currencyId);
//
//		Map<String, String> rs = httpClient.httpDoGet(sb.toString(), adminToken.getTokenid());
//		Util.checkResponseBody(rs,locale);
//		
//		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//		String failedMessage = Util.getFailedReason(rs);
//		if (!Util.isNullOrEmptyValue(failedMessage))
//			log.error(failedMessage);
//		Currency currency = null;
//		switch (httpCode) {
//		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
//			try {
//				ObjectMapper mapper = new ObjectMapper();
//				JsonNode currencyNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
//				currency = getCurrency(currencyNode);
//			}  catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			break;
//		}
//		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
//			String tokenid = "";// TODO reget the token id
//			try {
//				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
//				tokenid = newToken.getTokenid();
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			rs = httpClient.httpDoGet(sb.toString(), tokenid);
//			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//			failedMessage = Util.getFailedReason(rs);
//			if (!Util.isNullOrEmptyValue(failedMessage))
//				log.error(failedMessage);
//			try {
//				ObjectMapper mapper = new ObjectMapper();
//				JsonNode currencyNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
//				currency = getCurrency(currencyNode);
//			}  catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			break;
//		}
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
//		default:
//			throw new ResourceBusinessException(Message.CS_CURRENCY_DETAIL_GET_FAILED,httpCode,locale);
//		}
//		
//		return currency;
//	}
//	
//	@Override
//	public Currency updateCurrency(String currencyId,String updateBody,TokenOs ostoken) throws BusinessException{
//		Locale locale = new Locale(ostoken.getLocale());
//		TokenOs adminToken = null;
//		try{
//			adminToken = authService.createDefaultAdminOsToken();
//		}catch(Exception e){
//			throw new ResourceBusinessException(Message.CS_CURRENCY_UPDATE_FAILED,locale);
//		}
//		
//		String region = ostoken.getCurrentRegion(); // we should get the regioninfo by the guiTokenId
//		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_RATING, region).getPublicURL();
//		StringBuilder sb = new StringBuilder();
//		sb.append(url);
//		sb.append("/v1/rating/module_config/template/currency/");
//		sb.append(currencyId);
//
//		Map<String, String> rs = httpClient.httpDoPut(sb.toString(), adminToken.getTokenid(),updateBody);
//		Util.checkResponseBody(rs,locale);
//		String failedMessage = Util.getFailedReason(rs);
//		if (!Util.isNullOrEmptyValue(failedMessage))
//			log.error(failedMessage);
//		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//		Currency currency = null;
//		switch (httpCode) {
//		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
//			try {
//				ObjectMapper mapper = new ObjectMapper();
//				JsonNode currencyNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
//				currency = getCurrency(currencyNode);
//			}  catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			break;
//		}
//		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
//			String tokenid = "";// TODO reget the token id
//			try {
//				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
//				tokenid = newToken.getTokenid();
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			rs = httpClient.httpDoPut(sb.toString(), tokenid,updateBody);
//			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//			failedMessage = Util.getFailedReason(rs);
//			if (!Util.isNullOrEmptyValue(failedMessage))
//				log.error(failedMessage);
//			try {
//				ObjectMapper mapper = new ObjectMapper();
//				JsonNode currencyNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
//				currency = getCurrency(currencyNode);
//			}  catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			break;
//		}
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
//		default:
//			throw new ResourceBusinessException(Message.CS_CURRENCY_UPDATE_FAILED,httpCode,locale);
//		}
//		
//		return currency;
//	}
	
//	@Override
//	public void deleteCurrency(String currencyId,TokenOs ostoken) throws BusinessException{
//		Locale locale = new Locale(ostoken.getLocale());
//		TokenOs adminToken = null;
//		try{
//			adminToken = authService.createDefaultAdminOsToken();
//		}catch(Exception e){
//			throw new ResourceBusinessException(Message.CS_CURRENCY_DELETE_FAILED,locale);
//		}
//		
//		String region = ostoken.getCurrentRegion(); // we should get the regioninfo by the guiTokenId
//		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_RATING, region).getPublicURL();
//		StringBuilder sb = new StringBuilder();
//		sb.append(url);
//		sb.append("/v1/rating/module_config/template/currency/");
//		sb.append(currencyId);
//
//		Map<String, String> rs = httpClient.httpDoDelete(sb.toString(), adminToken.getTokenid());
//		Util.checkResponseBody(rs,locale);
//		String failedMessage = Util.getFailedReason(rs);
//		if (!Util.isNullOrEmptyValue(failedMessage))
//			log.error(failedMessage);
//		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//		switch (httpCode) {
//		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE: {
//			break;
//		}
//		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
//			String tokenid = "";// TODO reget the token id
//			try {
//				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
//				tokenid = newToken.getTokenid();
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			rs =  httpClient.httpDoDelete(sb.toString(), tokenid);
//			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//			failedMessage = Util.getFailedReason(rs);
//			if (!Util.isNullOrEmptyValue(failedMessage))
//				log.error(failedMessage);
//			if(httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//			break;
//		}
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
//		default:
//			throw new ResourceBusinessException(Message.CS_CURRENCY_DELETE_FAILED,httpCode,locale);
//		}
//	}

	@Override
	public List<TemplateVersion> getRatingVersions(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException{
		return templateVersionMapper.selectAll();
	}
	
//	@Override
//	public RatingVersion addRatingVersion(String templateId,String createBody,TokenOs ostoken) throws BusinessException{
//		Locale locale = new Locale(ostoken.getLocale());
//		TokenOs adminToken = null;
//		try{
//			adminToken = authService.createDefaultAdminOsToken();
//		}catch(Exception e){
//			throw new ResourceBusinessException(Message.CS_RATING_VERSION_CREATE_FAILED,locale);
//		}
//		
//		String region = ostoken.getCurrentRegion(); // we should get the regioninfo by the guiTokenId
//		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_RATING, region).getPublicURL();
//		StringBuilder sb = new StringBuilder();
//		sb.append(url);
//		sb.append("/v1/rating/module_config/template/");
//		sb.append(templateId);
//		sb.append("/version");
//		
//		HashMap<String, String> headers = new HashMap<String, String>();
//		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
//		
//		Map<String, String> rs = httpClient.httpDoPost(sb.toString(), headers,createBody);
//		Util.checkResponseBody(rs,locale);
//		String failedMessage = Util.getFailedReason(rs);
//		if (!Util.isNullOrEmptyValue(failedMessage))
//			log.error(failedMessage);
//		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//		RatingVersion version = null;
//		switch (httpCode) {
//		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: 
//		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE:{
//			try {
//				ObjectMapper mapper = new ObjectMapper();
//				JsonNode versionNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
//				version = getRatingVersion(versionNode);
//			}  catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			break;
//		}
//		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
//			String tokenid = "";// TODO reget the token id
//			try {
//				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
//				tokenid = newToken.getTokenid();
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
//			rs = httpClient.httpDoPost(sb.toString(), headers,createBody);
//			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//			failedMessage = Util.getFailedReason(rs);
//			if (!Util.isNullOrEmptyValue(failedMessage))
//				log.error(failedMessage);
//			try {
//				ObjectMapper mapper = new ObjectMapper();
//				JsonNode versionNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
//				version = getRatingVersion(versionNode);
//			}  catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			break;
//		}
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
//		default:
//			throw new ResourceBusinessException(Message.CS_RATING_VERSION_CREATE_FAILED,httpCode,locale);
//		}
//		
//		return version;
//	}
	
	@Override
	public TemplateVersion getRatingVersion(String versionId,TokenOs ostoken) throws BusinessException{
		return templateVersionMapper.selectByPrimaryKey(versionId);
	}
	
	@Override
	public TemplateVersion updateRatingVersion(String versionId,String updateBody,TokenOs ostoken) throws BusinessException{
		//TODO
		return new TemplateVersion();
	}
	
	@Override
	public void deleteRatingVersion(String versionId,TokenOs ostoken) throws BusinessException{
		
		TemplateVersion templateVersion = templateVersionMapper.selectByPrimaryKey(versionId);
		if(null == templateVersion)
			return;
		
		if(null != templateTenantMappingMapper.selectByTemplateAndVersionId(templateVersion.getTemplate_id(),versionId))
			throw new ResourceBusinessException(Message.CS_RATING_TEMPLATE_IS_DOING,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
	    templateVersionMapper.deleteByPrimaryKey(versionId);
		
		templateFieldRatingMapper.deleteByTemplateAndVersionId(templateVersion.getTemplate_id(), versionId);
		
		return;
	}
	
	@Override
	public List<TemplateTenantMapping> getTenantRatings(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException{
		return templateTenantMappingMapper.selectAll();
	}
	
	@Override
	public TemplateTenantMapping getTenantRating(String tenantId,TokenOs ostoken) throws BusinessException{
		return templateTenantMappingMapper.selectByTenantId(tenantId);
	}
	

//	private TenantRating replaceRatingUserInfo(String bodyInfo,Locale locale) throws ResourceBusinessException{
//		try {
//			ObjectMapper mapper = new ObjectMapper();
//			TenantRating createRatingInfo = mapper.readValue(bodyInfo, TenantRating.class);
//			String userId = createRatingInfo.getTenant_ids().get(0);
//			List<DomainTenantUser> tenantUsers = domainTenantUserMapper.selectListByUserId(userId);
//			createRatingInfo.getTenant_ids().set(0, tenantUsers.get(0).getOstenantid());
//			return createRatingInfo;
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			log.error(e);
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
//		}
//	}
	
	@Override
	public TemplateTenantMapping addTenantRating(String createBody,TokenOs ostoken,boolean replace) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(createBody);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_TENANT_RATING_CREATE_FAILED, ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		JsonNode usersNode = rootNode.path(ResponseConstant.TENANT_IDS);
		int usersCount = usersNode.size();
		if(0 == usersCount)
			throw new ResourceBusinessException(Message.CS_TENANT_RATING_CREATE_FAILED, ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		List<String> userIds = new ArrayList<String>();
		List<String> tenantIds = new ArrayList<String>();
		for(int index = 0; index < usersCount; ++index){
			userIds.add(usersNode.get(index).textValue());
			List<DomainTenantUser> tenantUsers = domainTenantUserMapper.selectListByUserId(usersNode.get(index).textValue());
			if(null != tenantUsers && 1 == tenantUsers.size())
				tenantIds.add(tenantUsers.get(0).getOstenantid());
		}
		
		String tenantId = Util.listToString(tenantIds, ',');
		TemplateTenantMapping tenantMapping = templateTenantMappingMapper.selectByTenantId(tenantId);
		if(tenantMapping == null){
			tenantMapping = new TemplateTenantMapping();
			tenantMapping.setTenant_mapping_id(Util.makeUUID());	
		}
		tenantMapping.setVersion_id(rootNode.path(ResponseConstant.VERSION_ID).textValue());
		tenantMapping.setTemplate_id(rootNode.path(ResponseConstant.TEMPLATE_ID).textValue());
		tenantMapping.setTenant_discount(rootNode.path(ResponseConstant.DISCOUNT).floatValue());
		tenantMapping.setTenant_id(tenantId);
		
		templateTenantMappingMapper.insertOrUpdate(tenantMapping);
		
		RatingTemplate template = ratingTemplateMapper.selectByPrimaryKey(tenantMapping.getTemplate_id());
		TemplateVersion version = templateVersionMapper.selectByPrimaryKey(tenantMapping.getVersion_id());
		
		updateCloudUserRatingInfo(template,version,Util.listToString(userIds, ','));
		return tenantMapping;
	}
	
	private void updateCloudUserRatingInfo(RatingTemplate template,TemplateVersion version,String userId){
		CloudUser cloudUser = cloudUserMapper.selectByPrimaryKey(userId);
		if (null != cloudUser) {
			cloudUser.setTemplateId(template.getTemplate_id());
			cloudUser.setVersion(version.getName());
			cloudUser.setUnitPriceName(template.getName());
			// cloudUser.setVersionName(rating.getVersion_name());
			cloudUserMapper.updateByPrimaryKeySelective(cloudUser);
		}
	}
	
	@Override
	public TemplateTenantMapping updateTenantRating(String tenantId,String updateBody,TokenOs ostoken) throws BusinessException{
		//TODO
		return new TemplateTenantMapping();
		
	}
	
	@Override
	public void deleteTenantRating(String tenantId,TokenOs ostoken) throws BusinessException{
		
		TemplateTenantMapping tenantMapping = templateTenantMappingMapper.selectByTenantId(tenantId);
		if(null == tenantMapping)
			return;
		String versionId = tenantMapping.getVersion_id();
		String templateId = tenantMapping.getTemplate_id();
		
		templateFieldRatingMapper.deleteByTemplateAndVersionId(templateId, versionId);
		templateTenantMappingMapper.deleteByPrimaryKey(tenantMapping.getTenant_mapping_id());
	}
	
	private List<TemplateField> getTemplateServiceFileds(String serviceId){
		if(Util.isNullOrEmptyValue(serviceId))
			return null;
		return templateFieldMapper.selectFiledsByServiceId(serviceId);
	}
	
	@Override
	public List<TemplateService> getTemplateServices() {
	   return templateServiceMapper.selectAll();
	}
	

	@Override
	public List<TemplateService> getRatingServices(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException{
		List<TemplateService> services = templateServiceMapper.selectAll();
		if(Util.isNullOrEmptyList(services))
			return new ArrayList<TemplateService>();
		Locale locale = new Locale(ostoken.getLocale());
		for(TemplateService service : services){
			normalFieldsInfo(service,locale);
		}
		return services;
	}
	
	@Override
	public TemplateService getRatingService(String serviceId,TokenOs ostoken) throws BusinessException{
		TemplateService service =  templateServiceMapper.selectByPrimaryKey(serviceId);
		if(null == service)
			return new TemplateService();
		service.setFields(templateFieldMapper.selectFiledsByServiceId(service.getService_id()));
		normalFieldsInfo(service,new Locale(ostoken.getLocale()));
		return service;
	}

	@Override
	public void addRatingService(List<TemplateService> services,TokenOs ostoken) throws BusinessException{
	   if(Util.isNullOrEmptyList(services))
		   return;
	   for(TemplateService service : services){
		   String serviceId = Util.makeUUID();
		   service.setService_id(serviceId);
		   service.setMillionSeconds(Util.getCurrentMillionsecond());
		   templateServiceMapper.insertSelective(service);
		   
		   List<TemplateField> fields = service.getFields();
		   if(Util.isNullOrEmptyList(fields))
			   continue;
		   for(TemplateField field : fields){
			   field.setService_id(serviceId);
			   field.setField_id(Util.makeUUID());
			   field.setMillionSeconds(Util.getCurrentMillionsecond());
		   }
		   templateFieldMapper.insertOrUpdateBatch(fields);
	   }
	}
	
	@Override
	public TemplateService addRatingService(String createBody,TokenOs ostoken) throws BusinessException{
		//TODO
		return new TemplateService();
	}
	
	@Override
	public TemplateField getRatingPolicy(String serviceId,String fieldId,TokenOs ostoken) throws BusinessException{
		return templateFieldMapper.selectByPrimaryKey(fieldId);
	}
	
	@Override
	public TemplateField addRatingPolicy(String serviceCode,String fieldCode,TokenOs ostoken) throws BusinessException{
		TemplateService service = templateServiceMapper.selectByServiceCode(serviceCode);
		if(null == service)
			return null;
		TemplateField field = null;
		if(serviceCode.equals(ParamConstant.COMPUTE)){
			field = addComputeRatingPolicy(service,serviceCode,fieldCode,ostoken);
		}else if(serviceCode.equals(ParamConstant.STORAGE)){
			field = addStorageRatingPolicy(service,serviceCode,fieldCode,ostoken);
		}
		return field;
	}
	

	@Override
	public void deleteRatingPolicy(String serviceCode,String fieldCode,TokenOs ostoken) throws BusinessException{
		TemplateService service = templateServiceMapper.selectByServiceCode(serviceCode);
		if(null == service)
			return ;
		if(serviceCode.equals(ParamConstant.COMPUTE))
			deleteComputeRatingPolicy(service,serviceCode,fieldCode,ostoken);
		else if(serviceCode.equals(ParamConstant.STORAGE))
			deleteStorageRatingPolicy(service,serviceCode,fieldCode,ostoken);
	}
	
	
	
	private void normalFieldsInfo(TemplateService service,Locale locale){
	//	String coreType = "CS_CORE_TYPE_NAME";
	//	String ramType  = "CS_RAM_TYPE_NAME";
	//	String volumeType = "CS_VOLUME_TYPE_NAME";
		String networkType = "CS_FLOTINGIP_TYPE_NAME";
		String serviceType = "CS_SERVICE_TYPE_NAME";
		List<TemplateField> fields = templateFieldMapper.selectFiledsByServiceId(service.getService_id());
		if(null == fields)
			return;
		List<TemplateField> ratingFields = new ArrayList<TemplateField>();
		if(service.getService_code().equals(ParamConstant.COMPUTE)){
			Map<String,String> instanceTypeMap = getInstanceTypeNames();
			for (TemplateField field : fields) {
				if(field.getRating() == false)
					continue;
				String type = field.getField_code().substring(0, field.getField_code().indexOf('_'));
				String name = instanceTypeMap.get(type);
	//			String typeName = null;
				if (field.getField_code().contains(ParamConstant.CORE)) {
					if (null != name) {
						if (locale.getLanguage().contains("zh"))
							name = StringHelper.ncr2String(name)
									+ Message.getMessage(Message.CS_CPU_NAME, locale, false);
						else
							name = StringHelper.ncr2String(name) + " "
									+ Message.getMessage(Message.CS_CPU_NAME, locale, false);
					}
			//		typeName = coreType.replaceFirst("TYPE", type.toUpperCase());
				} else {
					if (null != name) {
						if (locale.getLanguage().contains("zh"))
							name = StringHelper.ncr2String(name)
									+ Message.getMessage(Message.CS_MEMORY_NAME, locale, false);
						else
							name = StringHelper.ncr2String(name) + " "
									+ Message.getMessage(Message.CS_MEMORY_NAME, locale, false);
					}
			//		typeName = ramType.replaceFirst("TYPE", type.toUpperCase());
				}
				if (null != name) {
					field.setName(name);
				} else {
					//field.setName(Message.getMessage(typeName, locale, false));
					field.setName(field.getField_code());
				}
				field.setDefault_chargekey(null);
				field.setMillionSeconds(null);
				field.setService_id(null);
				ratingFields.add(field);
			}
		}else if(service.getService_code().equals(ParamConstant.STORAGE)){
			List<VolumeType> volumeTypes = volumeTypeMapper.selectAll();
			for (TemplateField field : fields){
				String typeName = getVolumeTypeVisibleName(volumeTypes,field.getField_code());
				if(Util.isNullOrEmptyValue(typeName))
    				typeName = field.getField_code();//Message.getMessage(field.getField_code().toUpperCase(), locale,false);
    			field.setName(typeName);
		//		String typeName = volumeType.replaceFirst("TYPE", field.getField_code().toUpperCase());
		//		field.setName(Message.getMessage(typeName, locale,false));
				field.setDefault_chargekey(null);
				field.setMillionSeconds(null);
				field.setService_id(null);
				ratingFields.add(field);
			}
		}else if(service.getService_code().equals(ParamConstant.NETWORK)){
			for (TemplateField field : fields){
				String typeName = networkType.replaceFirst("TYPE", field.getField_code().toUpperCase());
				field.setName(Message.getMessage(typeName, locale,false));
				field.setDefault_chargekey(null);
				field.setMillionSeconds(null);
				field.setService_id(null);
				ratingFields.add(field);
			}
		}else if(service.getService_code().equals(ParamConstant.EXTEND_SERVICE)){
			for (TemplateField field : fields){
				String typeName = serviceType.replaceFirst("TYPE", field.getField_code().toUpperCase());
				field.setName(Message.getMessage(typeName, locale,false));
				field.setDefault_chargekey(null);
				field.setMillionSeconds(null);
				field.setService_id(null);
				ratingFields.add(field);
			}	
		}else{
			for (TemplateField field : fields){
				field.setName(field.getField_code());
				field.setDefault_chargekey(null);
				field.setMillionSeconds(null);
				field.setService_id(null);
				ratingFields.add(field);
			}	
		}
		service.setFields(ratingFields);
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
	
	private Map<String,String> getInstanceTypeNames() {
		Map<String,String> typeMap = new HashMap<String,String>();
		List<HostAggregate> aggregates = hostAggregateMapper.selectAll();
		if (null != aggregates) {
			for (HostAggregate aggregate : aggregates) {
				typeMap.put(aggregate.getAvailabilityZone(),StringHelper.ncr2String(aggregate.getName()));
			}
		} 
		return typeMap;
	}
	
	private String getInstanceTypes() {
		List<HostAggregate> aggregates = hostAggregateMapper.selectAll();
		String instanceTypeSpec = null;
		if (null != aggregates) {
			List<String> zones = new ArrayList<String>();
			for (HostAggregate aggregate : aggregates) {
				zones.add(aggregate.getAvailabilityZone());
			}
			instanceTypeSpec = Util.listToString(zones, ',');
		} /*else {
			instanceTypeSpec = cloudconfig.getSystemInstanceSpec();
			if (!Util.isNullOrEmptyValue(cloudconfig.getSystemVdiSpec())) {
				instanceTypeSpec += ",";
				instanceTypeSpec += cloudconfig.getSystemVdiSpec();
			}
			if (!Util.isNullOrEmptyValue(cloudconfig.getSystemBaremetalSpec())) {
				instanceTypeSpec += ",";
				instanceTypeSpec += cloudconfig.getSystemBaremetalSpec();
			}
			if (!Util.isNullOrEmptyValue(cloudconfig.getSystemContainerSpec())) {
				instanceTypeSpec += ",";
				instanceTypeSpec += cloudconfig.getSystemContainerSpec();
			}
			if (!Util.isNullOrEmptyValue(cloudconfig.getSystemDBSpec())) {
				instanceTypeSpec += ",";
				instanceTypeSpec += cloudconfig.getSystemDBSpec();
			}
		}*/
		return instanceTypeSpec;
	}
	
	private String getVolumeTypes() {
		List<VolumeType> volumeTypes = volumeTypeMapper.selectAll();
		String volumeTypeSpec = null;
		if (null != volumeTypes) {
			List<String> types = new ArrayList<String>();
			for (VolumeType volumeType : volumeTypes) {
				types.add(volumeType.getName());
			}
			volumeTypeSpec = Util.listToString(types, ',');
		} /*else {
			volumeTypeSpec = cloudconfig.getSystemVolumeSpec();
		}*/
		return volumeTypeSpec;
	}
	
	private void deleteComputeRatingPolicy(TemplateService service,String serviceCode,String fieldCode,TokenOs ostoken){
		TemplateField cpuField = templateFieldMapper.selectByFieldCodeAndServiceId(fieldCode+"_core",service.getService_id());
		TemplateField ramField = templateFieldMapper.selectByFieldCodeAndServiceId(fieldCode+"_ram",service.getService_id());
        if(null != cpuField){
        	List<TemplateFieldRating> fieldRating = templateFieldRatingMapper.selectByFieldAndServiceId(cpuField.getField_id(), service.getService_id());
        	if(Util.isNullOrEmptyList(fieldRating))
        		templateFieldMapper.deleteByPrimaryKey(cpuField.getField_id());
        }
        
        if(null != ramField){
        	List<TemplateFieldRating> fieldRating = templateFieldRatingMapper.selectByFieldAndServiceId(ramField.getField_id(), service.getService_id());
        	if(Util.isNullOrEmptyList(fieldRating))
        		templateFieldMapper.deleteByPrimaryKey(ramField.getField_id());
        }
	}
	
	private void deleteStorageRatingPolicy(TemplateService service,String serviceCode,String fieldCode,TokenOs ostoken){
		TemplateField field = templateFieldMapper.selectByFieldCodeAndServiceId(fieldCode,service.getService_id());
        if(null != field){
        	List<TemplateFieldRating> fieldRating = templateFieldRatingMapper.selectByFieldAndServiceId(field.getField_id(), service.getService_id());
        	if(Util.isNullOrEmptyList(fieldRating))
        		templateFieldMapper.deleteByPrimaryKey(field.getField_id());
        }
	}
	
	private TemplateField addComputeRatingPolicy(TemplateService service,String serviceCode,String fieldCode,TokenOs ostoken) throws BusinessException{
		TemplateField cpuField = templateFieldMapper.selectByFieldCodeAndServiceId(fieldCode+"_core",service.getService_id());
		if(null == cpuField){
			cpuField = new TemplateField();
			cpuField.setField_id(Util.makeUUID());
			cpuField.setField_code(fieldCode+"_core");
			cpuField.setDefault_chargekey(fieldCode+"_core");
			cpuField.setService_id(service.getService_id());
			cpuField.setRating(true);
			
			TemplateField ramField = new TemplateField();
			ramField.setField_id(Util.makeUUID());
			ramField.setField_code(fieldCode+"_ram");
			ramField.setDefault_chargekey(fieldCode+"_ram");
			ramField.setService_id(service.getService_id());
			ramField.setRating(true);
			
			List<TemplateField> fields = new ArrayList<TemplateField>();
			fields.add(cpuField);
			fields.add(ramField);
			templateFieldMapper.insertOrUpdateBatch(fields);
		}
		return cpuField;
	}
	
	private TemplateField addStorageRatingPolicy(TemplateService service,String serviceCode,String fieldCode,TokenOs ostoken) throws BusinessException{
		TemplateField field = templateFieldMapper.selectByFieldCodeAndServiceId(fieldCode,service.getService_id());
		if(null == field){
			field = new TemplateField();
			field.setField_id(Util.makeUUID());
			field.setField_code(fieldCode);
			field.setDefault_chargekey(fieldCode);
			field.setService_id(service.getService_id());
			field.setRating(true);
			
			templateFieldMapper.insertOrUpdate(field);
		}
		return field;
	}
}
