package com.cloud.cloudapi.pojo.common;

public final class TokenOsEndPoint {

	private String region;
	private String adminURL;
	private String internalURL;
	private String publicURL;

	public TokenOsEndPoint(String region, String adminURL, String internalURL, String publicURL) {
		super();
		this.region = region;
		this.adminURL = adminURL;
		this.internalURL = internalURL;
		this.publicURL = publicURL;
	}

	public TokenOsEndPoint(){
		//nothing
	}
	
	public String getRegion() {
		return region;
	}
	
	public void setRegion(String region) {
		this.region = region;
	}
	
	public String getAdminURL() {
		return adminURL;
	}
	
	public void setAdminURL(String adminURL) {
		this.adminURL = adminURL;
	}
	
	public String getInternalURL() {
		return internalURL;
	}
	
	public void setInternalURL(String internalURL) {
		this.internalURL = internalURL;
	}
	
	public String getPublicURL() {
		return publicURL;
	}
	
	public void setPublicURL(String publicURL) {
		this.publicURL = publicURL;
	}

}
