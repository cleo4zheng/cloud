package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class IPSecPolicy extends Policy {

	private String transform_protocol;
	private String encapsulation_mode;

	public String getTransform_protocol() {
		return transform_protocol;
	}

	public void setTransform_protocol(String transform_protocol) {
		this.transform_protocol = transform_protocol;
	}

	public String getEncapsulation_mode() {
		return encapsulation_mode;
	}

	public void setEncapsulation_mode(String encapsulation_mode) {
		this.encapsulation_mode = encapsulation_mode;
	}
	
	

}
