package com.cloud.cloudapi.pojo.rating;

import java.util.ArrayList;
import java.util.List;

public class TemplateService {

	private String service_id;
	private String service_code;
	private String name;
	private Long millionSeconds;
	
	private List<TemplateField> fields;
	private List<TemplateFieldRating> fieldratings;
	
	public String getService_id() {
		return service_id;
	}

	public void setService_id(String service_id) {
		this.service_id = service_id;
	}

	public String getService_code() {
		return service_code;
	}

	public void setService_code(String service_code) {
		this.service_code = service_code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<TemplateField> getFields() {
		return fields;
	}

	public void setFields(List<TemplateField> fields) {
		this.fields = fields;
	}

	
	public List<TemplateFieldRating> getFieldratings() {
		return fieldratings;
	}

	public void setFieldratings(List<TemplateFieldRating> fieldratings) {
		this.fieldratings = fieldratings;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}
	
	public void addFieldratings(TemplateFieldRating fieldRating){
		if(null == this.fieldratings)
			this.fieldratings = new ArrayList<TemplateFieldRating>();
		this.fieldratings.add(fieldRating);
	}
	
}
