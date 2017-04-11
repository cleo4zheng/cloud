package com.cloud.cloudapi.pojo.openstackapi.foros;

public class Role {
	private String domain_id;
	
	private String id;

	private Links links;

	private String name;
	

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

}
