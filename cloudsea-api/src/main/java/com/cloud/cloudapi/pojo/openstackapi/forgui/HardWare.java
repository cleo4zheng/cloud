package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class HardWare {
	
	private String memory_mb;
	public String getMemory_mb() {
		return memory_mb;
	}
	public void setMemory_mb(String memory_mb) {
		this.memory_mb = memory_mb;
	}
	public String getCpu_arch() {
		return cpu_arch;
	}
	public void setCpu_arch(String cpu_arch) {
		this.cpu_arch = cpu_arch;
	}
	public String getLocal_gb() {
		return local_gb;
	}
	public void setLocal_gb(String local_gb) {
		this.local_gb = local_gb;
	}
	public String getCpus() {
		return cpus;
	}
	public void setCpus(String cpus) {
		this.cpus = cpus;
	}
	private String cpu_arch;
	private String local_gb;
	private String cpus;
	

}
