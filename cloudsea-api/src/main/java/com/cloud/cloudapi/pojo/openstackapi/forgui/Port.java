package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.util.StringHelper;

public class Port {
	private String id;
	private String name;
	private String macAddress;
	private String status;
	private String network_id;
	private String tenantId;
	private String subnetId;
	private String ip; // fixedIp or floatingIp
	private String createdAt;
	private Subnet subnet;
	private Boolean admin_state_up;
	private String device_owner;
	private String device_id;
	private List<FixedIP> fixed_ips;
	private List<SecurityGroup> securityGroups;
	private String securityGroupId;
    private List<String> security_groups;
	private ResourceSpec resource;
    //private Instance instance;
    //private Router router;
    private FloatingIP floatingIp;
    private String type;
    private Long millionSeconds;
    
	public Port() {
		this.id = new String();
		this.name = new String();
		this.status = new String();
		this.createdAt = new String();
		this.subnet = new Subnet();
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}

	public String getNetwork_id() {
		return network_id;
	}

	public void setNetwork_id(String network_id) {
		this.network_id = network_id;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public Subnet getSubnet() {
		return subnet;
	}

	public void setSubnet(Subnet subnet) {
		this.subnet = subnet;
	}

	public String getSubnetId() {
		return subnetId;
	}

	public void setSubnetId(String subnetId) {
		this.subnetId = subnetId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Boolean getAdmin_state_up() {
		return admin_state_up;
	}

	public void setAdmin_state_up(Boolean admin_state_up) {
		this.admin_state_up = admin_state_up;
	}

	public String getDevice_owner() {
		return device_owner;
	}

	public void setDevice_owner(String device_owner) {
		this.device_owner = device_owner;
	}

	public List<FixedIP> getFixed_ips() {
		return fixed_ips;
	}

	public void setFixed_ips(List<FixedIP> fixed_ips) {
		this.fixed_ips = fixed_ips;
	}

	public String getSecurityGroupId() {
		return securityGroupId;
	}

	public void setSecurityGroupId(String securityGroupId) {
		this.securityGroupId = securityGroupId;
	}

	public List<String> getSecurity_groups() {
		return security_groups;
	}

	public void setSecurity_groups(List<String> security_groups) {
		this.security_groups = security_groups;
	}

	public String getDevice_id() {
		return device_id;
	}

	public void setDevice_id(String device_id) {
		this.device_id = device_id;
	}

	public void makeIpAndSubnetId() {
		if (Util.isNullOrEmptyList(this.fixed_ips))
			return;
		StringBuilder ip = new StringBuilder();
		StringBuilder subnetId = new StringBuilder(); 
		int length = this.fixed_ips.size();
        for(int index =0; index < this.fixed_ips.size();++index){
        	ip.append(this.fixed_ips.get(index).getIp_address());
        	subnetId.append(this.fixed_ips.get(index).getSubnet_id());
        	if(index < length-1){
        		ip.append(",");
            	subnetId.append(",");
        	}	
        }
        this.subnetId = subnetId.toString();
        this.ip = ip.toString();
	}


	public FloatingIP getFloatingIp() {
		return floatingIp;
	}

	public void setFloatingIp(FloatingIP floatingIp) {
		this.floatingIp = floatingIp;
	}

	public List<SecurityGroup> getSecurityGroups() {
		return securityGroups;
	}

	public void setSecurityGroups(List<SecurityGroup> securityGroups) {
		this.securityGroups = securityGroups;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}


	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public ResourceSpec getResource() {
		return resource;
	}

	public void setResource(ResourceSpec resource) {
		this.resource = resource;
	}

	public void addResource(String id,String name,String type){
		this.resource = new ResourceSpec();
		this.resource.setId(id);
		this.resource.setName(name);
		this.resource.setType(type);
	}
	
	public void normalInfo(Boolean forInstance){
		this.setName(StringHelper.ncr2String(this.getName()));
		ResourceSpec resource = this.getResource();
		if(null != resource){
			resource.setName(StringHelper.ncr2String(resource.getName()));
			resource.setFree(null);
			resource.setRange(null);
			resource.setSize(null);
			resource.setTotal(null);
			resource.setTypeName(null);
			resource.setUnitPrice(null);
			resource.setUsed(null);
			this.setResource(resource);
		}
		if(true == forInstance){
			this.setAdmin_state_up(null);
			this.setDevice_id(null);
			this.setDevice_owner(null);
			this.setFixed_ips(null);
			this.setNetwork_id(null);
			this.setSecurity_groups(null);
			this.setSecurityGroupId(null);
			this.setTenantId(null);
			this.setMacAddress(null);
			this.setCreatedAt(null);
			this.setMillionSeconds(null);
			this.setStatus(null);
			this.setSubnetId(null);
		}else{
			this.setAdmin_state_up(null);
			this.setDevice_owner(null);
			this.setFixed_ips(null);
			this.setNetwork_id(null);
			this.setSecurity_groups(null);
			this.setSecurityGroupId(null);
			this.setSubnetId(null);
			this.setTenantId(null);
			if(null != this.getMillionSeconds()){
				this.setCreatedAt(Util.millionSecond2Date(this.getMillionSeconds()));
				this.setMillionSeconds(null);
			}
		}
		if(null != this.subnet)
			this.subnet.normalInfo(true);
//		if(null != this.instance)
//			this.instance.normalInfo(true);
//		if(null != this.router)
//			this.router.normalInfo();
		if(null != this.floatingIp)
			this.floatingIp.normalInfo(null);
		if(null != this.securityGroups){
			for(SecurityGroup securityGroup : this.securityGroups)
				securityGroup.normalInfo(true);
		}
	}
}
