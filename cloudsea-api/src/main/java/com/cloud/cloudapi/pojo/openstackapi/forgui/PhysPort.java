package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class PhysPort {

	private String address;
	private String created_at;
	private String node_uuid;
	private Boolean pxe_enabled;
	private String updated_at;
	private String uuid;
	private Long millionSeconds;

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getCreated_at() {
		return created_at;
	}

	public void setCreated_at(String created_at) {
		this.created_at = created_at;
	}

	public String getNode_uuid() {
		return node_uuid;
	}

	public void setNode_uuid(String node_uuid) {
		this.node_uuid = node_uuid;
	}

	public Boolean getPxe_enabled() {
		return pxe_enabled;
	}

	public void setPxe_enabled(Boolean pxe_enabled) {
		this.pxe_enabled = pxe_enabled;
	}

	public String getUpdated_at() {
		return updated_at;
	}

	public void setUpdated_at(String updated_at) {
		this.updated_at = updated_at;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

}
