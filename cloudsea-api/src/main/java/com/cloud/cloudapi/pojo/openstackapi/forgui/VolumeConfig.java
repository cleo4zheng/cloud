package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;

public class VolumeConfig {

	private String id;
	private List<VolumeType> types;
	private Range range;
    private Integer size;
    private Integer windowsSystemVolumeSize;
    private Integer linuxSystemVolumeSize;
    
	public VolumeConfig() {
		this.id = Util.makeUUID();
		this.types = new ArrayList<VolumeType>();
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<VolumeType> getTypes() {
		return types;
	}

	public void setTypes(List<VolumeType> types) {
		this.types = types;
		
	}

	public void addType(VolumeType volumeType) {
		this.types.add(volumeType);
	}

	public void addType(String id,String name,Double unitPrice) {
		VolumeType volumeType = new VolumeType();
		volumeType.setId(id);
		volumeType.setName(name);
		volumeType.setUnitPrice(unitPrice);
		this.types.add(volumeType);
	}
	
	public Double getUnitPriceByType(String name){
		if(Util.isNullOrEmptyList(this.types))
			return 0.0;
		for(VolumeType type : this.types){
			if(name.equals(type.getId()))
				return type.getUnitPrice();
		}
		return 0.0;
	}
	
	public Range getRange() {
		return range;
	}

	public void setRange(Range range) {
		this.range = range;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public Integer getWindowsSystemVolumeSize() {
		return windowsSystemVolumeSize;
	}

	public void setWindowsSystemVolumeSize(Integer windowsSystemVolumeSize) {
		this.windowsSystemVolumeSize = windowsSystemVolumeSize;
	}

	public Integer getLinuxSystemVolumeSize() {
		return linuxSystemVolumeSize;
	}

	public void setLinuxSystemVolumeSize(Integer linuxSystemVolumeSize) {
		this.linuxSystemVolumeSize = linuxSystemVolumeSize;
	}
	
}
