package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class Terminal {

	private String id;
	private String type;
	private String content;
	private boolean verified;
	private String notificationListId;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public boolean isVerified() {
		return verified;
	}

	public void setVerified(boolean verified) {
		this.verified = verified;
	}

	public String getNotificationListId() {
		return notificationListId;
	}

	public void setNotificationListId(String notificationListId) {
		this.notificationListId = notificationListId;
	}
	
}
