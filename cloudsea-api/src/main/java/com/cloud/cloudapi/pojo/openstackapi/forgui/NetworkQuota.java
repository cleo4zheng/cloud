package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class NetworkQuota {

	private Integer subnet;
	private Integer network;
	private Integer floatingip;
	private Integer subnetpool;
	private Integer security_group_rule;
	private Integer security_group;
	private Integer router;
	private Integer rbac_policy;
	private Integer port;

	public Integer getSubnet() {
		return subnet;
	}

	public void setSubnet(Integer subnet) {
		this.subnet = subnet;
	}

	public Integer getNetwork() {
		return network;
	}

	public void setNetwork(Integer network) {
		this.network = network;
	}

	public Integer getFloatingip() {
		return floatingip;
	}

	public void setFloatingip(Integer floatingip) {
		this.floatingip = floatingip;
	}

	public Integer getSubnetpool() {
		return subnetpool;
	}

	public void setSubnetpool(Integer subnetpool) {
		this.subnetpool = subnetpool;
	}

	public Integer getSecurity_group_rule() {
		return security_group_rule;
	}

	public void setSecurity_group_rule(Integer security_group_rule) {
		this.security_group_rule = security_group_rule;
	}

	public Integer getSecurity_group() {
		return security_group;
	}

	public void setSecurity_group(Integer security_group) {
		this.security_group = security_group;
	}

	public Integer getRouter() {
		return router;
	}

	public void setRouter(Integer router) {
		this.router = router;
	}

	public Integer getRbac_policy() {
		return rbac_policy;
	}

	public void setRbac_policy(Integer rbac_policy) {
		this.rbac_policy = rbac_policy;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

}
