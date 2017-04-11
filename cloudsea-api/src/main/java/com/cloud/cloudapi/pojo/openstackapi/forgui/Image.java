package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.util.StringHelper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

//只序列化非NULL的值
@JsonInclude(value = Include.NON_NULL)
public class Image {
	private String id;
	private String baseImageId;
	private String name;
	private String status;
	private List<String> tags;
	private String createdAt;
	private String updatedAt;
	private String visibility;
	private Boolean isProtected;
	private String file;
	private String owner;
	private String diskFormat;
	private Integer minDisk;
	private Long size;
	private Integer minRam;
	private String instanceId;
	private String systemName;
	private Double unitPrice;
	private Boolean privateFlag;
	private String tenantId;
	private String type;
	private Boolean forInstance;
	private Boolean rating;
	private String systemType;
	private String objectType;
	private Long millionSeconds;
 //   private boolean snapshotFlag;
    
	public Image() {
		this.id = new String();
		this.name = new String();
		this.status = new String();
		this.instanceId = new String();
		this.createdAt = new String();
	}

	public Image(String id, String name) {
		this.id = id;
		this.name = name;
		this.status = new String();
		this.instanceId = new String();
		this.createdAt = new String();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSystemName() {
		return systemName;
	}

	public void setSystemName(String systemName) {
		this.systemName = systemName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(String updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getVisibility() {
		return visibility;
	}

	public void setVisibility(String visibility) {
		this.visibility = visibility;
	}

	public Boolean getIsProtected() {
		return isProtected;
	}

	public void setIsProtected(Boolean isProtected) {
		this.isProtected = isProtected;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getDiskFormat() {
		return diskFormat;
	}

	public void setDiskFormat(String diskFormat) {
		this.diskFormat = diskFormat;
	}

	public Integer getMinDisk() {
		return minDisk;
	}

	public void setMinDisk(Integer minDisk) {
		this.minDisk = minDisk;
	}

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	public Integer getMinRam() {
		return minRam;
	}

	public void setMinRam(Integer minRam) {
		this.minRam = minRam;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

//	public boolean getSnapshotFlag() {
//		return snapshotFlag;
//	}
//
//	public void setSnapshotFlag(boolean snapshotFlag) {
//		this.snapshotFlag = snapshotFlag;
//	}

	public Boolean getPrivateFlag() {
		return privateFlag;
	}

	public void setPrivateFlag(Boolean privateFlag) {
		this.privateFlag = privateFlag;
	}
	
	public Double getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(Double unitPrice) {
		this.unitPrice = unitPrice;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Boolean getForInstance() {
		return forInstance;
	}

	public void setForInstance(Boolean forInstance) {
		this.forInstance = forInstance;
	}

	public String getSystemType() {
		return systemType;
	}

	public void setSystemType(String systemType) {
		this.systemType = systemType;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	public String getBaseImageId() {
		return baseImageId;
	}

	public void setBaseImageId(String baseImageId) {
		this.baseImageId = baseImageId;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}
	
	public Boolean getRating() {
		return rating;
	}

	public void setRating(Boolean rating) {
		this.rating = rating;
	}

	public void normalInfo(){
		this.setName(StringHelper.ncr2String(this.getName()));
		this.setBaseImageId(null);
		this.setDiskFormat(null);
		this.setFile(null);
		this.setForInstance(null);
		this.setIsProtected(null);
		this.setMinDisk(null);
		this.setMinRam(null);
		this.setOwner(null);
		this.setPrivateFlag(null);
		this.setTags(null);
		this.setTenantId(null);
		this.setUnitPrice(null);
		this.setUpdatedAt(null);
		this.setVisibility(null);
		this.setInstanceId(null);
		this.setRating(null);
		if(null != this.getMillionSeconds()){
			this.setCreatedAt(Util.millionSecond2Date(this.getMillionSeconds()));
			this.setMillionSeconds(null);	
		}
	}
}