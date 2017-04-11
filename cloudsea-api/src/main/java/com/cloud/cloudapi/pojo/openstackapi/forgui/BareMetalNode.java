package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class BareMetalNode {

	private String id;
	private String reservation;
	private String driver;
	private String uuid;
	private String provision_updated_at;
	private String provision_state;
	private Boolean maintenance;
	private Boolean console_enabled;
	private String name;
	private String created_at;
	private String updated_at;
	private String maintenance_reason;
	private String instance_uuid;
	private String power_state;
	private String target_power_state;
	private String target_provision_state;
	private String inspection_started_at;
	private String inspection_finished_at;
	private String last_error;
	private DriverInfo driver_info;
	private BareMetalPropertyInfo properties;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getReservation() {
		return reservation;
	}

	public void setReservation(String reservation) {
		this.reservation = reservation;
	}

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	
	public String getInspection_started_at() {
		return inspection_started_at;
	}

	public void setInspection_started_at(String inspection_started_at) {
		this.inspection_started_at = inspection_started_at;
	}

	public String getInspection_finished_at() {
		return inspection_finished_at;
	}

	public void setInspection_finished_at(String inspection_finished_at) {
		this.inspection_finished_at = inspection_finished_at;
	}

	public String getLast_error() {
		return last_error;
	}

	public void setLast_error(String last_error) {
		this.last_error = last_error;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getProvision_updated_at() {
		return provision_updated_at;
	}

	public void setProvision_updated_at(String provision_updated_at) {
		this.provision_updated_at = provision_updated_at;
	}

	public String getProvision_state() {
		return provision_state;
	}

	public void setProvision_state(String provision_state) {
		this.provision_state = provision_state;
	}

	public Boolean getMaintenance() {
		return maintenance;
	}

	public void setMaintenance(Boolean maintenance) {
		this.maintenance = maintenance;
	}

	public Boolean getConsole_enabled() {
		return console_enabled;
	}

	public void setConsole_enabled(Boolean console_enabled) {
		this.console_enabled = console_enabled;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public String getMaintenance_reason() {
		return maintenance_reason;
	}

	public void setMaintenance_reason(String maintenance_reason) {
		this.maintenance_reason = maintenance_reason;
	}

	public String getInstance_uuid() {
		return instance_uuid;
	}

	public void setInstance_uuid(String instance_uuid) {
		this.instance_uuid = instance_uuid;
	}

	public String getPower_state() {
		return power_state;
	}

	public void setPower_state(String power_state) {
		this.power_state = power_state;
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

	public DriverInfo getDriver_info() {
		return driver_info;
	}

	public void setDriver_info(DriverInfo driver_info) {
		this.driver_info = driver_info;
	}

	public BareMetalPropertyInfo getProperties() {
		return properties;
	}

	public void setProperties(BareMetalPropertyInfo properties) {
		this.properties = properties;
	}

}
