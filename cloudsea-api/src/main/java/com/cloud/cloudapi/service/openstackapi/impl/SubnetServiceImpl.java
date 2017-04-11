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

import com.cloud.cloudapi.dao.common.NetworkMapper;
import com.cloud.cloudapi.dao.common.PortMapper;
import com.cloud.cloudapi.dao.common.QuotaDetailMapper;
import com.cloud.cloudapi.dao.common.QuotaMapper;
import com.cloud.cloudapi.dao.common.RouterMapper;
import com.cloud.cloudapi.dao.common.SubnetMapper;
import com.cloud.cloudapi.dao.common.TenantMapper;
import com.cloud.cloudapi.dao.common.VPNMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.SubnetJSON;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Network;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Port;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Router;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Subnet;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VPN;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.NetworkService;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.service.openstackapi.RouterService;
import com.cloud.cloudapi.service.openstackapi.SubnetService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.StringHelper;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("subnetService")
public class SubnetServiceImpl  implements SubnetService {
	
	@Resource
	private OSHttpClientUtil client;

	@Autowired
	private SubnetMapper subnetMapper;
	
	@Autowired
	private NetworkMapper networkMapper;
	
	@Autowired
	private CloudConfig cloudconfig;
	
	@Autowired
	private QuotaMapper quotaMapper;
	
	@Autowired
	private RouterMapper routerMapper;
	
	@Resource
	private PortMapper portMapper;
	
	@Resource
	private VPNMapper vpnMapper;
	
	@Autowired
	private QuotaDetailMapper quotaDetailMapper;
	
	@Autowired
	private TenantMapper tenantMapper;
	
	@Resource
	private QuotaService quotaService;
	
	@Resource
	private RouterService routerService;

	@Resource
	private NetworkService networkService;
	
	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(SubnetServiceImpl.class);
	
	public SubnetMapper getSubnetMapper() {
		return subnetMapper;
	}

	public void setSubnetMapper(SubnetMapper subnetMapper) {
		this.subnetMapper = subnetMapper;
	}

	public NetworkMapper getNetworkMapper() {
		return networkMapper;
	}

	public void setNetworkMapper(NetworkMapper networkMapper) {
		this.networkMapper = networkMapper;
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

	public RouterMapper getRouterMapper() {
		return routerMapper;
	}

	public void setRouterMapper(RouterMapper routerMapper) {
		this.routerMapper = routerMapper;
	}

	
	/**
	 * get this users subnet list
	 */
	@Override
	public List<Subnet> getSubnetList(Map<String, String> paramMap,TokenOs ostoken)
			throws BusinessException {

		int limitItems = Util.getLimit(paramMap);
		List<Subnet> subnetsFromDB = getSubnetsFromDB(ostoken.getTenantid(),limitItems);
        if(!Util.isNullOrEmptyList(subnetsFromDB)){
        	//normalSubnetCreatedTime(subnetsFromDB);
        	return subnetsFromDB;
        }
        
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/subnets", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
        Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(url, ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		List<Subnet> subnets = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				subnets = getSubnets(rs, ostoken);
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
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				subnets = getSubnets(rs, ostoken);
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
			throw new ResourceBusinessException(Message.CS_SUBNET_GET_FAILED,httpCode,locale);
		}
		subnets = storeSubnets2DB(subnets);
		//normalSubnetCreatedTime(subnets);
		return getLimitItems(subnets,ostoken.getTenantid(),limitItems);
	}

	@Override
	public List<Subnet> getSpecialAdminNetwork(TokenOs ostoken)
			throws BusinessException {
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		String networkId = cloudconfig.getSystemVmwareNetworkId();
		Network network = networkService.getNetwork(networkId, null);
		if(null == network)
			return null;
		List<Subnet> subnets = network.getSubnets();
		//normalSubnetCreatedTime(subnets);
		return subnets;
	}
	
	@Override
	public Subnet getSubnet(String subnetId,TokenOs ostoken) throws BusinessException {
		Subnet subnet = getSubnetFromDB(subnetId);
		if(null != subnet){
			setSubnetInfo(subnet);
			return subnet;
		}
		String region = ostoken.getCurrentRegion();

		// String url=ot.getEndPoint(TokenOs.EP_TYPE_NETWORK,
		// region).getPublicURL();
		// url=url+"/v2.0/networks/" + NetworkId;
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/subnets/");
		sb.append(subnetId);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				subnet = getSubnet(rs, ostoken);
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
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				subnet = getSubnet(rs, ostoken);
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
			throw new ResourceBusinessException(Message.CS_SUBNET_DETAIL_GET_FAILED,httpCode,locale);
		}

		storeSubnet2DB(subnet);
		setSubnetInfo(subnet);
		return subnet;
	}

	private Subnet createSingleSubnet(Subnet subnetCreateInfo, TokenOs ostoken) throws BusinessException {
		String subnetCreateBody = generateBody(subnetCreateInfo);
		String region = ostoken.getCurrentRegion();
		// String url=ot.getEndPoint(TokenOs.EP_TYPE_NETWORK,
		// region).getPublicURL();
		// url=url+"/v2.0/networks/" + NetworkId;
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPost(url + "/v2.0/subnets", ostoken.getTokenid(), subnetCreateBody);
		Util.checkResponseBody(rs,locale);

		String errorMessage = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(errorMessage))
			log.error(errorMessage);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		Subnet subnet = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				subnet = getSubnet(rs, ostoken);
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
			rs = client.httpDoPost(url + "/v2.0/subnets", tokenid, subnetCreateBody);
			errorMessage = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(errorMessage))
				log.error(errorMessage);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				subnet = getSubnet(rs, ostoken);
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
			throw new ResourceBusinessException(Message.CS_SUBNET_CREATE_FAILED,httpCode,locale);
		}
		subnet.setMillionSeconds(Util.getCurrentMillionsecond());
		return subnet;
	}

	@Override
	public List<Subnet> createSubnets(List<Subnet> subnetsCreateInfo, TokenOs ostoken) throws BusinessException {
		List<Subnet> newSubnets = new ArrayList<Subnet>();
		List<String> newSubnetIds = new ArrayList<String>();
		String updatedNetworkId = "";

		for (Subnet subnetCreateInfo : subnetsCreateInfo) {
			checkName(subnetCreateInfo.getName(), ostoken);
			Subnet newSubnet = createSingleSubnet(subnetCreateInfo, ostoken);
			newSubnets.add(newSubnet);
			newSubnetIds.add(newSubnet.getId());
			updatedNetworkId = subnetCreateInfo.getNetwork_id();
		}
		
		if(!Util.isNullOrEmptyList(newSubnets)){
			subnetMapper.insertOrUpdateBatch(newSubnets);
			updateNetworksInfo(updatedNetworkId,newSubnetIds);		
		}

		return newSubnets;
	}
	
	@Override
	public List<Subnet> createSubnet(String createBody, String instanceName,TokenOs ostoken, String networkId)
			throws BusinessException, JsonProcessingException, IOException {
		List<Subnet> subnets = new ArrayList<Subnet>();
		if (Util.isNullOrEmptyValue(createBody)){
			Subnet subnetCreateInfo = new Subnet();
			subnetCreateInfo.setNetwork_id(networkId);
			subnetCreateInfo.setCidr(getSubNetCIDR(cloudconfig.getIpRange(),ostoken.getTenantid(),new Locale(ostoken.getLocale())));
			subnetCreateInfo.setIp_version(4);
			subnetCreateInfo.setName(String.format("%s_Subnet", StringHelper.string2Ncr(instanceName)));
			subnets.add(subnetCreateInfo);
		}else{
			makeSubnetInfoFromCreateBody(createBody,subnets);
//				Subnet subnetCreateInfo = makeSubnetInfoFromCreateBody(createBody, response);
//				subnets.add(subnetCreateInfo);
		}
		return createSubnets(subnets,ostoken);
	}

	@Override
	public Subnet updateSubnet(String subnetId,String updateBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException{
		String subnetUpdateBody = getUpdateBody(updateBody,ostoken);
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/subnets/");
		sb.append(subnetId);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoPut(sb.toString(), ostoken.getTokenid(),subnetUpdateBody);
		Util.checkResponseBody(rs, locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);	
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		Subnet subnet = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				subnet = getSubnet(rs, ostoken);
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
			rs =client.httpDoPut(sb.toString(), tokenid,subnetUpdateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				subnet = getSubnet(rs, ostoken);
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
			throw new ResourceBusinessException(Message.CS_SUBNET_UPDATE_FAILED,httpCode,locale);
		}
		setSubnetInfo(subnet);
		storeSubnet2DB(subnet);	
		return subnet;
	}
	
	@Override
	public void deleteSubnet(String subnetId,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(subnetId,locale);
		
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/subnets/");
		sb.append(subnetId);
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
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
			rs = client.httpDoDelete(sb.toString(), tokenid);
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			if(httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
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
			throw new ResourceBusinessException(Message.CS_SUBNET_DELETE_FAILED,httpCode,locale);
		}

		subnetMapper.deleteByPrimaryKey(subnetId);
		updateRelatedResourceInfo(subnetId);
	//	updateSubnetQuota(ostoken,false);
	}
	
	@Override
	public void connectRouter(String subnetId,String createBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(createBody);
		String routerId = rootNode.path(ResponseConstant.ROUTER_ID).textValue();
		
		StringBuilder sb = new StringBuilder();
		sb.append("{\"");
		sb.append(ParamConstant.SUBNET_ID);
		sb.append("\":\"");
		sb.append(subnetId);
		sb.append("\"}");

		try{
			routerService.addInterfaceToRouter(routerId, sb.toString(), ostoken);	
		}catch(Exception e){
			throw new ResourceBusinessException(Message.CS_SUBNET_ADD_ROUTER_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
	}
	
	private String getUpdateBody(String updateBody,TokenOs ostoken) throws JsonProcessingException, IOException, BusinessException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(updateBody);
		Subnet subnet = new Subnet();
		if (!rootNode.path(ResponseConstant.NAME).isMissingNode())
			subnet.setName(StringHelper.string2Ncr(rootNode.path(ResponseConstant.NAME).textValue()));
//		if(null != rootNode.path(ResponseConstant.ENABLE_DHCP))
//			subnet.setEnable_dhcp(rootNode.path(ResponseConstant.ENABLE_DHCP).booleanValue());
        checkName(subnet.getName(),ostoken);
        
		SubnetJSON subnetJSON = new SubnetJSON(subnet);
		JsonHelper<SubnetJSON, String> jsonHelp = new JsonHelper<SubnetJSON, String>();
		return jsonHelp.generateJsonBodySimple(subnetJSON);
	}
	
	private void updateNetworksInfo(String networkId,List<String> subnetIds){
		if(Util.isNullOrEmptyList(subnetIds))
			return;
		Network network = networkMapper.selectByPrimaryKey(networkId);
		if (null == network) {
			// TODO
		} else {
			if(!Util.isNullOrEmptyValue(network.getSubnetId())){
				String[] existingSubnetIds = network.getSubnetId().split(",");
				for(int index = 0; index < existingSubnetIds.length; ++index){
					if(subnetIds.contains(existingSubnetIds[index]))
						continue;
					subnetIds.add(existingSubnetIds[index]);
				}
			}
			network.setSubnetId(Util.listToString(subnetIds, ','));
			networkMapper.insertOrUpdate(network);
		//	networkMapper.updateByPrimaryKeySelective(network);
		}		
	}
	
//	private void updateNetworkInfo(Subnet subnet){
//		Network network = networkMapper.selectByPrimaryKey(subnet.getNetwork_id());
//		if (null == network) {
//			// TODO
//		} else {
//			String subnetId = Util.getIdWithAppendId(subnet.getId(), network.getSubnetId());
//			network.setSubnetId(subnetId);
//			networkMapper.updateByPrimaryKeySelective(network);
//		}		
//	}
	
//	private void updateSubnetQuota(TokenOs ostoken,boolean bAdd){
//		quotaService.updateQuota(ParamConstant.SUBNET,ostoken,bAdd,1);
//	}
	
	private void storeSubnet2DB(Subnet subnet){
		if(null == subnet)
			return;
		subnetMapper.insertOrUpdate(subnet);
//		if (null != subnetMapper.selectByPrimaryKey(subnet.getId()))
//			subnetMapper.updateByPrimaryKeySelective(subnet);
//		else
//			subnetMapper.insertSelective(subnet);
	}
	
	private void addNetworkInfo2Subnet(Subnet subnet){
		if(null == subnet)
			return;
		String networkId = subnet.getNetwork_id();
		Network network = networkMapper.selectByPrimaryKey(networkId);
        if(null != network)
           subnet.setNetwork(network);
	}
	
	private List<Subnet> storeSubnets2DB(List<Subnet> subnets){
		if(Util.isNullOrEmptyList(subnets))
			return null;
		List<Subnet> subnetsWithNetwork = new ArrayList<Subnet>();
		for (Subnet subnet : subnets) {
			storeSubnet2DB(subnet);
			addNetworkInfo2Subnet(subnet);			
			subnetsWithNetwork.add(subnet);
		}
		return subnetsWithNetwork;
	}

	private Subnet getSubnetFromDB(String subnetId){
		Subnet subnetFromDB = subnetMapper.selectByPrimaryKey(subnetId);
		if (null != subnetFromDB) {
			String networkId = subnetFromDB.getNetwork_id();
			if(!Util.isNullOrEmptyValue(networkId)){
				Network network = networkMapper.selectByPrimaryKey(networkId);
                if(null != network)
                	subnetFromDB.setNetwork(network);
			}
		}
	    return subnetFromDB;
	}
	
	public String getSubNetCIDR(String defaultCidr,String tenantId,Locale locale) throws BusinessException{

		// get the next sub net it by the current max subnet of local subnet db
		// the sub net mask is 255.255.0.0
		int pos = defaultCidr.indexOf('/');
		if (-1 == pos)
			return "10.167.0.0/16";
		String defaultIp = defaultCidr.substring(0, pos);
		String[] defaultIpOctals = defaultIp.split("\\.");
		if (4 != defaultIpOctals.length)
			return "10.167.0.0/16";

		int defaultMask = Integer.parseInt(defaultCidr.substring(pos + 1));
		List<Subnet> subnets = subnetMapper.selectListByTenantId(tenantId);
		if (Util.isNullOrEmptyList(subnets))
			return defaultCidr;
		if (defaultMask == ParamConstant.EIGHT_BIT_MASK) {
			int iFirstOctal = Integer.parseInt(defaultIpOctals[0]) + subnets.size();
			if (iFirstOctal > 255) {
				throw new ResourceBusinessException(Message.CS_GET_CIDR_ERROR,locale);
			}
			return String.format("%s.0.0.0/8", iFirstOctal);
		} else if (defaultMask == ParamConstant.SIXTEEN_BIT_MASK) {
			int iFirstOctal = Integer.parseInt(defaultIpOctals[0]);
			int iSecondOctal = Integer.parseInt(defaultIpOctals[1]) + subnets.size();
			if (iSecondOctal > 255) {
				iFirstOctal += 1;
				if (iFirstOctal > 255) {
					throw new ResourceBusinessException(Message.CS_GET_CIDR_ERROR,locale);
				}
			}
			return String.format("%s.%s.0.0/16", iFirstOctal,iSecondOctal);
		} else if (defaultMask == ParamConstant.TWENTYFOUR_BIT_MASK) {
			int iFirstOctal = Integer.parseInt(defaultIpOctals[0]);
			int iSecondOctal = Integer.parseInt(defaultIpOctals[1]);
			int iThirdOctal = Integer.parseInt(defaultIpOctals[2]) + subnets.size();
			if (iThirdOctal > 255) {
				iSecondOctal += 1;
				if (iSecondOctal > 255) {
					iSecondOctal -= 1;
					iFirstOctal += 1;
				}
				if (iFirstOctal > 255) {
					throw new ResourceBusinessException(Message.CS_GET_CIDR_ERROR,locale);
				}
			}
			return String.format("%s.%s.%s.0/24", iFirstOctal,iSecondOctal,iThirdOctal);
		} else {
			return "10.167.0.0/16";
		}
	}

//	private String generateBody(String network_id, String name, String cidr, String tenant_id, String ip_version) {
//
//		SubnetJSON subnetJSON = new SubnetJSON();
//		subnetJSON.setSubnetInfo(network_id, name, cidr, tenant_id, ip_version);
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

//	private List<Subnet> filterSubnet(Map<String, String> paramMap, List<Subnet> subnets) {
//		if (null == paramMap || 0 == paramMap.size())
//			return subnets;
//
//		String subnetName = paramMap.get(ParamConstant.NAME);
//		if (null != subnetName && !"".equals(subnetName)) {
//			for (Subnet subnet : subnets) {
//				if (subnetName.equals(subnet.getName())) {
//					List<Subnet> goodsubnets = new ArrayList<Subnet>();
//					goodsubnets.add(subnet);
//					return goodsubnets;
//				}
//			}
//		}
//
//		String strLimit = paramMap.get(ParamConstant.LIMIT);
//		if (null != strLimit && !"".equals(strLimit)) {
//			try {
//				int limit = Integer.parseInt(strLimit);
//				if (limit >= subnets.size())
//					return subnets;
//				return subnets.subList(0, limit);
//			} catch (Exception e) {
//				// TODO
//				return subnets;
//			}
//		}
//		return subnets;
//	}

	private List<Subnet> getSubnets(Map<String, String> rs, TokenOs ostoken)
			throws JsonProcessingException, IOException, BusinessException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode subnetsNode = rootNode.path(ResponseConstant.SUBNETS);
		int subnetsCount = subnetsNode.size();
		if (0 == subnetsCount)
			return null;

		List<Subnet> subnets = new ArrayList<Subnet>();
		for (int index = 0; index < subnetsCount; ++index) {
			Subnet subnet = getSubnetInfo(subnetsNode.get(index), ostoken);
			if(!ostoken.getTenantid().equals(subnet.getTenant_id()))
				continue;
			subnets.add(subnet);
		}

		return subnets;
	}

	private Subnet getSubnet(Map<String, String> rs,TokenOs ostoken) throws JsonProcessingException, IOException, BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		return getSubnetInfo(rootNode.path(ResponseConstant.SUBNET),ostoken);
	}
	
	private Subnet getSubnetInfo(JsonNode subnetNode, TokenOs ostoken) throws BusinessException {
		if (null == subnetNode)
			return null;
		Subnet subnet = new Subnet();
		subnet.setName(subnetNode.path(ResponseConstant.NAME).textValue());
		subnet.setId(subnetNode.path(ResponseConstant.ID).textValue());
		subnet.setIpVersion(Integer.toString(subnetNode.path(ResponseConstant.IP_VERSION).intValue()));
		subnet.setGateway(subnetNode.path(ResponseConstant.GATEWAY_IP).textValue());
		subnet.setCidr(subnetNode.path(ResponseConstant.CIDR).textValue());
		subnet.setNetwork_id(subnetNode.path(ResponseConstant.NETWORK_ID).textValue());
		subnet.setTenant_id(subnetNode.path(ResponseConstant.TENANT_ID).textValue());
	//	subnet.setCreatedAt(subnetNode.path(ResponseConstant.CREATED_AT).textValue());
	//	subnet.setMillionSeconds(Util.time2Millionsecond(subnetNode.path(ResponseConstant.CREATED_AT).textValue(), ParamConstant.TIME_FORMAT_02));
		// NetworkService nwService = OsApiServiceFactory.getNetworkService();
		// Network network = nwService.getNetwork(networkId,ostoken, null);
		// subnet.setNetwork(network);

		return subnet;
	}
	
	private List<Subnet> getLimitItems(List<Subnet> subnets,String tenantId,int limit){
		if(Util.isNullOrEmptyList(subnets))
			return null;
//		List<Subnet> tenantSubnets = new ArrayList<Subnet>();
//		for(Subnet subnet : subnets){
//			if(!tenantId.equals(subnet.getTenant_id()))
//				continue;
//			tenantSubnets.add(subnet);
//		}
		if(-1 != limit){
			if(limit <= subnets.size())
				return subnets.subList(0, limit);
		}
		return subnets;
	}
	
	private String generateBody(Subnet subnetCreateInfo ) {
		SubnetJSON subnetJson = new SubnetJSON();
		subnetJson.setSubnetInfo(subnetCreateInfo);

		JsonHelper<SubnetJSON, String> jsonHelp = new JsonHelper<SubnetJSON, String>();
		return jsonHelp.generateJsonBodySimple(subnetJson);
	}
	
//	private Subnet makeSubnetInfoFromCreateBody(String createBody, HttpServletResponse response)
//			throws ResourceBusinessException {
//		ObjectMapper mapper = new ObjectMapper();
//		Subnet subnetInfo = null;
//		try {
//			subnetInfo = mapper.readValue(createBody, Subnet.class);
//		} catch (Exception e) {
//			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
//		}
//
//		return subnetInfo;
//	}
	
	private void makeSubnetInfoFromCreateBody(String createBody,List<Subnet> subnets)
			throws ResourceBusinessException, JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(createBody);
		
		String networkId = rootNode.path(ResponseConstant.NETWORK).textValue();
		JsonNode subnetsNode = rootNode.path(ResponseConstant.SUBNETS);
		for(int index = 0; index < subnetsNode.size(); ++index){
			JsonNode subnetNode = subnetsNode.get(index);
			Subnet subnet = new Subnet();
			subnet.setCidr(subnetNode.path(ResponseConstant.CIDR).textValue());
			subnet.setDhcp(subnetNode.path(ResponseConstant.DHCP).booleanValue());
			subnet.setGateway(subnetNode.path(ResponseConstant.GATEWAY).textValue());
			subnet.setIp_version(4);
			subnet.setNetwork_id(networkId);
			subnet.setName(StringHelper.string2Ncr(subnetNode.path(ResponseConstant.NAME).textValue()));
			subnets.add(subnet);
		}
	}
	
	private List<Subnet> getSubnetsFromDB(String tenantId,int limitItems){
		List<Subnet> subnetsFromDB = null;
		if(-1 == limitItems){
			subnetsFromDB = subnetMapper.selectListByTenantId(tenantId);
		}else{
			subnetsFromDB = subnetMapper.selectListByTenantIdWithLimit(tenantId,limitItems);
		}
		if(Util.isNullOrEmptyList(subnetsFromDB))
		    return null;
		List<Subnet> subnetsWithNetwork = new ArrayList<Subnet>();
		for(Subnet subnet : subnetsFromDB){
			Network network = networkMapper.selectByPrimaryKey(subnet.getNetwork_id());
			if(null != network){
				subnet.setNetwork(network);
			}else{
				subnet.setNetwork(null);
			}
			subnetsWithNetwork.add(subnet);
		}
		return subnetsWithNetwork;
	}
	
	private void setSubnetInfo(Subnet subnet){
		if(null == subnet)
			return;
		addNetworkInfo2Subnet(subnet);
		List<Router> routers = routerMapper.selectBySubnetId(subnet.getId());
		subnet.setRouters(routers);
	}
	
	private void updateRelatedResourceInfo(String subnetId){
		List<Router> routers = routerMapper.selectBySubnetId(subnetId);
		if(!Util.isNullOrEmptyList(routers)){
			for(Router router : routers){
				if(subnetId.equals(router.getSubnet_id()))
					router.setSubnet_id(null);
				router.setSubnetIds(Util.listToString(Util.getCorrectedIdInfo(router.getSubnetIds(), subnetId), ','));
			}
			routerMapper.insertOrUpdateBatch(routers);	
		}
		
		
		List<Network> networks = networkMapper.selectBySubnetId(subnetId);
		if(!Util.isNullOrEmptyList(networks)){
			for(Network network : networks){
				network.setSubnetId(Util.listToString(Util.getCorrectedIdInfo(network.getSubnetId(), subnetId), ','));
			}
			networkMapper.insertOrUpdateBatch(networks);	
		}
	}
	
	private void checkResource(String id, Locale locale) throws BusinessException {
	    List<Port> ports = portMapper.selectPortsBySubetId(id);
		if(!Util.isNullOrEmptyList(ports))
			throw new ResourceBusinessException(Message.CS_HAVE_RELATED_PORT_RESOURCE,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
		
		VPN vpn = vpnMapper.selectVPNBySubetId(id);
		if(null != vpn)
			throw new ResourceBusinessException(Message.CS_HAVE_RELATED_VPN_RESOURCE_DELETE_WITH_SUBNET,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
		
		return;
	}
	
	private void checkName(String name,TokenOs ostoken)  throws BusinessException{
		if(Util.isNullOrEmptyValue(name))
			return;
		List<Subnet> subnets = subnetMapper.selectListByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(subnets))
			return;
		for(Subnet subnet : subnets){
			if(name.equals(subnet.getName()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_NAME_IS_SAME,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		return;
	}
}
