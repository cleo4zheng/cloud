package com.cloud.cloudapi.service.pool.resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TroveInstance extends BaseResource {
	private String resourceName = null;
	private final String type = "OS::Trove::Instance";
	private String flavor;
	private String datastore_type;
	private String datastore_version;
	private String network;
	private int volume_size;
	private String database;
	private String user;
	private String password;
	//private String troveNet;

	//public TroveInstance(String name, String flavor, String datastore_type, String datastore_version, String troveNet, String network,
	//		int volume_size, String database, String user, String password) {
	public TroveInstance(String name, String flavor, String datastore_type, String datastore_version, String network,
			int volume_size, String database, String user, String password) {	
		this.resourceName = "db_" + name;
		this.flavor = flavor;
		this.datastore_type = datastore_type;
		this.datastore_version = datastore_version;
		this.network = network;
		this.volume_size = volume_size;
		this.database = database;
		this.user = user;
		this.password = password;
		//this.troveNet = troveNet;
	}

	@Override
	public String getResourceName() {
		return this.resourceName;
	}

	@Override
	public Map<String, Object> getResourceMap() {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		
		properties.put("name", this.resourceName);
		properties.put("flavor", this.flavor);
		properties.put("datastore_type", this.datastore_type);
		properties.put("datastore_version", this.datastore_version);
		properties.put("size", this.volume_size);

		List<Map<String, Object>> ll = new ArrayList<Map<String, Object>>();
		Map<String, String> r1 = new LinkedHashMap<String, String>();
		r1.put("get_resource", this.network);
		Map<String, Object> r2 = new LinkedHashMap<String, Object>();
		r2.put("network", r1);
		//r2.put("network", this.network);
		ll.add(r2);
		//Map<String, Object> tn = new LinkedHashMap<String, Object>();
		//tn.put("network", this.troveNet);
		//ll.add(tn);
		properties.put("networks", ll);
		
		List<Map<String, Object>> dbs = new ArrayList<Map<String, Object>>();
		Map<String, Object> dbname = new LinkedHashMap<String, Object>();
		dbname.put("name", this.database);
		dbs.add(dbname);
		properties.put("databases", dbs);
		
		List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
		Map<String, Object> user = new LinkedHashMap<String, Object>();
		user.put("name", this.user);
		user.put("password", this.password);
		List<String> dbnames = new ArrayList<String>();
		dbnames.add(this.database);
		user.put("databases", dbnames);
		users.add(user);
		properties.put("users", users);
		
		Map<String, Object> one = new LinkedHashMap<String, Object>();
		one.put("type", this.type);
		one.put("properties", properties);
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put(this.resourceName, one);
		return res;
	}

	public static void main(String[] args){
		//TroveInstance t = new TroveInstance("db1", "mini", "mysql", "5.5", "123123dd", "net1", 1, "cloudapi", "admin", "123");
		TroveInstance t = new TroveInstance("db1", "mini", "mysql", "5.5", "net1", 1, "cloudapi", "admin", "123");
		System.out.println(t.getResourceMap());
	}
	
}
