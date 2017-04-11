package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.util.StringHelper;

public class Firewall {

	private String id;
	private String name;
	private String tenant_id;
	private String firewall_policy_id;
	private String status;
	private String routerIds;
	private Boolean admin_state_up;
	private String description;
	private String createdAt;
	private String ruleIds;
	private Long millionSeconds;
	private List<Router> routers;
    private List<FirewallRule> rules;
    private List<String> router_ids;
    
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

	public String getTenant_id() {
		return tenant_id;
	}

	public void setTenant_id(String tenant_id) {
		this.tenant_id = tenant_id;
	}

	public String getFirewall_policy_id() {
		return firewall_policy_id;
	}

	public void setFirewall_policy_id(String firewall_policy_id) {
		this.firewall_policy_id = firewall_policy_id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getRouterIds() {
		return routerIds;
	}

	public void setRouterIds(String routerIds) {
		this.routerIds = routerIds;
	}

	public Boolean getAdmin_state_up() {
		return admin_state_up;
	}

	public void setAdmin_state_up(Boolean admin_state_up) {
		this.admin_state_up = admin_state_up;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<Router> getRouters() {
		return routers;
	}

	public void setRouters(List<Router> routers) {
		this.routers = routers;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getRuleIds() {
		return ruleIds;
	}

	public void setRuleIds(String ruleIds) {
		this.ruleIds = ruleIds;
	}

	public List<FirewallRule> getRules() {
		return rules;
	}

	public void setRules(List<FirewallRule> rules) {
		this.rules = rules;
	}

	public List<String> getRouter_ids() {
		return router_ids;
	}

	public void setRouter_ids(List<String> router_ids) {
		this.router_ids = router_ids;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}
	
	public void normalInfo(Boolean normalDetailInfo){
		this.setName(StringHelper.ncr2String(this.getName()));
		this.setDescription(StringHelper.ncr2String(this.getDescription()));
		this.setAdmin_state_up(null);
		this.setFirewall_policy_id(null);
		if(null != this.getMillionSeconds()){
			this.setCreatedAt(Util.millionSecond2Date(this.getMillionSeconds()));
			this.setMillionSeconds(null);
		}
		
		this.setRouter_ids(null);
		this.setRouterIds(null);
		this.setTenant_id(null);
		this.setRuleIds(null);
		if(true == normalDetailInfo){
			List<FirewallRule> rules = this.getRules();
			if(null != rules){
				for(FirewallRule rule : rules){
					rule.normalInfo();
				}
			}
			
			List<Router> routers = this.getRouters();
			if(null != routers){
				for(Router router : routers){
					router.normalInfo();
				}
			}
		}
		else{
			this.setRules(null);
			this.setRouters(null);
		}
		
		
	}
}
