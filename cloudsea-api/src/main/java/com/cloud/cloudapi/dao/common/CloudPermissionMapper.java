package com.cloud.cloudapi.dao.common;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.cloud.cloudapi.pojo.common.CloudPermission;

public interface CloudPermissionMapper extends SuperMapper<CloudPermission,String>{
	
	public int countNum();
	
	public List<CloudPermission> selectList();
	
	public List<CloudPermission> selectListForPage(int start,int end);
	
	public List<CloudPermission> selectListByRoleId(String roleid);

	public int insertPermissionToRole(@Param(value = "id") String id,@Param(value="roleid") String roleid,@Param(value="permissionid") String permissionid); 
	
	public int deletePermissionsFromRole(String roleid);
	
}
