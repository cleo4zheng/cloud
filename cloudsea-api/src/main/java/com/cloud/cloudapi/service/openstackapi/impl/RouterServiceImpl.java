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

import com.cloud.cloudapi.dao.common.FixedIPMapper;
import com.cloud.cloudapi.dao.common.FloatingIPMapper;
import com.cloud.cloudapi.dao.common.GatewayMapper;
import com.cloud.cloudapi.dao.common.NetworkMapper;
import com.cloud.cloudapi.dao.common.PortMapper;
import com.cloud.cloudapi.dao.common.QuotaDetailMapper;
import com.cloud.cloudapi.dao.common.QuotaMapper;
import com.cloud.cloudapi.dao.common.ResourceCreateProcessMapper;
import com.cloud.cloudapi.dao.common.RouterMapper;
import com.cloud.cloudapi.dao.common.SubnetMapper;
import com.cloud.cloudapi.dao.common.SyncResourceMapper;
import com.cloud.cloudapi.dao.common.TenantMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.RouterJSON;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FixedIP;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FloatingIP;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Gateway;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Network;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Port;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceCreateProcess;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Router;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Subnet;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SyncResource;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.PortService;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.service.openstackapi.RouterService;
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

@Service("routerService")
public class RouterServiceImpl implements RouterService {

	@Resource
	private OSHttpClientUtil client;

	@Autowired
	private RouterMapper routerMapper;

	@Autowired
	private GatewayMapper gatewayMapper;
	
	@Autowired
	private FixedIPMapper fixedIPMapper;
	
	@Autowired
	private SubnetMapper subnetMapper;
	
	@Autowired
	private NetworkMapper networkMapper;
	
	@Autowired
	private PortMapper portMapper;
	
	@Autowired
	private CloudConfig cloudconfig;

	@Autowired
	private QuotaMapper quotaMapper;

	@Autowired
	private FloatingIPMapper floatingipMapper;

	@Autowired
	private QuotaDetailMapper quotaDetailMapper;

	@Autowired
	private TenantMapper tenantMapper;

	@Autowired
	private SyncResourceMapper syncResourceMapper;
	
	@Autowired
	private ResourceCreateProcessMapper resourceCreateProcessMapper;
	
	@Resource
	private QuotaService quotaService;
	
	@Resource
	private AuthService authService;
	
	@Resource
	private PortService portService;
	
	private Logger log = LogManager.getLogger(RouterServiceImpl.class);
	
	public RouterMapper getRouterMapper() {
		return routerMapper;
	}

	public void setRouterMapper(RouterMapper routerMapper) {
		this.routerMapper = routerMapper;
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

	public GatewayMapper getGatewayMapper() {
		return gatewayMapper;
	}

	public void setGatewayMapper(GatewayMapper gatewayMapper) {
		this.gatewayMapper = gatewayMapper;
	}

	public FixedIPMapper getFixedIPMapper() {
		return fixedIPMapper;
	}

	public void setFixedIPMapper(FixedIPMapper fixedIPMapper) {
		this.fixedIPMapper = fixedIPMapper;
	}

	@Override
	public List<Router> getRouterList(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {

		int limitItems = Util.getLimit(paramMap);
		List<Router> routersFromDB = getRoutersFromDB(ostoken.getTenantid(),limitItems);
		if (!Util.isNullOrEmptyList(routersFromDB)){
			setRouterSubnetInfo(routersFromDB);
			return routersFromDB;
		}
			

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/routers", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
        Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);

		List<Router> routers = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));

		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				routers = getRouters(rs);
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
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				routers = getRouters(rs);
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
			throw new ResourceBusinessException(Message.CS_ROUTER_GET_FAILED,httpCode,locale);
		}

		storeRouters2DB(routers);
		routers = getLimitItems(routers, ostoken.getTenantid(),limitItems);
		return routers;
	}

	@Override
	public Router getRouter(String routerId, TokenOs ostoken) throws BusinessException {
		Router router = routerMapper.selectByPrimaryKey(routerId);
		if (null != router){
			setRouterInfo(router);
			return router;
		}
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/routers/");
		sb.append(routerId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				router = getRouter(rs);
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
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				router = getRouter(rs);
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
			throw new ResourceBusinessException(Message.CS_ROUTER_DETAIL_GET_FAILED,httpCode,locale);
		}

		storeRouter2DB(router);
		setRouterInfo(router);
		return router;
	}

	private void setRouterInfo(Router router){
		if(null == router)
			return;
		if(Util.isNullOrEmptyValue(router.getGatewayId()))
			router.setPublicGateway(false);
		else
			router.setPublicGateway(true);
		String subnetIds = router.getSubnetIds();
		if(!Util.isNullOrEmptyValue(subnetIds)){
			List<String> subnetIdArray = Util.stringToList(subnetIds, ",");
			List<Subnet> subnets = subnetMapper.selectListBySubnetIds(subnetIdArray);
			if(!Util.isNullOrEmptyList(subnets)){
				for(Subnet subnet : subnets){
					Network network = networkMapper.selectByPrimaryKey(subnet.getNetwork_id());
					subnet.setNetwork(network);
				}
			}
			router.setSubnets(subnets);	
			
		}
		String floatingIps = router.getFloatingIps();
		if(!Util.isNullOrEmptyValue(floatingIps)){
			List<String> floatingIpArray = Util.stringToList(floatingIps, ",");
			List<FloatingIP> floatingIPs = floatingipMapper.selectListByFloatingIps(floatingIpArray);
			router.setFloatingIPs(floatingIPs);
		}
	}
	
	@Override
	public Router createRouter(String createBody, TokenOs ostoken)throws BusinessException {
		
		Locale locale = new Locale(ostoken.getLocale());
		Router routerCreateInfo = makeRouterFromCreateBody(createBody,locale);
		routerCreateInfo.setName(StringHelper.string2Ncr(routerCreateInfo.getName()));
		checkName(routerCreateInfo.getName(),ostoken);
		String routerCreateBody = generateRouterBody(routerCreateInfo);
        
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/routers", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoPost(url, ostoken.getTokenid(), routerCreateBody);
		Util.checkResponseBody(rs,locale);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		
		Router router = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				router = getRouter(rs);
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
			rs = client.httpDoPost(url, tokenid, routerCreateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				router = getRouter(rs);
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
			throw new ResourceBusinessException(Message.CS_ROUTER_CREATE_FAILED,httpCode,locale);
		}

        router.setMillionSeconds(Util.getCurrentMillionsecond());
		storeRouter2DB(router);
		return router;
	}

	private Router updateRouterInfo(String routerId,String messageId,String routerUpdateBody, TokenOs ostoken) throws BusinessException{
		
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/routers/");
		sb.append(routerId);
		Locale locale = new Locale(ostoken.getLocale());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = null;
		if(messageId.equals(Message.CS_ROUTER_SET_GATEWAY_FAILED)){
			try{
				TokenOs adminToken = authService.createDefaultAdminOsToken();
				rs = client.httpDoPut(sb.toString(), adminToken.getTokenid(), StringHelper.string2Ncr(routerUpdateBody) );
			}catch(Exception e){
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
		}
		else{
			rs = client.httpDoPut(sb.toString(), ostoken.getTokenid(), routerUpdateBody);
		}
		Util.checkResponseBody(rs,locale);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		Router router = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				router = getRouter(rs);
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
			rs = client.httpDoPut(sb.toString(), tokenid, routerUpdateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				router = getRouter(rs);
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
			throw new ResourceBusinessException(messageId,locale);
		}
		
		return router;
	}
	
	@Override
	public void attachPort(String routerId,String updateBody,TokenOs ostoken) throws BusinessException{
		
		checkResource(routerId,ostoken);
		
		Locale locale = new Locale(ostoken.getLocale());
	    ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(updateBody);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error("error",e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		} 
		String portId =  rootNode.path(ResponseConstant.ID).textValue();
		checkResource(portId,ostoken);
		
		Port port = portMapper.selectByPrimaryKey(portId);
		if(null == port)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, ostoken.getCurrentRegion()).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/routers/");
		sb.append(routerId);
		sb.append("/add_router_interface");
		
		StringBuilder body = new StringBuilder();
		body.append("{\"");
		body.append(ParamConstant.PORT_ID);
		body.append("\":\"");
		body.append(portId);
		body.append("\"}");
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoPut(sb.toString(), headers, body.toString());
		Util.checkResponseBody(rs,locale);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
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
			rs = client.httpDoPut(sb.toString(), headers, body.toString());
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if (httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
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
//			throw new ResourceBusinessException(Message.CS_ROUTER_DELETE_FAILED,httpCode,locale);
		}
		
		port.setDevice_id(routerId);
		portMapper.insertOrUpdate(port);
		
		StringBuilder relatedResource = new StringBuilder();
		relatedResource.append(ParamConstant.ROUTER);
		relatedResource.append(":");
		relatedResource.append(routerId);
		updateSyncResourceInfo(ostoken.getTenantid(),portId,relatedResource.toString(),port.getStatus(),ParamConstant.ACTIVE_STATUS,ParamConstant.PORT,ostoken.getCurrentRegion(),port.getName());	
	    
	}
	
	@Override
	public void detachPort(String routerId,String portId,TokenOs ostoken) throws BusinessException{
	    checkResource(routerId,ostoken);
	    checkResource(portId,ostoken);
	    
		Locale locale = new Locale(ostoken.getLocale());

		Port port = portMapper.selectByPrimaryKey(portId);
		if(null == port)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
	   

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, ostoken.getCurrentRegion()).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/routers/");
		sb.append(routerId);
		sb.append("/remove_router_interface");
		
		StringBuilder body = new StringBuilder();
		body.append("{\"");
		body.append(ParamConstant.PORT_ID);
		body.append("\":\"");
		body.append(portId);
		body.append("\"}");
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoPut(sb.toString(), headers, body.toString());
		Util.checkResponseBody(rs,locale);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
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
			rs = client.httpDoPut(sb.toString(), headers, body.toString());
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if (httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
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
//			throw new ResourceBusinessException(Message.CS_ROUTER_DELETE_FAILED,httpCode,locale);
		}
		
		port.setDevice_id(null);
		portMapper.insertOrUpdate(port);
		
		StringBuilder relatedResource = new StringBuilder();
		relatedResource.append(ParamConstant.ROUTER);
		relatedResource.append(":");
		relatedResource.append(routerId);
		updateSyncResourceInfo(ostoken.getTenantid(),portId,relatedResource.toString(),port.getStatus(),ParamConstant.DOWN_STATUS,ParamConstant.PORT,ostoken.getCurrentRegion(),port.getName());	

	}
	
	@Override
	public Router updateRouter(String routerId,String updateBody, TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException{
		Router updateRouterInfo = getUpdateRouterInfo(updateBody);
		checkName(updateRouterInfo.getName(),ostoken);
		String routerUpdateBody = getUpdateBody(updateRouterInfo);
		
		Router router = updateRouterInfo(routerId,Message.CS_ROUTER_UPDATE_FAILED,routerUpdateBody,ostoken);
		storeRouter2DB(router);
		setRouterInfo(router);
		return router;
	}
	
	@Override
	public Router setExternalGateway(String routerId, String updateBody, TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException{
		checkResource(routerId,ostoken);
		checkExternalGateway(routerId,ostoken);
		Router updateRouterInfo = getUpdateRouterInfo(updateBody);
		String routerUpdateBody = getUpdateBody(updateRouterInfo);
		
		Router router = updateRouterInfo(routerId,Message.CS_ROUTER_SET_GATEWAY_FAILED,routerUpdateBody,ostoken);
	//	routerMapper.deleteByPrimaryKey(routerId);
		Router routerFromDB = routerMapper.selectByPrimaryKey(routerId);
		routerFromDB.setGatewayId(router.getGatewayId());
		Gateway gateway = router.getExternal_gateway_info();
		routerFromDB.setExternal_gateway_info(gateway);
		if(null != gateway){
			List<FixedIP> fixedIps = gateway.getExternal_fixed_ips();
			if(!Util.isNullOrEmptyList(fixedIps)){
				List<String> floatingIps = new ArrayList<String>();
				for(FixedIP fixedIp : fixedIps){
					floatingIps.add(fixedIp.getIp());
				}
				routerFromDB.setFloatingIps(Util.listToString(floatingIps, ','));
			}
		}
		storeRouter2DB(routerFromDB);    	
		//updateRelatedFloatingIPInfo(routerFromDB,true);
		updateSyncResourceInfo(ostoken.getTenantid(),routerId,"port:add",router.getStatus(),ParamConstant.ACTIVE_STATUS,ostoken.getCurrentRegion(),router.getName());

		return router;
	}
	
	private void updateSyncResourceInfo(String tenantId,String id,String relatedResource,String orgStatus,String expectedStatus,String region,String name){
		SyncResource resource = new SyncResource();
		resource.setId(id);
		resource.setType(ParamConstant.ROUTER);
		resource.setOrgStatus(orgStatus);
		resource.setExpectedStatus(expectedStatus);
		resource.setRegion(region);
		resource.setRelatedResource(relatedResource);
		syncResourceMapper.insertSelective(resource);
		
		updateResourceCreateProcessInfo(tenantId,id,ParamConstant.ROUTER,name);
	}
	
	private void updateSyncResourceInfo(String tenantId,String id,String relatedResource,String orgStatus,String expectedStatus,String type,String region,String name){
		SyncResource resource = new SyncResource();
		resource.setId(id);
		resource.setType(type);
		resource.setOrgStatus(orgStatus);
		resource.setExpectedStatus(expectedStatus);
		resource.setRelatedResource(relatedResource);
		resource.setRegion(region);
		syncResourceMapper.insertSelective(resource);
		
		updateResourceCreateProcessInfo(tenantId,id,type,name);
	}
	
	private void updateResourceCreateProcessInfo(String tenantId,String id,String type,String name){
		ResourceCreateProcess createProcess = new ResourceCreateProcess();
		createProcess.setId(id);
		createProcess.setTenantId(tenantId);
		createProcess.setType(type);
		createProcess.setName(name);
		createProcess.setBegineSeconds(Util.getCurrentMillionsecond());
		resourceCreateProcessMapper.insertOrUpdate(createProcess);
	}
	
	private void checkRelatedFloatingIP(Router router,TokenOs ostoken) throws BusinessException{
		if(null == router)
			return;
		Gateway gateway = gatewayMapper.selectByPrimaryKey(router.getGatewayId());
		if(null == gateway)
			return;
		String networkId = gateway.getNetwork_id();
		if(Util.isNullOrEmptyValue(networkId))
			return;
		List<FloatingIP> floatingIPs = floatingipMapper.selectListByTenantIdAndNetId(ostoken.getTenantid(),networkId);
		if(Util.isNullOrEmptyList(floatingIPs))
			return;
		for(FloatingIP floatingIP : floatingIPs){
			if(!Util.isNullOrEmptyValue(floatingIP.getInstanceId())){
			   throw new ResourceBusinessException(Message.CS_HAVE_RELATED_FLOATING_RESOURCE,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,new Locale(ostoken.getLocale()));
			}
		}
	}
	
	@Override
	public Router clearExternalGateway(String routerId, TokenOs ostoken) throws BusinessException{
		checkResource(routerId,ostoken);
		Router routerFromDB = routerMapper.selectByPrimaryKey(routerId);
		checkRelatedFloatingIP(routerFromDB,ostoken);
		
		Router router = new Router();
		Gateway externalGateway = new Gateway();
		router.setExternal_gateway_info(externalGateway);
		RouterJSON routerJSON= new RouterJSON(router);
		JsonHelper<RouterJSON, String> jsonHelp = new JsonHelper<RouterJSON, String>();
		String routerUpdateBody = jsonHelp.generateJsonBodySimple(routerJSON);
		router = updateRouterInfo(routerId,Message.CS_ROUTER_CLEAR_GATEWAY_FAILED,routerUpdateBody,ostoken);
		
		if(null != routerFromDB){
			String gatewayID = routerFromDB.getGatewayId();
			externalGateway = gatewayMapper.selectByPrimaryKey(gatewayID);
			if(null != externalGateway){
				String fixedIds = externalGateway.getFixedIds();
				if(!Util.isNullOrEmptyValue(fixedIds)){
					fixedIPMapper.deleteFixedIPsById(fixedIds.split(","));
				}
			}
			gatewayMapper.deleteByPrimaryKey(gatewayID);
			deleteGatewyPortInfo(routerFromDB);
			//updateRelatedFloatingIPInfo(routerFromDB,false);
			routerFromDB.setFloatingIps(null);
			routerFromDB.setGatewayId(null);
			storeRouter2DB(routerFromDB);
		}
		updateSyncResourceInfo(ostoken.getTenantid(),routerId,"port:remove",router.getStatus(),ParamConstant.ACTIVE_STATUS,ostoken.getCurrentRegion(),router.getName());
		
		return router;
	}
	
	private void deleteGatewyPortInfo(Router router){
		portMapper.deleteByIPAndDeviceId(router.getFloatingIps(),router.getId());		
	}
	
	@Override
	public void deleteRouter(String routerId, TokenOs ostoken) throws BusinessException {
		checkResource(routerId,ostoken);
		Locale locale = new Locale(ostoken.getLocale());
		checkRelatedResource(routerId,locale);
		
		//removeInterface(routerId,ostoken);
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/routers/");
		sb.append(routerId);
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoDelete(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
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
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
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
			throw new ResourceBusinessException(Message.CS_ROUTER_DELETE_FAILED,httpCode,locale);
		}
		
		routerMapper.deleteByPrimaryKey(routerId);
	//	updateRelatedResourceInfo(routerId);
	//	updateRouterQuota(ostoken,false);
	}

	@Override
	public Router addInterfaceToRouter(String routerId,String body, TokenOs ostoken) throws BusinessException{

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/routers/");
		sb.append(routerId);
		sb.append("/add_router_interface");

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoPut(sb.toString(), ostoken.getTokenid(),body);
		Util.checkResponseBody(rs,locale);
	
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		Router router = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				router = getRouterInfo(rootNode);
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
			rs = client.httpDoPut(sb.toString(), tokenid, body);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				router = getRouterInfo(rootNode);
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
			throw new ResourceBusinessException(Message.CS_ROUTER_ADD_INTERFACE_FAILED,httpCode,locale);
		}
		
		Router updatedRouter = getUpdatedRouterInfo(router,ostoken);
		
		StringBuilder relatedResource = new StringBuilder();
		relatedResource.append(ParamConstant.ROUTER);
		relatedResource.append(":");
		relatedResource.append(router.getId());
		relatedResource.append(";");
		relatedResource.append(ParamConstant.TENANT);
		relatedResource.append(":");
		relatedResource.append(updatedRouter.getTenant_id());
		relatedResource.append(";");
		relatedResource.append(ParamConstant.NAME);
		relatedResource.append(":");
		relatedResource.append(updatedRouter.getName());
		
		updateSyncResourceInfo(ostoken.getTenantid(),router.getPortIds(),relatedResource.toString(),null,ParamConstant.ACTIVE_STATUS,ParamConstant.PORT,ostoken.getCurrentRegion(),updatedRouter.getName()+"_port");	

		storeRouter2DB(updatedRouter);
		return updatedRouter;
	}
	
	@Override
	public Router removeInterfaceFromRouter(String routerId,String subnetId, TokenOs ostoken) throws BusinessException{

		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/routers/");
		sb.append(routerId);
		sb.append("/remove_router_interface");

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		StringBuilder sbRemoveBody = new StringBuilder();
		sbRemoveBody.append("{\"");
		sbRemoveBody.append(ParamConstant.SUBNET_ID);
		sbRemoveBody.append("\":\"");
		sbRemoveBody.append(subnetId);
		sbRemoveBody.append("\"}");
		Locale locale = new Locale(ostoken.getLocale());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoPut(sb.toString(), ostoken.getTokenid(),sbRemoveBody.toString());
		Util.checkResponseBody(rs,locale);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		Router router = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				router = getRouterInfo(rootNode);
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
			rs = client.httpDoPut(sb.toString(), tokenid, sbRemoveBody.toString());
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				router = getRouterInfo(rootNode);
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
			throw new ResourceBusinessException(Message.CS_ROUTER_REMOVE_INTERFACE_FAILED,httpCode,locale);
		}
		
	    Router routerFromDB = routerMapper.selectByPrimaryKey(routerId);
	    if(null != routerFromDB){
	    	portMapper.deleteByPrimaryKey(router.getPortIds());
	    	routerFromDB.setSubnet_id(null);
	    	routerFromDB.setSubnetIds(Util.listToString(Util.getCorrectedIdInfo(routerFromDB.getSubnetIds(), subnetId), ','));
	    	routerFromDB.setPortIds(Util.listToString(Util.getCorrectedIdInfo(routerFromDB.getPortIds(), router.getPortIds()), ','));
	    	routerMapper.updateByPrimaryKeySelective(routerFromDB);
	    }
		return router;
	}
	
	private List<Router> getRoutersFromDB(String tenantId,int limitItems) {
		List<Router> routersFromDB = null;
		if (-1 == limitItems) {
			routersFromDB = routerMapper.selectAllByTenantId(tenantId);
		} else {
			routersFromDB = routerMapper.selectAllByTenantIdWithLimit(tenantId,limitItems);
		}
		return routersFromDB;
	}

	private void storeRouter2DB(Router router) {
		if (null == router)
			return;
		routerMapper.insertOrUpdate(router);
		
		Gateway gateway = router.getExternal_gateway_info();
		if (null != gateway) {
			if (null != gatewayMapper.selectByPrimaryKey(gateway.getId()))
				gatewayMapper.updateByPrimaryKeySelective(gateway);
			else
				gatewayMapper.insertSelective(gateway);

			List<FixedIP> fixedIps = gateway.getExternal_fixed_ips();
			if (!Util.isNullOrEmptyList(fixedIps)) {
               for(FixedIP fixedip : fixedIps){
            		if (null != fixedIPMapper.selectByPrimaryKey(fixedip.getId()))
            			fixedIPMapper.updateByPrimaryKeySelective(fixedip);
        			else
        				fixedIPMapper.insertSelective(fixedip);
               }
			}
		}
	}

	private List<Router> storeRouters2DB(List<Router> routers) {
		if (Util.isNullOrEmptyList(routers))
			return null;
		for (Router router : routers) {
			storeRouter2DB(router);
		}
		return null;
	}

	private List<Router> getRouters(Map<String, String> rs)
			throws JsonProcessingException, IOException, BusinessException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode routersNode = rootNode.path(ResponseConstant.ROUTERS);
		int routersCount = routersNode.size();
		if (0 == routersCount)
			return null;

		List<Router> routers = new ArrayList<Router>();
		for (int index = 0; index < routersCount; ++index) {
			Router router = getRouterInfo(routersNode.get(index));
			routers.add(router);
		}

		return routers;
	}

	private Router getRouter(Map<String, String> rs) throws JsonProcessingException, IOException, BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode routerNode = rootNode.path(ResponseConstant.ROUTER);
		return getRouterInfo(routerNode);
	}
	
	
	private Router getRouterInfo(JsonNode routerNode) throws BusinessException {
		if (null == routerNode)
			return null;
		Router router = new Router();
		router.setId(routerNode.path(ResponseConstant.ID).textValue());
		router.setName(routerNode.path(ResponseConstant.NAME).textValue());
		router.setStatus(routerNode.path(ResponseConstant.STATUS).textValue());
		router.setAdmin_state_up(routerNode.path(ResponseConstant.ADMIN_STATE_UP).booleanValue());
		router.setHa(routerNode.path(ResponseConstant.HA).booleanValue());
		router.setDistributed(routerNode.path(ResponseConstant.DISTRIBUTED).booleanValue());
		router.setTenant_id(routerNode.path(ResponseConstant.TENANT_ID).textValue());
		router.setExternal_gateway_info(getGateway(routerNode.path(ResponseConstant.EXTERNAL_GATEWAY_INFO)));
		router.setPortIds(routerNode.path(ResponseConstant.PORT_ID).textValue());
//		router.makeSunetAndIpInfo();
        router.setSubnet_id(routerNode.path(ResponseConstant.SUBNET_ID).textValue());
        router.setMillionSeconds(Util.time2Millionsecond(Util.getCurrentDate(), ParamConstant.TIME_FORMAT_01));
       // router.setCreatedAt(Util.getCurrentDate()); //may change it later
        JsonNode subnetIdsNode = routerNode.path(ResponseConstant.SUBNET_IDS);
        if(null != subnetIdsNode && !subnetIdsNode.isMissingNode()){
        	int subnetsCount = subnetIdsNode.size();
        	List<String> subnetIds = new ArrayList<String>();
        	for(int index = 0 ; index < subnetsCount; ++index){
        		subnetIds.add(subnetIdsNode.get(index).textValue());
			}
        	router.setSubnet_ids(subnetIds);
        	router.setSubnetIds(Util.listToString(subnetIds, ','));
        }
		return router;
	}

	private Gateway getGateway(JsonNode gatewayNode){
		if(null == gatewayNode)
			return null;
		Gateway gateway = new Gateway();
		gateway.setId(Util.makeUUID());
		gateway.setNetwork_id(gatewayNode.path(ResponseConstant.NETWORK_ID).textValue());
		if(Util.isNullOrEmptyValue(gateway.getNetwork_id()))
			return null;
		gateway.setEnable_snat(gatewayNode.path(ResponseConstant.ENABLE_SNAT).booleanValue());
		gateway.setExternal_fixed_ips(getFixedIP(gatewayNode.path(ResponseConstant.EXTERNAL_FIXED_IPS)));
		gateway.makeFixedIPs();
		return gateway;
	}
	
	private List<FixedIP> getFixedIP(JsonNode fixedIpNode){
		if(null == fixedIpNode)
			return null;
		int fixedIpSize = fixedIpNode.size();
		List<FixedIP> fixedIPs = new ArrayList<FixedIP>();
		for(int index = 0; index < fixedIpSize; ++index){
			FixedIP fixedIp = new FixedIP();
			fixedIp.setId(Util.makeUUID());
			fixedIp.setSubnet_id(fixedIpNode.get(index).path(ResponseConstant.SUBNET_ID).textValue());
			fixedIp.setIp(fixedIpNode.get(index).path(ResponseConstant.IP_ADDRESS).textValue());
			fixedIPs.add(fixedIp);
		}
		return fixedIPs;
	}
	
	private List<Router> getLimitItems(List<Router> routers, String tenantId,int limit) {
		if(Util.isNullOrEmptyList(routers))
			return null;
		List<Router> tenantRouters = new ArrayList<Router>();
		for(Router router : routers){
			if(!tenantId.equals(router.getTenant_id()))
				continue;
			tenantRouters.add(router);
		}
		setRouterSubnetInfo(tenantRouters);
		if(-1 != limit){
			if(limit <= tenantRouters.size())
				return tenantRouters.subList(0, limit);
		}
		return tenantRouters;
	}
	
	private Router getUpdatedRouterInfo(Router router,TokenOs ostoken) throws BusinessException{
		Router updatedRouter = routerMapper.selectByPrimaryKey(router.getId());
		if(null == updatedRouter)
			return router;
	//	String attachedPortId = router.getPortIds();
		
		updatedRouter.setSubnet_id(router.getSubnet_id());
		updatedRouter.setSubnetIds(Util.getAppendedIds(updatedRouter.getSubnetIds(), router.getSubnet_ids()));
		updatedRouter.setSubnet_name(getSubnetName(router.getSubnet_id()));
		List<String> portIds = new ArrayList<String>();
		portIds.add(router.getPortIds());
		updatedRouter.setPortIds(Util.getAppendedIds(updatedRouter.getPortIds(), portIds));
		List<Subnet> subnets = subnetMapper.selectListBySubnetIds(Util.stringToList(updatedRouter.getSubnetIds(), ","));
		List<Subnet> subnetsWithNetwork = new ArrayList<Subnet>();
		if(!Util.isNullOrEmptyList(subnets)){
			for(Subnet subnet : subnets){
				subnet.setNetwork(networkMapper.selectByPrimaryKey(subnet.getNetwork_id()));
				subnetsWithNetwork.add(subnet);
			}
		}
		updatedRouter.setSubnets(subnetsWithNetwork);
		
		/* 0222
		if(null == portMapper.selectByPrimaryKey(attachedPortId)){
		   Port attachedPort = 	portService.getPort(attachedPortId, ostoken, false);
		   attachedPort.setName(String.format("%s_port_%s", updatedRouter.getName(),subnets.size()));
		   portMapper.updateByPrimaryKeySelective(attachedPort);
		}*/
		
//		updatedRouter.setSubnet_names(getSubnetNames(router.getSubnetIds()));
//		updatedRouter.setFixedIps(getPortIPs(updatedRouter.getPortIds(),subnetsWithNetwork.size(),ostoken));
		
		return updatedRouter;
	}
	
	private String getSubnetName(String subnetId){
		Subnet subnet = subnetMapper.selectByPrimaryKey(subnetId);
		if(null == subnet)
			return null;
		return subnet.getName();
	}
	
//	private String getSubnetNames(String subnetId){
//		if(Util.isNullOrEmptyValue(subnetId))
//			return null;
//		List<String> subnetIds = Util.stringToList(subnetId, ",");
//		List<Subnet> subnets = subnetMapper.selectListBySubnetIds(subnetIds);
//		if(Util.isNullOrEmptyList(subnets))
//			return null;
//		List<String> subnetNames = new ArrayList<String>();
//		for(Subnet subnet : subnets){
//			subnetNames.add(subnet.getName());
//		}
//		return Util.listToString(subnetNames, ',');
//	}
	
	/*
	private List<String> getPortIPs(String portId,int size,TokenOs ostoken){
		if(Util.isNullOrEmptyValue(portId))
			return null;
		List<String> fixedIps = portMapper.selectIpOfPortsById(portId.split(","));
		if(Util.isNullOrEmptyList(fixedIps) || fixedIps.size() != size){
			try{
				portService.getPortList(null, ostoken, false);
				fixedIps = portMapper.selectIpOfPortsById(portId.split(","));
			}catch(Exception e){
				//
			}
			if(null == fixedIps)
				return null;
		}
		return fixedIps;
	}*/
	
	private Router makeRouterFromCreateBody(String createBody,Locale locale)
			throws ResourceBusinessException {
		ObjectMapper mapper = new ObjectMapper();
		Router routerInfo = null;
		try {
			routerInfo = mapper.readValue(createBody, Router.class);
		} catch (Exception e) {
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.NOT_FOUND_RESPONSE_CODE,locale);
		}

		return routerInfo;
	}
	
	private String generateRouterBody(Router routerInfo) {

		RouterJSON routerJSON = new RouterJSON(routerInfo);
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.setSerializationInclusion(Include.NON_EMPTY);
		String jsonStr = "";
		try {
			jsonStr = mapper.writeValueAsString(routerJSON);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
		return jsonStr;
	}
	
	private Router getUpdateRouterInfo(String updateBody) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(updateBody);
		String networkId = null;
		String name = null;
		String subnetId = null;
		String ip = null;
		if(!rootNode.path(ResponseConstant.NETWORK_ID).isMissingNode())
		   networkId = rootNode.path(ResponseConstant.NETWORK_ID).textValue();
		if(!rootNode.path(ResponseConstant.NAME).isMissingNode())
		   name = rootNode.path(ResponseConstant.NAME).textValue();
		if(!rootNode.path(ResponseConstant.SUBNET_ID).isMissingNode())
		   subnetId = rootNode.path(ResponseConstant.SUBNET_ID).textValue();
		if(!rootNode.path(ResponseConstant.IP).isMissingNode())
		   ip =rootNode.path(ResponseConstant.IP).textValue();
		FixedIP fixedIp = null;
		List<FixedIP> fixedIps = null;
		if(null != subnetId){
			fixedIp = new FixedIP();
			fixedIp.setIp(ip);
			fixedIp.setSubnet_id(subnetId);
			fixedIps = new ArrayList<FixedIP>();
			fixedIps.add(fixedIp);
		}
		
		Gateway gateway = null;
		if(null != networkId){
			gateway = new Gateway();	
			gateway.setNetwork_id(networkId);
			gateway.setEnable_snat(true);
			gateway.setExternal_fixed_ips(fixedIps);
		}
		Router router = new Router();
		router.setExternal_gateway_info(gateway);
		router.setName(name);
		return router;
	}
	
	private String getUpdateBody(Router router){
		RouterJSON routerJSON= new RouterJSON(router);
		JsonHelper<RouterJSON, String> jsonHelp = new JsonHelper<RouterJSON, String>();
		return jsonHelp.generateJsonBodySimple(routerJSON);
	}
	
	/*
	private void removeInterface(String routerId, TokenOs ostoken) throws ResourceBusinessException{
		Router router = routerMapper.selectByPrimaryKey(routerId);
		if(null != router){
			List<String> subnetIds = Util.stringToList(router.getSubnetIds(),",");
			if(null != subnetIds){
				for(String subnetId : subnetIds){
					try{
						removeInterfaceFromRouter(routerId, subnetId, ostoken);	
					}catch (Exception e){
						log.error(e);
						throw new ResourceBusinessException(Message.CS_ROUTER_DELETE_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
					}	
				}
			}
		}
	}*/
	
	private void setRouterSubnetInfo(List<Router> routersFromDB){
		for(Router router : routersFromDB){
			if(Util.isNullOrEmptyValue(router.getGatewayId()))
				router.setPublicGateway(false);
			else
				router.setPublicGateway(true);
			List<String> subnetIds = Util.stringToList(router.getSubnetIds(),",");
			if(Util.isNullOrEmptyList(subnetIds)){
				router.setSubnets(new ArrayList<Subnet>());
				continue;
			}
			router.setSubnets(subnetMapper.selectListBySubnetIds(subnetIds));
		}
	}
	
	/*
	private void updateRelatedResourceInfo(String routerId){
		List<Port> ports = portMapper.selectByDeviceId(routerId);
		if(Util.isNullOrEmptyList(ports))
			return;
		for(Port port : ports){
			portMapper.deleteByPrimaryKey(port.getId());
		}
	}*/
	
	private void checkRelatedResource(String id,Locale locale) throws BusinessException{
		Router routerFromDB = routerMapper.selectByPrimaryKey(id);
		if(null == routerFromDB)
			return;
		if(!Util.isNullOrEmptyValue(routerFromDB.getFirewallId()))
			throw new ResourceBusinessException(Message.CS_HAVE_RELATED_FIREWALL_RESOURCE,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
        if(!Util.isNullOrEmptyValue(routerFromDB.getVpnId()))
        	throw new ResourceBusinessException(Message.CS_HAVE_RELATED_VPN_RESOURCE,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
        if(!Util.isNullOrEmptyValue(routerFromDB.getGatewayId()))
        	throw new ResourceBusinessException(Message.CS_HAVE_RELATED_GATEWAY_RESOURCE,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
        if(!Util.isNullOrEmptyValue(routerFromDB.getPortIds()))
        	throw new ResourceBusinessException(Message.CS_HAVE_RELATED_PORT_RESOURCE_WITH_ROUTER,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);

        return;
	}
	
	private void checkExternalGateway(String id,TokenOs ostoken)  throws BusinessException{
		Router router = routerMapper.selectByPrimaryKey(id);
		if(null == router)
			return;
		if(!Util.isNullOrEmptyValue(router.getGatewayId()))
        	throw new ResourceBusinessException(Message.CS_HAVE_RELATED_GATEWAY_RESOURCE_WITH_ROUTER,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		return;
	}
	
	private void checkName(String name,TokenOs ostoken)  throws BusinessException{
		if(Util.isNullOrEmptyValue(name))
			return;
		List<Router> routers = routerMapper.selectAllByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(routers))
			return;
		for(Router router : routers){
			if(name.equals(router.getName()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_NAME_IS_SAME,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		return;
	}
	
	private void checkResource(String id, TokenOs ostoken) throws BusinessException {
		SyncResource syncResource = syncResourceMapper.selectByPrimaryKey(id);
		if (null != syncResource)
			throw new ResourceBusinessException(Message.CS_RESOURCE_IS_DOING,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE, new Locale(ostoken.getLocale()));
		return;
	}
}
