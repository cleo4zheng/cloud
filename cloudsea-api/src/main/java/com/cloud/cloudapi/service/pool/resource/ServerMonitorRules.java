package com.cloud.cloudapi.service.pool.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.service.pool.BasicResource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ServerMonitorRules extends BaseResource {
	private String resourceName = null;
	private final String type = "OS::Heat::TestResource";
	private List<Map<String, Object>> resources;
	List<Map<String, Object>> rules;
	private String name;
	private String mtype;
	private List<Map<String, Object>> notificationObjs;

	public ServerMonitorRules(String name, String type, List<Map<String, Object>> resources,
			List<Map<String, Object>> rules, List<Map<String, Object>> notificationObjs) {
		this.name = name;
		this.mtype = type;
		this.notificationObjs = notificationObjs;
		this.resourceName = "monitor_rule";
		this.resources = resources;
		this.rules = rules;
	}

	@Override
	public String getResourceName() {
		return this.resourceName;
	}

	// @Override
	// public Map<String, Object> getResourceMap() {
	// Map<String, Object> properties = new LinkedHashMap<String, Object>();
	// StringBuffer sb = new StringBuffer();
	// for(String s : this.serverNames){
	// sb.append(s).append("+");
	// }
	// sb.deleteCharAt(sb.length() - 1);
	// for (Map<String, Object> r : this.rules) {
	// for (Map.Entry<String, Object> entry : r.entrySet()) {
	// sb.append(entry.getKey()).append(":").append(entry.getValue()).append(";");
	// }
	// sb.deleteCharAt(sb.length() - 1);
	// sb.append("|");
	// }
	// sb.deleteCharAt(sb.length() - 1);
	// properties.put("value", sb.toString());
	// Map<String, Object> net = new LinkedHashMap<String, Object>();
	// net.put("type", this.type);
	// net.put("properties", properties);
	// Map<String, Object> res = new LinkedHashMap<String, Object>();
	// res.put(this.resourceName, net);
	// return res;
	// }
	@Override
	public Map<String, Object> getResourceMap() {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("name", this.name);
		m.put("type", this.mtype);
		m.put("resources", this.resources);
		m.put("rules", this.rules);
		m.put("notificationObjs", this.notificationObjs);
		ObjectMapper mapper = new ObjectMapper();
		String value = null;
		try {
			value = mapper.writeValueAsString(m);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		properties.put("value", value);
		Map<String, Object> net = new LinkedHashMap<String, Object>();
		net.put("type", this.type);
		net.put("properties", properties);
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put(this.resourceName, net);
		return res;
	}

	public static void main(String[] args) {
		String[] ss = { "server1", "server2" };

		List<Map<String, Object>> rules = new ArrayList<Map<String, Object>>();
		Map<String, Object> m1 = new HashMap<String, Object>();
		m1.put("period", "5min");
		m1.put("item", "cpuUse");
		m1.put("condition", ">");
		m1.put("threshold", 60);
		Map<String, Object> m2 = new HashMap<String, Object>();
		m2.put("period", "5min");
		m2.put("item", "cpuUse");
		m2.put("condition", ">");
		m2.put("threshold", 60);
		rules.add(m1);
		rules.add(m2);
//		ServerMonitorRules smr = new ServerMonitorRules("mon11", "instance", ss, rules, null);
//
//		System.out.println(smr.getResourceMap());
//		String yamlTemplate = BasicResource.convertMAP2YAML(smr.getResourceMap());
//		System.out.println(yamlTemplate);
		
		
		String attr = "\"output\": \"{\\\"name\":\\\"mon111\",\\\"resources\":[{\\\"name\":\\\"n1server1\\\"}],\\\"rules\\\":[{\\\"period\":\\\"1m\\\",\\\"item\\\":\\\"cpuUtil\\\",\\\"condition\\\":\\\">\\\",\\\"threshold\\\":60,\\\"unit\\\":\\\"%\\\"}],\\\"type\\\":\\\"instance\\\",\\\"notificationObjs\\\":[{\\\"status\\\":\\\"warning\\\",\\\"to\\\":\\\"4d98ce30-14ce-4f23-be41-2f023cb10f95\\\"}]}\"";
		System.out.println(attr);
		ObjectMapper mapper = new ObjectMapper();
		Map<String, String> attrobj=null;
		try {
			attrobj = mapper.readValue(attr, new TypeReference<HashMap<String, Object>>() {});
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String output = (String) attrobj.get("output");
		System.out.println(output);
	}

}
