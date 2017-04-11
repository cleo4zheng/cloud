package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;

public class LBPool {

	private String id;
	private String vip_id;
	private String name;
	private Boolean admin_state_up;
	private String subnet_id;
	private String tenant_id;
	private String status;
	private String lb_algorithm;
	private String protocol;
	private String description;
	private String health_monitor_id;
	private List<String> loadbalancers;
	private String loadbalancer_id;
	private List<String> listeners;
	private String listener_id;
	private List<String> members;
	private String members_id;
	private String status_description;
	private String provider;
	private List<LBHealthMonitor> health_monitors_status;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getVip_id() {
		return vip_id;
	}

	public void setVip_id(String vip_id) {
		this.vip_id = vip_id;
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getLb_algorithm() {
		return lb_algorithm;
	}

	public void setLb_algorithm(String lb_algorithm) {
		this.lb_algorithm = lb_algorithm;
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

	public String getHealth_monitor_id() {
		return health_monitor_id;
	}

	public void setHealth_monitor_id(String health_monitor_id) {
		this.health_monitor_id = health_monitor_id;
	}

	public List<String> getMembers() {
		return members;
	}

	public void setMembers(List<String> members) {
		this.members = members;
	}

	public String getMembers_id() {
		return members_id;
	}

	public void setMembers_id(String members_id) {
		this.members_id = members_id;
	}

	public String getStatus_description() {
		return status_description;
	}

	public void setStatus_description(String status_description) {
		this.status_description = status_description;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public List<LBHealthMonitor> getHealth_monitors_status() {
		return health_monitors_status;
	}

	public void setHealth_monitors_status(List<LBHealthMonitor> health_monitors_status) {
		this.health_monitors_status = health_monitors_status;
	}

	public List<String> getListeners() {
		return listeners;
	}

	public void setListeners(List<String> listeners) {
		this.listeners = listeners;
	}

	public String getListener_id() {
		return listener_id;
	}

	public void setListener_id(String listener_id) {
		this.listener_id = listener_id;
	}

	public List<String> getLoadbalancers() {
		return loadbalancers;
	}

	public void setLoadbalancers(List<String> loadbalancers) {
		this.loadbalancers = loadbalancers;
	}

	public String getLoadbalancer_id() {
		return loadbalancer_id;
	}

	public void setLoadbalancer_id(String loadbalancer_id) {
		this.loadbalancer_id = loadbalancer_id;
	}

}
