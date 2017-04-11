package com.cloud.cloudapi.pojo.openstackapi.forgui;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PoolStack {
    private String id;
    @JsonIgnore
    private String name;
    @JsonIgnore 
    private String poolId;

    private String status;
    @JsonProperty("createdAt")
    private String createAt;

    private String updateAt;
    @JsonIgnore 
    private Integer core;
    @JsonIgnore 
    private Integer ram;
    @JsonIgnore 
    private String fip;
    @JsonIgnore 
    private String volume;
    @JsonIgnore 
    private String dbaas;
    @JsonIgnore 
    private String maas;
    @JsonIgnore 
    private String vpnaas;
    @JsonIgnore 
    private String lbaas;
    @JsonIgnore 
    private String fwaas;
    @JsonIgnore
    private String az;
    @JsonIgnore
    private Long millionSeconds;
    @JsonProperty("name")
    private String displayName;
    

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id == null ? null : id.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getPoolId() {
        return poolId;
    }

    public void setPoolId(String poolId) {
        this.poolId = poolId == null ? null : poolId.trim();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null ? null : status.trim();
    }

    public String getCreateAt() {
        return createAt;
    }

    public void setCreateAt(String createAt) {
        this.createAt = createAt == null ? null : createAt.trim();
    }

    public String getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(String updateAt) {
        this.updateAt = updateAt == null ? null : updateAt.trim();
    }

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

    public String getFip() {
        return fip;
    }

    public void setFip(String fip) {
        this.fip = fip == null ? null : fip.trim();
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume == null ? null : volume.trim();
    }

    public String getDbaas() {
        return dbaas;
    }

    public void setDbaas(String dbaas) {
        this.dbaas = dbaas == null ? null : dbaas.trim();
    }

    public String getMaas() {
        return maas;
    }

    public void setMaas(String maas) {
        this.maas = maas == null ? null : maas.trim();
    }

    public String getVpnaas() {
        return vpnaas;
    }

    public void setVpnaas(String vpnaas) {
        this.vpnaas = vpnaas == null ? null : vpnaas.trim();
    }

    public String getLbaas() {
        return lbaas;
    }

    public void setLbaas(String lbaas) {
        this.lbaas = lbaas == null ? null : lbaas.trim();
    }

    public String getFwaas() {
        return fwaas;
    }

    public void setFwaas(String fwaas) {
        this.fwaas = fwaas == null ? null : fwaas.trim();
    }

	public String getAz() {
		return az;
	}

	public void setAz(String az) {
		this.az = az;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
}