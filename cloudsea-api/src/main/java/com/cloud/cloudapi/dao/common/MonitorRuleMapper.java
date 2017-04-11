package com.cloud.cloudapi.dao.common;


import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.cloud.cloudapi.pojo.openstackapi.forgui.MonitorRule;

public interface MonitorRuleMapper extends SuperMapper<MonitorRule, String> {
	
	public List<MonitorRule> selectListByMonitor(@Param("monitor_id") String monitor_id); 
	public void insertMonitorRuleRelations(@Param("monitor_rule_id") String monitor_rule_id, @Param("monitor_id") String monitor_id);
	public void deleteMonitorRule(@Param("rule_id") String rule_id);
	public void deleteMonitorRuleRelations(@Param("rule_id") String rule_id, @Param("monitor_id") String monitor_id);
	public void deleteRelationsForMonitorByMonitoId(@Param("monitor_id") String monitor_id);
}
