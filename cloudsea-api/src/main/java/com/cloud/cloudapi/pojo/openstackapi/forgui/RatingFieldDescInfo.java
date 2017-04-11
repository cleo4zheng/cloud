package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;

public class RatingFieldDescInfo {

	private Float price;
	private String fieldrating_id;
	private Float unit_price;
	private Float tenant_discount;
	private List<String> threshold_info;
	private Float init_price;

	public Float getPrice() {
		return price;
	}

	public void setPrice(Float price) {
		this.price = price;
	}

	public String getFieldrating_id() {
		return fieldrating_id;
	}

	public void setFieldrating_id(String fieldrating_id) {
		this.fieldrating_id = fieldrating_id;
	}

	public Float getUnit_price() {
		return unit_price;
	}

	public void setUnit_price(Float unit_price) {
		this.unit_price = unit_price;
	}

	public Float getTenant_discount() {
		return tenant_discount;
	}

	public void setTenant_discount(Float tenant_discount) {
		this.tenant_discount = tenant_discount;
	}

	public List<String> getThreshold_info() {
		return threshold_info;
	}

	public void setThreshold_info(List<String> threshold_info) {
		this.threshold_info = threshold_info;
	}

	public Float getInit_price() {
		return init_price;
	}

	public void setInit_price(Float init_price) {
		this.init_price = init_price;
	}

}
