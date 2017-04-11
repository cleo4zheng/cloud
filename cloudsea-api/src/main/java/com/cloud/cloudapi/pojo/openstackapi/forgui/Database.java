package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class Database {

	private String character_set;
	private String collate;
	private String name;
    private String id;
    private String instanceId;
    
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

	public String getCharacter_set() {
		return character_set;
	}

	public void setCharacter_set(String character_set) {
		this.character_set = character_set;
	}

	public String getCollate() {
		return collate;
	}

	public void setCollate(String collate) {
		this.collate = collate;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	
}
