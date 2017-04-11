package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;
import java.util.Locale;

public class EnvResource {

	private String id;
	private List<ResourceUsedInfo> physicalServers;
	private List<ResourceSpec> storages;
	private List<ResourceSpec> floatingIps;
	private List<ResourceSpec> createdResources;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<ResourceUsedInfo> getPhysicalServers() {
		return physicalServers;
	}

	public void setPhysicalServers(List<ResourceUsedInfo> physicalServers) {
		this.physicalServers = physicalServers;
	}

	public List<ResourceSpec> getFloatingIps() {
		return floatingIps;
	}

	public void setFloatingIps(List<ResourceSpec> floatingIps) {
		this.floatingIps = floatingIps;
	}

	public List<ResourceSpec> getStorages() {
		return storages;
	}

	public void setStorages(List<ResourceSpec> storages) {
		this.storages = storages;
	}

	public List<ResourceSpec> getCreatedResources() {
		return createdResources;
	}

	public void setCreatedResources(List<ResourceSpec> createdResources) {
		this.createdResources = createdResources;
	}

	public void normalInfo(Locale locale){
		if(null != physicalServers){
			for(ResourceUsedInfo resource : physicalServers){
				resource.normalInfo(locale);
			}
		}
		/*
		if (null != storages) {
			for (ResourceSpec resource : storages) {
				resource.normalInfo(locale);
			}
		}
		*/
		if (null != floatingIps) {
			for (ResourceSpec resource : floatingIps) {
				resource.normalInfo(locale);
			}
		}
	}
}
