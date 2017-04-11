package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class MonitorObj {
	private String monitor_id;
	private String id;
	private String name;
	private String status;
	private String type;
	private String createdAt;
	

	public String getMonitorId() {
		return monitor_id;
	}

	public void setMonitorId(String monitor_id) {
		this.monitor_id = monitor_id;
	}

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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
