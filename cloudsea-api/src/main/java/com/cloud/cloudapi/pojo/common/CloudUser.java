package com.cloud.cloudapi.pojo.common;

import java.util.List;

import com.cloud.cloudapi.pojo.rating.BillingReport;

public  class CloudUser {
	
	private String userid;
	//登陆账号
	private String account;
	//用户姓名
	private String name;
	private String role;
	private String password;
	private String mail;
	private String phone;
	private String company;
	private String createdAt;
	private String customerManager;
	private Boolean enabled;
	private String currentregion;
	private String locale;
	//private String billingId;
	private String templateId;
	private String version;
	private String tenantId;
	private String parentId;
	private String domainId;
	private String domainName;
	private String unitPriceName;
	private String currentTenantName;
	private String currentTenantId;

	private Long create_time;
	
	/***********os link**************/
	private String osTenantId;
    private String osTenantName;
    private String osDomainId;
	private String osUserId;
	
    private String customerManagerUserid;
    private String customerManagerName;
    private String customerManagerTenantid;
    
	private List<BillingReport> billings;
	/***********os link**************/
//	private String ostenantid;
//	private String osdomainid;
	
//	public CloudUser(){
//		
//	}
//	public CloudUser(String code, String name, int sex, Date birthday, String password, String mail,
//			String phone) {
//		super();
//		this.code = code;
//		this.name = name;
//		this.sex = sex;
//		this.birthday = birthday;
//		this.password = password;
//		this.mail = mail;
//		this.phone = phone;
//	}
//	
//	public CloudUser(String userid, String code, String name, int sex, Date birthday, String password, String mail,
//			String phone, String ostenantid, String osdomainid) {
//		super();
//		this.userid = userid;
//		this.code = code;
//		this.name = name;
//		this.sex = sex;
//		this.birthday = birthday;
//		this.password = password;
//		this.mail = mail;
//		this.phone = phone;
//		this.ostenantid = ostenantid;
//		this.osdomainid = osdomainid;
//	}

	public String getUserid() {
		return userid;
	}

	public Long getCreate_time() {
		return create_time;
	}


	public void setCreate_time(Long create_time) {
		this.create_time = create_time;
	}


	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
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

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

//	public String getOstenantid() {
//		return ostenantid;
//	}
//
//	public void setOstenantid(String ostenantid) {
//		this.ostenantid = ostenantid;
//	}
//
//	public String getOsdomainid() {
//		return osdomainid;
//	}
//
//	public void setOsdomainid(String osdomainid) {
//		this.osdomainid = osdomainid;
//	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getCustomerManager() {
		return customerManager;
	}

	public void setCustomerManager(String customerManager) {
		this.customerManager = customerManager;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getOsTenantId() {
		return osTenantId;
	}

	public void setOsTenantId(String osTenantId) {
		this.osTenantId = osTenantId;
	}

	public String getOsTenantName() {
		return osTenantName;
	}

	public void setOsTenantName(String osTenantName) {
		this.osTenantName = osTenantName;
	}

	public String getOsDomainId() {
		return osDomainId;
	}

	public void setOsDomainId(String osDomainId) {
		this.osDomainId = osDomainId;
	}

	public String getCustomerManagerUserid() {
		return customerManagerUserid;
	}

	public void setCustomerManagerUserid(String customerManagerUserid) {
		this.customerManagerUserid = customerManagerUserid;
	}

	public String getCustomerManagerName() {
		return customerManagerName;
	}

	public void setCustomerManagerName(String customerManagerName) {
		this.customerManagerName = customerManagerName;
	}

	public String getCustomerManagerTenantid() {
		return customerManagerTenantid;
	}

	public void setCustomerManagerTenantid(String customerManagerTenantid) {
		this.customerManagerTenantid = customerManagerTenantid;
	}

	public String getCurrentregion() {
		return currentregion;
	}

	public void setCurrentregion(String currentregion) {
		this.currentregion = currentregion;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

//	public String getBillingId() {
//		return billingId;
//	}
//
//	public void setBillingId(String billingId) {
//		this.billingId = billingId;
//	}

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getUnitPriceName() {
		return unitPriceName;
	}

	public void setUnitPriceName(String unitPriceName) {
		this.unitPriceName = unitPriceName;
	}

	public String getOsUserId() {
		return osUserId;
	}

	public void setOsUserId(String osUserId) {
		this.osUserId = osUserId;
	}

	
	public List<BillingReport> getBillings() {
		return billings;
	}

	public void setBillings(List<BillingReport> billings) {
		this.billings = billings;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getDomainId() {
		return domainId;
	}

	public void setDomainId(String domainId) {
		this.domainId = domainId;
	}

	public String getCurrentTenantName() {
		return currentTenantName;
	}

	public void setCurrentTenantName(String currentTenantName) {
		this.currentTenantName = currentTenantName;
	}

	public String getCurrentTenantId() {
		return currentTenantId;
	}

	public void setCurrentTenantId(String currentTenantId) {
		this.currentTenantId = currentTenantId;
	}

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}
	
	public void normalInfo(){
		this.setOsDomainId(null);
		this.setOsTenantId(null);
		this.setCurrentregion(null);
		this.setOsTenantId(null);
		this.setOsTenantName(null);
		this.setDomainId(null);
		if(null != this.getCreate_time())
			this.setCreatedAt(Util.millionSecond2Date(this.getCreate_time()));
		this.setCreate_time(null);
		this.setLocale(null);
		this.setPassword(null);
		this.setOsUserId(null);
		this.setTenantId(null);
		this.setCurrentTenantName(null);
		this.setCurrentTenantId(null);
		//this.setEnabled(null);
		
		if(null != this.billings){
			for(BillingReport billing : this.billings)
				billing.normalInfo(true);	
		}
	}
}
