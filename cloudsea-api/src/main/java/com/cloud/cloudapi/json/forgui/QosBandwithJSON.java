package com.cloud.cloudapi.json.forgui;

import com.cloud.cloudapi.pojo.openstackapi.forgui.QosBandwith;

public class QosBandwithJSON {

	private QosBandwith bandwidth_limit_rule;
	
	public QosBandwithJSON(QosBandwith bandwidth_limit_rule){
		this.bandwidth_limit_rule = bandwidth_limit_rule;
	}
}
