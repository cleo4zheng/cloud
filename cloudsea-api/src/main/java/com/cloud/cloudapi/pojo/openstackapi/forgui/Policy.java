package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class Policy {

	class Lifetime {
		private String units;
		private String value;

		public Lifetime(String units,String value){
			this.units = units;
			this.value = value;
		}
		
		public String getUnits() {
			return units;
		}

		public void setUnits(String units) {
			this.units = units;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

	private String id;
	private String name;
	private String tenant_id;
	private String auth_algorithm;
	private String encryption_algorithm;
	private String pfs;
	private String description;
	private Lifetime lifetime;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTenant_id() {
		return tenant_id;
	}

	public void setTenant_id(String tenant_id) {
		this.tenant_id = tenant_id;
	}

	public String getAuth_algorithm() {
		return auth_algorithm;
	}

	public void setAuth_algorithm(String auth_algorithm) {
		this.auth_algorithm = auth_algorithm;
	}

	public String getEncryption_algorithm() {
		return encryption_algorithm;
	}

	public void setEncryption_algorithm(String encryption_algorithm) {
		this.encryption_algorithm = encryption_algorithm;
	}

	public String getPfs() {
		return pfs;
	}

	public void setPfs(String pfs) {
		this.pfs = pfs;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Lifetime getLifetime() {
		return lifetime;
	}

	public void setLifetime(Lifetime lifetime) {
		this.lifetime = lifetime;
	}

	public void makeLifetime(String units,String value) {
		this.lifetime = new Lifetime(units,value);
	}
}
