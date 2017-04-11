package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class Flavor {

	private String id;
	private String name;
	private String extra;
	private Integer ram;
	private Integer vcpus;
	private Integer disk;
	private Integer swap;
	private Float rxtx_factor;
    private String createdAt;
    private String type;
   // "OS-FLV-EXT-DATA:ephemeral": ephemeral,
   // "os-flavor-access:is_public": is_public,
    
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
	
	public void setRam(Integer ram){
		this.ram = ram;
	}
	
	public Integer getRam(){
		return this.ram;
	}
	
	public void setVcpus(Integer vcpus){
		this.vcpus = vcpus;
	}
	
	public Integer getVcpus(){
		return this.vcpus;
	}
	
	public void setDisk(Integer disk){
		this.disk = disk;
	}
	
	public Integer getDisk(){
		return this.disk;
	}
	
	public void setSwap(Integer swap){
	    this.swap = swap;
	}
	
	public Integer getSwap(){
		return this.swap;
	}
	
	public void setRxtx_factor(Float rxtx_factor){
		this.rxtx_factor = rxtx_factor;
	}
	
	public Float getRxtx_factor(){
		return this.rxtx_factor;
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public void normalInfo(){
		this.setCreatedAt(null);
		this.setDisk(null);
		this.setExtra(null);
		this.setRam(null);
		this.setRxtx_factor(null);
		this.setSwap(null);
		this.setVcpus(null);
		this.setType(null);
	}
}
