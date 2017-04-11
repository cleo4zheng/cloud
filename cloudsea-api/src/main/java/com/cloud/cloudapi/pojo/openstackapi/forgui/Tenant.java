package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;

import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.util.StringHelper;

public class Tenant {

	private String id;
	private String domain_id;
	private String parent_id;
	private String name;
	private String description;
	private Boolean enabled;
	private String quota_template_id;
	private String monitor_template_id;
	private Long millionSeconds;
    private List<Tenant> tenants;
    private List<CloudUser> users;
    
	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public String getDomain_id() {
		return domain_id;
	}

	public void setDomain_id(String domain_id) {
		this.domain_id = domain_id;
	}

	public String getParent_id() {
		return parent_id;
	}

	public void setParent_id(String parent_id) {
		this.parent_id = parent_id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return this.description;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Boolean getEnabled() {
		return this.enabled;
	}

	public List<Tenant> getTenants() {
		return tenants;
	}

	public void setTenants(List<Tenant> tenants) {
		this.tenants = tenants;
	}

	public List<CloudUser> getUsers() {
		return users;
	}

	public void setUsers(List<CloudUser> users) {
		this.users = users;
	}
	
	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	public String getQuota_template_id() {
		return quota_template_id;
	}

	public void setQuota_template_id(String quota_template_id) {
		this.quota_template_id = quota_template_id;
	}

	public String getMonitor_template_id() {
		return monitor_template_id;
	}

	public void setMonitor_template_id(String monitor_template_id) {
		this.monitor_template_id = monitor_template_id;
	}

	public void normalInfo(){
		this.domain_id = null;
		this.parent_id = null;
		this.setName(StringHelper.ncr2String(this.getName()));
		this.setMillionSeconds(null);
	    if(null != this.users){
	    	for(CloudUser user : this.users)
	    		user.normalInfo();
	    }
	    if(null != this.tenants){
	    	for(Tenant tenant : this.tenants)
	    		tenant.normalInfo();
	    }
	}
}


