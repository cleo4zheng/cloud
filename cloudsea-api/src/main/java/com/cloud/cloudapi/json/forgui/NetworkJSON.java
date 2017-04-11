package com.cloud.cloudapi.json.forgui;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Network;

public class NetworkJSON {
	private Network network;

	public NetworkJSON() {
		this.network = new Network();
	}

	public NetworkJSON(Network network) {
		this.network = network;
	}
	
	public void setNetworkInfo(String name, String tenant_id, Boolean state) {
		if (null != this.network) {
			this.network.setName(name);
			this.network.setAdmin_state_up(state);
			this.network.setTenant_id(tenant_id);
		}
	}

}
