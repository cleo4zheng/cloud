package com.cloud.cloudapi.pojo.openstackapi.foros;

public class Project {
	private boolean is_domain;
	private String description;
	private Links links;
	private boolean enabled;
	private String id;
	private String parent_id;
	private String domain_id;
	private String name;

	public void setIs_domain(boolean is_domain) {
		this.is_domain = is_domain;
	}

	public boolean getIs_domain() {
		return this.is_domain;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return this.description;
	}

	public void setLinks(Links links) {
		this.links = links;
	}

	public Links getLinks() {
		return this.links;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean getEnabled() {
		return this.enabled;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public void setParent_id(String parent_id) {
		this.parent_id = parent_id;
	}

	public String getParent_id() {
		return this.parent_id;
	}

	public void setDomain_id(String domain_id) {
		this.domain_id = domain_id;
	}

	public String getDomain_id() {
		return this.domain_id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

}
