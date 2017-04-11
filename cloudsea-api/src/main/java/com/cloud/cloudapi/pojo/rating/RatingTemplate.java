package com.cloud.cloudapi.pojo.rating;

import java.util.List;

public class RatingTemplate {

	private String template_id;
	private String name;
	private String description;
	private String versionIds;
	private Boolean defaultFlag;
	private Long millionSeconds;
	private String createdAt;
	//private List<RatingVersion> versions;
	private List<TemplateVersion> versions;

	public String getTemplate_id() {
		return template_id;
	}

	public void setTemplate_id(String template_id) {
		this.template_id = template_id;
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

	public List<TemplateVersion> getVersions() {
		return versions;
	}

	public void setVersions(List<TemplateVersion> versions) {
		this.versions = versions;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getVersionIds() {
		return versionIds;
	}

	public void setVersionIds(String versionIds) {
		this.versionIds = versionIds;
	}

	public Boolean getDefaultFlag() {
		return defaultFlag;
	}

	public void setDefaultFlag(Boolean defaultFlag) {
		this.defaultFlag = defaultFlag;
	}
}
