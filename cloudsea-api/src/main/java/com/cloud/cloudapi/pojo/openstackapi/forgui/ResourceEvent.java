package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class ResourceEvent {

	private Integer id;
	private String tenantId;
	private String resourceId;
	private String resourceType;
	private String beginState;
	private String endState;
	private Long millionSeconds;

	public ResourceEvent(){
	//	this.id = Util.makeUUID();
	}
	
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public String getBeginState() {
		return beginState;
	}

	public void setBeginState(String beginState) {
		this.beginState = beginState;
	}

	public String getEndState() {
		return endState;
	}

	public void setEndState(String endState) {
		this.endState = endState;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

}
