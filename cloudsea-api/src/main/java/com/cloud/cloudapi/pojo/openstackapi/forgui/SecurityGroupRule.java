package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class SecurityGroupRule {
	private String id;
	private String name;
	private String direction;
	private String ethertype;
	private Integer port_range_max;
	private Integer port_range_min;
	private String protocol;
	private String remote_group_id;
	private String remoteIpPrefix;
	private String security_group_id;
	private String tenantId;
    private String createdAt;
    private String cidr;
    
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

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public String getEthertype() {
		return ethertype;
	}

	public void setEthertype(String ethertype) {
		this.ethertype = ethertype;
	}
	
	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getRemoteIpPrefix() {
		return remoteIpPrefix;
	}

	public void setRemoteIpPrefix(String remoteIpPrefix) {
		this.remoteIpPrefix = remoteIpPrefix;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public Integer getPort_range_max() {
		return port_range_max;
	}

	public void setPort_range_max(Integer port_range_max) {
		this.port_range_max = port_range_max;
	}

	public Integer getPort_range_min() {
		return port_range_min;
	}

	public void setPort_range_min(Integer port_range_min) {
		this.port_range_min = port_range_min;
	}

	public String getRemote_group_id() {
		return remote_group_id;
	}

	public void setRemote_group_id(String remote_group_id) {
		this.remote_group_id = remote_group_id;
	}

	public String getSecurity_group_id() {
		return security_group_id;
	}

	public void setSecurity_group_id(String security_group_id) {
		this.security_group_id = security_group_id;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getCidr() {
		return cidr;
	}

	public void setCidr(String cidr) {
		this.cidr = cidr;
	}

}
