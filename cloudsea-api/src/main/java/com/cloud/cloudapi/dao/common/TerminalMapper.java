package com.cloud.cloudapi.dao.common;


import org.apache.ibatis.annotations.Param;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Terminal;

/** 
* @author  hous.jy@cn.fujitsu.com (2016-6-2 13:28:31)
* 
*/
public interface TerminalMapper extends SuperMapper<Terminal,String> {
	public void deleteByNotificationListId(@Param("notification_list_id") String notification_list_id);
	
}
