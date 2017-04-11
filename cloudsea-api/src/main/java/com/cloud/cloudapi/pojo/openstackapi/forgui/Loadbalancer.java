package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.util.StringHelper;

public class Loadbalancer {

	private String id;
	private String name;
	private String description;
	private Boolean admin_state_up;
	private String tenant_id;
	private String provisioning_status;
	private String vip_address;
	private String vip_subnet_id;
	private String vip_port_id;
	private String floatingIp;
	private String operating_status;
	private String provider;
	private String listenerIds;
	private String poolIds;
	private String createdAt;
	private Long millionSeconds;
	private List<String> ports;
	private List<Instance> instances;
//	private List<Listener> listeners;
//	private List<LBPool> pools;
	
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getAdmin_state_up() {
		return admin_state_up;
	}

	public void setAdmin_state_up(Boolean admin_state_up) {
		this.admin_state_up = admin_state_up;
	}

	public String getTenant_id() {
		return tenant_id;
	}

	public void setTenant_id(String tenant_id) {
		this.tenant_id = tenant_id;
	}

	public String getProvisioning_status() {
		return provisioning_status;
	}

	public void setProvisioning_status(String provisioning_status) {
		this.provisioning_status = provisioning_status;
	}

	public String getVip_address() {
		return vip_address;
	}

	public void setVip_address(String vip_address) {
		this.vip_address = vip_address;
	}

	public String getVip_subnet_id() {
		return vip_subnet_id;
	}

	public void setVip_subnet_id(String vip_subnet_id) {
		this.vip_subnet_id = vip_subnet_id;
	}

	public String getOperating_status() {
		return operating_status;
	}

	public void setOperating_status(String operating_status) {
		this.operating_status = operating_status;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getListenerIds() {
		return listenerIds;
	}

	public void setListenerIds(String listenerIds) {
		this.listenerIds = listenerIds;
	}

	public String getPoolIds() {
		return poolIds;
	}

	public void setPoolIds(String poolIds) {
		this.poolIds = poolIds;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public List<Instance> getInstances() {
		return instances;
	}

	public void setInstances(List<Instance> instances) {
		this.instances = instances;
	}

	public List<String> getPorts() {
		return ports;
	}

	public void setPorts(List<String> ports) {
		this.ports = ports;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}
	
	public String getVip_port_id() {
		return vip_port_id;
	}

	public void setVip_port_id(String vip_port_id) {
		this.vip_port_id = vip_port_id;
	}

	public String getFloatingIp() {
		return floatingIp;
	}

	public void setFloatingIp(String floatingIp) {
		this.floatingIp = floatingIp;
	}

	public void normalInfo(Boolean normalInstanceInfo){
		this.setName(StringHelper.ncr2String(this.getName()));
		this.setDescription(StringHelper.ncr2String(this.getDescription()));
		this.setAdmin_state_up(null);
		this.setListenerIds(null);
		
		this.setOperating_status(null);
		this.setPoolIds(null);
		this.setProvider(null);
		this.setTenant_id(null);
		this.setVip_subnet_id(null);
		this.setVip_port_id(null);
		this.setTenant_id(null);
		if(null != this.getMillionSeconds()){
			this.setCreatedAt(Util.millionSecond2Date(this.getMillionSeconds()));
			this.setMillionSeconds(null);
		}
		if(true == normalInstanceInfo){
			this.setInstances(null);
			this.setPorts(null);
		}else{
			List<Instance> instances = this.getInstances();
			if(null == instances)
				return;
			for(Instance instance : instances){
				instance.normalInfoExceptIP();
			}
		}
	}

}
