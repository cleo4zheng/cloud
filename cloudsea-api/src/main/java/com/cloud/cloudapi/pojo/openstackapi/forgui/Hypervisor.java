package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class Hypervisor {
	private int id;
	private String status;
	private int vcpus_used;
	private String hypervisor_type;
	private int local_gb_used;
	private int vcpus;
	private String hypervisor_hostname;
	private int memory_mb_used;
	private int memory_mb;
	private int current_workload;
	private String state;
	private String host_ip;
	private String cpu_info;
	private int running_vms;
	private int free_disk_gb;
	private int hypervisor_version;
	private int disk_available_least;
	private int local_gb;
	private int free_ram_mb;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getVcpus_used() {
		return vcpus_used;
	}

	public void setVcpus_used(int vcpus_used) {
		this.vcpus_used = vcpus_used;
	}

	public String getHypervisor_type() {
		return hypervisor_type;
	}

	public void setHypervisor_type(String hypervisor_type) {
		this.hypervisor_type = hypervisor_type;
	}

	public int getLocal_gb_used() {
		return local_gb_used;
	}

	public void setLocal_gb_used(int local_gb_used) {
		this.local_gb_used = local_gb_used;
	}

	public int getVcpus() {
		return vcpus;
	}

	public void setVcpus(int vcpus) {
		this.vcpus = vcpus;
	}

	public String getHypervisor_hostname() {
		return hypervisor_hostname;
	}

	public void setHypervisor_hostname(String hypervisor_hostname) {
		this.hypervisor_hostname = hypervisor_hostname;
	}

	public int getMemory_mb_used() {
		return memory_mb_used;
	}

	public void setMemory_mb_used(int memory_mb_used) {
		this.memory_mb_used = memory_mb_used;
	}

	public int getMemory_mb() {
		return memory_mb;
	}

	public void setMemory_mb(int memory_mb) {
		this.memory_mb = memory_mb;
	}

	public int getCurrent_workload() {
		return current_workload;
	}

	public void setCurrent_workload(int current_workload) {
		this.current_workload = current_workload;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getHost_ip() {
		return host_ip;
	}

	public void setHost_ip(String host_ip) {
		this.host_ip = host_ip;
	}

	public String getCpu_info() {
		return cpu_info;
	}

	public void setCpu_info(String cpu_info) {
		this.cpu_info = cpu_info;
	}

	public int getRunning_vms() {
		return running_vms;
	}

	public void setRunning_vms(int running_vms) {
		this.running_vms = running_vms;
	}

	public int getFree_disk_gb() {
		return free_disk_gb;
	}

	public void setFree_disk_gb(int free_disk_gb) {
		this.free_disk_gb = free_disk_gb;
	}

	public int getHypervisor_version() {
		return hypervisor_version;
	}

	public void setHypervisor_version(int hypervisor_version) {
		this.hypervisor_version = hypervisor_version;
	}

	public int getDisk_available_least() {
		return disk_available_least;
	}

	public void setDisk_available_least(int disk_available_least) {
		this.disk_available_least = disk_available_least;
	}

	public int getLocal_gb() {
		return local_gb;
	}

	public void setLocal_gb(int local_gb) {
		this.local_gb = local_gb;
	}

	public int getFree_ram_mb() {
		return free_ram_mb;
	}

	public void setFree_ram_mb(int free_ram_mb) {
		this.free_ram_mb = free_ram_mb;
	}

}
