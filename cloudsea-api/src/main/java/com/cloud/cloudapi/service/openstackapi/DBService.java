package com.cloud.cloudapi.service.openstackapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.DBInstance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.DBUser;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Database;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Datastore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface DBService {
	
	public List<DBInstance> getDBInstances(Map<String,String> paramMap,String dbType, TokenOs ostoken) throws BusinessException;
	public DBInstance getRelationDBInstance(String instanceId,TokenOs guiToken) throws BusinessException;
	
	
	public List<Database> getDBs(String instanceId,TokenOs guiToken) throws BusinessException;
	public List<Database> createDBs(String createBody, String instanceId,TokenOs guiToken) throws BusinessException;
	public void deleteDB(String instanceId,String dbId, TokenOs guiToken) throws BusinessException;
	
	public List<DBInstance> getDBInstances(TokenOs guiToken) throws BusinessException;
	public DBInstance getDBInstance(String instanceId,TokenOs guiToken) throws BusinessException;
	public DBInstance createInstance(String createBody, TokenOs guiToken) throws BusinessException, JsonParseException, JsonMappingException, IOException;
    public void deleteDBInstance(String instanceId,TokenOs guiToken) throws BusinessException;
    
    public List<DBUser> getUsers(String instanceId,TokenOs guiToken) throws BusinessException;
    public void createDBUser(String instanceId,String createBody, TokenOs guiToken) throws BusinessException;
    public List<Datastore> getDatastores(TokenOs ostoken, String type)throws BusinessException;
	public void deleteDBUser(String instanceId, String name, TokenOs authToken) throws BusinessException;
}
