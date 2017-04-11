package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.LBPoolMember;

public interface LoadbalancerPoolMemberMapper extends SuperMapper<LBPoolMember,String> {
	
	public Integer insertOrUpdate(LBPoolMember member);
	
	public Integer insertOrUpdateBatch(List<LBPoolMember> members);
	
	public List<LBPoolMember> selectAll();

	public List<LBPoolMember> selectPoolMembersByPoolId(String pool_id);
	
	public List<LBPoolMember> selectListWithLimit(int limit);
	
	public List<LBPoolMember> selectAllForPage(int start, int end);
}