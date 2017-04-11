package com.cloud.cloudapi.service.pool.resource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NeutronVPNService extends BaseResource {
	private String type = "OS::Neutron::VPNService";
	private String resourceName = null;
	private String router;
	private String subnet;
	private List<String> routerInterfaces;

	public NeutronVPNService(int index, String router, String subnet, List<String> routerInterfaces) {
		this.resourceName = "vpn_service_" + index;
		this.router = router;
		this.subnet = subnet;
		this.routerInterfaces = routerInterfaces;
	}
	
	public NeutronVPNService(String name, String router, String subnet, List<String> routerInterfaces) {
		this.resourceName = name + "_vpn_service";
		this.router = router;
		this.subnet = subnet;
		this.routerInterfaces = routerInterfaces;
	}


	@Override
	public String getResourceName() {
		return this.resourceName;
	}

	@Override
	public Map<String, Object> getResourceMap() {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("name", this.resourceName);
		Map<String, String> r1 = new LinkedHashMap<String, String>();
		r1.put("get_resource", this.router);
		properties.put("router_id", r1);
		Map<String, String> r2 = new LinkedHashMap<String, String>();
		r2.put("get_resource", this.subnet);
		properties.put("subnet_id", r2);

		Map<String, Object> net = new LinkedHashMap<String, Object>();
		net.put("type", this.type);
		net.put("properties", properties);
		net.put("depends_on", this.routerInterfaces);
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put(this.resourceName, net);
		return res;
	}

}
