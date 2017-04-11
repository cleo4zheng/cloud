package com.cloud.cloudapi.pojo.rating;

public class Billing {

	private String id;
	private String userId;
	private String name;
	private String account;
	private Boolean defaultAccount;
	private Long millionSeconds;
	private String createAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	public String getCreateAt() {
		return createAt;
	}

	public void setCreateAt(String createAt) {
		this.createAt = createAt;
	}
	
	public Boolean getDefaultAccount() {
		return defaultAccount;
	}

	public void setDefaultAccount(Boolean defaultAccount) {
		this.defaultAccount = defaultAccount;
	}

	public void normalInfo(){
		this.setMillionSeconds(null);
		this.setCreateAt(null);
		this.setUserId(null);
	}
}
