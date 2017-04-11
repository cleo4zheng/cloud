package com.cloud.cloudapi.pojo.rating;

import java.util.List;

public class TemplateVersion {
	private String version_id;
	private String name;
	private String description;
	private String ccy_id;
	private String template_id;
	private List<TemplateService> services;
	private Currency currency;
	
	public String getVersion_id() {
		return version_id;
	}

	public void setVersion_id(String version_id) {
		this.version_id = version_id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCcy_id() {
		return ccy_id;
	}

	public void setCcy_id(String ccy_id) {
		this.ccy_id = ccy_id;
	}

	public String getTemplate_id() {
		return template_id;
	}

	public void setTemplate_id(String template_id) {
		this.template_id = template_id;
	}

	public List<TemplateService> getServices() {
		return services;
	}

	public void setServices(List<TemplateService> services) {
		this.services = services;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}
	
}
