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
import com.cloud.cloudapi.pojo.openstackapi.forgui.FloatingIP;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FloatingIPConfig;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.openstackapi.FloatingIPService;
import com.cloud.cloudapi.service.openstackapi.InstanceService;
import com.cloud.cloudapi.service.openstackapi.NetworkService;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.service.openstackapi.RouterService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

/**
 * 
 * floating ip操作
 *
 */
@RestController
public class FloatingIPController extends BaseController {

	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private RouterService routerService;
	
	@Resource
	private FloatingIPService floatingIPService;
	
	@Resource
	private InstanceService instanceService;
	
	@Resource
	private NetworkService networkService;
	
	@Resource
	private QuotaService quotaService;

	private Logger log = LogManager.getLogger(FloatingIPController.class);
	
	@RequestMapping(value = "/floating-ips", method = RequestMethod.GET)
	public String getFloatingIPList(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit, HttpServletResponse response) {

		TokenOs authToken=null;
		try{
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String, String> paramMap = null;
			if (!"".equals(limit)) {
				paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.STATUS, status);
			}
			if (!"".equals(limit)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.LIMIT, limit);
			}
			
			List<FloatingIP> floatingips = floatingIPService.getFloatingIPList(paramMap, authToken);
			if(null == floatingips){
				floatingips = new ArrayList<FloatingIP>();
				JsonHelper<List<FloatingIP>, String> jsonHelp = new JsonHelper<List<FloatingIP>, String>();
	//			this.operationLogService.addOperationLog(this.getthis.getAuthService()().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLOATINGIP,ParamConstant.FLOATINGIP,"",Message.SUCCESSED_FLAG,"");
				return jsonHelp.generateJsonBodyWithEmpty(floatingips);
			}
			normalFloatingIPsInfo(floatingips,new Locale(authToken.getLocale()));
	//		this.operationLogService.addOperationLog(this.getthis.getAuthService()().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLOATINGIP,ParamConstant.FLOATINGIP,getFloatingIPsId(floatingips),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<FloatingIP>, String> jsonHelp = new JsonHelper<List<FloatingIP>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(floatingips);
			
		}catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLOATINGIP,ParamConstant.FLOATINGIP,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLOATINGIP,ParamConstant.FLOATINGIP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_FLOATINGIP_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLOATINGIP,ParamConstant.FLOATINGIP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} 
	}

	@RequestMapping(value = "/floating-ips/{id}", method = RequestMethod.GET)
	public String getFloatingIP(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		TokenOs authToken=null;
		try{
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			FloatingIP floatingip = floatingIPService.getFloatingIP(id, authToken);
			if(null == floatingip){
	//			this.operationLogService.addOperationLog(this.getthis.getAuthService()().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLOATINGIP_DETAIL,ParamConstant.FLOATINGIP,"",Message.SUCCESSED_FLAG,"");
				JsonHelper<FloatingIP, String> jsonHelp = new JsonHelper<FloatingIP, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new FloatingIP());
			}
			floatingip.normalInfo(new Locale(authToken.getLocale()));
	//		this.operationLogService.addOperationLog(this.getthis.getAuthService()().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLOATINGIP_DETAIL,ParamConstant.FLOATINGIP,id,Message.SUCCESSED_FLAG,"");
			JsonHelper<FloatingIP, String> jsonHelp = new JsonHelper<FloatingIP, String>();
			return jsonHelp.generateJsonBodyWithEmpty(floatingip);
			
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLOATINGIP_DETAIL,ParamConstant.FLOATINGIP,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLOATINGIP_DETAIL,ParamConstant.FLOATINGIP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_FLOATINGIP_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLOATINGIP_DETAIL,ParamConstant.FLOATINGIP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} 
	}
	
	@RequestMapping(value = "/floating-ips/config", method = RequestMethod.GET)
	public String getFloatingIPConfig(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			HttpServletResponse response) {

		TokenOs authToken=null;
		try{
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			FloatingIPConfig floatingipConfig = floatingIPService.getFloatingIPConfig(authToken);
			if(null == floatingipConfig){
				response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_FLOATINGIP_CONFIG_GET_FAILED,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
	//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLOATINGIP_CONFIG,ParamConstant.FLOATINGIPCONFIG,"",Message.FAILED_FLAG,message);
				return message;
			}
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLOATINGIP_CONFIG,ParamConstant.FLOATINGIPCONFIG,floatingipConfig.getId(),Message.SUCCESSED_FLAG,"");
			JsonHelper<FloatingIPConfig, String> jsonHelp = new JsonHelper<FloatingIPConfig, String>();
			return jsonHelp.generateJsonBodyWithEmpty(floatingipConfig);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			String message = e.getResponseMessage();
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLOATINGIP_CONFIG,ParamConstant.FLOATINGIPCONFIG,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLOATINGIP_CONFIG,ParamConstant.FLOATINGIPCONFIG,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_FLOATINGIP_CONFIG_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLOATINGIP_CONFIG,ParamConstant.FLOATINGIPCONFIG,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} 
	}
	
    @RequestMapping(value="/floating-ips",method=RequestMethod.POST)
    public String createFloatingIp(@RequestHeader(value=ParamConstant.AUTH_TOKEN,defaultValue="nownoimpl") String guiToken,
    		@RequestBody String createBody, HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.FLOATINGIP_NEW);
			FloatingIP floatingIp = floatingIPService.createFloatingIp(createBody, authToken);
			if(null == floatingIp){
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_FLOATINGIP,ParamConstant.FLOATINGIP,"",Message.FAILED_FLAG,message);
				return message;
			}
			floatingIp.normalInfo(new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_FLOATINGIP,ParamConstant.FLOATINGIP,floatingIp.getId(),Message.SUCCESSED_FLAG,"");
			JsonHelper<FloatingIP, String> jsonHelp = new JsonHelper<FloatingIP, String>();
			return jsonHelp.generateJsonBodySimple(floatingIp);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_FLOATINGIP,ParamConstant.FLOATINGIP,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_FLOATINGIP,ParamConstant.FLOATINGIP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_FLOATINGIP_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_FLOATINGIP,ParamConstant.FLOATINGIP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}     
    }
    
	@RequestMapping(value = "/floating-ips/{id}", method = RequestMethod.PUT)
	public String updateFloatingIP(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String body, HttpServletResponse response) {

		TokenOs authToken=null;
		try{
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.FLOATINGIP_UPDATE);
			FloatingIP floatingip = floatingIPService.updateFloatingIP(id, body,authToken);
			floatingip.normalInfo(new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_FLOATINGIP,ParamConstant.FLOATINGIP,id,Message.SUCCESSED_FLAG,"");
			JsonHelper<FloatingIP, String> jsonHelp = new JsonHelper<FloatingIP, String>();
			return jsonHelp.generateJsonBodyWithEmpty(floatingip);
			
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_FLOATINGIP,ParamConstant.FLOATINGIP,id,Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR);
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_FLOATINGIP,ParamConstant.FLOATINGIP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_FLOATINGIP_UPDATE_FAILED);
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_FLOATINGIP,ParamConstant.FLOATINGIP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} 
	}
	
	@RequestMapping(value = "/floating-ips/{id}", method = RequestMethod.DELETE)
	public String deleteFloatingIP(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id,HttpServletResponse response) {

		TokenOs authToken=null;
		try{
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.FLOATINGIP_DELETE);
			floatingIPService.deleteFloatingIP(id,authToken);
			if(null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_FLOATINGIP_DELETE_SUCCESSED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_FLOATINGIP,ParamConstant.FLOATINGIP,id,Message.SUCCESSED_FLAG,message);
			return message;
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_FLOATINGIP,ParamConstant.FLOATINGIP,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_FLOATINGIP,ParamConstant.FLOATINGIP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_FLOATINGIP_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_FLOATINGIP,ParamConstant.FLOATINGIP,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} 
	}
	
	private void normalFloatingIPsInfo(List<FloatingIP> floatingIPs,Locale locale){
		if(null == floatingIPs)
			return;
		for(FloatingIP floatingIP : floatingIPs){
			floatingIP.normalInfo(locale);
		}
	}
}
