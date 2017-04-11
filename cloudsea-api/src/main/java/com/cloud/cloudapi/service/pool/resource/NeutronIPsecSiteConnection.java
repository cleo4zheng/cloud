package com.cloud.cloudapi.service.pool.resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NeutronIPsecSiteConnection extends BaseResource {
	private String type = "OS::Neutron::IPsecSiteConnection";
	private String resourceName = null;
	private String router;
	private String[] subnets;
	private String psk;
	private String ikepolicy;
	private String ipsecpolicy;
	private String vpnservice;
	private String peerAddress;
	private String peerCidr;

	public NeutronIPsecSiteConnection(int index, String router, String[] subnets, String psk, String ikepolicy,
			String ipsecpolicy, String vpnservice) {
		this.resourceName = "ipsec_site_connection_" + index;
		this.router = router;
		this.subnets = subnets;
		this.psk = psk;
		this.ikepolicy = ikepolicy;
		this.ipsecpolicy = ipsecpolicy;
		this.vpnservice = vpnservice;
	}

	public NeutronIPsecSiteConnection(String vpnname, String peerAddress, String peerCidr, String psk, String ikepolicy,
			String ipsecpolicy, String vpnservice) {
		this.resourceName = vpnname + "_ipsec_site_connection";

		this.psk = psk;
		this.ikepolicy = ikepolicy;
		this.ipsecpolicy = ipsecpolicy;
		this.vpnservice = vpnservice;
		this.peerAddress = peerAddress;
		this.peerCidr = peerCidr;
	}

	@Override
	public String getResourceName() {
		return this.resourceName;
	}

//	@Override
//	public Map<String, Object> getResourceMap() {
//		Map<String, Object> properties = new LinkedHashMap<String, Object>();
//		properties.put("name", this.resourceName);
//		Map<String, List<String>> r1 = new LinkedHashMap<String, List<String>>();
//		List<String> l1 = new ArrayList<String>();
//		l1.add(this.router);
//		l1.add("external_gateway_info");
//		l1.add("external_fixed_ips");
//		l1.add("0");
//		l1.add("ip_address");
//		r1.put("get_attr", l1);
//		properties.put("peer_address", r1);
//		properties.put("peer_id", r1);
//
//		List<Map<String, List<String>>> cidrs = new ArrayList<Map<String, List<String>>>();
//		for (String subnet : this.subnets) {
//			Map<String, List<String>> r2 = new LinkedHashMap<String, List<String>>();
//			List<String> l2 = new ArrayList<String>();
//			l2.add(subnet);
//			l2.add("cidr");
//			r2.put("get_attr", l2);
//			cidrs.add(r2);
//		}
//		properties.put("psk", this.psk);
//		properties.put("admin_state_up", true);
//
//		Map<String, String> r3 = new LinkedHashMap<String, String>();
//		r3.put("get_resource", this.ikepolicy);
//		properties.put("ikepolicy_id", r3);
//		Map<String, String> r4 = new LinkedHashMap<String, String>();
//		r4.put("get_resource", this.ipsecpolicy);
//		properties.put("ipsecpolicy_id", r4);
//		Map<String, String> r5 = new LinkedHashMap<String, String>();
//		r5.put("get_resource", this.vpnservice);
//		properties.put("vpnservice_id", r5);
//
//		Map<String, Object> one = new LinkedHashMap<String, Object>();
//		one.put("type", this.type);
//		one.put("properties", properties);
//		Map<String, Object> res = new LinkedHashMap<String, Object>();
//		res.put(this.resourceName, one);
//		return res;
//	}

	@Override
	public Map<String, Object> getResourceMap() {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("name", this.resourceName);
		Map<String, String> r3 = new LinkedHashMap<String, String>();
		r3.put("get_resource", this.ikepolicy);
		properties.put("ikepolicy_id", r3);
		Map<String, String> r4 = new LinkedHashMap<String, String>();
		r4.put("get_resource", this.ipsecpolicy);
		properties.put("ipsecpolicy_id", r4);
		Map<String, String> r5 = new LinkedHashMap<String, String>();
		r5.put("get_resource", this.vpnservice);
		properties.put("vpnservice_id", r5);
		properties.put("psk", this.psk);
		
		properties.put("peer_address", this.peerAddress);
		properties.put("peer_id", this.peerAddress);
		List<String> cidrs = new ArrayList<String>();
		cidrs.add(this.peerCidr);
		properties.put("peer_cidrs", cidrs);
		
		Map<String, Object> one = new LinkedHashMap<String, Object>();
		one.put("type", this.type);
		one.put("properties", properties);
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put(this.resourceName, one);
		return res;
	}
}
