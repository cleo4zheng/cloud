package com.cloud.cloudapi.pojo.common;

public class DomainTenantUser {
	
	private String osdomainid;
	
	private String ostenantid;
	
	private String clouduserid;
	
	private String id;

	public DomainTenantUser(){
		
	}
	
	public DomainTenantUser(String osdomainid, String ostenantid, String clouduserid) {
		super();
		this.osdomainid = osdomainid;
		this.ostenantid = ostenantid;
		this.clouduserid = clouduserid;
	}

	public String getOsdomainid() {
		return osdomainid;
	}

	public void setOsdomainid(String osdomainid) {
		this.osdomainid = osdomainid;
	}

	public String getOstenantid() {
		return ostenantid;
	}

	public void setOstenantid(String ostenantid) {
		this.ostenantid = ostenantid;
	}

	public String getClouduserid() {
		return clouduserid;
	}

	public void setClouduserid(String clouduserid) {
		this.clouduserid = clouduserid;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
