package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.HashMap;
import java.util.Map;

public class DiskType {

	private String id;
	private String name;
	private Map<String,String> extraSpec;
	
	public void setId(String id){
		this.id = id;
	}
	
	public String getId(){
		return this.id;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public String getName(){
		return this.name;
	}
	
	public void addExtraSpec(String key,String value){
		if(null == extraSpec){
			this.extraSpec = new HashMap<String,String>();
		this.extraSpec.put(key, value);
		}
	}
	
	public Map<String,String> getExtraSpec(){
		return this.extraSpec;
	}
}
