package com.cloud.cloudapi.service.pool.resource;

import java.util.LinkedHashMap;
import java.util.Map;

public class NeutronLBaaSPoolMember extends BaseResource {
	private String type = "OS::Neutron::LBaaS::PoolMember";
	private String resourceName = null;
	private String pool;
	private String server;
	private int protocol_port;
	private String subnet;

	public NeutronLBaaSPoolMember(String name, int index, String pool, String server, int protocol_port,
			String subnet) {
		this.resourceName = "lbpoolmember_" + name + index;
		this.pool = pool;
		this.server = server;
		this.protocol_port = protocol_port;
		this.subnet = subnet;
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

		properties.put("protocol_port", this.protocol_port);
		Map<String, String> r3 = new LinkedHashMap<String, String>();
		r3.put("get_resource", this.subnet);
		properties.put("subnet", r3);

		Map<String, String[]> r1 = new LinkedHashMap<String, String[]>();
		r1.put("get_attr", new String[] { this.server, "first_address" });
		properties.put("address", r1);

		Map<String, Object> net = new LinkedHashMap<String, Object>();
		net.put("type", this.type);
		net.put("properties", properties);
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put(this.resourceName, net);
		return res;
	}

}
