package com.cloud.cloudapi.dao.common;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.cloud.cloudapi.pojo.openstackapi.forgui.NotificationObj;



/** 
* @author  zhangxw@cn.fujitsu.com (2016-6-22)
* 
*/
public interface NotificationObjMapper extends SuperMapper<NotificationObj,String> {
	public List<NotificationObj> selectByMonitorId(String monitor_id);
	public List<NotificationObj> selectByTo(String to);
	public void insertNotificationObjRelations(@Param("notification_obj_id") String notificaiton_obj_id, @Param("monitor_id") String monitor_id);
	public void deleteNotificationObj(@Param("notification_obj_id") String notification_obj_id);
	public void deleteNotificationObjRelations(@Param("notification_obj_id") String notification_obj_id, @Param("monitor_id") String monitor_id);
	public void deleteNotificationObjByTo(@Param("notification_list_id") String notification_list_id);
	public void deleteNotificationObjRelationsByNOId(@Param("notification_obj_id") String notification_obj_id);
	public void deleteMonitorNotificationObjsByMonitorId(@Param("monitor_id") String monitor_id);
}
