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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.cloud.cloudapi.controller.common.BaseController;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.json.forgui.StackConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PoolConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PoolEntity;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PoolStack;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Stack;
import com.cloud.cloudapi.pojo.openstackapi.forgui.StackResource;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.openstackapi.ImageService;
import com.cloud.cloudapi.service.openstackapi.NetworkService;
import com.cloud.cloudapi.service.openstackapi.PoolEntityService;
import com.cloud.cloudapi.service.openstackapi.StackService;
import com.cloud.cloudapi.service.openstackapi.SubnetService;
import com.cloud.cloudapi.service.openstackapi.TenantService;
import com.cloud.cloudapi.service.pool.PoolResourceService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class PoolController  extends BaseController {
	
	@Resource
	private PoolEntityService poolEntityService;
	
	@Resource
	private PoolResourceService poolResourceService;
	
	@Resource
	private StackService stackService;
	
	@Resource
	private ImageService imageService;
	
	@Resource
	private NetworkService networkService;
	
	@Resource
	private SubnetService subnetService;

	@Resource
	private TenantService tenantServiceImpl;

	@Resource
	private OperationLogService operationLogService;

	private Logger log = LogManager.getLogger(PoolController.class);
	
	@RequestMapping(value = "/pools", method = RequestMethod.GET)
	public String getPools(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			HttpServletResponse response) {
		TokenOs authToken = null;
		try{
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
		    List<PoolEntity> pl = this.poolEntityService.listPoolEntity(authToken);
			return PoolEntity.toJSON(pl);
		} catch(Exception e){
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException rbe = new ResourceBusinessException(Message.CS_DEPLOY_POOL_GET_FAILED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_POOLS, ParamConstant.POOL, "", Message.FAILED_FLAG,
					rbe.getResponseMessage());
			log.error(rbe.getResponseMessage(),e);
			return rbe.getResponseMessage();
		}
	}

	@RequestMapping(value = "/pools/{pool_id}", method = RequestMethod.GET)
	public String getPool(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String pool_id, HttpServletResponse response) {
		PoolEntity p = this.poolEntityService.getPoolEntityById(pool_id);
		return PoolEntity.toJSON(p);
	}

	@RequestMapping(value = "/pools/{pool_id}", method = RequestMethod.PUT)
	public String updatePool(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String pool_id, HttpServletResponse response, @RequestBody String poolBody) {
		if (null == poolBody || "".equals(poolBody))
			return null;
		TokenOs authToken = null;

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> params = null;
		try {
			params = mapper.readValue(poolBody, new TypeReference<HashMap<String, Object>>() {
			});
		} catch (Exception e) {
			log.error(e);
			ResourceBusinessException rbe = new ResourceBusinessException(Message.CS_DEPLOY_POOL_UPDATE_FAILED,new Locale(this.getConfig().getSystemDefaultLocale()));
			return rbe.getResponseMessage();
		} 

		PoolEntity p = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.POOL_APPLY);
			p = poolEntityService.updatePoolEntity(params, pool_id, authToken);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_POOL, ParamConstant.POOL, pool_id,
					Message.SUCCESSED_FLAG, "");
			return PoolEntity.toJSON(p);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_POOL, ParamConstant.POOL, pool_id, Message.FAILED_FLAG,
					e.getResponseMessage());
			log.error(e.getResponseMessage(),e);
			return e.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException rbe = new ResourceBusinessException(Message.CS_DEPLOY_POOL_UPDATE_FAILED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_POOL, ParamConstant.POOL, pool_id, Message.FAILED_FLAG,
					rbe.getResponseMessage());
			log.error(rbe.getResponseMessage(),e);
			return rbe.getResponseMessage();
		}
	}

	@RequestMapping(value = "/pools", method = RequestMethod.POST)
	public String createPool(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String poolBody, HttpServletResponse response) {
		
		TokenOs authToken = null;
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> params = null;
		try {
			params = mapper.readValue(poolBody, new TypeReference<HashMap<String, Object>>() {
			});
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_DEPLOY_POOL_CREATE_FAILED,new Locale(this.getConfig().getSystemDefaultLocale())).getResponseMessage();
		}

		PoolEntity p = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.POOL_NEW);
			p = poolEntityService.createPoolEntity(params, authToken);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_POOL, ParamConstant.POOL, "",
					Message.SUCCESSED_FLAG, "");
			return PoolEntity.toJSON(p);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_POOL, ParamConstant.POOL, "",
					Message.FAILED_FLAG, e.getResponseMessage());
			log.error(e.getResponseMessage(),e);
			return e.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException rbe = new ResourceBusinessException(Message.CS_DEPLOY_POOL_CREATE_FAILED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_POOL, ParamConstant.POOL, "", Message.FAILED_FLAG,
					rbe.getResponseMessage());
			log.error(rbe.getResponseMessage(),e);
			return rbe.getResponseMessage();
		}
	}

	@RequestMapping(value = "/stacks", method = RequestMethod.POST)
	public String createStack(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String poolBody, HttpServletResponse response) {
		ObjectMapper mapper = new ObjectMapper();
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.STACK_NEW);
			Map<String, Object> params = mapper.readValue(poolBody, new TypeReference<HashMap<String, Object>>() {
			});
			Stack stack = this.poolResourceService.create(params, authToken, response);
			JsonHelper<Stack, String> jsonHelp = new JsonHelper<Stack, String>();
			String s = jsonHelp.generateJsonBodyWithEmpty(stack);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_STACK, ParamConstant.STACK, stack.getId(),
					Message.SUCCESSED_FLAG, "");
			return s;
		} catch (ResourceBusinessException e) {
			String message = e.getResponseMessage();
			response.setStatus(e.getStatusCode());
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_STACK, ParamConstant.STACK, "",
					Message.FAILED_FLAG, e.getResponseMessage());
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException rbe = new ResourceBusinessException(Message.CS_DEPLOY_POOL_TEMPLATE_CREATE_FAILED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_STACK, ParamConstant.STACK, "", Message.FAILED_FLAG,
					rbe.getResponseMessage());
			log.error(rbe.getResponseMessage(),e);
			return rbe.getResponseMessage();
		}
	}

	@RequestMapping(value = "stacks/{stack_id}", method = RequestMethod.GET)
	public String getStackResources(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String stack_id, HttpServletResponse response) {
		
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<StackResource> list = this.poolResourceService.getResources(stack_id, authToken);
			Stack s = this.stackService.getStack(stack_id, authToken);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_STACK, ParamConstant.STACK, stack_id,
					Message.SUCCESSED_FLAG, "");
			return StackResource.toJSON(s, list, this.poolEntityService, this.imageService, this.networkService, this.subnetService, authToken);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_STACK, ParamConstant.STACK, stack_id,
					Message.FAILED_FLAG, e.getResponseMessage());
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException rbe = new ResourceBusinessException(Message.CS_DEPLOY_POOL_TEMPLATE_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_STACK, ParamConstant.STACK, stack_id, Message.FAILED_FLAG,
					rbe.getResponseMessage());
			log.error(rbe.getResponseMessage(),e);
			return rbe.getResponseMessage();
		}
	}

	@RequestMapping(value = "stacks/{stack_id}", method = RequestMethod.DELETE)
	public String deleteStack(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String stack_id, HttpServletResponse response) {
		
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.STACK_DELETE);
			this.poolResourceService.delete(stack_id, authToken);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_STACK, ParamConstant.STACK, stack_id,
					Message.SUCCESSED_FLAG, "");
			return "success";
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_STACK, ParamConstant.STACK, stack_id,
					Message.FAILED_FLAG, e.getResponseMessage());
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException rbe = new ResourceBusinessException(Message.CS_DEPLOY_POOL_TEMPLATE_DELETE_FAILED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_STACK, ParamConstant.STACK, stack_id, Message.FAILED_FLAG,
					rbe.getResponseMessage());
			log.error(rbe.getResponseMessage(),e);
			return rbe.getResponseMessage();
		}
	}

	@RequestMapping(value = "/stacks", method = RequestMethod.GET)
	public String getStacks(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			HttpServletResponse response) {
	
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<PoolStack> sl = this.poolResourceService.getStackList(null, authToken);
			if (Util.isNullOrEmptyList(sl)) {
				JsonHelper<List<PoolStack>, String> jsonHelp = new JsonHelper<List<PoolStack>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<PoolStack>());
			}
			JsonHelper<List<PoolStack>, String> jsonHelp = new JsonHelper<List<PoolStack>, String>();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_STACKS, ParamConstant.STACK, this.getStacksId(sl),
					Message.SUCCESSED_FLAG, "");
			return jsonHelp.generateJsonBodyWithEmpty(sl);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_STACKS, ParamConstant.STACK, "",
					Message.FAILED_FLAG, e.getResponseMessage());
			log.error(e.getResponseMessage(),e);
			return e.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException rbe = new ResourceBusinessException(Message.CS_DEPLOY_POOL_TEMPLATE_GET_FAILED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_STACKS, ParamConstant.STACK, "", Message.FAILED_FLAG,
					rbe.getResponseMessage());
			log.error(rbe.getResponseMessage(),e);
			return rbe.getResponseMessage();
		}
	}

	@RequestMapping(value = "/pools/config", method = RequestMethod.GET)
	public String getPoolConfig(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			HttpServletResponse response) {
		
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			PoolConfig poolConfig = poolResourceService.getPoolConfig(authToken);
			JsonHelper<PoolConfig, String> jsonHelp = new JsonHelper<PoolConfig, String>();
//			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
//					authToken.getTenantid(), Message.GET_POOLS_CONFIG, ParamConstant.POOL, "",
//					Message.SUCCESSED_FLAG, "");
			return jsonHelp.generateJsonBodyWithEmpty(poolConfig);
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException rbe = new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_POOLS_CONFIG, ParamConstant.POOL, "", Message.FAILED_FLAG,
					rbe.getResponseMessage());
			log.error(rbe.getResponseMessage(),e);
			return rbe.getResponseMessage();
		}
	}

	@RequestMapping(value = "/stacks/config", method = RequestMethod.GET)
	public String getStackConfig(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			HttpServletResponse response) {
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			StackConfig stackConfig = poolResourceService.getStackConfig(authToken);
			JsonHelper<StackConfig, String> jsonHelp = new JsonHelper<StackConfig, String>();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_STACKS_CONFIG, ParamConstant.STACK, "",
					Message.SUCCESSED_FLAG, "");
			return jsonHelp.generateJsonBodyWithEmpty(stackConfig);
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException rbe = new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_STACKS_CONFIG, ParamConstant.STACK, "", Message.FAILED_FLAG,
					rbe.getResponseMessage());
			log.error(rbe.getResponseMessage(),e);
			return rbe.getResponseMessage();
		}
	}
	
//	private String getPoolsId(List<PoolEntity> pl) {
//		if (Util.isNullOrEmptyList(pl))
//			return "";
//		List<String> ids = new ArrayList<String>();
//		for (PoolEntity p : pl) {
//			ids.add(p.getId());
//		}
//		return Util.listToString(ids, ',');
//	}

	private String getStacksId(List<PoolStack> sl) {
		if (Util.isNullOrEmptyList(sl))
			return "";
		List<String> ids = new ArrayList<String>();
		for (PoolStack s : sl) {
			ids.add(s.getId());
		}
		return Util.listToString(ids, ',');
	}
}
