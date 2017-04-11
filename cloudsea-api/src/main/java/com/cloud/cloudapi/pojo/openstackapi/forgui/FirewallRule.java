package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class FirewallRule {

	private String id;
	private String name;
	private String tenant_id;
	private String action;
	private String description;
	private String destination_ip_address;
	private String source_ip_address;
	private String destination_port;
	private String source_port;
	private Boolean enabled;
	private Boolean shared;
	private String firewall_policy_id;
	private String ip_version;
	private String position;
	private String protocol;

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

	public String getTenant_id() {
		return tenant_id;
	}

	public void setTenant_id(String tenant_id) {
		this.tenant_id = tenant_id;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDestination_ip_address() {
		return destination_ip_address;
	}

	public void setDestination_ip_address(String destination_ip_address) {
		this.destination_ip_address = destination_ip_address;
	}

	public String getSource_ip_address() {
		return source_ip_address;
	}

	public void setSource_ip_address(String source_ip_address) {
		this.source_ip_address = source_ip_address;
	}

	public String getDestination_port() {
		return destination_port;
	}

	public void setDestination_port(String destination_port) {
		this.destination_port = destination_port;
	}

	public String getSource_port() {
		return source_port;
	}

	public void setSource_port(String source_port) {
		this.source_port = source_port;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Boolean getShared() {
		return shared;
	}

	public void setShared(Boolean shared) {
		this.shared = shared;
	}

	public String getFirewall_policy_id() {
		return firewall_policy_id;
	}

	public void setFirewall_policy_id(String firewall_policy_id) {
		this.firewall_policy_id = firewall_policy_id;
	}

	public String getIp_version() {
		return ip_version;
	}

	public void setIp_version(String ip_version) {
		this.ip_version = ip_version;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void normalInfo(){
		this.setDescription(null);
		this.setEnabled(null);
		this.setFirewall_policy_id(null);
		this.setIp_version(null);
		this.setPosition(null);
		this.setShared(null);
		this.setTenant_id(null);
	}
}
