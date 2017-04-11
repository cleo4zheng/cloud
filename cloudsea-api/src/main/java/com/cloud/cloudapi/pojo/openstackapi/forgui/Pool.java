package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.Map;

public class Pool {

	private String name;
	private Integer cores;
	private Integer ramSize;
	
	private Map<String,Integer> floatingIPNumbers;
	/*********
	 * Example:
	 * BGP_NETWORK:10;
	 * TELECOM_NETWORK:20;
	 * UNICOM_NETWORK:30;
	 * MOBILE_NETWORK:40;
	 *********/
	/*********
	 * Example:
	 * Performance Disk,100GB
	 * Capacity Disk, 1000GB
	 * HighPerformance Disk,50GB
	 **********/
	private Map<String,Integer>diskInfo; 
	/**********
	 * Example:
	 * LBaaS,true
	 * VPNaas,false
	 * FWaas,true
	 * MonitorService,true
	 * DatabaseService,false
	 **********/
	private Map<String,Boolean> serviceStatus;
	
	public void setName(String name){
		this.name  = name;
	}
	
	public String getName(){
		return this.name;
	}
	
	public void setCores(Integer cores){
		this.cores = cores;
	}
	
	public Integer getCores(){
		return this.cores;
	}
	
	public void setRamsSize(Integer ramSize){
		this.ramSize = ramSize;
	}
	
	public Integer getRamSize(){
		return this.ramSize;
	}
	
	public void setFloatingIPNumbers(Map<String,Integer> floatingIPNumbers){
		this.floatingIPNumbers = floatingIPNumbers;
	}
	
	public Map<String,Integer> getFloatingIPNumbers(){
		return this.floatingIPNumbers;
	}
	
	public void setDiskInfo(Map<String,Integer> diskInfo){
		this.diskInfo = diskInfo;
	}
	
	public Map<String,Integer> getDiskInfo(){
		return this.diskInfo;
	}

	public void setServiceStatus(Map<String,Boolean> serviceStatus){
		this.serviceStatus = serviceStatus;
	}
	
	public Map<String,Boolean> getServiceStatus(){
		return this.serviceStatus;
	}
}
