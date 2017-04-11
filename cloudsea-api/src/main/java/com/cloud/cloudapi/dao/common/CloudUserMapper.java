package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.common.CloudUser;

public interface CloudUserMapper extends SuperMapper<CloudUser,String>{
	
	public int countNum();
	
	public int countNumByUserAccount(String code);
	public CloudUser  selectByUserInfo(CloudUser user);
	public CloudUser  selectByGuiTokenId(String guitokenid);
	public CloudUser  selectByOsTokenId(String ostokenid);
	public List<CloudUser> selectUserByIds(List<String> ids);
	public List<CloudUser> selectUsersByTenantId(String parentId);
	public Integer insertOrUpdateBatch(List<CloudUser> users);

	/**
	 * 带有tenant信息的clouduser信息
	 * @param id
	 * @return
	 */
	public CloudUser  selectWithTenantByPrimaryKey(String id);
	public  List<CloudUser> selectList();
	/**
	 * 带有tenant信息的clouduser列表
	 * @param id
	 * @return
	 */
	public  List<CloudUser> selectListWithTenant();
	
	public List<CloudUser> selectListForPage(int start,int end);
	
	public CloudUser selectByAccount(String account);
	
}
