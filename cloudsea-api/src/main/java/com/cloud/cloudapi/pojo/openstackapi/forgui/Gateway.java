package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;


public class Gateway {
	private String id;
	private String network_id;
	private Boolean enable_snat;
	private List<FixedIP> external_fixed_ips;
    private String fixedIds; 
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNetwork_id() {
		return network_id;
	}

	public void setNetwork_id(String network_id) {
		this.network_id = network_id;
	}

	public Boolean getEnable_snat() {
		return enable_snat;
	}

	public void setEnable_snat(Boolean enable_snat) {
		this.enable_snat = enable_snat;
	}

	public List<FixedIP> getExternal_fixed_ips() {
		return external_fixed_ips;
	}

	public void setExternal_fixed_ips(List<FixedIP> external_fixed_ips) {
		this.external_fixed_ips = external_fixed_ips;
	}

	public void makeFixedIPs() {
		if(null == this.external_fixed_ips)
			return;
		List<String> fixedIPIds = new ArrayList<String>();
		for(FixedIP fixedIP : this.external_fixed_ips){
			fixedIPIds.add(fixedIP.getId());
		}
		this.fixedIds = Util.listToString(fixedIPIds, ',');
	}
	
	public String getFixedIds() {
		return fixedIds;
	}

	public void setFixedIds(String fixedIds) {
		this.fixedIds = fixedIds;
	}

}
