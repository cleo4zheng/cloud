package com.cloud.cloudapi.service.rating.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.BillingMapper;
import com.cloud.cloudapi.dao.common.BillingReportMapper;
import com.cloud.cloudapi.dao.common.CloudUserMapper;
import com.cloud.cloudapi.dao.common.DomainTenantUserMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.DomainTenantUser;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.rating.Billing;
import com.cloud.cloudapi.pojo.rating.BillingReport;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.pool.PoolResource;
import com.cloud.cloudapi.service.rating.BillingService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("billingService")
public class BillingServiceImpl implements BillingService {
	
	@Resource
	private OSHttpClientUtil httpClient;

	@Autowired
	private BillingReportMapper billingReportMapper;
	
	@Resource
	private PoolResource poolService;

	@Autowired
	private BillingMapper billingMapper;
	
    @Resource
    private CloudUserMapper cloudUserMapper;
    
    @Resource
    private DomainTenantUserMapper tenantUserMapper;
    
	@Resource
	private AuthService authService;
		
	private Logger log = LogManager.getLogger(BillingServiceImpl.class);
	
	@Override
	public List<Billing> getBillings(TokenOs ostoken){
		List<DomainTenantUser> users = tenantUserMapper.selectListByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(users))
			return new ArrayList<Billing>();
		
		List<Billing> billings = billingMapper.selectAllByUserId(users.get(0).getClouduserid());
		return billings;
	}
	
	@Override
	public Billing createBilling(TokenOs ostoken,String createBody) throws BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode billingNode = null;
		try {
			billingNode = mapper.readTree(createBody);
		} catch (Exception e) {
			log.error(e);
			return new Billing();
		} 
		
		String account = billingNode.path(ResponseConstant.ACCOUNT).asText();
		String name = billingNode.path(ResponseConstant.NAME).asText();
		
		List<DomainTenantUser> users = tenantUserMapper.selectListByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(users))
			return new Billing();
		
		//check name
		List<Billing> billings = billingMapper.selectAllByUserId(users.get(0).getClouduserid());
	    if(!Util.isNullOrEmptyList(billings)){
	    	for(Billing bill : billings){
	    		if(account.equals(bill.getAccount()))
	    			throw new ResourceBusinessException(Message.CS_BILLING_NAME_CONFLICT,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale())); 
	    	}
	    }
	    
		Billing billing = new Billing();
		billing.setId(Util.makeUUID());
		billing.setMillionSeconds(Util.getCurrentMillionsecond());
		billing.setUserId(users.get(0).getClouduserid());
		billing.setAccount(account);
		billing.setName(name);
		billing.setDefaultAccount(false);
		billingMapper.insertOrUpdate(billing);
		
//		billing.setUserId(null);
//		billing.setMillionSeconds(null);
		return billing;
	}
	
	@Override
	public void deleteBilling(TokenOs ostoken,String billingId) throws BusinessException{
		Billing billing = billingMapper.selectByPrimaryKey(billingId);
		if(true == billing.getDefaultAccount())
			throw new ResourceBusinessException(Message.CS_BILLING_IS_USED, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		billingMapper.deleteByPrimaryKey(billingId);
	}
	
	@Override
	public BillingReport bindingBilling(TokenOs ostoken, String reportName, String billingId) throws BusinessException {
		Billing billing = billingMapper.selectByPrimaryKey(billingId);
		if(null == billing)
			return null;
	    BillingReport report = billingReportMapper.selectByTenantIdAndName(reportName, ostoken.getTenantid());
	    if(null == report)
	    	return null;
	    report.setBillingId(billingId);
	    billingReportMapper.updateByPrimaryKeySelective(report);
	    return report;
	}

	@Override
	public Billing setDefaultBilling(TokenOs ostoken,String billingId) throws BusinessException{
		Billing billing = billingMapper.selectByPrimaryKey(billingId);
		if(null == billing)
			return null;
		List<DomainTenantUser> users = tenantUserMapper.selectListByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(users))
			return null;
		String userId = users.get(0).getClouduserid();
		Billing defaultAccount = billingMapper.selectDefaultUserAccount(userId);
		if(null != defaultAccount){
			defaultAccount.setDefaultAccount(false);
			billingMapper.updateByPrimaryKeySelective(defaultAccount);
		}
//		if(!ostoken.getTenantUserid().equals(billing.getUserId()))
//			throw new ResourceBusinessException(Message.CS_BILLING_IS_DIFF_WITH_USER, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		billing.setDefaultAccount(true);
		billing.setUserId(userId);
		billingMapper.updateByPrimaryKeySelective(billing);
		return billing;
	}
	
	private void makeUserBillingAccount(String tenantId,List<BillingReport> reports){
		List<DomainTenantUser> users = tenantUserMapper.selectListByTenantId(tenantId);
		if(Util.isNullOrEmptyList(users))
			return;
		String userId = users.get(0).getClouduserid();
		Billing defaultAccount = billingMapper.selectDefaultUserAccount(userId);
	   	for(BillingReport report : reports){
	   		if(null == report.getBillingId())
	   			report.setBilling(defaultAccount);
	   		else
	   			report.setBilling(billingMapper.selectByPrimaryKey(report.getBillingId()));
	   	}
	}
	
	@Override
	public List<BillingReport> getBillStatistics(String tenantId,String billingMonthUntil,TokenOs ostoken) throws BusinessException {
		
		if(Util.isNullOrEmptyValue(tenantId))
			tenantId = ostoken.getTenantid();
		List<BillingReport> reports = billingReportMapper.selectByTenantId(tenantId);
		if(Util.isNullOrEmptyList(reports))
			return new ArrayList<BillingReport>();
		if(Util.isNullOrEmptyValue(billingMonthUntil)){
			makeUserBillingAccount(tenantId,reports);
			return reports;
		}
		
		List<BillingReport> filterReports = new ArrayList<BillingReport>();
		for(BillingReport report : reports){
			if(billingMonthUntil.compareTo(report.getBilling_month()) > 0)
				continue;
			filterReports.add(report);
		}
		makeUserBillingAccount(tenantId,filterReports);
		return filterReports;	
	}
	
//	private List<BillStatistic> getSimpleBilling(String tenantId,String billingMonthUntil,TokenOs ostoken) throws BusinessException{
//		if(Util.isNullOrEmptyValue(tenantId))
//			tenantId = ostoken.getTenantid();
//		
//		String region = ostoken.getCurrentRegion(); 
//		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_RATING, region).getPublicURL();
//		url = RequestUrlHelper.createFullUrl(url + "/v1/billing/list_month_report", null);
//		StringBuilder sb = new StringBuilder();
//		sb.append(url);
//		if(!Util.isNullOrEmptyValue(tenantId) && !Util.isNullOrEmptyValue(billingMonthUntil)){
//			sb.append("?");
//			sb.append(ParamConstant.TENANT_ID);
//			sb.append("=");
//			sb.append(tenantId);
//			sb.append("&");
//			sb.append(ParamConstant.BILLING_MONTH_UNTIL);
//			sb.append("=");
//			sb.append(billingMonthUntil);
//		}else if(!Util.isNullOrEmptyValue(tenantId)){
//			sb.append("?");
//			sb.append(ParamConstant.TENANT_ID);
//			sb.append("=");
//			sb.append(tenantId);
//		}else if(!Util.isNullOrEmptyValue(billingMonthUntil)){
//			sb.append("?");
//			sb.append(ParamConstant.BILLING_MONTH_UNTIL);
//			sb.append("=");
//			sb.append(billingMonthUntil);
//		}
//
//		Locale locale = new Locale(ostoken.getLocale());
//		url = RequestUrlHelper.createFullUrl(sb.toString(), null);
//
//		Map<String, String> rs = httpClient.httpDoGet(url, ostoken.getTokenid());
//		Util.checkResponseBody(rs,locale);
//		
//		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//		String failedMessage = Util.getFailedReason(rs);
//		if (!Util.isNullOrEmptyValue(failedMessage))
//			log.error(failedMessage);
//        List<BillStatistic> billings = null;
//		switch (httpCode) {
//		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
//			try {
//				billings = getBillingStatistics(rs,false);
//			}  catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			break;
//		}
//		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
//			String tokenid = "";// TODO reget the token id
//			try {
//				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
//				tokenid = newToken.getTokenid();
//			} catch (Exception e1) {
//				// TODO Auto-generated catch block
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			rs = httpClient.httpDoGet(sb.toString(), tokenid);
//			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//			failedMessage = Util.getFailedReason(rs);
//			if (!Util.isNullOrEmptyValue(failedMessage))
//				log.error(failedMessage);
//			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//			try {
//				billings = getBillingStatistics(rs,false);
//			}  catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			break;
//		}
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
//		default:
//			throw new ResourceBusinessException(Message.CS_BILLING_SIMPLE_REPORT_GET_FAILED,httpCode,locale);
//		}
//		
//		return billings;
//	}
	
//	private Map<String,Float> getChargingTime(String createdDate) throws ParseException{
//		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		Calendar createdTime = Calendar.getInstance();
//		createdTime.setTime(df.parse(createdDate));
//		
//		Calendar currentTime = Calendar.getInstance();
//		Date currentDate = new Date();
//		currentTime.setTime(currentDate);
//		
//		int createdMonth = createdTime.get(Calendar.MONTH);
//		int currentMonth = currentTime.get(Calendar.MONTH);
//		
//		int currentYear = currentTime.get(Calendar.YEAR); 
//				 
//		int currentDay = currentTime.get(Calendar.DATE);
//		int ratingday = Integer.parseInt(cloudconfig.getRatingReportEndDay());
//
//	//	long[] chargeHours = new long[2];
//		Map<String,Float> chargeHours = new HashMap<String,Float>();
//		
//		if(currentMonth > createdMonth && currentDay > ratingday){
//			//srh from db
//			String reportTime = String.format("%s-%s-%s", currentYear,createdMonth,ratingday);
//			reportTime += " ";
//			reportTime += cloudconfig.getRatingReportTime();
//			float diffHours = (df.parse(reportTime).getTime()-df.parse(createdDate).getTime())/((float)60*60*1000);
//			chargeHours.put(String.format("%s-%s", currentYear,createdMonth+1), diffHours);
//			diffHours = (currentDate.getTime()-df.parse(reportTime).getTime())/((float)60*60*1000);
//			chargeHours.put(String.format("%s-%s", currentYear,currentMonth+1), diffHours);
//		
//		}else{
//			float diffHours = (currentDate.getTime()-df.parse(createdDate).getTime())/((float)60*60*1000);
//			chargeHours.put(String.format("%s-%s", currentYear,currentMonth+1), diffHours);
//		}
//		return chargeHours;
////		Calendar createdTime = Calendar.getInstance();
////		createdTime.setTime(df.parse(createdDate));
////		
////		Calendar currentTime = Calendar.getInstance();
////		currentTime.setTime(new Date());
////		int year = currentTime.get(Calendar.YEAR);
////        int month = currentTime.get(Calendar.MONTH);
////        
////        int differYears = currentTime.get(Calendar.YEAR) - createdTime.get(Calendar.YEAR);
////        int differMonths = currentTime.get(Calendar.MONTH) - createdTime.get(Calendar.MONTH);
////        int differDays =  currentTime.get(Calendar.DATE) - createdTime.get(Calendar.DATE);
////        int differHours = currentTime.get(Calendar.HOUR) - createdTime.get(Calendar.HOUR);
////        int differMinutes = currentTime.get(Calendar.MINUTE) - createdTime.get(Calendar.MINUTE);
////        int differSeconds = currentTime.get(Calendar.SECOND) - createdTime.get(Calendar.SECOND);
//        
////        double chargeTime  = (differYears*365*24*60*60 + differMonths*30*24*60*60 + differHours*60*60 + differMinutes*60 + differSeconds)/(60*60);
//        
//        
//	}
//	
//	private double calculatePoolPrice(List<RatingService> services,PoolConfig poolConfig,Map<String, Integer> tVols,Map<String, Integer> tCpus,Map<String, Integer> tMems,
//			Map<String,Double> storage,Map<String,Double> cpu,Map<String,Double> ram,double chargeHours,Locale locale){
//		
//		double totalRate = 0.0;
//		double computeRate = 0.0;
//		double storageRate = 0.0;
//		
//		RatingService storageService = new RatingService();
//		List<RatingPolicy> storageFields = new ArrayList<RatingPolicy>();
//		storageService.setService_code(ParamConstant.STORAGE);
//		storageService.setService_name(Message.getMessage(Message.CS_QUOTA_STORAGE_TYPE, locale,false));
//		for (Map.Entry<String, Integer> entry : tVols.entrySet()) {
//			RatingPolicy policy = new RatingPolicy();
//			policy.setField_code(entry.getKey());
//			String typeName = "CS_VOLUME_TYPE_NAME";
//			typeName = typeName.replaceFirst("TYPE", entry.getKey().toUpperCase());
//			policy.setField_name(Message.getMessage(typeName,locale,false));
//			double unitPrice = poolConfig.getVolume().getUnitPriceByType(entry.getKey());
//			double rate = unitPrice * chargeHours * entry.getValue();
//			policy.setRate((float)rate);
//			storageFields.add(policy);
//			totalRate += rate;
//			storageRate += rate;
////			if(storage.containsKey(entry.getKey())){
////				storage.put(entry.getKey(), storage.get(entry.getKey()) + rate);
////			}else{
////				storage.put(entry.getKey(), rate);
////			}
//		}
//		storageService.setRate((float)storageRate);
//		storageService.setFieldratings(storageFields);
//		
//		RatingService computeService = new RatingService();
//		List<RatingPolicy> computeFields = new ArrayList<RatingPolicy>();
//		storageService.setService_code(ParamConstant.COMPUTE);
//		storageService.setService_name(Message.getMessage(Message.CS_QUOTA_COMPUTE_TYPE, locale,false));
//		
//		for (Map.Entry<String, Integer> entry : tCpus.entrySet()) {
//			InstanceType type = poolConfig.getInstanceTypeByName(entry.getKey());
//			
//			RatingPolicy policy = new RatingPolicy();
//			policy.setField_code(entry.getKey()+"_core");
//			String typeName = "CS_CORE_TYPE_NAME";
//			typeName = typeName.replaceFirst("TYPE", entry.getKey().toUpperCase());
//			policy.setField_name(Message.getMessage(typeName,locale,false));
//			
//			double unitPrice = type != null ? type.getCore().getUnitPrice() : 0.0;
//			double rate = unitPrice * chargeHours * entry.getValue();
//			totalRate += rate;
//			computeRate += rate;
//			policy.setRate((float)rate);
//			computeFields.add(policy);
////			if(cpu.containsKey(entry.getKey())){
////				cpu.put(entry.getKey(), cpu.get(entry.getKey()) + rate);
////			}else{
////				cpu.put(entry.getKey(), rate);
////			}
//		}
//		
//		for (Map.Entry<String, Integer> entry : tMems.entrySet()) {
//			InstanceType type = poolConfig.getInstanceTypeByName(entry.getKey());
//			
//			RatingPolicy policy = new RatingPolicy();
//			policy.setField_code(entry.getKey()+"_ram");
//			String typeName = "CS_RAM_TYPE_NAME";
//			typeName = typeName.replaceFirst("TYPE", entry.getKey().toUpperCase());
//			policy.setField_name(Message.getMessage(typeName,locale,false));
//			
//			double unitPrice = type != null ? type.getRam().getUnitPrice() : 0.0;
//			double rate = unitPrice * chargeHours * entry.getValue();
//			totalRate += rate;
//			computeRate += rate;
//			policy.setRate((float)rate);
//			computeFields.add(policy);
//			
////			totalRate += rate;
////			computeRate += rate;
////			if(ram.containsKey(entry.getKey())){
////				ram.put(entry.getKey(), ram.get(entry.getKey()) + rate);
////			}else{
////				ram.put(entry.getKey(), rate);
////			}
//		}
//		computeService.setRate((float)computeRate);
//		computeService.setFieldratings(computeFields);
//		services.add(computeService);
//		services.add(storageService);
//		return totalRate;
////		double[] costs = new double[3];
////		costs[0] = totalRate;
////		costs[1] = storageRate;
////		costs[2] = computeRate;
////		return costs;
//	}
	
//	private void setNewBillingReport(BillStatistic billing,String tenantId,TokenOs ostoken) throws Exception{
//		List<BillingReport> reports = new ArrayList<BillingReport>();
//		chargeTenantPool(tenantId,ostoken,reports);
//		List<BillingReport> existedReports = billing.getMonth_reports();
//		if(Util.isNullOrEmptyList(existedReports)){
//			billing.setMonth_reports(reports);	
//		}
//		else{
//			existedReports.addAll(reports);
//			billing.setMonth_reports(existedReports);
//		}
//	}
	
//	private void updateBillingReport(BillStatistic billing,BillingReport existedReport,String tenantId,TokenOs ostoken) throws Exception{
//		ObjectMapper mapper = new ObjectMapper();
//		Map<String, Float> uStorages = null;
//		Map<String, Float> uComputes = null;
//		Map<String, Float> uServices = null;
//		try {
//			uStorages = existedReport.getStorageDetails() != null ? mapper.readValue(existedReport.getStorageDetails(), new TypeReference<HashMap<String, Float>>() {}) : null;
//			uComputes = existedReport.getComputeDetails() != null ? mapper.readValue(existedReport.getComputeDetails(), new TypeReference<HashMap<String, Float>>() {}) : null;
//			uServices = existedReport.getServiceDetails() != null ? mapper.readValue(existedReport.getServiceDetails(), new TypeReference<HashMap<String, Float>>() {}) : null;
//		} catch (Exception e) {
//			log.error(e);
//		}
//		
//		Locale locale = new Locale(ostoken.getLocale());
//		
//		List<RatingService> services = new ArrayList<RatingService>();
//		List<BillingReport> reports = new ArrayList<BillingReport>();
//		BillingReport report = new BillingReport();
//		report.setTenantId(tenantId);
//		report.setCcy(Message.getMessage(Message.CHINESE_CURRENCY,locale,false));
//		report.setBilling_month(billing.getBilling_month());
//		report.setCost(existedReport.getCost());
//		report.setCompute(existedReport.getCompute());
//		report.setStorage(existedReport.getStorage());
//		reports.add(report);
//
//		RatingService storageService = new RatingService();
//		List<RatingPolicy> storageFields = new ArrayList<RatingPolicy>();
//		storageService.setService_code(ParamConstant.STORAGE);
//		storageService.setService_name(Message.getMessage(Message.CS_QUOTA_STORAGE_TYPE, locale,false));
//		float total = 0;
//		if(null != uStorages){
//			for (Map.Entry<String, Float> entry : uStorages.entrySet()) {
//				total += entry.getValue();
//				RatingPolicy policy = new RatingPolicy();
//				String typeName = "CS_VOLUME_TYPE_NAME";
//				typeName = typeName.replaceFirst("TYPE", entry.getKey().toUpperCase());
//				policy.setField_code(entry.getKey());
//				policy.setField_name(Message.getMessage(typeName,locale,false));
//				policy.setRate(entry.getValue());
//				storageFields.add(policy);
//			}
//		}
//		storageService.setRate(total);
//		storageService.setFields(storageFields);
//		
//		
//		RatingService serviceService = new RatingService();
//		List<RatingPolicy> serviceFields = new ArrayList<RatingPolicy>();
//		serviceService.setService_code(ParamConstant.SERVICE);
//		serviceService.setService_name(Message.getMessage(Message.CS_QUOTA_SERVICE_TYPE,locale, false));
//		total = 0;
//		if(null != uServices){
//			for (Map.Entry<String, Float> entry : uServices.entrySet()) {
//				total += entry.getValue();
//				RatingPolicy policy = new RatingPolicy();
//				String typeName = "CS_SERVICE_TYPE_NAME";
//				typeName = typeName.replaceFirst("TYPE", entry.getKey().toUpperCase());
//				policy.setField_code(entry.getKey());
//				policy.setField_name(Message.getMessage(typeName,locale,false));
//				policy.setRate(entry.getValue());
//				serviceFields.add(policy);
//			}
//		}
//		
//		serviceService.setRate(total);
//		serviceService.setFields(serviceFields);
//		
//		total = 0;
//		RatingService computeService = new RatingService();
//		List<RatingPolicy> computeFields = new ArrayList<RatingPolicy>();
//		computeService.setService_code(ParamConstant.COMPUTE);
//		computeService.setService_name(Message.getMessage(Message.CS_QUOTA_COMPUTE_TYPE,locale, false));
//		if(null != uComputes){
//			for (Map.Entry<String, Float> entry : uComputes.entrySet()) {
//				total += entry.getValue();
//				RatingPolicy policy = new RatingPolicy();
//				policy.setField_code(entry.getKey());
//				String typeName = "";
//				if(entry.getKey().contains("_ram")){
//					typeName = "CS_RAM_TYPE_NAME";
//					String ramType = entry.getKey().replace("_ram", "");
//					typeName = typeName.replaceFirst("TYPE", ramType.toUpperCase());
//				}else{
//					typeName = "CS_CORE_TYPE_NAME";
//					String coreType = entry.getKey().replace("_core", "");
//					typeName = typeName.replaceFirst("TYPE", coreType.toUpperCase());
//				}
//				policy.setField_name(Message.getMessage(typeName,locale,false));
//				policy.setRate(entry.getValue());
//				computeFields.add(policy);
//			}
//		}
//
//		List<RatingService> existedService = billing.getServices();
//		List<BillingReport> existedReports = billing.getMonth_reports();
//		if(null == existedReports)
//			existedReports = new ArrayList<BillingReport>();
//		if(Util.isNullOrEmptyList(existedService)){
//			computeService.setRate(total);
//			computeService.setFields(computeFields);
//			
//			services.add(computeService);
//			services.add(storageService);
//			services.add(serviceService);
//			billing.setServices(services);
//			billing.setMonth_reports(reports);
//		}
//		else{
//			for(BillingReport billingReport : existedReports){
//				if(!billingReport.getBilling_month().equals(billing.getBilling_month()))
//					continue;
//				billingReport.setCost(billingReport.getCost() + existedReport.getCost());
//				List<RatingService> billingServices = billing.getServices();
//				if(null ==  billingServices)
//					continue;
//				for(RatingService service : billingServices){
//					if(service.getService_code().equals(ParamConstant.COMPUTE)){
//						List<RatingPolicy> fields = service.getFields();
//						if(!Util.isNullOrEmptyList(fields)){
//							List<RatingPolicy> notHitFields = new ArrayList<RatingPolicy>();
//							for(RatingPolicy computeField : computeFields){
//								boolean bHit = false;
//								for(RatingPolicy field : fields){
//									if(field.getField_code().equals(computeField.getField_code())){
//										field.setRate(field.getRate() + computeField.getRate());
//										bHit = true;
//										break;
//									}
//								}
//								if(false == bHit)
//									notHitFields.add(computeField);
//							}
//							fields.addAll(notHitFields);
//							service.setFields(fields);
//						}else{
//							service.setFields(computeFields);
//						}
//					}else if(service.getService_code().equals(ParamConstant.STORAGE)){
//						List<RatingPolicy> fields = service.getFields();
//						if(!Util.isNullOrEmptyList(fields)){
//							List<RatingPolicy> notHitFields = new ArrayList<RatingPolicy>();
//							for(RatingPolicy storageField : storageFields){
//								boolean bHit = false;
//								for(RatingPolicy field : fields){
//									if(field.getField_code().equals(storageField.getField_code())){
//										field.setRate(field.getRate() + storageField.getRate());
//										bHit = true;
//										break;
//									}
//								}
//								if(false == bHit)
//									notHitFields.add(storageField);
//							}
//							fields.addAll(notHitFields);
//							service.setFields(fields);
//						}else{
//							service.setFields(storageFields);
//						}
//						
//					}
//				}
//			}
//			existedReports.addAll(reports);
//			billing.setMonth_reports(existedReports);
//		}
//	}
//	
//	private void setBillingReportDetails(List<BillStatistic> billings,String tenantId,TokenOs ostoken) throws Exception{
//		if(Util.isNullOrEmptyList(billings))
//			return;
//		if(Util.isNullOrEmptyValue(tenantId))
//			tenantId = ostoken.getTenantid();
//		
//		for(BillStatistic billing : billings){
//			if(ParamConstant.DEFAULT_CURRENCY.equals(billing.getCcy())){
//				billing.setCcy(Message.getMessage(Message.CHINESE_CURRENCY,new Locale(ostoken.getLocale()),false));	
//			}
//			BillingReport existedReport = billingReportMapper.selectByPrimaryKey(billing.getBilling_month());
//			if(null == existedReport){
//				setNewBillingReport(billing,tenantId,ostoken);
//			}else{
//				updateBillingReport(billing,existedReport,tenantId,ostoken);
//			}
//		}
//	}
	
//	private void chargeTenantServices(PoolConfig poolConfig,List<BillingReport> reports,String tenantId) throws ParseException{
//	   Map<String, Float> uServices = new HashMap<String, Float>();
//	   List<Firewall> firewalls = firewallMapper.selectAllByTenantId(tenantId);
//	   double servicePrice = 0;
//	   double rate = 0;
//	   if(!Util.isNullOrEmptyList(firewalls)){
//		   for(Firewall firewall : firewalls){
//			   if(null == firewall.getMillionSeconds())
//				   continue;
//			   Map<String, Float> chargeHours = getChargingTime(Util.millionSecond2Date(firewall.getMillionSeconds()));
//			   for (Map.Entry<String, Float> entry : chargeHours.entrySet()){
//				   for (BillingReport report : reports){
//					   if (report.getBilling_month().equals(entry.getKey())){
//						   servicePrice = entry.getValue()*poolConfig.getServicePrice(ParamConstant.FWAAS);
//						   rate += servicePrice;
//						   if(uServices.get(ParamConstant.FWAAS) == null){
//							   uServices.put(ParamConstant.FWAAS, (float)servicePrice);   
//						   }else{
//							   uServices.put(ParamConstant.FWAAS, uServices.get(ParamConstant.FWAAS) + (float)servicePrice);
//						   }  
//					   }else{
//						   //skip the existed billing report
//					   }
//					  
//				   }
//			   }
//		   }
//		  
//	   }
//	   
//	   List<VPN> vpns = vpnMapper.selectAllByTenantId(tenantId);
//	   if(!Util.isNullOrEmptyList(vpns)){
//		   for(VPN vpn : vpns){
//			   if(null == vpn.getMillionSeconds())
//				   continue;
//			   Map<String, Float> chargeHours = getChargingTime(Util.millionSecond2Date(vpn.getMillionSeconds()));
//			   for (Map.Entry<String, Float> entry : chargeHours.entrySet()){
//				   for (BillingReport report : reports){
//					   if (report.getBilling_month().equals(entry.getKey())){
//						   servicePrice = entry.getValue()*poolConfig.getServicePrice(ParamConstant.VPNAAS);
//						   rate += servicePrice;
//						   if(uServices.get(ParamConstant.VPNAAS) == null){
//							   uServices.put(ParamConstant.VPNAAS, (float)servicePrice);   
//						   }else{
//							   uServices.put(ParamConstant.VPNAAS, uServices.get(ParamConstant.VPNAAS) + (float)servicePrice);
//						   }
//					   }else{
//						   //skip the existed billing report
//					   }
//				   }
//			   }
//		   }
//	   }
//	   
//	   List<Loadbalancer> loadbalancers = loadbalancerMapper.selectAllByTenantId(tenantId);
//	   if(!Util.isNullOrEmptyList(loadbalancers)){
//		   for(Loadbalancer loadbalancer : loadbalancers){
//			   if(null == loadbalancer.getMillionSeconds())
//				   continue;
//			   Map<String, Float> chargeHours = getChargingTime(Util.millionSecond2Date(loadbalancer.getMillionSeconds()));
//			   for (Map.Entry<String, Float> entry : chargeHours.entrySet()){
//				   for (BillingReport report : reports){
//					   if (report.getBilling_month().equals(entry.getKey())){
//						   servicePrice = entry.getValue()*poolConfig.getServicePrice(ParamConstant.LBAAS);
//						   rate += servicePrice;
//						   if(uServices.get(ParamConstant.LBAAS) == null){
//							   uServices.put(ParamConstant.LBAAS, (float)servicePrice);   
//						   }else{
//							   uServices.put(ParamConstant.LBAAS, uServices.get(ParamConstant.LBAAS) + (float)servicePrice);
//						   }
//					   }else{
//						   //skip the existed billing report
//					   }
//				
//				   }
//			   }
//		   }
//		  
//	   }
//	   
//	   for (BillingReport report : reports){
//			BillingReport existedReport = billingReportMapper.selectByPrimaryKey(report.getBilling_month());
//			if (null != existedReport && null != existedReport.getService()) {
//				report.setService(existedReport.getService());
//			}else{
//				report.setService((float)rate);
//				JsonHelper<Map<String, Float>, String> jsonHelp = new JsonHelper<Map<String, Float>, String>();
//				report.setServiceDetails(jsonHelp.generateJsonBodyWithEmpty(uServices));
//				billingReportMapper.insertOrUpdate(report);
//			}
//	   }
//	}
	
//	private void chargeTenantPool(String tenantId, TokenOs ostoken, List<BillingReport> reports) throws Exception {
//		
//		if(Util.isNullOrEmptyValue(tenantId))
//			tenantId = ostoken.getTenantid();
//		
//		Locale locale = new Locale(ostoken.getLocale());
//		
//		if (0 == reports.size()){
//			BillingReport report = new BillingReport();
//			report.setTenantId(tenantId);
//			report.setCcy(Message.getMessage(Message.CHINESE_CURRENCY,locale,false));
//			Calendar currentTime = Calendar.getInstance();
//			Date currentDate = new Date();
//			currentTime.setTime(currentDate);
//			report.setBilling_month(String.format("%s-%s", currentTime.get(Calendar.YEAR),currentTime.get(Calendar.MONTH)+1));	
//			reports.add(report);
//		}
//
//
//		List<PoolEntity> pools = poolMapper.selectByTenantId(tenantId);
//		if (Util.isNullOrEmptyList(pools))
//			return;
//
//		PoolConfig poolConfig = poolService.getPoolConfig(ostoken);
//		Map<String, Double> storage = new HashMap<String, Double>();
//		Map<String, Double> cpu = new HashMap<String, Double>();
//		Map<String, Double> ram = new HashMap<String, Double>();
//
//		for (PoolEntity pool : pools) {
//			if(Util.isNullOrEmptyValue(pool.getCreatedAt()))
//				continue;
//			ObjectMapper mapper = new ObjectMapper();
//			Map<String, Float> chargeHours = getChargingTime(pool.getCreatedAt());
//
//			Map<String, Integer> tVols = null;
//			Map<String, Integer> tCpus = null;
//			Map<String, Integer> tMems = null;
//			Map<String, Float> uStorages = new HashMap<String, Float>();
//			Map<String, Float> uComputes = new HashMap<String, Float>();
//	
//			try {
//				tVols = null != pool.gettVolumes() ? mapper.readValue(pool.gettVolumes(), new TypeReference<HashMap<String, Integer>>() {
//				}) : null;
//				tCpus = null != pool.gettCpus() ? mapper.readValue(pool.gettCpus(), new TypeReference<HashMap<String, Integer>>() {
//				}) : null;
//				tMems = null != pool.gettMems() ? mapper.readValue(pool.gettMems(), new TypeReference<HashMap<String, Integer>>() {
//				}) : null;
//			} catch (Exception e) {
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			List<RatingService> services = new ArrayList<RatingService>();
//			for (Map.Entry<String, Float> entry : chargeHours.entrySet()) {
//				for (BillingReport report : reports) {
//					if (report.getBilling_month().equals(entry.getKey())) {
//						double totalRate = calculatePoolPrice(services,poolConfig, tVols, tCpus, tMems, storage, cpu, ram,
//								entry.getValue(),locale);
//						report.setCost((float)totalRate);
//						report.setCompute(services.get(0).getRate());
//						report.setStorage(services.get(1).getRate());
//						
//						List<RatingPolicy> computeFileds = services.get(0).getFieldratings();
//						if(!Util.isNullOrEmptyList(computeFileds)){
//							for(RatingPolicy field : computeFileds){
//								uComputes.put(field.getField_code(), field.getRate());
//							}
//							JsonHelper<Map<String, Float>, String> jsonHelp = new JsonHelper<Map<String, Float>, String>();
//							report.setComputeDetails(jsonHelp.generateJsonBodyWithEmpty(uComputes));
//						}
//						
//						List<RatingPolicy> storageFileds = services.get(1).getFieldratings();
//						if(!Util.isNullOrEmptyList(storageFileds)){
//							for(RatingPolicy field : storageFileds){
//								uStorages.put(field.getField_code(), field.getRate());
//							}
//							JsonHelper<Map<String, Float>, String> jsonHelp = new JsonHelper<Map<String, Float>, String>();
//                            report.setStorageDetails(jsonHelp.generateJsonBodyWithEmpty(uStorages));
//						}
//						
//						billingReportMapper.insertOrUpdate(report);
//					} else {
//						BillingReport existedReport = billingReportMapper.selectByPrimaryKey(report.getBilling_month());
//						if (null != existedReport) {
//							report.setCost(existedReport.getCost());
//							report.setCompute(existedReport.getCompute());
//							report.setStorage(existedReport.getStorage());
//						}
//					}
//				}
//			}
//		}
//		chargeTenantServices(poolConfig,reports,tenantId);
//	}
	
//	private Float getFieldRate(RatingService rating) {
//		List<RatingPolicy> fields = rating.getFields();
//		float rate = 0;
//		if (Util.isNullOrEmptyList(fields))
//			return rate;
//		for (RatingPolicy field : fields) {
//			if (field.getRate() == null)
//				continue;
//			rate += field.getRate();
//		}
//		return rate;
//	}

//	private List<BillingReport> getGeneralBilling(List<BillStatistic> bills,TokenOs ostoken,String tenantId) throws BusinessException{
//	   	if(Util.isNullOrEmptyList(bills))
//	   		return null;
//	   	List<BillingReport> generalReports = new ArrayList<BillingReport>();
//	   	for(BillStatistic bill : bills){
//	   		if(!tenantId.equals(bill.getTenant_id()))
//	   			continue;
//	   		List<BillingReport> reports = bill.getMonth_reports();
//	   		if(Util.isNullOrEmptyList(reports))
//	   			continue;
//	   		for(BillingReport report : reports){
//	   			float compute = 0;
//	   			float storage = 0;
//	   			float network = 0;
//	   			float image = 0;
//	   			float service = 0;
//	   			String billingMonth = report.getBilling_month();
//	   			List<BillStatistic> detailBills = getBillDetails(tenantId, billingMonth, ostoken);
//	   			if(Util.isNullOrEmptyList(detailBills))
//	   				continue;
//	   			for(BillStatistic detailBill : detailBills){
//	   				List<RatingService> ratings = detailBill.getServices();
//	   				if(Util.isNullOrEmptyList(ratings))
//	   					continue;
//	   				for(RatingService rating : ratings){
//	   					if(rating.getService_code().equals(ParamConstant.COMPUTE)){
//	   						compute += rating.getRate();//getFieldRate(rating);
//	   					}else if(rating.getService_code().equals(ParamConstant.STORAGE)){
//	   						storage +=  rating.getRate();//getFieldRate(rating);
//	   					}else if(rating.getService_code().equals(ParamConstant.NETWORK)){
//	   						network +=  rating.getRate();//getFieldRate(rating);
//	   					}else if(rating.getService_code().equals(ParamConstant.IMAGE)){
//	   						image +=  rating.getRate();//getFieldRate(rating);
//	   					}else if(rating.getService_code().equals(ParamConstant.SERVICE)){
//	   						service +=  rating.getRate();//getFieldRate(rating);
//	   					}
//	   				}
//	   			}
//	   			report.setCompute(compute);
//	   			report.setStorage(storage);
//	   			report.setNetwork(network);
//	   			report.setImage(image);
//	   			report.setService(service);
//	   			generalReports.add(report);
//	   		}
//	   	}
//	   	
//	   	return generalReports;
//	}
	
//	private void makeBillingDetailInfo(Float rate,String billingDetailInfo,List<RatingService> ratingServices,String resourceTypeId,Locale locale){
//		if(Util.isNullOrEmptyValue(billingDetailInfo))
//			return;
//		RatingService service = new RatingService();
//		if(!Message.CS_QUOTA_IMAGE_TYPE.equals(resourceTypeId))
//			service.setService_name(Message.getMessage(resourceTypeId,locale,false));
//		else
//			service.setService_name(resourceTypeId);
//		service.setRate(rate);
//		String ramType = "CS_RAM_TYPE_NAME";
//		String coreType = "CS_CORE_TYPE_NAME";
//		String volumeType = "CS_VOLUME_TYPE_NAME";
//		String floatingipType = "CS_FLOTINGIP_TYPE_NAME";
//		String serviceType = "CS_SERVICE_TYPE_NAME";
//		String coreTypeName = null;
//		String ramTypeName = null;
//		String volumeTypeName = null;
//		String floatingTypeName = null;
//		String serviceTypeName = null;
//		List<RatingPolicy> ratingFields = new ArrayList<RatingPolicy>();
//		String[] resourceId =  billingDetailInfo.split(",");
//		for(int index = 0; index < resourceId.length; ++index){
//		   int srhPos = resourceId[index].indexOf(":");
//			if(srhPos != -1){
//				String type = resourceId[index].substring(0,srhPos);
//				String value = resourceId[index].substring(srhPos+1);
//				RatingPolicy field = new RatingPolicy();
//				if(Message.CS_QUOTA_COMPUTE_TYPE.equals(resourceTypeId)){
//				
//					if(type.contains("_core")){
//						coreTypeName = coreType.replaceFirst("TYPE", type.substring(0, type.indexOf("_core")).toUpperCase());
//						field.setField_name((Message.getMessage(coreTypeName,locale,false)));
//					}else{
//						ramTypeName = ramType.replaceFirst("TYPE", type.substring(0, type.indexOf("_ram")).toUpperCase());
//						field.setField_name((Message.getMessage(ramTypeName,locale,false)));
//					}		
//				}else if(Message.CS_QUOTA_STORAGE_TYPE.equals(resourceTypeId)){
//					volumeTypeName = volumeType.replaceFirst("TYPE", type.toUpperCase());
//					field.setField_name((Message.getMessage(volumeTypeName,locale,false)));	
//				}else if(Message.CS_QUOTA_NETWORK_TYPE.equals(resourceTypeId)){
//					floatingTypeName = floatingipType.replaceFirst("TYPE", type.toUpperCase());
//					field.setField_name((Message.getMessage(floatingTypeName,locale,false)));	
//				}else if(Message.CS_QUOTA_SERVICE_TYPE.equals(resourceTypeId)){
//					serviceTypeName = serviceType.replaceFirst("TYPE", type.toUpperCase());
//					field.setField_name((Message.getMessage(serviceTypeName,locale,false)));	
//				}else{
//					field.setField_name(type.toUpperCase());
//				}
//		
//					
//				field.setRate(Float.parseFloat(value));
//				ratingFields.add(field);
//			}
//		}
//		service.setFields(ratingFields);
//		ratingServices.add(service);
//	}
	
	@Override
	public BillingReport getBillDetails(String tenantId,String billingMonth,TokenOs ostoken) throws BusinessException{
		if(Util.isNullOrEmptyValue(tenantId))
			tenantId = ostoken.getTenantid();
		if(Util.isNullOrEmptyValue(billingMonth)){
			Calendar currentTime = Calendar.getInstance();
			Date currentDate = new Date();
			currentTime.setTime(currentDate);
			int currentMonth = currentTime.get(Calendar.MONTH);
			int currentYear = currentTime.get(Calendar.YEAR); 
			billingMonth = String.format("%s-%s",currentYear,currentMonth+1);
		}
		BillingReport report = this.billingReportMapper.selectByTenantIdAndName(billingMonth,tenantId);
		if(null == report)
			return new BillingReport();
		Locale locale = new Locale(ostoken.getLocale());
		report.setCcy(Message.getMessage(Message.CHINESE_CURRENCY,locale,false));
		
		
//		List<RatingService> ratingServices = new ArrayList<RatingService>();
//
//		makeBillingDetailInfo(report.getCompute(),report.getComputeDetails(),ratingServices,Message.CS_QUOTA_COMPUTE_TYPE,locale);
//		makeBillingDetailInfo(report.getStorage(),report.getStorageDetails(),ratingServices,Message.CS_QUOTA_STORAGE_TYPE,locale);
//		makeBillingDetailInfo(report.getImage(),report.getImageDetails(),ratingServices,Message.CS_QUOTA_IMAGE_TYPE,locale);
//		makeBillingDetailInfo(report.getNetwork(),report.getNetworkDetails(),ratingServices,Message.CS_QUOTA_NETWORK_TYPE,locale);
//		makeBillingDetailInfo(report.getService(),report.getServiceDetails(),ratingServices,Message.CS_QUOTA_SERVICE_TYPE,locale);
        
		return report;
	}
	
}
