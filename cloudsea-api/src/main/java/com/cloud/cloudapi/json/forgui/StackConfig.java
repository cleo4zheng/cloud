package com.cloud.cloudapi.json.forgui;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Datastore;

public class StackConfig {
	private List<String> az;
	private List<String> ipVersion;
	private Map<String, Object> core;
	private Map<String, Object> ram;
	private Map<String, Object> volume;
	private List<Map<String, String>> instanceType;
	private List<Map<String, String>> floatingIpType;
	private List<Datastore> datastore;

	public StackConfig() {
	//	String[] iv = { "IPv4", "IPv6" };
		String[] iv = { "IPv4" };
		this.ipVersion = Arrays.asList(iv);
	}

	public List<String> getAz() {
		return az;
	}

	public void setAz(List<String> az) {
		this.az = az;
	}

	public List<String> getIpVersion() {
		return ipVersion;
	}

	public void setIpVersion(List<String> ipVersion) {
		this.ipVersion = ipVersion;
	}

	public Map<String, Object> getCore() {
		return core;
	}

	public void setCore(Map<String, Object> core) {
		this.core = core;
	}

	public Map<String, Object> getRam() {
		return ram;
	}

	public void setRam(Map<String, Object> ram) {
		this.ram = ram;
	}

	public Map<String, Object> getVolume() {
		return volume;
	}

	public void setVolume(Map<String, Object> volume) {
		this.volume = volume;
	}

	public List<Map<String, String>> getInstanceType() {
		return instanceType;
	}

	public void setInstanceType(List<Map<String, String>> instanceType) {
		this.instanceType = instanceType;
	}

	public List<Map<String, String>> getFloatingIpType() {
		return floatingIpType;
	}

	public void setFloatingIpType(List<Map<String, String>> floatingIpType) {
		this.floatingIpType = floatingIpType;
	}

	public List<Datastore> getDatastore() {
		return datastore;
	}

	public void setDatastore(List<Datastore> datastore) {
		this.datastore = datastore;
	}

}
