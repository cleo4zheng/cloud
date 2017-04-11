package com.cloud.cloudapi.json.forgui;



public class QuotaJSON {
	
	private QuotaSet quota_set;
	
	class QuotaSet {
		  private Boolean force;
		  private Integer  ram;
		  private Integer  floating_ips;
		  private Integer  cores;
		 
		   
		  public QuotaSet(Boolean force,Integer ram,Integer floating_ips,Integer cores){
			  this.force = force;
			  this.ram = ram;
			  this.cores = cores;
			  this.floating_ips = floating_ips;
		  }
		  
		  public void setForce(Boolean force){
			  this.force = force;
		  }
		   
		  public Boolean getForce(){
			  return this.force;
		  }
		   
		  public void setRam(Integer ram){
			  this.ram = ram;
		  }
		   
		  public Integer getRam(){
			  return this.ram;
		  }
		   
		   
		  public void setCores(Integer cores){
		      this.cores = cores;
		  }
		   
		  public Integer getCores(){
			  return this.cores;
		  }
		   
		  public void setFloating_ips(Integer floating_ips){
			  this.floating_ips = floating_ips;
		  }
		   
		  public Integer getFloating_ips(){
			  return this.floating_ips;
		  }
		   
		}

	
	public QuotaJSON(Boolean force,Integer ram,Integer floating_ips,Integer cores){
    	this.quota_set = new QuotaSet(force,ram,floating_ips,cores);
}
}
