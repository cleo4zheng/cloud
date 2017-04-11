package com.cloud.cloudapi.service.pool.resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NeutronFirewallPolicy extends BaseResource {
	private String type = "OS::Neutron::FirewallPolicy";
	private String resourceName = null;
	private List<String> ruleList;
	public NeutronFirewallPolicy(int index, List<String> ruleList) {
		this.resourceName = "firewallpolicy_" + index;
		this.ruleList = ruleList;
	}

	@Override
	public String getResourceName() {
		return this.resourceName;
	}

	@Override
	public Map<String, Object> getResourceMap() {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("name", this.resourceName);
		
		List<Map<String, String>> rules = new ArrayList<Map<String, String>>();
		for (String rule : this.ruleList) {
			Map<String, String> r2 = new LinkedHashMap<String, String>();
			r2.put("get_resource", rule);
			rules.add(r2);
		}
		properties.put("firewall_rules", rules);

		Map<String, Object> net = new LinkedHashMap<String, Object>();
		net.put("type", this.type);
		net.put("properties", properties);
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put(this.resourceName, net);
		return res;
	}

}
