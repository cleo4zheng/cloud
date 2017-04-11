package com.cloud.cloudapi.pojo.openstackapi.forgui;

public class VolumeType {

//	class Metadata{
//		private String capabilities;
//        private String volume_backend_name;
//        
//		public String getVolume_backend_name() {
//			return volume_backend_name;
//		}
//
//		public void setVolume_backend_name(String volume_backend_name) {
//			this.volume_backend_name = volume_backend_name;
//		}
//
//		public Metadata(List<String> extraSpecs) {
//			this.volume_backend_name = extraSpecs.get(0);
//		}
//
//		public String getCapabilities() {
//			return capabilities;
//		}
//
//		public void setCapabilities(String capabilities) {
//			this.capabilities = capabilities;
//		}
//		
//	}
	private String id;
	private String name;
	private String description;
	private Boolean is_public;
	private String backendName;
	private String displayName;
	private Double unitPrice;
	
	public Boolean getIs_public() {
		return is_public;
	}

	public void setIs_public(Boolean is_public) {
		this.is_public = is_public;
	}

//	private Metadata extra_specs;

	public void setId(String id){
		this.id = id;
	}
	
	public String getId(){
		return this.id;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getDescription() {
		return description;
	}

	public Double getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(Double unitPrice) {
		this.unitPrice = unitPrice;
	}

	public void setDescription(String description) {
		this.description = description;
	}

//	public void setExtra_Specs(List<String> extraSpecs){
//		
//		if(null == extraSpecs || 1 != extraSpecs.size())
//			return;
//		this.extra_specs = new Metadata(extraSpecs);
//	}

	public String getBackendName() {
		return backendName;
	}

	public void setBackendName(String backendName) {
		this.backendName = backendName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

  
//	public Metadata getExtra_specs(){
//		return this.extra_specs;
//	}
//	
//	public String getBackendName(){
//		if(null == this.extra_specs)
//			return null;
//		return this.extra_specs.getVolume_backend_name();
//	}
	
	public void normalInfo(){
		this.setUnitPrice(null);
		this.setIs_public(null);
	}
}
