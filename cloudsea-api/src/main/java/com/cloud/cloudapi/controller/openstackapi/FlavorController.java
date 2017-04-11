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

import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Flavor;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.openstackapi.FlavorService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;


@RestController
public class FlavorController {
	
	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private FlavorService flavorService;
	
	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(FlavorController.class);
	
	@RequestMapping(value = "/flavors", method = RequestMethod.GET)
	public String getFlavors(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value=ParamConstant.LIMIT,defaultValue="") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.TYPE, defaultValue = "") String type,
			HttpServletResponse response) {
		//get ostoken by cuibl
		TokenOs authToken=null;
		try{
		authToken = authService.insertCheckGuiAndOsTokenByEncrypt(guiToken);
		} catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return e.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR).getResponseMessage();
		}		
		try{
			Map<String, String> paramMap = null;
			if (!"".equals(limit)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.LIMIT, limit);
			}
			if (!"".equals(name)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.NAME, name);
			}
			if (!"".equals(type)) {
				if (paramMap == null)
					paramMap = new HashMap<String, String>();
				paramMap.put(ParamConstant.TYPE, type);
			}
			
			List<Flavor> flavors = flavorService.getFlavorList(paramMap, authToken);
			if(Util.isNullOrEmptyList(flavors)){
//				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLAVOR,ParamConstant.FLAVOR,"",Message.SUCCESSED_FLAG,"");
				JsonHelper<List<Flavor>, String> jsonHelp = new JsonHelper<List<Flavor>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Flavor>());
			}
			normalFlavorInfo(flavors);
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLAVOR,ParamConstant.FLAVOR,getFlavorsId(flavors),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<Flavor>, String> jsonHelp = new JsonHelper<List<Flavor>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(flavors);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLAVOR,ParamConstant.FLAVOR,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLAVOR,ParamConstant.FLAVOR,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_FLAVOR_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLAVOR,ParamConstant.FLAVOR,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	@RequestMapping(value = "/flavors/{id}", method = RequestMethod.GET)
	public String getFlavor(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response){
		//get ostoken by cuibl
		TokenOs authToken=null;
		try{
		authToken = authService.insertCheckGuiAndOsTokenByEncrypt(guiToken);
		} catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return e.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR).getResponseMessage();
		}		
		try{
			Flavor flavor = flavorService.getFlavor(id, authToken);
			if(null == flavor){
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
	//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLAVOR_DETAIL,ParamConstant.FLAVOR,"",Message.FAILED_FLAG,message);
				return message;
			}
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLAVOR_DETAIL,ParamConstant.FLAVOR,flavor.getId(),Message.SUCCESSED_FLAG,"");
			JsonHelper<Flavor, String> jsonHelp = new JsonHelper<Flavor, String>();
			return jsonHelp.generateJsonBodyWithEmpty(flavor);	
		}catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLAVOR_DETAIL,ParamConstant.FLAVOR,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLAVOR_DETAIL,ParamConstant.FLAVOR,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_FLAVOR_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_FLAVOR_DETAIL,ParamConstant.FLAVOR,"",Message.FAILED_FLAG,message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	@RequestMapping(value = "/flavors", method = RequestMethod.POST)
	public String createFlavor(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {
		// get ostoken by cuibl
		TokenOs authToken = null;
		try {
			authToken = authService.insertCheckGuiAndOsTokenByEncrypt(guiToken);
		} catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return e.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR).getResponseMessage();
		}
		try {
			Flavor flavor = flavorService.createFlavor(createBody, authToken, ParamConstant.INSTANCE_TYPE);
			if (null == flavor) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
//				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),
//						authToken.getTenantid(), Message.CREATE_FLAVOR, ParamConstant.FLAVOR, "", Message.FAILED_FLAG,
//						message);
				return message;
			}
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),
//					authToken.getTenantid(), Message.CREATE_FLAVOR, ParamConstant.FLAVOR, flavor.getId(),
//					Message.SUCCESSED_FLAG, "");
			JsonHelper<Flavor, String> jsonHelp = new JsonHelper<Flavor, String>();
			return jsonHelp.generateJsonBodyWithEmpty(flavor);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),
//					authToken.getTenantid(), Message.CREATE_FLAVOR, ParamConstant.FLAVOR, "", Message.FAILED_FLAG,
//					message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),
//					authToken.getTenantid(), Message.CREATE_FLAVOR, ParamConstant.FLAVOR, "", Message.FAILED_FLAG,
//					message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_COMPUTE_FLAVOR_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),
//					authToken.getTenantid(), Message.CREATE_FLAVOR, ParamConstant.FLAVOR, "", Message.FAILED_FLAG,
//					message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}

	@RequestMapping(value = "/flavors/{id}/extraspecs", method = RequestMethod.POST)
	public String createFlavorExtraSpec(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String createBody, HttpServletResponse response) {
		// get ostoken by cuibl
		TokenOs authToken = null;
		try {
			authToken = authService.insertCheckGuiAndOsTokenByEncrypt(guiToken);
		} catch (ResourceBusinessException e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return e.getResponseMessage();
		} catch (Exception e) {
			response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
			return new ResourceBusinessException(Message.CS_AUTH_ERROR).getResponseMessage();
		}
		try {
			Flavor flavor = flavorService.createFlavorExtraSpecs(id, createBody, authToken);
			if (null == flavor) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
//				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),
//						authToken.getTenantid(), Message.CREATE_FLAVOR, ParamConstant.FLAVOR, "", Message.FAILED_FLAG,
//						message);
				return message;
			}
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_FLAVOR, ParamConstant.FLAVOR, flavor.getId(),
					Message.SUCCESSED_FLAG, "");
			JsonHelper<Flavor, String> jsonHelp = new JsonHelper<Flavor, String>();
			return jsonHelp.generateJsonBodyWithEmpty(flavor);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),
//					authToken.getTenantid(), Message.CREATE_FLAVOR, ParamConstant.FLAVOR, "", Message.FAILED_FLAG,
//					message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),
//					authToken.getTenantid(), Message.CREATE_FLAVOR, ParamConstant.FLAVOR, "", Message.FAILED_FLAG,
//					message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_COMPUTE_FLAVOR_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken),
//					authToken.getTenantid(), Message.CREATE_FLAVOR, ParamConstant.FLAVOR, "", Message.FAILED_FLAG,
//					message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	private void normalFlavorInfo(List<Flavor> flavors){
		if(Util.isNullOrEmptyList(flavors))
			return;
		for(Flavor flavor : flavors){
			flavor.normalInfo();
		}
	}
//	private String getFlavorsId(List<Flavor> flavors){
//		if(Util.isNullOrEmptyList(flavors))
//			return "";
//		List<String> ids = new ArrayList<String>();
//		for(Flavor flavor : flavors){
//			ids.add(flavor.getId());
//		}
//		return Util.listToString(ids, ',');
//	}
}
