package com.cloud.cloudapi.pojo.openstackapi.forgui;

//只序列化非NULL的值
//@JsonInclude(value = Include.NON_NULL)
public class MonitorRule {
	private String id;
	private String item;
	private String period;
	private String condition; 
	private String threshold;
	private String unit;
	private String createdAt;
    
	public MonitorRule() {
		this.id = new String();
		this.item = new String();
		this.period = new String();
		this.condition = new String();
		this.threshold = new String();;
		this.unit = new String();
		this.createdAt = new String();
	}


	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	public String getItem() {
		return item;
	}


	public void setItem(String item) {
		this.item = item;
	}


	public String getPeriod() {
		return period;
	}


	public void setPeriod(String period) {
		this.period = period;
	}


	public String getCondition() {
		return condition;
	}


	public void setCondition(String condition) {
		this.condition = condition;
	}


	public String getThreshold() {
		return threshold;
	}


	public void setThreshold(String threshold) {
		this.threshold = threshold;
	}


	public String getUnit() {
		return unit;
	}


	public void setUnit(String unit) {
		this.unit = unit;
	}


	public String getCreatedAt() {
		return createdAt;
	}


	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}


}