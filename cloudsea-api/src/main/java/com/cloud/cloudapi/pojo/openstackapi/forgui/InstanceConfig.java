package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.List;

public class InstanceConfig {

	private String id;
	private VolumeConfig volume;
	private ImageConfig image;
	private List<InstanceType> instanceTypes;
	
	//For PAAS---
	private List tags;
   
	public InstanceConfig(){
	//	this.id = Util.makeUUID();
		this.instanceTypes = new ArrayList<InstanceType>();
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
	
	public void addInstanceType(InstanceType instanceType) {
		this.instanceTypes.add(instanceType);
	}

	public ImageConfig getImage() {
		return image;
	}

	public void setImage(ImageConfig image) {
		this.image = image;
	}

	public List getTags() {
		return tags;
	}

	public void setTags(List tags) {
		this.tags = tags;
	}
	
}
