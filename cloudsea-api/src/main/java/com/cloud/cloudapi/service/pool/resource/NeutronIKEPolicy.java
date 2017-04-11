package com.cloud.cloudapi.service.pool.resource;

import java.util.LinkedHashMap;
import java.util.Map;

public class NeutronIKEPolicy extends BaseResource {
	private String type = "OS::Neutron::IKEPolicy";
	private String resourceName = null;
	private String auth_algorithm;
	private String encryption_algorithm;

	public NeutronIKEPolicy(String vpnName) {
		this.resourceName = vpnName + "_ike_policy";
		this.auth_algorithm = "sha1";
		this.encryption_algorithm = "3des";
	}

	public NeutronIKEPolicy(String auth_algorithm, String encryption_algorithm) {
		this.resourceName = "ike_policy";
		this.auth_algorithm = auth_algorithm;
		this.encryption_algorithm = encryption_algorithm;
	}

	@Override
	public String getResourceName() {
		return this.resourceName;
	}

	@Override
	public Map<String, Object> getResourceMap() {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("name", resourceName);
		properties.put("auth_algorithm", auth_algorithm);
		properties.put("encryption_algorithm", encryption_algorithm);

		Map<String, Object> net = new LinkedHashMap<String, Object>();
		net.put("type", this.type);
		net.put("properties", properties);
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put(this.resourceName, net);
		return res;
	}

}
