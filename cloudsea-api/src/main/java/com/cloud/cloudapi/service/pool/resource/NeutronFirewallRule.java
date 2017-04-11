package com.cloud.cloudapi.service.pool.resource;

import java.util.LinkedHashMap;
import java.util.Map;

public class NeutronFirewallRule extends BaseResource {
	private String type = "OS::Neutron::FirewallRule";
	private String resourceName = null;
	private String protocol;
	private String ipversion = "4";
	private String sourceIp;
	private String sourcePort;
	private String destIp;
	private String destPort;
	private String action;
	private boolean enable = true;

	public NeutronFirewallRule(int index, String protocol, String ipversion, String sourceIp, String sourcePort,
			String destIp, String destPort, String action, boolean enable) {
		this.resourceName = "firewall_rule_" + index;
		this.protocol = protocol;
		this.ipversion = ipversion;
		this.sourceIp = sourceIp;
		this.sourcePort = sourcePort;
		this.destIp = destIp;
		this.destPort = destPort;
		this.action = action;
		this.enable = enable;
	}

	@Override
	public String getResourceName() {
		return this.resourceName;
	}

	@Override
	public Map<String, Object> getResourceMap() {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("name", this.resourceName);
		properties.put("protocol", this.protocol);
		properties.put("ip_version", this.ipversion);
		if (sourceIp != null && !"".equals(sourceIp)) {
			properties.put("source_ip_address", this.sourceIp);
		}
		if (sourcePort != null && !"".equals(sourcePort)) {
			properties.put("source_port", this.sourcePort);
		}
		if (destIp != null && !"".equals(destIp)) {
			properties.put("destination_ip_address", this.destIp);
		}
		if (destPort != null && !"".equals(destPort)) {
			properties.put("destination_port", this.destPort);
		}
		properties.put("action", this.action);
		properties.put("enabled", this.enable);
		Map<String, Object> net = new LinkedHashMap<String, Object>();
		net.put("type", this.type);
		net.put("properties", properties);
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put(this.resourceName, net);
		return res;
	}

	public static void main(String[] args) {
	}

}
