package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class ContainerModel {

	private String insecure_registry;
	private String http_proxy;
	private Boolean floating_ip_enabled;
	private String fixed_subnet;
	private String uuid;
	private String name;
	private String no_proxy;
	private String https_proxy;
	private Boolean tls_disabled;
	private String keypair_id;
	private Boolean publicFlag;
	private Integer docker_volume_size;
	private String server_type;
	private String external_network_id;
	private String cluster_distro;
	private String image_id;
	private String volume_driver;
	private Boolean registry_enabled;
	private String docker_storage_driver;
	private String apiserver_port;
	private String created_at;
	private String updated_at;
	private String network_driver;
	private String fixed_network;
	private String coe;
	private String flavor_id;
	private String master_flavor_id;
	private Boolean master_lb_enabled;
	private String dns_nameserver;
	private String tenantId;
	public Integer getCore() {
		return core;
	}

	public void setCore(Integer core) {
		this.core = core;
	}

	public Integer getRam() {
		return ram;
	}

	public void setRam(Integer ram) {
		this.ram = ram;
	}

	public Integer getLocal_volume_size() {
		return local_volume_size;
	}

	public void setLocal_volume_size(Integer local_volume_size) {
		this.local_volume_size = local_volume_size;
	}

	private Integer core;
	private Integer ram;
	private Integer local_volume_size;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getInsecure_registry() {
		return insecure_registry;
	}

	public void setInsecure_registry(String insecure_registry) {
		this.insecure_registry = insecure_registry;
	}

	public String getHttp_proxy() {
		return http_proxy;
	}

	public void setHttp_proxy(String http_proxy) {
		this.http_proxy = http_proxy;
	}

	public Boolean getFloating_ip_enabled() {
		return floating_ip_enabled;
	}

	public void setFloating_ip_enabled(Boolean floating_ip_enabled) {
		this.floating_ip_enabled = floating_ip_enabled;
	}

	public String getFixed_subnet() {
		return fixed_subnet;
	}

	public void setFixed_subnet(String fixed_subnet) {
		this.fixed_subnet = fixed_subnet;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getNo_proxy() {
		return no_proxy;
	}

	public void setNo_proxy(String no_proxy) {
		this.no_proxy = no_proxy;
	}

	public String getHttps_proxy() {
		return https_proxy;
	}

	public void setHttps_proxy(String https_proxy) {
		this.https_proxy = https_proxy;
	}

	public Boolean getTls_disabled() {
		return tls_disabled;
	}

	public void setTls_disabled(Boolean tls_disabled) {
		this.tls_disabled = tls_disabled;
	}

	public String getKeypair_id() {
		return keypair_id;
	}

	public void setKeypair_id(String keypair_id) {
		this.keypair_id = keypair_id;
	}


	public Integer getDocker_volume_size() {
		return docker_volume_size;
	}

	public void setDocker_volume_size(Integer docker_volume_size) {
		this.docker_volume_size = docker_volume_size;
	}

	public String getServer_type() {
		return server_type;
	}

	public void setServer_type(String server_type) {
		this.server_type = server_type;
	}

	public String getExternal_network_id() {
		return external_network_id;
	}

	public void setExternal_network_id(String external_network_id) {
		this.external_network_id = external_network_id;
	}

	public String getCluster_distro() {
		return cluster_distro;
	}

	public void setCluster_distro(String cluster_distro) {
		this.cluster_distro = cluster_distro;
	}

	public String getImage_id() {
		return image_id;
	}

	public void setImage_id(String image_id) {
		this.image_id = image_id;
	}

	public String getCreated_at() {
		return created_at;
	}

	public void setCreated_at(String created_at) {
		this.created_at = created_at;
	}

	public String getUpdated_at() {
		return updated_at;
	}

	public void setUpdated_at(String updated_at) {
		this.updated_at = updated_at;
	}

	public String getNetwork_driver() {
		return network_driver;
	}

	public void setNetwork_driver(String network_driver) {
		this.network_driver = network_driver;
	}

	public String getFixed_network() {
		return fixed_network;
	}

	public void setFixed_network(String fixed_network) {
		this.fixed_network = fixed_network;
	}

	public String getCoe() {
		return coe;
	}

	public void setCoe(String coe) {
		this.coe = coe;
	}

	public String getFlavor_id() {
		return flavor_id;
	}

	public void setFlavor_id(String flavor_id) {
		this.flavor_id = flavor_id;
	}

	public String getMaster_flavor_id() {
		return master_flavor_id;
	}

	public void setMaster_flavor_id(String master_flavor_id) {
		this.master_flavor_id = master_flavor_id;
	}

	public Boolean getMaster_lb_enabled() {
		return master_lb_enabled;
	}

	public void setMaster_lb_enabled(Boolean master_lb_enabled) {
		this.master_lb_enabled = master_lb_enabled;
	}

	public String getDns_nameserver() {
		return dns_nameserver;
	}

	public void setDns_nameserver(String dns_nameserver) {
		this.dns_nameserver = dns_nameserver;
	}

	public String getVolume_driver() {
		return volume_driver;
	}

	public void setVolume_driver(String volume_driver) {
		this.volume_driver = volume_driver;
	}

	public Boolean getRegistry_enabled() {
		return registry_enabled;
	}

	public void setRegistry_enabled(Boolean registry_enabled) {
		this.registry_enabled = registry_enabled;
	}

	public String getDocker_storage_driver() {
		return docker_storage_driver;
	}

	public void setDocker_storage_driver(String docker_storage_driver) {
		this.docker_storage_driver = docker_storage_driver;
	}

	public String getApiserver_port() {
		return apiserver_port;
	}

	public void setApiserver_port(String apiserver_port) {
		this.apiserver_port = apiserver_port;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public Boolean getPublicFlag() {
		return publicFlag;
	}

	public void setPublicFlag(Boolean publicFlag) {
		this.publicFlag = publicFlag;
	}
	
}
