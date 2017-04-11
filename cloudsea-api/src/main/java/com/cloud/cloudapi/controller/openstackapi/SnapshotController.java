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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Image;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.openstackapi.SnapshotService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

/**
 * 
 * snapshot操作
 *
 */
@RestController
public class SnapshotController {

	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private SnapshotService snapshotService;
	
	@Resource
	private AuthService authService;	
	
	private Logger log = LogManager.getLogger(SnapshotController.class);
	
	@RequestMapping(value = "/snapshots", method = RequestMethod.GET)
	public String getSnapshotList(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
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
		try {
			Map<String, String> paramMap = Util.makeRequestParamInfo(limit, name, status, null, null);
			List<Image> snapshots = snapshotService.getSnapshotList(paramMap, authToken);
			if (null == snapshots) {
		//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_COMPUTE_SNAPSHOT,ParamConstant.IMAGE,"",Message.SUCCESSED_FLAG,"");
				JsonHelper<List<Image>, String> jsonHelp = new JsonHelper<List<Image>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Image>());
			}
	//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_COMPUTE_SNAPSHOT,ParamConstant.IMAGE,getImagesId(snapshots),Message.SUCCESSED_FLAG,"");
			JsonHelper<List<Image>, String> jsonHelp = new JsonHelper<List<Image>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(snapshots);

		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_COMPUTE_SNAPSHOT,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_COMPUTE_SNAPSHOT,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_SNAPSHOT_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_COMPUTE_SNAPSHOT,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/snapshots/{id}", method = RequestMethod.GET)
	public String getSnapshot(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
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
		try {
			Image image = snapshotService.getSnapshot(id, authToken);
			if (null == image) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale())); 
				String message = exception.getResponseMessage();
		//		this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_COMPUTE_SNAPSHOT_DETAIL,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
				return message;
			}
		//	this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_COMPUTE_SNAPSHOT_DETAIL,ParamConstant.IMAGE,image.getId(),Message.SUCCESSED_FLAG,"");
			JsonHelper<Image, String> jsonHelp = new JsonHelper<Image, String>();
			return jsonHelp.generateJsonBodySimple(image);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_COMPUTE_SNAPSHOT_DETAIL,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_COMPUTE_SNAPSHOT_DETAIL,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_INSTANCE_SNAPSHOT_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_COMPUTE_SNAPSHOT_DETAIL,ParamConstant.IMAGE,"",Message.FAILED_FLAG,message);
			log.error(message,e);
			return message;
		}
	}

	@RequestMapping(value = "/snapshots", method = RequestMethod.POST)
	public Image createSnapshot() {
		return null;

	}

//	private String getImagesId(List<Image> images){
//		if(Util.isNullOrEmptyList(images))
//			return "";
//		List<String> ids = new ArrayList<String>();
//		for(Image image : images){
//			ids.add(image.getId());
//		}
//		return Util.listToString(ids, ',');
//	}
}
