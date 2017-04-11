package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class AggregationInfo {

	private String id;
	private String status;
	private String type;
	private String title;
	private String value;
    private String active;
    private String nonActive;
    
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getActive() {
		return active;
	}

	public void setActive(String active) {
		this.active = active;
	}

	public String getNonActive() {
		return nonActive;
	}

	public void setNonActive(String nonActive) {
		this.nonActive = nonActive;
	}

	
}
