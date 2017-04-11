package com.cloud.cloudapi.service.pool.resource;

import java.util.LinkedHashMap;
import java.util.Map;

public class NeutronLBaaSListener extends BaseResource {
	private String type = "OS::Neutron::LBaaS::Listener";
	private String resourceName = null;
	private String loadbalancer;
	private String protocol;
	private int protocol_port;

	public NeutronLBaaSListener(String name, String loadbalancer, String protocol, int protocol_port) {
		this.resourceName = "lblistener_" + name;
		this.loadbalancer = loadbalancer;
		this.protocol = protocol;
		this.protocol_port = protocol_port;
	}

	public NeutronLBaaSListener(String name, String loadbalancer, int protocol_port) {
		this.resourceName = "listener_" + name;
		this.loadbalancer = loadbalancer;
		this.protocol = "HTTP";
		this.protocol_port = protocol_port;
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
		properties.put("protocol_port", this.protocol_port);
		Map<String, String> r2 = new LinkedHashMap<String, String>();
		r2.put("get_resource", this.loadbalancer);
		properties.put("loadbalancer", r2);

		Map<String, Object> net = new LinkedHashMap<String, Object>();
		net.put("type", this.type);
		net.put("properties", properties);
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put(this.resourceName, net);
		return res;
	}

}
