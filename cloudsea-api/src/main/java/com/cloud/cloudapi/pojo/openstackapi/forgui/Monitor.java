package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;


//只序列化非NULL的值
//@JsonInclude(value = Include.NON_NULL)
public class Monitor {
	private String id;
	private String name;
	private String type; // instance / bareMetal / vdiInstance/ service
	private Boolean enable; // true / false
	private String status; // normal /shortage / warning 	
	private String createdAt;
	private String description;
	private String tenantId;
	private Long millionSeconds;
	
	private List<MonitorObj> resources;
	private List<MonitorRule> rules;
	private List<NotificationObj> notificationObjs;
    
	public Monitor() {
		this.id = new String();
		this.name = new String();
		this.status = new String();
		this.type = new String();
		this.createdAt = new String();
		this.enable = false;
		this.description = new String();
		this.resources = null;
		this.notificationObjs = null;
		this.rules = null;
	}

	public void normalInfo(){
		this.setTenantId(null);
		if(null != this.getMillionSeconds()){
			this.setCreatedAt(Util.millionSecond2Date(this.getMillionSeconds()));
			this.setMillionSeconds(null);
		}
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


	public String getType() {
		return type;
	}


	public void setType(String type) {
		this.type = type;
	}


	public Boolean getEnable() {
		return enable;
	}


	public void setEnable(Boolean enable) {
		this.enable = enable;
	}


	public String getStatus() {
		return status;
	}


	public void setStatus(String status) {
		this.status = status;
	}


	public List<MonitorObj> getResources() {
		return resources;
	}


	public void setResources(List<MonitorObj> resources) {
		this.resources = resources;
	}


	public String getCreatedAt() {
		return createdAt;
	}


	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}


	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public String getTenantId() {
		return tenantId;
	}


	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}


	public List<MonitorRule> getRules() {
		return rules;
	}


	public void setRules(List<MonitorRule> rules) {
		this.rules = rules;
	}


	public List<NotificationObj> getNotificationObjs() {
		return notificationObjs;
	}


	public void setNotificationObjs(List<NotificationObj> notificationObjs) {
		this.notificationObjs = notificationObjs;
	}


	public Long getMillionSeconds() {
		return millionSeconds;
	}


	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

}