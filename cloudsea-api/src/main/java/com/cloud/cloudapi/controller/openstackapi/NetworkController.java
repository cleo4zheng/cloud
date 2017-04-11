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
import com.cloud.cloudapi.pojo.openstackapi.forgui.Subnet;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.openstackapi.NetworkService;
import com.cloud.cloudapi.service.openstackapi.PortService;
import com.cloud.cloudapi.service.openstackapi.SubnetService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class NetworkController extends BaseController{
	
	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private NetworkService networkService;
	
	@Resource
	private SubnetService subnetService;
	
	@Resource
	private PortService portService;
	
	private Logger log = LogManager.getLogger(NetworkController.class);
	
	/**
	 * get the network list by parameter and guitoken
	 * @param guiToken guitokenid
	 * @param limit    how many to be show
	 * @param name     the name of network
	 * @param status   the status of network
	 * @return
	 */
  
    @RequestMapping(value="/networks",method=RequestMethod.GET)
    public String  getNetworksList(@RequestHeader(value=ParamConstant.AUTH_TOKEN,defaultValue="nownoimpl") String guiToken,
		    @RequestParam(value=ParamConstant.LIMIT,defaultValue="") String limit,
    		@RequestParam(value=ParamConstant.NAME, defaultValue="") String name,
    		@RequestParam(value=ParamConstant.STATUS,defaultValue="") String status,HttpServletResponse response) {

    	TokenOs authToken = null;
    	try{
    		authToken = this.getUserOsToken(guiToken); 
    		if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
        	Map<String,String> paramMap=null; 
        	if(!"".equals(limit)){
        		paramMap=new HashMap<String,String>();
        		paramMap.put(ParamConstant.LIMIT, limit);
        	}
        	
        	if(!"".equals(name)){		
        		if(paramMap==null) paramMap=new HashMap<String,String>();
        		paramMap.put(ParamConstant.NAME, name);
        	}
        	
        	if(!"".equals(status)){		
        		if(paramMap==null) paramMap=new HashMap<String,String>();
        		paramMap.put(ParamConstant.STATUS, status);
        	}
        	
        	List<Network> networks = networkService.getNetworkList(paramMap, authToken);
			if(null == networks){
		//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_NETWORK,ParamConstant.NETWORK,"",Message.SUCCESSED_FLAG,"");
				JsonHelper<List<Network>, String> jsonHelp = new JsonHelper<List<Network>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Network>());
			}
			normalNetworksInfo(networks);
		//	this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_NETWORK,ParamConstant.NETWORK,getNetworksId(networks),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<Network>,String> jsonHelp = new JsonHelper<List<Network>,String>();
			return jsonHelp.generateJsonBodyWithEmpty(networks);
    	} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_NETWORK,ParamConstant.NETWORK,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_NETWORK,ParamConstant.NETWORK,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_NETWORK,ParamConstant.NETWORK,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}     	
    	//@TODO 1. guitoken should has no defaultValue,if there no token ,bad request  
    	
    	//@TODO 2. guitoken should be checked, timeout or not 
    }
    
    @RequestMapping(value="/networks/{id}",method=RequestMethod.GET)
    public String  getNetwork(@RequestHeader(value=ParamConstant.AUTH_TOKEN,defaultValue="nownoimpl") String guiToken,
    		@PathVariable String id, HttpServletResponse response){
		TokenOs authToken=null;
		
    	try{
    		authToken = this.getUserOsToken(guiToken); 
    		if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Network network = networkService.getNetwork(id, authToken);
			if(null == network){
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
		//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_NETWORK_DETAIL,ParamConstant.NETWORK,"",Message.FAILED_FLAG,message);
				return message;
			}
			normalNetworkInfo(network,true);
		//	this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.GET_NETWORK_DETAIL,ParamConstant.NETWORK,network.getId(),Message.SUCCESSED_FLAG,"");
			JsonHelper<Network, String> jsonHelp = new JsonHelper<Network, String>();
			return jsonHelp.generateJsonBodyWithEmpty(network);
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
    
    @RequestMapping(value="/networks",method=RequestMethod.POST)
    public String  createNetwork(@RequestHeader(value=ParamConstant.AUTH_TOKEN,defaultValue="nownoimpl") String guiToken,
    		@RequestBody String createBody,HttpServletResponse response) {
		//get ostoken by cuibl
		TokenOs authToken=null;
    	try{
    		authToken = this.getUserOsToken(guiToken); 
    		if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
    		this.checkUserPermission(authToken, ParamConstant.NETWORK_NEW);
        	Network network = networkService.createNetwork(createBody,authToken);
			if(null == network){
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_NETWORK,ParamConstant.NETWORK,"",Message.FAILED_FLAG,message);
				return message;
			}
			network.normalInfo(false);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.CREATE_NETWORK,ParamConstant.NETWORK,network.getId(),Message.SUCCESSED_FLAG,"");
			JsonHelper<Network, String> jsonHelp = new JsonHelper<Network, String>();
			return jsonHelp.generateJsonBodyWithEmpty(network);
     	} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_NETWORK,ParamConstant.NETWORK,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_NETWORK,ParamConstant.NETWORK,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_NETWORK,ParamConstant.NETWORK,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
    }
    
    @RequestMapping(value="/networks/{id}",method=RequestMethod.PUT)
    public String  updateNetwork(@RequestHeader(value=ParamConstant.AUTH_TOKEN,defaultValue="nownoimpl") String guiToken,
    		@PathVariable String id,@RequestBody String body,HttpServletResponse response) {

		TokenOs authToken=null;
    	try{
    		authToken = this.getUserOsToken(guiToken);
    		if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
    		this.checkUserPermission(authToken, ParamConstant.NETWORK_UPDATE);
        	Network network = networkService.updateNetwork(id, body,authToken);
        	normalNetworkInfo(network,true);
        	ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_UPDATE_SUCCESSED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_NETWORK,ParamConstant.NETWORK,id,Message.SUCCESSED_FLAG,exception.getResponseMessage());
			JsonHelper<Network, String> jsonHelp = new JsonHelper<Network, String>();
			return jsonHelp.generateJsonBodyWithEmpty(network);
     	} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_NETWORK,ParamConstant.NETWORK,id,Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_NETWORK,ParamConstant.NETWORK,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_NETWORK,ParamConstant.NETWORK,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
    }
    
    @RequestMapping(value="/networks/{id}",method=RequestMethod.DELETE)
    public String  deleteNetwork(@RequestHeader(value=ParamConstant.AUTH_TOKEN,defaultValue="nownoimpl") String guiToken,
    		@PathVariable String id,HttpServletResponse response) {
		//get ostoken by cuibl
		TokenOs authToken=null;
    	try{
    		authToken = this.getUserOsToken(guiToken); 
    		if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
    		this.checkUserPermission(authToken, ParamConstant.NETWORK_DELETE);
        	networkService.deleteNetwork(id, authToken);
			if(null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
        	ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_DELETE_SUCCESSED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_NETWORK,ParamConstant.NETWORK,id,Message.SUCCESSED_FLAG,exception.getResponseMessage());
			return exception.getResponseMessage();
     	} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_NETWORK,ParamConstant.NETWORK,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.DELETE_NETWORK,ParamConstant.NETWORK,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.DELETE_NETWORK,ParamConstant.NETWORK,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
    }
    
    private void normalNetworkInfo(Network network,Boolean normalSubnetInfo){
	    network.normalInfo(false);
		if(false == normalSubnetInfo){
			network.setManaged(null);
			List<Subnet> subnets = network.getSubnets();
			if(Util.isNullOrEmptyList(subnets))
				return;
			for(Subnet subnet : subnets){
				subnet.setCidr(null);
				subnet.setGateway(null);
				subnet.setIpVersion(null);
				subnet.normalInfo(true);
			}
		}else{
			List<Subnet> subnets = network.getSubnets();
			if(Util.isNullOrEmptyList(subnets))
				return;
			for(Subnet subnet : subnets){
				subnet.normalInfo(true);
			}
		}
	}
	
	private void normalNetworksInfo(List<Network> networks){
		if(Util.isNullOrEmptyList(networks))
			return;
		for(Network network : networks){
			normalNetworkInfo(network,false);
		}
	}
}
