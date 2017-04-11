package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.List;

//this is for gui
public class Datastore {
	private List<String> version;
	private String type;

	public Datastore() {
		super();
		this.version = new ArrayList<String>();
		this.type = "";
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<String> getVersion() {
		return version;
	}

	public void setVersion(List<String> version) {
		this.version = version;
	}


}
