package com.cloud.cloudapi.pojo.rating;

public class TemplateTenantMapping {

	private String tenant_mapping_id;
	private String tenant_id;
	private Float tenant_discount;
	private Boolean usethreshold;
	private String field_id;
	private String service_id;
	private String version_id;
	private String template_id;
	private Long millionSeconds;

	public String getTenant_mapping_id() {
		return tenant_mapping_id;
	}

	public void setTenant_mapping_id(String tenant_mapping_id) {
		this.tenant_mapping_id = tenant_mapping_id;
	}

	public String getTenant_id() {
		return tenant_id;
	}

	public void setTenant_id(String tenant_id) {
		this.tenant_id = tenant_id;
	}

	public Float getTenant_discount() {
		return tenant_discount;
	}

	public void setTenant_discount(Float tenant_discount) {
		this.tenant_discount = tenant_discount;
	}

	public Boolean getUsethreshold() {
		return usethreshold;
	}

	public void setUsethreshold(Boolean usethreshold) {
		this.usethreshold = usethreshold;
	}

	public String getField_id() {
		return field_id;
	}

	public void setField_id(String field_id) {
		this.field_id = field_id;
	}

	public String getService_id() {
		return service_id;
	}

	public void setService_id(String service_id) {
		this.service_id = service_id;
	}

	public String getVersion_id() {
		return version_id;
	}

	public void setVersion_id(String version_id) {
		this.version_id = version_id;
	}

	public String getTemplate_id() {
		return template_id;
	}

	public void setTemplate_id(String template_id) {
		this.template_id = template_id;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

}
