package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class Range {

	private Integer min;
	private Integer max;

	public Range(){}
	
	public Range(Integer min, Integer max){
		this.min = min;
		this.max = max;
	}
	
	public Integer getMin() {
		return min;
	}

	public void setMin(Integer min) {
		this.min = min;
	}

	public Integer getMax() {
		return max;
	}

	public void setMax(Integer max) {
		this.max = max;
	}

}
