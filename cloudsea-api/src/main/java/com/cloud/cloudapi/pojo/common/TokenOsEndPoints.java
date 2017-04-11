package com.cloud.cloudapi.pojo.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class TokenOsEndPoints {
    
    //serviceType：endpint-admin,internal,public:Region=1:(1:N)N
	private String serviceType;
	private String serviceName;
    
	private List<TokenOsEndPoint> endpointList;

	public TokenOsEndPoints(String type, String name) {
		this.serviceType = type;
		this.serviceName =name;
		this.endpointList = new ArrayList<TokenOsEndPoint>();
	}
	
	public TokenOsEndPoints(){
		//nothing
		this.endpointList = new ArrayList<TokenOsEndPoint>();
	}
	
	public String getServiceType() {
		return serviceType;
	}
	public void setServiceType(String type) {
		this.serviceType = type;
	}
	
	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public List<TokenOsEndPoint> getEndpointList() {
		return endpointList;
	}

	public void setEndpointList(List<TokenOsEndPoint> endpointList) {
		this.endpointList = endpointList;
	}

	public void setEndpointListv3(List<TokenOsEndPointV3> endpointListv3) {

//		this.endpointList = new ArrayList<TokenOsEndPoint>();

//取到不重复的regionlist-starat
		List<String> regionlist = new ArrayList<String>();
		for (TokenOsEndPointV3 epv3 : endpointListv3) {
			regionlist.add(epv3.getRegion());
		}
		HashSet<String> tempset = new HashSet<String>(regionlist);
		regionlist.clear();
		regionlist.addAll(tempset);
		System.out.println(regionlist);
		
//取到不重复的regionlist-end
		
		for (String region : regionlist) {
			TokenOsEndPoint ep = new TokenOsEndPoint();
			ep.setRegion(region);
			for (TokenOsEndPointV3 epv31 : endpointListv3) {

				if (region.equals(epv31.getRegion())) {
					if ("admin".equals(epv31.getUrlType())) {
						ep.setAdminURL(epv31.getUrl());
					} else if ("internal".equals(epv31.getUrlType())) {
						ep.setInternalURL(epv31.getUrl());
					} else {
						ep.setPublicURL(epv31.getUrl());
					}
				}
			}
			
			this.endpointList.add(ep);
		}

	}

}
