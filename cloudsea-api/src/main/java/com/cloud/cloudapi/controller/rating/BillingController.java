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
import org.springframework.web.bind.annotation.RestController;

import com.cloud.cloudapi.controller.common.BaseController;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.rating.Billing;
import com.cloud.cloudapi.pojo.rating.BillingReport;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.common.OperationResourceService;
import com.cloud.cloudapi.service.rating.BillingService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class BillingController   extends BaseController {
	
	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private OperationResourceService operationResourceService;
	
	@Resource
	private BillingService billingService;

	private Logger log = LogManager.getLogger(BillingController.class);
	
	@RequestMapping(value = "/billing-statistic", method = RequestMethod.GET)
	public String getBillingStatistics(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestHeader(value = ParamConstant.TENANT_ID, defaultValue = "") String tenantId,
			@RequestHeader(value = ParamConstant.BILLING_MONTH_UNTIL, defaultValue = "") String billingMonthUntil,
			HttpServletResponse response) {
		

		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<BillingReport> reports = billingService.getBillStatistics(tenantId,billingMonthUntil,authToken);
			normalBillingReports(reports);
			JsonHelper<List<BillingReport>, String> jsonHelp = new JsonHelper<List<BillingReport>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(reports);

		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_BILLING_INFO,ParamConstant.BILLING,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_BILLING_INFO,ParamConstant.BILLING,"",Message.FAILED_FLAG,message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_BILLING_SIMPLE_REPORT_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_BILLING_INFO,ParamConstant.BILLING,"",Message.FAILED_FLAG,message);
			return message;
		}
	}
	
	@RequestMapping(value = "/billing-details", method = RequestMethod.GET)
	public String getBillingDetails(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestHeader(value = ParamConstant.TENANT_ID, defaultValue = "") String tenantId,
			@RequestHeader(value = ParamConstant.BILLING_MONTH, defaultValue = "") String billingMonth,
			HttpServletResponse response) {
		

		TokenOs authToken=null;			
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			BillingReport report = billingService.getBillDetails(tenantId,billingMonth,authToken);
			report.normalInfo(false);
			JsonHelper<BillingReport, String> jsonHelp = new JsonHelper<BillingReport, String>();
			return jsonHelp.generateJsonBodyWithEmpty(report);

		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_BILLING_INFO,ParamConstant.BILLING,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_BILLING_INFO,ParamConstant.BILLING,"",Message.FAILED_FLAG,message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_BILLING_DETAIL_REPORT_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_BILLING_INFO,ParamConstant.BILLING,"",Message.FAILED_FLAG,message);
			return message;
		}
	}
	
	
	@RequestMapping(value = "/billings", method = RequestMethod.GET)
	public String getBillings(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestHeader(value = ParamConstant.TENANT_ID, defaultValue = "") String tenantId,
			@RequestHeader(value = ParamConstant.BILLING_MONTH_UNTIL, defaultValue = "") String billingMonthUntil,
			HttpServletResponse response) {
		

		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<Billing> billings = billingService.getBillings(authToken);
			normalBillings(billings);
			response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			JsonHelper<List<Billing>, String> jsonHelp = new JsonHelper<List<Billing>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(billings);

		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_BILLINGS,ParamConstant.BILLING,"",Message.FAILED_FLAG,message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_BILLING_ACCOUNT_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_BILLINGS,ParamConstant.BILLING,"",Message.FAILED_FLAG,message);
			return message;
		}
	}
	
	@RequestMapping(value = "/billings", method = RequestMethod.POST)
	public String createBilling(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {
		

		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.BILLACCONT_NEW);
			Billing billing = billingService.createBilling(authToken,createBody);
			billing.normalInfo();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.CREATE_BILLING,ParamConstant.BILLING, billing.getId(),
					Message.SUCCESSED_FLAG, "");

			response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			JsonHelper<Billing, String> jsonHelp = new JsonHelper<Billing, String>();
			return jsonHelp.generateJsonBodyWithEmpty(billing);
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_BILLING,ParamConstant.BILLING,"",Message.FAILED_FLAG,message);
			return message;
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_BILLING,ParamConstant.BILLING,"",Message.FAILED_FLAG,message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_BILLING_ACCOUNT_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_BILLING,ParamConstant.BILLING,"",Message.FAILED_FLAG,message);
			return message;
		}
	}
	
	@RequestMapping(value = "/billings/{id}", method = RequestMethod.DELETE)
	public String deleteBilling(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response){

		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.BILLACCONT_DELETE);
			billingService.deleteBilling(authToken,id);
			response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken),
					authToken.getTenantid(), Message.DELETE_BILLING,ParamConstant.BILLING, id,
					Message.SUCCESSED_FLAG, "");

			JsonHelper<Billing, String> jsonHelp = new JsonHelper<Billing, String>();
			return jsonHelp.generateJsonBodyWithEmpty(new Billing());
		} catch (ResourceBusinessException e) {
			log.error(e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_BILLING,ParamConstant.BILLING,"",Message.FAILED_FLAG,message);
			return message;
		}catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_BILLING,ParamConstant.BILLING,"",Message.FAILED_FLAG,message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_BILLING_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_BILLING,ParamConstant.BILLING,"",Message.FAILED_FLAG,message);
			return message;
		}
	}
	
	@RequestMapping(value = "/billings/{name}/{id}", method = RequestMethod.PUT)
	public String setBillingAccount(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String name, @PathVariable String id, HttpServletResponse response){

		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.BILLACCONT_APPLY);
			BillingReport report = billingService.bindingBilling(authToken,name,id);
			if(null == report){
				response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_BILLING_ACCOUNT_BINDING_FAILED,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				return message;
			}
			response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			JsonHelper<BillingReport, String> jsonHelp = new JsonHelper<BillingReport, String>();
			return jsonHelp.generateJsonBodyWithEmpty(report);
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.BINDING_BILLING,ParamConstant.BILLING,"",Message.FAILED_FLAG,message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_BILLING_ACCOUNT_BINDING_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.BINDING_BILLING,ParamConstant.BILLING,"",Message.FAILED_FLAG,message);
			return message;
		}
	}
	
	@RequestMapping(value = "/billings/{id}", method = RequestMethod.PUT)
	public String setDefaultBilling(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String id, HttpServletResponse response){

		TokenOs authToken=null;
		try {
			authToken = getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			this.checkUserPermission(authToken, ParamConstant.BILLACCONT_SET_DEFAULT);
			Billing billing = billingService.setDefaultBilling(authToken,id);
			if(null == billing){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_BILLING_ACCOUNT_BINDING_FAILED,new Locale(authToken.getLocale()));
				return exception.getResponseMessage();
			}
			response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			JsonHelper<Billing, String> jsonHelp = new JsonHelper<Billing, String>();
			return jsonHelp.generateJsonBodyWithEmpty(billing);
		} catch (MyBatisSystemException e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.BINDING_BILLING,ParamConstant.BILLING,"",Message.FAILED_FLAG,message);
			return message;
		} catch (Exception e) {
			log.error(e);
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_BILLING_ACCOUNT_BINDING_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(this.getAuthService().getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.BINDING_BILLING,ParamConstant.BILLING,"",Message.FAILED_FLAG,message);
			return message;
		}
	}

	private void normalBillingReports(List<BillingReport> reports){
		if(Util.isNullOrEmptyList(reports))
			return;
		for(BillingReport report : reports){
			report.normalInfo(false);
		}
	}
	
	private void normalBillings(List<Billing> billings){
		if(Util.isNullOrEmptyList(billings))
			return;
		for(Billing billing : billings){
			billing.normalInfo();
		}
	}
}
