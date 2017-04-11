package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class DriverInfo {

	private String id;
	private String deploy_kernel;
	private String ipmi_address;
	private String deploy_ramdisk;
	private String ipmi_password;
	private String ipmi_username;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDeploy_kernel() {
		return deploy_kernel;
	}

	public void setDeploy_kernel(String deploy_kernel) {
		this.deploy_kernel = deploy_kernel;
	}

	public String getIpmi_address() {
		return ipmi_address;
	}

	public void setIpmi_address(String ipmi_address) {
		this.ipmi_address = ipmi_address;
	}

	public String getDeploy_ramdisk() {
		return deploy_ramdisk;
	}

	public void setDeploy_ramdisk(String deploy_ramdisk) {
		this.deploy_ramdisk = deploy_ramdisk;
	}

	public String getIpmi_password() {
		return ipmi_password;
	}

	public void setIpmi_password(String ipmi_password) {
		this.ipmi_password = ipmi_password;
	}

	public String getIpmi_username() {
		return ipmi_username;
	}

	public void setIpmi_username(String ipmi_username) {
		this.ipmi_username = ipmi_username;
	}

}
