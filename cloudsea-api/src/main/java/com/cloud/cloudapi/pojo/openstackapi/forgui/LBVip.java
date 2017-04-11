package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class LBVip {

	private String id;
	private String status;
	private String protocol;
	private String description;
	private String address;
	private Integer protocol_port;
	private String port_id;
	private String status_description;
	private String name;
	private Boolean admin_state_up;
	private String subnet_id;
	private String tenant_id;
	private Integer connection_limit;
	private String pool_id;
	private String session_persistence;

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

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Integer getProtocol_port() {
		return protocol_port;
	}

	public void setProtocol_port(Integer protocol_port) {
		this.protocol_port = protocol_port;
	}

	public String getPort_id() {
		return port_id;
	}

	public void setPort_id(String port_id) {
		this.port_id = port_id;
	}

	public String getStatus_description() {
		return status_description;
	}

	public void setStatus_description(String status_description) {
		this.status_description = status_description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getAdmin_state_up() {
		return admin_state_up;
	}

	public void setAdmin_state_up(Boolean admin_state_up) {
		this.admin_state_up = admin_state_up;
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

	public Integer getConnection_limit() {
		return connection_limit;
	}

	public void setConnection_limit(Integer connection_limit) {
		this.connection_limit = connection_limit;
	}

	public String getPool_id() {
		return pool_id;
	}

	public void setPool_id(String pool_id) {
		this.pool_id = pool_id;
	}

	public String getSession_persistence() {
		return session_persistence;
	}

	public void setSession_persistence(String session_persistence) {
		this.session_persistence = session_persistence;
	}

}
