package com.cloud.cloudapi.pojo.common;

import com.cloud.cloudapi.util.http.RequestUrlHelper;

public class CloudConfig {

	private String os_authurl;
	private String os_authuser;
	private String os_authpwd;
	private String os_authdomainid;
	private String os_authtenantid;
	private String os_defaultregion;
	private String os_utc_timezone;
	// zabbix
	private String zabbix_enabled;
	
	// openstack token有效时长：单位小时
	private int timeout_token_os;
	// 中间层api token有效时长:单位小时
	private int timeout_token_cloudapi;
	// openstack token过期前，提前多久创建token：单位分钟：
	private int time_createtoken_beforehand;

	private String workflow_enabled;
	private String workflow_url;
	private String workflow_deployid;
    private String permission_enabled;
	// system spec and price configuration
	private String systemVolumeSize;
	private String systemWindowsVolumeSize;
	private String systemLinuxVolumeSize;
	private String systemCpuSpec;
	private String systemRamSpec;
	private String systemVdiSpec;
	private String systemBaremetalSpec;
	private String systemDBSpec;
	private String systemContainerSpec;
	private String systemInstanceSpec;
	private String systemInstanceType;
	private String systemVolumeSpec;
	private String systemFloatingSpec;
	private String systemServiceSpec;
	private String systemFloatingNum;
	private String systemCpuRatio;
	private String systemRamRatio;
	private String systemVolumeQuota;
	private String systemFloatingIPQuota;
	private String volumePrice;
	private String corePrice;
	private String ramPrice;
	private String imagePrice;
	private String floatingPrice;
	private String servicePrice;
	private String volumeRange;
	private String floatingRange;
    private String ipRange;
    private String systemMaxTries;
    private String systemWaitTime;
    private String poolVolumeRange;
    private String poolFipRange;
    private String poolCpuRange;
    private String poolRamfipRange;
    private String externalNetworkName;
    private String systemIngoreHosts;
    private String vdiCorePrice;
    private String vdiRamPrice;
    private String baremetalCorePrice;
    private String baremetalRamPrice;
    private String poolVdiCpuRange;
    private String poolVdiRamRange;
    private String poolGeneralCpuRange;
    private String poolGeneralRamRange;
    private String systemVmwareNetworkId;
    private String systemVmwareZone;
    private String systemAdminDefaultPassword;
    private String systemAdminDefaultAccount;
    private String systemAdminDefaultName;
    private String systemAdminDefaultMail;
    private String systemAdminDefaultPhone;
    private String systemAdminDefaultCompany;
    private String systemDefaultLocale;
    private String cloudVMWareUrl;
    //quota
    private String instanceQuota;
    private String coreQuota;
    private String ramQuota;
    private String keypairQuota;
    private String floatingipQuota;
    private String volumeQuota;
    private String vdiCoreQuota;
    private String vdiRamQuota;
    //rating
    private String systemCcy;
    private String systemCcyName;
    private String systemCcyUnit;
    private String systemCcyUnitName;
    private String ratingReportTime;
    private String ratingReportBeginDay;
    private String ratingReportEndDay;
    private String systemDefaultPriceName;
    private String systemDefaultCurrencyName;
    //ironic
    private String depolyKernel;
	private String depolyRamdisk;
    private String systemIronicNetworkId;

    //mail
    private String mailSmtpHost;
    private String mailSmtpStatrttlsEnable;
    private String mailSmtpPort;
    private String mailSmtpAuth;
    private String mailSenderAddress;
    private String mailSenderUsername;
    private String mailSenderPassword;
	// 为开发调试用的Flag:用于解决认证URL变动/或整个换了一个Openstack时
	// DB保存的endpoint更新问题
	// true:在每次发行新的OpentackToken时，DB中的endpoint会更新。
	private boolean endpoint_refresh = false;

	public int getTime_createtoken_beforehand() {
		return time_createtoken_beforehand;
	}

	public void setTime_createtoken_beforehand(int time_createtoken_beforehand) {
		this.time_createtoken_beforehand = time_createtoken_beforehand;
	}

	public CloudConfig() {
		super();
	}

	public CloudConfig(String os_authurl, String os_authuser, String os_authpwd) {
		super();
		this.os_authurl = os_authurl;
		this.os_authuser = os_authuser;
		this.os_authpwd = os_authpwd;
	}

	public CloudConfig(String os_authurl, String os_authuser, String os_authpwd, String os_authdomainid,
			String os_authetenantid) {
		super();
		this.os_authurl = os_authurl;
		this.os_authuser = os_authuser;
		this.os_authpwd = os_authpwd;
		this.os_authdomainid = os_authdomainid;
		this.os_authtenantid = os_authetenantid;
	}

	public boolean isEndpoint_refresh() {
		return endpoint_refresh;
	}

	public void setEndpoint_refresh(boolean endpoint_refresh) {
		this.endpoint_refresh = endpoint_refresh;
	}

	public String getWorkflow_url() {
		return workflow_url;
	}

	public void setWorkflow_url(String workflow_url) {
		this.workflow_url = RequestUrlHelper.checkUrlEnd(workflow_url);
	}

	public String getWorkflow_deployid() {
		return workflow_deployid;
	}

	public void setWorkflow_deployid(String workflow_deployid) {
		this.workflow_deployid = workflow_deployid;
	}

	public String getOs_authurl() {
		return os_authurl;
	}

	public void setOs_authurl(String os_authurl) {
		this.os_authurl = RequestUrlHelper.checkUrlEnd(os_authurl);
	}

	public String getOs_authuser() {
		return os_authuser;
	}

	public void setOs_authuser(String os_authuser) {
		this.os_authuser = os_authuser;
	}

	public String getOs_authpwd() {
		return os_authpwd;
	}

	public void setOs_authpwd(String os_authpwd) {
		this.os_authpwd = os_authpwd;
	}

	public String getOs_authdomainid() {
		return os_authdomainid;
	}

	public void setOs_authdomainid(String os_authdomainid) {
		this.os_authdomainid = os_authdomainid;
	}

	public String getOs_authtenantid() {
		return os_authtenantid;
	}

	public void setOs_authtenantid(String os_authetenantid) {
		this.os_authtenantid = os_authetenantid;
	}

	public String getOs_defaultregion() {
		return os_defaultregion;
	}

	public void setOs_defaultregion(String os_defaultregion) {
		this.os_defaultregion = os_defaultregion;
	}

	public int getTimeout_token_os() {
		return timeout_token_os;
	}

	public void setTimeout_token_os(int timeout_token_os) {
		this.timeout_token_os = timeout_token_os;
	}

	public int getTimeout_token_cloudapi() {
		return timeout_token_cloudapi;
	}

	public void setTimeout_token_cloudapi(int timeout_token_cloudapi) {
		this.timeout_token_cloudapi = timeout_token_cloudapi;
	}

	public String getWorkflow_enabled() {
		return workflow_enabled;
	}

	public void setWorkflow_enabled(String workflow_enabled) {
		this.workflow_enabled = workflow_enabled;
	}

	public String getOs_utc_timezone() {
		return os_utc_timezone;
	}

	public void setOs_utc_timezone(String os_utc_timezone) {
		this.os_utc_timezone = os_utc_timezone;
	}

	public String getSystemVolumeSize() {
		return systemVolumeSize;
	}

	public void setSystemVolumeSize(String systemVolumeSize) {
		this.systemVolumeSize = systemVolumeSize;
	}

	public String getSystemWindowsVolumeSize() {
		return systemWindowsVolumeSize;
	}

	public void setSystemWindowsVolumeSize(String systemWindowsVolumeSize) {
		this.systemWindowsVolumeSize = systemWindowsVolumeSize;
	}

	public String getSystemLinuxVolumeSize() {
		return systemLinuxVolumeSize;
	}

	public void setSystemLinuxVolumeSize(String systemLinuxVolumeSize) {
		this.systemLinuxVolumeSize = systemLinuxVolumeSize;
	}

	public String getSystemCpuSpec() {
		return systemCpuSpec;
	}

	public void setSystemCpuSpec(String systemCpuSpec) {
		this.systemCpuSpec = systemCpuSpec;
	}

	public String getSystemRamSpec() {
		return systemRamSpec;
	}

	public void setSystemRamSpec(String systemRamSpec) {
		this.systemRamSpec = systemRamSpec;
	}

	public String getSystemInstanceSpec() {
		return systemInstanceSpec;
	}

	public void setSystemInstanceSpec(String systemInstanceSpec) {
		this.systemInstanceSpec = systemInstanceSpec;
	}

	public String getSystemInstanceType() {
		return systemInstanceType;
	}

	public void setSystemInstanceType(String systemInstanceType) {
		this.systemInstanceType = systemInstanceType;
	}

	public String getSystemVolumeSpec() {
		return systemVolumeSpec;
	}

	public void setSystemVolumeSpec(String systemVolumeSpec) {
		this.systemVolumeSpec = systemVolumeSpec;
	}

	public String getSystemFloatingSpec() {
		return systemFloatingSpec;
	}

	public void setSystemFloatingSpec(String systemFloatingSpec) {
		this.systemFloatingSpec = systemFloatingSpec;
	}

	public String getSystemFloatingNum() {
		return systemFloatingNum;
	}

	public void setSystemFloatingNum(String systemFloatingNum) {
		this.systemFloatingNum = systemFloatingNum;
	}

	public String getSystemCpuRatio() {
		return systemCpuRatio;
	}

	public void setSystemCpuRatio(String systemCpuRatio) {
		this.systemCpuRatio = systemCpuRatio;
	}

	public String getSystemRamRatio() {
		return systemRamRatio;
	}

	public void setSystemRamRatio(String systemRamRatio) {
		this.systemRamRatio = systemRamRatio;
	}

	public String getVolumePrice() {
		return volumePrice;
	}

	public void setVolumePrice(String volumePrice) {
		this.volumePrice = volumePrice;
	}

	public String getCorePrice() {
		return corePrice;
	}

	public void setCorePrice(String corePrice) {
		this.corePrice = corePrice;
	}

	public String getRamPrice() {
		return ramPrice;
	}

	public void setRamPrice(String ramPrice) {
		this.ramPrice = ramPrice;
	}

	public String getFloatingPrice() {
		return floatingPrice;
	}

	public void setFloatingPrice(String floatingPrice) {
		this.floatingPrice = floatingPrice;
	}

	public String getImagePrice() {
		return imagePrice;
	}

	public void setImagePrice(String imagePrice) {
		this.imagePrice = imagePrice;
	}

	public String getVolumeRange() {
		return volumeRange;
	}

	public void setVolumeRange(String volumeRange) {
		this.volumeRange = volumeRange;
	}

	public String getFloatingRange() {
		return floatingRange;
	}

	public void setFloatingRange(String floatingRange) {
		this.floatingRange = floatingRange;
	}

	public String getIpRange() {
		return ipRange;
	}

	public void setIpRange(String ipRange) {
		this.ipRange = ipRange;
	}

	public String getZabbix_enabled() {
		return zabbix_enabled;
	}

	public void setZabbix_enabled(String zabbix_enabled) {
		this.zabbix_enabled = zabbix_enabled;
	}
	
	public Boolean isZabbixEnabled() {
		return "true".equals(getZabbix_enabled());
	}

	public String getSystemMaxTries() {
		return systemMaxTries;
	}

	public void setSystemMaxTries(String systemMaxTries) {
		this.systemMaxTries = systemMaxTries;
	}

	public String getSystemWaitTime() {
		return systemWaitTime;
	}

	public void setSystemWaitTime(String systemWaitTime) {
		this.systemWaitTime = systemWaitTime;
	}

	public String getSystemServiceSpec() {
		return systemServiceSpec;
	}

	public void setSystemServiceSpec(String systemServiceSpec) {
		this.systemServiceSpec = systemServiceSpec;
	}

	public String getServicePrice() {
		return servicePrice;
	}

	public void setServicePrice(String servicePrice) {
		this.servicePrice = servicePrice;
	}

	public String getPoolVolumeRange() {
		return poolVolumeRange;
	}

	public void setPoolVolumeRange(String poolVolumeRange) {
		this.poolVolumeRange = poolVolumeRange;
	}

	public String getPoolFipRange() {
		return poolFipRange;
	}

	public void setPoolFipRange(String poolFipRange) {
		this.poolFipRange = poolFipRange;
	}

	public String getPoolCpuRange() {
		return poolCpuRange;
	}

	public void setPoolCpuRange(String poolCpuRange) {
		this.poolCpuRange = poolCpuRange;
	}

	public String getPoolRamfipRange() {
		return poolRamfipRange;
	}

	public void setPoolRamfipRange(String poolRamfipRange) {
		this.poolRamfipRange = poolRamfipRange;
	}

	public String getInstanceQuota() {
		return instanceQuota;
	}

	public void setInstanceQuota(String instanceQuota) {
		this.instanceQuota = instanceQuota;
	}

	public String getCoreQuota() {
		return coreQuota;
	}

	public void setCoreQuota(String coreQuota) {
		this.coreQuota = coreQuota;
	}

	public String getRamQuota() {
		return ramQuota;
	}

	public void setRamQuota(String ramQuota) {
		this.ramQuota = ramQuota;
	}

	public String getKeypairQuota() {
		return keypairQuota;
	}

	public void setKeypairQuota(String keypairQuota) {
		this.keypairQuota = keypairQuota;
	}

	public String getFloatingipQuota() {
		return floatingipQuota;
	}

	public void setFloatingipQuota(String floatingipQuota) {
		this.floatingipQuota = floatingipQuota;
	}

	public String getVolumeQuota() {
		return volumeQuota;
	}

	public void setVolumeQuota(String volumeQuota) {
		this.volumeQuota = volumeQuota;
	}

	public String getExternalNetworkName() {
		return externalNetworkName;
	}

	public void setExternalNetworkName(String externalNetworkName) {
		this.externalNetworkName = externalNetworkName;
	}

	public String getSystemIngoreHosts() {
		return systemIngoreHosts;
	}

	public void setSystemIngoreHosts(String systemIngoreHosts) {
		this.systemIngoreHosts = systemIngoreHosts;
	}

	public String getSystemVdiSpec() {
		return systemVdiSpec;
	}

	public void setSystemVdiSpec(String systemVdiSpec) {
		this.systemVdiSpec = systemVdiSpec;
	}

	public String getSystemBaremetalSpec() {
		return systemBaremetalSpec;
	}

	public void setSystemBaremetalSpec(String systemBaremetalSpec) {
		this.systemBaremetalSpec = systemBaremetalSpec;
	}

	public String getSystemDBSpec() {
		return systemDBSpec;
	}

	public void setSystemDBSpec(String systemDBSpec) {
		this.systemDBSpec = systemDBSpec;
	}

	public String getSystemContainerSpec() {
		return systemContainerSpec;
	}

	public void setSystemContainerSpec(String systemContainerSpec) {
		this.systemContainerSpec = systemContainerSpec;
	}

	public String getVdiCorePrice() {
		return vdiCorePrice;
	}

	public void setVdiCorePrice(String vdiCorePrice) {
		this.vdiCorePrice = vdiCorePrice;
	}

	public String getVdiRamPrice() {
		return vdiRamPrice;
	}

	public void setVdiRamPrice(String vdiRamPrice) {
		this.vdiRamPrice = vdiRamPrice;
	}

	public String getVdiCoreQuota() {
		return vdiCoreQuota;
	}

	public void setVdiCoreQuota(String vdiCoreQuota) {
		this.vdiCoreQuota = vdiCoreQuota;
	}

	public String getVdiRamQuota() {
		return vdiRamQuota;
	}

	public void setVdiRamQuota(String vdiRamQuota) {
		this.vdiRamQuota = vdiRamQuota;
	}

	public String getSystemCcy() {
		return systemCcy;
	}

	public void setSystemCcy(String systemCcy) {
		this.systemCcy = systemCcy;
	}

	public String getSystemCcyName() {
		return systemCcyName;
	}

	public void setSystemCcyName(String systemCcyName) {
		this.systemCcyName = systemCcyName;
	}

	public String getSystemCcyUnit() {
		return systemCcyUnit;
	}

	public void setSystemCcyUnit(String systemCcyUnit) {
		this.systemCcyUnit = systemCcyUnit;
	}

	public String getSystemCcyUnitName() {
		return systemCcyUnitName;
	}

	public void setSystemCcyUnitName(String systemCcyUnitName) {
		this.systemCcyUnitName = systemCcyUnitName;
	}

	public String getBaremetalCorePrice() {
		return baremetalCorePrice;
	}

	public void setBaremetalCorePrice(String baremetalCorePrice) {
		this.baremetalCorePrice = baremetalCorePrice;
	}

	public String getBaremetalRamPrice() {
		return baremetalRamPrice;
	}

	public void setBaremetalRamPrice(String baremetalRamPrice) {
		this.baremetalRamPrice = baremetalRamPrice;
	}

	public String getPoolVdiCpuRange() {
		return poolVdiCpuRange;
	}

	public void setPoolVdiCpuRange(String poolVdiCpuRange) {
		this.poolVdiCpuRange = poolVdiCpuRange;
	}

	public String getPoolVdiRamRange() {
		return poolVdiRamRange;
	}

	public void setPoolVdiRamRange(String poolVdiRamRange) {
		this.poolVdiRamRange = poolVdiRamRange;
	}

	public String getPoolGeneralCpuRange() {
		return poolGeneralCpuRange;
	}

	public void setPoolGeneralCpuRange(String poolGeneralCpuRange) {
		this.poolGeneralCpuRange = poolGeneralCpuRange;
	}

	public String getPoolGeneralRamRange() {
		return poolGeneralRamRange;
	}

	public void setPoolGeneralRamRange(String poolGeneralRamRange) {
		this.poolGeneralRamRange = poolGeneralRamRange;
	}

	public String getRatingReportTime() {
		return ratingReportTime;
	}

	public void setRatingReportTime(String ratingReportTime) {
		this.ratingReportTime = ratingReportTime;
	}

	public String getRatingReportBeginDay() {
		return ratingReportBeginDay;
	}

	public void setRatingReportBeginDay(String ratingReportBeginDay) {
		this.ratingReportBeginDay = ratingReportBeginDay;
	}

	public String getRatingReportEndDay() {
		return ratingReportEndDay;
	}

	public void setRatingReportEndDay(String ratingReportEndDay) {
		this.ratingReportEndDay = ratingReportEndDay;
	}

	public String getSystemVmwareNetworkId() {
		return systemVmwareNetworkId;
	}

	public void setSystemVmwareNetworkId(String systemVmwareNetworkId) {
		this.systemVmwareNetworkId = systemVmwareNetworkId;
	}

	public String getSystemVmwareZone() {
		return systemVmwareZone;
	}

	public void setSystemVmwareZone(String systemVmwareZone) {
		this.systemVmwareZone = systemVmwareZone;
	}
	
    public String getDepolyKernel() {
		return depolyKernel;
	}

	public void setDepolyKernel(String depolyKernel) {
		this.depolyKernel = depolyKernel;
	}

	public String getDepolyRamdisk() {
		return depolyRamdisk;
	}

	public void setDepolyRamdisk(String depolyRamdisk) {
		this.depolyRamdisk = depolyRamdisk;
	}

	public String getSystemIronicNetworkId() {
		return systemIronicNetworkId;
	}

	public void setSystemIronicNetworkId(String systemIronicNetworkId) {
		this.systemIronicNetworkId = systemIronicNetworkId;
	}

	public String getMailSmtpHost() {
		return mailSmtpHost;
	}

	public void setMailSmtpHost(String mailSmtpHost) {
		this.mailSmtpHost = mailSmtpHost;
	}

	public String getMailSmtpStatrttlsEnable() {
		return mailSmtpStatrttlsEnable;
	}

	public void setMailSmtpStatrttlsEnable(String mailSmtpStatrttlsEnable) {
		this.mailSmtpStatrttlsEnable = mailSmtpStatrttlsEnable;
	}

	public String getMailSmtpPort() {
		return mailSmtpPort;
	}

	public void setMailSmtpPort(String mailSmtpPort) {
		this.mailSmtpPort = mailSmtpPort;
	}

	public String getMailSmtpAuth() {
		return mailSmtpAuth;
	}

	public void setMailSmtpAuth(String mailSmtpAuth) {
		this.mailSmtpAuth = mailSmtpAuth;
	}

	public String getMailSenderAddress() {
		return mailSenderAddress;
	}

	public void setMailSenderAddress(String mailSenderAddress) {
		this.mailSenderAddress = mailSenderAddress;
	}

	public String getMailSenderUsername() {
		return mailSenderUsername;
	}

	public void setMailSenderUsername(String mailSenderUsername) {
		this.mailSenderUsername = mailSenderUsername;
	}

	public String getMailSenderPassword() {
		return mailSenderPassword;
	}

	public void setMailSenderPassword(String mailSenderPassword) {
		this.mailSenderPassword = mailSenderPassword;
	}

	public String getSystemDefaultPriceName() {
		return systemDefaultPriceName;
	}

	public void setSystemDefaultPriceName(String systemDefaultPriceName) {
		this.systemDefaultPriceName = systemDefaultPriceName;
	}

	public String getSystemDefaultCurrencyName() {
		return systemDefaultCurrencyName;
	}

	public void setSystemDefaultCurrencyName(String systemDefaultCurrencyName) {
		this.systemDefaultCurrencyName = systemDefaultCurrencyName;
	}

	public String getSystemAdminDefaultPassword() {
		return systemAdminDefaultPassword;
	}

	public void setSystemAdminDefaultPassword(String systemAdminDefaultPassword) {
		this.systemAdminDefaultPassword = systemAdminDefaultPassword;
	}

	
	public String getSystemAdminDefaultAccount() {
		return systemAdminDefaultAccount;
	}

	public void setSystemAdminDefaultAccount(String systemAdminDefaultAccount) {
		this.systemAdminDefaultAccount = systemAdminDefaultAccount;
	}

	public String getSystemAdminDefaultName() {
		return systemAdminDefaultName;
	}

	public void setSystemAdminDefaultName(String systemAdminDefaultName) {
		this.systemAdminDefaultName = systemAdminDefaultName;
	}

	public String getSystemAdminDefaultMail() {
		return systemAdminDefaultMail;
	}

	public void setSystemAdminDefaultMail(String systemAdminDefaultMail) {
		this.systemAdminDefaultMail = systemAdminDefaultMail;
	}

	public String getSystemAdminDefaultPhone() {
		return systemAdminDefaultPhone;
	}

	public void setSystemAdminDefaultPhone(String systemAdminDefaultPhone) {
		this.systemAdminDefaultPhone = systemAdminDefaultPhone;
	}

	public String getSystemAdminDefaultCompany() {
		return systemAdminDefaultCompany;
	}

	public void setSystemAdminDefaultCompany(String systemAdminDefaultCompany) {
		this.systemAdminDefaultCompany = systemAdminDefaultCompany;
	}

	public String getSystemDefaultLocale() {
		return systemDefaultLocale;
	}

	public void setSystemDefaultLocale(String systemDefaultLocale) {
		this.systemDefaultLocale = systemDefaultLocale;
	}

	public String getPermission_enabled() {
		return permission_enabled;
	}

	public void setPermission_enabled(String permission_enabled) {
		this.permission_enabled = permission_enabled;
	}
	
	public Boolean isPermissionEnabled() {
		return "true".equalsIgnoreCase(getZabbix_enabled());
	}

	public String getSystemVolumeQuota() {
		return systemVolumeQuota;
	}

	public void setSystemVolumeQuota(String systemVolumeQuota) {
		this.systemVolumeQuota = systemVolumeQuota;
	}

	public String getSystemFloatingIPQuota() {
		return systemFloatingIPQuota;
	}

	public void setSystemFloatingIPQuota(String systemFloatingIPQuota) {
		this.systemFloatingIPQuota = systemFloatingIPQuota;
	}

	public String getCloudVMWareUrl() {
		return cloudVMWareUrl;
	}

	public void setCloudVMWareUrl(String cloudVMWareUrl) {
		this.cloudVMWareUrl = cloudVMWareUrl;
	}
	
}
