package com.cloud.cloudapi.util.http.pool;
/** 
* @author  wangw
* @create  2016年5月25日 下午5:56:20 
* 
* openstack endpoint类型 枚举
*/
public enum  OSEndpointType {
	EP_TYPE_COMPUTE("compute"),
	EP_TYPE_NETWORK("network"),
	EP_TYPE_VOLUMEV2("volumev2"),
	EP_TYPE_IMAGE("image"),
	EP_TYPE_METERING("metering"),
	EP_TYPE_VOLUME("volume"),
	EP_TYPE_LBAAS("loadbalancing"),
	EP_TYPE_ORCHESTRATION("orchestration"),
	EP_TYPE_CLOUDFORMATION("cloudformation"),
	EP_TYPE_IDENTIFY("identity"),
	EP_TYPE_SHARE("share"),
	EP_TYPE_SHAREV2("sharev2"),
	EP_TYPE_RATING("rating");
    
	private String type;
	
	
	OSEndpointType(String type){
		this.type = type;
	}
	
	public String getType(){
		return this.type;
	}
	

}
