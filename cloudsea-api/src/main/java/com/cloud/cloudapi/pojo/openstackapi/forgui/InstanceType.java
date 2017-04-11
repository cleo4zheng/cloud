package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.util.ParamConstant;

public class InstanceType {

	private String id;
	private String name;
	private ResourceSpec core;
	private ResourceSpec ram;

	public InstanceType(){
		this.id = Util.makeUUID();
	}
	
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

	public ResourceSpec getCore() {
		return core;
	}

	public void setCore(ResourceSpec core) {
		this.core = core;
	}

	public void addCore(String[] coreSize,Double price){
		if(null == coreSize)
			return;
		List<Integer> coreSizeList = new ArrayList<Integer>();
		for(int index = 0; index < coreSize.length; ++index){
			coreSizeList.add(Integer.parseInt(coreSize[index]));
		}
		ResourceSpec core = new ResourceSpec();
		core.setSize(coreSizeList);
		core.setUnitPrice(price);
		this.setCore(core);
	}
	
	public void addRam(String[] ramSize,Double price){
		if(null == ramSize)
			return;
		List<Integer> ramSizeList = new ArrayList<Integer>();
		for(int index = 0; index < ramSize.length; ++index){
			Float value = (Float.valueOf(ramSize[index]))*ParamConstant.MB;
			ramSizeList.add(value.intValue());
		}
		ResourceSpec ram = new ResourceSpec();
		ram.setSize(ramSizeList);
		ram.setUnitPrice(price);
		this.setRam(ram);
	}
	
	public ResourceSpec getRam() {
		return ram;
	}

	public void setRam(ResourceSpec ram) {
		this.ram = ram;
	}

}
