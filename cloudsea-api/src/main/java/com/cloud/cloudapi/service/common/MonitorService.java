package com.cloud.cloudapi.service.common;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.monitor.MonitorTemplate;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Monitor;


public interface MonitorService {	
	public List<Monitor> getMonitorListPage(Map<String, String>paramMap, TokenOs ostoken) throws BusinessException;
	public Monitor getMonitorById(String id, TokenOs ostoken) throws BusinessException;
	public void createMonitor(String createBody, TokenOs ostoken) throws BusinessException;
	public void updateMonitor(String updateBody, TokenOs ostoken) throws BusinessException;
	public void addResource(String addBody, TokenOs ostoken) throws BusinessException;
	public void deleteResource(String monitorId, String resourceId, TokenOs ostoken) throws BusinessException;
	public void addRule(String monitorId, String addBody, TokenOs ostoken) throws BusinessException;
	public void deleteRule(String ruleId, String monitorId, TokenOs ostoken) throws BusinessException;
	public void addNotificationList(String addBody, TokenOs ostoken) throws BusinessException;
	public void deleteNotificationObj(String notificationObjId, String monitorId, TokenOs ostoken) throws BusinessException;
	public void deleteMonitor(String monitorId, TokenOs ostoken) throws BusinessException;
	public void actionMonitor(String id, String actionBody, TokenOs ostoken) throws BusinessException;
	public String getMonitorNewConfigInfo() throws BusinessException;
	public String getVMCpuUsageStatics(Map<String, Object> paramMap,Locale locale) throws BusinessException;
	public String getVMMemoryUsageStatics(Map<String, Object> paramMap,Locale locale) throws BusinessException;
	public String getVMDiskUsageStatics(Map<String, Object> paramMap,Locale locale) throws BusinessException;
	public String getVMNetworkUsageStatics(Map<String, Object> paramMap,Locale locale) throws BusinessException;
	public String getPhysicalCpuUsageStatics(Map<String, Object> paramMap,Locale locale) throws BusinessException;
	public String getPhysicalMemoryUsageStatics(Map<String, Object> paramMap,Locale locale) throws BusinessException;
	public String getPhysicalDiskUsageStatics(Map<String, Object> paramMap,Locale locale) throws BusinessException;
	public String getPhysicalNetworkUsageStatics(Map<String, Object> paramMap,Locale locale) throws BusinessException;
	
	public List<MonitorTemplate> getMonitorTemplates(Map<String, String>paramMap, TokenOs ostoken) throws BusinessException;
	public MonitorTemplate getMonitorTemplate(String id,TokenOs ostoken) throws BusinessException;
	public MonitorTemplate createMonitorTemplate(String body,TokenOs ostoken) throws BusinessException;
	public void deleteMonitorTemplate(String id,TokenOs ostoken) throws BusinessException;
	public MonitorTemplate updateMonitorTemplate(String id,String body,TokenOs ostoken) throws BusinessException;
	public void applyMonitorTemplate(String id,String tenantId,TokenOs ostoken) throws BusinessException;
}
