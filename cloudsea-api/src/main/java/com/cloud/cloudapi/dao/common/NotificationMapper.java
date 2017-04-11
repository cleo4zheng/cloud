package com.cloud.cloudapi.dao.common;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Notification;


/** 
* @author  zhangxw@cn.fujitsu.com (2016-6-22)
* 
*/
public interface NotificationMapper extends SuperMapper<Notification,String> {
	
	public List<Notification> selectAllPage(@Param("paramMap") Map<String,String> paramMap, @Param("tenant_id") String tenant_id);
	
	public List<Notification> selectByIds(String[] ids);
	
	public int updateRead(@Param("id") String id, @Param("read") Boolean read);

	public Integer countNumByTenantId(String tenantId);
	
	public Notification selectLastByCreatedAt(@Param("tenant_id") String tenant_id);

	public void updateNotificationsRead(@Param("ids")List<String> notificationIds, @Param("read") Boolean read);
}
