package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;

public class IPSecSiteConnection {

	private String id;
	private String name;
	private String psk;
	private String status;
	private String tenant_id;
	private String description;
	private String auth_mode;
	private String route_mode;
	/*
	 * A valid value is response- only or
	 * bi-directional. Default is bi-directional
	 */
	private String initiator; 
	private String ipsecpolicy_id;
	private Boolean admin_state_up;
	private Integer mtu;
	private String peer_ep_group_id;
	private String ikepolicy_id;
	private String vpnservice_id;
	private String local_ep_group_id;
	private String peer_address;
	private String peer_id;
    private List<String> peer_cidrs;
    
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPsk() {
		return psk;
	}

	public void setPsk(String psk) {
		this.psk = psk;
	}

	public String getInitiator() {
		return initiator;
	}

	public void setInitiator(String initiator) {
		this.initiator = initiator;
	}

	public String getIpsecpolicy_id() {
		return ipsecpolicy_id;
	}

	public void setIpsecpolicy_id(String ipsecpolicy_id) {
		this.ipsecpolicy_id = ipsecpolicy_id;
	}

	public Boolean getAdmin_state_up() {
		return admin_state_up;
	}

	public void setAdmin_state_up(Boolean admin_state_up) {
		this.admin_state_up = admin_state_up;
	}

	public Integer getMtu() {
		return mtu;
	}

	public void setMtu(Integer mtu) {
		this.mtu = mtu;
	}

	public String getPeer_ep_group_id() {
		return peer_ep_group_id;
	}

	public void setPeer_ep_group_id(String peer_ep_group_id) {
		this.peer_ep_group_id = peer_ep_group_id;
	}

	public String getIkepolicy_id() {
		return ikepolicy_id;
	}

	public void setIkepolicy_id(String ikepolicy_id) {
		this.ikepolicy_id = ikepolicy_id;
	}

	public String getVpnservice_id() {
		return vpnservice_id;
	}

	public void setVpnservice_id(String vpnservice_id) {
		this.vpnservice_id = vpnservice_id;
	}

	public String getLocal_ep_group_id() {
		return local_ep_group_id;
	}

	public void setLocal_ep_group_id(String local_ep_group_id) {
		this.local_ep_group_id = local_ep_group_id;
	}

	public String getPeer_address() {
		return peer_address;
	}

	public void setPeer_address(String peer_address) {
		this.peer_address = peer_address;
	}

	public String getPeer_id() {
		return peer_id;
	}

	public void setPeer_id(String peer_id) {
		this.peer_id = peer_id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTenant_id() {
		return tenant_id;
	}

	public void setTenant_id(String tenant_id) {
		this.tenant_id = tenant_id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAuth_mode() {
		return auth_mode;
	}

	public void setAuth_mode(String auth_mode) {
		this.auth_mode = auth_mode;
	}

	public String getRoute_mode() {
		return route_mode;
	}

	public void setRoute_mode(String route_mode) {
		this.route_mode = route_mode;
	}

	public List<String> getPeer_cidrs() {
		return peer_cidrs;
	}

	public void setPeer_cidrs(List<String> peer_cidrs) {
		this.peer_cidrs = peer_cidrs;
	}
}
