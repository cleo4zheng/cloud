package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class ComputeQuota {

	private Integer cores;
	private Integer fixed_ips;
	private Integer floating_ips;
	private Integer injected_file_content_bytes;
	private Integer injected_file_path_bytes;
	private Integer injected_files;
	private Integer instances;
	private Integer key_pairs;
	private Integer metadata_items;
	private Integer ram;
	private Integer security_group_rules;
	private Integer security_groups;
	private Integer server_group_members;

	public Integer getCores() {
		return cores;
	}

	public void setCores(Integer cores) {
		this.cores = cores;
	}

	public Integer getFixed_ips() {
		return fixed_ips;
	}

	public void setFixed_ips(Integer fixed_ips) {
		this.fixed_ips = fixed_ips;
	}

	public Integer getFloating_ips() {
		return floating_ips;
	}

	public void setFloating_ips(Integer floating_ips) {
		this.floating_ips = floating_ips;
	}

	public Integer getInjected_file_content_bytes() {
		return injected_file_content_bytes;
	}

	public void setInjected_file_content_bytes(Integer injected_file_content_bytes) {
		this.injected_file_content_bytes = injected_file_content_bytes;
	}

	public Integer getInjected_file_path_bytes() {
		return injected_file_path_bytes;
	}

	public void setInjected_file_path_bytes(Integer injected_file_path_bytes) {
		this.injected_file_path_bytes = injected_file_path_bytes;
	}

	public Integer getInjected_files() {
		return injected_files;
	}

	public void setInjected_files(Integer injected_files) {
		this.injected_files = injected_files;
	}

	public Integer getInstances() {
		return instances;
	}

	public void setInstances(Integer instances) {
		this.instances = instances;
	}

	public Integer getKey_pairs() {
		return key_pairs;
	}

	public void setKey_pairs(Integer key_pairs) {
		this.key_pairs = key_pairs;
	}

	public Integer getMetadata_items() {
		return metadata_items;
	}

	public void setMetadata_items(Integer metadata_items) {
		this.metadata_items = metadata_items;
	}

	public Integer getRam() {
		return ram;
	}

	public void setRam(Integer ram) {
		this.ram = ram;
	}

	public Integer getSecurity_group_rules() {
		return security_group_rules;
	}

	public void setSecurity_group_rules(Integer security_group_rules) {
		this.security_group_rules = security_group_rules;
	}

	public Integer getSecurity_groups() {
		return security_groups;
	}

	public void setSecurity_groups(Integer security_groups) {
		this.security_groups = security_groups;
	}

	public Integer getServer_group_members() {
		return server_group_members;
	}

	public void setServer_group_members(Integer server_group_members) {
		this.server_group_members = server_group_members;
	}

}
