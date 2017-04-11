package com.cloud.cloudapi.service.pool.resource;

import java.util.LinkedHashMap;
import java.util.Map;

public class NeutronSubnet extends BaseResource {
	private String type = "OS::Neutron::Subnet";
	private String resourceName = null;
	private String cidr = null;
	private String gateway = null;
	private Boolean enableDhcp = null;
	private String netName = null;
	private int ipVersion = 4;
	
	public NeutronSubnet(String netName, String cidr, String gateway, Boolean enableDhcp, int ipVersion) {
		this.netName = netName;
		this.resourceName = netName + "_subnet";
		this.cidr = cidr;
		this.gateway = gateway;
		this.enableDhcp = enableDhcp;
		this.ipVersion = ipVersion;
	}

	@Override
	public String getResourceName() {
		return this.resourceName;
	}

	@Override
	public Map<String, Object> getResourceMap() {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		Map<String, Object> r1 = new LinkedHashMap<String, Object>();
		r1.put("get_resource", this.netName);
		properties.put("name", this.resourceName);
		properties.put("network_id", r1);
		properties.put("cidr", this.cidr);
		properties.put("gateway_ip", this.gateway);
		properties.put("enable_dhcp", this.enableDhcp);
		properties.put("ip_version", this.ipVersion);

		Map<String, Object> subnet = new LinkedHashMap<String, Object>();
		subnet.put("type", this.type);
		subnet.put("properties", properties);
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put(this.resourceName, subnet);
		return res;
	}

	public static void main(String[] args) {
		NeutronSubnet sub = new NeutronSubnet("xxnet", "192.168.1.0/24", "192.168.1.1", true, 4);
		System.out.println(sub.getResourceMap());
	}

}
