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

import com.cloud.cloudapi.dao.common.CloudUserMapper;
import com.cloud.cloudapi.dao.common.InstanceMapper;
import com.cloud.cloudapi.dao.common.NetworkMapper;
import com.cloud.cloudapi.dao.common.QuotaDetailMapper;
import com.cloud.cloudapi.dao.common.QuotaMapper;
import com.cloud.cloudapi.dao.common.SubnetMapper;
import com.cloud.cloudapi.dao.common.TenantMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.NetworkJSON;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.InstanceNetworkRel;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Network;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Subnet;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.NetworkService;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.service.openstackapi.SubnetService;
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

@Service("networkService")
public class NetworkServiceImpl implements NetworkService {
	@Resource
	private OSHttpClientUtil client;

	@Autowired
	private NetworkMapper networkMapper;

	@Autowired
	private SubnetMapper subnetMapper;

	@Autowired
	private InstanceMapper instanceMapper;
	
	@Autowired
	private CloudConfig cloudconfig;

	@Autowired
	private QuotaMapper quotaMapper;

	@Autowired
	private QuotaDetailMapper quotaDetailMapper;

	@Autowired
	private TenantMapper tenantMapper;

	@Resource
	private SubnetService subnetService;
	
	@Resource
	private AuthService authService;
	
	@Resource
	private QuotaService quotaService;
	
	@Resource
	private CloudUserMapper cloudUserMapper;
	
	private Logger log = LogManager.getLogger(NetworkServiceImpl.class);
	
	public NetworkMapper getNetworkMapper() {
		return networkMapper;
	}

	public void setNetworkMapper(NetworkMapper networkMapper) {
		this.networkMapper = networkMapper;
	}

	public SubnetMapper getSubnetMapper() {
		return subnetMapper;
	}

	public void setSubnetMapper(SubnetMapper subnetMapper) {
		this.subnetMapper = subnetMapper;
	}

	public CloudConfig getCloudconfig() {
		return cloudconfig;
	}

	public void setCloudconfig(CloudConfig cloudconfig) {
		this.cloudconfig = cloudconfig;
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

	public TenantMapper getTenantMapper() {
		return tenantMapper;
	}

	public void setTenantMapper(TenantMapper tenantMapper) {
		this.tenantMapper = tenantMapper;
	}

	@Override
	public List<Network> getNetworkList(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {

		int limitItems = Util.getLimit(paramMap);

		List<Network> networks = getNetworksFromDB(ostoken.getTenantid(), limitItems);
		if (!Util.isNullOrEmptyList(networks)){
			//normalNetworkCreatedTime(networks);
			return networks;
		}
		// Firstly, get the networks from local db
		// if not, get the network from openstack
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/networks", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
        Locale locale = new Locale(ostoken.getLocale());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoGet(url, ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				networks = getNetworks(rs,ostoken.getTenantid());
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
			rs = client.httpDoGet(url, headers);
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				networks = getNetworks(rs,ostoken.getTenantid());
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
			throw new ResourceBusinessException(Message.CS_NETWORK_GET_FAILED,httpCode,locale);
		}

		networks = storeNetworks2DB(networks, ostoken);
		networks = getLimitItems(networks, ostoken.getTenantid(),limitItems);
		return networks;
		//normalNetworkCreatedTime(networks);
	}

	@Override
	public List<Network> getExternalNetworks(TokenOs ostoken) throws BusinessException {
		List<Network> networks = networkMapper.selectExternalNetworks();
		if(!Util.isNullOrEmptyList(networks)){
			appendSubnetInfo(networks);
			//normalNetworkCreatedTime(networks);
			return networks;
		}
		Locale locale = new Locale(ostoken.getLocale());
		TokenOs adminToken = null;
		try{
			adminToken = authService.createDefaultAdminOsToken();
		}catch(Exception e){
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,locale);
		}
		
		String region = ostoken.getCurrentRegion();

		String url = adminToken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/networks", null);

		Map<String, String> rs = client.httpDoGet(url, adminToken.getTokenid());
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				networks = getNetworks(rs,adminToken.getTenantid());
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
			HashMap<String, String> headers = new HashMap<String, String>();
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoGet(url, headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode !=  ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_NETWORK_GET_FAILED,httpCode,locale);
			try {
				networks = getNetworks(rs,adminToken.getTenantid());
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
			throw new ResourceBusinessException(Message.CS_NETWORK_GET_FAILED,httpCode,locale);
		}

		networks = storeNetworks2DB(networks, adminToken);
		//normalNetworkCreatedTime(networks);
		networks =  getExternalnetworks(networks);
		return networks;
	}
	
	@Override
	public List<Network> getInstanceAttachedNetworks(String instanceId){
		List<InstanceNetworkRel> instanceNetworks = networkMapper.selectListByInstanceId(instanceId);
		if(Util.isNullOrEmptyList(instanceNetworks))
			return null;
		List<String> networksId = new ArrayList<String>();
		for(InstanceNetworkRel instanceNetwork : instanceNetworks){
			networksId.add(instanceNetwork.getNetworkId());
		}
		String[] networkIdArray = networksId.toArray(new String[networksId.size()]);
		List<Network> networks = networkMapper.selectNetworksById(networkIdArray);
		return networks;
	}
	
	@Override
	public Network getNetwork(String networkId, TokenOs ostoken)
			throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		Network network = networkMapper.selectByPrimaryKey(networkId);
		if (null != network) {
			appendSubnetInfo(network);
		//	network.setCreatedAt(Util.millionSecond2Date(network.getMillionSeconds()));
			return network;
		}
		
		if(null == ostoken){
			try{
				ostoken = authService.createDefaultAdminOsToken();
			}catch(Exception e){
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			}
		}
		
		String region = ostoken.getCurrentRegion();
		// String url=ot.getEndPoint(TokenOs.EP_TYPE_NETWORK,
		// region).getPublicURL();
		// url=url+"/v2.0/networks/" + NetworkId;
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/os-networks/");
		sb.append(networkId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoGet(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));

		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				network = getNetwork(rs);
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
			rs = client.httpDoGet(sb.toString(), headers);
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				network = getNetwork(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
//		default:
//			throw new ResourceBusinessException(Message.CS_NETWORK_DETAIL_GET_FAILED,httpCode,locale);
		}
        if(null == network)
        	return null;
		networkMapper.insertOrUpdate(network);
		appendSubnetInfoAndStore2DB(network, ostoken);
		//network.setCreatedAt(Util.millionSecond2Date(network.getMillionSeconds()));
		return network;
		
	}

	@Override
	public Network createNetwork(String createBody, TokenOs ostoken)
			throws BusinessException, JsonProcessingException, IOException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		String networkBody = "";
//		String currentDate = Util.getCurrentDate();
		Network networkCreateInfo = new Network();
		if (Util.isNullOrEmptyValue(createBody)){
			String tenant_id = ostoken.getTenantid();
	//		String tenant_name = ostoken.getTenantname(); 
			CloudUser cloudUser = cloudUserMapper.selectByOsTokenId(ostoken.getTokenid());
			String name = String.format("%s_Network", cloudUser.getAccount());
			networkCreateInfo = new Network();
			networkCreateInfo.setName(name);
			networkCreateInfo.setAdmin_state_up(true);
			networkCreateInfo.setTenant_id(tenant_id);
			networkBody = generateBody(name, tenant_id);
		}else{
			makeNetworkInfoFromCreateBody(createBody, networkCreateInfo);
			networkCreateInfo.setName(StringHelper.string2Ncr(networkCreateInfo.getName()));
			networkBody = generateNetworkBody(networkCreateInfo.getName() , "", networkCreateInfo.getManaged());
		}
		checkName(networkCreateInfo.getName(),ostoken);
		String region = ostoken.getCurrentRegion();
		// token should have Regioninfo
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/networks", null);
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoPost(url, headers, networkBody);
		Util.checkResponseBody(rs,locale);
		
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		Network network = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				network = getNetwork(rs);
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
			rs = client.httpDoPost(url, headers, networkBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				network = getNetwork(rs);
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
			throw new ResourceBusinessException(Message.CS_NETWORK_CREATE_FAILED,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_NETWORK_CREATE_FAILED,httpCode,locale);
		}

	//	network.setCreatedAt(currentDate);
		network.setMillionSeconds(Util.getCurrentMillionsecond());
		storeNetworkAndSubnet2DB(network, networkCreateInfo, ostoken);
	//	updateSyncResourceInfo(network.getId(),null,null,ParamConstant.ACTIVE_STATUS,ParamConstant.NETWORK,ostoken.getCurrentRegion());
		return network;
	}

	@Override
	public Network updateNetwork(String networkId, String updateBody, TokenOs ostoken)
			throws BusinessException, JsonProcessingException, IOException {
	
		Locale locale = new Locale(ostoken.getLocale());
	//	checkResource(networkId,locale);
		
		Network updatedNetwork = getUpdateNetworkInfo(updateBody);
		updatedNetwork.setName(StringHelper.string2Ncr(updatedNetwork.getName()));
		Network network = networkMapper.selectByPrimaryKey(networkId);
		if(null == network)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		if(true == network.getExternal())
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);


		checkName(updatedNetwork.getName(),ostoken);
		String networkUpdateBody = getUpdateBody(updatedNetwork);
		String region = ostoken.getCurrentRegion(); 
		
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/networks/");
		sb.append(networkId);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoPut(sb.toString(), headers, networkUpdateBody);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
	//	Network network = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				network = getNetwork(rs);
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
			rs = client.httpDoPut(sb.toString(), headers, networkUpdateBody);
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				network = getNetwork(rs);
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
			throw new ResourceBusinessException(Message.CS_NETWORK_UPDATE_FAILED,httpCode,locale);
		}
      
		/** comment by tanggc
		Network networkFromDB = networkMapper.selectByPrimaryKey(network.getId());
		if(null != networkFromDB){
			networkFromDB.setName(updatedNetwork.getName());
		}else{
			appendSubnetInfoAndStore2DB(network, ostoken, response);
		}
		**/
		networkMapper.insertOrUpdate(network);
		appendSubnetInfoAndStore2DB(network, ostoken);
	//	updateSyncResourceInfo(networkId,null,ParamConstant.ACTIVE_STATUS,ParamConstant.ACTIVE_STATUS,ParamConstant.NETWORK,ostoken.getCurrentRegion());

		return network;
	}

	private Network getUpdateNetworkInfo(String updateBody) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(updateBody);
		Network network = new Network();
		if (!rootNode.path(ResponseConstant.NAME).isMissingNode())
			network.setName(rootNode.path(ResponseConstant.NAME).textValue());
		return network;
//		if (null != rootNode.path(ResponseConstant.ADMIN_STATE_UP))
//			network.setAdmin_state_up(rootNode.path(ResponseConstant.ADMIN_STATE_UP).booleanValue());
//		if (null != rootNode.path(ResponseConstant.SHARED))
//			network.setShared(rootNode.path(ResponseConstant.SHARED).booleanValue());

	
	}
	
	private String getUpdateBody(Network network) throws JsonProcessingException, IOException {
		NetworkJSON networkJSON = new NetworkJSON(network);
		JsonHelper<NetworkJSON, String> jsonHelp = new JsonHelper<NetworkJSON, String>();
		return jsonHelp.generateJsonBodySimple(networkJSON);
//		if (null != rootNode.path(ResponseConstant.ADMIN_STATE_UP))
//			network.setAdmin_state_up(rootNode.path(ResponseConstant.ADMIN_STATE_UP).booleanValue());
//		if (null != rootNode.path(ResponseConstant.SHARED))
//			network.setShared(rootNode.path(ResponseConstant.SHARED).booleanValue());
	}

	@Override
	public void deleteNetwork(String networkId, TokenOs ostoken)
			throws BusinessException{

		Locale locale = new Locale(ostoken.getLocale());
		checkResource(networkId,locale);
		String region = ostoken.getCurrentRegion();

		// String url=ot.getEndPoint(TokenOs.EP_TYPE_NETWORK,
		// region).getPublicURL();
		// url=url+"/v2.0/networks/" + NetworkId;
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/networks/");
		sb.append(networkId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoDelete(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);

		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE: {
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
			rs = client.httpDoDelete(sb.toString(), headers);
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			if(httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			if (httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
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
			throw new ResourceBusinessException(Message.CS_NETWORK_DELETE_FAILED,httpCode,locale);
		}
		
		updateNetworkDBInfo(networkId);
	//	updateSyncResourceInfo(networkId,null,ParamConstant.ACTIVE_STATUS,ParamConstant.DELETED_STATUS,ParamConstant.NETWORK,ostoken.getCurrentRegion());

	}

	@Override
	public String getExternalNetworkId(String name,TokenOs ostoken) throws BusinessException{
		if(Util.isNullOrEmptyValue(name))
			return null;
		List<Network> networks = networkMapper.selectExternalNetworks();
		if(Util.isNullOrEmptyList(networks)){
			networks = getExternalNetworks(ostoken);
			if(Util.isNullOrEmptyList(networks))
				return null;
		}
		
		for(Network network : networks){
			if(network.getName().matches("(?i)"+name+".*"))
				return network.getId();
		}
		
		return null;
	}
	
	private void updateNetworkDBInfo(String networkId) {
		Network network = networkMapper.selectByPrimaryKey(networkId);
		if (null == network)
			return;
		List<Instance> instances = instanceMapper.selectByNetworkId(networkId);
		if (!Util.isNullOrEmptyList(instances)) {
			List<Instance> updatedInstances = new ArrayList<Instance>();
			for (Instance instance : instances) {
				List<String> networkIds = Util.getCorrectedIdInfo(instance.getNetworkIds(), networkId);
				instance.setNetworkIds(Util.listToString(networkIds, ','));
				updatedInstances.add(instance);
			}
			try {
				instanceMapper.insertOrUpdateBatch(updatedInstances);
				//updateByPrimaryKeySelective(instance);
			} catch (Exception e) {
				// TODO
			}
		}
		
		List<Subnet> subnets = subnetMapper.selectListByNetworkId(networkId);
		if(!Util.isNullOrEmptyList(subnets)){
			List<Subnet> updatedSubnets = new ArrayList<Subnet>();
			for (Subnet subnet : subnets) {
				subnet.setNetwork_id(null);
				updatedSubnets.add(subnet);
			}
			try {
				subnetMapper.insertOrUpdateBatch(updatedSubnets);
				// updateByPrimaryKeySelective(subnet);
			} catch (Exception e) {
				// TODO
			}
		}
		
		try {
			networkMapper.deleteByPrimaryKey(networkId);
		} catch (Exception e) {
			// TODO
		}
	}
	
	private String generateBody(String name, String tenant_id) {

		NetworkJSON networkJSON = new NetworkJSON();
		networkJSON.setNetworkInfo(name, tenant_id, true);

		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.setSerializationInclusion(Include.NON_EMPTY);
		String jsonStr = "";
		try {
			jsonStr = mapper.writeValueAsString(networkJSON);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		return jsonStr;
	}

	private void makeNetworkInfoFromCreateBody(String createBody,Network network)
			throws ResourceBusinessException, JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(createBody);
		
		network.setName(rootNode.path(ResponseConstant.NAME).textValue());
		
		JsonNode subnetsNode =  rootNode.path(ResponseConstant.SUBNETS);
		int size = subnetsNode.size();
		if(0 == size)
			return;
		List<Subnet> subnets = new ArrayList<Subnet>();
		for(int index = 0; index < size; ++index){
			Subnet subnet  = new Subnet();
			subnet.setName(subnetsNode.get(index).path(ResponseConstant.NAME).textValue());
			subnet.setIp_version(4);
			subnet.setCidr(subnetsNode.get(index).path(ResponseConstant.CIDR).textValue());
			subnet.setGateway_ip(subnetsNode.get(index).path(ResponseConstant.GATEWAY).textValue());
			if(!subnetsNode.get(index).path(ResponseConstant.DHCP).isMissingNode())
				subnet.setDhcp(subnetsNode.get(index).path(ResponseConstant.DHCP).booleanValue());
			subnets.add(subnet);
		}
		
		network.setSubnets(subnets);
	}

	private List<Network> getNetworks(Map<String, String> rs,String tenantId) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode networksNode = rootNode.path(ResponseConstant.NETWORKS);
		int networksCount = networksNode.size();
		if (0 == networksCount)
			return null;
		List<Network> networks = new ArrayList<Network>();
		for (int index = 0; index < networksCount; ++index) {
			Network network = getNetworkInfo(networksNode.get(index));
			if(!tenantId.equals(network.getTenant_id()))
				continue;
			networks.add(network);
		}
		return networks;
	}

//	private String getFailedReason(Map<String, String> rs){
//		String message = "";
//		ObjectMapper mapper = new ObjectMapper();
//		try{
//			JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
//			JsonNode networkNode = rootNode.path(ResponseConstant.NETWORK_FAILED);
//			message = networkNode.path(ResponseConstant.MESSAGE).textValue();
//		}catch(Exception e){
//			return "";
//		}
//		return message;
//	}
	
	private Network getNetwork(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode networkNode = rootNode.path(ResponseConstant.NETWORK);
		return getNetworkInfo(networkNode);
	}
	
	private boolean isExernalNetwork(String name){
		if(Util.isNullOrEmptyValue(cloudconfig.getSystemFloatingSpec()) || Util.isNullOrEmptyValue(name))
			return false;
		if(Util.isNullOrEmptyValue(name))
			return false;
		String[] externals = cloudconfig.getSystemFloatingSpec().split(",");
		for(int index = 0; index < externals.length; ++index){
			if(name.matches("(?i)"+externals[index]+".*"))
				return true;
		}
		return false;
	}
	
	private Network getNetworkInfo(JsonNode networkNode) {
		if (null == networkNode)
			return null;
		Network network = new Network();
		network.setStatus(networkNode.path(ResponseConstant.STATUS).textValue());
		network.setName(networkNode.path(ResponseConstant.NAME).textValue());
		network.setExternal(isExernalNetwork(network.getName()));
		network.setId(networkNode.path(ResponseConstant.ID).textValue());
		network.setTenant_id(networkNode.path(ResponseConstant.TENANT_ID).textValue());
		network.setMtu(networkNode.path(ResponseConstant.MTU).intValue());
		network.setQos_policy_id(networkNode.path(ResponseConstant.QOS_POLICY_ID).textValue());
		network.setShared(networkNode.path(ResponseConstant.SHARED).booleanValue());
	//	network.setMillionSeconds(Util.time2Millionsecond(networkNode.path(ResponseConstant.CREATED_AT).textValue(), ParamConstant.TIME_FORMAT_02));
	//	network.setCreatedAt(networkNode.path(ResponseConstant.CREATED_AT).textValue());
		JsonNode subnetsNode = networkNode.path(ResponseConstant.SUBNETS);
		if (null != subnetsNode) {
			int subnetsCount = subnetsNode.size();
			for (int index = 0; index < subnetsCount; ++index) {
				network.addSubnetId(subnetsNode.get(index).textValue());
			}
		}
		return network;
	}

//	private List<Network> filterNetwork(Map<String, String> paramMap, List<Network> networks) {
//		if (null == paramMap || 0 == paramMap.size())
//			return networks;
//
//		String networkName = paramMap.get(ParamConstant.NAME);
//		if (null != networkName && !"".equals(networkName)) {
//			for (Network network : networks) {
//				if (networkName.equals(network.getName())) {
//					List<Network> goodNetworks = new ArrayList<Network>();
//					goodNetworks.add(network);
//					return goodNetworks;
//				}
//			}
//		}
//
//		String strLimit = paramMap.get(ParamConstant.LIMIT);
//		if (null != strLimit && !"".equals(strLimit)) {
//			try {
//				int limit = Integer.parseInt(strLimit);
//				if (limit >= networks.size())
//					return networks;
//				return networks.subList(0, limit);
//			} catch (Exception e) {
//				// TODO
//				return networks;
//			}
//		}
//
//		return networks;
//	}

//	private List<Network> setSubnetInfos(List<Network> networks, TokenOs token) throws BusinessException {
//		if (null == networks || 0 == networks.size())
//			return null;
//
//		SubnetService snService = OsApiServiceFactory.getSubnetService();
//		List<Subnet> subnets = snService.getSubnetList(null, token, null);
//		if (null == subnets || 0 == subnets.size())
//			return null;
//
//		List<Network> networkwithSubnets = new ArrayList<Network>();
//
//		for (Network network : networks) {
//			for (Subnet subnet : subnets) {
//				if (network.getId().equals(subnet.getNetwork_id())) {
//					network.addSubnet(subnet);
//				}
//			}
//			networkwithSubnets.add(network);
//		}
//		return networkwithSubnets;
//	}

//	private String generateSubnetkBody(Subnet subnetInfo) {
//		SubnetJSON subnetJSON = new SubnetJSON();
//		subnetJSON.setSubnetInfo(subnetInfo);
//
//		ObjectMapper mapper = new ObjectMapper();
//		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
//		mapper.setSerializationInclusion(Include.NON_NULL);
//		mapper.setSerializationInclusion(Include.NON_EMPTY);
//		String jsonStr = "";
//		try {
//			jsonStr = mapper.writeValueAsString(subnetJSON);
//		} catch (JsonProcessingException e) {
//			// TODO Auto-generated catch block
//			log.error(e);
//		}
//		return jsonStr;
//	}

	private String generateNetworkBody(String name, String tenant_id, Boolean state) {

		NetworkJSON networkJSON = new NetworkJSON();
		networkJSON.setNetworkInfo(name, tenant_id, state);

		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.setSerializationInclusion(Include.NON_EMPTY);
		String jsonStr = "";
		try {
			jsonStr = mapper.writeValueAsString(networkJSON);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		return jsonStr;
	}

	private void storeNetworkAndSubnet2DB(Network network, Network networkCreateInfo, TokenOs ostoken) throws BusinessException {
		if (null == network)
			return;
		List<Subnet> subnets = networkCreateInfo.getSubnets();
		List<Subnet> subnetsCreateInfo = new ArrayList<Subnet>();
		for (Subnet subnet : subnets) {
			subnet.setNetwork_id(network.getId());
			subnet.setName(StringHelper.string2Ncr(subnet.getName()));
			subnetsCreateInfo.add(subnet);
		}
		List<Subnet> createdSubnets = null;
		try{
			createdSubnets = subnetService.createSubnets(subnetsCreateInfo, ostoken);
		}catch(BusinessException e){
			this.deleteNetwork(network.getId(), ostoken);
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		//List<Subnet> createdSubnets = subnetService.createSubnets(subnetsCreateInfo, ostoken, response);
		network.setSubnets(createdSubnets);
		network.makeSubnetsId();
		//networkMapper.insertSelective(network);
		networkMapper.insertOrUpdate(network);
	}
	
	private void appendSubnetInfo(Network network) {
		String subnetId = network.getSubnetId();
		if (!Util.isNullOrEmptyValue(subnetId)) {
			String[] subnetsId = subnetId.split(",");
			for (int index = 0; index < subnetsId.length; ++index) {
				Subnet subnet = subnetMapper.selectByPrimaryKey(subnetsId[index]);
				if (null != subnet)
					network.addSubnet(subnet);
			}
		}
	}

	private void appendSubnetInfo(List<Network> networksFromDB){
		for (Network network : networksFromDB) {
			appendSubnetInfo(network);
		}
	}
	
	private List<Network> getNetworksFromDB(String tenantId, int limit) {
		List<Network> networksFromDB = null;
		if (-1 == limit) {
			networksFromDB = networkMapper.selectListByTenantId(tenantId);
		} else {
			networksFromDB = networkMapper.selectListByTenantIdWithLimit(tenantId, limit);
		}
		if (Util.isNullOrEmptyList(networksFromDB))
			return null;
		appendSubnetInfo(networksFromDB);
		return networksFromDB;
//		List<Network> networksFromDBWithSubnet = new ArrayList<Network>();
//		for (Network network : networksFromDB) {
//			appendSubnetInfo(network);
//			networksFromDBWithSubnet.add(network);
//		}
//		return networksFromDBWithSubnet;
	}

	private void appendSubnetInfoAndStore2DB(Network network, TokenOs ostoken)
			throws BusinessException {
		//networkMapper.insertOrUpdate(network);

		List<String> subnetsId = network.getSubnetsId();
		for (String subnetId : subnetsId) {
			Subnet subnet = subnetMapper.selectByPrimaryKey(subnetId);
			if (null != subnet) {
				subnet.setName(StringHelper.ncr2String(subnet.getName()));
				network.addSubnet(subnet);
			} else {
				subnet = subnetService.getSubnet(subnetId, ostoken);
				if (null == subnet)
					continue;
				network.addSubnet(subnet);
				//subnetMapper.updateByPrimaryKeySelective(subnet);
			}
		}
	}

	private List<Network> storeNetworks2DB(List<Network> networks, TokenOs ostoken)
			throws BusinessException {
		if (Util.isNullOrEmptyList(networks))
			return null;
		List<Network> networksWithSubnet = new ArrayList<Network>();
		for (Network network : networks) {
			List<String> subnetsId = network.getSubnetsId();
			if (!Util.isNullOrEmptyList(subnetsId))
				network.setSubnetId(Util.listToString(subnetsId, ','));
			appendSubnetInfoAndStore2DB(network, ostoken);
			networksWithSubnet.add(network);
		}
		networkMapper.insertOrUpdateBatch(networks);

		return networksWithSubnet;
	}

	private List<Network> getLimitItems(List<Network> networks, String tenantId,int limit) {
		if(Util.isNullOrEmptyList(networks))
			return null;
//		List<Network> tenantNetworks = new ArrayList<Network>();
//		for(Network network : networks){
//			if(!tenantId.equals(network.getTenant_id()))
//				continue;
//			tenantNetworks.add(network);
//		}
		if(-1 != limit){
			if(limit <= networks.size())
				return networks.subList(0, limit);
		}
		return networks;
	}
	
	private List<Network> getExternalnetworks(List<Network> networks) {
		if(Util.isNullOrEmptyList(networks))
			return null;
		List<Network> externalNetworks = new ArrayList<Network>();
		for(Network network : networks){
			if(false == network.getExternal())
				continue;
			externalNetworks.add(network);
		}

		return externalNetworks;
	}
	
//	private void normalNetworkCreatedTime(List<Network> networks){
//		if(null == networks)
//			return;
//		for(Network network : networks){
//		   	network.setCreatedAt(Util.millionSecond2Date(network.getMillionSeconds()));
//		   	network.setName(StringHelper.ncr2String(network.getName()));
//		}
//	}
	
//	private void checkResource(String id,Locale locale) throws BusinessException{
//		SyncResource syncResource = syncResourceMapper.selectByPrimaryKey(id);
//		if(null != syncResource)
//			throw new ResourceBusinessException(Message.CS_RESOURCE_IS_DOING_ERROR,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
//	}
	
//	private void updateSyncResourceInfo(String id,String relatedResource,String orgStatus,String expectedStatus,String type,String region){
//		SyncResource resource = new SyncResource();
//		resource.setId(id);
//		resource.setType(type);
//		resource.setOrgStatus(orgStatus);
//		resource.setExpectedStatus(expectedStatus);
//		resource.setRelatedResource(relatedResource);
//		resource.setRegion(region);
//		syncResourceMapper.insertSelective(resource);
//	}
	
	private void checkResource(String id,Locale locale) throws BusinessException{
		Network network = networkMapper.selectByPrimaryKey(id);
		if(null == network)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
        String subnetid = network.getSubnetId();
        if(!Util.isNullOrEmptyValue(subnetid))
			throw new ResourceBusinessException(Message.CS_NET_HAVE_RELATED_RESOURCE,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
		if(true == network.getExternal())
			throw new ResourceBusinessException(Message.CS_EXTERNAL_NETWORK_DELETE_FAILED,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
        return;
	}
	
	private void checkName(String name,TokenOs ostoken)  throws BusinessException{
		if(Util.isNullOrEmptyValue(name))
			return;
		List<Network> nets = networkMapper.selectListByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(nets))
			return;
		for(Network net : nets){
			if(name.equals(net.getName()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_NAME_IS_SAME,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
            if(this.isExernalNetwork(name))
				throw new ResourceBusinessException(Message.CS_UPDATE_EXTRENAL_NET_IS_FORBIDDEN,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		return;
	}
}
