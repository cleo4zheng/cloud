package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;

public class DBInstance extends Instance {

	private List<Database> databases;
	private List<DBUser> users;
	private Volume volume;
	private String flavorRef;
    private String flavorId;
    private String hostname;
    private String dataStoreVersion;
    private String dataStoreType;
    private int dataVolumeSize;
    private String dataVolumeType;
    private int systemVolumeSize;
    private String systemVolumeType;
    private RequestDatastore datastore;
  //  private Datastore datastore;
    
	public List<Database> getDatabases() {
		return databases;
	}

	public void setDatabases(List<Database> databases) {
		this.databases = databases;
	}

	public List<DBUser> getUsers() {
		return users;
	}

	public void setUsers(List<DBUser> users) {
		this.users = users;
	}

	public String getFlavorRef() {
		return flavorRef;
	}

	public void setFlavorRef(String flavorRef) {
		this.flavorRef = flavorRef;
	}

	public Volume getVolume() {
		return volume;
	}

	public void setVolume(Volume volume) {
		this.volume = volume;
	}

	public String getFlavorId() {
		return flavorId;
	}

	public void setFlavorId(String flavorId) {
		this.flavorId = flavorId;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getDataStoreVersion() {
		return dataStoreVersion;
	}

	public void setDataStoreVersion(String dataStoreVersion) {
		this.dataStoreVersion = dataStoreVersion;
	}

	public String getDataStoreType() {
		return dataStoreType;
	}

	public void setDataStoreType(String dataStoreType) {
		this.dataStoreType = dataStoreType;
	}

	public RequestDatastore getDatastore() {
		return datastore;
	}

	public void setDatastore(RequestDatastore datastore) {
		this.datastore = datastore;
	}

	public int getDataVolumeSize() {
		return dataVolumeSize;
	}

	public void setDataVolumeSize(int dataVolumeSize) {
		this.dataVolumeSize = dataVolumeSize;
	}

	public String getDataVolumeType() {
		return dataVolumeType;
	}

	public void setDataVolumeType(String dataVolumeType) {
		this.dataVolumeType = dataVolumeType;
	}

	public int getSystemVolumeSize() {
		return systemVolumeSize;
	}

	public void setSystemVolumeSize(int systemVolumeSize) {
		this.systemVolumeSize = systemVolumeSize;
	}

	public String getSystemVolumeType() {
		return systemVolumeType;
	}

	public void setSystemVolumeType(String systemVolumeType) {
		this.systemVolumeType = systemVolumeType;
	}

	
	
//	public Datastore getDatastore() {
//		return datastore;
//	}
//
//	public void setDatastore(Datastore datastore) {
//		this.datastore = datastore;
//	}
	
}
