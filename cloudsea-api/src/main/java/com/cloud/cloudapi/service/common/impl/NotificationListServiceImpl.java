package com.cloud.cloudapi.service.common.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.NotificationListMapper;
import com.cloud.cloudapi.dao.common.NotificationObjMapper;
import com.cloud.cloudapi.dao.common.TerminalMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.NotificationList;
import com.cloud.cloudapi.pojo.openstackapi.forgui.NotificationObj;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Terminal;
import com.cloud.cloudapi.service.common.NotificationListService;
import com.cloud.cloudapi.util.DictConstant;

@Service("notificationListService")
public class NotificationListServiceImpl implements NotificationListService {

	@Autowired
	private NotificationListMapper notificationListMapper;
	
	@Autowired
	private TerminalMapper terminalMapper;
	
	@Resource
	private CloudConfig cloudConfig; 
	
	@Resource
	private NotificationObjMapper notificationObjMapper;
		
	@Override
	public List<NotificationList> getNotificationLists(Map<String, String> paraMap, TokenOs ostoken) throws BusinessException {

		List<NotificationList> notificationLists = new ArrayList<>();
		
		notificationLists = notificationListMapper.selectAllByTenantId(ostoken.getTenantid());
		
		return notificationLists;
	}

	@Override
	public void insertNotificationList(NotificationList notificationList, TokenOs ostoken) throws BusinessException {
		
		String uuid = Util.makeUUID();
		notificationList.setId(uuid);
		notificationList.setTenant_id(ostoken.getTenantid());
	
		notificationListMapper.insertSelective(notificationList);
		
		if(notificationList.getTerminals() != null) {
			List<Terminal> terminals = notificationList.getTerminals();
			for(Terminal terminal : terminals) {
				terminal.setNotificationListId(uuid);
				//@TODO now set the verfied = true
				terminal.setVerified(true);
				terminalMapper.insertSelective(terminal);
			}

		}	
	}
	
	@Override
	public NotificationList selectNotificationListById(String id) throws BusinessException {
		NotificationList notificationList =  notificationListMapper.selectByPrimaryKey(id);
		return notificationList;
	}

	@Override
	public void deleteNotificationListById(String id, TokenOs ostoken) throws BusinessException {
		terminalMapper.deleteByNotificationListId(id);
		List<NotificationObj> notificationObjs = notificationObjMapper.selectByTo(id);
		for(NotificationObj notificationObj : notificationObjs){
			notificationObjMapper.deleteNotificationObjRelationsByNOId(notificationObj.getId());
		}
		notificationObjMapper.deleteNotificationObjByTo(id);
		notificationListMapper.deleteByPrimaryKey(id);
	}
	
	//update name and description not need operate zabbix
	@Override
	public void updateNotificationListById(NotificationList notificationList, TokenOs ostoken) throws BusinessException {
		notificationListMapper.updateByPrimaryKeySelective(notificationList);
	}

	@Override
	public void insertTerminal(Terminal terminal, TokenOs ostoken) throws BusinessException {
		//@TODO now set the verified = true ,because have not the verify module
		terminal.setVerified(true);
		
		terminalMapper.insertSelective(terminal);
	}

	@Override
	public void deleteTerminal(String id, TokenOs ostoken) throws BusinessException {
		terminalMapper.deleteByPrimaryKey(id);
	}
	
	@Override
	public void verifyNotificationListTerminal(String id, TokenOs ostoken) throws BusinessException {		
		//@TODO how to verify?????
		Terminal terminal = new Terminal();
		terminal.setId(id);
		terminal.setVerified(true);
		terminalMapper.updateByPrimaryKeySelective(terminal);
	}	
}
