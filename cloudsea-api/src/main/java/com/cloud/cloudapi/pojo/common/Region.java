package com.cloud.cloudapi.pojo.common;

import com.cloud.cloudapi.pojo.openstackapi.foros.Links;

public class Region {

	private String id;
	private String name;
	private String description;
	private String parent_region_id;
	private Links links;

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

	public String getParent_region_id() {
		return parent_region_id;
	}

	public void setParent_region_id(String parent_region_id) {
		this.parent_region_id = parent_region_id;
	}

	public Links getLinks() {
		return links;
	}

	public void setLinks(Links links) {
		this.links = links;
	}
}
