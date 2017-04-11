package com.cloud.cloudapi.pojo.openstackapi.forgui;

import com.cloud.cloudapi.pojo.common.Util;

public class PortalSkin {

	private String id;
	private String userId;
	private String name;
	private String skin;

	public PortalSkin(){
		this.id = Util.makeUUID();
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSkin() {
		return skin;
	}

	public void setSkin(String skin) {
		this.skin = skin;
	}

}
