package com.cloud.cloudapi.service.pool.resource;

import java.util.LinkedHashMap;
import java.util.Map;

public class NeutronLBaaSLoadBalancer extends BaseResource {
	private String type = "OS::Neutron::LBaaS::LoadBalancer";
	private String resourceName = null;
	private String vip_subnet;

	public NeutronLBaaSLoadBalancer(String name, String vip_subnet) {
		this.resourceName = "loadbalancer_" + name;
		this.vip_subnet = vip_subnet;
	}

	@Override
	public String getResourceName() {
		return this.resourceName;
	}

	@Override
	public Map<String, Object> getResourceMap() {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		Map<String, String> r2 = new LinkedHashMap<String, String>();
		r2.put("get_resource", this.vip_subnet);
		properties.put("vip_subnet", r2);

		Map<String, Object> net = new LinkedHashMap<String, Object>();
		net.put("type", this.type);
		net.put("properties", properties);
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put(this.resourceName, net);
		return res;
	}

}
