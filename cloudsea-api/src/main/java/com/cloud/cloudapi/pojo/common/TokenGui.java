package com.cloud.cloudapi.pojo.common;

import java.util.List;

public final class TokenGui {
	
	/**********base table*************/
	private String tokenid;
	private long createTime;
	private long expiresTime;
	private String tenantuserid;
	private String currentRegion;
	
	/************fuction flieds***************************/
	private String tenantid;
	private String tenantname;
	private String domainid;
	private String domainName;
	private String locale;
	private Boolean enableZabbix;
	private Boolean enableWorkflow;
	
	private List<String> projectNames;
	
	public String getTenantid() {
		return tenantid;
	}

	public void setTenantid(String tenantid) {
		this.tenantid = tenantid;
	}

	public String getTenantuserid() {
		return tenantuserid;
	}

	public void setTenantuserid(String tenantuserid) {
		this.tenantuserid = tenantuserid;
	}

	public String getDomainid() {
		return domainid;
	}

	public void setDomainid(String domainid) {
		this.domainid = domainid;
	}

	public String getCurrentRegion() {
		return currentRegion;
	}

	public void setCurrentRegion(String currentRegion) {
		this.currentRegion = currentRegion;
	}

	public String getTokenid() {
		return tokenid;
	}

	public void setTokenid(String tokenid) {
		this.tokenid = tokenid;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public long getExpiresTime() {
		return expiresTime;
	}

	public void setExpiresTime(long expiresTime) {
		this.expiresTime = expiresTime;
	}

	public String getTenantname() {
		return tenantname;
	}

	public void setTenantname(String tenantname) {
		this.tenantname = tenantname;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public Boolean getEnableZabbix() {
		return enableZabbix;
	}

	public void setEnableZabbix(Boolean enableZabbix) {
		this.enableZabbix = enableZabbix;
	}

	public Boolean getEnableWorkflow() {
		return enableWorkflow;
	}

	public void setEnableWorkflow(Boolean enableWorkflow) {
		this.enableWorkflow = enableWorkflow;
	}

	public List<String> getProjectNames() {
		return projectNames;
	}

	public void setProjectNames(List<String> projectNames) {
		this.projectNames = projectNames;
	}

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

}
