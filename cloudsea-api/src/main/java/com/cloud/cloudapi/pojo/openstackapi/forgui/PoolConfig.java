package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;

public class PoolConfig {
	private String id;
	private VolumeConfig volume;
	private ImageConfig image;
	private List<InstanceType> instanceTypes;
	private List<ResourceSpec> fips;
	private List<ResourceSpec> services;

	public PoolConfig(){
		this.instanceTypes = new ArrayList<InstanceType>();
		this.fips = new ArrayList<ResourceSpec>();
		this.services = new ArrayList<ResourceSpec>();
		
	}
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public VolumeConfig getVolume() {
		return volume;
	}

	public void setVolume(VolumeConfig volume) {
		this.volume = volume;
	}

	public List<InstanceType> getInstanceTypes() {
		return instanceTypes;
	}

	public void setInstanceTypes(List<InstanceType> instanceTypes) {
		this.instanceTypes = instanceTypes;
	}

	public List<ResourceSpec> getFips() {
		return fips;
	}

	public void setFips(List<ResourceSpec> fips) {
		this.fips = fips;
	}

	public List<ResourceSpec> getServices() {
		return services;
	}

	public void setServices(List<ResourceSpec> services) {
		this.services = services;
	}
	
	public void addInstanceType(InstanceType instanceType) {
		this.instanceTypes.add(instanceType);
	}
	
	public void addResource(String name,Double unitPrice,boolean bService){
		ResourceSpec resource = new ResourceSpec();
		resource.setName(name);
		resource.setUnitPrice(unitPrice);
		if(true == bService){
			this.services.add(resource);
		}else{
			this.fips.add(resource);
		}
	}

	public void addFipResource(ResourceSpec resource) {
		this.fips.add(resource);
	}

	public void addServiceResource(ResourceSpec resource) {
		this.services.add(resource);
	}
	public ImageConfig getImage() {
		return image;
	}
	public void setImage(ImageConfig image) {
		this.image = image;
	}
	
	public InstanceType getInstanceTypeByName(String name){
		if(Util.isNullOrEmptyList(this.instanceTypes))
			return null;
		for(InstanceType type : this.instanceTypes){
			if(name.equals(type.getId()))
				return type;
		}
		return null;
	}
	
	public double getServicePrice(String serviceType){
		for(ResourceSpec service : this.services){
			if(serviceType.equalsIgnoreCase(service.getName()))
				return service.getUnitPrice();
		}
		return 0;
	}
}
