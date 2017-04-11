package com.cloud.cloudapi.pojo.common;

public class TenantEndpoint {
	
	private String id;
	private String ostenantid;
	private String serviceType;
	private String serviceName;
	private String publicUrl;
	private String internalUrl;
	private String adminUrl;
	private String belongRegion;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOstenantid() {
		return ostenantid;
	}

	public void setOstenantid(String ostenantid) {
		this.ostenantid = ostenantid;
	}

	public String getServiceType() {
		return serviceType;
	}

	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getPublicUrl() {
		return publicUrl;
	}

	public void setPublicUrl(String publicUrl) {
		this.publicUrl = publicUrl;
	}

	public String getInternalUrl() {
		return internalUrl;
	}

	public void setInternalUrl(String internalUrl) {
		this.internalUrl = internalUrl;
	}

	public String getAdminUrl() {
		return adminUrl;
	}

	public void setAdminUrl(String adminUrl) {
		this.adminUrl = adminUrl;
	}

	public String getBelongRegion() {
		return belongRegion;
	}

	public void setBelongRegion(String belongRegion) {
		this.belongRegion = belongRegion;
	}

}
