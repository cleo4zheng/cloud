package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.util.StringHelper;

public class Volume {
	private String id;
	private String name;
	private String status;
	private Integer size;
	private String createdAt;
	private String instanceId;
	private String volumeId;
	private String device;
	private String backupId;
	private String description;
	private String tenantId;
	private Boolean multiattach;
	private Boolean bootable;
	private String volume_type;
	private String volumeTypeName;
	private Long millionSeconds;
	private List<Instance> instances;
	private List<Backup> backups;
	private List<VolumeSnapshot> snapshots;
	
	public Volume() {
		this.id = new String();
		this.name = new String();
		this.status = new String();
		this.createdAt = new String();
		this.instances = new ArrayList<Instance>();
		this.backups = new ArrayList<Backup>();
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getDevice() {
		return device;
	}

	public void setDevice(String device) {
		this.device = device;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
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

	public String getVolume_type() {
		return volume_type;
	}

	public void setVolume_type(String volume_type) {
		this.volume_type = volume_type;
	}

	public List<Instance> getInstances() {
		return instances;
	}

	public void setInstances(List<Instance> instance) {
		this.instances = instance;
	}

	public void addInstance(Instance instance) {
		this.instances.add(instance);
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getBackupId() {
		return backupId;
	}

	public void setBackupId(String backupId) {
		this.backupId = backupId;
	}

	public List<Backup> getBackups() {
		return backups;
	}

	public void setBackups(List<Backup> backups) {
		this.backups = backups;
	}

	public void addBackup(Backup backup) {
		this.backups.add(backup);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getVolumeId() {
		return volumeId;
	}

	public void setVolumeId(String volumeId) {
		this.volumeId = volumeId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public Boolean getMultiattach() {
		return multiattach;
	}

	public void setMultiattach(Boolean multiattach) {
		this.multiattach = multiattach;
	}

	public Boolean getBootable() {
		return bootable;
	}

	public void setBootable(Boolean bootable) {
		this.bootable = bootable;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	public String getVolumeTypeName() {
		return volumeTypeName;
	}

	public void setVolumeTypeName(String volumeTypeName) {
		this.volumeTypeName = volumeTypeName;
	}
	
	public List<VolumeSnapshot> getSnapshots() {
		return snapshots;
	}

	public void setSnapshots(List<VolumeSnapshot> snapshots) {
		this.snapshots = snapshots;
	}

	public void normalInfo(Boolean normalRelatedResource,Locale locale){
		this.setName(StringHelper.ncr2String(this.getName()));
		this.setDescription(StringHelper.ncr2String(this.getDescription()));
	//	this.setVolumeTypeName(Message.getMessage(this.getVolume_type().toUpperCase(),locale,false));
		this.setBackupId(null);
		//this.setBootable(null);
		this.setDevice(null);
		this.setInstanceId(null);
		this.setMultiattach(null);
		this.setTenantId(null);
		this.setVolumeId(null);
		if(true == normalRelatedResource){
			this.setBackups(null);
			this.setInstances(null);
		}
		if(null != this.snapshots){
			for(VolumeSnapshot snapshot : this.snapshots)
				snapshot.normalInfo(false);
		}
		if(null != this.getMillionSeconds()){
			this.setCreatedAt(Util.millionSecond2Date(this.getMillionSeconds()));
			this.setMillionSeconds(null);
		}
	}
}
