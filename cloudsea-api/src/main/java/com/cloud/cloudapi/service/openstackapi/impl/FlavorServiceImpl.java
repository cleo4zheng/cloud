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

import com.cloud.cloudapi.dao.common.FlavorMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.FlavorJSON;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Flavor;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.impl.AuthServiceImpl;
import com.cloud.cloudapi.service.openstackapi.FlavorService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("flavorService")
public class FlavorServiceImpl  implements FlavorService{
	
	@Resource
	private OSHttpClientUtil httpClient;

	@Autowired
	private CloudConfig cloudconfig;
	
	@Autowired
	private FlavorMapper flavorMapper;
   
	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(FlavorServiceImpl.class);
	
	public CloudConfig getCloudconfig() {
		return cloudconfig;
	}

	public void setCloudconfig(CloudConfig cloudconfig) {
		this.cloudconfig = cloudconfig;
	}

	public FlavorMapper getFlavorMapper() {
		return flavorMapper;
	}

	public void setFlavorMapper(FlavorMapper flavorMapper) {
		this.flavorMapper = flavorMapper;
	}
	
	@Override
	public List<Flavor> getFlavorList(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException{
		int limitItems = Util.getLimit(paramMap);
		String type = paramMap != null ? paramMap.get(ParamConstant.TYPE) : null;
		List<Flavor> flavorsFromDB = getFlavorsFromDB(limitItems,type);
		
		if(!Util.isNullOrEmptyList(flavorsFromDB))
			return flavorsFromDB;
		//todo 1: 通过guitokenid 取得实际，用户信息
        //AuthService	as = new AuthServiceImpl();	
        //as.GetTokenOS(guiTokenId);
		String region = ostoken.getCurrentRegion();//we should get the regioninfo by the guiTokenId
		
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();		
		url=RequestUrlHelper.createFullUrl(url+"/flavors/detail", null);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String>  rs = httpClient.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		List<Flavor> flavors = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				flavors = getFlavors(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			rs = httpClient.httpDoGet(url, headers);
		    httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
		    if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				flavors = getFlavors(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_FLAVOR_GET_FAILED,httpCode,locale);
		}
		
		storeFlavorsToDB(flavors);
		return flavors;
	}
	
	@Override
	public Flavor getFlavor(String flavorId,TokenOs ostoken) throws BusinessException{
		Flavor flavor = flavorMapper.selectByPrimaryKey(flavorId);
		if(null != flavor)
			return flavor;
		
		//todo 1: 通过guitokenid 取得实际，用户信息
        //AuthService	as = new AuthServiceImpl();	
        //as.GetTokenOS(guiTokenId);
		String region = ostoken.getCurrentRegion();//we should get the regioninfo by the guiTokenId
		
		String url=ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/flavors/");
		sb.append(flavorId);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String>  rs = httpClient.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				flavor = getFlavor(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			rs = httpClient.httpDoGet(sb.toString(), headers);
		    httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				flavor = getFlavor(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_FLAVOR_DETAIL_GET_FAILED,httpCode,locale);
		}
		
		storeFlavor2DB(flavor);
		return flavor;
	}
	
	
	@Override
	public String getFlavor(TokenOs ostoken, String flavor_vcpus, String flavor_ram, String flavor_disk ,
			String cpu_arch, String type) throws BusinessException{
		int disk = Integer.parseInt(flavor_disk); 
		int vcpus = Integer.parseInt(flavor_vcpus);
		int ram = Integer.parseInt(flavor_ram);
		
		int id = 0;
		String flavorName;
		if (cpu_arch == null) 
			flavorName = String.format("%s_%s_%s_Flavor", vcpus, ram, disk);
		else
			flavorName = String.format("%s_%s_%s_%s_Flavor", vcpus, ram, disk, cpu_arch);
		Flavor decidedFlavor = flavorMapper.selectByName(flavorName);
		if(null != decidedFlavor)
			return decidedFlavor.getId();
		List<Flavor> list = getFlavorList(null, ostoken);
		if (!Util.isNullOrEmptyList(list)) {
			for (Flavor flavor : list) {
				if (flavor.getDisk() == disk && flavor.getRam() == ram && flavor.getVcpus() == vcpus)
					return flavor.getId();
				int flavor_id = Integer.parseInt(flavor.getId());
				if (id < flavor_id)
					id = flavor_id;
			}
		}

		// if can not find flavor,create the flavor
		Flavor flavorCreate = new Flavor();
	//	String name = String.format("%s_%s_%s_Flavor", vcpus, ram, disk);
		flavorCreate.setName(flavorName);
		flavorCreate.setId(Integer.toString(++id));
		flavorCreate.setDisk(disk);
		flavorCreate.setRam(ram);
		flavorCreate.setVcpus(vcpus);
		FlavorJSON flavorJson = new FlavorJSON(flavorCreate);


		JsonHelper<FlavorJSON, String> jsonHelp = new JsonHelper<FlavorJSON, String>();
		decidedFlavor = createFlavor(jsonHelp.generateJsonBodyWithEmpty(flavorJson), ostoken, type);
		if (null == decidedFlavor)
			return null;
		if (cpu_arch != null ){
			String extra_fomat = "{\"extra_specs\": {\"cpu_arch\": \"%s\"}}";
			String extra = String.format(extra_fomat, cpu_arch);
			createFlavorExtraSpecs(decidedFlavor.getId(), extra, ostoken);
		}
		
		return decidedFlavor.getId();
		
	}
	
	private String makeFlavor(TokenOs ostoken, String flavor_vcpus, String flavor_ram, String flavor_disk,Boolean vmwareZone,String type) throws BusinessException{
		if(null == flavor_disk)
			flavor_disk = "0";
		int disk = Integer.parseInt(flavor_disk); 
		int vcpus = Integer.parseInt(flavor_vcpus);
		int ram = Integer.parseInt(flavor_ram);
		int id = 0;
		String flavorName = null;
		if(true == vmwareZone)
			flavorName = String.format("%s_%s_%s_Flavor", vcpus, ram, disk);
		else
			flavorName = String.format("%s_%s_%s_Flavor", vcpus, ram, 0);
		// First get the flavor from the local DB
		Flavor decidedFlavor = flavorMapper.selectByName(flavorName);
		if(null != decidedFlavor)
			return decidedFlavor.getId();
		List<Flavor> list = getFlavorList(null, ostoken);
		if (!Util.isNullOrEmptyList(list)) {
			for (Flavor flavor : list) {
				if (flavor.getDisk() == disk && flavor.getRam() == ram && flavor.getVcpus() == vcpus)
					return flavor.getId();
				int flavor_id = Integer.parseInt(flavor.getId());
				if (id < flavor_id)
					id = flavor_id;
			}
		}

		// if can not find flavor,create the flavor
		Flavor flavorCreate = new Flavor();
	//	String name = String.format("%s_%s_%s_Flavor", vcpus, ram, disk);
		flavorCreate.setName(flavorName);
		flavorCreate.setId(Integer.toString(++id));
		if(true == vmwareZone)
			flavorCreate.setDisk(disk);
		else
			flavorCreate.setDisk(0);
		flavorCreate.setRam(ram);
		flavorCreate.setVcpus(vcpus);
		FlavorJSON flavorJson = new FlavorJSON(flavorCreate);


		JsonHelper<FlavorJSON, String> jsonHelp = new JsonHelper<FlavorJSON, String>();
		decidedFlavor = createFlavor(jsonHelp.generateJsonBodyWithEmpty(flavorJson), ostoken,type);
		if (null == decidedFlavor)
			return null;
		// TODO save the decidedFlavor to the local db
		return decidedFlavor.getId();
	}
	
	@Override
	public String getFlavor(TokenOs ostoken, String flavor_vcpus, String flavor_ram, String flavor_disk,Boolean vmwareZone,String type)
			throws BusinessException{
		return makeFlavor(ostoken,flavor_vcpus,flavor_ram,flavor_disk,vmwareZone,type);
	}
	
	@Override
	public String getFlavor(TokenOs ostoken, String flavor_vcpus, String flavor_ram, String flavor_disk,Boolean vmwareZone)
			throws BusinessException {
		return makeFlavor(ostoken,flavor_vcpus,flavor_ram,flavor_disk,vmwareZone,ParamConstant.INSTANCE_TYPE);
	}
	
	@Override
	public Flavor createFlavor(String createBody,TokenOs ostoken,String type) throws BusinessException{
		//todo 1: 通过guitokenid 取得实际，用户信息
        //AuthService	as = new AuthServiceImpl();	
        //as.GetTokenOS(guiTokenId);
		Locale locale = new Locale(ostoken.getLocale());
		AuthServiceImpl authService =  new AuthServiceImpl();
		authService.setCloudconfig(cloudconfig);
		TokenOs adminToken = null;
		try {
			adminToken = authService.createDefaultAdminOsToken();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,locale);
		}
		
		String region = ostoken.getCurrentRegion();
		
		//String url=ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		url=RequestUrlHelper.createFullUrl(url+"/flavors", null);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		//headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
		
	//	String flavorBody = generateBody(paramMap);
		Map<String, String>  rs = httpClient.httpDoPost(url, headers,createBody);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		Flavor flavor = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				flavor = getFlavor(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			rs = httpClient.httpDoPost(url, headers,createBody);
		    httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
		    if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				flavor = getFlavor(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_FLAVOR_CREATE_FAILED,httpCode,locale);
		}
		
		flavor.setCreatedAt(Util.getCurrentDate());
		flavor.setType(type);
		storeFlavor2DB(flavor);
		return flavor;
	}
	
	@Override
	public Flavor createFlavorExtraSpecs(String flavorId,String createBody,TokenOs ostoken) throws BusinessException{
		//todo 1: 通过guitokenid 取得实际，用户信息
        //AuthService	as = new AuthServiceImpl();	
        //as.GetTokenOS(guiTokenId);
		Locale locale = new Locale(ostoken.getLocale());
		AuthServiceImpl authService =  new AuthServiceImpl();
		authService.setCloudconfig(cloudconfig);
		TokenOs adminToken = null;
		try {
			adminToken = authService.createDefaultAdminOsToken();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,locale);
		}
		
		String region = ostoken.getCurrentRegion();
		
		//String url=ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		url=RequestUrlHelper.createFullUrl(url+"/flavors/", null);
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append(flavorId);
		sb.append("/os-extra_specs");
		
		HashMap<String, String> headers = new HashMap<String, String>();
		//headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
		
	//	String flavorBody = generateBody(paramMap);
		Map<String, String>  rs = httpClient.httpDoPost(sb.toString(), headers,createBody);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		Flavor flavor = flavorMapper.selectByPrimaryKey(flavorId);
		String extra = "";
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				extra = getFlavorExtraSpecs(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			rs = httpClient.httpDoPost(sb.toString(), headers,createBody);
		    httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
		    if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				extra = getFlavorExtraSpecs(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_FLAVOR_CREATE_FAILED,httpCode,locale);
		}
		
		if(null != flavor)
			flavor.setExtra(extra);
		storeFlavor2DB(flavor);
		return flavor;
	}
	
	private List<Flavor> getFlavorsFromDB(int limitItems, String type){
		List<Flavor> flavorsFromDB = null;
		if(-1 == limitItems){
			if (type != null && !"".equals(type)){
				flavorsFromDB = flavorMapper.selectByType(type);
			}else{
				flavorsFromDB = flavorMapper.selectAll();
			}	
		}else{
			flavorsFromDB = flavorMapper.selectListWithLimit(limitItems);
		}
		return flavorsFromDB;
	}
	
	private void storeFlavor2DB(Flavor flavor){
		if(null == flavor)
			return;
		if(null != flavorMapper.selectByPrimaryKey(flavor.getId()))
			flavorMapper.updateByPrimaryKeySelective(flavor);
		else
			flavorMapper.insertSelective(flavor);
	}
	
	private List<Flavor> storeFlavorsToDB(List<Flavor> flavors){
		if(Util.isNullOrEmptyList(flavors))
			return null;
		for(Flavor flavor : flavors){
			flavor.setType(ParamConstant.INSTANCE_TYPE);
			storeFlavor2DB(flavor);
		}
		return null;
	}
	
	private Flavor getFlavorInfo(JsonNode flavorNode){
		if(null == flavorNode)
			return null;
		Flavor flavor = new Flavor();
		flavor.setId(flavorNode.path(ResponseConstant.ID).textValue());
		flavor.setName(flavorNode.path(ResponseConstant.NAME).textValue());
		flavor.setRam(flavorNode.path(ResponseConstant.RAM).intValue());
		flavor.setVcpus(flavorNode.path(ResponseConstant.VCPUS).intValue());
		flavor.setDisk(flavorNode.path(ResponseConstant.DISK).intValue());
		flavor.setSwap(flavorNode.path(ResponseConstant.SWAP).intValue());
		flavor.setRxtx_factor(flavorNode.path(ResponseConstant.RXTX_FACTOR).floatValue());
		return flavor;
	}
	
	private String getFlavorExtraSpecs(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode extraSpecNode=rootNode.path(ResponseConstant.EXTRA_SPECS);
		return mapper.writeValueAsString(extraSpecNode);
	}
	
	private Flavor getFlavor(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode flavorNode=rootNode.path(ResponseConstant.FLAVOR);
		return getFlavorInfo(flavorNode);
	}
	
	private List<Flavor> getFlavors(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode flavorsNode=rootNode.path(ResponseConstant.FLAVORS);
		int flavorsCount =flavorsNode.size();
        if(0 == flavorsCount)
        	return null;
        List<Flavor> flavors= new ArrayList<Flavor>();	
		for(int index = 0; index < flavorsCount; ++index){
			Flavor flavorInfo = getFlavorInfo(flavorsNode.get(index));
			if(null == flavorInfo)
				continue;
			flavors.add(flavorInfo);
	     }
		return flavors;
	}
	
//	private String generateBody(Map<String,String> paramMap){
//		if(null == paramMap || 0 == paramMap.size())
//			return "";
//		    
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
//        mapper.setSerializationInclusion(Include.NON_NULL);
//        mapper.setSerializationInclusion(Include.NON_EMPTY);
//       
//        Flavor flavor = new Flavor();
//        flavor.setName(paramMap.get(ParamConstant.NAME));
//        flavor.setRam(Integer.parseInt(paramMap.get(ParamConstant.RAM)));
//        flavor.setVcpus(Integer.parseInt(paramMap.get(ParamConstant.VCPUS)));
//        flavor.setDisk(Integer.parseInt(paramMap.get(ParamConstant.DISK)));
//        flavor.setId(paramMap.get(ParamConstant.ID));
//        flavor.setSwap(Integer.parseInt(paramMap.get(ParamConstant.SWAP)));
//        flavor.setRxtx_factor(Float.parseFloat(paramMap.get(ParamConstant.RXTX_FACTOR)));
// 
//        FlavorJSON flavorJSON = new FlavorJSON(flavor);
//        String jsonStr = "";
//		try {
//			jsonStr = mapper.writeValueAsString(flavorJSON);
//		} catch (JsonProcessingException e) {
//			// TODO Auto-generated catch block
//			log.error(e);
//		}
//        return jsonStr; 
//	}
//	
//    private List<Flavor> filterFlavor(Map<String,String> paramMap,List<Flavor> flavors){
//		if(null == paramMap || 0 == paramMap.size())
//			return flavors;
//
//		String flavorName = paramMap.get(ParamConstant.NAME);
//		if(null != flavorName && !"".equals(flavorName)){
//		    for(Flavor flavor:flavors){  
//		        if(flavorName.equals(flavor.getName())){
//		        	List<Flavor> goodFlavors= new ArrayList<Flavor>();	
//		        	goodFlavors.add(flavor);
//		        	return goodFlavors;
//		        }
//		    } 
//		}
//		
//		String strLimit = paramMap.get(ParamConstant.LIMIT);
//		if(null != strLimit && !"".equals(strLimit)){
//			try{
//				int limit = Integer.parseInt(strLimit);
//				if(limit >= flavors.size())
//					return flavors;
//				return flavors.subList(0, limit);
//			}catch(Exception e){
//				//TODO
//				return flavors;
//			}
//		}
//	
//		
//    	return flavors;
//	}
}
