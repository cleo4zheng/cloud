package com.cloud.cloudapi.service.openstackapi;

import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SecurityGroupRule;

public interface SecurityGroupRuleService {
	public List<SecurityGroupRule> getSecurityGroupRuleList(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public SecurityGroupRule createSecurityGroupRule(String createBody,TokenOs ostoken) throws BusinessException;
	public List<SecurityGroupRule> createSecurityGroupRule(List<SecurityGroupRule> rulesCreateInfo,String securityGroupName,TokenOs ostoken) throws BusinessException;
	public SecurityGroupRule getSecurityGroupRule(String securityGroupRuleId,TokenOs ostoken) throws BusinessException;
    public void deleteSecurityGroupRule(String securityGroupRuleId,TokenOs ostoken) throws BusinessException;
}
