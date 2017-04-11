package com.cloud.cloudapi.pojo.quota;

public class QuotaField {
	private String id;
	private String field_id;
	private String service_id;
	private String template_id;
	private String name;
	private Integer used;
	private Integer max;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getField_id() {
		return field_id;
	}

	public void setField_id(String field_id) {
		this.field_id = field_id;
	}

	public String getTemplate_id() {
		return template_id;
	}

	public void setTemplate_id(String template_id) {
		this.template_id = template_id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getUsed() {
		return used;
	}

	public void setUsed(Integer used) {
		this.used = used;
	}

	public Integer getMax() {
		return max;
	}

	public void setMax(Integer max) {
		this.max = max;
	}

	public String getService_id() {
		return service_id;
	}

	public void setService_id(String service_id) {
		this.service_id = service_id;
	}
	
	public void normalInfo(){
		this.setUsed(null);
		this.setService_id(null);
		this.setTemplate_id(null);
		this.setField_id(null);
	}
}
