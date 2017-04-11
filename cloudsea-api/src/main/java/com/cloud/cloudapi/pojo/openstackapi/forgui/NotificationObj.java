package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class NotificationObj{
	private String id;
	private String to;
	private String status;
	private String createdAt;

	private NotificationList notificationList;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
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

	public NotificationList getNotificationList() {
		return notificationList;
	}

	public void setNotificationList(NotificationList notificationList) {
		this.notificationList = notificationList;
	}

}