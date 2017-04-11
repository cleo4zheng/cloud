package com.cloud.cloudapi.service.pool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public abstract class BasicResource {
	public enum ResourceAction {
		CREATE, UPDATE, DELETE, ROLLBACK
	}

	public String getTemplateContent(String template, boolean json) {
		String path = "src/com/cloud/cloudapi/service/pool/resource/template/" + template;
		if (json) {
			path = path + ".json";
		} else {
			path = path + ".yaml";
		}
		File t_file = new File(path);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(t_file));
			StringBuffer sb = new StringBuffer();
			String temp = null;
			while ((temp = reader.readLine()) != null) {
				if (json) {
					sb.append(temp);
				} else {
					sb.append(temp + "\n");
				}
			}
			return sb.toString();
		} catch (Exception e) {
			Logger log = LogManager.getLogger(BasicResource.class);
			log.error(e);
			
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					Logger log = LogManager.getLogger(BasicResource.class);
					log.error(e);
				}
			}
		}
		return null;
	}

	public String convertYAML2JSON(String yaml) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try {
			System.out.println("yaml: " + yaml);
			JsonNode rootNode = mapper.readTree(yaml);
			// System.out.println("all: " + rootNode.toString());
			// String resources =
			// rootNode.path("heat_template_version").textValue();
			// System.out.println("all: " + resources);
			return rootNode.toString();
		} catch (Exception e) {
			Logger log = LogManager.getLogger(BasicResource.class);
			log.error(e);
		}
		return null;

	}

	public static String convertMAP2YAML(Map<String, Object> map) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try {
			String yaml = mapper.writeValueAsString(map);
			return yaml.substring(4, yaml.length());
		} catch (Exception e) {
			Logger log = LogManager.getLogger(BasicResource.class);
			log.error(e);
		}
		return null;
	}

	public static String convertMAP2JSON(Map<String, Object> map) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(map);
		} catch (Exception e) {
			Logger log = LogManager.getLogger(BasicResource.class);
			log.error(e);
		}
		return null;
	}

	public String addResource2YAML(String yaml, String resName, Map<String, Object> resMap) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try {
			Map<String, Object> map = mapper.readValue(yaml, new TypeReference<HashMap<String, Object>>() {
			});
			@SuppressWarnings("unchecked")
			Map<String, Object> resources = (Map<String, Object>) map.get("resources");
			resources.put(resName, resMap);
			return mapper.writeValueAsString(map);
		} catch (Exception e) {
			Logger log = LogManager.getLogger(BasicResource.class);
			log.error(e);
		}
		return null;
	}

	public void updatePool(ResourceAction action) {
		switch (action) {
		case CREATE:
			break;
		case UPDATE:
			break;
		case DELETE:
			break;
		case ROLLBACK:
			break;
		}
	}

}
