package com.cloud.cloudapi.controller.openstackapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloud.cloudapi.controller.common.BaseController;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FloatingIP;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Port;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SecurityGroup;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Subnet;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.openstackapi.PortService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.StringHelper;

@RestController
public class PortController extends BaseController {
	
	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private PortService portService;

	private Logger log = LogManager.getLogger(PortController.class);
	
	@RequestMapping(value = "/ports", method = RequestMethod.GET)
	public String getPorts(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String, String> paramMap = null;
			if (!"".equals(limit)) {
				paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.LIMIT, limit);
			}

			if (!"".equals(name)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.NAME, name);
			}

			if (!"".equals(status)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.STATUS, status);
			}

			List<Port> ports = portService.getPortList(paramMap, authToken, true);
			if (Util.isNullOrEmptyList(ports)) {
//				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//						Message.GET_PORT, ParamConstant.PORT, "", Message.SUCCESSED_FLAG, "");
				JsonHelper<List<Port>, String> jsonHelp = new JsonHelper<List<Port>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Port>());
			}
			normalPortsInfo(ports);
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_PORT,
//					ParamConstant.PORT, getPortsId(ports), Message.SUCCESSED_FLAG, "");
			response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			JsonHelper<List<Port>, String> jsonHelp = new JsonHelper<List<Port>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(ports);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_PORT,
					ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.GET_PORT,
					ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_PORT_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_PORT,
					ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/ports/{id}", method = RequestMethod.GET)
	public String getPort(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Port port = portService.getPort(id, authToken, true);
			if (null == port) {
//				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//						Message.GET_PORT_DETAIL, ParamConstant.PORT, "", Message.SUCCESSED_FLAG, "");
				JsonHelper<Port, String> jsonHelp = new JsonHelper<Port, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new Port());
			}
			normalPortInfo(port,true);
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//					Message.GET_PORT_DETAIL, ParamConstant.PORT, port.getId(), Message.SUCCESSED_FLAG, "");
			JsonHelper<Port, String> jsonHelp = new JsonHelper<Port, String>();
			return jsonHelp.generateJsonBodySimple(port);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_PORT_DETAIL, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_PORT_DETAIL, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NETWORK_PORT_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_PORT_DETAIL, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/ports", method = RequestMethod.POST)
	public String createPort(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.PORT_NEW);
			Port port = portService.createPort(createBody, authToken);
			if (null == port) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.CREATE_PORT, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
				return message;
			}
			normalPortInfo(port,false);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_PORT, ParamConstant.PORT, port.getId(), Message.SUCCESSED_FLAG, "");
			JsonHelper<Port, String> jsonHelp = new JsonHelper<Port, String>();
			return jsonHelp.generateJsonBodySimple(port);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_PORT, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_PORT, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_PORT_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_PORT, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/ports/{id}", method = RequestMethod.PUT)
	public String updatePort(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id,@RequestBody String body, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.PORT_UPDATE);
			Port port = portService.updatePort(id,body, authToken);
			normalPortInfo(port,true);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_PORT, ParamConstant.PORT, id, Message.SUCCESSED_FLAG, "");
			JsonHelper<Port, String> jsonHelp = new JsonHelper<Port, String>();
			return jsonHelp.generateJsonBodySimple(port);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_PORT, ParamConstant.PORT, id, Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_PORT, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_PORT_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_PORT, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/ports/{id}", method = RequestMethod.DELETE)
	public String deletePort(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.PORT_DELETE);
			portService.deletePort(id, authToken);
			if(null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_PORT_DELETE_SUCCESSED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_PORT, ParamConstant.PORT, id, Message.SUCCESSED_FLAG, exception.getResponseMessage());
			return exception.getResponseMessage();
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_PORT, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_PORT, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NETWORK_PORT_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_PORT, ParamConstant.PORT, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	private void normalPortInfo(Port port,Boolean normalDetailInfo){
		port.normalInfo(false);
        if(false == normalDetailInfo){
        	port.setMacAddress(null);
        	port.setFloatingIp(null);
        	port.setSecurityGroups(null);
        }else{
        	FloatingIP floatingIP = port.getFloatingIp();
        	if(null != floatingIP){
        		floatingIP.setAssigned(null);
        		floatingIP.setBandwith(null);
        		floatingIP.setCreatedAt(null);
        		floatingIP.setFixedIpAddress(null);
        		floatingIP.setFloating_network_id(null);
        		floatingIP.setFloatingIpAddress(null);
        		floatingIP.setInstanceId(null);
        		floatingIP.setMillionSeconds(null);
        		floatingIP.setName(StringHelper.ncr2String(floatingIP.getName()));
        		floatingIP.setNetworkId(null);
        		floatingIP.setPort_id(null);
        		floatingIP.setResource(null);
        		floatingIP.setRouterId(null);
        		floatingIP.setStatus(null);
        		floatingIP.setTenantId(null);
        		floatingIP.setType(null);
        		floatingIP.setTypeName(null);
        		floatingIP.setUnitPrice(null);
        		port.setFloatingIp(floatingIP);
        	}
        	
        	List<SecurityGroup> securityGroups = port.getSecurityGroups();
        	if(null != securityGroups){
        		for(SecurityGroup securityGroup : securityGroups){
        			securityGroup.setName(StringHelper.ncr2String(securityGroup.getName()));
        			securityGroup.setDescription(StringHelper.ncr2String(securityGroup.getDescription()));
        			securityGroup.setMillionSeconds(null);
        			securityGroup.setSecurityGroupRuleIds(null);
        			securityGroup.setSecurityGroupRules(null);
        			securityGroup.setTenantId(null);
        			securityGroup.setCreatedAt(null);
        		}	
        	}
       
        }

        Subnet subnet = port.getSubnet();
        if(null != subnet){
    		subnet.setName(StringHelper.ncr2String(subnet.getName()));
    		subnet.setCreatedAt(null);
    		subnet.setMillionSeconds(null);
    		subnet.setDhcp(null);
    		subnet.setEnable_dhcp(null);
    		subnet.setGateway_ip(null);
    		subnet.setGateway(null);
    		subnet.setIp_version(null);
    		subnet.setNetwork_id(null);
    		subnet.setSegment(null);
    		subnet.setTenant_id(null);
    		subnet.setNetwork(null);
    		subnet.setRouters(null);
    		subnet.setCidr(null);
    		subnet.setIpVersion(null);
    		port.setSubnet(subnet);
        }
	}
	
	private void normalPortsInfo(List<Port> ports){
		if(Util.isNullOrEmptyList(ports))
			return;
		for(Port port : ports){
			normalPortInfo(port,false);
			
		}
	}
//	private String getPortsId(List<Port> ports){
//		if(Util.isNullOrEmptyList(ports))
//			return "";
//		List<String> ids = new ArrayList<String>();
//		for(Port port : ports){
//			ids.add(port.getId());
//		}
//		return Util.listToString(ids, ',');
//	}
}
