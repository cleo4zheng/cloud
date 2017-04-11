package com.cloud.cloudapi.pojo.rating;

public class TemplateField {

	private String field_id;
	private String field_code;
	private String default_chargekey;
	private String name;
	private String service_id;
	private Long millionSeconds;
	private Integer max;
	private Boolean rating;
	
	public String getField_id() {
		return field_id;
	}

	public void setField_id(String field_id) {
		this.field_id = field_id;
	}

	public String getField_code() {
		return field_code;
	}

	public void setField_code(String field_code) {
		this.field_code = field_code;
	}

	public String getDefault_chargekey() {
		return default_chargekey;
	}

	public void setDefault_chargekey(String default_chargekey) {
		this.default_chargekey = default_chargekey;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getService_id() {
		return service_id;
	}

	public void setService_id(String service_id) {
		this.service_id = service_id;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	public Boolean getRating() {
		return rating;
	}

	public void setRating(Boolean rating) {
		this.rating = rating;
	}

	public Integer getMax() {
		return max;
	}

	public void setMax(Integer max) {
		this.max = max;
	}

	public void normalInfo(){
		this.setDefault_chargekey(null);
		this.setMillionSeconds(null);
		this.setField_code(null);
		this.setRating(null);
	}
}
