package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;

public class RatingPolicy {

	private String field_id;
	private String field_code;
	private String name;
	private String default_chargekey;
	private List<String> charging_keys;
	private String c_unit;
	private String c_unit_name;
	private Integer c_unit_conversion;
	private Integer c_unit_value;
	private String t_unit;
	private String t_unit_name;
	private Integer t_unit_conversion;
	private Integer t_unit_value;
	
	private String field_key;
	private String field_name;
	private Float qty;
	private Float rate;
	private Float price;
	private Float unit_price;
    private String unit;
	private RatingFieldDescInfo desc;
	private Boolean usethreshold;
	private Float tenant_discount;
	
	public String getField_id() {
		return field_id;
	}

	public void setField_id(String field_id) {
		this.field_id = field_id;
	}

	public String getField_code() {
		return field_code;
	}

	public void setField_code(String field_code) {
		this.field_code = field_code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getCharging_keys() {
		return charging_keys;
	}

	public void setCharging_keys(List<String> charging_keys) {
		this.charging_keys = charging_keys;
	}

	public String getC_unit() {
		return c_unit;
	}

	public void setC_unit(String c_unit) {
		this.c_unit = c_unit;
	}

	public String getC_unit_name() {
		return c_unit_name;
	}

	public void setC_unit_name(String c_unit_name) {
		this.c_unit_name = c_unit_name;
	}

	public Integer getC_unit_conversion() {
		return c_unit_conversion;
	}

	public void setC_unit_conversion(Integer c_unit_conversion) {
		this.c_unit_conversion = c_unit_conversion;
	}

	public Integer getC_unit_value() {
		return c_unit_value;
	}

	public void setC_unit_value(Integer c_unit_value) {
		this.c_unit_value = c_unit_value;
	}

	public String getT_unit() {
		return t_unit;
	}

	public void setT_unit(String t_unit) {
		this.t_unit = t_unit;
	}

	public String getT_unit_name() {
		return t_unit_name;
	}

	public void setT_unit_name(String t_unit_name) {
		this.t_unit_name = t_unit_name;
	}

	public Integer getT_unit_conversion() {
		return t_unit_conversion;
	}

	public void setT_unit_conversion(Integer t_unit_conversion) {
		this.t_unit_conversion = t_unit_conversion;
	}

	public Integer getT_unit_value() {
		return t_unit_value;
	}

	public void setT_unit_value(Integer t_unit_value) {
		this.t_unit_value = t_unit_value;
	}


	public String getDefault_chargekey() {
		return default_chargekey;
	}

	public void setDefault_chargekey(String default_chargekey) {
		this.default_chargekey = default_chargekey;
	}

	public String getField_key() {
		return field_key;
	}

	public void setField_key(String field_key) {
		this.field_key = field_key;
	}

	public String getField_name() {
		return field_name;
	}

	public void setField_name(String field_name) {
		this.field_name = field_name;
	}

	public Float getQty() {
		return qty;
	}

	public void setQty(Float qty) {
		this.qty = qty;
	}

	public Float getRate() {
		return rate;
	}

	public void setRate(Float rate) {
		this.rate = rate;
	}

	public Float getPrice() {
		return price;
	}

	public void setPrice(Float price) {
		this.price = price;
	}

	public Float getUnit_price() {
		return unit_price;
	}

	public void setUnit_price(Float unit_price) {
		this.unit_price = unit_price;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public RatingFieldDescInfo getDesc() {
		return desc;
	}

	public void setDesc(RatingFieldDescInfo desc) {
		this.desc = desc;
	}

	public Boolean getUsethreshold() {
		return usethreshold;
	}

	public void setUsethreshold(Boolean usethreshold) {
		this.usethreshold = usethreshold;
	}

	public Float getTenant_discount() {
		return tenant_discount;
	}

	public void setTenant_discount(Float tenant_discount) {
		this.tenant_discount = tenant_discount;
	}

}
