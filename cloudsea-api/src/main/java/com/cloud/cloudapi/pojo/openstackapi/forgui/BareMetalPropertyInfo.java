package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class BareMetalPropertyInfo {

	private Integer cpus;
	private Integer memory_mb;
	private Integer local_gb;
	private String cpu_arch;

	public Integer getCpus() {
		return cpus;
	}

	public void setCpus(Integer cpus) {
		this.cpus = cpus;
	}

	public Integer getMemory_mb() {
		return memory_mb;
	}

	public void setMemory_mb(Integer memory_mb) {
		this.memory_mb = memory_mb;
	}

	public Integer getLocal_gb() {
		return local_gb;
	}

	public void setLocal_gb(Integer local_gb) {
		this.local_gb = local_gb;
	}

	public String getCpu_arch() {
		return cpu_arch;
	}

	public void setCpu_arch(String cpu_arch) {
		this.cpu_arch = cpu_arch;
	}

}
