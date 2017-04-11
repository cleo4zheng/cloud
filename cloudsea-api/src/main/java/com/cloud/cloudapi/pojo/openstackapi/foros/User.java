package com.cloud.cloudapi.pojo.openstackapi.foros;

public class User {
	private String default_project_id;

	private String description;

	private String domain_id;

	private String email;

	private boolean enabled;

	private String id;

	private Links links;

	private String name;
	private String password;

	public void setDefault_project_id(String default_project_id) {
		this.default_project_id = default_project_id;
	}

	public String getDefault_project_id() {
		return this.default_project_id;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDomain_id(String domain_id) {
		this.domain_id = domain_id;
	}

	public String getDomain_id() {
		return this.domain_id;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getEmail() {
		return this.email;
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

	public void setLinks(Links links) {
		this.links = links;
	}

	public Links getLinks() {
		return this.links;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	

}
