package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;

public class FirewallPolicy {
	private String id;
	private String tenant_id;
	private String name;
	private Boolean audited;
	private Boolean shared;
	private String description;
	private String ruleIds;
	private List<String> firewall_rules;
	private List<FirewallRule> firewallRules;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTenant_id() {
		return tenant_id;
	}

	public void setTenant_id(String tenant_id) {
		this.tenant_id = tenant_id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getAudited() {
		return audited;
	}

	public void setAudited(Boolean audited) {
		this.audited = audited;
	}

	public Boolean getShared() {
		return shared;
	}

	public void setShared(Boolean shared) {
		this.shared = shared;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getRuleIds() {
		return ruleIds;
	}

	public void setRuleIds(String ruleIds) {
		this.ruleIds = ruleIds;
	}

	public List<FirewallRule> getFirewallRules() {
		return firewallRules;
	}

	public void setFirewallRules(List<FirewallRule> firewallRules) {
		this.firewallRules = firewallRules;
	}

	public List<String> getFirewall_rules() {
		return firewall_rules;
	}

	public void setFirewall_rules(List<String> firewall_rules) {
		this.firewall_rules = firewall_rules;
	}
}
