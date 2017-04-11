package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.Locale;

import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

public class HostDetail {
	private String id;
	private String name;
	private String type;
	private String typeName;
	private String unit;
	private String unitName;
	private String project;
	private Integer total;
	private Integer free;
	private Integer reserved;
	private Integer used;
	
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

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getUnitName() {
		return unitName;
	}

	public void setUnitName(String unitName) {
		this.unitName = unitName;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public Integer getTotal() {
		return total;
	}

	public void setTotal(Integer total) {
		this.total = total;
	}

	public Integer getReserved() {
		return reserved;
	}

	public void setReserved(Integer reserved) {
		this.reserved = reserved;
	}

//	public Integer getResState() {
//		return resState;
//	}
//
//	public void setResState(Integer resState) {
//		this.resState = resState;
//	}

	public Integer getFree() {
		return free;
	}

	public void setFree(Integer free) {
		this.free = free;
	}

	public Integer getUsed() {
		return used;
	}

	public void setUsed(Integer used) {
		this.used = used;
	}
	
	public void normalInfo(Locale locale){
		if(this.type.contains(ParamConstant.CORE)){
			String coreType = "CS_CORE_TYPE_NAME";
			String resourceType = this.type.substring(0, this.type.indexOf('_'));
			String coreTypeName = coreType.replaceFirst("TYPE", resourceType.toUpperCase());
			this.typeName = Message.getMessage(coreTypeName, locale,false);
			this.unitName = Message.getMessage(this.unit,locale,false);
		}
		else if(this.type.contains(ParamConstant.RAM)){
			String ramType = "CS_RAM_TYPE_NAME";
			String resourceType = this.type.substring(0, this.type.indexOf('_'));
			String ramTypeName = ramType.replaceFirst("TYPE", resourceType.toUpperCase());
			this.typeName = Message.getMessage(ramTypeName, locale,false);
			this.unitName = Message.getMessage(this.unit,locale,false);
		}
		else if(this.type.contains(ParamConstant.DISK)){
			this.typeName = Message.getMessage(Message.CS_DISK_NAME,locale,false);
			this.unitName = Message.getMessage(this.unit,locale,false);
		}
		if(null == this.used)
			this.used = 0;
		this.unit = null;
		this.project = null;
		this.reserved = null;
		this.free = null;
	}
}
