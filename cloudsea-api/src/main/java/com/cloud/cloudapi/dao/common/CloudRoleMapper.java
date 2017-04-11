package com.cloud.cloudapi.dao.common;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.cloud.cloudapi.pojo.common.CloudRole;

public interface CloudRoleMapper extends SuperMapper<CloudRole,String>{
	
	public int countNum();
	
	public Integer insertOrUpdate(CloudRole role);
	
	public Integer insertOrUpdateBatch(List<CloudRole> roles);
	
	public  List<CloudRole> selectList();
	
	public List<CloudRole> selectListForPage(int start,int end);
	
	public List<CloudRole> selectListByUserId(String userid);
	
	public List<CloudRole> selectRoleByIds(List<String> ids);
	
	public int insertRoleToUser(@Param(value = "id") String id,@Param(value="userid") String userid,@Param(value="roleid") String roleid); 
	
	public int deleteRolesFromUser(String userid);
	
    public CloudRole selectByRoleName(String roleName);
    

}
