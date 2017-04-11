package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.util.StringHelper;

public class HostAggregate {

	private String id;
	private String name;
	private String availabilityZone;
	private Long millionSeconds;
	private String createdAt;
	private List<Host> hosts;
	private List<String> hostNames;
	private String hostIds;
	private String description;
	private String serviceId;
	private String source;
	
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
	
	public String getAvailabilityZone() {
		return availabilityZone;
	}
	
	public void setAvailabilityZone(String availabilityZone) {
		this.availabilityZone = availabilityZone;
	}
	
	public Long getMillionSeconds() {
		return millionSeconds;
	}
	
	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public List<Host> getHosts() {
		return hosts;
	}

	public void setHosts(List<Host> hosts) {
		this.hosts = hosts;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getHostIds() {
		return hostIds;
	}

	public void setHostIds(String hostIds) {
		this.hostIds = hostIds;
	}

	public List<String> getHostNames() {
		return hostNames;
	}

	public void setHostNames(List<String> hostNames) {
		this.hostNames = hostNames;
	}
	
	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void normalInfo(){
		if(null != this.getMillionSeconds()){
			this.setCreatedAt(Util.millionSecond2Date(this.getMillionSeconds()));
			this.setMillionSeconds(null);
		}
		this.setName(StringHelper.ncr2String(this.getName()));
		this.setHostIds(null);
		this.setHostNames(null);
		this.setServiceId(null);
		if(null != this.getHosts()){
			for(Host host : this.getHosts()){
				host.normalInfo(null,true);
			}
		}
	}
}
