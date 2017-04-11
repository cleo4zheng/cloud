package com.cloud.cloudapi.json.forgui;

import java.util.ArrayList;
import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;


public class InstanceJSON {
	
	class Securitygroup{
		private String name;
		
		public Securitygroup(String name){
			this.name = name;
		}
		
		public void setName(String name){
			this.name = name;
		}
		
		public String getName(){
			return this.name;
		}
	}

	class BlockDeviceMappingV2{
		private String uuid;
		private String source_type;
		private String destination_type;
		private Integer boot_index;
		private Integer volume_size;
		private Boolean delete_on_termination;
		
		public BlockDeviceMappingV2(String uuid,String source_type,String destination_type,Integer boot_index,Integer volume_size,Boolean delete_on_termination){
			this.uuid = uuid;
			this.source_type = source_type;
			this.destination_type = destination_type;
			this.boot_index = boot_index;
			this.delete_on_termination = delete_on_termination;
			this.volume_size = volume_size;
		}
		
		public void setUuid(String uuid){
			this.uuid = uuid;
		}
		
		public String getUuid(){
			return this.uuid;
		}
		
		public void setSource_type(String source_type){
			this.source_type = source_type;
		}
		
		public String getSource_type(){
			return this.source_type;
		}
		
		public void setDestination_type(String destination_type){
			this.destination_type = destination_type;
		}
		
		public String getDestination_type(){
			return this.destination_type;
		}
		
		public void setBoot_index(Integer boot_index){
			this.boot_index = boot_index;
		}
		
		public Integer getBoot_index(){
			return this.boot_index;
		}
		
		
		public void setDelete_on_termination(Boolean delete_on_termination){
			this.delete_on_termination = delete_on_termination;
		}
		
		public Boolean getDelete_on_termination(){
			return this.delete_on_termination;
		}

		public Integer getVolume_size() {
			return volume_size;
		}

		public void setVolume_size(Integer volume_size) {
			this.volume_size = volume_size;
		}

	}

	class Network{
		private String uuid;
		private String fixed_ip; //V4 or V6
		private String port;
		
		public Network(String uuid, String fixed_ip, String port){
			this.uuid = uuid;
			this.fixed_ip = fixed_ip;
			this.port = port;
		}
		
		public void setUuid(String uuid){
			this.uuid = uuid;
		}
		
		public String getUuid(){
			return this.uuid;
		}
		
		public void setFixed_ip(String fixed_ip){
			this.fixed_ip = fixed_ip;
		}
		
		public String getFixed_ip(){
			return this.fixed_ip;
		}
		
		public void setPort(String port){
			this.port = port;
		}
		
		public String getPort(){
		    return this.port;
		}
		
	}

	class Metadata{
		private String volume_type;
		private String admin_pass;

		public Metadata(List<String> metadataValues) {
			this.volume_type = metadataValues.get(0);
			if(metadataValues.size()>1)
				this.admin_pass = metadataValues.get(1);
		}

		public String getVolume_type() {
			return volume_type;
		}

		public void setVolume_type(String volume_type) {
			this.volume_type = volume_type;
		}

		public String getAdmin_pass() {
			return admin_pass;
		}

		public void setAdmin_pass(String admin_pass) {
			this.admin_pass = admin_pass;
		}
		
	}
	
	class Server{
		private String name;
		private String imageRef;
		private String flavorRef;
		private String key_name;
		private Integer min_count;
		private Integer max_count;
		private String description;
		private String user_data;
		private List<Securitygroup> security_groups;
		private String availability_zone;
		private List<BlockDeviceMappingV2> block_device_mapping_v2;
		private List<Network> networks;
		private Metadata metadata;
		
		public Server(){}
		
		public Server(String name,String imageRef,String flavorRef,String key_name,String userdata,String availability_zone,Integer min_count,Integer max_count){
			this.name = name;
			this.imageRef = imageRef;
			this.flavorRef = flavorRef;
			this.key_name = key_name;
			this.availability_zone = availability_zone;
			this.user_data = userdata;
//			this.min_count = min_count;
//			this.max_count = max_count;
		}

		public void setName(String name){
			this.name = name;
		}
		
		public String getName(){
			return this.name;
		}
		
		public void setImageRef(String imageRef){
			this.imageRef = imageRef;
		}
		
		public String getImageRef(){
			return this.imageRef;
		}
		
		public void setFlavorRef(String flavorRef){
			this.flavorRef = flavorRef;
		}
		
		public String getFlavorRef(){
			return this.flavorRef;
		}
		
		public void setKey_name(String key_name){
			this.key_name = key_name;
		}
		
		public String getKey_name(){
			return this.key_name;
		}
		
		public void setMin_count(Integer min_count){
			this.min_count = min_count;
		}
		
		public Integer getMin_count(){
			return min_count;
		}
		
		public void setMax_count(Integer max_count){
			this.max_count = max_count;
		}
		
		public Integer getMax_count(){
			return this.max_count;
		}
		
		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getUser_data() {
			return user_data;
		}

		public void setUser_data(String user_data) {
			this.user_data = user_data;
		}

		public void setSecurity_groups(List<String> names){
			if(null == names || 0 == names.size())
				return;
			security_groups = new ArrayList<Securitygroup>();
			
			for(String name : names){
				Securitygroup securityGroup = new Securitygroup(name); 
				security_groups.add(securityGroup);
			}
		}
		
		
		public Metadata getMetadata() {
			return metadata;
		}

		public void setMetadata(List<String> metadataValues) {
			if(null == metadataValues || (1 != metadataValues.size() && 2 != metadataValues.size()))
				return;
			this.metadata = new Metadata(metadataValues);
		}

		public List<Securitygroup> getSecurity_groups(){
			return this.security_groups;
		}
		
		public void setAvailability_zone(String availability_zone){
			this.availability_zone = availability_zone;
		}
		
		public String getAvailability_zone(){
			return this.availability_zone;
		}
		
		public void setBlock_device_mapping_v2(String uuid,String source_type,String destination_type,Integer boot_index,Integer size,Boolean delete_on_termination){
			if(null == this.block_device_mapping_v2)
				this.block_device_mapping_v2 = new ArrayList<BlockDeviceMappingV2>();
			this.block_device_mapping_v2.add(new BlockDeviceMappingV2(uuid,source_type,destination_type,boot_index,size,delete_on_termination));
		}
		 
		public List<BlockDeviceMappingV2> getBlock_device_mapping_v2(){
			return this.block_device_mapping_v2;
		}
		
		public void setBlock_device_mapping_v2(List<BlockDeviceMappingV2> block_device_mapping_v2) {
			this.block_device_mapping_v2 = block_device_mapping_v2;
		}

		public void setNetworks(String uuid, String fixed_ip, String port){
			if(null == this.networks)
			    this.networks = new ArrayList<Network>();
			this.networks.add( new Network(uuid, fixed_ip, port));
		}
		
		public List<Network> getNetworks(){
			return this.networks;
		}
		
		
	}
	
    private Server server;
//    private String tenant_id;
    
    public InstanceJSON(String name,String imageRef,String flavorRef,String key_name,String userdata,String availability_zone,Integer min_count,Integer max_count){
    	this.server = new Server(name,imageRef,flavorRef,key_name,userdata,availability_zone,min_count,max_count);
    }
    
    public InstanceJSON(){}
    
    public void updateInstanceInfo(String name,String description){
    	this.server = new Server();
    	if(!Util.isNullOrEmptyValue(name))
    		this.server.setName(name);
    	if(!Util.isNullOrEmptyValue(description))
    		this.server.setDescription(description);
    }
//    public void setTenant_id(String tenant_id){
//    	this.tenant_id = tenant_id;
//    }
//    
//    public String getTenant_id(){
//    	return this.tenant_id;
//    }
//    
    public void createNetworks(String uuid, String fixed_ip, String port){
    	if(null == uuid && null == fixed_ip && null == port)
    		return;
    	this.server.setNetworks(uuid, fixed_ip, port);
    }
    
    public void createBlock_device_mapping_v2(String uuid,String source_type,String destination_type,Integer boot_index,Integer size,Boolean delete_on_termination){
    	if(null == uuid && null == source_type && null == destination_type && null == boot_index && null == size && null == delete_on_termination)
    		return;
    	this.server.setBlock_device_mapping_v2(uuid, source_type, destination_type, boot_index,size, delete_on_termination);
    }
    
    public void createSecurity_groups(List<String> names){
    	this.server.setSecurity_groups(names);
    }
    
    public void  createMetadata(List<String> metadatas){
    	this.server.setMetadata(metadatas);
    }
}
