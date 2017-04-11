package com.cloud.cloudapi.pojo.common;

public final class TokenOsEndPointV3 {

	private String region;
	private String urlType;
	private String url;
		
	public TokenOsEndPointV3(){
		//nothing
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getUrlType() {
		return urlType;
	}

	public void setUrlType(String urlType) {
		this.urlType = urlType;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
   
	
}
