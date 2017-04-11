package com.cloud.cloudapi.service.common;

import java.util.List;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.common.TokenGui;
import com.cloud.cloudapi.pojo.common.TokenOs;

public interface CloudUserService extends SuperDaoService<CloudUser,String>{
	public int countNum() throws ResourceBusinessException, BusinessException, Exception;
	
	public List<CloudUser> selectList() throws ResourceBusinessException,BusinessException, Exception;
	
	public List<CloudUser> selectListForPage(int start,int end)throws ResourceBusinessException,BusinessException, Exception;
	
	public CloudUser selectUserByOsTokenId(String osTokenId) throws ResourceBusinessException,BusinessException, Exception;
	
	public CloudUser selectUserByGuiTokenId(String guiTokenId) throws ResourceBusinessException,BusinessException, Exception;
	
	public CloudUser insertUserAndTenant(CloudUser user,TokenOs adminToken) throws ResourceBusinessException,BusinessException, Exception;
	
//	public void addRoles(String id,String body,TokenOs ostoken) throws ResourceBusinessException;

	/**
	 * 检查用户是否为admin用户
	 * @param clouduserId
	 * @return
	 */
	public Boolean checkIsAdmin(TokenOs ostoken);
	public Boolean checkIsAdmin(String clouduserId);
	public Boolean checkIsSystemAdmin(String clouduserId);
	
	public List<CloudUser> getCloudUsersList() throws Exception;
	public CloudUser getCloudUserDetail(TokenOs token, String userId) throws BusinessException;
	public String updateUserProject(TokenGui token,String name) throws BusinessException;
	public CloudUser operateUser(String userId,String action) throws Exception;
	public void addEndPointsNoOSToObject(TokenOs userToken);
	public void insertWorkFlowEndPointsToDb(String ostenantid,List<String> insertEndpoints);
	public int bindddh(String userId, String ddh) throws Exception;
	public int unbindddh(String userId, String ddh) throws Exception;
}
