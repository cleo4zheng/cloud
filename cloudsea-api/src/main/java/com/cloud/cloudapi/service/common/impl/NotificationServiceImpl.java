package com.cloud.cloudapi.service.common.impl;



import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.NotificationMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Notification;
import com.cloud.cloudapi.service.common.NotificationService;

@Service
public class NotificationServiceImpl implements NotificationService {
	@Resource
	private NotificationMapper notificationMapper;
	
	@Override
	public List<Notification> getNotificationsPage(Map<String, String> paramMap, TokenOs ostoken) throws BusinessException{
		List<Notification> list  = notificationMapper.selectAllPage(paramMap, ostoken.getTenantid());
		return list;
	}

	@Override
	public Notification getNotification(String id) throws BusinessException {
		// TODO Auto-generated method stub
		notificationMapper.updateRead(id, true);
		Notification notification  = notificationMapper.selectByPrimaryKey(id);
		return notification;
	}

	@Override
	public void updateNotificationRead(String id, Boolean read) throws BusinessException {
		// TODO Auto-generated method stub
		notificationMapper.updateRead(id, read);
	}
	
	@Override
	public void updateNotificationsReadStatus(List<String> notificationIds, Boolean read) throws BusinessException {
		// TODO Auto-generated method stub
		notificationMapper.updateNotificationsRead(notificationIds, read);
	}

}
