package com.cloud.cloudapi.dao.common;


import com.cloud.cloudapi.pojo.crm.UserBindInfo;

public interface UserBindInfoMapper extends SuperMapper<UserBindInfo, String> {
	
	public UserBindInfo getLastBindActionByUserId(String userId);
	
}
