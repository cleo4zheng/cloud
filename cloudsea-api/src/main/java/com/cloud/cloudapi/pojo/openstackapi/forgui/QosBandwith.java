package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class QosBandwith {

	private String id;
	private String policy_id;
	private Integer max_kbps;
	private Integer max_burst_kbps;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPolicy_id() {
		return policy_id;
	}

	public void setPolicy_id(String policy_id) {
		this.policy_id = policy_id;
	}

	public Integer getMax_kbps() {
		return max_kbps;
	}

	public void setMax_kbps(Integer max_kbps) {
		this.max_kbps = max_kbps;
	}

	public Integer getMax_burst_kbps() {
		return max_burst_kbps;
	}

	public void setMax_burst_kbps(Integer max_burst_kbps) {
		this.max_burst_kbps = max_burst_kbps;
	}

}
