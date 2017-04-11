package com.cloud.cloudapi.json.forgui;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Subnet;

public class SubnetJSON {
    private Subnet subnet;
    
    public SubnetJSON(){
    	this.subnet = new Subnet();
    }
    
    public SubnetJSON(Subnet subnet){
    	this.subnet = subnet;
    }
    
    public void setSubnetInfo(String network_id,String name,String cidr,String tenant_id,String ip_version){
    	this.subnet.setNetwork_id(network_id);
    	this.subnet.setName(name);
    	this.subnet.setCidr(cidr);
    	this.subnet.setTenant_id(tenant_id);
    	this.subnet.setIp_version(Integer.parseInt(ip_version));
    }
    
    public void setSubnetInfo(Subnet subnetInfo){
    	this.subnet.setNetwork_id(subnetInfo.getNetwork_id());
    	this.subnet.setName(subnetInfo.getName());
    	this.subnet.setIp_version(4/*Integer.parseInt(subnetInfo.getIpVersion())*/); //TODO
    	this.subnet.setCidr(subnetInfo.getCidr());
    	this.subnet.setEnable_dhcp(subnetInfo.getDhcp());;
    	this.subnet.setGateway_ip(subnetInfo.getGateway());
    }
}
