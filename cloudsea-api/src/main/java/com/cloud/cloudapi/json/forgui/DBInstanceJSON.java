package com.cloud.cloudapi.json.forgui;

import com.cloud.cloudapi.pojo.openstackapi.forgui.DBInstance;

public class DBInstanceJSON {

	private DBInstance instance;
	

	public DBInstance getInstance() {
		return instance;
	}


	public void setInstance(DBInstance instance) {
		this.instance = instance;
	}


	public DBInstanceJSON(DBInstance instance){
		this.instance = instance;
	}
	
	public DBInstanceJSON(){
		
	}
}
