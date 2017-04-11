package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.FirewallRule;

public interface FirewallRuleMapper  extends SuperMapper<FirewallRule,String> {
	
	public Integer insertOrUpdate(FirewallRule firewallRule);
	
	public List<FirewallRule> selectAll();

	public List<FirewallRule> selectAllByTenantId(String tenantId);
	
	public int deleteRulesById(String[] ids);
	
	public List<FirewallRule> selectListWithLimit(int limit);
	
	public List<FirewallRule> selectAllByTenantIdWithLimit(String tenantId,int limit);
	
	public List<FirewallRule> selectAllForPage(int start, int end);
}
