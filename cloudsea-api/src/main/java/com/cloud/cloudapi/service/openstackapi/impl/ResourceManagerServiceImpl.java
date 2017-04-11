package com.cloud.cloudapi.service.openstackapi.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.BillingReportMapper;
import com.cloud.cloudapi.dao.common.InstanceMapper;
import com.cloud.cloudapi.dao.common.NotificationMapper;
import com.cloud.cloudapi.dao.common.QuotaDetailMapper;
import com.cloud.cloudapi.dao.common.ResourceCreateProcessMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.AggregationInfo;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceCreateProcess;
import com.cloud.cloudapi.pojo.rating.BillingReport;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.service.openstackapi.ResourceManagerService;
import com.cloud.cloudapi.service.rating.BillingService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.StringHelper;

@Service("resourceManagerService")
public class ResourceManagerServiceImpl implements ResourceManagerService{

	@Autowired
	private ResourceCreateProcessMapper resourceCreateProcessMapper;
	
	@Autowired
	private InstanceMapper instanceMapper;
	
	@Resource
	private QuotaService quotaService;

	@Resource
	private QuotaDetailMapper quotaDetailMapper;
	
	@Resource
	private NotificationMapper notificationMapper;
	
	@Resource
	private BillingReportMapper billingReportMapper;
	
	@Resource
	private BillingService billingService;
	
	@Override
	public List<ResourceCreateProcess> getResourceCreateProcesses(TokenOs ostoken){
		List<ResourceCreateProcess> resources = resourceCreateProcessMapper.selectAllByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(resources))
			return resources;
		for(ResourceCreateProcess resource : resources){
			if(!Util.isNullOrEmptyValue(resource.getName()))
				resource.setName(StringHelper.ncr2String(resource.getName()));
			if(null != resource.getBegineSeconds()){
				resource.setBeginTime(Util.millionSecond2Date(resource.getBegineSeconds()));
				resource.setBegineSeconds(null);
			}
			if(null != resource.getCompleteSeconds()){
				resource.setEndTime(Util.millionSecond2Date(resource.getCompleteSeconds()));
				resource.setCompleteSeconds(null);
			}
			resource.setId(null);
		}
		return resources;
	}
	
	
	@Override
	public List<AggregationInfo> getAggregationInfos(TokenOs ostoken) throws BusinessException{
		
		List<AggregationInfo> aggregationInfos = new ArrayList<AggregationInfo>();
		
		String tenantId = ostoken.getTenantid();
	 
		AggregationInfo instanceInfo = new AggregationInfo();
		instanceInfo.setId(Util.makeUUID());
		instanceInfo.setType(ParamConstant.INSTANCE);
		instanceInfo.setStatus(ParamConstant.INFO);
		instanceInfo.setTitle(Message.getMessage(Message.CS_AGGREGATION_INSTANCE_INFO,new Locale(ostoken.getLocale()),false));
		setTenantInstanceInfo(instanceInfo,ostoken);
		aggregationInfos.add(instanceInfo);
		
		AggregationInfo billingInfo = new AggregationInfo();
		billingInfo.setId(Util.makeUUID());
		billingInfo.setType(ParamConstant.BILLING);
		billingInfo.setStatus(ParamConstant.INFO);
		billingInfo.setTitle(Message.getMessage(Message.CS_AGGREGATION_TOTAL_BILLING,new Locale(ostoken.getLocale()),false));
		Float totalBilling  = getTenantBillingInfo(ostoken);
		billingInfo.setValue(Float.toString(totalBilling));
		aggregationInfos.add(billingInfo);
		
		AggregationInfo notificationInfo = new AggregationInfo();
		notificationInfo.setId(Util.makeUUID());
		notificationInfo.setType(ParamConstant.NOTIFICATION);
		notificationInfo.setStatus(ParamConstant.SUCCESS);
		notificationInfo.setTitle(Message.getMessage(Message.CS_AGGREGATION_NOTIFICATION_INFO,new Locale(ostoken.getLocale()),false));
		notificationInfo.setValue(Integer.toString(notificationMapper.countNumByTenantId(tenantId)));
		aggregationInfos.add(notificationInfo);
		
//		AggregationInfo mailInfo = new AggregationInfo();
//		mailInfo.setId(Util.makeUUID());
//		mailInfo.setType(ParamConstant.MAIL);
//		mailInfo.setStatus(ParamConstant.WARNING);
//		mailInfo.setTitle(Message.getMessage(Message.CS_AGGREGATION_MAIL_INFO,new Locale(ostoken.getLocale()),false));
//		mailInfo.setValue(5); //TODO
//		aggregationInfos.add(mailInfo);
		
		return aggregationInfos;

	}
	
	private void setTenantInstanceInfo(AggregationInfo instanceInfo,TokenOs ostoken) throws BusinessException{
		List<Instance> instances = instanceMapper.selectListByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(instances))
			return;
		int active = 0;
		int other = 0;
		for(Instance instance : instances){
			if(instance.getStatus().equalsIgnoreCase(ParamConstant.ACTIVE))
				++active;
			else
				++other;
		}
		instanceInfo.setValue(Integer.toString(instances.size()));
		instanceInfo.setActive(Integer.toString(active));
		instanceInfo.setNonActive(Integer.toString(other));
	}
	
	private float getTenantBillingInfo(TokenOs ostoken) throws BusinessException {
		float totalBilling = 0;
		List<BillingReport> reports = billingReportMapper.selectByTenantId(ostoken.getTenantid());
		if(Util.isNullOrEmptyList(reports))
			return totalBilling;
		
		for (BillingReport report : reports) {
			if(null == report.getCost())
				continue;
			totalBilling += report.getCost();
		}
		return totalBilling;
	}
}
