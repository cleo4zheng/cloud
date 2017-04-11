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

import com.cloud.cloudapi.dao.common.FloatingIPMapper;
import com.cloud.cloudapi.dao.common.InstanceMapper;
import com.cloud.cloudapi.dao.common.LoadbalancerMapper;
import com.cloud.cloudapi.dao.common.NetworkMapper;
import com.cloud.cloudapi.dao.common.PortMapper;
import com.cloud.cloudapi.dao.common.QuotaDetailMapper;
import com.cloud.cloudapi.dao.common.QuotaMapper;
import com.cloud.cloudapi.dao.common.ResourceCreateProcessMapper;
import com.cloud.cloudapi.dao.common.RouterMapper;
import com.cloud.cloudapi.dao.common.SecurityGroupMapper;
import com.cloud.cloudapi.dao.common.SubnetMapper;
import com.cloud.cloudapi.dao.common.SyncResourceMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.PortJSON;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FixedIP;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FloatingIP;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Loadbalancer;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Network;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Port;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceCreateProcess;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Router;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SecurityGroup;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Subnet;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SyncResource;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.NetworkService;
import com.cloud.cloudapi.service.openstackapi.PortService;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.service.openstackapi.SecurityGroupService;
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

@Service("portService")
public class PortServiceImpl implements PortService{
	
	@Resource
	private OSHttpClientUtil client;
	
	@Resource
	private PortMapper portMapper;

	@Resource
	private SubnetMapper subnetMapper;
	
	@Resource
	private NetworkMapper networkMapper;
	
	@Resource
	private InstanceMapper instanceMapper;
	
	@Resource
	private LoadbalancerMapper loadbalancerMapper;
	
	@Resource
	private RouterMapper routerMapper;
	
	@Resource
	private FloatingIPMapper floatingipMapper;

	@Resource
	private SecurityGroupMapper securityGroupMapper;

	@Autowired
	private SyncResourceMapper syncResourceMapper;
	
	@Autowired
	private ResourceCreateProcessMapper resourceCreateProcessMapper;
	
	@Resource
	private CloudConfig cloudconfig;

	@Resource
	private QuotaMapper quotaMapper;
	
	@Resource
	private QuotaDetailMapper quotaDetailMapper;
	
	@Resource
	private QuotaService quotaService;
	
	@Resource
	private NetworkService networkService;
	
	@Resource
	private SubnetService subnetService;
	
	@Resource
	private AuthService authService;
	
	@Resource
	private SecurityGroupService securityGroupService;
	
	private Logger log = LogManager.getLogger(PortServiceImpl.class);
	
	@Override
	public List<Port> getPortList(Map<String,String> paramMap,TokenOs ostoken,Boolean bFromDB) throws BusinessException
	{
		int limitItems = Util.getLimit(paramMap);
		List<Port> portsFromDB = null;
		if(true == bFromDB){
			portsFromDB = getPortsFromDB(ostoken,limitItems);
			if(!Util.isNullOrEmptyList(portsFromDB)){
				return portsFromDB;	
			}	
		}
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		List<Port> ports = refreshPorts(paramMap,ostoken);
	//	ports = storePorts2DB(ports,ostoken,response);
		return getLimitItems(ports,ostoken.getTenantid(),limitItems);
	}
	
	
	@Override
	public List<Port> refreshPorts(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException{
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		if(null == ostoken){
			try {
				ostoken = authService.createDefaultAdminOsToken();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			}
		}
		
		String region = ostoken.getCurrentRegion(); // we should get the regioninfo by the guiTokenId
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/v2.0/ports", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
        Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(url, ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
        List<Port> ports = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				ports = getPorts(rs);
			}  catch (Exception e) {
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
            if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
            	throw new ResourceBusinessException(Message.CS_NETWORK_PORT_GET_FAILED,httpCode,locale);
			try {
				ports = getPorts(rs);
			}  catch (Exception e) {
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
			throw new ResourceBusinessException(Message.CS_NETWORK_PORT_GET_FAILED,httpCode,locale);
		}
		
		return storePorts2DB(ports,ostoken);
	}
	
	@Override
	public Port getPort(String portId,TokenOs ostoken,Boolean bFromDB) throws BusinessException{
		Port port = null;
		if(true == bFromDB)
			port = portMapper.selectByPrimaryKey(portId);
		if(null != port){
		//	setSubnetInfo2Port(port,ostoken, response);
			setPortInfo(port,true,ostoken);
			return port;
		}
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		String region = ostoken.getCurrentRegion(); // we should get the regioninfo by the guiTokenId
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/ports/");
		sb.append(portId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				port = getPort(rs);
			}  catch (Exception e) {
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
				throw new ResourceBusinessException(Message.CS_NETWORK_PORT_DETAIL_GET_FAILED,httpCode,locale);
			try {
				port = getPort(rs);
			}  catch (Exception e) {
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
			throw new ResourceBusinessException(Message.CS_NETWORK_PORT_DETAIL_GET_FAILED,httpCode,locale);
		}
		
		setPortInfo(port,false,ostoken);
		storePort2DB(port);
		return port;
	}
	
	
	@Override
	public List<Port> getBindingPortsInfo(String routerId,TokenOs ostoken) throws BusinessException{
	
		String region = ostoken.getCurrentRegion(); // we should get the regioninfo by the guiTokenId
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/ports/?device_id=");
		sb.append(routerId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		List<Port> ports = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				ports = getPorts(rs);
			}  catch (Exception e) {
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
				throw new ResourceBusinessException(Message.CS_NETWORK_PORT_DETAIL_GET_FAILED,httpCode,locale);
			try {
				ports = getPorts(rs);
			}  catch (Exception e) {
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
			throw new ResourceBusinessException(Message.CS_NETWORK_PORT_DETAIL_GET_FAILED,httpCode,locale);
		}
		
		setPortsInfo(ports,false,ostoken);
		storePorts2DB(ports,ostoken);
		return ports;
	}
	
	private void setPortsInfo(List<Port> ports,Boolean getSecurityInfo,TokenOs ostoken) throws BusinessException{
		if(Util.isNullOrEmptyList(ports))
				return;
		for(Port port : ports){
			setPortInfo(port,getSecurityInfo,ostoken);
		}
	}
	
	private void setPortInfo(Port port,Boolean getSecurityInfo,TokenOs ostoken) throws BusinessException{
	    if(null == port)
	    	return;
	    setSubnetInfo2Port(port,ostoken);
	    setRelateResourceInfo2Port(port);
	    if(false == getSecurityInfo)
	    	return;
	    
	    String securityGroupIds = port.getSecurityGroupId();
	    if(!Util.isNullOrEmptyValue(securityGroupIds)){
	    	String[] securtyGroupIDArray = securityGroupIds.split(",");
	    	List<SecurityGroup> securityGroups = securityGroupMapper.selectSecurityGroupsById(securtyGroupIDArray);
	    	port.setSecurityGroups(securityGroups);
	    	
//	    	for(int index = 0; index < securtyGroupIDArray.length; ++index){
//	    		SecurityGroup securityGroup = securityGroupMapper.selectByPrimaryKey(securtyGroupIDArray[index]);
//	    		if(null == securityGroup){
//	    			securityGroup = securityGroupService.getSecurityGroup(securtyGroupIDArray[index], ostoken);
//	    		}
//	    		securityGroups.add(securityGroup);
//	    	}
//	    	port.setSecurityGroups(securityGroups);
	    }
	}
	
	@Override
	public Port createPort(String createBody, TokenOs ostoken) throws BusinessException, IOException{
		// token should have Regioninfo
		Locale locale = new Locale(ostoken.getLocale());
		String portCreateBody = null;
		portCreateBody = getCreatedPortBody(createBody,ostoken,locale);		
		String region = ostoken.getCurrentRegion(); // we should get the regioninfo by the guiTokenId
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url+"/v2.0/ports", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoPost(url, headers,portCreateBody);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		Port port = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE: {
			try {
				port = getPort(rs);
			}  catch (Exception e) {
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
			rs = client.httpDoPost(url, headers,portCreateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_NETWORK_PORT_CREATE_FAILED,httpCode,locale);
			try {
				port = getPort(rs);
			}  catch (Exception e) {
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
			throw new ResourceBusinessException(Message.CS_NETWORK_PORT_CREATE_FAILED,httpCode,locale);
		}
		
		//update network's portid info
		port.setMillionSeconds(Util.getCurrentMillionsecond());
		updateNetworkPortInfo(port,ostoken);
		storePort2DB(port);
		
		updateSyncResourceInfo(ostoken.getTenantid(),port.getId(),null,null,ParamConstant.DOWN_STATUS,ParamConstant.PORT,ostoken.getCurrentRegion(),port.getName());	
		return port;
	}
	
	@Override
	public Port addSecurityGroup(String securityGroupId,String portId,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(portId,locale,false);
		
		Port port = portMapper.selectByPrimaryKey(portId);
		if(null == port)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		
        List<String> securityGroupIds = Util.stringToList(Util.getAppendedIds(port.getSecurityGroupId(), securityGroupId), ",");
		
        Port updatePort = new Port();
        updatePort.setSecurity_groups(securityGroupIds);
        updatePort.setSubnet(null);
        PortJSON portJSON = new PortJSON(updatePort);
		JsonHelper<PortJSON, String> jsonHelp = new JsonHelper<PortJSON, String>();
		String updatePortBody = jsonHelp.generateJsonBodySimple(portJSON);
		port = updatePortInfo(portId,updatePortBody,ostoken);
		if(null != port){
			port.setSecurityGroupId(Util.listToString(securityGroupIds, ','));
			portMapper.insertOrUpdate(port);	
		}
		
		return port;
	}
	
	@Override
	public Port removeSecurityGroup(String securityGroupId,String portId,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(portId,locale,false);
		
		Port port = portMapper.selectByPrimaryKey(portId);
		if(null == port)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		
        List<String> securityGroupIds = Util.getCorrectedIdInfo(port.getSecurityGroupId(), securityGroupId);
		
        Port updatePort = new Port();
        updatePort.setSecurity_groups(securityGroupIds);
        updatePort.setSubnet(null);
        PortJSON portJSON = new PortJSON(updatePort);
		JsonHelper<PortJSON, String> jsonHelp = new JsonHelper<PortJSON, String>();
		String updatePortBody = jsonHelp.generateJsonBodySimple(portJSON);
		port = updatePortInfo(portId,updatePortBody,ostoken);
		if(null != port){
			port.setSecurityGroupId(Util.listToString(securityGroupIds,','));
			portMapper.insertOrUpdate(updatePort);
		}
		
		return port;
	}
	
	@Override
	public Port updatePort(String portId,String updateBody,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(portId,locale,false);
		
		String portUpdateBody = getUpdateBody(updateBody,ostoken);
		Port port = updatePortInfo(portId,portUpdateBody,ostoken);
	//	port.setCreatedAt(Util.millionSecond2Date(port.getMillionSeconds()));
		storePort2DB(port);
		return port;
	}
	
	
	private Port updatePortInfo(String portId,String updateBody,TokenOs ostoken)  throws BusinessException{
		
		Locale locale = new Locale(ostoken.getLocale());
		
		String region = ostoken.getCurrentRegion(); // we should get the regioninfo by the guiTokenId
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/ports/");
		sb.append(portId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoPut(sb.toString(), ostoken.getTokenid(),updateBody);
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		Port port = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				port = getPort(rs);
			}  catch (Exception e) {
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
			rs = client.httpDoPut(sb.toString(), ostoken.getTokenid(),updateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_NETWORK_PORT_UPDATE_FAILED,httpCode,locale);
			try {
				port = getPort(rs);
			}  catch (Exception e) {
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
			throw new ResourceBusinessException(Message.CS_NETWORK_PORT_UPDATE_FAILED,httpCode,locale);
		}
		return port;
	}
	
	@Override
	public void deletePort(String portId,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(portId,locale,true);
		
		String region = ostoken.getCurrentRegion(); // we should get the regioninfo by the guiTokenId
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/v2.0/ports/");
		sb.append(portId);

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
			rs =  client.httpDoDelete(sb.toString(), ostoken.getTokenid());
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
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
			throw new ResourceBusinessException(Message.CS_NETWORK_PORT_DELETE_FAILED,httpCode,locale);
		}
		
		updatePortDBInfo(portId);
	//	updatePortQuota(ostoken,false);
	}
	
	private void updatePortDBInfo(String portId){
		FloatingIP floatingIP = floatingipMapper.selectByPortId(portId);
		if(null != floatingIP){
			floatingIP.setPort_id(null);
			try{
				floatingipMapper.updateByPrimaryKeySelective(floatingIP);	
			}catch (Exception e){
				//TODO
			}	
		}
		
		List<Network> networks = networkMapper.selectListByPortId(portId);
		if(!Util.isNullOrEmptyList(networks)){
			for(Network network : networks){
				List<String> portIds = Util.getCorrectedIdInfo(network.getPortId(), portId);
				network.setPortId(Util.listToString(portIds, ','));
				try{
					networkMapper.updateByPrimaryKeySelective(network);	
				}catch(Exception e){
					//TODO
				}	
			}
		}
		
		try{
			portMapper.deleteByPrimaryKey(portId);
		}catch(Exception e){
			//TODO
		}
	}
	
	private String getUpdateBody(String updateBody,TokenOs ostoken) throws BusinessException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(updateBody);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error("error",e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		} 
		Port port = new Port();
		if (true != rootNode.path(ResponseConstant.NAME).isMissingNode())
			port.setName(StringHelper.string2Ncr(rootNode.path(ResponseConstant.NAME).textValue()));
		if(true != rootNode.path(ResponseConstant.ADMIN_STATE_UP).isMissingNode())
			port.setAdmin_state_up(rootNode.path(ResponseConstant.ADMIN_STATE_UP).booleanValue());
		if(true != rootNode.path(ResponseConstant.DEVICE_OWNER).isMissingNode())
			port.setDevice_owner(rootNode.path(ResponseConstant.DEVICE_OWNER).textValue());

		checkName(port.getName(),ostoken);
		
		if(true != rootNode.path(ResponseConstant.SECURITY_GROUP).isMissingNode()){
			List<String> securityGroups = new ArrayList<String>();
			securityGroups.add(rootNode.path(ResponseConstant.SECURITY_GROUP).textValue());
			port.setSecurity_groups(securityGroups);
		}
		
		//TODO change
		String ip = null;
		String subnetId = null;
		if(true != rootNode.path(ResponseConstant.IP).isMissingNode())
		   ip = rootNode.path(ResponseConstant.IP).textValue();
		if(true != rootNode.path(ResponseConstant.SUBNET).isMissingNode())
		   subnetId = rootNode.path(ResponseConstant.SUBNET).textValue();
		if(null != ip || null != subnetId){
			FixedIP fixedIP = new FixedIP();
			fixedIP.setIp_address(ip);
			fixedIP.setSubnet_id(subnetId);
			List<FixedIP> fixedIPs = new ArrayList<FixedIP>();
			fixedIPs.add(fixedIP);
			port.setFixed_ips(fixedIPs);
		}
		port.setSubnet(null);
		PortJSON portJSON = new PortJSON(port);
		JsonHelper<PortJSON, String> jsonHelp = new JsonHelper<PortJSON, String>();
		return jsonHelp.generateJsonBodySimple(portJSON);
	}
	
//	private void updatePortQuota(TokenOs ostoken,boolean bAdd){
//		quotaService.updateQuota(ParamConstant.PORT,ostoken,bAdd,1);
//	}
	
	private void updateNetworkPortInfo(Port port,TokenOs ostoken) throws BusinessException{
		Network network = networkMapper.selectByPrimaryKey(port.getNetwork_id());
		if(null == network){
//			NetworkServiceImpl networkService = new NetworkServiceImpl(); 
//			networkService.setCloudconfig(cloudconfig);
//			networkService.setNetworkMapper(networkMapper);
//			networkService.setSubnetMapper(subnetMapper);
			network = networkService.getNetwork(port.getNetwork_id(),ostoken);
		}
		if(null != network){
			String portId = Util.getIdWithAppendId(port.getId(),network.getPortId());
			network.setPortId(portId);
			networkMapper.updateByPrimaryKeySelective(network);	
		}
	}
	
	private void setSubnetInfo2Port(Port port,TokenOs ostoken){
		if(null == port)
			return;
    	try {
			setPortSubnetInfo(port,ostoken);
		} catch (BusinessException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
	}
	
	private void setRelateResourceInfo2Port(Port port){
		if(null == port)
			return;
		Instance instance = instanceMapper.selectByPrimaryKey(port.getDevice_id());
		if(null != instance){
			 port.addResource(port.getDevice_id(), instance.getName(), ParamConstant.INSTANCE);
			 return;
		}
		
		Router router = routerMapper.selectByPrimaryKey(port.getDevice_id());
		if(null != router){
			port.addResource(port.getDevice_id(), router.getName(), ParamConstant.ROUTER);
		    return;
		}
		
		Loadbalancer lb = loadbalancerMapper.selectByPrimaryKey(port.getDevice_id());
		if(null != lb){
			 port.addResource(port.getDevice_id(), lb.getName(), ParamConstant.LOADBALANCER);
			 return;
		}
		
		FloatingIP floatingIP = floatingipMapper.selectByPrimaryKey(port.getDevice_id());
		if(null != floatingIP){
			 port.addResource(port.getDevice_id(), StringHelper.string2Ncr(floatingIP.getName()), ParamConstant.FLOATINGIP);
			 return;
		}
//	    FloatingIP floatingIP = floatingipMapper.selectByPortId(port.getId());
//	    Instance instance = null;
//	    if(null != floatingIP){
//	    	port.setFloatingIp(floatingIP);	
//	    	instance = instanceMapper.selectInstanceByFloatingIp(floatingIP.getFloatingIpAddress());	
//	    }else{
//	    	String ip = port.getIp();
//	    	String networkId = null != port.getSubnet() ? port.getSubnet().getNetwork_id() : "";
//		    //instance = instanceMapper.selectInstanceByFixedIp(ip); 
//	    	instance = instanceMapper.selectInstanceByFixedIpAndNetwork(ip,networkId);
//	    }
	   
	}
	
	private List<Port> storePorts2DB(List<Port> ports, TokenOs ostoken) {
		if (Util.isNullOrEmptyList(ports))
			return null;
		List<Port> portsWithSubnet = new ArrayList<Port>();

		for (Port port : ports) {
			setSubnetInfo2Port(port, ostoken);
			portsWithSubnet.add(port);
			// storePort2DB(port);
		}
		portMapper.insertOrUpdateBatch(portsWithSubnet);

		return portsWithSubnet;
	}
	
	private void storePort2DB(Port port){
		if(null == port)
			return;
//		if(null == portMapper.selectByPrimaryKey(port.getId())){
//			port.setCreatedAt(Util.getCurrentDate());
//		}
		portMapper.insertOrUpdate(port);
//		if(null != portMapper.selectByPrimaryKey(port.getId()))
//			portMapper.updateByPrimaryKeySelective(port);
//		else{
//			port.setCreatedAt(Util.getCurrentDate()); //maybe change it later
//			portMapper.insertSelective(port);
//		}
			
	}
	
	private List<Port> getPortsFromDB(TokenOs ostoken,int limitItems){
		List<Port> portsFromDB = null;
		if(-1 == limitItems){
			portsFromDB = portMapper.selectAllByTenantId(ostoken.getTenantid());
		}else{
			portsFromDB = portMapper.selectAllByTenantIdWithLimit(ostoken.getTenantid(),limitItems);
		}
		if(Util.isNullOrEmptyList(portsFromDB))
			return null;
    	List<Port> portsFromDBWithSubnet = new ArrayList<Port>();
    	for(Port port : portsFromDB){
    		setSubnetInfo2Port(port,ostoken);
    		setRelateResourceInfo2Port(port);
    		portsFromDBWithSubnet.add(port);
    	}
    	return portsFromDBWithSubnet;
	}
	
    private void setPortSubnetInfo(Port port,TokenOs ostoken) throws BusinessException{
   		Subnet subnet = subnetMapper.selectByPrimaryKey(port.getSubnetId());
		if(null != subnet){
			port.setSubnet(subnet);
			port.setSubnetId(subnet.getId());
		}else{
//			SubnetServiceImpl snService = new SubnetServiceImpl();
//			snService.setCloudconfig(cloudconfig);
//			snService.setNetworkMapper(networkMapper);
//			snService.setSubnetMapper(subnetMapper);
			if(Util.isNullOrEmptyValue(port.getSubnetId()))
				return;
			int	index = port.getSubnetId().indexOf(",");
			if(-1 == index) //multiple subnet may bind to the same port
				subnet = subnetService.getSubnet(port.getSubnetId(),ostoken);
			else{
				subnet = subnetService.getSubnet(port.getSubnetId().substring(0, index),ostoken);
			}
				
			if(null != subnet){
		//		subnetMapper.insertSelective(subnet);
				port.setSubnet(subnet);
				port.setSubnetId(subnet.getId());
			}
		}
    }
    
	private Port getPort(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode portNode = rootNode.path(ResponseConstant.PORT);
		return getPortInfo(portNode);
	}
	
    private List<Port> getPorts(Map<String, String> rs) throws JsonProcessingException, IOException{
    	ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode portsNode = rootNode.path(ResponseConstant.PORTS);
		List<Port> ports = new ArrayList<Port>();
		int size = portsNode.size();
		for(int i = 0; i < size; ++i){
			Port port = getPortInfo(portsNode.get(i));
			if(null == port)
				continue;
			ports.add(port);
		}
		return ports;
    }
    
	private Port getPortInfo(JsonNode portNode){
		if(null == portNode)
			return null;
//		String deviceName = portNode.path(ResponseConstant.DEVICE_OWNER).textValue();
//		if(!Util.isNullOrEmptyValue(deviceName))
//			return null;
		Port port = new Port();
		port.setId(portNode.path(ResponseConstant.ID).textValue());
		port.setName(portNode.path(ResponseConstant.NAME).textValue());
		if(Util.isNullOrEmptyValue(port.getName()))
			return null;
		port.setStatus(portNode.path(ResponseConstant.STATUS).textValue());
		port.setMacAddress(portNode.path(ResponseConstant.MAC_ADDRESS).textValue());
		port.setDevice_owner(portNode.path(ResponseConstant.DEVICE_OWNER).textValue());
		port.setNetwork_id(portNode.path(ResponseConstant.NETWORK_ID).textValue());
		port.setTenantId(portNode.path(ResponseConstant.TENANT_ID).textValue());
		//port.setCreatedAt(portNode.path(ResponseConstant.CREATED_AT).textValue());
		//port.setMillionSeconds(Util.time2Millionsecond(portNode.path(ResponseConstant.CREATED_AT).textValue(), ParamConstant.TIME_FORMAT_02));
		port.setAdmin_state_up(portNode.path(ResponseConstant.ADMIN_STATE_UP).booleanValue());
		port.setDevice_id(portNode.path(ResponseConstant.DEVICE_ID).textValue());
	    JsonNode securityGroupNode = portNode.path(ResponseConstant.SECURITY_GROUPS);
	    if(null != securityGroupNode){
	    	int securityGroupCount = securityGroupNode.size();
	    	List<String> securityGroups = new ArrayList<String>();
	    	for(int index = 0; index < securityGroupCount; ++index){
	    		securityGroups.add(securityGroupNode.get(index).textValue());
	    	}
	    	port.setSecurity_groups(securityGroups);
	    	port.setSecurityGroupId(Util.listToString(securityGroups, ','));
	    }
	    
		port.setFixed_ips(getFixedIP(portNode.path(ResponseConstant.FIXED_IPS)));
		port.makeIpAndSubnetId();
		return port;
	}
	
	private List<FixedIP> getFixedIP(JsonNode fixedIpsNode){
		if(null == fixedIpsNode)
			return null;
		int fixedipsCount = fixedIpsNode.size();
		if(0 == fixedipsCount)
			return null;
		List<FixedIP> fixedIPs = new ArrayList<FixedIP>();
		for(int index = 0; index < fixedipsCount; ++index){
			FixedIP fixedIp = new FixedIP();
			fixedIp.setId(Util.makeUUID());
			fixedIp.setSubnet_id(fixedIpsNode.get(index).path(ResponseConstant.SUBNET_ID).textValue());
			fixedIp.setIp_address(fixedIpsNode.get(index).path(ResponseConstant.IP_ADDRESS).textValue());
			fixedIPs.add(fixedIp);
		}
		
		return fixedIPs;
	}
	
	private String getCreatedPortBody(String createBody,TokenOs ostoken,Locale locale) throws JsonProcessingException, IOException, BusinessException {
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(createBody);
		String subnetId = rootNode.path(ResponseConstant.SUBNET).textValue();
		Subnet subnet = subnetMapper.selectByPrimaryKey(subnetId);
		if(null == subnet){
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		String networkId = subnet.getNetwork_id();
		if(Util.isNullOrEmptyValue(networkId)){
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		String name = rootNode.path(ResponseConstant.NAME).textValue();
		name = StringHelper.string2Ncr(name);
		checkName(name,ostoken);
		String ip = rootNode.path(ResponseConstant.IP).textValue();
		
		Port port = new Port();
		port.setNetwork_id(networkId);
		FixedIP fixedIP = new FixedIP();
		fixedIP.setSubnet_id(subnetId);
		fixedIP.setIp_address(ip);
		List<FixedIP> fixedIPs = new ArrayList<FixedIP>();
		fixedIPs.add(fixedIP);
		port.setFixed_ips(fixedIPs);
		port.setName(name);
		port.setSubnet(null);
		port.setAdmin_state_up(true);
		
		PortJSON portJson = new PortJSON(port);
		JsonHelper<PortJSON, String> jsonHelp = new JsonHelper<PortJSON, String>();
		return jsonHelp.generateJsonBodySimple(portJson);
	}
	
	private List<Port> getLimitItems(List<Port> ports,String tenantId,int limit){
		if(Util.isNullOrEmptyList(ports))
			return null;
		List<Port> tenantPorts = new ArrayList<Port>();
		for(Port port : ports){
			if(!tenantId.equals(port.getTenantId()))
				continue;
			tenantPorts.add(port);
		}
		if(-1 != limit){
			if(limit <= tenantPorts.size())
				return tenantPorts.subList(0, limit);
		}
		return tenantPorts;
	}
	
	private void checkResource(String id, Locale locale,Boolean checkBinding) throws BusinessException {
		SyncResource resource = syncResourceMapper.selectByPrimaryKey(id);
		if(null != resource)
			throw new ResourceBusinessException(Message.CS_RESOURCE_IS_DOING,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);
        
		if(true == checkBinding){
			Port port = portMapper.selectByPrimaryKey(id);
			if(null == port)
				return;
			if(!Util.isNullOrEmptyValue(port.getDevice_id()))
				throw new ResourceBusinessException(Message.CS_HAVE_RELATED_RESOURCE_ATTACH_WITH_PORT,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,locale);	
		}
		return;
	}
	
	private void checkName(String name,TokenOs ostoken)  throws BusinessException{
		if(Util.isNullOrEmptyValue(name))
			return;
		List<Port> ports = portMapper.selectAllByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(ports))
			return;
		for(Port port : ports){
			if(name.equals(port.getName()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_NAME_IS_SAME,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		return;
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
}
