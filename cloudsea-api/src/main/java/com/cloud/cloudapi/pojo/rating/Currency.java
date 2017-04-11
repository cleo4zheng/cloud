package com.cloud.cloudapi.pojo.rating;

public class Currency {

	private String ccy_id;
	private String ccy;
	private String ccy_name;
	private String ccy_unit;
	private String ccy_unit_name;
	private Long millionSeconds;
	
	public Currency(){
	}
	
	public Currency(String ccy,String ccy_name,String ccy_unit,String ccy_unit_name){
		this.ccy = ccy;
		this.ccy_name = ccy_name;
		this.ccy_unit = ccy_unit;
		this.ccy_unit_name = ccy_unit_name;
	}
	
	public String getCcy_id() {
		return ccy_id;
	}

	public void setCcy_id(String ccy_id) {
		this.ccy_id = ccy_id;
	}

	public String getCcy() {
		return ccy;
	}

	public void setCcy(String ccy) {
		this.ccy = ccy;
	}

	public String getCcy_name() {
		return ccy_name;
	}

	public void setCcy_name(String ccy_name) {
		this.ccy_name = ccy_name;
	}

	public String getCcy_unit() {
		return ccy_unit;
	}

	public void setCcy_unit(String ccy_unit) {
		this.ccy_unit = ccy_unit;
	}

	public String getCcy_unit_name() {
		return ccy_unit_name;
	}

	public void setCcy_unit_name(String ccy_unit_name) {
		this.ccy_unit_name = ccy_unit_name;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}
}
