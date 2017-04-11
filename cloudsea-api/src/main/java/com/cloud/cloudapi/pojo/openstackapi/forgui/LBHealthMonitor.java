package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;

public class LBHealthMonitor {

	private String id;
	private Boolean admin_state_up;
	private String pool_id;
	private String tenant_id;
	private Integer delay;
	private String expected_codes;
	private Integer max_retries;
	private Integer timeout;
	private String http_method;
	private List<LBPool> pools;
	private String url_path;
	private String type;
	private String monitor_id;
	private String status;
	private String status_description;
    private String name;
    
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

	public String getMonitor_id() {
		return monitor_id;
	}

	public void setMonitor_id(String monitor_id) {
		this.monitor_id = monitor_id;
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

	public Boolean getAdmin_state_up() {
		return admin_state_up;
	}

	public void setAdmin_state_up(Boolean admin_state_up) {
		this.admin_state_up = admin_state_up;
	}

	public String getTenant_id() {
		return tenant_id;
	}

	public void setTenant_id(String tenant_id) {
		this.tenant_id = tenant_id;
	}

	public Integer getDelay() {
		return delay;
	}

	public void setDelay(Integer delay) {
		this.delay = delay;
	}

	public Integer getMax_retries() {
		return max_retries;
	}

	public void setMax_retries(Integer max_retries) {
		this.max_retries = max_retries;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	public String getExpected_codes() {
		return expected_codes;
	}

	public void setExpected_codes(String expected_codes) {
		this.expected_codes = expected_codes;
	}

	public String getHttp_method() {
		return http_method;
	}

	public void setHttp_method(String http_method) {
		this.http_method = http_method;
	}

	public List<LBPool> getPools() {
		return pools;
	}

	public void setPools(List<LBPool> pools) {
		this.pools = pools;
	}

	public String getUrl_path() {
		return url_path;
	}

	public void setUrl_path(String url_path) {
		this.url_path = url_path;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPool_id() {
		return pool_id;
	}

	public void setPool_id(String pool_id) {
		this.pool_id = pool_id;
	}

}
