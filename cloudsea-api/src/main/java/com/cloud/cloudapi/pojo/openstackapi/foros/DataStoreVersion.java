package com.cloud.cloudapi.pojo.openstackapi.foros;


//this is foros and cloud_api db
public class DataStoreVersion {
	
	private String version_id;
	private String datastore_id;
	private String version_name;
	private String datastore_name;
	private String type;
	
	
	public String getVersion_id() {
		return version_id;
	}
	public void setVersion_id(String version_id) {
		this.version_id = version_id;
	}
	public String getDatastore_id() {
		return datastore_id;
	}
	public void setDatastore_id(String datastore_id) {
		this.datastore_id = datastore_id;
	}
	public String getVersion_name() {
		return version_name;
	}
	public void setVersion_name(String version_name) {
		this.version_name = version_name;
	}
	public String getDatastore_name() {
		return datastore_name;
	}
	public void setDatastore_name(String datastore_name) {
		this.datastore_name = datastore_name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	

}
