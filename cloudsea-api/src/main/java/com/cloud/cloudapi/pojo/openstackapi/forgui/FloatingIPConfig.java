package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;

public class FloatingIPConfig {
	private String id;
	private String unit;
	private Range range;
	private List<ResourceSpec> types;

	public FloatingIPConfig(){
		this.id = Util.makeUUID();
		this.types = new ArrayList<ResourceSpec>();
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public Range getRange() {
		return range;
	}

	public void setRange(Range range) {
		this.range = range;
	}

	public List<ResourceSpec> getTypes() {
		return types;
	}

	public void setTypes(List<ResourceSpec> types) {
		this.types = types;
	}

	public void addResource(String id,String name,Double unitPrice){
		ResourceSpec resource = new ResourceSpec(id,name,unitPrice);
//		resource.setName(name);
//		resource.setUnitPrice(unitPrice);
		this.types.add(resource);
	}
}
