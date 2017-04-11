package com.cloud.cloudapi.service.pool.resource;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.service.pool.BasicResource;

public class NeutronFloatingIP extends BaseResource {
	private int no;
	private final String type = "OS::Neutron::FloatingIP";
	private String pool = null;
	private String resoureName = null;
	private List<String> routerInterfaces;

	public NeutronFloatingIP(int no, String pool,String extnet, List<String> routerInterfaces) {
		this.no = no;
		this.pool = extnet;
		this.resoureName = String.format("fip_" + pool + "_%03d", this.no);
		this.routerInterfaces = routerInterfaces;
	}
	
	public NeutronFloatingIP(int no, String instanceName, String pool,  String extnet, List<String> routerInterfaces) {
		this.no = no;
		this.pool = extnet;
		this.resoureName = "fip_" + pool + "_" + instanceName;
		//this.resoureName = String.format("fip_" + pool + "_%03d", this.no);
		this.routerInterfaces = routerInterfaces;
	}

	public NeutronFloatingIP(String sName, String pool, List<String> routerInterfaces) {
		this.no = Integer.parseInt(sName.substring(sName.length() - 3, sName.length()));
		this.pool = pool;
		this.resoureName = String.format("fip-%03d", this.no);
		this.routerInterfaces = routerInterfaces;
	}

	@Override
	public Map<String, Object> getResourceMap() {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("floating_network", this.pool);
		Map<String, Object> f_ip = new LinkedHashMap<String, Object>();
		f_ip.put("type", this.type);
		f_ip.put("properties", properties);
		
		//fix delete failed bug
		f_ip.put("depends_on", this.routerInterfaces);
		
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put(this.resoureName, f_ip);
		return res;
	}

	@Override
	public String getResourceName() {
		return this.resoureName;
	}

	public static void main(String[] args) {
		String[] ris = { "net1_router", "net2_router" };
		NeutronFloatingIP fip = new NeutronFloatingIP(1, "public", "123123123",Arrays.asList(ris));
		Map<String, Object> map = fip.getResourceMap();
		String yaml = BasicResource.convertMAP2YAML(map);
		System.out.println(yaml);
	}

}
