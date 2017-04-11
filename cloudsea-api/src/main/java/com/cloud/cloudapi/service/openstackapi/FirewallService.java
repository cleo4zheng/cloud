package com.cloud.cloudapi.service.openstackapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Firewall;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FirewallPolicy;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FirewallRule;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface FirewallService {
	public List<Firewall> getFirewalls(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public Firewall getFirewall(String firewallId,TokenOs ostoken) throws BusinessException;
	public Firewall createFirewall(String createBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public Firewall updateFirewall(String firewalId,String updateBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public Firewall bindRouter(String firewalId,String updateBody,TokenOs ostoken) throws BusinessException;
	public Firewall removeRouter(String firewalId,String routerId,TokenOs ostoken) throws BusinessException;
	public void deleteFirewall(String firewallId,TokenOs ostoken) throws BusinessException;
	public List<FirewallPolicy> getFirewallPolices(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public FirewallPolicy getFirewallPolicy(String firewallPolicyId,TokenOs ostoken) throws BusinessException;
	public FirewallPolicy createFirewallPolicy(String createBody,TokenOs ostoken) throws BusinessException;
	public void deleteFirewallPolicy(String firewallPolicyId,TokenOs ostoken) throws BusinessException;
	public FirewallPolicy addFirewallRuleToPolicy(String policyId,String createBody,TokenOs ostoken) throws BusinessException;
	public FirewallPolicy removeFirewallRuleFromPolicy(String policyId,String removeBody,TokenOs ostoken) throws BusinessException;
	public List<FirewallRule> getFirewallRules(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public FirewallRule getFirewallRule(String firewallRuleId,TokenOs ostoken) throws BusinessException;
	public FirewallRule createFirewallRule(String createBody,TokenOs ostoken) throws BusinessException;
	public void deleteFirewallRule(String firewallRuleId,TokenOs ostoken) throws BusinessException;
}
