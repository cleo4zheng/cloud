package com.cloud.cloudapi.pojo.quota;

import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Tenant;
import com.cloud.cloudapi.pojo.rating.TemplateField;

public class QuotaTemplate {
	private String id;
	private String name;
	private Boolean defaultFlag;
	private String description;
	private Long millionSeconds;
	private String createdAt;
	private List<TemplateField> computeFields;
	private List<TemplateField> storageFields;
	private List<TemplateField> networkFields;
	
	private List<Tenant> tenants;
	
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<TemplateField> getComputeFields() {
		return computeFields;
	}

	public void setComputeFields(List<TemplateField> computeFields) {
		this.computeFields = computeFields;
	}

	public List<TemplateField> getStorageFields() {
		return storageFields;
	}

	public void setStorageFields(List<TemplateField> storageFields) {
		this.storageFields = storageFields;
	}

	public List<TemplateField> getNetworkFields() {
		return networkFields;
	}

	public void setNetworkFields(List<TemplateField> networkFields) {
		this.networkFields = networkFields;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public Boolean getDefaultFlag() {
		return defaultFlag;
	}

	public void setDefaultFlag(Boolean defaultFlag) {
		this.defaultFlag = defaultFlag;
	}
	
	public List<Tenant> getTenants() {
		return tenants;
	}

	public void setTenants(List<Tenant> tenants) {
		this.tenants = tenants;
	}

	public void normalInfo(){
		if(null != this.getMillionSeconds()){
			this.setCreatedAt(Util.millionSecond2Date(this.getMillionSeconds()));
			this.setMillionSeconds(null);
		}
		if(null != this.computeFields){
			for(TemplateField field : this.computeFields)
               field.normalInfo();
		}
		if(null != this.storageFields){
			for(TemplateField field : this.storageFields)
				field.normalInfo();
		}
		if(null != this.networkFields){
			for(TemplateField field : this.networkFields)
				field.normalInfo();
		}

		if(null != this.tenants){
			for(Tenant tenant : this.tenants)
				tenant.normalInfo();
		}
	}
}
