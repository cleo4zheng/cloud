package com.cloud.cloudapi.service.common.impl;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.util.http.HttpClientForOsBase;

public abstract class CloudConfigAndTokenHandler {
	
	protected CloudConfig cloudconfig;
	protected TokenOs  osToken;
	private HttpClientForOsBase client;

	public CloudConfigAndTokenHandler(CloudUser user) {

		ApplicationContext ac = new FileSystemXmlApplicationContext("classpath:com/cloud/conf/applicationContext.xml");
		cloudconfig=(CloudConfig) ac.getBean("cloudconfig");	
        this.UpdateConfigAndToken(user);
	}
	
	public CloudConfigAndTokenHandler() {

		ApplicationContext ac = new FileSystemXmlApplicationContext("classpath:com/cloud/conf/applicationContext.xml");
		cloudconfig=(CloudConfig) ac.getBean("cloudconfig");	
        this.getToken();
	}
	
	private void getToken(){
		this.client = new HttpClientForOsBase(cloudconfig);
		this.osToken=this.client.getToken();
		
	}
	
	public void UpdateConfigAndToken(CloudUser user){
		
		//todo 通过用户信息 从数据库里，找到此用户对应的openstack user,pwd,domainid,projectid
		//然后更当前osConfig.
		
		//然后更新token
		this.getToken();
	}		
	
}
