package com.cloud.cloudapi.pojo.openstackapi.forgui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Stack {
	private String id;
	private String name;
	private String template;
	private String owner;
	private String status;
	private String statusReason;
	private String createdAt;
	private String updatedAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStatusReason() {
		return statusReason;
	}

	public void setStatusReason(String statusReason) {
		this.statusReason = statusReason;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(String updatedAt) {
		this.updatedAt = updatedAt;
	}

	public static String toJSON(Stack s) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("id", s.getId());
		map.put("name", s.getName());
		map.put("status", s.getStatus());
		map.put("updatedAt", s.getUpdatedAt());
		map.put("createdAt", s.getCreatedAt());
		try {
			return mapper.writeValueAsString(map);
		} catch (Exception e) {
			Logger log = LogManager.getLogger(Stack.class);
			log.error(e);
		}
		return null;
	}

	public static String toJSON(List<Stack> sl) {
		StringBuffer sb = new StringBuffer("[");
		for (Stack s : sl) {
			String sp = Stack.toJSON(s);
			sb.append(sp).append(",");
		}
		return sb.substring(0, sb.length() - 1) + "]";
	}
}
