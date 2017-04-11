package com.cloud.cloudapi.service.pool.resource;

import java.util.LinkedHashMap;
import java.util.Map;

public class NeutronLBaaSHealthMonitor extends BaseResource {
	private String type = "OS::Neutron::LBaaS::HealthMonitor";
	private String resourceName = null;
	private int delay;
	private String protocol_type;
	private int timeout;
	private int max_retries;
	private String pool;

	public NeutronLBaaSHealthMonitor(String name, String type, String pool, int delay, int timeout, int max_retries) {
		this.resourceName = "lbhealthmonitor_" + name;
		this.pool = pool;
		this.protocol_type = type;
		this.delay = delay;
		this.timeout = timeout;
		this.max_retries = max_retries;
	}

	public NeutronLBaaSHealthMonitor(String name, String pool) {
		this.resourceName = "lbhealthmonitor_" + name;
		this.pool = pool;
		this.protocol_type = "HTTP";
		this.delay = 3;
		this.timeout = 3;
		this.max_retries = 3;
	}

	@Override
	public String getResourceName() {
		return this.resourceName;
	}

	@Override
	public Map<String, Object> getResourceMap() {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		Map<String, String> r2 = new LinkedHashMap<String, String>();
		r2.put("get_resource", this.pool);
		properties.put("pool", r2);
		properties.put("delay", delay);
		properties.put("type", protocol_type);
		properties.put("timeout", timeout);
		properties.put("max_retries", max_retries);

		Map<String, Object> net = new LinkedHashMap<String, Object>();
		net.put("type", this.type);
		net.put("properties", properties);
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put(this.resourceName, net);
		return res;
	}

}
