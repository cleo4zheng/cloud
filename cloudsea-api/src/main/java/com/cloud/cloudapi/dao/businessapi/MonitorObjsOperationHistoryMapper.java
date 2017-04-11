package com.cloud.cloudapi.dao.businessapi;

import java.util.List;

import com.cloud.cloudapi.dao.common.SuperMapper;
import com.cloud.cloudapi.pojo.businessapi.zabbix.MonitorObjsOperationHistory;


/** 
* @author  zhangxw
* @create  2016年11月20日 上午11:52:51 
* 
*/
public interface MonitorObjsOperationHistoryMapper extends SuperMapper<MonitorObjsOperationHistory,String> {
	public void insertSelectives(List<MonitorObjsOperationHistory> historys);
}
