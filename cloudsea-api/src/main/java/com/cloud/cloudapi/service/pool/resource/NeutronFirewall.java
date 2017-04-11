package com.cloud.cloudapi.service.pool.resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NeutronFirewall extends BaseResource {

	private String type = "OS::Neutron::Firewall";
	private String resourceName = null;
	private String firewallPolicy;
	private String[] routers;

	public NeutronFirewall(int index, String firewallPolicy, String[] routers) {
		this.resourceName = "firewall_" + index;
		this.firewallPolicy = firewallPolicy;
		this.routers = routers;
	}

	@Override
	public String getResourceName() {
		// TODO Auto-generated method stub
		return this.resourceName;
	}

	@Override
	public Map<String, Object> getResourceMap() {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("name", this.resourceName);
		Map<String, String> r2 = new LinkedHashMap<String, String>();
		r2.put("get_resource", this.firewallPolicy);
		properties.put("firewall_policy_id", r2);
		
		List<Map<String, String>> rl = new ArrayList<Map<String, String>>();
		for (String router : this.routers) {
			Map<String, String> rr = new LinkedHashMap<String, String>();
			rr.put("get_resource", router);
			rl.add(rr);
		}
		Map<String, Object> vs = new LinkedHashMap<String, Object>();
		vs.put("router_ids", rl);
		properties.put("value_specs", vs);
		
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
