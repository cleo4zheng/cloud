package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;
import java.util.Locale;

import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

public class ResourceSpec {

	private String id;
	private String name;
	private Double unitPrice;
	private String type;
	private String typeName;
    private List<Integer> size;
    private Range range;
    private Integer total;
    private Integer used;
    private Integer free;
    
	public List<Integer> getSize() {
		return size;
	}

	public void setSize(List<Integer> size) {
		this.size = size;
	}

	public ResourceSpec(){
		this.id = Util.makeUUID();
	}
	
	public ResourceSpec(String id,String name, Double unitPrice){
		this.id = id;
		this.name = name;
		this.unitPrice = unitPrice;
	}
	
	public ResourceSpec(String name, Double unitPrice){
		this.name = name;
		this.unitPrice = unitPrice;
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

	public Double getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(Double unitPrice) {
		this.unitPrice = unitPrice;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public Range getRange() {
		return range;
	}

	public void setRange(Range range) {
		this.range = range;
	}

	public Integer getTotal() {
		return total;
	}

	public void setTotal(Integer total) {
		this.total = total;
	}

	public Integer getUsed() {
		return used;
	}

	public void setUsed(Integer used) {
		this.used = used;
	}

	public Integer getFree() {
		return free;
	}

	public void setFree(Integer free) {
		this.free = free;
	}
	
	public void normalInfo(Locale locale){

		if(this.type.contains(ParamConstant.CORE)){
//			String coreType = "CS_CORE_TYPE_NAME";
//			String resourceType = this.type.substring(0, this.type.indexOf('_'));
//			String coreTypeName = coreType.replaceFirst("TYPE", resourceType.toUpperCase());
//			this.typeName = Message.getMessage(coreTypeName, locale,false);
		}else if(this.type.contains(ParamConstant.RAM)){
//			String ramType = "CS_RAM_TYPE_NAME";
//			String resourceType = this.type.substring(0, this.type.indexOf('_'));
//			String ramTypeName = ramType.replaceFirst("TYPE", resourceType.toUpperCase());
//			this.typeName = Message.getMessage(ramTypeName, locale,false);
		}else{
			if(null != this.name && this.name.contains(ParamConstant.STORAGE)){
				//String volumeType = "CS_VOLUME_TYPE_NAME";
				//String typeName = volumeType.replaceFirst("TYPE", this.type.toUpperCase());
				//this.typeName = Message.getMessage(typeName, locale,false);
			}else{
				String networkType = "CS_FLOTINGIP_TYPE_NAME";
				String ipTypeName = networkType.replaceFirst("TYPE",type.toUpperCase());
				this.typeName = Message.getMessage(ipTypeName,locale,false);
			}
			
		}
	}
}
