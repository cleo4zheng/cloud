package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.List;




public class Topology {

	private String id;
	private List<Network> networks;
	private List<Instance> servers;
	private List<Router> routers;

	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<Network> getNetworks() {
		return networks;
	}

	public void setNetworks(List<Network> networks) {
		this.networks = networks;
	}

	public List<Instance> getServers() {
		return servers;
	}

	public void setServers(List<Instance> servers) {
		this.servers = servers;
	}

	public List<Router> getRouters() {
		return routers;
	}

	public void setRouters(List<Router> routers) {
		this.routers = routers;
	}

}
