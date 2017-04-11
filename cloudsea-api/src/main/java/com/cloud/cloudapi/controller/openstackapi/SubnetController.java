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
import com.cloud.cloudapi.pojo.openstackapi.forgui.Network;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Router;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Subnet;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.openstackapi.NetworkService;
import com.cloud.cloudapi.service.openstackapi.SubnetService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.StringHelper;

@RestController
public class SubnetController  extends BaseController {

	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private NetworkService networkService;
	
	@Resource
	private SubnetService subnetService;

	private Logger log = LogManager.getLogger(SubnetController.class);
	
	/**
	 * get the subnet list by parameter and guitoken - this user's subnets
	 * filter by opentack api (tenant_id) - network filter by openstack api -
	 * 
	 * @param guiToken
	 *            guitokenid
	 * @param limit
	 *            how many to be show
	 * @param name
	 *            the name of subnet
	 * @param status
	 *            the status of subnet
	 * @param network
	 *            the id of network
	 * @return
	 */

	@RequestMapping(value = "/subnets", method = RequestMethod.GET)
	public String getSubnetsList(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.NETWORK_ID, defaultValue = "") String network_id,
			HttpServletResponse response) {
		// get ostoken by cuibl
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

			if (!"".equals(network_id)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.NETWORK_ID, network_id);
			}

			List<Subnet> subnets = subnetService.getSubnetList(paramMap, authToken);
			if (null == subnets) {
//				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//						Message.GET_SUBNET, ParamConstant.SUBNET, "", Message.SUCCESSED_FLAG, "");
				JsonHelper<List<Subnet>, String> jsonHelp = new JsonHelper<List<Subnet>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Subnet>());
			}
			normalSubnetsInfo(subnets);
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//					Message.GET_SUBNET, ParamConstant.SUBNET, getSubnetsId(subnets), Message.SUCCESSED_FLAG, "");
			JsonHelper<List<Subnet>, String> jsonHelp = new JsonHelper<List<Subnet>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(subnets);

		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_SUBNET, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_SUBNET, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_SUBNET_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_SUBNET, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	   @RequestMapping(value="/special-networks",method=RequestMethod.GET)
	    public String  getSpecialNetwork(@RequestHeader(value=ParamConstant.AUTH_TOKEN,defaultValue="nownoimpl") String guiToken,
	    		HttpServletResponse response){
			TokenOs authToken=null;
	    	try{
	    		authToken = this.getUserOsToken(guiToken);
	    		if(null == authToken){
					response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
					ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
					return exception.getResponseMessage();
				}
				List<Subnet> subnets = subnetService.getSpecialAdminNetwork(authToken);
				if(null == subnets)
					subnets = new ArrayList<Subnet>();
				normalSubnetsInfo(subnets);
			//	this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.GET_NETWORK_DETAIL,ParamConstant.NETWORK,network.getId(),Message.SUCCESSED_FLAG,"");
				JsonHelper<List<Subnet>, String> jsonHelp = new JsonHelper<List<Subnet>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(subnets);
	    	} catch (ResourceBusinessException e) {
				response.setStatus(e.getStatusCode());
				String message = e.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_NETWORK_DETAIL,ParamConstant.NETWORK,"",Message.FAILED_FLAG,message);
				log.error(message,e);
				return message;
			} catch (MyBatisSystemException e) {
				response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.GET_NETWORK_DETAIL,ParamConstant.NETWORK,"",Message.FAILED_FLAG,message);
				log.error(message,e);
				return message;
			} catch (Exception e) {
				response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_NETWORK_DETAIL,ParamConstant.NETWORK,"",Message.FAILED_FLAG,message);
				log.error(message,e);
				return message;
			} 
	    }
	   
	@RequestMapping(value = "/subnets/{id}", method = RequestMethod.GET)
	public String getSubnet(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Subnet subnet = subnetService.getSubnet(id, authToken);
			if (null == subnet) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
//				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//						Message.GET_SUBNET_DETAIL, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
				return message;
			}
			normalSubnetInfo(subnet,true);
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//					Message.GET_SUBNET_DETAIL, ParamConstant.SUBNET, subnet.getId(), Message.SUCCESSED_FLAG, "");
			JsonHelper<Subnet, String> jsonHelp = new JsonHelper<Subnet, String>();
			return jsonHelp.generateJsonBodyWithEmpty(subnet);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_SUBNET_DETAIL, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_SUBNET_DETAIL, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_SUBNET_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_SUBNET_DETAIL, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/subnets", method = RequestMethod.POST)
	public String createSubnet(
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
			this.checkUserPermission(authToken, ParamConstant.SUBNET_NEW);
			List<Subnet> subnets = subnetService.createSubnet(createBody, null,authToken, null);
			if (null == subnets) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.CREATE_SUBNET, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
				return message;
			}
			normalSubnetsInfo(subnets);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_SUBNET, ParamConstant.SUBNET, getSubnetsId(subnets), Message.SUCCESSED_FLAG, "");
			JsonHelper<List<Subnet> , String> jsonHelp = new JsonHelper<List<Subnet> , String>();
			return jsonHelp.generateJsonBodyWithEmpty(subnets);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_SUBNET, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_SUBNET, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_SUBNET_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_SUBNET, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/subnets/{id}/connectRouter", method = RequestMethod.PUT)
	public String connectRouter(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String createBody, HttpServletResponse response) {
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			subnetService.connectRouter(id,createBody, authToken);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.ADD_ROUTER, ParamConstant.SUBNET, id, Message.SUCCESSED_FLAG, "");
			JsonHelper<Subnet, String> jsonHelp = new JsonHelper<Subnet, String>();
			return jsonHelp.generateJsonBodyWithEmpty(new Subnet());
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.ADD_ROUTER, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.ADD_ROUTER, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_SUBNET_ADD_ROUTER_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.ADD_ROUTER, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/subnets/{id}", method = RequestMethod.PUT)
	public String updateSubnet(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String body, HttpServletResponse response) {
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.SUBNET_UPDATE);
			Subnet subnet = subnetService.updateSubnet(id, body, authToken);
			normalSubnetInfo(subnet,true);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_SUBNET_UPDATE_SUCCESSED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_SUBNET, ParamConstant.SUBNET, id, Message.SUCCESSED_FLAG,
					exception.getResponseMessage());
			JsonHelper<Subnet, String> jsonHelp = new JsonHelper<Subnet, String>();
			return jsonHelp.generateJsonBodyWithEmpty(subnet);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_SUBNET, ParamConstant.SUBNET, id, Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_SUBNET, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_SUBNET_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_SUBNET, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/subnets/{id}", method = RequestMethod.DELETE)
	public String deleteSubnet(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.SUBNET_DELETE);
			subnetService.deleteSubnet(id, authToken);
			if(null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_SUBNET_DELETE_SUCCESSED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_SUBNET, ParamConstant.SUBNET, id, Message.SUCCESSED_FLAG,
					exception.getResponseMessage());
			return exception.getResponseMessage();
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_SUBNET, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_SUBNET, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_SUBNET_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.DELETE_SUBNET, ParamConstant.SUBNET, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	private String getSubnetsId(List<Subnet> subnets) {
		if (Util.isNullOrEmptyList(subnets))
			return "";
		List<String> ids = new ArrayList<String>();
		for (Subnet subnet : subnets) {
			ids.add(subnet.getId());
		}
		return Util.listToString(ids, ',');
	}
	
	private void normalSubnetInfo(Subnet subnet,Boolean normalDetailInfo){
		subnet.setName(StringHelper.ncr2String(subnet.getName()));
	    if(null != subnet.getMillionSeconds()){
	    	subnet.setCreatedAt(Util.millionSecond2Date(subnet.getMillionSeconds()));
			subnet.setMillionSeconds(null);
	    }
		subnet.setEnable_dhcp(null);
		subnet.setGateway_ip(null);
		subnet.setIp_version(null);
		subnet.setNetwork_id(null);
		subnet.setSegment(null);
		subnet.setTenant_id(null);

		Network network = subnet.getNetwork();
		if(null != network){
			network.setName(StringHelper.ncr2String(network.getName()));
			network.setMillionSeconds(null);
			network.setAdmin_state_up(null);
			network.setBasic(null);
			network.setExternal(null);
			network.setFloatingipId(null);
			network.setFloatingIps(null);
			network.setInstance_id(null);
			network.setMtu(null);
			network.setNodeType(null);
			network.setPortId(null);
			network.setPorts(null);
			network.setQos_policy_id(null);
			network.setSecurityGroups(null);
			network.setServers(null);
			network.setShared(null);
			network.setTenant_id(null);
			network.setSubnetId(null);
			network.setSubnetsId(null);
			network.setSubnets(null);
			network.setStatus(null);
			subnet.setNetwork(network);
		}
		if(false == normalDetailInfo){
			subnet.setDhcp(null);
			subnet.setRouters(null);
		}else{
			List<Router> routers = subnet.getRouters();
			if(Util.isNullOrEmptyList(routers))
				return;
			for(Router router : routers){
				router.setAdmin_state_up(null);
				router.setCreatedAt(null);
				router.setDistributed(null);
				router.setExternal_gateway_info(null);
				router.setFirewallId(null);
				router.setFixedIps(null);
				router.setFloatingIPs(null);
				router.setGateway(null);
				router.setGatewayId(null);
				router.setHa(null);
				router.setMillionSeconds(null);
				router.setName(StringHelper.ncr2String(router.getName()));
			}
		}
	}
	
	private void normalSubnetsInfo(List<Subnet> subnets){
		if(Util.isNullOrEmptyList(subnets))
			return;
		for(Subnet subnet : subnets){
			normalSubnetInfo(subnet,false);
		}
	}
}
