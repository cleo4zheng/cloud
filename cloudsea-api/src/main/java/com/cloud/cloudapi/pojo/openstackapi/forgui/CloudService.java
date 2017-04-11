package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.Locale;

import com.cloud.cloudapi.util.Message;

public class CloudService {
	private String id;
	private String name;
	private String type;
	private String status;
	private String tenantId;
	private String createdAt;
	private String description;

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void normalInfo(Locale locale){
		this.setCreatedAt(null);
		this.setTenantId(null);
		this.setName(Message.getMessage(this.getType().toUpperCase(),locale,false));	
		this.setType(null);
	}
}
