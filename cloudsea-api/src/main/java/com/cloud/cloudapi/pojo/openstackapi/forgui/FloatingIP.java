package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.Locale;

import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.StringHelper;

//@JsonInclude(value=Include.NON_NULL)
public class FloatingIP {
	private String id;
	private String tenantId;
	private String status;
	private String routerId;
	private String networkId;
	private String instanceId;
	private String loadbalancerId;
	private String fixedIpAddress;
	//for API /floating-ips & /floatingip
	private String floatingIpAddress;
	private String port_id;
	private String type;
	private String typeName;
	private Double unitPrice;
	//for API /floating-ips
	private String name;
//	private String bandwidth;
//	private String line;
//	private HashMap<String,String> resource;
	private String createdAt;
	private Integer bandwith;
	private ResourceSpec resource;
	private String floating_network_id;
    private Long millionSeconds;
    private Boolean assigned;
    
	public void setId(String id){
		this.id = id;
	}
	
	public String getId(){
		return this.id;
	}
    
	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public void setTenantId(String tenantId){
		this.tenantId = tenantId;
	}
	
	public String getTenantId(){
		return this.tenantId;
	}
	
	public Double getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(Double unitPrice) {
		this.unitPrice = unitPrice;
	}

	public void setStatus(String status){
		this.status = status;
	}
	
	public String getStatus(){
		return this.status;
	}
	
	public void setRouterId(String routerId){
		this.routerId = routerId;
	}
	
	public String getRouterId(){
		return this.routerId;
	}
	
	public String getNetworkId() {
		return networkId;
	}

	public void setNetworkId(String networkId) {
		this.networkId = networkId;
	}
	
	public void setFixedIpAddress(String fixedIpAddress){
		this.fixedIpAddress = fixedIpAddress;
	}
	
	public String getFixedIpAddress(){
		return this.fixedIpAddress;
	}
	
	public void setFloatingIpAddress(String floatingIpAddress){
		this.floatingIpAddress = floatingIpAddress;
	}
	
	public String getFloatingIpAddress(){
		return this.floatingIpAddress;
	}
	
	public String getPort_id() {
		return port_id;
	}

	public void setPort_id(String port_id) {
		this.port_id = port_id;
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

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public ResourceSpec getResource() {
		return resource;
	}

	public void setResource(ResourceSpec resource) {
		this.resource = resource;
	}
	
	public String getResourceId(){
		return null != this.resource ? this.resource.getId() : null;
	}

	public String getResourceName(){
		return null != this.resource ? this.resource.getName() : null;
	}
	
	public String getResourceType(){
		return null != this.resource ? this.resource.getType() : null;
	}
	
	public void addResource(String id,String name,String type){
		this.resource = new ResourceSpec();
		this.resource.setId(id);
		this.resource.setName(name);
		this.resource.setType(type);
	}

	public String getFloating_network_id() {
		return floating_network_id;
	}

	public void setFloating_network_id(String floating_network_id) {
		this.floating_network_id = floating_network_id;
	}

	public Integer getBandwith() {
		return bandwith;
	}

	public void setBandwith(Integer bandwith) {
		this.bandwith = bandwith;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	public Boolean getAssigned() {
		return assigned;
	}

	public void setAssigned(Boolean assigned) {
		this.assigned = assigned;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public String getLoadbalancerId() {
		return loadbalancerId;
	}

	public void setLoadbalancerId(String loadbalancerId) {
		this.loadbalancerId = loadbalancerId;
	}

	public void normalInfo(Locale locale){
		String floatingipType = "CS_FLOTINGIP_TYPE_NAME";
		if(!Util.isNullOrEmptyValue(this.getType())){
			if(null == locale)
				locale = new Locale("zh");
			String floatingipTypeName = floatingipType.replaceFirst("TYPE", this.getType().toUpperCase());
			this.setTypeName(Message.getMessage(floatingipTypeName,locale,false));
		}
		//this.setAssigned(null);
		this.setFixedIpAddress(null);
		this.setFloating_network_id(null);
		this.setInstanceId(null);
		this.setPort_id(null);
		this.setLoadbalancerId(null);
		this.setNetworkId(null);
		this.setRouterId(null);
		this.setTenantId(null);
		this.setUnitPrice(null);
		ResourceSpec resource = this.getResource();
		if(null != resource){
			resource.setName(StringHelper.ncr2String(resource.getName()));
			resource.setFree(null);
			resource.setRange(null);
			resource.setSize(null);
			resource.setTotal(null);
			resource.setTypeName(null);
			resource.setUnitPrice(null);
			resource.setUsed(null);
			this.setResource(resource);
		}
		if(null != this.getMillionSeconds()){
			this.setCreatedAt(Util.millionSecond2Date(this.getMillionSeconds()));
			this.setMillionSeconds(null);	
		}
	}
}
