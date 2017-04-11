package com.cloud.cloudapi.pojo.rating;

import java.util.List;

public class BillingReport {

	private String id;
	private String name;
	private String tenantId;
	private String ccy;
	private String billing_month;
	private Float cost;
    private Float compute;
    private Float network;
    private Float storage;
    private Float image;
    private Float service;
    private String computeDetails;
    private String storageDetails;
    private String networkDetails;
    private String serviceDetails;
    private String imageDetails;
    private Boolean complete;
    private Long millionSeconds;
    private String billingId;
    private List<TemplateService> ratingServices;
    private Billing billing;
    public BillingReport(){
    	
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

	public Boolean getComplete() {
		return complete;
	}

	public void setComplete(Boolean complete) {
		this.complete = complete;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	public String getCcy() {
		return ccy;
	}

	public void setCcy(String ccy) {
		this.ccy = ccy;
	}

	public String getBilling_month() {
		return billing_month;
	}

	public void setBilling_month(String billing_month) {
		this.billing_month = billing_month;
	}

	public Float getCost() {
		return cost;
	}

	public void setCost(Float cost) {
		this.cost = cost;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public Float getCompute() {
		return compute;
	}

	public void setCompute(Float compute) {
		this.compute = compute;
	}

	public Float getNetwork() {
		return network;
	}

	public void setNetwork(Float network) {
		this.network = network;
	}

	public Float getStorage() {
		return storage;
	}

	public void setStorage(Float storage) {
		this.storage = storage;
	}

	public Float getImage() {
		return image;
	}

	public void setImage(Float image) {
		this.image = image;
	}

	public Float getService() {
		return service;
	}

	public void setService(Float service) {
		this.service = service;
	}

	public String getComputeDetails() {
		return computeDetails;
	}

	public void setComputeDetails(String computeDetails) {
		this.computeDetails = computeDetails;
	}

	public String getStorageDetails() {
		return storageDetails;
	}

	public void setStorageDetails(String storageDetails) {
		this.storageDetails = storageDetails;
	}

	public String getNetworkDetails() {
		return networkDetails;
	}

	public void setNetworkDetails(String networkDetails) {
		this.networkDetails = networkDetails;
	}

	public String getServiceDetails() {
		return serviceDetails;
	}

	public void setServiceDetails(String serviceDetails) {
		this.serviceDetails = serviceDetails;
	}

	public String getImageDetails() {
		return imageDetails;
	}

	public void setImageDetails(String imageDetails) {
		this.imageDetails = imageDetails;
	}

	public List<TemplateService> getRatingServices() {
		return ratingServices;
	}

	public void setRatingServices(List<TemplateService> ratingServices) {
		this.ratingServices = ratingServices;
	}

	public Billing getBilling() {
		return billing;
	}

	public void setBilling(Billing billing) {
		this.billing = billing;
	}

	public String getBillingId() {
		return billingId;
	}

	public void setBillingId(String billingId) {
		this.billingId = billingId;
	}

	public void normalInfo(Boolean detail){
		this.setComplete(null);
		this.setId(null);
		this.setMillionSeconds(null);
		this.setTenantId(null);
		this.setBilling_month(null);
		this.setBillingId(null);
		if(true == detail){
			this.setComputeDetails(null);
			this.setStorageDetails(null);
			this.setNetworkDetails(null);
			this.setImageDetails(null);
			this.setServiceDetails(null);
		}
		if(null != this.billing)
			this.billing.normalInfo();
	}
}
