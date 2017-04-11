package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class QuotaDetail {
	private String id;
	private String fieldId;
	private String tenantId;
	private String type;
	private String typeName;
//	private String name;
	private String unit;
	private Integer used;
	private Integer total;
	// private Integer limit;
	private Integer reserved;
	private Boolean notDisplay;
	
	// private int in_use;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFieldId() {
		return fieldId;
	}

	public void setFieldId(String fieldId) {
		this.fieldId = fieldId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
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

	public Integer getUsed() {
		return used == null ? 0 : used;
	}

	public void setUsed(Integer used) {
		this.used = used;
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

	public Boolean getNotDisplay() {
		return notDisplay;
	}

	public void setNotDisplay(Boolean notDisplay) {
		this.notDisplay = notDisplay;
	}

//	public String getName() {
//		return name;
//	}
//
//	public void setName(String name) {
//		this.name = name;
//	}
}
