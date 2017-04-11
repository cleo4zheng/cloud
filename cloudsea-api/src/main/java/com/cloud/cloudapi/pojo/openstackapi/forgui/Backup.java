package com.cloud.cloudapi.pojo.openstackapi.forgui;

import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.util.StringHelper;

public class Backup {
	private String id;
	private String name;
	private String status;
	private Integer size;
	private String createdAt;
	private String availabilityZone;
	private String failReason;
	private String container;
	private Integer objectCount;
	private Boolean incremental ;
	private Boolean hasDependentBackups;
	private String  volume_id;
	private String  tenantId;
	private String  volume_type;
	private Boolean force;
	private Long millionSeconds;
	private Volume  volume;
	
    public String getAvailabilityZone() {
		return availabilityZone;
	}

	public void setAvailabilityZone(String availabilityZone) {
		this.availabilityZone = availabilityZone;
	}

	public String getFailReason() {
		return failReason;
	}

	public void setFailReason(String failReason) {
		this.failReason = failReason;
	}

	public String getContainer() {
		return container;
	}

	public void setContainer(String container) {
		this.container = container;
	}

	public Integer getObjectCount() {
		return objectCount;
	}

	public void setObjectCount(Integer objectCount) {
		this.objectCount = objectCount;
	}

	public Boolean getIncremental() {
		return incremental;
	}

	public void setIncremental(Boolean incremental) {
		this.incremental = incremental;
	}

	public Boolean getHasDependentBackups() {
		return hasDependentBackups;
	}

	public void setHasDependentBackups(Boolean hasDependentBackups) {
		this.hasDependentBackups = hasDependentBackups;
	}

	public String getVolume_id() {
		return volume_id;
	}

	public void setVolume_id(String volume_id) {
		this.volume_id = volume_id;
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public Volume getVolume() {
		return volume;
	}

	public void setVolume(Volume volume) {
		this.volume = volume;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	public Boolean getForce() {
		return force;
	}

	public void setForce(Boolean force) {
		this.force = force;
	}

	public String getVolume_type() {
		return volume_type;
	}

	public void setVolume_type(String volume_type) {
		this.volume_type = volume_type;
	}

	public void normalInfo(Boolean normalVolume){
		this.setName(StringHelper.ncr2String(this.getName()));
		this.setVolume_type(null);
		this.setAvailabilityZone(null);
		this.setContainer(null);
		this.setFailReason(null);
		this.setForce(null);
		this.setHasDependentBackups(null);
		this.setIncremental(null);
		this.setObjectCount(null);
		this.setTenantId(null);
		this.setVolume_id(null);
		if(true == normalVolume)
			this.setVolume(null);
		if(null != this.getMillionSeconds()){
			this.setCreatedAt(Util.millionSecond2Date(this.getMillionSeconds()));
			this.setMillionSeconds(null);
		}
	}
}
