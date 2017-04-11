package com.cloud.cloudapi.service.openstackapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SecurityGroup;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public interface SecurityGroupService {
	public List<SecurityGroup> getSecurityGroupList(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public List<SecurityGroup> getInstanceAttachedSecurityGroup(JsonNode instanceNode,String tenantId) throws BusinessException;
	public SecurityGroup addSecurityGroupRule(String securityGroupId,String createBody,TokenOs ostoken)  throws BusinessException, JsonProcessingException, IOException;
	public SecurityGroup removeSecurityGroupRule(String securityGroupId,String securityGroupRuleId,TokenOs ostoken) throws BusinessException;
	public void addSecurityGroupToPort(String securityGroupId,String updateBody,TokenOs ostoken) throws BusinessException;
	public void removeSecurityGroupFromPort(String securityGroupId,String updateBody,TokenOs ostoken) throws BusinessException;

	public SecurityGroup getSecurityGroup(String securityGroupId,TokenOs ostoken) throws BusinessException;
	public SecurityGroup getSecurityGroupByName(String securityGroupName,TokenOs ostoken) throws BusinessException;
	public SecurityGroup createSecurityGroup(String createBody,TokenOs ostoken)  throws BusinessException, JsonProcessingException, IOException;
	public SecurityGroup updateSecurityGroup(String securityGroupId,String updateBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public void deleteSecurityGroup(String securityGroupId,TokenOs ostoken) throws BusinessException;
	public Boolean hasDefaultSecurityGroup(String tenantId);
	public void makeTenantDefaultSecurityGroup(String securityGroupId,TokenOs ostoken);
}
