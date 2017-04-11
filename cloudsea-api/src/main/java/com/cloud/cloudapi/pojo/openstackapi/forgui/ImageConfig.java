package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;

public class ImageConfig {
	private String id;
	private List<ResourceSpec> imageTypes;

	public ImageConfig(){
		this.id = Util.makeUUID();
		this.imageTypes = new ArrayList<ResourceSpec>();
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<ResourceSpec> getImageTypes() {
		return imageTypes;
	}

	public void setImageTypes(List<ResourceSpec> imageTypes) {
		this.imageTypes = imageTypes;
	}

	public void addImageType(ResourceSpec imageType){
		this.imageTypes.add(imageType);
	}
}
