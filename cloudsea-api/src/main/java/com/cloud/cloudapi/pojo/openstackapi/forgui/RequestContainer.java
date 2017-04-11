package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class RequestContainer {
	
	public Integer getMaster_count() {
		return master_count;
	}
	public void setMaster_count(Integer master_count) {
		this.master_count = master_count;
	}
	public String getBaymodel_id() {
		return baymodel_id;
	}
	public void setBaymodel_id(String baymodel_id) {
		this.baymodel_id = baymodel_id;
	}
	public Integer getNode_count() {
		return node_count;
	}
	public void setNode_count(Integer node_count) {
		this.node_count = node_count;
	}
	public Integer getBay_create_timeout() {
		return bay_create_timeout;
	}
	public void setBay_create_timeout(Integer bay_create_timeout) {
		this.bay_create_timeout = bay_create_timeout;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDiscovery_url() {
		return discovery_url;
	}
	public void setDiscovery_url(String discovery_url) {
		this.discovery_url = discovery_url;
	}
	private Integer master_count;
	private String baymodel_id;
	private Integer node_count;
	private Integer bay_create_timeout;
	private String name;
    private String discovery_url;

}
