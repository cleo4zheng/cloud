package com.cloud.cloudapi.controller.openstackapi;

import java.util.ArrayList;
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
import com.cloud.cloudapi.pojo.openstackapi.forgui.HardWare;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PhysNode;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.openstackapi.InstanceService;
import com.cloud.cloudapi.service.openstackapi.PhysNodeService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

/**
 * 
 * @author tanggc
 * 物理机controller
 * 包括ironic node和instance
 */
@RestController
public class PhysNodeController extends BaseController{
	@Resource
	private OperationLogService operationLogService;

	@Resource
	private PhysNodeService physNodeService;

	@Resource
	private AuthService authService;
	
	@Resource
	private InstanceService instanceService;
	
	private Logger log = LogManager.getLogger(PhysNodeController.class);
	//实验性的创建物理机接口，暂时不可用。
	@RequestMapping(value = "/bare-metals/instancenew", method = RequestMethod.POST )
	public String createInstancenew(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {
		//get ostoken by cuibl
		TokenOs authToken=null;	
		try{
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
		    List<Instance> createdInstances = instanceService.createInstance(createBody, ParamConstant.BAREMETAL_TYPE,authToken);
		    this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VDI_INSTANCE,ParamConstant.INSTANCE,getInstancesId(createdInstances),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<Instance>, String> jsonHelp = new JsonHelper<List<Instance>, String>();
			return jsonHelp.generateJsonBodySimple(createdInstances);
		}catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VDI_INSTANCE,ParamConstant.INSTANCE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}  catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VDI_INSTANCE,ParamConstant.INSTANCE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_VDI_INSTANCE_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_VDI_INSTANCE,ParamConstant.INSTANCE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	
	@RequestMapping(value = "/bare-metals/instances", method = RequestMethod.GET)
	//获取物理机实例一览
	public String getInstances(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			@RequestParam(value = ParamConstant.IMAGE_TYPE, defaultValue = "") String imageid,HttpServletResponse response){
		//TODO tanggc
		TokenOs authToken=null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Map<String, String> paramMap = Util.makeRequestParamInfo(limit,name,status,ParamConstant.IMAGE_TYPE,imageid);
			List<Instance> instances = instanceService.getVDIInstanceList(paramMap, ParamConstant.BAREMETAL_TYPE,authToken);
			if(Util.isNullOrEmptyList(instances)){
	//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VDI_INSTANCE,ParamConstant.INSTANCE,"",Message.SUCCESSED_FLAG,"");
				JsonHelper<List<Instance>, String> jsonHelp = new JsonHelper<List<Instance>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Instance>());
			}
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VDI_INSTANCE,ParamConstant.INSTANCE,getInstancesId(instances),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<Instance>, String> jsonHelp = new JsonHelper<List<Instance>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(instances);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VDI_INSTANCE,ParamConstant.INSTANCE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message =  exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VDI_INSTANCE,ParamConstant.INSTANCE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_VDI_INSTANCE_GET_FAILED,new Locale(authToken.getLocale()));
			String message =  exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_VDI_INSTANCE,ParamConstant.INSTANCE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/bare-metals/instances/{uuid}", method = RequestMethod.GET)
	//获取物理机实例详细信息
	public String getInstance(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String uuid, HttpServletResponse response) {
		// TODO tanggc
		// get ostoken by cuibl
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if (null == authToken) {
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,
						new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Instance instance = instanceService.getInstance(uuid, ParamConstant.BAREMETAL_TYPE, authToken, true);
			// this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),
			// authToken.getTenantid(),Message.CREATE_VDI_INSTANCE,ParamConstant.INSTANCE,id,Message.SUCCESSED_FLAG,"");
			JsonHelper<Instance, String> jsonHelp = new JsonHelper<Instance, String>();
			return jsonHelp.generateJsonBodySimple(instance);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_VDI_INSTANCE, ParamConstant.INSTANCE, "", Message.FAILED_FLAG,
					message);
			log.error(message, e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,
					new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_VDI_INSTANCE, ParamConstant.INSTANCE, "", Message.FAILED_FLAG,
					message);
			log.error(message, e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_COMPUTE_VDI_INSTANCE_DETAIL_GET_FAILED, new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_VDI_INSTANCE, ParamConstant.INSTANCE, "", Message.FAILED_FLAG,
					message);
			log.error(message, e);
			return message;
		}
	}

	@RequestMapping(value = "/bare-metals/instances" , method = RequestMethod.POST)
	//创建物理机实例
	public String createInstance(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody,HttpServletResponse response){
		
		TokenOs authToken = null;
		
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.BARE_METAL_INSTANCE_NEW);
			String instanceStr = physNodeService.createInstance(createBody, authToken);
			if (null == instanceStr) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.CREATE_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG,
						message);
				return message;
			}
			return instanceStr;

		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NODE_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			
			return message;
		}
	}
	
	@RequestMapping(value = "/bare-metals/flavors" , method = RequestMethod.GET)
	//返回目前可用的物理机规格信息，供申请物理机时调用
	public String getAvailableSpec(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			HttpServletResponse response){
		
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<HardWare> hardWares = physNodeService.getAvailableSpec(authToken);
			if (Util.isNullOrEmptyList(hardWares)) {	
				hardWares = new ArrayList<HardWare>();
			}
			JsonHelper<List<HardWare>, String> jsonHelp = new JsonHelper<List<HardWare>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(hardWares);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NODE_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
		
	}
	
	@RequestMapping(value = "/bare-metals", method = RequestMethod.GET)
	public String getNodes(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
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
			Map<String, String> paramMap = Util.makeRequestParamInfo(limit, name, status, "", "");
			List<PhysNode> physNodes = physNodeService.getPhysNodes(paramMap, authToken);
			if (Util.isNullOrEmptyList(physNodes)) {
//				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//						Message.GET_NODE, ParamConstant.NODE, "", Message.SUCCESSED_FLAG, "");
				JsonHelper<List<PhysNode>, String> jsonHelp = new JsonHelper<List<PhysNode>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<PhysNode>());
			}
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//					Message.GET_NODE, ParamConstant.NODE, getPhysNodesId(physNodes),
//					Message.SUCCESSED_FLAG, "");
			JsonHelper<List<PhysNode>, String> jsonHelp = new JsonHelper<List<PhysNode>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(physNodes);

		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NODE_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/bare-metals/{uuid}", method = RequestMethod.GET)
	public String getNode(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String uuid, HttpServletResponse response) {
		// get ostoken by cuibl
		TokenOs authToken = null;

		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			PhysNode physNode = physNodeService.getPhysNode(uuid, authToken);
			if (null == physNode) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
//				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//						Message.GET_NODE_DETAIL, ParamConstant.NODE, "", Message.FAILED_FLAG,
//						message);
				return message;
			}
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
//					Message.GET_NODE_DETAIL, ParamConstant.NODE, uuid, Message.SUCCESSED_FLAG, "");
			JsonHelper<PhysNode, String> jsonHelp = new JsonHelper<PhysNode, String>();
			return jsonHelp.generateJsonBodyWithEmpty(physNode);

		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_NODE_DETAIL, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_NODE_DETAIL, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NODE_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_NODE_DETAIL, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/bare-metals", method = RequestMethod.POST)
	public String createNode(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody,HttpServletResponse response) {
		// get ostoken by cuibl
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.BARE_METAL_NEW);
			PhysNode physNode = physNodeService.createPhysNode(createBody, authToken);
			if (null == physNode) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.CREATE_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG,
						message);
				return message;
			}
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_NODE, ParamConstant.NODE, physNode.getUuid(), Message.SUCCESSED_FLAG, "");
			JsonHelper<PhysNode, String> jsonHelp = new JsonHelper<PhysNode, String>();
			return jsonHelp.generateJsonBodyWithEmpty(physNode);

		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NODE_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/bare-metals/{uuid}", method = RequestMethod.PATCH)
	public String updateNode(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String uuid, @RequestBody String updateBody,HttpServletResponse response) {
		// get ostoken by cuibl
		TokenOs authToken = null;

		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.BARE_METAL_NEW);
			PhysNode physNode = physNodeService.updatePhysNode(uuid,updateBody, authToken);
			if (null == physNode) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.UPDATE_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG,
						message);
				return message;
			}
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_NODE, ParamConstant.NODE, physNode.getUuid(), Message.SUCCESSED_FLAG, "");
			JsonHelper<PhysNode, String> jsonHelp = new JsonHelper<PhysNode, String>();
			return jsonHelp.generateJsonBodyWithEmpty(physNode);

		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NODE_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/bare-metals/{uuid}", method = RequestMethod.PUT)
	public String changeNodePower(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String uuid, @RequestBody String updateBody,HttpServletResponse response) {
		// get ostoken by cuibl
		TokenOs authToken = null;

		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.BARE_METAL_STOP);
			physNodeService.changePhysNodePowerStates(uuid,updateBody, authToken);
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_NODE,ParamConstant.NODE,uuid,Message.SUCCESSED_FLAG,"");
		    ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NODE_UPDATE_POWER_STATES_SUCCESSED,new Locale(authToken.getLocale()));
			return exception.getResponseMessage();

		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_NODE_UPDATE_POWER_STATES_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.UPDATE_NODE, ParamConstant.NODE, "", Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
    @RequestMapping(value="/bare-metals/{uuid}",method=RequestMethod.DELETE)
    public String  deleteNode(@RequestHeader(value=ParamConstant.AUTH_TOKEN,defaultValue="nownoimpl") String guiToken,
    		@PathVariable String uuid,HttpServletResponse response) {
		//get ostoken by cuibl
		TokenOs authToken=null;
    	try{
    		authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
    		this.checkUserPermission(authToken, ParamConstant.BARE_METAL_DELETE);
    		physNodeService.deletePhysNode(uuid, authToken);
			if(null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
        	ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NODE_DELETE_FAILED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_NODE,ParamConstant.NODE,uuid,Message.SUCCESSED_FLAG,exception.getResponseMessage());
			return exception.getResponseMessage();
     	} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_NODE,ParamConstant.NODE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.DELETE_NODE,ParamConstant.NODE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NODE_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.DELETE_NODE,ParamConstant.NODE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
    }
    
    @RequestMapping(value="/bare-metals/delete",method=RequestMethod.DELETE)
    //一次删除多个裸机
    public String deleteNodes(@RequestHeader(value=ParamConstant.AUTH_TOKEN,defaultValue="nownoimpl") String guiToken,
    		@RequestBody String deleteBody,HttpServletResponse response){
		TokenOs authToken=null;
    	try{
    		authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
    		this.checkUserPermission(authToken, ParamConstant.BARE_METAL_DELETE);
    		String id = physNodeService.deletePhysNodes(deleteBody, authToken);
			if(null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_NODE,ParamConstant.NODE,id,Message.SUCCESSED_FLAG,"");
        	ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NODE_DELETE_SUCCESSED,new Locale(authToken.getLocale()));
			return exception.getResponseMessage();
     	} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_NODE,ParamConstant.NODE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.DELETE_NODE,ParamConstant.NODE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NODE_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.DELETE_NODE,ParamConstant.NODE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
    }
    
    
    //一次开机/关机/重启 多台裸机
    @RequestMapping(value="/bare-metals/power/{action}",method=RequestMethod.PUT)
    public String chageNodesPower(@RequestHeader(value=ParamConstant.AUTH_TOKEN,defaultValue="nownoimpl") String guiToken,
    		@PathVariable String action,@RequestBody String actionBody, HttpServletResponse response){
    	TokenOs authToken=null;
    	try{
    		authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
    		this.checkUserPermission(authToken, ParamConstant.BARE_METAL_STOP);
    		String id = physNodeService.changeNodespower(actionBody, action, authToken);
			if(null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_NODE,ParamConstant.NODE,id,Message.SUCCESSED_FLAG,"");
        	ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NODE_UPDATE_POWER_STATES_SUCCESSED,new Locale(authToken.getLocale()));
			return exception.getResponseMessage();
     	} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_NODE,ParamConstant.NODE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.UPDATE_NODE,ParamConstant.NODE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NODE_UPDATE_POWER_STATES_SUCCESSED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.UPDATE_NODE,ParamConstant.NODE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
    }
    
//	private String getPhysNodesId(List<PhysNode> physNodes) {
//		if (Util.isNullOrEmptyList(physNodes))
//			return "";
//		List<String> ids = new ArrayList<String>();
//		for (PhysNode physNode : physNodes) {
//			ids.add(physNode.getUuid());
//		}
//		return Util.listToString(ids, ',');
//	}
	
	private String getInstancesId(List<Instance> instances){
		if(Util.isNullOrEmptyList(instances))
			return "";
		List<String> ids = new ArrayList<String>();
		for(Instance instance : instances){
			ids.add(instance.getId());
		}
		return Util.listToString(ids, ',');
	}
}
