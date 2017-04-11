package com.cloud.cloudapi.pojo.openstackapi.forgui;

import com.cloud.cloudapi.pojo.common.Util;

public class VPN {

	private String id;
	private String router_id;
	private String status;
	private String name;
	private String external_v6_ip;
	private Boolean admin_state_up;
	private String subnet_id;
	private String tenant_id;
	private String external_v4_ip;
	private String description;
    private String ikePolicyId;
    private String ipsecPolicyId;
    private String ikePolicyName;
    private String ipsecPolicyName;
    private String ipsecSiteconId;
    private String createdAt;
    private Long millionSeconds;
    private Router router;
    private Subnet subnet;
    
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRouter_id() {
		return router_id;
	}

	public void setRouter_id(String router_id) {
		this.router_id = router_id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getExternal_v6_ip() {
		return external_v6_ip;
	}

	public void setExternal_v6_ip(String external_v6_ip) {
		this.external_v6_ip = external_v6_ip;
	}

	public Boolean getAdmin_state_up() {
		return admin_state_up;
	}

	public void setAdmin_state_up(Boolean admin_state_up) {
		this.admin_state_up = admin_state_up;
	}

	public String getSubnet_id() {
		return subnet_id;
	}

	public void setSubnet_id(String subnet_id) {
		this.subnet_id = subnet_id;
	}

	public String getTenant_id() {
		return tenant_id;
	}

	public void setTenant_id(String tenant_id) {
		this.tenant_id = tenant_id;
	}

	public String getExternal_v4_ip() {
		return external_v4_ip;
	}

	public void setExternal_v4_ip(String external_v4_ip) {
		this.external_v4_ip = external_v4_ip;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getIkePolicyId() {
		return ikePolicyId;
	}

	public void setIkePolicyId(String ikePolicyId) {
		this.ikePolicyId = ikePolicyId;
	}

	public String getIpsecPolicyId() {
		return ipsecPolicyId;
	}

	public void setIpsecPolicyId(String ipsecPolicyId) {
		this.ipsecPolicyId = ipsecPolicyId;
	}

	public String getIpsecSiteconId() {
		return ipsecSiteconId;
	}

	public void setIpsecSiteconId(String ipsecSiteconId) {
		this.ipsecSiteconId = ipsecSiteconId;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getIkePolicyName() {
		return ikePolicyName;
	}

	public void setIkePolicyName(String ikePolicyName) {
		this.ikePolicyName = ikePolicyName;
	}

	public String getIpsecPolicyName() {
		return ipsecPolicyName;
	}

	public void setIpsecPolicyName(String ipsecPolicyName) {
		this.ipsecPolicyName = ipsecPolicyName;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}
	
	public Router getRouter() {
		return router;
	}

	public void setRouter(Router router) {
		this.router = router;
	}

	public Subnet getSubnet() {
		return subnet;
	}

	public void setSubnet(Subnet subnet) {
		this.subnet = subnet;
	}

	public void normalInfo(){
		this.setAdmin_state_up(null);
		this.setTenant_id(null);
		this.setExternal_v4_ip(null);
		this.setExternal_v6_ip(null);
		this.setSubnet_id(null);
		this.setRouter_id(null);
		this.setIkePolicyId(null);
		this.setIpsecPolicyId(null);
		this.setIpsecSiteconId(null);
		if(null != this.getMillionSeconds()){
			this.setCreatedAt(Util.millionSecond2Date(this.getMillionSeconds()));
			this.setMillionSeconds(null);
		}
		this.setMillionSeconds(null);
		if(null != subnet)
			subnet.normalInfo(true);
		if(null != router)
			router.normalInfo();
	}
}
