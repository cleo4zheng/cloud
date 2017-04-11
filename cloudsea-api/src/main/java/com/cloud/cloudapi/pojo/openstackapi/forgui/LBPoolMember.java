package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class LBPoolMember {

	private String id;
	private String address;
	private Boolean admin_state_up;
	private Integer protocol_port;
	private String subnet_id;
	private String tenant_id;
	private Integer weight;
    private String pool_id;
    private String status;
    private String status_description;
    private String instanceId;
    
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

	public Boolean getAdmin_state_up() {
		return admin_state_up;
	}

	public void setAdmin_state_up(Boolean admin_state_up) {
		this.admin_state_up = admin_state_up;
	}

	public Integer getProtocol_port() {
		return protocol_port;
	}

	public void setProtocol_port(Integer protocol_port) {
		this.protocol_port = protocol_port;
	}

	public String getSubnet_id() {
		return subnet_id;
	}

	public void setSubnet_id(String subnet_id) {
		this.subnet_id = subnet_id;
	}

	public String getTenant_id() {
		return tenant_id;
	}

	public void setTenant_id(String tenant_id) {
		this.tenant_id = tenant_id;
	}

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	public String getPool_id() {
		return pool_id;
	}

	public void setPool_id(String pool_id) {
		this.pool_id = pool_id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStatus_description() {
		return status_description;
	}

	public void setStatus_description(String status_description) {
		this.status_description = status_description;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

}
