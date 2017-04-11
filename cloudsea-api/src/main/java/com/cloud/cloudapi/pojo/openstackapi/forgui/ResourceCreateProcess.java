package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class ResourceCreateProcess {

	private String id;
	private String tenantId;
	private String name;
	private String type;
	private String taskState;
	private String resourceState;
	private String message;
    private Long begineSeconds;
    private Long completeSeconds;
	private String beginTime;
    private String endTime;
    
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
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

	public String getTaskState() {
		return taskState;
	}

	public void setTaskState(String taskState) {
		this.taskState = taskState;
	}

	public String getResourceState() {
		return resourceState;
	}

	public void setResourceState(String resourceState) {
		this.resourceState = resourceState;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Long getBegineSeconds() {
		return begineSeconds;
	}

	public void setBegineSeconds(Long begineSeconds) {
		this.begineSeconds = begineSeconds;
	}

	public Long getCompleteSeconds() {
		return completeSeconds;
	}

	public void setCompleteSeconds(Long completeSeconds) {
		this.completeSeconds = completeSeconds;
	}

	public String getBeginTime() {
		return beginTime;
	}

	public void setBeginTime(String beginTime) {
		this.beginTime = beginTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

}
