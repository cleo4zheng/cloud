package com.cloud.cloudapi.dao.common;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.cloud.cloudapi.pojo.openstackapi.forgui.MonitorHistory;


public interface MonitorHistoryMapper extends SuperMapper<MonitorHistory, String> {
	public List<MonitorHistory> selectListByMonitorObjIdAndDataTime(@Param("monitor_obj_id") String monitorObjId, @Param("start_time") String startTime, @Param("end_time") String endTime, @Param("data_type") String data_type);

}
