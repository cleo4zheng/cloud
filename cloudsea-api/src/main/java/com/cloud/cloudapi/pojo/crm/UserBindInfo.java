package com.cloud.cloudapi.pojo.crm;

public class UserBindInfo {

	private String userid;//用户id
	private String ddh;//订单号
	private String action;//动作 绑定/解除绑定
	private Long millionSeconds; //发生时间
	
	public String getUserid() {
		return userid;
	}
	public void setUserid(String userid) {
		this.userid = userid;
	}
	public String getDdh() {
		return ddh;
	}
	public void setDdh(String ddh) {
		this.ddh = ddh;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public Long getMillionSeconds() {
		return millionSeconds;
	}
	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	
}
