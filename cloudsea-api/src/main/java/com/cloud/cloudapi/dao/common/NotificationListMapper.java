package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.NotificationList;


/** 
* @author  hous.jy@cn.fujitsu.com (2016-6-2 9:43:38)
* 
*/
public interface NotificationListMapper extends SuperMapper<NotificationList,String> {
	
	public List<NotificationList> selectAllByTenantId(String tenant_id);
	
	public List<NotificationList> selectByIds(String[] ids);

}
