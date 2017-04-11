package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.util.StringHelper;

public class Router {
	private String id;
	private String name;
	private String gateway;
//	private String floatingIp;
	private List<String> fixedIps;
	private String createdAt;
	private String updatedAt;
	private String status;
	private String tenant_id;
	private String gatewayId;
//	private String port_id;
	private String portIds;
	private Boolean admin_state_up;
	private Boolean ha;
	private Boolean distributed;
	private Boolean publicGateway;
	private Gateway external_gateway_info;
	private List<Router> routers;
	private String subnet_id;
	private String subnet_name;
    private List<String> subnet_ids;
    private List<Subnet> subnets;
    private String subnetIds;
    private List<FloatingIP> floatingIPs;
    private String floatingIps;
    private List<String> networks;
    private String firewallId;
    private String vpnId;
    private String nodeType;
    private Long millionSeconds;
   // private String subnet_names;
    
	public Router() {
		this.id = new String();
		this.name = new String();
		this.gateway = new String();
		this.floatingIps = new String();
		this.createdAt = new String();
		this.status = new String();
	}

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

	public String getGateway() {
		return gateway;
	}

	public void setGateway(String gateway) {
		this.gateway = gateway;
	}

//	public String getFloatingIp() {
//		return floatingIp;
//	}
//
//	public void setFloatingIp(String floatingIp) {
//		this.floatingIp = floatingIp;
//	}

	
	public String getCreatedAt() {
		return createdAt;
	}

	public List<String> getFixedIps() {
		return fixedIps;
	}

	public void setFixedIps(List<String> fixedIps) {
		this.fixedIps = fixedIps;
	}

	public String getFloatingIps() {
		return floatingIps;
	}

	public void setFloatingIps(String floatingIps) {
		this.floatingIps = floatingIps;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Gateway getExternal_gateway_info() {
		return external_gateway_info;
	}

	public void setExternal_gateway_info(Gateway external_gateway_info) {
		this.external_gateway_info = external_gateway_info;
		if(null != external_gateway_info)
			this.gatewayId = external_gateway_info.getId();
	}

	public void makeSunetAndIpInfo(){
	  if(null == this.external_gateway_info)
		  return;
	  List<FixedIP> fixedIPs = this.external_gateway_info.getExternal_fixed_ips();
	  if(Util.isNullOrEmptyList(fixedIPs))
		  return;
	  List<String> subnetIds = new ArrayList<String>();
	  List<String> floatingIps = new ArrayList<String>();
	  for(FixedIP fixedIP : fixedIPs){
		  subnetIds.add(fixedIP.getSubnet_id());
		  floatingIps.add(fixedIP.getIp());
	  }
	  this.subnetIds = Util.listToString(subnetIds, ',');
	  this.floatingIps = Util.listToString(floatingIps, ',');; 
	  this.subnet_ids = subnetIds;
	}
	
	public List<Router> getRouters() {
		return routers;
	}

	public void setRouters(List<Router> routers) {
		this.routers = routers;
	}

	public String getTenant_id() {
		return tenant_id;
	}

	public void setTenant_id(String tenant_id) {
		this.tenant_id = tenant_id;
	}

	public Boolean getAdmin_state_up() {
		return admin_state_up;
	}

	public void setAdmin_state_up(Boolean admin_state_up) {
		this.admin_state_up = admin_state_up;
	}

	public Boolean getHa() {
		return ha;
	}

	public void setHa(Boolean ha) {
		this.ha = ha;
	}

	public Boolean getDistributed() {
		return distributed;
	}

	public void setDistributed(Boolean distributed) {
		this.distributed = distributed;
	}

//	public String getPort_id() {
//		return port_id;
//	}
//
//	public void setPort_id(String port_id) {
//		this.port_id = port_id;
//	}

	public String getSubnet_id() {
		return subnet_id;
	}

	public void setSubnet_id(String subnet_id) {
		this.subnet_id = subnet_id;
	}
	
	public List<String> getSubnet_ids() {
		return subnet_ids;
	}

	public void setSubnet_ids(List<String> subnet_ids) {
		this.subnet_ids = subnet_ids;
	}

	public String getSubnet_name() {
		return subnet_name;
	}

	public void setSubnet_name(String subnet_name) {
		this.subnet_name = subnet_name;
	}

//	public String getSubnet_names() {
//		return subnet_names;
//	}
//
//	public void setSubnet_names(String subnet_names) {
//		this.subnet_names = subnet_names;
//	}

    

	public String getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(String updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getGatewayId() {
		return gatewayId;
	}

	public void setGatewayId(String gatewayId) {
		this.gatewayId = gatewayId;
	}

	public String getSubnetIds() {
		return subnetIds;
	}

	public void setSubnetIds(String subnetIds) {
		this.subnetIds = subnetIds;
	}

	public List<Subnet> getSubnets() {
		return subnets;
	}

	public void setSubnets(List<Subnet> subnets) {
		this.subnets = subnets;
	}

	public List<FloatingIP> getFloatingIPs() {
		return floatingIPs;
	}

	public void setFloatingIPs(List<FloatingIP> floatingIPs) {
		this.floatingIPs = floatingIPs;
	}

	public String getNodeType() {
		return nodeType;
	}

	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}

	public List<String> getNetworks() {
		return networks;
	}

	public void setNetworks(List<String> networks) {
		this.networks = networks;
	}

	public String getPortIds() {
		return portIds;
	}

	public void setPortIds(String portIds) {
		this.portIds = portIds;
	}

	public Boolean getPublicGateway() {
		return publicGateway;
	}

	public void setPublicGateway(Boolean publicGateway) {
		this.publicGateway = publicGateway;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	public String getFirewallId() {
		return firewallId;
	}

	public void setFirewallId(String firewallId) {
		this.firewallId = firewallId;
	}
	
	public String getVpnId() {
		return vpnId;
	}

	public void setVpnId(String vpnId) {
		this.vpnId = vpnId;
	}

	public void normalInfo(){
		this.setName(StringHelper.ncr2String(this.getName()));
		this.setAdmin_state_up(null);
		this.setDistributed(null);
		this.setExternal_gateway_info(null);
//		this.setFirewallId(null);
		this.setFixedIps(null);
		this.setFloatingIPs(null);
		this.setGateway(null);
		this.setGatewayId(null);
		this.setHa(null);
		this.setNetworks(null);
		this.setNodeType(null);
		this.setRouters(null);
		this.setSubnet_id(null);
		this.setSubnet_ids(null);
		this.setTenant_id(null);
		this.setUpdatedAt(null);
	    this.setVpnId(null);
		if(null != this.getMillionSeconds()){
			this.setCreatedAt(Util.millionSecond2Date(this.getMillionSeconds()));
			this.setMillionSeconds(null);
		}
	}
}
