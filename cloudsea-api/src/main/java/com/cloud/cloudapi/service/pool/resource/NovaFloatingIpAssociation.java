package com.cloud.cloudapi.service.pool.resource;

import java.util.LinkedHashMap;
import java.util.Map;

public class NovaFloatingIpAssociation extends BaseResource {
	private String server_res_name;
	private String fip_res_name;
	private String resourceName;
	private final String type = "OS::Nova::FloatingIPAssociation";

	public NovaFloatingIpAssociation(String server_name, String fip_name) {
		this.server_res_name = server_name;
		this.fip_res_name = fip_name;
		this.resourceName = server_name + "_fip";
	}

	public String getServerName(){
		return this.server_res_name;
	}
	
	public String getFipName(){
		return this.fip_res_name;
	}
	
	
	@Override
	public Map<String, Object> getResourceMap() {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		Map<String, String> r1 = new LinkedHashMap<String, String>();
		r1.put("get_resource", this.server_res_name);
		properties.put("server_id", r1);
		Map<String, String> r2 = new LinkedHashMap<String, String>();
		r2.put("get_resource", this.fip_res_name);
		properties.put("floating_ip", r2);

		Map<String, Object> fip_ass = new LinkedHashMap<String, Object>();
		fip_ass.put("type", this.type);
		fip_ass.put("properties", properties);
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put(this.resourceName, fip_ass);
		return res;
	}

	@Override
	public String getResourceName() {
		return this.resourceName;
	}

	public static void main(String[] args) {
		String a = "pool_instance_002";
		String b = a.substring(a.length() - 3, a.length());
		System.out.println(b);
		int bb = Integer.parseInt(b);
		System.out.println(bb);
	}

}
