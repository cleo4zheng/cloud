package com.cloud.cloudapi.service.openstackapi;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.foros.User;

public interface UserService {
	
	public User createUser(User user,TokenOs admintoken) throws BusinessException;
	public void deleteUser(String userId,TokenOs admintoken) throws BusinessException;
	public void addUserToProject(CloudUser user,String projectId,TokenOs ostoken) throws BusinessException;
	public void removeUserFromProject(CloudUser user,String projectId,TokenOs ostoken) throws BusinessException;

}
