package com.cloud.cloudapi.pojo.rating;

import java.util.ArrayList;
import java.util.List;

public class Price {

	class User {
		private String id;
		private String name;

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

		public User(){}
		public User(String id,String name){
			this.id = id;
			this.name = name;
		}
	}

	class PriceInfo {
		private String id;
		private String type;
		private String pricing;
		private String unit;

		public PriceInfo(){}
		
		public PriceInfo(String id,String type,String pricing,String unit){
			this.id = id;
			this.type = type;
			this.pricing = pricing;
			this.unit = unit;
		}
		
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getPricing() {
			return pricing;
		}

		public void setPricing(String pricing) {
			this.pricing = pricing;
		}

		public String getUnit() {
			return unit;
		}

		public void setUnit(String unit) {
			this.unit = unit;
		}
	}

	private String id;
	private String name;
	private String version;
	private String status;
	private String createdAt;
	private String description;
	private List<User> users;
	private List<PriceInfo> computes;
	private List<PriceInfo> storages;
	private List<PriceInfo> networks;
	private List<PriceInfo> images;
	private List<PriceInfo> services;
	
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

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<User> getUsers() {
		return users;
	}

	public void setUsers(List<User> users) {
		this.users = users;
	}

	public List<PriceInfo> getComputes() {
		return computes;
	}

	public void setComputes(List<PriceInfo> computes) {
		this.computes = computes;
	}

	public List<PriceInfo> getStorages() {
		return storages;
	}

	public void setStorages(List<PriceInfo> storages) {
		this.storages = storages;
	}

	public List<PriceInfo> getNetworks() {
		return networks;
	}

	public void setNetworks(List<PriceInfo> networks) {
		this.networks = networks;
	}

	public List<PriceInfo> getImages() {
		return images;
	}

	public void setImages(List<PriceInfo> images) {
		this.images = images;
	}

	public List<PriceInfo> getServices() {
		return services;
	}

	public void setServices(List<PriceInfo> services) {
		this.services = services;
	}
	
	public void addUser(String id,String name){
		if(null  == this.users)
			this.users = new ArrayList<User>();
		this.users.add(new User(id,name));
	}
	
	public void addComputePrice(String id,String type,String pricing,String unit){
		if(null == this.computes)
			this.computes = new ArrayList<PriceInfo>();
		this.computes.add(new PriceInfo(id,type,pricing,unit));
	}

	public void addStoragePrice(String id,String type,String pricing,String unit){
		if(null == this.storages)
			this.storages = new ArrayList<PriceInfo>();
		this.storages.add(new PriceInfo(id,type,pricing,unit));
	}
	
	public void addNetworkPrice(String id,String type,String pricing,String unit){
		if(null == this.networks)
			this.networks = new ArrayList<PriceInfo>();
		this.networks.add(new PriceInfo(id,type,pricing,unit));
	}
	
	public void addImagePrice(String id,String type,String pricing,String unit){
		if(null == this.images)
			this.images = new ArrayList<PriceInfo>();
		this.images.add(new PriceInfo(id,type,pricing,unit));
	}
	
	public void addServicePrice(String id,String type,String pricing,String unit){
		if(null == this.services)
			this.services = new ArrayList<PriceInfo>();
		this.services.add(new PriceInfo(id,type,pricing,unit));
	}
}
