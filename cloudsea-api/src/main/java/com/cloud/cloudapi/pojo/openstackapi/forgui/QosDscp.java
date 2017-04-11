package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class QosDscp {

	private String id;
	private String policy_id;
	private Integer dscp_mark;

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

	public Integer getDscp_mark() {
		return dscp_mark;
	}

	public void setDscp_mark(Integer dscp_mark) {
		this.dscp_mark = dscp_mark;
	}

}
