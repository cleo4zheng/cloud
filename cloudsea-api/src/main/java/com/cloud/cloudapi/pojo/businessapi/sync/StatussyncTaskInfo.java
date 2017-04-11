package com.cloud.cloudapi.pojo.businessapi.sync;

import java.util.Date;

/** 
* @author  wangw
* @create  2016年8月23日 下午1:41:09 
* 
*/
public class StatussyncTaskInfo {
	
	private String id;
	private String resourceUuid;
	private String resourceBeginStatus;
	private String resourceTargetStatus;
	private String resourceEndStatus;
	private String resourceOsUrl;
	private String resourceOsKey;
	private String resourceDbTable;
	private String resourceDbColumn;
	private String syncTaskStatus;
	private Date createdAt;
	private Date updatedAt;
	
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getResourceUuid() {
		return resourceUuid;
	}
	public void setResourceUuid(String resourceUuid) {
		this.resourceUuid = resourceUuid;
	}
	public String getResourceBeginStatus() {
		return resourceBeginStatus;
	}
	public void setResourceBeginStatus(String resourceBeginStatus) {
		this.resourceBeginStatus = resourceBeginStatus;
	}
	public String getResourceTargetStatus() {
		return resourceTargetStatus;
	}
	public void setResourceTargetStatus(String resourceTargetStatus) {
		this.resourceTargetStatus = resourceTargetStatus;
	}
	public String getResourceEndStatus() {
		return resourceEndStatus;
	}
	public void setResourceEndStatus(String resourceEndStatus) {
		this.resourceEndStatus = resourceEndStatus;
	}
	public String getResourceOsUrl() {
		return resourceOsUrl;
	}
	public void setResourceOsUrl(String resourceOsUrl) {
		this.resourceOsUrl = resourceOsUrl;
	}
	public String getResourceOsKey() {
		return resourceOsKey;
	}
	public void setResourceOsKey(String resourceOsKey) {
		this.resourceOsKey = resourceOsKey;
	}
	public String getResourceDbTable() {
		return resourceDbTable;
	}
	public void setResourceDbTable(String resourceDbTable) {
		this.resourceDbTable = resourceDbTable;
	}
	public String getResourceDbColumn() {
		return resourceDbColumn;
	}
	public void setResourceDbColumn(String resourceDbColumn) {
		this.resourceDbColumn = resourceDbColumn;
	}
	public String getSyncTaskStatus() {
		return syncTaskStatus;
	}
	public void setSyncTaskStatus(String syncTaskStatus) {
		this.syncTaskStatus = syncTaskStatus;
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
	
	
	@Override
	public String toString(){
		
		StringBuilder sb =  new StringBuilder();
		sb.append("id:").append(id).append(",resourceUuid:").append(resourceUuid).append(",resourceBeginStatus:").append(resourceBeginStatus)
		.append(",resourceTargetStatus:").append(resourceTargetStatus).append(",resourceEndStatus:").append(resourceEndStatus).append(",resourceOsUrl:").append(resourceOsUrl)
		.append(",resourceOsKey:").append(resourceOsKey).append(",resourceDbTable:").append(resourceDbTable).append(",resourceDbColumn:").append(resourceDbColumn)
		.append(",syncTaskStatus:").append(syncTaskStatus).append(",createdAt:").append(createdAt).append(",updatedAt:").append(updatedAt);
		
		return sb.toString();
		
	}

}
