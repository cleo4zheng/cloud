package com.cloud.cloudapi.service.pool.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NeutronRouter extends BaseResource {
	private String type = "OS::Neutron::Router";
	private String resourceName = null;
	private String pubNet;
	private List<String> netNameList;

	public NeutronRouter(String name, List<String> netNameList, String pubNet) {
		this.resourceName = name;
		this.pubNet = pubNet;
		this.netNameList = netNameList;
	}

	@Override
	public String getResourceName() {
		return this.resourceName;
	}

	@Override
	public Map<String, Object> getResourceMap() {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();

		Map<String, Object> r1 = new LinkedHashMap<String, Object>();
		r1.put("network", this.pubNet);
		if (null != pubNet && !"".equals(pubNet)) {
			properties.put("external_gateway_info", r1);
		}
		properties.put("name", this.resourceName);
		Map<String, Object> router = new LinkedHashMap<String, Object>();
		router.put("type", this.type);
		router.put("properties", properties);
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put(this.resourceName, router);

		for(String netName : this.netNameList){
			String subName = netName + "_subnet";
			Map<String, Object> prop = new LinkedHashMap<String, Object>();
			Map<String, Object> r12 = new LinkedHashMap<String, Object>();
			r12.put("get_resource", this.resourceName);
			Map<String, Object> r13 = new LinkedHashMap<String, Object>();
			r13.put("get_resource", subName);
			prop.put("router", r12);
			prop.put("subnet", r13);
			Map<String, Object> rIf = new LinkedHashMap<String, Object>();
			rIf.put("type", this.type + "Interface");
			rIf.put("properties", prop);
			res.put(this.resourceName + "_" + netName + "_interface", rIf);
		}
		return res;
	}

	public List<String> getRouterInterfaces(){
		List<String> result = new ArrayList<String>();
		for (String netName : netNameList) {
			result.add(this.resourceName + "_" + netName + "_interface");
		}
		return result;
	}
	
	public static void main(String[] args) {
		String[] nets = {"net1", "net2"};
		NeutronRouter r = new NeutronRouter("route1", Arrays.asList(nets),"CMCC");
		System.out.println(r.getResourceMap());
		System.out.println(r.getRouterInterfaces());
	}
}
