package com.cloud.cloudapi.json.forgui;

import com.cloud.cloudapi.pojo.openstackapi.forgui.SecurityGroup;

public class SecurityGroupJSON {
	 private SecurityGroup security_group;
	 
	 public SecurityGroupJSON(){
	    	this.security_group = new SecurityGroup();
	    }
	 
	 public SecurityGroupJSON(SecurityGroup security_group){
	    	this.security_group = security_group;
	    }
}
