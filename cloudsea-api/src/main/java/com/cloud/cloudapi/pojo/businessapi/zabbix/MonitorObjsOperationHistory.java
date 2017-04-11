package com.cloud.cloudapi.pojo.businessapi.zabbix;

import java.util.Date;

public class MonitorObjsOperationHistory {
	private int id;
	private String monitorObjId;
	private String monitorId;
	private String monitorType; //instance, bareMetal, vdiInstance, service
	private String operationType; // add,delete
	private int operationResult; // default -1 (db default) , execute success 1, faild 0;
	private Date createdAt;
	private Date updatedAt;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getMonitorObjId() {
		return monitorObjId;
	}

	public void setMonitorObjId(String monitorObjId) {
		this.monitorObjId = monitorObjId;
	}

	public String getMonitorId() {
		return monitorId;
	}

	public void setMonitorId(String monitorId) {
		this.monitorId = monitorId;
	}

	public String getOperationType() {
		return operationType;
	}

	public void setOperationType(String operationType) {
		this.operationType = operationType;
	}

	public int getOperationResult() {
		return operationResult;
	}

	public void setOperationResult(int operationResult) {
		this.operationResult = operationResult;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getMonitorType() {
		return monitorType;
	}

	public void setMonitorType(String monitorType) {
		this.monitorType = monitorType;
	}

}
