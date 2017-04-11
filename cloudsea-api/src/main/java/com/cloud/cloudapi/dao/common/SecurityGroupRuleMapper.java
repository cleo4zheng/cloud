package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.SecurityGroupRule;

public interface SecurityGroupRuleMapper extends SuperMapper<SecurityGroupRule, String> {
	public Integer countNum();
	
	public Integer insertOrUpdate(SecurityGroupRule rule);
	
	public Integer insertOrUpdateBatch(List<SecurityGroupRule> rules);
	
	public List<SecurityGroupRule> selectAllList();
	
	public List<SecurityGroupRule> selectListBySecurityGroupId(String securityGroupId);

	public List<SecurityGroupRule> selectListBySecurityGroupRuleIds(String[] ids);
	
	public List<SecurityGroupRule> selectListForPage(int start, int end);

}
