package com.cloud.cloudapi.pojo.openstackapi.forgui;

import com.cloud.cloudapi.pojo.common.Util;

public class Keypair {

	private String fingerprint;
	private String name;
	private String public_key;
	private String user_id;
	private String private_key;
	private String createdAt;
	private String instanceId;
	private String tenantId;
	private String id;
	private Long millionSeconds;
	
	public Keypair(){
		this.fingerprint = "";
		this.name = "";
		this.public_key = "";
//		this.user_id = "";
//		this.private_key = "";
	}
	
	public Keypair(String name){
		this.name = name;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public Keypair(String name,String public_key){
		this.name = name;
		this.public_key = public_key;
	}
	
	public Keypair(String name,String public_key,String fingerprint){
		this.name = name;
		this.public_key = public_key;
		this.fingerprint = fingerprint;
	}
	
	public void setFingerprint(String fingerprint){
		this.fingerprint = fingerprint;
	}
	
	public String getFingerprint(){
		return this.fingerprint;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public String getName(){
		return this.name;
	}

	public String getPublic_key() {
		return public_key;
	}

	public void setPublic_key(String public_key) {
		this.public_key = public_key;
	}

	public String getUser_id() {
		return user_id;
	}

	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}

	public String getPrivate_key() {
		return private_key;
	}

	public void setPrivate_key(String private_key) {
		this.private_key = private_key;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public Long getMillionSeconds() {
		return millionSeconds;
	}

	public void setMillionSeconds(Long millionSeconds) {
		this.millionSeconds = millionSeconds;
	}
	
	
	public void normalInfo(){
		this.setInstanceId(null);
        this.setPrivate_key(null);
		this.setPublic_key(null);
		this.setTenantId(null);
		this.setUser_id(null);
		if(null != this.getMillionSeconds()){
			this.setCreatedAt(Util.millionSecond2Date(this.getMillionSeconds()));
			this.setMillionSeconds(null);
		}
	}
}
