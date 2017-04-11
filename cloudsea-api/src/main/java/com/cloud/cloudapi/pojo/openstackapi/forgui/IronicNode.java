package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class IronicNode {
	
	private String driver;
	private String name;
	private Properties properties;
	private Driver_info driver_info;
	private Extra extra;
	
	public class  Extra{
		private String address;

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}
		
	}
	
	public class Properties{
		private String memory_mb;
		private String cpu_arch;
		private String local_gb;
		private String cpus;
		private String capabilities;
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
		public String getCapabilities() {
			return capabilities;
		}
		public void setCapabilities(String capabilities) {
			this.capabilities = capabilities;
		}
	}

	
	public class Driver_info{
		private String ipmi_password;
		private String ipmi_address;
		private String ipmi_username;
		private String ipmi_terminal_port;
		private String deploy_kernel;
		private String deploy_ramdisk;
		public String getIpmi_password() {
			return ipmi_password;
		}
		public void setIpmi_password(String ipmi_password) {
			this.ipmi_password = ipmi_password;
		}
		public String getIpmi_address() {
			return ipmi_address;
		}
		public void setIpmi_address(String ipmi_address) {
			this.ipmi_address = ipmi_address;
		}
		public String getIpmi_username() {
			return ipmi_username;
		}
		public void setIpmi_username(String ipmi_username) {
			this.ipmi_username = ipmi_username;
		}
		public String getIpmi_terminal_port() {
			return ipmi_terminal_port;
		}
		public void setIpmi_terminal_port(String ipmi_terminal_port) {
			this.ipmi_terminal_port = ipmi_terminal_port;
		}
		public String getDeploy_kernel() {
			return deploy_kernel;
		}
		public void setDeploy_kernel(String deploy_kernel) {
			this.deploy_kernel = deploy_kernel;
		}
		public String getDeploy_ramdisk() {
			return deploy_ramdisk;
		}
		public void setDeploy_ramdisk(String deploy_ramdisk) {
			this.deploy_ramdisk = deploy_ramdisk;
		}
	}


	public String getDriver() {
		return driver;
	}


	public void setDriver(String driver) {
		this.driver = driver;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public Properties getProperties() {
		return properties;
	}


	public void setProperties(Properties properties) {
		this.properties = properties;
	}


	public Driver_info getDriver_info() {
		return driver_info;
	}


	public void setDriver_info(Driver_info driver_info) {
		this.driver_info = driver_info;
	}


	public Extra getExtra() {
		return extra;
	}


	public void setExtra(Extra extra) {
		this.extra = extra;
	}
}
