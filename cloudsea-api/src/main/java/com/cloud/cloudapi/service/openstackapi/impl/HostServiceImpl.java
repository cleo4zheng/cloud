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

import com.cloud.cloudapi.dao.common.BackupMapper;
import com.cloud.cloudapi.dao.common.CloudServiceMapper;
import com.cloud.cloudapi.dao.common.FirewallMapper;
import com.cloud.cloudapi.dao.common.HostAggregateMapper;
import com.cloud.cloudapi.dao.common.HostDetailMapper;
import com.cloud.cloudapi.dao.common.HostMapper;
import com.cloud.cloudapi.dao.common.ImageMapper;
import com.cloud.cloudapi.dao.common.InstanceMapper;
import com.cloud.cloudapi.dao.common.LoadbalancerMapper;
import com.cloud.cloudapi.dao.common.NetworkMapper;
import com.cloud.cloudapi.dao.common.ResourceMapper;
import com.cloud.cloudapi.dao.common.SubnetMapper;
import com.cloud.cloudapi.dao.common.VolumeMapper;
import com.cloud.cloudapi.dao.common.VolumeTypeMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.CloudService;
import com.cloud.cloudapi.pojo.openstackapi.forgui.EnvResource;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Host;
import com.cloud.cloudapi.pojo.openstackapi.forgui.HostAggregate;
import com.cloud.cloudapi.pojo.openstackapi.forgui.HostDetail;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceSpec;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceUsedInfo;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeType;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.HostService;
import com.cloud.cloudapi.service.rating.RatingTemplateService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.StringHelper;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("hostService")
public class HostServiceImpl implements HostService {
	@Resource
	private OSHttpClientUtil httpClient;

	@Autowired
	private HostMapper hostMapper;

	@Autowired
	private HostDetailMapper hostDetailMapper;

	@Autowired
	private ResourceMapper resourceMapper;

	@Autowired
	private InstanceMapper instanceMapper;

	@Autowired
	private ImageMapper imageMapper;
	
	@Autowired
	private VolumeMapper volumeMapper;

	@Autowired
	private BackupMapper backupMapper;

	@Autowired
	private SubnetMapper subnetMapper;

	@Autowired
	private LoadbalancerMapper lbMapper;

	@Autowired
	private FirewallMapper fwMapper;
	
	@Autowired
	private NetworkMapper networkMapper;
	
	@Autowired
	private VolumeTypeMapper volumeTypeMapper;
	
	@Autowired
	private HostAggregateMapper hostAggregateMapper;
	
	@Autowired
	private CloudConfig cloudconfig;

	@Resource
	private AuthService authService;

	@Resource
	private CloudServiceMapper serviceMapper;
	
	@Resource
	private RatingTemplateService ratingService;
	
	
	private Logger log = LogManager.getLogger(HostServiceImpl.class);
	 
	@Override
	public List<Host> getHostList(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {

		List<Host> hostsFromDB = getHostsFromDB();
		if (!Util.isNullOrEmptyList(hostsFromDB)){
			return hostsFromDB;
		}
		Locale locale = new Locale(ostoken.getLocale());  
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		TokenOs adminToken = null;
		try{
			adminToken = authService.createDefaultAdminOsToken();
		}catch(Exception e){
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		  
		String region = ostoken.getCurrentRegion();
		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/os-hosts", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
     
		Map<String, String> rs = httpClient.httpDoGet(url, headers);
		Util.checkResponseBody(rs, locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		List<Host> hosts = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				hosts = getHosts(rs);
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
				hosts = getHosts(rs);
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
			throw new ResourceBusinessException(Message.CS_HOST_GET_FAILED,httpCode,locale);
		}

		List<Host> hostWithDetails = makeHostDetails(hosts, adminToken);
		if (null == hostWithDetails)
			hostWithDetails = new ArrayList<Host>();
		Host hostStorage = getStorageInfo(adminToken);
		if (null != hostStorage)
			hostWithDetails.add(hostStorage);
		hostWithDetails.add(makeFloatigIPInfoHost(locale));
		storeHostsToDB(hostWithDetails);
		
		setEnvResourceQuota(hostWithDetails);

		return hostWithDetails;
	}
	

	@Override
	public List<HostAggregate> getHostAggregates(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException{
		
		List<HostAggregate> aggregates = hostAggregateMapper.selectAll();
		if(!Util.isNullOrEmptyList(aggregates)){
			setHostsInfo(aggregates);
			return aggregates;
		}
		Locale locale = new Locale(ostoken.getLocale());  
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		TokenOs adminToken = null;
		try{
			adminToken = authService.createDefaultAdminOsToken();
		}catch(Exception e){
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		  
		String region = ostoken.getCurrentRegion();
		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/os-aggregates", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
     
		Map<String, String> rs = httpClient.httpDoGet(url, headers);
		Util.checkResponseBody(rs, locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				aggregates = getHostAggregates(rs);
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
				aggregates = getHostAggregates(rs);
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
			throw new ResourceBusinessException(Message.CS_HOST_AGGREGATE_GET_FAILED,httpCode,locale);
		}
		setHostsInfo(aggregates);
		makeHostAggregateServiceMapping(aggregates);
		if(!Util.isNullOrEmptyList(aggregates)){
		   hostAggregateMapper.insertOrUpdateBatch(aggregates);	
		}
		return aggregates;
	}
	
	private void makeHostAggregateServiceMapping(List<HostAggregate> aggregates){
		if(Util.isNullOrEmptyList(aggregates))
			return;
		List<CloudService> services = serviceMapper.selectAll();
		if(Util.isNullOrEmptyList(services))
			return;
		for(HostAggregate aggregate : aggregates){
			if(aggregate.getAvailabilityZone().equals(ParamConstant.KVM_ZONE)){
				for(CloudService service : services){
					if(service.getType().equals(ParamConstant.INSTANCE_TYPE)){
						aggregate.setServiceId(service.getId());
						break;
					}
				}
			} else if (aggregate.getAvailabilityZone().equals(ParamConstant.VDI_ZONE)) {
				for (CloudService service : services) {
					if (service.getType().equals(ParamConstant.VDI_TYPE)) {
						aggregate.setServiceId(service.getId());
						break;
					}
				}
			}else if(aggregate.getAvailabilityZone().equals(ParamConstant.DATABASE_ZONE)){
				for (CloudService service : services) {
					if (service.getType().equals(ParamConstant.DATABASE_TYPE)) {
						aggregate.setServiceId(service.getId());
						break;
					}
				}
			}else if(aggregate.getAvailabilityZone().equals(ParamConstant.CONTAINER_ZONE)){
				for (CloudService service : services) {
					if (service.getType().equals(ParamConstant.CONTAINER_TYPE)) {
						aggregate.setServiceId(service.getId());
						break;
					}
				}
			}else if(aggregate.getAvailabilityZone().equals(ParamConstant.BAREMETAL_ZONE)){
				for (CloudService service : services) {
					if (service.getType().equals(ParamConstant.BAREMETAL_TYPE)) {
						aggregate.setServiceId(service.getId());
						break;
					}
				}
			}else if(aggregate.getAvailabilityZone().equals(ParamConstant.VMWARE_ZONE)){
				for (CloudService service : services) {
					if (service.getType().equals(ParamConstant.VMWARE_TYPE)) {
						aggregate.setServiceId(service.getId());
						break;
					}
				}
			}
		}
	}
	
	@Override
	public HostAggregate getHostAggregate(String id,TokenOs ostoken) throws BusinessException{
		
		HostAggregate aggregate = hostAggregateMapper.selectByPrimaryKey(id);
		if(null != aggregate){
			setHostInfo(aggregate);
			return aggregate;
		}
		Locale locale = new Locale(ostoken.getLocale());  
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		TokenOs adminToken = null;
		try{
			adminToken = authService.createDefaultAdminOsToken();
		}catch(Exception e){
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		  
		String region = ostoken.getCurrentRegion();
		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/os-aggregates/");
		sb.append(id);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
     
		Map<String, String> rs = httpClient.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs, locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				aggregate = getHostAggregate(rs);
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
				aggregate = getHostAggregate(rs);
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
			throw new ResourceBusinessException(Message.CS_HOST_AGGREGATE_DETAIL_GET_FAILED,httpCode,locale);
		}
		
		setHostInfo(aggregate);
		try{
			hostAggregateMapper.insertOrUpdate(aggregate);
		}catch(Exception e){
			log.error(e);
		}
		return aggregate;
	}
	

	
	@Override
	public HostAggregate createHostAggregate(String body,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());  
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode  = null;
		try {
			 rootNode = mapper.readTree(body);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		
		checkName(StringHelper.string2Ncr(rootNode.path(ResponseConstant.NAME).textValue()),locale);
		checkZoneName(rootNode.path(ResponseConstant.AVAILABILITY_ZONE).textValue(),locale);
        String createBody = makeRequestBody(rootNode,locale);
		String serviceId  = rootNode.get(ParamConstant.TYPE).textValue();
		
		TokenOs adminToken = null;
		try{
			adminToken = authService.createDefaultAdminOsToken();
		}catch(Exception e){
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		  
		String region = ostoken.getCurrentRegion();
		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/os-aggregates", null);
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
		Map<String, String> rs = httpClient.httpDoPost(url,adminToken.getTokenid() , createBody);
		
		Util.checkResponseBody(rs, locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		HostAggregate aggregate = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				aggregate = getHostAggregate(rs);
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
			rs = httpClient.httpDoPost(url,tokenid,createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				aggregate = getHostAggregate(rs);
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
			throw new ResourceBusinessException(Message.CS_HOST_AGGREGATE_CREATE_FAILED,httpCode,locale);
		}
		setHostAggregateMetadate(aggregate.getId(),aggregate.getAvailabilityZone(),ostoken);
		aggregate.setMillionSeconds(Util.getCurrentMillionsecond());
		aggregate.setDescription(rootNode.path(ResponseConstant.DESCRIPTION).textValue());
		aggregate.setServiceId(serviceId);
		hostAggregateMapper.insertSelective(aggregate);
		ratingService.addRatingPolicy(ParamConstant.COMPUTE,aggregate.getAvailabilityZone(),ostoken); //add the rating template field
		return aggregate;
	}
	
	
	private void checkName(String name,Locale locale)  throws BusinessException{
		List<HostAggregate> aggregates = hostAggregateMapper.selectAll();
		if(null == aggregates)
			return;
		for(HostAggregate aggregate : aggregates){
			if(aggregate.getName().equals(name))
				throw new ResourceBusinessException(Message.CS_HAVE_SAME_NAME_EXIST,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
	}
	
	private void checkZoneName(String name,Locale locale)  throws BusinessException{
		List<HostAggregate> aggregates = hostAggregateMapper.selectAll();
		if(null == aggregates)
			return;
		for(HostAggregate aggregate : aggregates){
			if(aggregate.getAvailabilityZone().equals(name))
				throw new ResourceBusinessException(Message.CS_HAVE_SAME_NAME_EXIST,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
	}
	
	private void checkHostAggregate(String id,Locale locale)  throws BusinessException{
		HostAggregate aggregate = hostAggregateMapper.selectByPrimaryKey(id);
		if(null == aggregate)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
	    if(!Util.isNullOrEmptyValue(aggregate.getHostIds()))
			throw new ResourceBusinessException(Message.CS_HAVE_HOST_IN_AGGREGATE,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
	    checkAggregate(aggregate,locale);
	    return;
	}

	@Override
	public HostAggregate updateHostAggregate(String id,String body,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale()); 
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode  = null;
		try {
			 rootNode = mapper.readTree(body);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		String name = null;
		String description = null;
		if(!rootNode.path(ResponseConstant.NAME).isMissingNode()){
			name = StringHelper.string2Ncr(rootNode.path(ResponseConstant.NAME).textValue());
			checkName(name,locale);
		}
		if(!rootNode.path(ResponseConstant.DESCRIPTION).isMissingNode()){
			description = rootNode.path(ResponseConstant.DESCRIPTION).textValue();
		}
		HostAggregate aggregate = hostAggregateMapper.selectByPrimaryKey(id);
		if(null == aggregate)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);

		if(null != name){
			TokenOs adminToken = null;
			try{
				adminToken = authService.createDefaultAdminOsToken();
			}catch(Exception e){
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append("{\"aggregate\":{");
			sb.append("\"name\":");
			sb.append("\"");
			sb.append(name);
			sb.append("\"}}");
			
			String region = ostoken.getCurrentRegion();
			String url = adminToken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
			StringBuilder urlSb = new StringBuilder();
			urlSb.append(url);
			urlSb.append("/os-aggregates/");
			urlSb.append(id);
			
			Map<String, String> rs = httpClient.httpDoPut(urlSb.toString(), ostoken.getTokenid(),sb.toString());
			Util.checkResponseBody(rs, locale);
			String failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);

			int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			switch (httpCode) {
			case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
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
			
				rs = httpClient.httpDoPut(urlSb.toString(),tokenid,sb.toString());
				httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
				failedMessage = Util.getFailedReason(rs);
				if (!Util.isNullOrEmptyValue(failedMessage))
					log.error(failedMessage);
				if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
					throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
				break;
			}
			}
			if(null != description)
				aggregate.setDescription(description);
			if(null != name)
				aggregate.setName(name);
		}else{
			if(null != description)
				aggregate.setDescription(description);
		}
		hostAggregateMapper.insertOrUpdate(aggregate);
		return aggregate;
	}
	
	@Override
	public void deleteHostAggregate(String id,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());  
		checkHostAggregate(id,locale);
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		TokenOs adminToken = null;
		try{
			adminToken = authService.createDefaultAdminOsToken();
		}catch(Exception e){
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		  
		String region = ostoken.getCurrentRegion();
		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/os-aggregates/");
		sb.append(id);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
     
		Map<String, String> rs = httpClient.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs, locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
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
			rs = httpClient.httpDoDelete(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
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
			throw new ResourceBusinessException(Message.CS_HOST_AGGREGATE_DELETE_FAILED,httpCode,locale);
		}
		
		HostAggregate aggregate = hostAggregateMapper.selectByPrimaryKey(id);
		if(null != aggregate){
			ratingService.deleteRatingPolicy(ParamConstant.COMPUTE, aggregate.getAvailabilityZone(), ostoken);//delete the rating field
			hostAggregateMapper.deleteByPrimaryKey(id);
		}
	}
	
	private void checkHost(List<Host> hosts,TokenOs ostoken) throws BusinessException{
	  if(null == hosts)
		  return;
	  for(Host host : hosts){
		 if(!Util.isNullOrEmptyValue(host.getZoneName()))
				throw new ResourceBusinessException(Message.CS_HOST_HAS_EXIST_ZONE,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
	  }
	}
	
	@Override
	public void addHostToAggregate(String id,String body,TokenOs ostoken) throws BusinessException{
		
		Locale locale = new Locale(ostoken.getLocale());
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode  = null;
		try {
			 rootNode = mapper.readTree(body);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		HostAggregate hostAggregate = hostAggregateMapper.selectByPrimaryKey(id);
		checkAggregate(hostAggregate,locale);
		
		String ids = rootNode.path(ResponseConstant.IDS).textValue();
		if(Util.isNullOrEmptyValue(ids) || null == hostAggregate)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
        String[] hostIds = ids.split(",");
        List<Host> hosts = hostMapper.selectByIds(hostIds);
        if(Util.isNullOrEmptyList(hosts))
        	throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
        checkHost(hosts,ostoken);
        for(Host host : hosts){
        	StringBuilder sb = new StringBuilder();
    		sb.append("{\"add_host\":{");
    		sb.append("\"host\":");
    		sb.append(host.getHostName());
    		sb.append("}}");
    		operateHostAggregate(id,sb.toString(),ostoken);
        }
        
        hostAggregate.setHostIds(Util.getAppendedIds(hostAggregate.getHostIds(), Util.stringToList(ids,",")));
        hostAggregateMapper.updateByPrimaryKeySelective(hostAggregate);
        updateHostDetailInfo(hosts,hostAggregate.getAvailabilityZone(),true);
	}
	
	@Override
	public void removeHostFromAggregate(String id,String body,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode  = null;
		try {
			 rootNode = mapper.readTree(body);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		HostAggregate hostAggregate = hostAggregateMapper.selectByPrimaryKey(id);
		checkAggregate(hostAggregate,locale);
		String ids = rootNode.path(ResponseConstant.IDS).textValue();
		if(Util.isNullOrEmptyValue(ids) || null == hostAggregate)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
        String[] hostIds = ids.split(",");
        List<Host> hosts = hostMapper.selectByIds(hostIds);
        if(Util.isNullOrEmptyList(hosts))
        	throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);

        for(Host host : hosts){
        	StringBuilder sb = new StringBuilder();
    		sb.append("{\"remove_host\":{");
    		sb.append("\"host\":");
    		sb.append(host.getHostName());
    		sb.append("}}");
    		operateHostAggregate(id,sb.toString(),ostoken);
            hostAggregate.setHostIds(Util.listToString(Util.getCorrectedIdInfo(hostAggregate.getHostIds(), host.getId()), ','));
        }
        hostAggregateMapper.updateByPrimaryKeySelective(hostAggregate);
        updateHostDetailInfo(hosts,hostAggregate.getAvailabilityZone(),false);
	}
	
	private void updateHostDetailInfo(List<Host> hosts, String zoneName, Boolean add) {
		if (true == add) {
			for (Host host : hosts) {
				host.setZoneName(zoneName);
				if (Util.isNullOrEmptyValue(host.getHostDetailsId()))
					continue;
				List<HostDetail> hostDetails = hostDetailMapper.getHostDetailsById(host.getHostDetailsId().split(","));
				for (HostDetail hostDetail : hostDetails) {
					if (null == hostDetail.getName())
						continue;
					if (hostDetail.getName().equals(ParamConstant.CORE))
						hostDetail.setType(zoneName+"_core");
					else if (hostDetail.getName().equals(ParamConstant.RAM))
						hostDetail.setType(zoneName+"_ram");
				}
				hostDetailMapper.insertOrUpdateBatch(hostDetails);
			}
			hostMapper.insertOrUpdateBatch(hosts);
		} else {
			for (Host host : hosts) {
				host.setZoneName(null);
				if (Util.isNullOrEmptyValue(host.getHostDetailsId()))
					continue;
				List<HostDetail> hostDetails = hostDetailMapper.getHostDetailsById(host.getHostDetailsId().split(","));
				for (HostDetail hostDetail : hostDetails) {
					if (null == hostDetail.getName())
						continue;
					if (hostDetail.getName().equals(ParamConstant.CORE))
						hostDetail.setType(null);
					else if (hostDetail.getName().equals(ParamConstant.RAM))
						hostDetail.setType(null);
				}
				hostDetailMapper.insertOrUpdateBatch(hostDetails);
			}
			hostMapper.insertOrUpdateBatch(hosts);
		}
	}
	
	@Override
	public void setHostAggregateMetadate(String id,String name,TokenOs ostoken) throws BusinessException{
        StringBuilder sb = new StringBuilder();
    	sb.append("{\"set_metadata\":{");
    	sb.append("\"metadata\":");
    	sb.append("{\"");
    	sb.append(ParamConstant.AVAILABILITY_ZONE);
    	sb.append("\":\"");
    	sb.append(name);
    	sb.append("\"}}}");
    	
    	operateHostAggregate(id,sb.toString(),ostoken); 
	}
	
	private void operateHostAggregate(String id,String body,TokenOs ostoken) throws BusinessException{
	
		Locale locale = new Locale(ostoken.getLocale());
		TokenOs adminToken = null;
		try{
			adminToken = authService.createDefaultAdminOsToken();
		}catch(Exception e){
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		  
		String region = ostoken.getCurrentRegion();
		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/os-aggregates/");
		sb.append(id);
		sb.append("/action");
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
		Map<String, String> rs = httpClient.httpDoPost(sb.toString(),adminToken.getTokenid(),body);
		
		Util.checkResponseBody(rs, locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
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
			rs = httpClient.httpDoPost(sb.toString(),tokenid,body);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
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
			throw new ResourceBusinessException(Message.CS_HOST_AGGREGATE_CREATE_FAILED,httpCode,locale);
		}
		return;
	}
	
	private String makeRequestBody(JsonNode rootNode,Locale locale) throws ResourceBusinessException{
		StringBuilder sb = new StringBuilder();
		sb.append("{\"aggregate\":{");
		sb.append("\"name\":\"");
		sb.append(StringHelper.string2Ncr(rootNode.path(ResponseConstant.NAME).textValue()));
		sb.append("\",\"");
		sb.append(ParamConstant.AVAILABILITY_ZONE);
		sb.append("\":\"");
		sb.append(rootNode.path(ResponseConstant.AVAILABILITY_ZONE).textValue());
		sb.append("\"}}");
		return sb.toString();
	}
	
//	private void setHostDisplayName(ResourceUsedInfo hostResourceInfo,Locale locale) {
//		if (null == hostResourceInfo){
//			return;
//		}
//		String displayName = "";
//		if (Util.isNullOrEmptyValue(hostResourceInfo.getName())){
//			displayName = Message.getMessage("DEFAULT-COMPUTE", locale,false);
//		}else{
//			try {
//				displayName = Message.getMessage(hostResourceInfo.getName().toUpperCase(), locale, false);
//			} catch (Exception e) {
//				displayName = Message.getMessage("DEFAULT-COMPUTE",locale,false);
//			}	
//		}
//		
//		hostResourceInfo.setHostDisplayName(displayName);
//	}
	
	private List<ResourceUsedInfo> makeEnvComputeResourceFromDB(List<Host> hosts){
		List<ResourceUsedInfo> resources = new ArrayList<ResourceUsedInfo>();
		for(Host host : hosts){
			if(Util.isNullOrEmptyValue(host.getHostDetailsId()))
				continue;
			List<HostDetail> details = hostDetailMapper.getHostDetailsById(host.getHostDetailsId().split(","));
			if(Util.isNullOrEmptyList(details))
				continue;
			ResourceUsedInfo hostResourceInfo = new ResourceUsedInfo();
			hostResourceInfo.setName(host.getHostName());
			hostResourceInfo.setSource(host.getSource());
			for(HostDetail detail : details){
		       if(ParamConstant.CORE.equals(detail.getName())){
		    	   hostResourceInfo.setCpu(detail);
		       }else if(ParamConstant.RAM.equals(detail.getName())){
		    	   hostResourceInfo.setMem(detail);
		       }else{
		    	   hostResourceInfo.setDisk(detail);
		       }
			}
			resources.add(hostResourceInfo);
		}
		return resources;
	}
	
//	private List<ResourceUsedInfo> makeEnvComputeResourceFromOS(List<Host> hosts,TokenOs ostoken) throws BusinessException{
//		List<ResourceUsedInfo> resources = new ArrayList<ResourceUsedInfo>();
//		TokenOs adminToken = null;
//		try{
//			adminToken = authService.createDefaultAdminOsToken();	
//			adminToken.setLocale(ostoken.getLocale());
//		}catch(Exception e){
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
//		}
//		for (Host host : hosts) {
//			ResourceUsedInfo hostResourceInfo = null;
//			try {
//				String region = ostoken.getCurrentRegion();
//				String url = adminToken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
//				StringBuilder sb = new StringBuilder();
//				sb.append(url);
//				sb.append("/os-hosts/");
//				sb.append(host.getHostName());
//
//				HashMap<String, String> headers = new HashMap<String, String>();
//				headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, adminToken.getTokenid());
//				Locale locale = new Locale(ostoken.getLocale());
//				Map<String, String> rs = httpClient.httpDoGet(sb.toString(), headers);
//				Util.checkResponseBody(rs,locale);
//				String failedMessage = Util.getFailedReason(rs);
//				if (!Util.isNullOrEmptyValue(failedMessage))
//					log.error(failedMessage);
//				int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//				switch (httpCode) {
//				case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
//					try {
//						hostResourceInfo = getHostResourceUsedDetail(rs, host.getHostName());
//					} catch (Exception e) {
//						// TODO Auto-generated catch block
//						log.error(e);
//						throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//					}
//					break;
//				}		
//				case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
//					String tokenid = "";// TODO reget the token id
//					try {
//						TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
//						tokenid = newToken.getTokenid();
//					} catch (Exception e) {
//						// TODO Auto-generated catch block
//						throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//					}
//					headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
//					rs = httpClient.httpDoGet(sb.toString(), headers);
//					httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//					failedMessage = Util.getFailedReason(rs);
//					if (!Util.isNullOrEmptyValue(failedMessage))
//						log.error(failedMessage);
//					if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
//						throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//					try {
//						hostResourceInfo = getHostResourceUsedDetail(rs, host.getHostName());
//					} catch (Exception e) {
//						// TODO Auto-generated catch block
//						log.error(e);
//						throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//					}
//					break;
//				}
//				default:
//					throw new ResourceBusinessException(Message.CS_HOST_DETAIL_GET_FAILED,httpCode,locale);
//				}
//			} catch (Exception e) {
//				// TODO
//			}
//			if (null != hostResourceInfo){
//				resources.add(hostResourceInfo);
//			}	
//		}
//		return resources;
//	}
	
	@Override
	public EnvResource getTotalResource(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {

		authService.checkIsAdmin(ostoken);
		EnvResource envResource = new EnvResource();
		envResource.setId(Util.makeUUID());
		
		
		List<Host> hosts = hostMapper.selectByServiceName(ParamConstant.COMPUTE);
		if (Util.isNullOrEmptyList(hosts)){
			return null;
		}
		List<ResourceUsedInfo> resources = makeEnvComputeResourceFromDB(hosts);
		envResource.setPhysicalServers(resources);

		List<ResourceSpec> backendStorages = getStorageInfoFromDB(ostoken);
		if(Util.isNullOrEmptyList(backendStorages)){
			TokenOs adminToken = null;
			try{
				adminToken = authService.createDefaultAdminOsToken();	
				adminToken.setLocale(ostoken.getLocale());
				Host hostStorage = getStorageInfo(adminToken);
				backendStorages  = new ArrayList<ResourceSpec>();
		        if(null != hostStorage){
		        	List<HostDetail> storages = hostStorage.getHostDetails();
		        	if(!Util.isNullOrEmptyList(storages)){
		        	//	Locale locale = new Locale(ostoken.getLocale());
		        		List<VolumeType> volumeTypes = volumeTypeMapper.selectAll();
		        		for(HostDetail detail : storages){
		        			ResourceSpec resource = new ResourceSpec();
		            		resource.setType(detail.getType());
		        			String typeName = getVolumeTypeVisibleName(volumeTypes,detail.getType());
		        			if(Util.isNullOrEmptyValue(typeName))
		        				typeName = detail.getType();//Message.getMessage(detail.getType().toUpperCase(), locale,false);
		            		//resource.setTypeName(detail.getTypeName());
		            		resource.setTypeName(typeName);
		            		resource.setName(ParamConstant.STORAGE);
		            		resource.setTotal(detail.getTotal());
		            		resource.setFree(detail.getFree());
		            		resource.setUsed(detail.getTotal() - detail.getFree());
		            		backendStorages.add(resource);
		        		}
		        	}
		        }
			}catch(Exception e){
				log.error(e);
			}
		}
		
        envResource.setStorages(backendStorages);
		
		List<ResourceSpec> floatingIps = new ArrayList<ResourceSpec>();
		String[] floatingipSpec = cloudconfig.getSystemFloatingSpec().split(",");
	//	String floatingipType = "CS_FLOTINGIP_TYPE_NAME";
		for(int index = 0; index < floatingipSpec.length; ++index){
			ResourceSpec resource = resourceMapper.selectByName(floatingipSpec[index]);
			if(null == resource)
				continue;
		//	String floatingipTypeName = floatingipType.replaceFirst("TYPE", resource.getName().toUpperCase());
			resource.setType(floatingipSpec[index]);
		//	resource.setTypeName(Message.getMessage(floatingipTypeName,new Locale(ostoken.getLocale()),false));
			resource.setName(ParamConstant.FLOATINGIP);
			floatingIps.add(resource);
		}
		envResource.setFloatingIps(floatingIps);
		envResource.setCreatedResources(buildCreatedResourceInfo());
		return envResource;
	}
	
	private List<ResourceSpec> buildCreatedResourceInfo(){
		List<ResourceSpec> createdResource = new ArrayList<ResourceSpec>();
		ResourceSpec instanceResource = new ResourceSpec();
		instanceResource.setType(ParamConstant.INSTANCE);
		Integer count = instanceMapper.countNum();
		if(null == count)
			count = 0;
		instanceResource.setTotal(count);
		createdResource.add(instanceResource);
		
		ResourceSpec volumeResource = new ResourceSpec();
		volumeResource.setType(ParamConstant.VOLUME);
		count = volumeMapper.countNum();
		if(null == count)
			count = 0;
		volumeResource.setTotal(count);
		createdResource.add(volumeResource);
		
		ResourceSpec volumeBackupResource = new ResourceSpec();
		volumeBackupResource.setType(ParamConstant.BACKUP);
		count = backupMapper.countNum();
		if(null == count)
			count = 0;
		volumeBackupResource.setTotal(count);
		createdResource.add(volumeBackupResource);
		
		ResourceSpec imageResource = new ResourceSpec();
		imageResource.setType(ParamConstant.IMAGE);
		count = imageMapper.countNumByImageFlag(true);
		if(null == count)
			count = 0;
		imageResource.setTotal(count);
		createdResource.add(imageResource);
		
		ResourceSpec networkResource = new ResourceSpec();
		networkResource.setType(ParamConstant.NETWORK);
		count = networkMapper.countNum();
		if(null == count)
			count = 0;
		networkResource.setTotal(count);
		createdResource.add(networkResource);
		
		ResourceSpec subnetResource = new ResourceSpec();
		subnetResource.setType(ParamConstant.SUBNET);
		count = subnetMapper.countNum();
		if(null == count)
			count = 0;
		subnetResource.setTotal(count);
		createdResource.add(subnetResource);
		
		ResourceSpec lbResource = new ResourceSpec();
		lbResource.setType(ParamConstant.LOADBALANCER);
		count = lbMapper.countNum();
		if(null == count)
			count = 0;
		lbResource.setTotal(count);
		createdResource.add(lbResource);
		
		ResourceSpec fwResource = new ResourceSpec();
		fwResource.setType(ParamConstant.FIREWALL);
		count = fwMapper.countNum();
		if(null == count)
			count = 0;
		fwResource.setTotal(count);
		createdResource.add(fwResource);
		
		return createdResource;
	}
	
	@Override
	public Host getHostDetail(String hostName, String zoneName,TokenOs ostoken) throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信息
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		Host host = getHostDetailFromDB(hostName);
		if (null != host)
			return host;

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/os-hosts/");
		sb.append(hostName);

		// url=RequestUrlHelper.createFullUrl(url+"/os-hosts/"+hostName, null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = httpClient.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				host = getHostDetail(rs, hostName,zoneName,locale);
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
			if (httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_HOST_DETAIL_GET_FAILED,httpCode,locale);
			try {
				host = getHostDetail(rs, hostName,zoneName,locale);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		/*
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_HOST_DETAIL_GET_FAILED,httpCode,locale);
			*/
		}
		// storeHost2DB(host);
		return host;
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
	
	private List<ResourceSpec> getStorageInfoFromDB(TokenOs ostoken) {
		List<VolumeType> volumeTypes = volumeTypeMapper.selectAll();
		if(Util.isNullOrEmptyList(volumeTypes))
			return null;
		List<String> volumeNames = new ArrayList<String>();
		for(VolumeType volumeType:volumeTypes){
			volumeNames.add(volumeType.getName());
		}
		List<ResourceSpec> resources = resourceMapper.findResourcesByNames(volumeNames);
		if(Util.isNullOrEmptyList(resources))
			return null;
		//Locale locale = new Locale(ostoken.getLocale());
		for(ResourceSpec resource : resources){
			resource.setType(resource.getName());
			String typeName = getVolumeTypeVisibleName(volumeTypes,resource.getName());
			if(Util.isNullOrEmptyValue(typeName))
				typeName = resource.getName();//Message.getMessage(resource.getName().toUpperCase(), locale,false);
			resource.setTypeName(typeName);
			resource.setName(ParamConstant.STORAGE);
		}
		return resources;
	}
	
	private Host getStorageInfo(TokenOs ostoken) throws BusinessException {

		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/scheduler-stats/get_pools?detail=true", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		// get storage quota
		Map<String, String> rs = httpClient.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);

		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		List<HostDetail> hostDetails = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				hostDetails = getStorageQuotas(rs,locale);
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
			if (httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_HOST_DETAIL_GET_FAILED,httpCode,locale);
			try {
				hostDetails = getStorageQuotas(rs,locale);
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
			throw new ResourceBusinessException(Message.CS_STORAGE_QUOTA_GET_FAILED,httpCode,locale);
		}

		Host hostStorage = new Host();
		hostStorage.setId(Util.makeUUID());
		hostStorage.setServiceName(ParamConstant.STORAGE);
		hostStorage.setHostDetails(hostDetails);
		hostStorage.makeHostDetailsId();
		return hostStorage;
	}

	private Host makeFloatigIPInfoHost(Locale locale) throws BusinessException {
		String[] floatingSpecs = cloudconfig.getSystemFloatingSpec().split(",");
		String[] floatingNums = cloudconfig.getSystemFloatingNum().split(",");

		if (floatingSpecs.length != floatingNums.length)
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,locale);
		Host hostFloatingIP = new Host();
		hostFloatingIP.setId(Util.makeUUID());
		hostFloatingIP.setServiceName(ParamConstant.FLOATINGIP);
		List<HostDetail> hostDetails = new ArrayList<HostDetail>();
		for (int index = 0; index < floatingSpecs.length; ++index) {
			HostDetail floatingIp = new HostDetail();
			floatingIp.setId(Util.makeUUID());
			floatingIp.setTotal(Integer.parseInt(floatingNums[index]));
		//	String floatingipTypeName = "CS_FLOTINGIP_TYPE_NAME";
		//	floatingipTypeName = floatingipTypeName.replaceFirst("TYPE", floatingSpecs[index].toUpperCase());
			floatingIp.setType(floatingSpecs[index]);
		//	floatingIp.setTypeName(Message.getMessage(floatingipTypeName,locale, false));
			floatingIp.setName(ParamConstant.FLOATINGIP);
		//	floatingIp.setUnit(Message.getMessage(Message.CS_COUNT_UNIT,locale, false));
			floatingIp.setUnit(Message.CS_COUNT_UNIT);
			hostDetails.add(floatingIp);
		}
		hostFloatingIP.setHostDetails(hostDetails);
		hostFloatingIP.makeHostDetailsId();
		return hostFloatingIP;
	}

	private List<Host> makeHostDetails(List<Host> hosts, TokenOs ostoken)
			throws BusinessException {
		if (Util.isNullOrEmptyList(hosts))
			return null;
		List<Host> hostWithDetails = new ArrayList<Host>();
		for (Host host : hosts) {
//			if (!host.getServiceName().equals(ParamConstant.COMPUTE) || host.getHostName().contains(ParamConstant.IRONIC)) {
//				continue;
//			}
			Host hostDetail = getHostDetail(host.getHostName(), host.getZoneName(),ostoken);
			if(null == hostDetail)
				continue;
//			host.setHostDetails(hostDetail.getHostDetails());
//			host.makeHostDetailsId();
			hostWithDetails.add(hostDetail);
		}
		return hostWithDetails;
	}
	
	private void setEnvResourceQuota(List<Host> hosts) {
//		int totalCore = 0;
//		int totalRam = 0;
		List<ResourceSpec> resources = resourceMapper.selectByType(ParamConstant.CORE);
		if(!Util.isNullOrEmptyList(resources))
			return;
		Map<String, Integer> storageQuota = new HashMap<String, Integer>();
		Map<String, Integer> floatingQuota = new HashMap<String, Integer>();
		Map<String, Integer> cpuQuota = new HashMap<String, Integer>();
		Map<String, Integer> ramQuota = new HashMap<String, Integer>();
		for (Host host : hosts) {
			if(Util.isNullOrEmptyList(host.getHostDetails()))
					continue;
			if (ParamConstant.STORAGE.equals(host.getServiceName())) {
				for (HostDetail hostDetail : host.getHostDetails()) {
					if (storageQuota.containsKey(hostDetail.getType())) {
						storageQuota.put(hostDetail.getType(),
								hostDetail.getTotal() + storageQuota.get(hostDetail.getType()));
					} else {
						storageQuota.put(hostDetail.getType(), hostDetail.getTotal());
					}
				}
			} else if (ParamConstant.COMPUTE.equals(host.getServiceName())) {
				for (HostDetail hostDetail : host.getHostDetails()) {
					if (hostDetail.getType().contains("core")) {
						if (cpuQuota.containsKey(hostDetail.getType())) {
							cpuQuota.put(hostDetail.getType(),
									hostDetail.getTotal() + cpuQuota.get(hostDetail.getType()));
						} else {
							cpuQuota.put(hostDetail.getType(), hostDetail.getTotal());
						}
					} else {
						if (ramQuota.containsKey(hostDetail.getType())) {
							ramQuota.put(hostDetail.getType(),
									hostDetail.getTotal() + ramQuota.get(hostDetail.getType()));
						} else {
							ramQuota.put(hostDetail.getType(), hostDetail.getTotal());
						}
					}
				}
			} else {
				for (HostDetail hostDetail : host.getHostDetails()) {
					if (floatingQuota.containsKey(hostDetail.getType())) {
						floatingQuota.put(hostDetail.getType(),
								hostDetail.getTotal() + floatingQuota.get(hostDetail.getType()));
					} else {
						floatingQuota.put(hostDetail.getType(), hostDetail.getTotal());
					}
				}
			}
		}
		storeResourceToDB(cpuQuota, ramQuota, storageQuota, floatingQuota);
	}
	
	private void storeResourceToDB(Map<String,Integer> cpuQuota,Map<String,Integer> ramQuota,Map<String,Integer> storageQuota,Map<String,Integer> floatingQuota){
		List<ResourceSpec> resources = new ArrayList<ResourceSpec>();
//		ResourceSpec cpuResource = new ResourceSpec();
//		cpuResource.setId(Util.makeUUID());
//		cpuResource.setName(ParamConstant.CORE);
//		cpuResource.setType(ParamConstant.CORE);
//		cpuResource.setTotal(totalCore);
//		cpuResource.setUsed(0);
//		resources.add(cpuResource);
//		
//		ResourceSpec ramResource = new ResourceSpec();
//		ramResource.setId(Util.makeUUID());
//		ramResource.setName(ParamConstant.RAM);
//		ramResource.setType(ParamConstant.RAM);
//		ramResource.setTotal(totalRam);
//		ramResource.setUsed(0);
//		resources.add(ramResource);
		
		for (Map.Entry<String, Integer> entry : cpuQuota.entrySet()){
			ResourceSpec cpuResource = new ResourceSpec();
			cpuResource.setId(Util.makeUUID());
			cpuResource.setName(entry.getKey());
			cpuResource.setType(ParamConstant.CORE);
			cpuResource.setTotal(entry.getValue());
			cpuResource.setUsed(0);
			resources.add(cpuResource);
		}
		
		for (Map.Entry<String, Integer> entry : ramQuota.entrySet()){
			ResourceSpec ramResource = new ResourceSpec();
			ramResource.setId(Util.makeUUID());
			ramResource.setName(entry.getKey());
			ramResource.setType(ParamConstant.RAM);
			ramResource.setTotal(entry.getValue());
			ramResource.setUsed(0);
			resources.add(ramResource);
		}
		
		for (Map.Entry<String, Integer> entry : storageQuota.entrySet()){
			ResourceSpec diskResource = new ResourceSpec();
			diskResource.setId(Util.makeUUID());
			diskResource.setName(entry.getKey());
			diskResource.setType(ParamConstant.STORAGE);
			diskResource.setTotal(entry.getValue());
			diskResource.setUsed(0);
			resources.add(diskResource);
		}
		
		for (Map.Entry<String, Integer> entry : floatingQuota.entrySet()){
			ResourceSpec floatingResource = new ResourceSpec();
			floatingResource.setId(Util.makeUUID());
			floatingResource.setName(entry.getKey());
			floatingResource.setType(ParamConstant.FLOATINGIP);
			floatingResource.setTotal(entry.getValue());
			floatingResource.setUsed(0);
			resources.add(floatingResource);
		}
		
		resourceMapper.insertOrUpdateBatch(resources);
		//resourceMapper.addResourcesBatch(resources);	
	}
	
	private List<HostDetail> getStorageQuotas(Map<String, String> rs,Locale locale) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode poolsNode = rootNode.path(ResponseConstant.POOLS);
		int poolsCount = poolsNode.size();
		if (0 == poolsCount)
			return null;
		List<HostDetail> hostDetails = new ArrayList<HostDetail>();
		for (int index = 0; index < poolsCount; ++index) {
			HostDetail hostDetail = getStorageDetailInfo(poolsNode.get(index),locale);
			if (null == hostDetail)
				continue;
			hostDetails.add(hostDetail);
		}
		return hostDetails;
	}

	private HostDetail getStorageDetailInfo(JsonNode poolCapacityNode,Locale locale) {
		if (null == poolCapacityNode)
			return null;
		JsonNode capabilityNode = poolCapacityNode.path(ResponseConstant.CAPABILITIES);
		if (null == capabilityNode)
			return null;
		HostDetail storageDetail = new HostDetail();
		storageDetail.setId(Util.makeUUID());
		storageDetail.setTotal(capabilityNode.path(ResponseConstant.TOTAL_CAPACITY).intValue());
		storageDetail.setFree(capabilityNode.path(ResponseConstant.FREE_CAPACITY).intValue());
		storageDetail.setName(ParamConstant.STORAGE);
		VolumeType type = volumeTypeMapper.selectByBackendName(capabilityNode.path(ResponseConstant.VOLUME_BACKEND_NAME).textValue());
		if(null == type)
			return null; //maybe set the default volume type 09/26
		//String volumeTypeName = "CS_VOLUME_TYPE_NAME";
		//volumeTypeName = volumeTypeName.replaceFirst("TYPE",type.getName().toUpperCase());
		storageDetail.setType(type.getName());
		//storageDetail.setTypeName(Message.getMessage(volumeTypeName,locale,false));
		//storageDetail.setUnit(Message.getMessage(Message.CS_CAPACITY_UNIT,locale,false));
		storageDetail.setUnit(Message.CS_CAPACITY_UNIT);

		return storageDetail;
	}

	private List<Host> getHostsFromDB() {
		List<Host> hosts = hostMapper.selectAll();
		if (Util.isNullOrEmptyList(hosts))
			return null;
		List<Host> hostsWithHostDetail = new ArrayList<Host>();
		for (Host host : hosts) {
			Host hostDetail = getHostDetailFromDB(host.getId());
			hostsWithHostDetail.add(hostDetail);
		}
		return hostsWithHostDetail;
	}

	private void storeHost2DB(Host host) {
		if (null == host)
			return;
		if (null != hostMapper.selectByHostName(host.getHostName()))
			hostMapper.updateByPrimaryKeySelective(host);
		else
			hostMapper.insertSelective(host);
	}

	private void storeHostDetail2DB(List<HostDetail> hostDetails) {
		if (Util.isNullOrEmptyList(hostDetails))
			return;
		for (HostDetail hostDetail : hostDetails) {
			if (null != hostDetailMapper.selectByPrimaryKey(hostDetail.getId()))
				hostDetailMapper.updateByPrimaryKeySelective(hostDetail);
			else
				hostDetailMapper.insertSelective(hostDetail);
		}
	}

	private Host getHostDetailFromDB(String hostId) {
		//Host host = hostMapper.selectByHostName(hostName);
		Host host = hostMapper.selectByPrimaryKey(hostId);
		if (null == host)
			return null;
		String[] hostDetailsId = host.getHostDetailsId().split(",");
		if (null == hostDetailsId || 0 == hostDetailsId.length)
			return null;
		List<HostDetail> hostDetails = hostDetailMapper.getHostDetailsById(hostDetailsId);
		host.setHostDetails(hostDetails);
		return host;
	}

	private List<Host> storeHostsToDB(List<Host> hosts) {
		if (Util.isNullOrEmptyList(hosts))
			return null;
		for (Host host : hosts) {
			storeHost2DB(host);
			storeHostDetail2DB(host.getHostDetails());
		}
		return null;
	}

	private boolean isIngoreHost(String hostName){
	   String ingoreHostNames = cloudconfig.getSystemIngoreHosts();
	   if(Util.isNullOrEmptyValue(ingoreHostNames))
		   return false;
	   String[] hostNames = ingoreHostNames.split(",");
	   for(int index = 0; index < hostNames.length; ++index){
		   if(hostNames[index].equals(hostName))
			   return true;
	   }
	   return false;
	}
	
	private Host getHostInfo(JsonNode hostNode) {
		if (null == hostNode)
			return null;
		if(isIngoreHost(hostNode.path(ResponseConstant.HOST_NAME).textValue()))
			return null;
		Host host = null;
		host = hostMapper.selectByHostName(hostNode.path(ResponseConstant.HOST_NAME).textValue());
		if(null != host)
			return host;
		host = new Host();
		host.setId(Util.makeUUID());
		host.setHostName(hostNode.path(ResponseConstant.HOST_NAME).textValue());
		host.setServiceName(hostNode.path(ResponseConstant.SERVICE_NAME).textValue());
		host.setSource(ParamConstant.OPENSTACK_ZONE);
		if(!ParamConstant.COMPUTE.equals(host.getServiceName())) //skip not compute node
			return null;
		host.setZoneName(hostNode.path(ResponseConstant.ZONE_NAME).textValue());
		return host;
	}

	private List<Host> getHosts(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode hostsNode = rootNode.path(ResponseConstant.HOSTS);
		int hostsCount = hostsNode.size();
		if (0 == hostsCount)
			return null;

		List<Host> hosts = new ArrayList<Host>();
		for (int index = 0; index < hostsCount; ++index) {
			Host host = getHostInfo(hostsNode.get(index));
			if (null == host)
				continue;
			hosts.add(host);
		}
		return hosts;
	}

	private Host getHostDetail(Map<String, String> rs, String hostName,String zoneName,Locale locale) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode resourcesNode = rootNode.path(ResponseConstant.HOST);
		int resourcesCount = resourcesNode.size();
		if (0 == resourcesCount)
			return null;
		Host host = new Host();
		host.setId(Util.makeUUID());
		host.setHostName(hostName);
		host.setZoneName(zoneName);
		host.setServiceName(ParamConstant.COMPUTE);
		for (int index = 0; index < resourcesCount; ++index) {
			setHostDetail(host, resourcesNode.get(index),locale);
		}
		return host;
	}

	private void setHostDetail(Host host, JsonNode resourceNode,Locale locale) {
		if (null == resourceNode)
			return;
		JsonNode resDetail = resourceNode.get(ResponseConstant.RESOURCE);
		if (resDetail.path(ResponseConstant.PROJECT).textValue().equals(ResponseConstant.PROJECT_TOTAL)) {
			// cpu info
			List<HostDetail> hostDetails = new ArrayList<HostDetail>();
			HostDetail hostCpuDetail = new HostDetail();
			hostCpuDetail.setId(Util.makeUUID());
			hostCpuDetail.setProject(ResponseConstant.PROJECT_TOTAL);
			hostCpuDetail.setName(ParamConstant.CORE);
	//		hostCpuDetail.setResState(ParamConstant.TOTAL_RES);
			if(!Util.isNullOrEmptyValue(host.getZoneName())){
			//	String coreType = "CS_CORE_TYPE_NAME";
			//	String coreTypeName = coreType.replaceFirst("TYPE", host.getZoneName().toUpperCase());
				hostCpuDetail.setType(host.getZoneName()+"_"+ParamConstant.CORE);
			//	hostCpuDetail.setTypeName(Message.getMessage(coreTypeName, locale,false));
			}else{
				hostCpuDetail.setType(ParamConstant.CORE);
			//	hostCpuDetail.setTypeName(Message.getMessage(Message.CS_CPU_NAME,locale,false));
			}
		
			//hostCpuDetail.setUnit(Message.getMessage(Message.CS_COUNT_UNIT,locale,false));
			hostCpuDetail.setUnit(Message.CS_COUNT_UNIT);
			
			String systemCpuRatio = cloudconfig.getSystemCpuRatio();
			Float totalCores = Float.valueOf(systemCpuRatio) * resDetail.path(ResponseConstant.CPU).intValue();
			hostCpuDetail.setTotal(totalCores.intValue());
			host.setHostDetails(hostDetails);
			hostDetails.add(hostCpuDetail);

			// memory info
			HostDetail hostMemoryDetail = new HostDetail();
			hostMemoryDetail.setId(Util.makeUUID());
			hostMemoryDetail.setProject(ResponseConstant.PROJECT_TOTAL);
			hostMemoryDetail.setName(ParamConstant.RAM);
	//		hostMemoryDetail.setResState(ParamConstant.TOTAL_RES);
			if(!Util.isNullOrEmptyValue(host.getZoneName())){
				hostMemoryDetail.setType(host.getZoneName()+"_"+ParamConstant.RAM);
			//	String ramType = "CS_RAM_TYPE_NAME";
			//	String ramTypeName = ramType.replaceFirst("TYPE", host.getZoneName().toUpperCase());
			//	hostMemoryDetail.setTypeName(Message.getMessage(ramTypeName,locale,false));
			}else{
				hostCpuDetail.setType(ParamConstant.RAM);
			//	hostCpuDetail.setTypeName(Message.getMessage(Message.CS_MEMORY_NAME,locale,false));
			}
			//hostMemoryDetail.setUnit(Message.getMessage(Message.CS_CAPACITY_UNIT,locale,false));
			hostMemoryDetail.setUnit(Message.CS_CAPACITY_UNIT);

			String systemRamRatio = cloudconfig.getSystemRamRatio();
			Float totalRams = Float.valueOf(systemRamRatio) * resDetail.path(ResponseConstant.MEMORY).intValue();
			hostMemoryDetail.setTotal(totalRams.intValue());
			hostDetails.add(hostMemoryDetail);

			//disk info
			HostDetail hostDiskDetail = new HostDetail();
			hostDiskDetail.setId(Util.makeUUID());
			hostDiskDetail.setProject(ResponseConstant.PROJECT_TOTAL);
			hostDiskDetail.setType(ParamConstant.DISK);
		//	hostDiskDetail.setTypeName(Message.getMessage(Message.CS_DISK_NAME,locale,false));
		//	hostDiskDetail.setUnit(Message.getMessage(Message.CS_CAPACITY_UNIT,locale,false));
			hostDiskDetail.setUnit(Message.CS_CAPACITY_UNIT);
			hostDiskDetail.setTotal(resDetail.path(ResponseConstant.DISK_GB).intValue());
			hostDetails.add(hostDiskDetail);
			
			host.setHostDetails(hostDetails);
			host.makeHostDetailsId();
		}
	}

	private List<HostAggregate> getHostAggregates(Map<String, String> rs) {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		} catch (Exception e) {
			log.error(e);
			return null;
		} 
		JsonNode aggregatesNode = rootNode.path(ResponseConstant.AGGREGATES);
		int aggregatesCount = aggregatesNode.size();
		if (0 == aggregatesCount)
			return null;

		List<HostAggregate> aggregates = new ArrayList<HostAggregate>();
		for (int index = 0; index < aggregatesCount; ++index) {
			HostAggregate aggregate = getHostAggregateInfo(aggregatesNode.get(index));
			if (null == aggregate)
				continue;
			aggregates.add(aggregate);
		}
		return aggregates;
	}
	
	private HostAggregate getHostAggregate(Map<String, String> rs) {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		} catch (Exception e) {
			log.error(e);
			return null;
		} 
		JsonNode aggregateNode = rootNode.path(ResponseConstant.AGGREGATE);
		return getHostAggregateInfo(aggregateNode);
	}
	
	private HostAggregate getHostAggregateInfo(JsonNode aggregateNode){
		HostAggregate aggregate = new HostAggregate();
		if(aggregateNode.path(ResponseConstant.DELETED).asBoolean() == true)
			return null;
		aggregate.setId(Integer.toString(aggregateNode.path(ResponseConstant.ID).asInt()));
		aggregate.setName(aggregateNode.path(ResponseConstant.NAME).textValue());
		aggregate.setAvailabilityZone(aggregateNode.path(ResponseConstant.AVAILABILITY_ZONE).textValue());
		aggregate.setMillionSeconds(Util.getCurrentMillionsecond());
		CloudService service = null;
		if(aggregate.getAvailabilityZone().equals("baremetal-zone")){
			service = serviceMapper.selectByType(ParamConstant.BAREMETAL_TYPE);	
		}else if(aggregate.getAvailabilityZone().equals("kvm-zone")){
			service = serviceMapper.selectByType(ParamConstant.INSTANCE_TYPE);	
		}else if(aggregate.getAvailabilityZone().equals("vmware-zone")){
			service = serviceMapper.selectByType(ParamConstant.VMWARE_TYPE);	
		}else if(aggregate.getAvailabilityZone().equals("vdi-zone")){
			service = serviceMapper.selectByType(ParamConstant.VDI_TYPE);	
		}else if(aggregate.getAvailabilityZone().equals("database-zone")){
			service = serviceMapper.selectByType(ParamConstant.DATABASE_TYPE);	
		}else if(aggregate.getAvailabilityZone().equals("container-zone")){
			service = serviceMapper.selectByType(ParamConstant.CONTAINER_TYPE);	
		}
		if(null != service)
			aggregate.setServiceId(service.getId());
		aggregate.setSource(ParamConstant.OPENSTACK_ZONE);
		JsonNode hostsNode = aggregateNode.path(ResponseConstant.HOSTS);
		int hostsCount = hostsNode.size();
		if (0 == hostsCount)
			return aggregate;
		List<String> hostNames = new ArrayList<String>();
		for (int index = 0; index < hostsCount; ++index) {
			hostNames.add(hostsNode.get(index).textValue());
		}
		aggregate.setHostNames(hostNames);
		return aggregate;
	}
	
	private void setHostInfo(HostAggregate aggregate) {
		if (null == aggregate)
			return;
		List<Host> hosts = null;
		String hostIds = aggregate.getHostIds();
		if (Util.isNullOrEmptyValue(hostIds)){
			List<String> hostNames = aggregate.getHostNames();
			if(Util.isNullOrEmptyList(hostNames))
				return;
			hosts = hostMapper.selectByHostNames(hostNames);
			if(null == hosts)
				return;
			List<String> ids = new ArrayList<String>();
			for(Host host : hosts){
				ids.add(host.getId());
			}
			aggregate.setHostIds(Util.listToString(ids, ','));	
		}else{
			hosts = hostMapper.selectByIds(hostIds.split(","));
		}
		aggregate.setHosts(hosts);
	}

	private void setHostsInfo(List<HostAggregate> aggregates) {
		if (Util.isNullOrEmptyList(aggregates))
			return;
		for (HostAggregate aggregate : aggregates)
			setHostInfo(aggregate);
	}
	
	private void checkAggregate(HostAggregate aggregate, Locale locale) throws BusinessException {
		if(null == aggregate)
			return;
		if(Util.isNullOrEmptyValue(aggregate.getSource()))
			return;
		if(!aggregate.getSource().equals(ParamConstant.OPENSTACK_ZONE))
			throw new ResourceBusinessException(Message.CS_OPERATE_VMWAREZONE_IS_FORBIDDEN,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
		return;
	}
	
//	private ResourceUsedInfo getHostResourceUsedDetail(Map<String, String> rs, String hostName) throws JsonProcessingException, IOException {
//		ObjectMapper mapper = new ObjectMapper();
//		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
//		JsonNode resourcesNode = rootNode.path(ResponseConstant.HOST);
//		int resourcesCount = resourcesNode.size();
//		if (0 == resourcesCount)
//			return null;
//		ResourceUsedInfo hostResource = new ResourceUsedInfo();
//		hostResource.setName(hostName);
//		for (int index = 0; index < resourcesCount; ++index) {
//			setHostResourceUsedInfo(hostResource, resourcesNode.get(index));
//		}
//		return hostResource;
//	}
	
//	private void setHostResourceUsedInfo(ResourceUsedInfo hostResource, JsonNode resourceNode) {
//		if (null == resourceNode)
//			return;
//		JsonNode resDetail = resourceNode.get(ResponseConstant.RESOURCE);
//		if (resDetail.path(ResponseConstant.PROJECT).textValue().equals(ResponseConstant.PROJECT_TOTAL)) {
//			// cpu info
//			HostDetail hostCpuDetail = hostResource.getCpu();
//			if(null == hostCpuDetail)
//				hostCpuDetail = new HostDetail();
//			String systemCpuRatio = cloudconfig.getSystemCpuRatio();
//			Float totalCores = Float.valueOf(systemCpuRatio) * resDetail.path(ResponseConstant.CPU).intValue();
//			hostCpuDetail.setTotal(totalCores.intValue());
//			hostResource.setCpu(hostCpuDetail);
//            
//			// memory info
//			HostDetail hostMemoryDetail = hostResource.getMem();
//			if(null == hostMemoryDetail)
//				hostMemoryDetail = new HostDetail();
//			String systemRamRatio = cloudconfig.getSystemRamRatio();
//			Float totalRams = Float.valueOf(systemRamRatio) * resDetail.path(ResponseConstant.MEMORY).intValue();
//			hostMemoryDetail.setTotal(totalRams.intValue());
//			hostResource.setMem(hostMemoryDetail);
//
//			//disk info
//			HostDetail hostDiskDetail = hostResource.getDisk();
//			if(null == hostDiskDetail)
//				hostDiskDetail = new HostDetail();
//			hostDiskDetail.setTotal(resDetail.path(ResponseConstant.DISK_GB).intValue());
//			hostResource.setDisk(hostDiskDetail);
//		}
//		else if (resDetail.path(ResponseConstant.PROJECT).textValue().equals(ResponseConstant.PROJECT_USED)) {
//			// cpu info
//	
//			HostDetail hostCpuDetail = hostResource.getCpu();
//			if(null == hostCpuDetail)
//				hostCpuDetail = new HostDetail();
//			hostCpuDetail.setUsed(resDetail.path(ResponseConstant.CPU).intValue());
//			hostResource.setCpu(hostCpuDetail);
//
//			// memory info
//			HostDetail hostMemoryDetail = hostResource.getMem();
//			if(null == hostMemoryDetail)
//				hostMemoryDetail = new HostDetail();
//			hostMemoryDetail.setUsed(resDetail.path(ResponseConstant.MEMORY).intValue());
//			hostResource.setMem(hostMemoryDetail);
//
//			// disk info
//			HostDetail hostDiskDetail = hostResource.getDisk();
//			if(null == hostDiskDetail)
//				hostDiskDetail = new HostDetail();
//			hostDiskDetail.setUsed(resDetail.path(ResponseConstant.DISK_GB).intValue());
//			hostResource.setDisk(hostDiskDetail);
//		}
//	}
	
}
