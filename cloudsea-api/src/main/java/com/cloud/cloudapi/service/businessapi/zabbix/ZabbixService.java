package com.cloud.cloudapi.service.businessapi.zabbix;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Monitor;
import com.cloud.cloudapi.pojo.openstackapi.forgui.MonitorObj;

/** 
* @author  zhangxw@cn.fujitsu.com 
* @create  2016/7/28 10:35:06 
* 
*/

public interface ZabbixService {
	public void addMonitorObjs(Monitor monitor, List<MonitorObj> monitorObjs);
	public void addMonitorObjsByMonitor(Monitor monitor);
	
	public void deleteMonitorObj(String objId);
	public void deleteMonitorObjByMonitorId(String monitorId, String objId);
	public void deleteMonitorObjs(List<String> monitorId);
	public void deleteMonitorObjsByMonitorId(String monitorId);
	
	public void updateMonitorObjName(String objId, String name);
}