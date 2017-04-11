package com.cloud.cloudapi.util;

public class DictConstant {

	/*monitor start--- */
	public static final String MONITOR_TYPE_INSTANCE = "instance";
	public static final String MONITOR_TYPE_BAREMETAL = "bareMetal";
	public static final String MONITOR_TYPE_VDI_INSTANCE = "vdiInstance";
	public static final String MONITOR_TYPE_SERVICE = "service";
    public static final String MONITOR_TYPE_APPLICATION = "application";
    public static final String MONITOR_TYPE_LOADBANLANCER = "loadbalancer";
	
	public static final String MONITOROBJ_TYPE_SERVICE_VPN = "service_vpn";
	public static final String MONITOROBJ_TYPE_SERVICE_LOADBALANCER = "service_loadbalancer";
	public static final String MONITOROBJ_TYPE_SERVICE_FIREWALL = "service_firewall";
	public static final String MONITOROBJ_TYPE_SERVICE_ = "service_";
	
	public static final String MONITOR_STATUS_NORMAL = "normal";
	public static final String MONITOR_STATUS_SHORTAGE = "shortage";
	public static final String MONITOR_STATUS_WARNING = "warning";
	
	/*notification start--- */
	public static final String NOTIFICATION_TYPE_WARNING = "warning";
	public static final String NOTIFICATION_TYPE_FINANCE = "finance";
	public static final String NOTIFICATION_TYPE_SYSTEM = "system";
	public static final String NOTIFICATION_TYPE_OTHER = "other";
	
	public static final String NOTIFICATION_TERMINAL_TYPE_EMAIL = "EMail";
	public static final String NOTIFICATION_TERMINAL_TYPE_SMS = "SMS";
    
	public static final String HTTP_REQUEST_TYPE_GET = "GET";
	public static final String HTTP_REQUEST_TYPE_POST = "POST";
	public static final String HTTP_REQUEST_TYPE_PUT = "PUT";
	public static final String HTTP_REQUEST_TYPE_DELETE = "DELETE";
	
	public static final String MONITOR_RULE_TYPE_CPU_UTIL = "cpuUtil";
	public static final String MONITOR_RULE_TYPE_MEMORY_UTIL = "memoryUtil";
	public static final String MONITOR_RULE_TYPE_DISK_UTIL = "diskUtil";
	public static final String MONITOR_RULE_TYPE_INTERNALNETOUT = "internalNetOut";
	public static final String MONITOR_RULE_TYPE_INTERNALNETIN = "internalNetIn";
	public static final String MONITOR_RULE_TYPE_EXTERNALNETOUT = "externalNetOut";
	public static final String MONITOR_RULE_TYPE_EXTERNALNETIN = "externalNetIn";
	
	public static final String MONITOR_RULE_TYPE_CPU_UTIL_VM = "cpu_util";
	public static final String MONITOR_RULE_TYPE_MEMORY_UTIL_VM = "memory.usage";
	public static final String MONITOR_RULE_TYPE_DISK_UTIL_VM = "disk.usage";
	public static final String MONITOR_RULE_TYPE_INTERNALNETOUT_VM = "inner.network.outgoing.bytes";
	public static final String MONITOR_RULE_TYPE_INTERNALNETIN_VM = "inner.network.incoming.bytes";
	public static final String MONITOR_RULE_TYPE_EXTERNALNETOUT_VM = "outer.network.outgoing.bytes";
	public static final String MONITOR_RULE_TYPE_EXTERNALNETIN_VM = "outer.network.incoming.bytes";
	
	public static final String MONITOR_RULE_TYPE_CPU_UTIL_BAREMETAL = "physical.cpu.usage";
	public static final String MONITOR_RULE_TYPE_MEMORY_UTIL_BAREMETAL = "physical.memory.usage";
	public static final String MONITOR_RULE_TYPE_DISK_UTIL_BAREMETAL = "physical.disk.usage";
	public static final String MONITOR_RULE_TYPE_INTERNALNETOUT_BAREMETAL = "physical.inner.network.outgoing.bytes"; // act not has this type
	public static final String MONITOR_RULE_TYPE_INTERNALNETIN_BAREMETAL = "physical.inner.network.incoming.bytes"; //act not has this type
	public static final String MONITOR_RULE_TYPE_EXTERNALNETOUT_BAREMETAL = "physical.outer.network.outgoing.bytes";
	public static final String MONITOR_RULE_TYPE_EXTERNALNETIN_BAREMETAL = "physical.outer.network.incoming.bytes";
	
	public static final String MONITOR_HISTORY_DATA_TYPE_VM_CPU = "cpu_util";
	public static final String MONITOR_HISTORY_DATA_TYPE_VM_MEMORY = "memory.usage";
	public static final String MONITOR_HISTORY_DATA_TYPE_VM_DISK = "disk.usage";
	public static final String MONITOR_HISTORY_DATA_TYPE_VM_INTERNALNET_OUT = "inner.network.outgoing.bytes";
	public static final String MONITOR_HISTORY_DATA_TYPE_VM_INTERNALNET_IN = "inner.network.incoming.bytes";
	public static final String MONITOR_HISTORY_DATA_TYPE_VM_EXTERNALNET_OUT = "outer.network.outgoing.bytes";
	public static final String MONITOR_HISTORY_DATA_TYPE_VM_EXTERNALNET_IN = "outer.network.incoming.bytes";
	
	public static final String MONITOR_HISTORY_DATA_TYPE_PHYSICAL_CPU = "physical.cpu.usage";
	public static final String MONITOR_HISTORY_DATA_TYPE_PHYSICAL_MEMORY = "physical.memory.usage";
	public static final String MONITOR_HISTORY_DATA_TYPE_PHYSICAL_DISK = "physical.disk.usage";
	public static final String MONITOR_HISTORY_DATA_TYPE_PHYSICAL_NET_OUT = "physical.net.out";
	public static final String MONITOR_HISTORY_DATA_TYPE_PHYSICAL_NET_IN = "physical.net.in";
	
	
	public static final String MONITOR_RULE_NET_UNIT_MB = "MB";
	public static final String MONITOR_RULE_NET_UNIT_PERCENTAGE = "%";
	
	public static final String ZABBIX_ADMIN_NOTIFICATIONlIST_ID = "Admin";
	
	public static final String NOTIFICATIONSLIST_TERMINAL_TYPE_SMS = "SMS";
	public static final String NOTIFICATIONSLIST_TERMINAL_TYPE_EMAIL = "EMail";
	
	public static final String CLOUDSERVICE_TYPE_VPN = "vpn";
	public static final String CLOUDSERVICE_TYPE_LOADBALANCER = "loadBalancer";
	public static final String CLOUDSERVICE_TYPE_FIREWALL = "fireWall";
	
	public static final String MONITOR_OBJS_OPERATION_TYPE_ADD = "add";
	public static final String MONITOR_OBJS_OPERATION_TYPE_DELETE = "delete";
	
	private DictConstant(){}
}
