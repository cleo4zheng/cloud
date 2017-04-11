package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class IkePolicy extends Policy {
	private String phase1_negotiation_mode;
	private String ike_version;

	public String getPhase1_negotiation_mode() {
		return phase1_negotiation_mode;
	}

	public void setPhase1_negotiation_mode(String phase1_negotiation_mode) {
		this.phase1_negotiation_mode = phase1_negotiation_mode;
	}

	public String getIke_version() {
		return ike_version;
	}

	public void setIke_version(String ike_version) {
		this.ike_version = ike_version;
	}

}
