package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.List;

public class OperationLog {

	private String id;
	private String title;
	private String user;
	private String tenantId;
	private String status;
	private String details;
	private String resourcesId;
	private String resourceType;
	private String timestamp;
	private long millionSeconds;
	private List<OperationResource> resources;

	public OperationLog(){
		this.resources = new ArrayList<OperationResource>();
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public String getResourcesId() {
		return resourcesId;
	}

	public void setResourcesId(String resourcesId) {
		this.resourcesId = resourcesId;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	public List<OperationResource> getResources() {
		return resources;
	}

	public void setResources(List<OperationResource> resources) {
		this.resources = resources;
	}

	public void addResource(OperationResource resource) {
		this.resources.add(resource);
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}
	
}
