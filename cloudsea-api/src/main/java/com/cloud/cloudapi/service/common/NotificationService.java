package com.cloud.cloudapi.service.common;

import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Notification;

public interface NotificationService {

	public List<Notification> getNotificationsPage(Map<String,String> paraMap, TokenOs ostoken) throws BusinessException;
	public Notification getNotification(String id) throws BusinessException;
	public void updateNotificationRead(String id, Boolean read) throws BusinessException;
	public void updateNotificationsReadStatus(List<String> notificationIds, Boolean read) throws BusinessException;

}
