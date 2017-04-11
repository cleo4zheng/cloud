package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class PhysNode {

	private String id;
	private String uuid;
	private String name;
	private String chassis_uuid;
	private String instance_uuid;
	private String driver;
	private String power_state;
	private String provision_state;
	private String provision_updated_at;
	private String raid_config;
	private String reservation;
	private String resource_class;
	private String target_power_state;
	private String target_provision_state;
	private String target_raid_config;
	private String network_interface;
	private String created_at;
	private String updated_at;
	private String inspection_finished_at;
	private String inspection_started_at;
	private Boolean console_enabled;
	private Boolean maintenance;
    private DriverInfo driver_info;
    private String driver_info_str;
    private String properties;
    private Long millionSeconds;
    
    public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	public String getProperties() {
		return properties;
	}

	public void setProperties(String properties) {
		this.properties = properties;
	}

	public String getDriver_info_str() {
		return driver_info_str;
	}

	public void setDriver_info_str(String driver_info_str) {
		this.driver_info_str = driver_info_str;
	}
	
	public String getMemory_mb() {
		return memory_mb;
	}

	public void setMemory_mb(String memory_mb) {
		this.memory_mb = memory_mb;
	}

	public String getLocal_gb() {
		return local_gb;
	}

	public void setLocal_gb(String local_gb) {
		this.local_gb = local_gb;
	}

	public String getCpus() {
		return cpus;
	}

	public void setCpus(String cpus) {
		this.cpus = cpus;
	}

	public String getCpu_arch() {
		return cpu_arch;
	}

	public void setCpu_arch(String cpu_arch) {
		this.cpu_arch = cpu_arch;
	}

	private String memory_mb;
    private String local_gb;
    private String cpus;
    private String cpu_arch;
    
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getChassis_uuid() {
		return chassis_uuid;
	}

	public void setChassis_uuid(String chassis_uuid) {
		this.chassis_uuid = chassis_uuid;
	}

	public String getInstance_uuid() {
		return instance_uuid;
	}

	public void setInstance_uuid(String instance_uuid) {
		this.instance_uuid = instance_uuid;
	}

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public String getPower_state() {
		return power_state;
	}

	public void setPower_state(String power_state) {
		this.power_state = power_state;
	}

	public String getProvision_state() {
		return provision_state;
	}

	public void setProvision_state(String provision_state) {
		this.provision_state = provision_state;
	}

	public String getProvision_updated_at() {
		return provision_updated_at;
	}

	public void setProvision_updated_at(String provision_updated_at) {
		this.provision_updated_at = provision_updated_at;
	}

	public String getRaid_config() {
		return raid_config;
	}

	public void setRaid_config(String raid_config) {
		this.raid_config = raid_config;
	}

	public String getReservation() {
		return reservation;
	}

	public void setReservation(String reservation) {
		this.reservation = reservation;
	}

	public String getResource_class() {
		return resource_class;
	}

	public void setResource_class(String resource_class) {
		this.resource_class = resource_class;
	}

	public String getTarget_power_state() {
		return target_power_state;
	}

	public void setTarget_power_state(String target_power_state) {
		this.target_power_state = target_power_state;
	}

	public String getTarget_provision_state() {
		return target_provision_state;
	}

	public void setTarget_provision_state(String target_provision_state) {
		this.target_provision_state = target_provision_state;
	}

	public String getTarget_raid_config() {
		return target_raid_config;
	}

	public void setTarget_raid_config(String target_raid_config) {
		this.target_raid_config = target_raid_config;
	}

	public String getNetwork_interface() {
		return network_interface;
	}

	public void setNetwork_interface(String network_interface) {
		this.network_interface = network_interface;
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

	public String getInspection_finished_at() {
		return inspection_finished_at;
	}

	public void setInspection_finished_at(String inspection_finished_at) {
		this.inspection_finished_at = inspection_finished_at;
	}

	public String getInspection_started_at() {
		return inspection_started_at;
	}

	public void setInspection_started_at(String inspection_started_at) {
		this.inspection_started_at = inspection_started_at;
	}

	public Boolean getConsole_enabled() {
		return console_enabled;
	}

	public void setConsole_enabled(Boolean console_enabled) {
		this.console_enabled = console_enabled;
	}

	public Boolean getMaintenance() {
		return maintenance;
	}

	public void setMaintenance(Boolean maintenance) {
		this.maintenance = maintenance;
	}

	public DriverInfo getDriver_info() {
		return driver_info;
	}

	public void setDriver_info(DriverInfo driver_info) {
		this.driver_info = driver_info;
	}

}
