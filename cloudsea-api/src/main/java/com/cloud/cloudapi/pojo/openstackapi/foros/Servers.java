package com.cloud.cloudapi.pojo.openstackapi.foros;

import java.util.List;

public class Servers {
	private String id;

	private List<Links> links;

	private String name;

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public void setLinks(List<Links> links) {
		this.links = links;
	}

	public List<Links> getLinks() {
		return this.links;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

}
