package com.cloud.cloudapi.util;

public class ParamConstant {

	public static final String OS_DEFAULT_REGION="RegionOne";
	public static final String OS_CLOUDUSER_ROLE="user";
    public static final String API2OS_PREFIX_TENANT="cloud_tenant_";
    public static final String API2OS_PREFIX_USER="cloud_user_";
    public static final String API2OS_PREFIX_USER_PWD="cloud_userpwd_";
    
	public static final String AUTH_TOKEN = "X-ApiAuth-Token";
	public static final String OPENSTACK_AUTH_TOKEN = "X-Auth-Token";
	public static final String LIMIT = "limit";
	public static final String NAME = "name";
	public static final String DISPLAYNAME = "displayName";
	public static final String OWNER = "owner";
	public static final String STATUS = "status";
	public static final String VISIBILITY = "visibility";
    public static final String TENANT_ID = "tenant_id";
    public static final String TENANT = "tenant";
	public static final String PUBLIC_KEY = "public_key";
	public static final String DISK = "disk";
	public static final String VOLUME = "volume";
	public static final String LOCAL = "local";
	public static final String BACKUP = "backup";
	public static final String BACKEND = "backend";
	public static final String BAREMETAL = "baremetal";
	public static final String BAREMETALPORT = "baremetalPort";
	public static final String PRICE = "price";
	public static final String AGGREGATION = "aggregation";
	public static final String BILLING = "billing";
	public static final String CORE_NUMBERS = "core_numbers";
	public static final String MEMORY_SIZE = "memory_size";
	public static final String DISK_INFO = "disk_info";
	public static final String PERFORMANCE_DISK = "performanceDisk";
	public static final String HIGH_PERFORMANCE_DISK = "highPerformanceDisk";
	public static final String CAPACITY_DISK = "capacityDisk";
	public static final String SERVICE_NAME = "service";
	public static final String HOST_NAME = "host_name";
	public static final String TOPOLOGY = "topology";
	public static final String ROLE = "role";
	public static final String USER = "user";
	public static final int TOTAL_RES = 1;
	public static final int USED_RES = 2;
	public static final int FREE_RES = 3;
	public static final int MB = 1024;
	public static final int IMAGE_GB_UNIT = 1024*1024*1024;
	public static final String MBPS = "Mbps";
	public static final String SUPPORT_IP_VERSION = "4";
	public static final int NORMAL_SYNC_RESPONSE_CODE = 200;
	public static final int NORMAL_CREATE_RESPONSE_CODE = 201;
	public static final int NORMAL_GET_RESPONSE_CODE = 203;
	public static final int NORMAL_CREATE_RESPONSE_CODE_WITHOUT_RESPONSE = 204;
	public static final int NORMAL_DELETE_RESPONSE_CODE = 204;
	public static final int NORMAL_ASYNC_RESPONSE_CODE = 202;
	public static final int BAD_REQUEST_RESPONSE_CODE = 400;
	public static final int UN_AUTHHORIZED_RESPONSE_CODE = 401;
	public static final int SERVICE_FORBIDDEN_RESPONSE_CODE = 403;
	public static final int NOT_FOUND_RESPONSE_CODE = 404;
	public static final int ALREADLAY_EXIST_RESPONSE_CODE = 409;
	public static final int SERVICE_UNAVAILABLE_RESPONSE_CODE = 503;
	public static final int SERVICE_ERROR_RESPONSE_CODE = 500;
	public static final String NOTIFICATION = "notification";
	public static final String MAIL = "mail";
	public static final String VMDK = "vmdk";
	public static final String ENCODEING = "UTF-8";
	public static final String POOL = "pool";
	public static final String USERID = "userId";
	public static final String ROLEID = "roleId";
	
	/****** network type******/
	public static final String BGP_NETWORK = "bgp_net";
	public static final String TELECOM_NETWORK = "telcom_net";
	public static final String UNICOM_NETWORK = "unicom_net";
	public static final String MOBILE_NETWORK = "mobile_net";
	public static final String NETWORK_ID = "network_id";
	public static final String BGP = "BGP";
	public static final String CMCC = "CMCC";
	public static final String CTCC = "CTCC";
	public static final String CUCC = "CUCC";
	
	public static final String RAM_LIMIT = "ram";
	public static final String FLOATING_IPS_LIMIT = "floating_ips";
	public static final String FLOATING_IPS = "floating_ips";
	public static final String CORES_LIMIT = "limit";
    
	public static final String ACTIVE_STATUS = "ACTIVE";
	public static final String INUSE_STATUS = "IN-USE";
	public static final String DELETED_STATUS = "DELETED";
	public static final String STORAGE_BACKEND_NAME = "volume_backend_name";
	public static final String STOPPED_STATUS = "STOPPED";
	public static final String INACTIVE_STATUS = "INACTIVE";
	public static final String DOWN_STATUS = "DOWN";
	public static final String SUSPENDED_STATUS = "SUSPENDED";
	public static final String PAUSED_STATUS = "PAUSED";
	public static final String RESIZE_STATUS = "RESIZED";
	public static final String CREATE_COMPLETE = "CREATE_COMPLETE";
	public static final String UPDATED_STATUS = "UPDATED";
	public static final String AVAILABLE_STATUS = "AVAILABLE";
//	public static final String STORAGE = "volume_backend_capcacity_info";
	public static final String STORAGE = "storage";
    public static final String RAM = "ram";
    public static final String CORE = "core";
    public static final String VCPUS = "vcpus";
    public static final String SNAPSHOT = "snapshot";
    public static final String ID = "id";
    public static final String SWAP = "swap";
    public static final String RXTX_FACTOR = "rxtx_factor";
    public static final String SNAPSHOT_TYPE_IMAGE="snapshot";
    
    public static final String IMAGE_TYPE = "image";
    public static final String SNAPSHOT_TYPE = "snapshot";
    public static final String BLANK_TYPE = "blank";
    public static final String LOCAL_TYPE = "local";
    public static final String IMAGEREF = "imageRef";
    public static final String FLAVORREF = "flavorRef";
    public static final String KEY_NAME = "key_name";
    public static final String ADMINPASS = "adminPass";
    public static final String USER_DATA = "user_data";
    public static final String AVAILABILITY_ZONE = "availability_zone";
    public static final String MIN_COUNT = "min_count";
    public static final String MAX_COUNT = "max_count";
    public static final String UUID = "uuid";
    public static final String EXTERNAL_NET_UUID = "external_net_id";
    public static final String SOURCE_TYPE = "source_type";
    public static final String DESTINATION = "destination_type";
    public static final String BOOT_INDEX = "boot_index";
    public static final String DELETE_ON_TERMINATION = "delete_on_termination";
    public static final String FIXED_IP = "fixed_ip";
    public static final String PORT = "port";
    public static final String PORT_ID = "port_id";
    public static final String SERVER = "server";
    public static final String BLOCK_MIGRATION = "block_migration";
    public static final String BLOCK_DEVICE_MAPPING_V2 = "block_device_mapping_v2";
    public static final String NETWORKS = "networks";
    public static final String IP_VERSION = "ip_version";
    public static final String CIDR = "cidr";
    public static final String BASIC_NET = "basic";
    public static final String PRIVATE_NET = "private";
    public static final String PASSWORD_CREDENTIAL = "password";
    public static final String KEYPAIR_CREDENTIAL = "keypair";
    public static final String METADATA = "metadata";
    public static final String VOLUME_TYPE = "volume_type";
    public static final String VOLUME_SIZE = "volume_size";
    public static final String FIXED = "fixed";
    public static final String CREATE_IMAGE_ACTION = "createImage";
    public static final String PAUSE_INSTANCE_ACTION = "pause";
    public static final String UNPAUSE_INSTANCE_ACTION = "unpause";
    public static final String SOFT_REBOOT_INSTANCE_ACTION = "restart";
    public static final String HARD_REBOOT_INSTANCE_ACTION = "forceRestart";
    public static final String START_INSTANCE_ACTION = "start";
    public static final String STOP_INSTANCE_ACTION = "stop";
    public static final String SUSPEND_INSTANCE_ACTION = "suspend";
    public static final String RESUME_INSTANCE_ACTION = "resume";
    public static final String GET_SNAPSHOT_ACTION = "getSnapshot";
    public static final String GET_VNCCONSOLE_ACTION = "os-getVNCConsole";
    public static final String GET_SPICECONSOLE_ACTION = "os-getSPICEConsole";
    public static final String GET_SERIALCONSOLE_ACTION = "os-getSerialConsole";
    public static final String GET_RDPCONSOLE_ACTION = "os-getRDPConsole";
    public static final String ADD_SECURITYGROUP_ACTION = "addSecurityGroup";
    public static final String REMOVE_SECURITYGROUP_ACTION = "removeSecurityGroup";
    public static final String ADD_FLOATINGIP_ACTION = "addFloatingIp";
    public static final String REMOVE_FLOATINGIP_ACTION = "removeFloatingIp";
    public static final String ADD_FIXEDIP_ACTION = "addFixedIp";
    public static final String REMOVE_FIXEDIP_ACTION = "removeFixedIp";
    public static final String RESIZE_ACTION = "resize";
    public static final String LIVE_MIGRATION_ACTION = "os-migrateLive";
    public static final String VNCCONSOLE = "novnc";
    public static final String SPICECONSOLE = "spice-html5";
    public static final String SERIALCONSOLE = "serial";
    public static final String TYPE = "type";
    public static final String READ = "read";
    public static final String INSTANCE = "instance";
    public static final String VOLUME_ATTACHMENT = "volume";
    public static final String INTERFACE_ATTACHMENT = "port";
    public static final String FLAVOR = "flavor";
    public static final String QUOTA = "quota";
    public static final String HOST = "host";
    public static final String HOST_AGGREGATE = "hostAggregate";
    public static final String ROUTER = "router";
    public static final String DEVICE = "device";
    public static final String IMAGES = "images";
    public static final String IMAGE = "image";
    public static final String KEYPAIR = "keypair";
    public static final String SUBNET = "subnet";
    public static final String SUBNET_ID = "subnet_id";
    public static final String RESOURCE = "resource";
    public static final String FLOATINGIP = "floatingip";
    public static final String FLOATING_TYPE = "floatingType";
    public static final String FLOATINGIPCONFIG = "floatingipConfig";
    public static final String NETWORK = "network";
    public static final String SECURITYGROUP = "securityGroup";
    public static final String SECURITYGROUP_RULE = "securityGroupRule";
    public static final String VOLUMETYPE = "volumeType";
    public static final String INSTANCECONFIG = "instanceConfig";
    public static final Integer BYTE2MEGA = 1024*1024;
    public static final String FIREWALL = "firewall";
    public static final String VPN = "vpn";
    public static final String IKE = "ike";
    public static final String IPSEC = "ipsec";
    public static final String IPSEC_SITE_CONNECTION = "ipsecSiteConnection";
    public static final String LOADBALANCER = "loadbalancer";
    public static final String LISTENER = "listener";
    public static final String FIREWALL_POLICY = "firewallPolicy";
    public static final String FIREWALL_RULE = "firewallRule";
    public static final String ACCOUNTID = "accountId";
    public static final String INSTANCEID = "instanceId";
    public static final String DATABASE = "database";
    public static final String DESCRIPTION = "description";
    public static final String VOLUME_TYPE_PUBLIC_ACCESS = "os-volume-type-access:is_public";
    
    public static final String DOMAIN_ID = "domain_id";
    public static final String PARENT_ID = "parent_id";
    public static final String DATABASEINSTANCE = "databaseInstance";
    public static final String DATABASEINSTANCE_USER = "instanceUser";
    public static final String CORES = "cores";
    public static final String CHASSIS = "chassis";
    public static final String NODE = "node";
    public static final String QOSPOLICY = "qospolicy";
    public static final String QOSBANDWITH = "bandwith";
    public static final String COMPUTE = "compute";
    public static final String IRONIC = "ironic";
    public static final String CONTAINER = "container";
    public static final String CONTAINER_MODEL = "containerModel";
    public static final String RATING_TEMPLATE = "ratingTemplate";
    public static final String CURRENCY = "currency";
    public static final String RATING_VERSION = "ratingVersion";
    public static final String TENANT_RATING = "tenantRating";
    public static final String RATING_SERVICE = "ratingService";
    public static final String RATING_POLICY = "ratingPolicy";
    public static final String EXTEND_SERVICE = "extendservice";
    public static final String STACK = "stack";    /*************resource status *************/
    public static final String IN_USE = "in-use";
    public static final String AVAILABLE = "available";
    public static final String RESTORING = "restoring";
    public static final String SKIN = "skin";
    
    /****************default zone**********************/
    public static final String KVM_ZONE = "kvm-zone";
    public static final String VMWARE_ZONE = "vmware-zone";
    public static final String VDI_ZONE = "vdi-zone";
    public static final String BAREMETAL_ZONE = "baremetal-zone";
    public static final String CONTAINER_ZONE = "container-zone";
    public static final String DATABASE_ZONE = "database-zone";
    public static final String GENERAL_ZONE = "general*zone";
    public static final String GENERAL_VOLUME = "general*volume";
    public static final String OPENSTACK_ZONE = "OpenStack";
    /*************aggregation info status *************/
    public static final String INFO = "info";
    public static final String DANGER = "danger";
    public static final String SUCCESS = "success";
    public static final String WARNING = "warning";
    
    /*************loadblancer service *************/
    public static final String LBPOOL = "loadbalancerPool";
    public static final String LBPOOL_MEMBER = "loadbalancerPoolMember";
    public static final String HEALTH_MONITOR = "loadbalancerHealthMonitor";
    public static final String LBVIP = "loadbalancerVp";
    
    public static final String SERVICE="service";
    /****** workflow******/
    //创建process instance时，jsonbody的key�?
    public static final String PROCESS_JSON_KEY = "params";
    
    public static final int EIGHT_BIT_MASK = 8;
    public static final int SIXTEEN_BIT_MASK = 16;
    public static final int TWENTYFOUR_BIT_MASK = 24;
    
    /*********************MONITOR*ZABBIX******/
    public static final String MONITOR="monitor";
    public static final String NOTIFICATIONLIST="notificationlist";
    public static final String SERVICE_LIST = "service_list";
    public static final String SERVICE_ID = "service_id";
    public static final String RULEGRPNAME = "rulegrpname";
    public static final String NOTIFICAT_ID = "notificat_id";
    public static final String INSTANCE_LIST = "instance_list";
    public static final String RULELIST = "rulelist";
    public static final String PRIORITY = "priority";
	public static final String TENANT_LIST = "tenant_list";
	public static final String IDS = "ids";
	public static final String ADDR = "addr";
	public static final String ACTIVE = "active";
	public static final String ADDR_LIST = "addr_list";
	public static final String MOBILE_LIST = "mobile_list";
	public static final String RULEGRPNAMELIST = "rulegrpnamelist";
	public static final String START_TIME = "starttime";
	public static final String END_TIME = "endtime";
	public static final String INSTANCE_ID = "instance_id";
	public static final String DURATION = "duration";
	public static final String PHY_HOST = "phy_host";
	public static final String INACTIVE = "inactive";
	public static final String TRUE = "true";
	public static final String PROJECT = "project";
	public static final String IPV4="IPv4";
	public static final String IPV6="IPv6";
	public static final String DURATION_START= "duration_start";
	public static final String UNIT = "unit";
	public static final String AVG = "avg";
	public static final String DEFAULT = "default";
	
	/*************billing************************/
	public static final String BILLING_MONTH_UNTIL = "billing_month_until";
	public static final String BILLING_MONTH = "billing_month";
	public static final String DEFAULT_CURRENCY = "yuan";
	public static final String LBAAS = "LBAAS";
	public static final String VPNAAS = "VPNAAS";
	public static final String FWAAS = "FWAAS";
	public static final String MAAS = "MAAS";
	public static final String DBAAS = "DBAAS";
	public static final String CAAS = "CAAS";
	/*********node type for topology***********/
	public static final String EXTERNAL_NETWORK_TYPE = "EXTERNAL_NETWORK";
	public static final String INTERNAL_NETWORK_TYPE = "NETWORK";
	public static final String INSTANCE_TYPE = "SERVER";
	public static final String ROUTER_TYPE = "ROUTER";
	public static final String BAREMETAL_TYPE = "BAREMETAL";
	public static final String VDI_TYPE = "VDI";
	public static final String DATABASE_TYPE = "DATABASE";
	public static final String CONTAINER_TYPE = "CONTAINER";
	public static final String VMWARE_TYPE = "VMWARE";
	/************image type******************************/
	public static final String CENTOS = "Centos";
	public static final String UBUNTU = "Ubuntu";
	public static final String WINDOWS = "windows";
	public static final String FEDORA = "Fedora";
	public static final String MYSQL = "MySQL";
	public static final String MONOGODB = "Mongodb";
	public static final String POSTGRE = "Postgre";
	public static final String PHYSICAL = "Baremetal";
	public static final String DOCKER = "Container";
	
	public static final String TIME_FORMAT_01 = "yyyy-MM-dd HH:mm:ss";
	public static final String TIME_FORMAT_02 = "yyyy-MM-dd'T'HH:mm:ss";
	
	/***************ironic constant***********************/
	public static final String IRONIC_API_VERSION_STRING = "X-OpenStack-Ironic-API-Version";
	public static final String IRONIC_API_VERSION_VALUE = "1.9";
	public static final String PXE_IPMITOOL_DRIVER = "pxe_ipmitool";
	public static final String BOOT_LOCAL = "boot_option:local";
	public static final String IPMI_TERMINAL_PORT="623";
	public static final String CPU_ARCH_64 = "x86_64";
	public static final String CPU_ARCH_86 = "x86";
	/*******************database type****************************/
	public static final String DBTYPE_SQL = "sql";
	public static final String DBTYPE_NOSQL = "nosql";
	
	/***************user manager constant***********************/
	public static final String ENABLE_USER_ACTION = "enable"; //启用用户
	public static final String DISABLE_USER_ACTION = "disable";//禁用用户
	public static final String RESETPASSWORD_USER_ACTION = "resetPassword"; //reset密码
	public static final String BOND_DDH = "bondddh";//绑定订单�?
	public static final String DEFAULT_PASSWORD="123456";
	
	/***************instance volume ip rating***********************/
	public static final String CAPACITY = "capacity";
	public static final String PERFORMANCE = "performance";
	public static final String CONTAINER_RAM = "container-zone_ram";
	public static final String CONTAINER_CORE = "container-zone_core";
	public static final String VDI_RAM = "vdi-zone_ram";
	public static final String VDI_CORE = "vdi-zone_core";
	public static final String KVM_RAM = "kvm-zone_ram";
	public static final String KVM_CORE = "kvm-zone_core";
	public static final String VMWARE_RAM = "vmware-zone_ram";
	public static final String VMWARE_CORE = "vmware-zone_core";
	public static final String BAREMETAL_RAM = "baremetal-zone_ram";
	public static final String BAREMETAL_CORE = "baremetal-zone_core";
	public static final String DATABASE_RAM = "database-zone_ram";
	public static final String DATABASE_CORE = "database-zone_core";
	public static final String GENERAL_RAM = "general-zone_ram";
	public static final String GENERAL_CORE = "general-zone_core";
	public static final String MB_UNIT = "MB";
	public static final String GB_UNIT = "GB";
	public static final String CORE_UNIT = "Core";
	public static final String HOUR_UNIT = "hour";
	public static final String WIN7X64 = "Win7X64";
	public static final String WIN7VMDK = "Win7-vmdk";
	public static final String WINDOWS7CN = "Windows7CN";
	public static final String WIN2012R2CN = "Win2012R2CN";
	public static final String WIN2012CN = "Win2012CN";
	public static final String IMAGE_UNIT = "image_count";
	public static final String IP_UNIT = "ip_count";
	public static final String EXTRA_SPECS = "extra_specs";
	public static final String VOLUME_BACKEND_NAME = "volume_backend_name";
	
	//permission
	public static final String BACKUP_NEW = "backup_new";
	public static final String BACKUP_DELETE = "backup_delete";
	
	public static final String VOLUME_NEW = "volume_new";
	public static final String VOLUME_RESTORE = "volume_restore";
	public static final String VOLUME_UPDATE = "volume_update";
	public static final String VOLUME_DELETE = "volume_delete";
	
	public static final String NETWORK_NEW = "network_new";
	public static final String NETWORK_UPDATE = "network_update";
	public static final String NETWORK_DELETE = "network_delete";
	
	public static final String SUBNET_NEW = "subnet_new";
	public static final String SUBNET_UPDATE = "subnet_update";
	public static final String SUBNET_DELETE = "subnet_delete";
	
	public static final String ROUTER_NEW = "router_new";
	public static final String ROUTER_ENABLE_GATEWAY = "router_openFloatingIP";
	public static final String ROUTER_DISABLE_GATEWAY = "router_closeFloatingIP";
	public static final String ROUTER_UPDATE = "router_update";
	public static final String ROUTER_DELETE = "router_delete";
	public static final String ROUTER_ADD_SUBNET = "router_addSubnet";
	public static final String ROUTER_REMOVE_SUBNET = "router_removeSubnet";
	public static final String ROUTER_ADD_PORT = "router_addPort";
	public static final String ROUTER_REMOVE_PORT = "router_removePort";
	
	public static final String KEYPAIR_NEW = "keypair_new";
	public static final String KEYPAIR_UPLOAD = "keypair_upload";
	public static final String KEYPAIR_DOWNLOAD = "keypair_download";
	public static final String KEYPAIR_DELETE = "keypair_delete";
	
	public static final String LOADBALANCER_NEW = "loadBalancer_new";
	public static final String LOADBALANCER_UPDATE = "loadBalancer_update";
	public static final String LOADBALANCER_ENABLE = "loadBalancer_enable";
	public static final String LOADBALANCER_DISABLE = "loadBalancer_disable";
	public static final String LOADBALANCER_DELETE = "loadBalancer_delete";
	public static final String LOADBALANCER_ADD_FLOATINGIP = "loadBalancer_addFIP";
	public static final String LOADBALANCER_REMOVE_FLOATINGIP = "loadBalancer_removeFIP";
	
	public static final String FIREWALL_NEW = "firewall_new";
	public static final String FIREWALL_UPDATE = "firewall_update";
	public static final String FIREWALL_ENABLE = "firewall_enable";
	public static final String FIREWALL_DISABLE = "firewall_disable";
	public static final String FIREWALL_DELETE = "firewall_delete";
	public static final String FIREWALL_ADD_ROUTER = "firewall_addRouter";
	public static final String FIREWALL_REMOVE_ROUTER = "firewall_removeRouter";
	
	public static final String VPN_NEW = "vpn_new";
	public static final String VPN_UPDATE = "vpn_update";
	public static final String VPN_ENABLE = "vpn_enable";
	public static final String VPN_DISABLE = "vpn_disable";
	public static final String VPN_DELETE = "vpn_delete";
	
	public static final String FLOATINGIP_NEW = "floatingIP_new";
	public static final String FLOATINGIP_UPDATE = "floatingIP_update";
	public static final String FLOATINGIP_DELETE = "floatingIP_delete";
	
	public static final String PORT_NEW = "port_new";
	public static final String PORT_UPDATE = "port_update";
	public static final String PORT_DELETE = "port_delete";
	
	public static final String SECURITYGROUP_NEW = "securityGroup_new";
	public static final String SECURITYGROUP_UPDATE = "securityGroup_update";
	public static final String SECURITYGROUP_DELETE = "securityGroup_delete";
	public static final String SECURITYGROUP_ADD_RULE = "securityGroup_addRule";
	public static final String SECURITYGROUP_REMOVE_RULE = "securityGroup_removeRule";
	public static final String SECURITYGROUP_ADD_PORT = "securityGroup_addPort";
	public static final String SECURITYGROUP_REMOVE_PORT = "securityGroup_removePort";
	
	public static final String IMAGE_NEW = "image_new";
	public static final String IMAGE_DELETE = "image_delete";
	
	public static final String INSTANCE_NEW = "instance_new";
	public static final String INSTANCE_UPDATE = "instance_update";
	public static final String INSTANCE_START = "instance_start";
	public static final String INSTANCE_STOP = "instance_stop";
	public static final String INSTANCE_RESTART = "instance_restart";
	public static final String INSTANCE_FORCE_RESTART = "instance_forceRestart";
	public static final String INSTANCE_SUSPEND = "instance_suspend";
	public static final String INSTANCE_PAUSE = "instance_pause";
	public static final String INSTANCE_ADD_VOLUME = "instance_addVolume";
	public static final String INSTANCE_REMOVE_VOLUME = "instance_removeVolume";
	public static final String INSTANCE_ADD_SECURITYGROUP = "instance_addSecurityGroup";
	public static final String INSTANCE_REMOVE_SECURITYGROUP = "instance_removeSecurityGroup";
	public static final String INSTANCE_RECOVER = "instance_recover";
	public static final String INSTANCE_CONSOLE = "instance_console";
	public static final String INSTANCE_ADD_FLOATINGIP = "instance_addFloatingIP";
	public static final String INSTANCE_REMOVE_FLOATINGIP = "instance_removeFloatingIP";
	public static final String INSTANCE_RESIZE = "instance_resize";
	public static final String INSTANCE_ADD_PORT = "instance_addPort";
	public static final String INSTANCE_REMOVE_PORT = "instance_removePort";
	public static final String INSTANCE_DELETE = "instance_delete";
	
	public static final String VDIINSTANCE_NEW = "vdiInstance_new";
	public static final String VDIINSTANCE_UPDATE = "vdiInstance_update";
	public static final String VDIINSTANCE_START = "vdiInstance_start";
	public static final String VDIINSTANCE_STOP = "vdiInstance_stop";
	public static final String VDIINSTANCE_RESTART = "vdiInstance_restart";
	public static final String VDIINSTANCE_FORCE_RESTART = "vdiInstance_forceRestart";
	public static final String VDIINSTANCE_SUSPEND = "vdiInstance_suspend";
	public static final String VDIINSTANCE_PAUSE = "vdiInstance_pause";
	public static final String VDIINSTANCE_ADD_VOLUME = "vdiInstance_addVolume";
	public static final String VDIINSTANCE_REMOVE_VOLUME = "vdiInstance_removeVolume";
	public static final String VDIINSTANCE_ADD_SECURITYGROUP = "vdiInstance_addSecurityGroup";
	public static final String VDIINSTANCE_REMOVE_SECURITYGROUP = "vdiInstance_removeSecurityGroup";
	public static final String VDIINSTANCE_RECOVER = "vdiInstance_recover";
	public static final String VDIINSTANCE_CONSOLE = "vdiInstance_console";
	public static final String VDIINSTANCE_ADD_FLOATINGIP = "vdiInstance_addFloatingIP";
	public static final String VDIINSTANCE_REMOVE_FLOATINGIP = "vdiInstance_removeFloatingIP";
	public static final String VDIINSTANCE_RESIZE = "vdiInstance_resize";
	public static final String VDIINSTANCE_ADD_PORT = "vdiInstance_addPort";
	public static final String VDIINSTANCE_REMOVE_PORT = "vdiInstance_removePort";
	public static final String VDIINSTANCE_DELETE = "vdiInstance_delete";
	
	public static final String USER_NEW = "user_new";
	public static final String USER_ENABLE = "user_enable";
	public static final String USER_DISABLE = "user_disable";
	public static final String USER_RESET = "user_reset";
	public static final String USER_BIND_ROLE = "user_bindRole";
	public static final String USER_REMOVE_ROLE = "user_removeRole";
	public static final String USER_MODIFY_PASSWORD = "user_modifyPassword";
	
	public static final String ROLE_NEW = "role_new";
	public static final String ROLE_DELETE = "role_delete";
	public static final String ROLE_BIND_USER = "role_bindUser";
	public static final String ROLE_UPDATE = "role_update";
	
	public static final String COMMON_UPDATE_LOCALE = "common_updateLanguage";
	public static final String COMMON_UPDATE_REGION = "common_updateRegion";
	public static final String COMMON_UPDATE_COLOR = "common_updateColor";
	public static final String COMMON_UPDATE_TENANT = "common_updateTenant";
	
	public static final String VOLUMETYPE_NEW = "volumeType_new";
	public static final String VOLUMETYPE_DELETE = "volumeType_delete";
	public static final String VOLUMETYPE_UPDATE = "volumeType_update";
	
	public static final String TENANT_NEW = "tenant_new";
	public static final String TENANT_DELETE = "tenant_delete";
	public static final String TENANT_UPDATE = "tenant_update";
	public static final String TENANT_ADD_USER = "tenant_addUser";
	public static final String TENANT_REMOVE_USER = "tenant_removeUser";
	public static final String TENANT_UPDATE_USERROLE = "tenant_updateUserRole";
	
	public static final String POOL_NEW = "pool_new";
	public static final String POOL_APPLY = "pool_apply";
	public static final String STACK_NEW = "pool_newStack";
	public static final String STACK_DELETE = "pool_deleteStack";
	
	public static final String NOTIFICATION_READ = "notification_read";
	public static final String NOTIFICATION_UNREAD = "notification_unread";
	
	public static final String NOTIFICATION_LIST_NEW = "notificationList_new";
	public static final String NOTIFICATION_LIST_DELETE = "notificationList_delete";
	public static final String NOTIFICATION_LIST_UPDATE = "notificationList_update";
	public static final String NOTIFICATION_ADD_TERMINAL = "notificationList_addTerminal";
	public static final String NOTIFICATION_DELETE_TERMINAL = "notificationList_deleteTerminal";
	
	public static final String ALARM_NEW = "alarm_new";
	public static final String ALARM_UPDATE = "alarm_update";
	public static final String ALARM_ADD_RESOURCE = "alarm_addResource";
	public static final String ALARM_REMOVE_RESOURCE = "alarm_removeResource";
	public static final String ALARM_ADD_RULE = "alarm_addRule";
	public static final String ALARM_REMOVE_RULE = "alarm_removeRule";
	public static final String ALARM_ADD_NOTIFICATION = "alarm_addNotificationList";
	public static final String ALARM_REMOVE_NOTIFICATION = "alarm_removeNotificationList";
	public static final String ALARM_DELETE = "alarm_delete";
	public static final String ALARM_ENABLE = "alarm_enable";
	public static final String ALARM_DISABLE = "alarm_disable";
	
	public static final String BARE_METAL_NEW = "bareMetal_new";
	public static final String BARE_METAL_START = "bareMetal_start";
	public static final String BARE_METAL_STOP = "bareMetal_stop";
	public static final String BARE_METAL_DELETE = "bareMetal_delete";
	
	public static final String BARE_METAL_INSTANCE_NEW = "bareMetalInstance_new";
	public static final String BARE_METAL_INSTANCE_START = "bareMetalInstance_start";
	public static final String BARE_METAL_INSTANCE_STOP = "bareMetalInstance_stop";
	public static final String BARE_METAL_INSTANCE_DELETE = "bareMetalInstance_delete";
	
	public static final String CONTAINER_NEW = "container_new";
	public static final String CONTAINER_DELETE = "container_delete";
	public static final String CONTAINER_UPDATE = "container_update";

	public static final String RELATIONAL_DATABASE_NEW = "relationalDatabase_new";
	public static final String RELATIONAL_DATABASE_DELETE = "relationalDatabase_delete";
	public static final String RELATIONAL_DATABASE_START = "relationalDatabase_start";
	public static final String RELATIONAL_DATABASE_STOP = "relationalDatabase_stop";
	public static final String RELATIONAL_DATABASE_ADD_USER = "relationalDatabase_addUser";
	public static final String RELATIONAL_DATABASE_DELETE_USER = "relationalDatabase_deleteUser";
	public static final String RELATIONAL_DATABASE_ADD_DATABASE = "relationalDatabase_addDatabase";
	public static final String RELATIONAL_DATABASE_DELETE_DATABASE = "relationalDatabase_deleteDatabase";
	
	public static final String NON_RELATIONAL_DATABASE_NEW = "nonrelationalDatabase_new";
	public static final String NON_RELATIONAL_DATABASE_DELETE = "nonrelationalDatabase_delete";
	public static final String NON_RELATIONAL_DATABASE_START = "nonrelationalDatabase_start";
	public static final String NON_RELATIONAL_DATABASE_STOP = "nonrelationalDatabase_stop";
	public static final String NON_RELATIONAL_DATABASE_ADD_USER = "nonrelationalDatabase_addUser";
	public static final String NON_RELATIONAL_DATABASE_DELETE_USER = "nonrelationalDatabase_deleteUser";
	public static final String NON_RELATIONAL_DATABASE_ADD_DATABASE = "nonrelationalDatabase_addDatabase";
	public static final String NON_RELATIONAL_DATABASE_DELETE_DATABASE = "nonrelationalDatabase_deleteDatabase";
	
	
	public static final String PRICE_NEW = "pricing_new";
	public static final String PRICE_DELETE = "pricing_delete";
	public static final String PRICE_UPDATE = "pricing_update";
	public static final String PRICE_NEW_VERSION = "pricing_newVersion";
	public static final String PRICE_APPLY = "pricing_execute";
	
	public static final String BILLACCONT_NEW = "billAccount_new";
	public static final String BILLACCONT_DELETE = "billAccount_delete";
	public static final String BILLACCONT_SET_DEFAULT = "billAccount_setDefaultAccount";
	public static final String BILLACCONT_APPLY = "bill_updateAccount";
	
	public static final String AGGREGATE_NEW = "zone_new";
	public static final String AGGREGATE_DELETE = "zone_delete";
	public static final String AGGREGATE_UPDATE = "zone_update";
	public static final String AGGREGATE_ADD_HOST = "zone_addHost";
	public static final String AGGREGATE_REMOVE_HOST = "zone_removeHost";

	private ParamConstant(){}
}

