package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class SyncResource {

	private String id;
	private String type;
	private String orgStatus;
	private String expectedStatus;
	private String syncStatus;
    private String relatedResource;
    private String region;
    
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

	public String getOrgStatus() {
		return orgStatus;
	}

	public void setOrgStatus(String orgStatus) {
		this.orgStatus = orgStatus;
	}

	public String getExpectedStatus() {
		return expectedStatus;
	}

	public void setExpectedStatus(String expectedStatus) {
		this.expectedStatus = expectedStatus;
	}

	public String getSyncStatus() {
		return syncStatus;
	}

	public void setSyncStatus(String syncStatus) {
		this.syncStatus = syncStatus;
	}

	public String getRelatedResource() {
		return relatedResource;
	}

	public void setRelatedResource(String relatedResource) {
		this.relatedResource = relatedResource;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

}
