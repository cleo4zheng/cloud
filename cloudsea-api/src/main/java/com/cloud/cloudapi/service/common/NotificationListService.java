package com.cloud.cloudapi.service.common;

import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.NotificationList;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Terminal;

public interface NotificationListService {
	
	public List<NotificationList> getNotificationLists(Map<String, String> paraMap, TokenOs ostoken) throws BusinessException;
	
	public void insertNotificationList(NotificationList notificationList, TokenOs ostoken) throws BusinessException;
	
	public NotificationList selectNotificationListById(String id) throws BusinessException;
	
	public void deleteNotificationListById(String id, TokenOs ostoken) throws BusinessException;
	
	public void updateNotificationListById(NotificationList notificationList, TokenOs ostoken) throws BusinessException;
	
	public void insertTerminal(Terminal terminal, TokenOs ostoken) throws BusinessException;
	
	public void deleteTerminal(String id, TokenOs ostoken) throws BusinessException;
	
	public void verifyNotificationListTerminal(String id, TokenOs ostoken) throws BusinessException;
}
