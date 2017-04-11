package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class BareMetalPort {

	private String node_uuid;
	private String uuid;
	private String id;
	private String address;
	private String created_at;
	private String updated_at;
	
	public String getNode_uuid() {
		return node_uuid;
	}

	public void setNode_uuid(String node_uuid) {
		this.node_uuid = node_uuid;
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

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

}
