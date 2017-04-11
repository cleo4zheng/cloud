package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.util.StringHelper;

public class SecurityGroup {
	private String id;
	private String name;
	private String description;
	private String tenantId;
	private String createdAt;
	private String securityGroupRuleIds;
	private Long millionSeconds;
	private List<SecurityGroupRule> securityGroupRules;
	private List<Port> ports;

	public SecurityGroup() {
//		this.id = new String();
//		this.name = new String();
//		this.description = new String();
//		this.createdAt = new String();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public List<SecurityGroupRule> getSecurityGroupRules() {
		return securityGroupRules;
	}

	public void setSecurityGroupRules(List<SecurityGroupRule> securityGroupRules) {
		this.securityGroupRules = securityGroupRules;
	}

	public void addSecurityGroupRule(SecurityGroupRule secutiryGroupRule) {
		this.securityGroupRules.add(secutiryGroupRule);
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getSecurityGroupRuleIds() {
		return securityGroupRuleIds;
	}

	public void setSecurityGroupRuleIds(String securityGroupRuleIds) {
		this.securityGroupRuleIds = securityGroupRuleIds;
	}

	public void makeSecurityGroupRuleIds() {
		if(Util.isNullOrEmptyList(this.securityGroupRules))
			return;
		List<String> detailsId = new ArrayList<String>();
		for(SecurityGroupRule rule : this.securityGroupRules){
			detailsId.add(rule.getId());
		}
		this.securityGroupRuleIds = Util.listToString(detailsId, ',');
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}
	
	public List<Port> getPorts() {
		return ports;
	}

	public void setPorts(List<Port> ports) {
		this.ports = ports;
	}

	public void normalInfo(Boolean normalRuleInfo){
		this.setName(StringHelper.ncr2String(this.getName()));
		this.setDescription(StringHelper.ncr2String(this.getDescription()));
		this.setSecurityGroupRuleIds(null);
		this.setTenantId(null);
		if(null != this.getMillionSeconds()){
			this.setCreatedAt(Util.millionSecond2Date(this.getMillionSeconds()));
			this.setMillionSeconds(null);	
		}
		if(null != this.ports){
			for(Port port : this.ports){
				port.setSecurity_groups(null);
				port.setFloatingIp(null);
                port.setSubnet(null);
				port.normalInfo(true);
			}	
		}
		if(true == normalRuleInfo)
			this.setSecurityGroupRules(null);
	}

}
