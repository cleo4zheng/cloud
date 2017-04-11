package com.cloud.cloudapi.controller.common;

import java.util.Locale;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PortalSkin;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.PortalSkinService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class PortalSkinController  extends BaseController {
	
	@Resource
	private OperationLogService operationLogService;

	@Resource
	private PortalSkinService portalSkinService;


	private Logger log = LogManager.getLogger(PortalSkinController.class);
	
	
	@RequestMapping(value = "/skins", method = RequestMethod.GET)
	public String getSkin(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			PortalSkin skin = portalSkinService.getPortakSkin(authToken);
			if(skin == null){
				JsonHelper<PortalSkin, String> jsonHelp = new JsonHelper<PortalSkin, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new PortalSkin());
			}
			JsonHelper<PortalSkin, String> jsonHelp = new JsonHelper<PortalSkin, String>();
			return jsonHelp.generateJsonBodySimple(skin);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_PORTAL_SKIN_DETAIL, ParamConstant.SKIN, "", Message.FAILED_FLAG, message);
			return message;
		}catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_PORTAL_SKIN_DETAIL, ParamConstant.SKIN, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_PORTAL_SKIN_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.GET_PORTAL_SKIN_DETAIL, ParamConstant.SKIN, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	@RequestMapping(value = "/skins", method = RequestMethod.POST)
	public String createSkin(
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
			this.checkUserPermission(authToken, ParamConstant.COMMON_UPDATE_COLOR);
			PortalSkin skin = portalSkinService.createPortalSkin(createBody, authToken);
			if (null == skin) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
						Message.CREATE_PORTAL_SKIN, ParamConstant.SKIN, "", Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_PORTAL_SKIN, ParamConstant.SKIN, skin.getId(), Message.SUCCESSED_FLAG, "");
			JsonHelper<PortalSkin, String> jsonHelp = new JsonHelper<PortalSkin, String>();
			return jsonHelp.generateJsonBodySimple(skin);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_PORTAL_SKIN, ParamConstant.SKIN, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_PORTAL_SKIN, ParamConstant.SKIN, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_PORTAL_SKIN_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),authToken.getTenantid(),
					Message.CREATE_PORTAL_SKIN, ParamConstant.SKIN, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
}
