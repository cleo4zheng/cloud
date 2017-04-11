package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.cloud.cloudapi.pojo.common.Util;

public class Host {
	
	private String id;
    private String hostName;
    private String serviceName;
    private String zoneName;
    private String hostDetailsId;
    private String source;
    private List<HostDetail> hostDetails;
    
    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setHostName(String hostName){
    	this.hostName = hostName;
    }
    
    public String getHostName(){
    	return this.hostName;
    }
    
    public void setServiceName(String serviceName){
    	this.serviceName = serviceName;
    }
    
    public String getServiceName(){
    	return this.serviceName;
    }
    
    public void setZoneName(String zoneName){
    	this.zoneName = zoneName;
    }
    
    public String getZoneName(){
    	return this.zoneName;
    }

	public String getHostDetailsId() {
		return hostDetailsId;
	}

	public void setHostDetailsId(String hostDetailsId) {
		this.hostDetailsId = hostDetailsId;
	}

	public void makeHostDetailsId() {
		if(Util.isNullOrEmptyList(this.hostDetails))
			return;
		List<String> detailsId = new ArrayList<String>();
		for(HostDetail hostDetail : this.hostDetails){
			detailsId.add(hostDetail.getId());
		}
		this.hostDetailsId = Util.listToString(detailsId, ',');
	}
	
	public List<HostDetail> getHostDetails() {
		return hostDetails;
	}

	public void setHostDetails(List<HostDetail> hostDetails) {
		this.hostDetails = hostDetails;
	}
	
	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void normalInfo(Locale locale,Boolean normalRelated){
		if(null == locale){
			this.setHostDetailsId(null);
			this.setHostDetails(null);
			this.setServiceName(null);
			return;
		}
		if(null == this.hostDetails)
			return;
		this.setHostDetailsId(null);
		this.setServiceName(null);
		if(false == normalRelated)
			this.setHostDetails(null);
		else{
			for(HostDetail hostDetail : this.hostDetails)
				hostDetail.normalInfo(locale);	
		}
	}
}
