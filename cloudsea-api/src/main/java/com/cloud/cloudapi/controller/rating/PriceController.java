package com.cloud.cloudapi.controller.rating;

import java.util.List;
import java.util.Locale;

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
import com.cloud.cloudapi.pojo.rating.Currency;
import com.cloud.cloudapi.pojo.rating.RatingTemplate;
import com.cloud.cloudapi.pojo.rating.TemplateVersion;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.openstackapi.PriceService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class PriceController  extends BaseController {

	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private PriceService priceService;

	private Logger log = LogManager.getLogger(PriceController.class);
	
	@RequestMapping(value = "/currencies", method = RequestMethod.GET)
	public String getCurrencies(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			HttpServletResponse response) {
		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<Currency> currencies = priceService.getCurrencies();
			JsonHelper<List<Currency>, String> jsonHelp = new JsonHelper<List<Currency>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(currencies);
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_CURRENCY, ParamConstant.CURRENCY, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_CURRENCY_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_CURRENCY, ParamConstant.CURRENCY, "",
					Message.FAILED_FLAG, message);
			return message;
		}
	}

	@RequestMapping(value = "/currencies/{id}", method = RequestMethod.GET)
	public String getCurrency(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			Currency currency = priceService.getCurrency(id);
			JsonHelper<Currency, String> jsonHelp = new JsonHelper<Currency, String>();
			return jsonHelp.generateJsonBodyWithEmpty(currency);
		}  catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_CURRENCY_DETAIL, ParamConstant.CURRENCY, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_CURRENCY_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_CURRENCY_DETAIL, ParamConstant.CURRENCY, "",
					Message.FAILED_FLAG, message);
			return message;
		}
	}

	@RequestMapping(value = "/currencies", method = RequestMethod.POST)
	public String createCurrency(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			priceService.createCurrency(createBody, authToken);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_CURRENCY_CREATE_SUCCESSED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			return message;
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_CURRENCY, ParamConstant.CURRENCY, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_CURRENCY, ParamConstant.CURRENCY, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_CURRENCY_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_CURRENCY, ParamConstant.CURRENCY, "",
					Message.FAILED_FLAG, message);
			return message;
		}

	}
	
	@RequestMapping(value = "/currencies/{id}", method = RequestMethod.DELETE)
	public String deleteCurrency(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {
		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			priceService.deleteCurrency(id);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_CURRENCY_DELETE_SUCCESSED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_CURRENCY, ParamConstant.CURRENCY, id,
					Message.SUCCESSED_FLAG, exception.getResponseMessage());
			return exception.getResponseMessage();
		}  catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_CURRENCY, ParamConstant.CURRENCY, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_CURRENCY_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_CURRENCY, ParamConstant.CURRENCY, "",
					Message.FAILED_FLAG, message);
			return message;
		}
	}
	
	@RequestMapping(value = "/templates", method = RequestMethod.GET)
	public String getTemplates(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<RatingTemplate> templates = priceService.getTemplates(null, authToken);
			JsonHelper<List<RatingTemplate>, String> jsonHelp = new JsonHelper<List<RatingTemplate>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(templates);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_TEMPLATE, ParamConstant.RATING_TEMPLATE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_TEMPLATE, ParamConstant.RATING_TEMPLATE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_RATING_TEMPLATE_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_TEMPLATE, ParamConstant.RATING_TEMPLATE, "",
					Message.FAILED_FLAG, message);
			return message;
		}
	}
	
	@RequestMapping(value = "/templates/{id}", method = RequestMethod.GET)
	public String getTemplate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs authToken = null;	
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			RatingTemplate template = priceService.getTemplate(id, authToken);
			JsonHelper<RatingTemplate, String> jsonHelp = new JsonHelper<RatingTemplate, String>();
			return jsonHelp.generateJsonBodyWithEmpty(template);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_TEMPLATE_DETAIL, ParamConstant.RATING_TEMPLATE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_TEMPLATE_DETAIL, ParamConstant.RATING_TEMPLATE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_RATING_TEMPLATE_DETAILE_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.GET_RATING_TEMPLATE_DETAIL, ParamConstant.RATING_TEMPLATE, "",
					Message.FAILED_FLAG, message);
			return message;
		}
	}
	
	@RequestMapping(value = "/templates", method = RequestMethod.POST)
	public String createTemplate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.PRICE_NEW);
			RatingTemplate template = priceService.addTemplate(createBody, authToken);
			if (null == template) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
						authToken.getTenantid(), Message.CREATE_RATING_TEMPLATE, ParamConstant.RATING_TEMPLATE, "",
						Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_RATING_TEMPLATE, ParamConstant.RATING_TEMPLATE,
					template.getTemplate_id(), Message.SUCCESSED_FLAG, "");
			JsonHelper<RatingTemplate, String> jsonHelp = new JsonHelper<RatingTemplate, String>();
			return jsonHelp.generateJsonBodyWithEmpty(template);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_RATING_TEMPLATE, ParamConstant.RATING_TEMPLATE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_RATING_TEMPLATE, ParamConstant.RATING_TEMPLATE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_RATING_TEMPLATE_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_RATING_TEMPLATE, ParamConstant.RATING_TEMPLATE, "",
					Message.FAILED_FLAG, message);
			return message;
		}
	}
	
	@RequestMapping(value = "/templates/{id}", method = RequestMethod.DELETE)
	public String deleteTemplate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.PRICE_DELETE);

			priceService.deleteTemplate(id, authToken);
			if (null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_RATING_TEMPLATE_DELETE_SUCCESSED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_RATING_TEMPLATE, ParamConstant.RATING_TEMPLATE, id,
					Message.SUCCESSED_FLAG, exception.getResponseMessage());
			return exception.getResponseMessage();
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_RATING_TEMPLATE, ParamConstant.RATING_TEMPLATE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_RATING_TEMPLATE, ParamConstant.RATING_TEMPLATE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_RATING_TEMPLATE_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_RATING_TEMPLATE, ParamConstant.RATING_TEMPLATE, "",
					Message.FAILED_FLAG, message);
			return message;
		}
	}
	
	@RequestMapping(value = "/templates/{id}", method = RequestMethod.PUT)
	public String updateTemplate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String body, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.PRICE_DELETE);

			RatingTemplate template = priceService.updateTemplate(id, body, authToken);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_UPDATE_SUCCESSED,new Locale(authToken.getLocale()));
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_RATING_TEMPLATE, ParamConstant.RATING_TEMPLATE, id, Message.SUCCESSED_FLAG,
					exception.getResponseMessage());
			JsonHelper<RatingTemplate, String> jsonHelp = new JsonHelper<RatingTemplate, String>();
			return jsonHelp.generateJsonBodyWithEmpty(template);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_RATING_TEMPLATE, ParamConstant.RATING_TEMPLATE, "", Message.FAILED_FLAG,
					message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_RATING_TEMPLATE, ParamConstant.RATING_TEMPLATE, "", Message.FAILED_FLAG,
					message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_RATING_TEMPLATE_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.UPDATE_RATING_TEMPLATE, ParamConstant.RATING_TEMPLATE, "", Message.FAILED_FLAG,
					message);
			return message;
		}
	}
	
	@RequestMapping(value = "/rating-versions/{id}", method = RequestMethod.POST)
	public String createRatingVersion(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String createBody, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.PRICE_NEW_VERSION);
			TemplateVersion version = priceService.addTemplateVersion(id, createBody, authToken);
			if (null == version) {
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
						authToken.getTenantid(), Message.CREATE_RATING_VERSION, ParamConstant.RATING_VERSION, "",
						Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_RATING_VERSION, ParamConstant.RATING_VERSION,
					version.getVersion_id(), Message.SUCCESSED_FLAG, "");
			JsonHelper<TemplateVersion, String> jsonHelp = new JsonHelper<TemplateVersion, String>();
			return jsonHelp.generateJsonBodyWithEmpty(version);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_RATING_VERSION, ParamConstant.RATING_VERSION, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_RATING_VERSION, ParamConstant.RATING_VERSION, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_RATING_VERSION_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_RATING_VERSION, ParamConstant.RATING_VERSION, "",
					Message.FAILED_FLAG, message);
			return message;
		}

	}
}
