package com.cloud.cloudapi.dao.common;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.cloud.cloudapi.pojo.openstackapi.forgui.MonitorObj;

public interface MonitorObjMapper extends SuperMapper<MonitorObj, String> {
	public List<MonitorObj> selectAllList();
	public List<MonitorObj> selectListByMonitorId(String monitor_id);
	public List<MonitorObj> selectListById(String id);
	public void deleteResource( @Param("id") String id,  @Param("monitor_id") String monitor_id);
	public void deleteResourcesByIds( @Param("ids") List<String> ids);
	public void deleteResourceByMonitorId(@Param("monitor_id") String monitor_id);
	public void updateName(@Param("id") String id, @Param("name") String name);
}
