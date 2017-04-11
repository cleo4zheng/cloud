package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;

public class Container {

	private String uuid;
	private String status;
	private String stack_id;
	private Integer master_count;
	private String baymodel_id;
	private Integer node_count;
	private Integer bay_create_timeout;
	private String name;
    private String created_at;
    private String updated_at;
    private String api_address;
    private String discovery_url;
    private List<String> master_addresses;
    private List<String> node_addresses;
    private String status_reason;
    private String tenantId;
    private int core;
    private int ram;
    private int systemVolumeSize;
    private String systemVolumeType;
    private int dataVolumeSize;
    private String dataVolumeType;
    private String imageId;
    private Long millionSeconds; 
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStack_id() {
		return stack_id;
	}

	public void setStack_id(String stack_id) {
		this.stack_id = stack_id;
	}

	public String getBaymodel_id() {
		return baymodel_id;
	}

	public void setBaymodel_id(String baymodel_id) {
		this.baymodel_id = baymodel_id;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getMaster_count() {
		return master_count;
	}

	public void setMaster_count(Integer master_count) {
		this.master_count = master_count;
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

	public String getCreated_at() {
		return created_at;
	}

	public void setCreated_at(String created_at) {
		this.created_at = created_at;
	}

	public String getUpdated_at() {
		return updated_at;
	}

	public void setUpdated_at(String updated_at) {
		this.updated_at = updated_at;
	}

	public String getApi_address() {
		return api_address;
	}

	public void setApi_address(String api_address) {
		this.api_address = api_address;
	}

	public String getDiscovery_url() {
		return discovery_url;
	}

	public void setDiscovery_url(String discovery_url) {
		this.discovery_url = discovery_url;
	}

	public List<String> getMaster_addresses() {
		return master_addresses;
	}

	public void setMaster_addresses(List<String> master_addresses) {
		this.master_addresses = master_addresses;
	}

	public List<String> getNode_addresses() {
		return node_addresses;
	}

	public void setNode_addresses(List<String> node_addresses) {
		this.node_addresses = node_addresses;
	}

	public String getStatus_reason() {
		return status_reason;
	}

	public void setStatus_reason(String status_reason) {
		this.status_reason = status_reason;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public int getCore() {
		return core;
	}

	public void setCore(int core) {
		this.core = core;
	}

	public int getRam() {
		return ram;
	}

	public void setRam(int ram) {
		this.ram = ram;
	}

	public int getSystemVolumeSize() {
		return systemVolumeSize;
	}

	public void setSystemVolumeSize(int systemVolumeSize) {
		this.systemVolumeSize = systemVolumeSize;
	}

	public String getSystemVolumeType() {
		return systemVolumeType;
	}

	public void setSystemVolumeType(String systemVolumeType) {
		this.systemVolumeType = systemVolumeType;
	}

	public int getDataVolumeSize() {
		return dataVolumeSize;
	}

	public void setDataVolumeSize(int dataVolumeSize) {
		this.dataVolumeSize = dataVolumeSize;
	}

	public String getDataVolumeType() {
		return dataVolumeType;
	}

	public void setDataVolumeType(String dataVolumeType) {
		this.dataVolumeType = dataVolumeType;
	}

	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}
	
	
	
}
