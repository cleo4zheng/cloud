package com.cloud.cloudapi.pojo.openstackapi.forgui;

import com.cloud.cloudapi.util.DateHelper;

public class MonitorHistory {
	private String id;
	private String monitorObjId;
	private long dataTime;
	private String dataValue;
	private String dataType;
	private String dataUnit;
	private String createAt;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMonitorObjId() {
		return monitorObjId;
	}

	public void setMonitorObjId(String monitorObjId) {
		this.monitorObjId = monitorObjId;
	}

	public long getDataTime() {
		return dataTime;
	}

	public void setDataTime(long dataTime) {
		this.dataTime = dataTime;
	}

	public String getDataValue() {
		return dataValue;
	}

	public void setDataValue(String dataValue) {
		this.dataValue = dataValue;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	public String getCreateAt() {
		return createAt;
	}

	public void setCreateAt(String createAt) {
		this.createAt = createAt;
	}

	public String getDataUnit() {
		return dataUnit;
	}

	public void setDataUnit(String dataUnit) {
		this.dataUnit = dataUnit;
	}
	
	@Override
	public String toString(){
		String rs = "";
		rs = rs + this.getDataType() + ", ";
		rs = rs + this.getDataValue() + ", ";
		rs = rs + DateHelper.longToStr(this.getDataTime() * 1000);
		return rs;
	}

}
