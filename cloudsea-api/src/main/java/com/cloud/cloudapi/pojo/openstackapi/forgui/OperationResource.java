package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class OperationResource {

	private String id;
	private String name;
	private String type;
	private String operationId;

	public OperationResource(){}
	
	public OperationResource(String id, String name, String type,String operationId){
		this.id = id;
		this.name = name;
		this.type = type;
		this.operationId = operationId;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getOperationId() {
		return operationId;
	}

	public void setOperationId(String operationId) {
		this.operationId = operationId;
	}

}
