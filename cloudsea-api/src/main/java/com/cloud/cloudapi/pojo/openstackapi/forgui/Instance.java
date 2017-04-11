package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.List;

import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.util.StringHelper;


public class Instance {
	private String id;
	private String name;
	private String status;
	private String type;
	private Image image;
	private String description;
	
	private String createdAt;
//	private String networkId;
	
	private String imageIds;
	private String volumeIds;
	private String networkIds;
    private String keypairIds;
	private String fixedips;
	private String floatingips;
	private String securityGroupIds;
	private String portIds;
	private String lbid;
	private String source;
	
	private List<Image> images;
	private List<Volume> volumes;
	private List<Network> networks;
	private List<Keypair> keypairs;
	private List<SecurityGroup> securityGroups;
	private List<FloatingIP> attachedFloatingIPs;
	
	private List<String> ips;
	private List<String> floatingIps;
	private List<Port> ports;
	
	//Receive data from Portal
	private String sourceType;//image/snapshot
	private String sourceId; //image/snapshot's id
	private String sourceName;
	private String systemName;
	private String core;
	private String ram;
	private String networkType;
	private String externalNetId;
	private String subnet;
	private String credentialType;
	private String username;
	private String password;
	private String keypairId;
	private String keypairName;
	private String quantity;
	private String volumeType;
	private String volumeTypeName;
	private String volumeSize;
	private String tenantId;
	private String availabilityZone;
	private String availabilityZoneName;
	private String nodeType;
	private String hostName;
	private Long millionSeconds;
	private List<String> networksId;
	
	//For �����Ŀ:  ��ʾ�����׶�(development,test,production)
	private String tag;
	
	public Instance(){
		this.createdAt = new String();
		this.fixedips = new String();
		this.floatingips = new String();
		this.imageIds = new String();
		this.volumeIds = new String();
        this.networkIds = new String();
        this.keypairIds = new String();
		this.sourceType = new String();
		this.sourceId = new String();
		this.sourceName = new String();
		this.systemName = new String();
		this.core = new String();
		this.ram = new String();
		this.subnet = new String();
		this.credentialType = new String();
		this.username = new String();
		this.password = new String();
		this.keypairId = new String();
		this.keypairName = new String();
		this.quantity = new String();
		this.volumeType = new String();
		this.volumeSize = new String();
		this.tenantId = new String();
		this.securityGroupIds = new String();
		this.portIds = new String();
		this.externalNetId = new String();
		
		this.images = new ArrayList<Image>();
		this.volumes = new ArrayList<Volume>();
		this.networks = new ArrayList<Network>();
		this.keypairs = new ArrayList<Keypair>();
		this.attachedFloatingIPs = new ArrayList<FloatingIP>();
		this.ips = new ArrayList<String>();
		this.floatingIps = new ArrayList<String>();
		this.securityGroups = new ArrayList<SecurityGroup>();
		this.ports = new ArrayList<Port>();
		//For �����Ŀ:  ��ʾ�����׶�(development,test,production)
		this.tag = new String();
	}
	
	public List<Image> getImages() {
		return images;
	}

	public void setImages(List<Image> images) {
		this.images = images;
	}

	public void addImage(Image image){
		this.images.add(image);
	}
	
	public List<Volume> getVolumes() {
		return volumes;
	}

	public void setVolumes(List<Volume> volumes) {
		this.volumes = volumes;
	}
    
	public void addVolume(Volume volume){
		this.volumes.add(volume);
	}
	
	public String getExternalNetId() {
		return externalNetId;
	}

	public void setExternalNetId(String externalNetId) {
		this.externalNetId = externalNetId;
	}

	public List<Network> getNetworks() {
		return networks;
	}

	public void setNetworks(List<Network> networks) {
		this.networks = networks;
	}
    
	public void addNetwork(Network network){
		this.networks.add(network);
	}
	
	public List<Keypair> getKeypairs() {
		return keypairs;
	}

	public void setKeypairs(List<Keypair> keypairs) {
		this.keypairs = keypairs;
	}
    
	public void addKeypair(Keypair keypair){
		this.keypairs.add(keypair);
	}
    
	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStatus() {
		return this.status;
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public Image getImage() {
		return this.image;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return this.type;
	}

	public List<String> getFloatingIps() {
		return floatingIps;
	}

	public void setFloatingIps(List<String> floatingIp) {
		this.floatingIps = floatingIp;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getCreatedAt() {
		return this.createdAt;
	}

	public List<String> getIps() {
		return ips;
	}

	public void setIps(List<String> ips) {
		this.ips = ips;
	}

	public void setSourceType(String sourceType){
		this.sourceType = sourceType;
	}
	
	public String getSourceType(){
		return this.sourceType;
	}
	
	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}
    
	public String getSystemName() {
		return systemName;
	}

	public void setSystemName(String systemName) {
		this.systemName = systemName;
	}

	public void setCore(String core){
		this.core = core; 
	}
	
	public String getCore(){
		return this.core;
	}
	
	public void setRam(String ram){
		this.ram  = ram;
	}
	
	public String getRam(){
		return this.ram;
	}
	
	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public void setNetworkType(String networkType){
		this.networkType = networkType;
	}
	
	public String getNetworkType(){
		return this.networkType;
	}
	
	public void setSubnet(String subnet){
		this.subnet = subnet;
	}
	
	public String getSubnet(){
		return this.subnet;
	}
	
	public void setCredentialType(String credentialType){
		this.credentialType = credentialType;
	}
	
	public String getCredentialType(){
		return this.credentialType;
	}
	
	public String getKeypairId() {
		return keypairId;
	}

	public void setKeypairId(String keypairId) {
		this.keypairId = keypairId;
	}

	public String getKeypairName() {
		return keypairName;
	}

	public void setKeypairName(String keypairName) {
		this.keypairName = keypairName;
	}

	public void setUsername(String username){
		this.username = username;
	}
	
	public String getUsername(){
		return this.username;
	}

	public void setPassword(String password){
		this.password = password;
	}
	
	public String getPassword(){
		return this.password;
	}
	
	public void setQuantity(String quantity){
		this.quantity = quantity;
	}
	
	public String getQuantity(){
		return this.quantity;
	}
	
	public void setVolumeType(String volumeType){
		this.volumeType = volumeType;
	}
	
	public String getVolumeType(){
		return this.volumeType;
	}
	
	public void setVolumeSize(String volumeSize){
		this.volumeSize = volumeSize;
	}
	
	public String getVolumeSize(){
		return this.volumeSize;
	}

//	public String getNetworkId() {
//		return networkId;
//	}
//
//	public void setNetworkId(String networkId) {
//		this.networkId = networkId;
//	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAvailabilityZone() {
		return availabilityZone;
	}

	public void setAvailabilityZone(String availabilityZone) {
		this.availabilityZone = availabilityZone;
	}

	public String getNetworkIds() {
		return networkIds;
	}

	public void setNetworkIds(String networkIds) {
		this.networkIds = networkIds;
	}

	public String getVolumeIds() {
		return volumeIds;
	}

	public void setVolumeIds(String volumeIds) {
		this.volumeIds = volumeIds;
	}

	public String getImageIds() {
		return imageIds;
	}

	public void setImageIds(String imageIds) {
		this.imageIds = imageIds;
	}

	public String getKeypairIds() {
		return keypairIds;
	}

	public void setKeypairIds(String keypairIds) {
		this.keypairIds = keypairIds;
	}

	public String getFixedips() {
		return fixedips;
	}

	public void setFixedips(String fixedips) {
		this.fixedips = fixedips;
	}

	public String getFloatingips() {
		return floatingips;
	}

	public void setFloatingips(String floatingips) {
		this.floatingips = floatingips;
	}

	public void makePrivateImageIds(){
		if(null == this.images)
			return;
		List<String> imageIds = new ArrayList<String>();
		for(Image image : this.images){
			imageIds.add(image.getId());
		}
		this.imageIds = Util.listToString(imageIds, ',');
	}
	
	public void makeVolumeIds(){
		if(null == this.volumes)
			return;
		List<String> volumeIds = new ArrayList<String>();
		for(Volume volume : this.volumes){
			volumeIds.add(volume.getId());
		}
		this.volumeIds = Util.listToString(volumeIds, ',');
	}
	
	public void makeNetworkIds(){
		if(null == this.networks)
			return;
		List<String> networkIds = new ArrayList<String>();
		for(Network network : this.networks){
			networkIds.add(network.getId());
		}
		this.networkIds = Util.listToString(networkIds, ',');
	}
	
	public void makeKeypairIds(){
		if(null == this.keypairs)
			return;
		List<String> keypairIds = new ArrayList<String>();
		for(Keypair keypair : this.keypairs){
			keypairIds.add(keypair.getId());
		}
		this.keypairIds = Util.listToString(keypairIds, ',');
	}

	public void makeSecurityGroupIds(){
		if(null == this.securityGroups)
			return;
		List<String> securityGroupIds = new ArrayList<String>();
		for(SecurityGroup securityGroup : this.securityGroups){
			securityGroupIds.add(securityGroup.getId());
		}
		this.securityGroupIds = Util.listToString(securityGroupIds, ',');
	}
	
	public String getNodeType() {
		return nodeType;
	}

	public List<String> getNetworksId() {
		return networksId;
	}

	public void setNetworksId(List<String> networksId) {
		this.networksId = networksId;
	}

	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}

	public String getVolumeTypeName() {
		return volumeTypeName;
	}

	public void setVolumeTypeName(String volumeTypeName) {
		this.volumeTypeName = volumeTypeName;
	}

	public String getAvailabilityZoneName() {
		return availabilityZoneName;
	}

	public void setAvailabilityZoneName(String availabilityZoneName) {
		this.availabilityZoneName = availabilityZoneName;
	}

	public String getSecurityGroupIds() {
		return securityGroupIds;
	}

	public void setSecurityGroupIds(String securityGroupIds) {
		this.securityGroupIds = securityGroupIds;
	}

	public List<SecurityGroup> getSecurityGroups() {
		return securityGroups;
	}

	public void setSecurityGroups(List<SecurityGroup> securityGroups) {
		this.securityGroups = securityGroups;
	}

	public String getPortIds() {
		return portIds;
	}

	public void setPortIds(String portIds) {
		this.portIds = portIds;
	}

	public List<Port> getPorts() {
		return ports;
	}

	public void setPorts(List<Port> ports) {
		this.ports = ports;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}

	public List<FloatingIP> getAttachedFloatingIPs() {
		return attachedFloatingIPs;
	}

	public void setAttachedFloatingIPs(List<FloatingIP> attachedFloatingIPs) {
		this.attachedFloatingIPs = attachedFloatingIPs;
	}
	
	public String getLbid() {
		return lbid;
	}

	public void setLbid(String lbid) {
		this.lbid = lbid;
	}
	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}
	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void normalInfo(Boolean normalRelatedResource){
		this.setName(StringHelper.ncr2String(this.getName()));
		this.setCore(null);
		this.setCredentialType(null);
		this.setDescription(null);
		this.setImage(null);
		this.setImageIds(null);
		this.setKeypairId(null);
		this.setKeypairIds(null);
		this.setKeypairName(null);
		this.setKeypairs(null);
		this.setNetworkIds(null);
		this.setNetworks(null);
		this.setNetworksId(null);
		this.setNetworkType(null);
		this.setNodeType(null);
		this.setSourceName(null);
		this.setSourceId(null);
		this.setSourceType(null);
		this.setSubnet(null);
		this.setVolumeSize(null);
		this.setVolumeIds(null);
		this.setVolumeType(null);
		this.setVolumeTypeName(null);
		this.setPassword(null);
		this.setPortIds(null);
		this.setQuantity(null);
		this.setRam(null);
		this.setTenantId(null);
		this.setType(null);
		this.setUsername(null);
		this.setLbid(null);
	//	this.setSecurityGroupIds(null);
		if(true == normalRelatedResource){
			this.setSystemName(null);
			this.setAvailabilityZone(null);
			this.setAvailabilityZoneName(null);
			this.setFixedips(null);
			this.setFloatingIps(null);
			this.setFloatingips(null);
			this.setIps(null);
			this.setImages(null);
			this.setSecurityGroups(null);
			this.setAttachedFloatingIPs(null);
			this.setVolumes(null);
			this.setPorts(null);
		}
		if(null != this.getMillionSeconds()){
			this.setCreatedAt(Util.millionSecond2Date(this.getMillionSeconds()));
			this.setMillionSeconds(null);
		}
	}
	
	public void normalInfoExceptIP(){
		this.setCreatedAt(null);
		this.setMillionSeconds(null);
		this.setName(StringHelper.ncr2String(this.getName()));
		this.setCore(null);
		this.setCredentialType(null);
		this.setDescription(null);
		this.setImage(null);
		this.setImageIds(null);
		this.setKeypairId(null);
		this.setKeypairIds(null);
		this.setKeypairName(null);
		this.setKeypairs(null);
		this.setNetworkIds(null);
		this.setNetworks(null);
		this.setNetworksId(null);
		this.setNetworkType(null);
		this.setNodeType(null);
		this.setSourceName(null);
		this.setSourceId(null);
		this.setSourceType(null);
		this.setSubnet(null);
		this.setVolumeSize(null);
		this.setVolumeIds(null);
		this.setVolumeType(null);
		this.setVolumeTypeName(null);
		this.setPassword(null);
		this.setPortIds(null);
		this.setPorts(null);
		this.setQuantity(null);
		this.setRam(null);
		this.setTenantId(null);
		this.setType(null);
		this.setUsername(null);
		this.setSecurityGroupIds(null);
		this.setSystemName(null);
		this.setAvailabilityZone(null);
		this.setAvailabilityZoneName(null);
		this.setFixedips(null);
		this.setFloatingIps(null);
		this.setFloatingips(null);
		this.setImages(null);
		this.setSecurityGroups(null);
		this.setAttachedFloatingIPs(null);
		this.setVolumes(null);
	}
}

