package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;

public class InterfaceAttachment {

	class FixedIP {
		private String ip_address;
		private String subnet_id;

		public FixedIP(){};
		
		public FixedIP(String ip_address, String subnet_id){
			this.ip_address = ip_address;
			this.subnet_id = subnet_id;
		}
		
		public String getIp_address() {
			return ip_address;
		}

		public void setIp_address(String ip_address) {
			this.ip_address = ip_address;
		}

		public String getSubnet_id() {
			return subnet_id;
		}

		public void setSubnet_id(String subnet_id) {
			this.subnet_id = subnet_id;
		}

	}

	private String mac_addr;
	private String port_state;
	private String net_id;
	private String port_id;
	private String ip_address;
	private String subnet_id;
    private List<FixedIP> fixed_ips;
    
	public String getNet_id() {
		return net_id;
	}

	public void setNet_id(String net_id) {
		this.net_id = net_id;
	}

	public String getPort_id() {
		return port_id;
	}

	public void setPort_id(String port_id) {
		this.port_id = port_id;
	}

	public String getMac_addr() {
		return mac_addr;
	}

	public void setMac_addr(String mac_addr) {
		this.mac_addr = mac_addr;
	}

	public String getPort_state() {
		return port_state;
	}

	public void setPort_state(String port_state) {
		this.port_state = port_state;
	}

	public String getIp_address() {
		return ip_address;
	}

	public void setIp_address(String ip_address) {
		this.ip_address = ip_address;
	}

	public String getSubnet_id() {
		return subnet_id;
	}

	public void setSubnet_id(String subnet_id) {
		this.subnet_id = subnet_id;
	}

	public List<FixedIP> getFixed_ips() {
		return fixed_ips;
	}

	public void setFixed_ips(List<FixedIP> fixed_ips) {
		this.fixed_ips = fixed_ips;
	}
	
	public void addFixedIP(String ipAddress,String subnetId){
		if(null == this.fixed_ips)
			this.fixed_ips = new ArrayList<FixedIP>();
		this.fixed_ips.add(new FixedIP(ipAddress,subnetId));
	}

	public void makeSubnetId(){
		if(Util.isNullOrEmptyList(this.fixed_ips))
			return;
		int index = 1;
		this.subnet_id = "";
		for(FixedIP fixedIP : this.fixed_ips){
			this.subnet_id += fixedIP.getSubnet_id();
			if(index < this.fixed_ips.size())
				this.subnet_id += ",";
			++index;
		}
	}
}
