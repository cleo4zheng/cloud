package com.cloud.cloudapi.util;

import java.util.HashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
public class JsonHelper<JavaObject, Key> {
	
//	public static <T> generateJsonStr(<T> object){
//		
//		
//	}
	private Logger log = LogManager.getLogger(JsonHelper.class);
	
	public  String generateJsonBodySimple(JavaObject mode,Key keyString){
		
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        String jsonbody=null;
        HashMap<Key,JavaObject> map=new HashMap<Key,JavaObject>(1);
        map.put(keyString,mode);
		try {
			jsonbody = mapper.writeValueAsString(map);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			log.error(e);
			return null;
		}
		
		log.info(jsonbody);
		return jsonbody; 
		
	}
	
	public  String generateJsonBodySimple(JavaObject mode){
		
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        
        String jsonbody=null;
		try {
			jsonbody = mapper.writeValueAsString(mode);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			log.error(e);
			return null;
		}
		log.info(jsonbody);
        return jsonbody; 
		
	}
	
	public String generateJsonBodyWithEmpty(JavaObject mode){
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        mapper.setSerializationInclusion(Include.NON_NULL);
  //      mapper.setSerializationInclusion(Include.NON_EMPTY);
        String jsonbody=null;
		try {
			jsonbody = mapper.writeValueAsString(mode);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			log.error(e);
			return null;
		}

		log.info(jsonbody);
        return jsonbody; 	
	}
	
	public String generateJsonBodyWithoutDefaultValue(JavaObject mode){
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.setSerializationInclusion(Include.NON_DEFAULT);
  //      mapper.setSerializationInclusion(Include.NON_EMPTY);
        String jsonbody=null;
		try {
			jsonbody = mapper.writeValueAsString(mode);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			log.error(e);
			return null;
		}
		log.info(jsonbody);
        return jsonbody; 	
	}
}
