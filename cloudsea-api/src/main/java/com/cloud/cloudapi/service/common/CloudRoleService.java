package com.cloud.cloudapi.service.common;

import java.io.IOException;
import java.util.List;

import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudRole;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface CloudRoleService{
	
	public List<CloudRole> getRoles(TokenOs ostoken) throws ResourceBusinessException;
	public List<CloudRole> getUserRoles(TokenOs ostoken,String name) throws ResourceBusinessException;
	public CloudRole createRoles(String body,TokenOs ostoken) throws ResourceBusinessException, JsonProcessingException, IOException;
	//public CloudRole removeRole(String roleId,String userId,TokenOs ostoken) throws ResourceBusinessException;
	public CloudRole updateRole(TokenOs ostoken,String id,String body) throws ResourceBusinessException;
	//public void addUsers(String id,String body,TokenOs ostoken) throws ResourceBusinessException;
	public void deleteRole(String roleId,TokenOs ostoken) throws ResourceBusinessException;
	public void checkUserPermission(TokenOs ostoken,String operation) throws ResourceBusinessException;
	public void checkUserPermission(TokenOs ostoken,String operation,String instanceId) throws ResourceBusinessException;
}
