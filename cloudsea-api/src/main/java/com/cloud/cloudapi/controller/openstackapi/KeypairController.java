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
import com.cloud.cloudapi.pojo.openstackapi.forgui.Keypair;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.openstackapi.KeypairService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class KeypairController  extends BaseController {

	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private KeypairService keypairService;
	
	private Logger log = LogManager.getLogger(KeypairController.class);
	
	@RequestMapping(value = "/keypairs", method = RequestMethod.GET)
	public String getKeypairList(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name, HttpServletResponse response) {
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
    			paramMap.put(ParamConstant.LIMIT, limit);
    		}

    		if (!"".equals(name)) {
    			if (null == paramMap)
    				paramMap = new HashMap<String, String>();
    			paramMap.put(ParamConstant.OWNER, name);
    		}
    		
    		List<Keypair> keypairs = keypairService.getKeypairList(paramMap, authToken);
    		if(null == keypairs){
    	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_KEYPAIR,ParamConstant.KEYPAIR,"",Message.SUCCESSED_FLAG,"");
    			JsonHelper<List<Keypair>, String> jsonHelp = new JsonHelper<List<Keypair>, String>();
    			return jsonHelp.generateJsonBodySimple(new ArrayList<Keypair>());
    		}
    		normalKeypairsInfo(keypairs);
    //		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_KEYPAIR,ParamConstant.KEYPAIR,getKeypairsId(keypairs),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<Keypair>, String> jsonHelp = new JsonHelper<List<Keypair>, String>();
			return jsonHelp.generateJsonBodySimple(keypairs);
    		
        } catch (ResourceBusinessException e) {
        	response.setStatus(e.getStatusCode());
        	String message = e.getResponseMessage();
        	this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_KEYPAIR,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
        	log.error(message,e);
        	return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
        	this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_KEYPAIR,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
        	log.error(message,e);
        	return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_KEYPAIR_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
        	this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_KEYPAIR,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
        	log.error(message,e);
        	return message;
		}
	}

	@RequestMapping(value = "/keypairs/{name}", method = RequestMethod.GET)
	public String getKeypair(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String name,HttpServletResponse response) {
		
		TokenOs authToken=null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Keypair keypair = keypairService.getKeypair(name,authToken);
			if(null == keypair){
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
		//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_KEYPAIR_DETAIL,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
				return message;
			}
			normalKeypairInfo(keypair,true);
		//	this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_KEYPAIR_DETAIL,ParamConstant.KEYPAIR,keypair.getId(),Message.SUCCESSED_FLAG,"");
			JsonHelper<Keypair, String> jsonHelp = new JsonHelper<Keypair, String>();
			return jsonHelp.generateJsonBodySimple(keypair);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_KEYPAIR_DETAIL,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_KEYPAIR_DETAIL,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_KEYPAIR_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_KEYPAIR_DETAIL,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/keypairs/download/{name}", method = RequestMethod.GET)
	public String downloadKeypair(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String name,HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
	//		this.checkUserPermission(authToken, ParamConstant.KEYPAIR_DOWNLOAD);
			Keypair keypair = keypairService.downloadKeypair(name,authToken);
			String privateKey = keypair.getPrivate_key();
			//normalKeypairInfo(keypair,false);
			if(!Util.isNullOrEmptyValue(privateKey)){
				privateKey = privateKey.replace("\\n", "\n");
			//	keypair.setPrivate_key(privateKey);
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_KEYPAIR_DETAIL,ParamConstant.KEYPAIR,keypair.getName(),Message.SUCCESSED_FLAG,"");
			//JsonHelper<Keypair, String> jsonHelp = new JsonHelper<Keypair, String>();
			return new JsonHelper<String,String>().generateJsonBodySimple(privateKey,"private_key");
			//return jsonHelp.generateJsonBodySimple(keypair);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_KEYPAIR_DOWNLOAD,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_KEYPAIR_DOWNLOAD,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_KEYPAIR_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_KEYPAIR_DOWNLOAD,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/keypairs", method = RequestMethod.POST)
	public String createKeypair(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody,HttpServletResponse response) {

		TokenOs authToken=null;
		try{
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.KEYPAIR_NEW);

			Keypair keypair = keypairService.createKeypair(createBody, authToken);
			if(null == keypair){
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.CREATE_KEYPAIR,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
				return message;
			}
			normalKeypairInfo(keypair,true);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_KEYPAIR,ParamConstant.KEYPAIR,keypair.getName(),Message.SUCCESSED_FLAG,"");
			JsonHelper<Keypair, String> jsonHelp = new JsonHelper<Keypair, String>();
			return jsonHelp.generateJsonBodyWithEmpty(keypair);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.CREATE_KEYPAIR,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_KEYPAIR,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_KEYPAIR_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_KEYPAIR,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/keypairs", method = RequestMethod.PUT)
	public String uploadKeypair(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody,HttpServletResponse response) {

		TokenOs authToken=null;
		try{
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.KEYPAIR_UPLOAD);
			Keypair keypair = keypairService.uploadKeypair(createBody, authToken);
			if(null == keypair){
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.CREATE_KEYPAIR,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
				return message;
			}
			normalKeypairInfo(keypair,true);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_KEYPAIR,ParamConstant.KEYPAIR,keypair.getName(),Message.SUCCESSED_FLAG,"");
			JsonHelper<Keypair, String> jsonHelp = new JsonHelper<Keypair, String>();
			return jsonHelp.generateJsonBodyWithEmpty(keypair);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(), Message.CREATE_KEYPAIR,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_KEYPAIR,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_KEYPAIR_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_KEYPAIR,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/keypairs/{name}", method = RequestMethod.DELETE)
	public String deleteKeypair(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String name,HttpServletResponse response) {

		TokenOs authToken=null;
		try{
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.KEYPAIR_DELETE);
			//String keypairId = keypairService.deleteKeypair(name, authToken);
			//if(null != response)
			//	response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_KEYPAIR,ParamConstant.KEYPAIR,"",Message.SUCCESSED_FLAG,"");
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_KEYPAIR_DELETE_SUCCESSED,new Locale(authToken.getLocale()));
			return exception.getResponseMessage();
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_KEYPAIR,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_KEYPAIR,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_KEYPAIR_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_KEYPAIR,ParamConstant.KEYPAIR,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}
	
	private void normalKeypairInfo(Keypair keypair,Boolean normalPrivateInfo){
		keypair.normalInfo();
	}
	
	private void normalKeypairsInfo(List<Keypair> keypairs){
		if(Util.isNullOrEmptyList(keypairs))
			return;
		for(Keypair keypair : keypairs){
			normalKeypairInfo(keypair,true);
		}
	}
}
