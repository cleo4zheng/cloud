package com.cloud.cloudapi.util;

public class PermissionConstant {
	
	//instance
	public static final String[] instances = {"LIST_INSTANCE","GET_INSTANCE","CREATE_INSTANCE","UPDATE_INSTANCE","START_INSTANCE","STOP_INSTANCE","RESIZE_INSTANCE","RESTART_INSTANCE","FORCE_RESTART_INSTANCE","SUSPEND_INSTANCE","PAUSE_INSTANCE","FORCE_STOP_INSTANCE","DELETE_INSTANCE"};
	
	//image
	public static final String[] images = {"LIST_IMAGE","GET_IMAGE","CREATE_PRIVATE_IMAGE","DELETE_IMAGE"}; 
	
	//ironic
	public static final String[] physMachines = {"LIST_PHYS_MACHINE","GET_PHYS_MACHINE","REGISTER_PHYS_MACHINE","CREATE_PHYS_MACHINE","START_PHYS_MACHINE","STOP_PHYS_MACHINE","DELETE_PHYS_MACHINE"}; 

	//security group
	public static final String[] securityGroups = {"LIST_SECURITY_GROUP","GET_SECURITY_GROUP","CREATE_SECURITY_GROUP","UPDATE_SECURITY_GROUP","ATTACH_SECURITY_GROUP","DELETE_SECURITY_GROUP"};
	
	//security group rule
	public static final String[] securityGroupRules = {"LIST_SECURITY_GROUP_RULE","GET_SECURITY_GROUP_RULE","CREATE_SECURITY_GROUP_RULE","DELETE_SECURITY_GROUP_RULE"}; 
	
	//network
	public static final String[] nets = {"LIST_NET","GET_NET","CREATE_NET","UPDATE_NET","DELETE_NET"}; 

	//subnets
	public static final String[] subnets = {"LIST_SUBNET","GET_SUBNET","CREATE_SUBNET","UPDATE_SUBNET","DELETE_SUBNET"}; 
	
	//ports
	public static final String[] ports = {"LIST_PORT","GET_PORT","CREATE_PORT","UPDATE_PORT","DELETE_PORT"}; 
	
	//floatings
	public static final String[] floatings = {"LIST_FLOATING","GET_FLOATING","CREATE_FLOATING","UPDATE_FLOATING","ATTACH_FLOATING","DETACH_FLOATING","DELETE_FLOATING"}; 
	
	//routers
	public static final String[] routers = {"LIST_ROUTER","GET_ROUTER","CREATE_ROUTER","UPDATE_ROUTER","ENABLE_GATEWAY_ROUTER","DISABLE_GATEWAY_ROUTER","ATTACH_SUBNET_ROUTER","DETACH_SUBNET_ROUTER","DELETE_ROUTER"}; 

	//keypairs
	public static final String[] keypairs = {"LIST_KEYPAIR","GET_KEYPAIR","CREATE_KEYPAIR","UPLOAD_KEYPAIR","DELETE_KEYPAIR"}; 

	//volumes
	public static final String[] volumes = {"LIST_VOLUME","GET_VOLUME","CREATE_VOLUME","UPDATE_VOLUME","ATTACH_VOLUME","DETACH_VOLUME","RESTORE_VOLUME","DELETE_VOLUME"}; 
	
	//backups
	public static final String[] backups = {"LIST_BACKUP","GET_BACKUP","CREATE_BACKUP","DELETE_BACKUP"}; 

	//loadbalancers
	public static final String[] lbs = {"LIST_LB","GET_LB","CREATE_LB","UPDATE_LB","ENABLE_LB","DISABLE_LB","DELETE_LB"}; 
	
	//firewalls
	public static final String[] fws = {"LIST_FW","GET_LB","CREATE_FW","UPDATE_FW","ENABLE_FW","DISABLE_FW","DELETE_FW"}; 
	
	//vpns
	public static final String[] vpns = {"LIST_VPN","GET_VPN","CREATE_VPN","UPDATE_VPN","ENABLE_VPN","DISABLE_VPN","DELETE_VPN"};
	
	//monitors
	public static final String[] monitors = {"LIST_MONITOR","GET_MONITOR","CREATE_MONITOR","UPDATE_MONITOR","ENABLE_MONITOR","DISABLE_MONITOR","DELETE_MONITOR"};

	//workflows
	public static final String[] workflows = {"LIST_WF","GET_WF","TRANSFER_FW","RECLAIM_FW","APPROVE_FW","DELETE_FW"};

	//workflows
	public static final String[] containers = {"LIST_CONTAINER","GET_CONTAINER","TRANSFER_FW","RECLAIM_FW","APPROVE_FW","DELETE_FW"};
}
