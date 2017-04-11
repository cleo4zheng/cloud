package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.util.StringHelper;

public class Network {

	private String id;
	private String name;
	private String status;
	private String createdAt;
	private Boolean managed;
	private Boolean admin_state_up;
	private Boolean shared;
	private Boolean basic;
	// other attibute
	private String tenant_id;
	private String instance_id;
	private String subnetId;
	private String portId;
	private String floatingipId;
	private Integer mtu;
	private String qos_policy_id;
	private Boolean external;
	private String nodeType;
	private Long millionSeconds;
	private List<String> subnetsId = new ArrayList<String>();
	private List<Subnet> subnets = new ArrayList<Subnet>();
	private List<Port> ports = new ArrayList<Port>();
	private List<FloatingIP> floatingIps = new ArrayList<FloatingIP>();
	private List<SecurityGroup> securityGroups = new ArrayList<SecurityGroup>();
    private List<String> servers;
    
	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStatus() {
		return this.status;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getCreatedAt() {
		return this.createdAt;
	}

	public void setTenant_id(String tenant_id) {
		this.tenant_id = tenant_id;
	}

	public String getTenant_id() {
		return this.tenant_id;
	}

	public List<Subnet> getSubnets() {
		return subnets;
	}

	public void setSubnets(List<Subnet> subnets) {
		this.subnets = subnets;
	}

	public void addSubnet(Subnet subnet) {
		this.subnets.add(subnet);
	}

	public String getInstance_id() {
		return instance_id;
	}

	public void setInstance_id(String instance_id) {
		this.instance_id = instance_id;
	}

	public List<String> getSubnetsId() {
		return subnetsId;
	}

	public void setSubnetsId(List<String> subnetsId) {
		this.subnetsId = subnetsId;
	}

	public String getSubnetId() {
		return subnetId;
	}

	public void setSubnetId(String subnetId) {
		this.subnetId = subnetId;
	}

	public String getPortId() {
		return portId;
	}

	public void setPortId(String portId) {
		this.portId = portId;
	}

	public void addSubnetId(String id) {
		this.subnetsId.add(id);
	}

	public void makeSubnetsId() {
		if(Util.isNullOrEmptyList(this.subnets))
			return;
		List<String> subnetsId = new ArrayList<String>();
		for(Subnet subnet : this.subnets){
			subnetsId.add(subnet.getId());
		}
		this.setSubnetId(Util.listToString(subnetsId, ','));
		this.setSubnetsId(subnetsId);
	}
	
	public List<Port> getPorts() {
		return ports;
	}

	public void setPorts(List<Port> ports) {
		this.ports = ports;
	}

	public void addPort(Port port) {
		this.ports.add(port);
	}

	public String getFloatingipId() {
		return floatingipId;
	}

	public void setFloatingipId(String floatingipId) {
		this.floatingipId = floatingipId;
	}

	public List<FloatingIP> getFloatingIps() {
		return floatingIps;
	}

	public void setFloatingIps(List<FloatingIP> floatingIps) {
		this.floatingIps = floatingIps;
	}

	public void addFloatingIP(FloatingIP floatingIP) {
		this.floatingIps.add(floatingIP);
	}

	public Boolean getManaged() {
		return managed;
	}

	public void setManaged(Boolean managed) {
		this.managed = managed;
	}

	public Boolean getAdmin_state_up() {
		return admin_state_up;
	}

	public void setAdmin_state_up(Boolean admin_state_up) {
		this.admin_state_up = admin_state_up;
	}

	public Boolean getShared() {
		return shared;
	}

	public void setShared(Boolean shared) {
		this.shared = shared;
	}

	public Integer getMtu() {
		return mtu;
	}

	public void setMtu(Integer mtu) {
		this.mtu = mtu;
	}

	public String getQos_policy_id() {
		return qos_policy_id;
	}

	public void setQos_policy_id(String qos_policy_id) {
		this.qos_policy_id = qos_policy_id;
	}

	public List<SecurityGroup> getSecurityGroups() {
		return securityGroups;
	}

	public void setSecurityGroups(List<SecurityGroup> securityGroups) {
		this.securityGroups = securityGroups;
	}

	public void addSecurityGroup(SecurityGroup securityGroup){
		this.securityGroups.add(securityGroup);
	}

	public Boolean getExternal() {
		return external;
	}

	public void setExternal(Boolean external) {
		this.external = external;
	}

	public List<String> getServers() {
		return servers;
	}

	public void setServers(List<String> servers) {
		this.servers = servers;
	}

	public String getNodeType() {
		return nodeType;
	}

	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	public Boolean getBasic() {
		return basic;
	}

	public void setBasic(Boolean basic) {
		this.basic = basic;
	}
	
	public void normalInfo(Boolean normalSubnet){
		this.setName(StringHelper.ncr2String(this.getName()));
		this.setAdmin_state_up(null);
		this.setBasic(null);
		this.setExternal(null);
		this.setFloatingipId(null);
		this.setFloatingIps(null);
		this.setInstance_id(null);
		this.setMtu(null);
		this.setNodeType(null);
		this.setPortId(null);
		this.setPorts(null);
		this.setQos_policy_id(null);
		this.setSecurityGroups(null);
		this.setServers(null);
		this.setShared(null);
		this.setTenant_id(null);
		this.setSubnetId(null);
		this.setSubnetsId(null);
		if(true == normalSubnet)
			this.setSubnets(null);
		if(null != this.getMillionSeconds()){
			this.setCreatedAt(Util.millionSecond2Date(this.getMillionSeconds()));
			this.setMillionSeconds(null);	
		}
	}
}
