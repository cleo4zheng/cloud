package com.cloud.cloudapi.service.pool.resource;

import java.util.LinkedHashMap;
import java.util.Map;

public class NeutronLBaasPool extends BaseResource {
	private String type = "OS::Neutron::LBaaS::Pool";
	private String resourceName = null;
	private String algorithm;
	private String protocol;
	private String listener;

	public NeutronLBaasPool(String name, String protocol, String algorithm, String listener) {
		this.resourceName = "lbpool_" + name;
		this.protocol = protocol;
		this.algorithm = algorithm;
		this.listener = listener;
	}

	public NeutronLBaasPool(String name, String listener) {
		this.resourceName = "lbpool_" + name;
		this.protocol = "HTTP";
		this.algorithm = "ROUND_ROBIN";
		this.listener = listener;
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
		properties.put("lb_algorithm", this.algorithm);
		Map<String, String> r2 = new LinkedHashMap<String, String>();
		r2.put("get_resource", this.listener);
		properties.put("listener", r2);

		Map<String, Object> net = new LinkedHashMap<String, Object>();
		net.put("type", this.type);
		net.put("properties", properties);
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put(this.resourceName, net);
		return res;
	}

}
