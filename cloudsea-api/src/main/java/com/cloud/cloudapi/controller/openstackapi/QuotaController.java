package com.cloud.cloudapi.controller.openstackapi;

import java.util.ArrayList;
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
import com.cloud.cloudapi.json.forgui.TenantQuotaJSON;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Quota;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceSpec;
import com.cloud.cloudapi.pojo.quota.QuotaTemplate;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.openstackapi.OsApiServiceFactory;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class QuotaController  extends BaseController {

	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private QuotaService quotaService;

	private Logger log = LogManager.getLogger(QuotaController.class);
	
	@RequestMapping(value = "/quotas", method = RequestMethod.GET)
	public String getQuotas(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			authToken = this.getUserOsToken(guiToken);
			
			List<Quota> quotas = quotaService.getQuotas(null, authToken);
			if (Util.isNullOrEmptyList(quotas)) {
//				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_QUOTA, ParamConstant.QUOTA, "",
//						Message.SUCCESSED_FLAG, "");
				JsonHelper<List<Quota>, String> jsonHelp = new JsonHelper<List<Quota>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Quota>());
			}
			normalQuotaInfo(quotas);
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_QUOTA, ParamConstant.QUOTA,
//					getQuotasId(quotos), Message.SUCCESSED_FLAG, "");
			return getQuotaDisplayInfo(quotas);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_QUOTA, ParamConstant.QUOTA, "",
//					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_QUOTA, ParamConstant.QUOTA, "",
//					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_QUOTA_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_QUOTA, ParamConstant.QUOTA, "",
//					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}

	
	@RequestMapping(value = "/tenantQuotas", method = RequestMethod.GET)
	public String getTenantQuota(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.INSTANCE_TYPE, defaultValue = "") String instanceType,
			@RequestParam(value = ParamConstant.VOLUME_TYPE, defaultValue = "") String volumeTypeId,
			@RequestParam(value = ParamConstant.FLOATING_TYPE, defaultValue = "") String floatingType,HttpServletResponse response) {
	
		TokenOs authToken=null;
		try {
			authToken = this.getUserOsToken(guiToken);

			List<ResourceSpec> resourceSpec = quotaService.getTenantQuota(authToken, instanceType,volumeTypeId,floatingType);
			JsonHelper<List<ResourceSpec>, String> jsonHelp = new JsonHelper<List<ResourceSpec>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(resourceSpec);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_QUOTA_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		}
	}
	
	
	@RequestMapping(value = "/quota-templates", method = RequestMethod.GET)
	public String getQuotaTemplates(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			authToken = this.getUserOsToken(guiToken);

			List<QuotaTemplate> templates = quotaService.getQuotaTemplates(authToken);
			normalQuotaTemplateInfo(templates);
			JsonHelper<List<QuotaTemplate>, String> jsonHelp = new JsonHelper<List<QuotaTemplate>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(templates);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_QUOTA_TEMPLATE_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/quota-templates/{id}", method = RequestMethod.DELETE)
	public String deleteQuotaTemplate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			 authToken = this.getUserOsToken(guiToken);
			 quotaService.deleteQuotaTemplate(id, authToken);
			 JsonHelper<QuotaTemplate, String> jsonHelp = new JsonHelper<QuotaTemplate, String>();
		     return jsonHelp.generateJsonBodyWithEmpty(new QuotaTemplate());
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_QUOTA_TEMPLATE_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/quota-templates", method = RequestMethod.POST)
	public String createQuotaTemplate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			 authToken = this.getUserOsToken(guiToken);
			 QuotaTemplate template  = quotaService.createQuotaTemplate(createBody, authToken);
			 template.normalInfo();
			 JsonHelper<QuotaTemplate, String> jsonHelp = new JsonHelper<QuotaTemplate, String>();
		     return jsonHelp.generateJsonBodyWithEmpty(template);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_QUOTA_TEMPLATE_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/quota-templates/{id}/{tenantId}", method = RequestMethod.PUT)
	public String applyQuotaTemplate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @PathVariable String tenantId, HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			 authToken = this.getUserOsToken(guiToken);
			 quotaService.applyQuotaTemplate(id,tenantId, authToken);
			 JsonHelper<QuotaTemplate, String> jsonHelp = new JsonHelper<QuotaTemplate, String>();
		     return jsonHelp.generateJsonBodyWithEmpty(new QuotaTemplate());
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_QUOTA_TEMPLATE_APPLY_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/quota-templates/{id}", method = RequestMethod.PUT)
	public String updateQuotaTemplate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, @RequestBody String updateBody, HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			 authToken = this.getUserOsToken(guiToken);
			 QuotaTemplate template = quotaService.updateQuotaTemplate(id,updateBody, authToken);
			 template.normalInfo();
			 JsonHelper<QuotaTemplate, String> jsonHelp = new JsonHelper<QuotaTemplate, String>();
		     return jsonHelp.generateJsonBodyWithEmpty(template);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_QUOTA_TEMPLATE_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/quota-fields", method = RequestMethod.GET)
	public String getQuotaFields(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			authToken = this.getUserOsToken(guiToken);
			QuotaTemplate template = quotaService.getQuotaTemplateFields(authToken);
			template.normalInfo();
			JsonHelper<QuotaTemplate, String> jsonHelp = new JsonHelper<QuotaTemplate, String>();
			return jsonHelp.generateJsonBodyWithEmpty(template);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_QUOTA_FIELD_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		}
	}
	
	
	@RequestMapping(value = "/quota-templates/{id}", method = RequestMethod.GET)
	public String getQuotaTemplate(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			authToken = this.getUserOsToken(guiToken);
			QuotaTemplate template = quotaService.getQuotaTemplate(id,authToken);
			template.normalInfo();
			JsonHelper<QuotaTemplate, String> jsonHelp = new JsonHelper<QuotaTemplate, String>();
			return jsonHelp.generateJsonBodyWithEmpty(template);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_QUOTA_TEMPLATE_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/compute-quotas", method = RequestMethod.PUT)
	public String uptateComputeQuota(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String body,HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			authToken = this.getUserOsToken(guiToken);
			Quota quoto = quotaService.updateComputeQuota(body, authToken);
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_COMPUTE_QUOTA, ParamConstant.QUOTA,
//					quoto.getId(), Message.SUCCESSED_FLAG, "");
			JsonHelper<Quota, String> jsonHelp = new JsonHelper<Quota, String>();
			return jsonHelp.generateJsonBodyWithEmpty(quoto);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_COMPUTE_QUOTA, ParamConstant.QUOTA, "",
//					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_COMPUTE_QUOTA, ParamConstant.QUOTA, "",
//					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_COMPUTE_QUOTA_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_COMPUTE_QUOTA, ParamConstant.QUOTA, "",
//					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/network-quotas", method = RequestMethod.PUT)
	public String uptateNetworkQuota(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String body,HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			authToken = this.getUserOsToken(guiToken);

			Quota quoto = quotaService.updateNetworkQuota(body, authToken);
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_NETWORK_QUOTA, ParamConstant.QUOTA,
//					quoto.getId(), Message.SUCCESSED_FLAG, "");
			JsonHelper<Quota, String> jsonHelp = new JsonHelper<Quota, String>();
			return jsonHelp.generateJsonBodyWithEmpty(quoto);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_NETWORK_QUOTA, ParamConstant.QUOTA, "",
//					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_NETWORK_QUOTA, ParamConstant.QUOTA, "",
//					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_QUOTA_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_NETWORK_QUOTA, ParamConstant.QUOTA, "",
//					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/storage-quotas", method = RequestMethod.PUT)
	public String uptateStorageQuota(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String body,HttpServletResponse response) {

		TokenOs authToken=null;
		try {
			authToken = this.getUserOsToken(guiToken);

			Quota quoto = quotaService.updateNetworkQuota(body, authToken);
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_NETWORK_QUOTA, ParamConstant.QUOTA,
//					quoto.getId(), Message.SUCCESSED_FLAG, "");
			JsonHelper<Quota, String> jsonHelp = new JsonHelper<Quota, String>();
			return jsonHelp.generateJsonBodyWithEmpty(quoto);
		} catch (ResourceBusinessException e) {
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_NETWORK_QUOTA, ParamConstant.QUOTA, "",
//					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_NETWORK_QUOTA, ParamConstant.QUOTA, "",
//					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_NETWORK_QUOTA_UPDATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.UPDATE_NETWORK_QUOTA, ParamConstant.QUOTA, "",
//					Message.FAILED_FLAG, message);
			log.error(message,e);
			return message;
		}
	}
	
	@RequestMapping(value = "/storage-quotas", method = RequestMethod.GET)
	public String getStorageQuotas(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			HttpServletResponse response) {
		return "";
	}

	@RequestMapping(value = "/update-hard-quota", method = RequestMethod.POST)
	public Boolean setHardQuota(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken) {
		TokenOs authToken=null;
		authToken = this.getUserOsToken(guiToken);
		QuotaService resService = OsApiServiceFactory.getQuotaService();
		return resService.setHardQuota(null, authToken);
	}

	
	private String getQuotaDisplayInfo(List<Quota> quotos){
		List<Quota> computeQuotas = new ArrayList<Quota>();
		List<Quota> otherQuotas = new ArrayList<Quota>();
//		String computeType = Message.getMessage(Message.CS_QUOTA_COMPUTE_TYPE, false);

		for(Quota quota : quotos){
			if(quota.getQuotaType().equalsIgnoreCase(ParamConstant.COMPUTE)){
				computeQuotas.add(quota);
			}else {
				otherQuotas.add(quota);
			}
		}
		
		TenantQuotaJSON tenantQuota = new TenantQuotaJSON(computeQuotas,otherQuotas);
		JsonHelper<TenantQuotaJSON, String> jsonHelp = new JsonHelper<TenantQuotaJSON, String>();
		return jsonHelp.generateJsonBodyWithEmpty(tenantQuota);
	}
	
	private void normalQuotaInfo(List<Quota> quotas){
		if(Util.isNullOrEmptyList(quotas))
			return;
		for(Quota quota : quotas){
			quota.normalInfo();
		}
	}
	
	private void normalQuotaTemplateInfo(List<QuotaTemplate> templates){
		if(Util.isNullOrEmptyList(templates))
			return;
		for(QuotaTemplate template : templates)
			template.normalInfo();
	}
}
