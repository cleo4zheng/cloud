package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;

public class Listener {

	private String id;
	private String name;
	private String description;
	private Boolean admin_state_up;
	private Integer connection_limit;
	private String default_pool_id;
	private String loadbalancer_id;
	private String protocol;
	private Integer protocol_port;
	private String tenant_id;
	private String default_tls_container_ref;
	private String sni_container_refs;
	private List<Loadbalancer> loadbalancers;

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getAdmin_state_up() {
		return admin_state_up;
	}

	public void setAdmin_state_up(Boolean admin_state_up) {
		this.admin_state_up = admin_state_up;
	}

	public Integer getConnection_limit() {
		return connection_limit;
	}

	public void setConnection_limit(Integer connection_limit) {
		this.connection_limit = connection_limit;
	}

	public String getDefault_pool_id() {
		return default_pool_id;
	}

	public void setDefault_pool_id(String default_pool_id) {
		this.default_pool_id = default_pool_id;
	}

	public String getLoadbalancer_id() {
		return loadbalancer_id;
	}

	public void setLoadbalancer_id(String loadbalancer_id) {
		this.loadbalancer_id = loadbalancer_id;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public Integer getProtocol_port() {
		return protocol_port;
	}

	public void setProtocol_port(Integer protocol_port) {
		this.protocol_port = protocol_port;
	}

	public String getTenant_id() {
		return tenant_id;
	}

	public void setTenant_id(String tenant_id) {
		this.tenant_id = tenant_id;
	}

	public String getDefault_tls_container_ref() {
		return default_tls_container_ref;
	}

	public void setDefault_tls_container_ref(String default_tls_container_ref) {
		this.default_tls_container_ref = default_tls_container_ref;
	}

	public String getSni_container_refs() {
		return sni_container_refs;
	}

	public void setSni_container_refs(String sni_container_refs) {
		this.sni_container_refs = sni_container_refs;
	}

	public List<Loadbalancer> getLoadbalancers() {
		return loadbalancers;
	}

	public void setLoadbalancers(List<Loadbalancer> loadbalancers) {
		this.loadbalancers = loadbalancers;
	}

}
