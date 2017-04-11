package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;

public class QosPolicy {

	private String id;
	private String description;
	private String tenant_id;
	private String name;
	private Boolean shared;
	private List<QosBandwith> bandwidth_limit_rules;
 	private List<QosDscp> dscp_marking_rules;
    private String bandwithIds;
    private String dscpIds;
    
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTenant_id() {
		return tenant_id;
	}

	public void setTenant_id(String tenant_id) {
		this.tenant_id = tenant_id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getShared() {
		return shared;
	}

	public void setShared(Boolean shared) {
		this.shared = shared;
	}

	public List<QosBandwith> getBandwidth_limit_rules() {
		return bandwidth_limit_rules;
	}

	public void setBandwidth_limit_rules(List<QosBandwith> bandwidth_limit_rules) {
		this.bandwidth_limit_rules = bandwidth_limit_rules;
	}

	public List<QosDscp> getDscp_marking_rules() {
		return dscp_marking_rules;
	}

	public void setDscp_marking_rules(List<QosDscp> dscp_marking_rules) {
		this.dscp_marking_rules = dscp_marking_rules;
	}

	public String getBandwithIds() {
		return bandwithIds;
	}

	public void setBandwithIds(String bandwithIds) {
		this.bandwithIds = bandwithIds;
	}

	public String getDscpIds() {
		return dscpIds;
	}

	public void setDscpIds(String dscpIds) {
		this.dscpIds = dscpIds;
	}

}
