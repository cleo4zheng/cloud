package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;

public class DBUser {

	List<Database> databases;
	private String name;
	private String password;
    private String id;
    private String instanceId;
    //host that can access the database with user, % means everywhere
    private String host;
    private String granteddatabases;
    
    
	public List<Database> getDatabases() {
		return databases;
	}

	public void setDatabases(List<Database> databases) {
		this.databases = databases;
	}
	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getGranteddatabases() {
		return granteddatabases;
	}

	public void setGranteddatabases(String granteddatabases) {
		this.granteddatabases = granteddatabases;
	}

}
