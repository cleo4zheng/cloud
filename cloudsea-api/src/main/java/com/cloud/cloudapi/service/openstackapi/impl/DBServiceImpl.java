package com.cloud.cloudapi.service.openstackapi.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.DBInstanceMapper;
import com.cloud.cloudapi.dao.common.DBUserMapper;
import com.cloud.cloudapi.dao.common.DataStoreVersionMapper;
import com.cloud.cloudapi.dao.common.DatabaseMapper;
import com.cloud.cloudapi.dao.common.ResourceCreateProcessMapper;
import com.cloud.cloudapi.dao.common.ResourceEventMapper;
import com.cloud.cloudapi.dao.common.SyncResourceMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.DBInstance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.DBUser;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Database;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Datastore;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceCreateProcess;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceEvent;
import com.cloud.cloudapi.pojo.openstackapi.forgui.SyncResource;
import com.cloud.cloudapi.pojo.openstackapi.foros.DataStoreVersion;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.DBService;
import com.cloud.cloudapi.service.openstackapi.FlavorService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("dbService")
public class DBServiceImpl implements DBService {

	@Autowired
	private DBInstanceMapper dbinstanceMapper;
	
	@Autowired
	private DatabaseMapper databaseMapper;
	
	@Autowired
	private DBUserMapper dbuserMapper;
	
	@Autowired
	private DataStoreVersionMapper dsvMapper;
	
	@Autowired
	private SyncResourceMapper syncResourceMapper;
	
	@Autowired
	private ResourceCreateProcessMapper resourceCreateProcessMapper;

	@Autowired
	private ResourceEventMapper resourceEventMapper;
	
	@Resource
	private OSHttpClientUtil client;
	
	@Resource
	private AuthService authService;
	
	@Resource
	private FlavorService flavorService;
	
	private Logger log = LogManager.getLogger(DBServiceImpl.class);
	
	@Override
	public List<DBInstance> getDBInstances(Map<String,String> paramMap, String dbType, TokenOs ostoken) throws BusinessException{
	
		/*
		TokenOs adminToken = null;
		try{
			adminToken = authService.createDefaultAdminOsToken();	
		}catch(Exception e){
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG);
		}
		*/
		// get db instance from db
		int limit = Util.getLimit(paramMap);
		List<DBInstance> dbinstances = getDBinstancesFromDB(ostoken.getTenantid(), limit, dbType);
		return dbinstances;
		
		/**
		//get from openstack
		String region = ostoken.getCurrentRegion();
        Locale locale = new Locale(ostoken.getLocale());
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_DB, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/instances", paramMap);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		Map<String, String> rs = client.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		List<DBInstance> dbInstances = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				dbInstances = getDBInstances(rs , dbType);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoGet(url, headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				dbInstances = getDBInstances(rs, dbType);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_DB_INSTANCE_GET_FAILED,httpCode,locale);
		}
		if (!Util.isNullOrEmptyList(dbInstances)) {
			for (DBInstance dbinstance : dbInstances) {
				dbinstance.setTenantId(ostoken.getTenantid());
			}
			saveDBInstance2DB(dbInstances);
		}
		return dbInstances;
		**/
	}
	
//	private void saveDBInstance2DB(List<DBInstance> dbinstances) {
//		if(Util.isNullOrEmptyList(dbinstances))
//			return;
//		
//		for(DBInstance dbinstance : dbinstances){
//			if(null == dbinstanceMapper.selectByPrimaryKey(dbinstance.getId()))
//				dbinstanceMapper.insertSelective(dbinstance);
//			else
//				dbinstanceMapper.updateByPrimaryKeySelective(dbinstance);
//		}
//		
//	}

	private List<DBInstance> getDBinstancesFromDB(String tenantid, int limit , String type) {
		List<DBInstance> dbinstances = null;
		if (-1 == limit) {
			dbinstances = dbinstanceMapper.selectAllByTenantIdAndType(tenantid, type);
		} else {
			dbinstances = dbinstanceMapper.selectAllByTenantIdAndTypeWithLimit(tenantid, limit, type);
		}
		if (Util.isNullOrEmptyList(dbinstances))
			return null;
		return dbinstances;
	}

	@Override
	public DBInstance getRelationDBInstance(String instanceId,TokenOs ostoken) throws BusinessException{
		
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_DB, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/instances/");
		sb.append(instanceId);
		
		Locale locale = new Locale(ostoken.getLocale());
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		DBInstance dbInstance = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				dbInstance = getDBInstance(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoGet(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				dbInstance = getDBInstance(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_DB_INSTANCE_DETAIL_GET_FAILED,locale);
		}
		return dbInstance;
	}
	
	@Override
	public List<Database> getDBs(String instanceId,TokenOs ostoken)

			throws BusinessException {
		//get from cloud db
		List<Database> dbList = null;
		dbList = databaseMapper.selectByinstanceId(instanceId);
		if(! Util.isNullOrEmptyList(dbList)){
			return dbList;
		}
		
		//get from openstack
		// todo 1: 通过guitokenid 取得实际，用户信�?
		// AuthService as = new AuthServiceImpl();
		// as.GetTokenOS(guiTokenId);
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_DB, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/instances/");
		sb.append(instanceId);
		sb.append("/databases");

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		List<Database> dbs = new ArrayList<Database>();
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				dbs = getDBs(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoGet(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				dbs = getDBs(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_DB_GET_FAILED,httpCode,locale);
		}
		if(!Util.isNullOrEmptyList(dbs)){
			for (Database database : dbs) {
				database.setInstanceId(instanceId);
			}
			databaseMapper.insertOrUpdateBatch(dbs);
		}
		return dbs;
	}

	@Override
	public List<Database> createDBs(String createBody, String instanceId,TokenOs ostoken)

			throws BusinessException {
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_DB, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/instances/");
		sb.append(instanceId);
		sb.append("/databases");

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoPost(sb.toString(), headers, createBody);
		Util.checkResponseBody(rs,locale);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE: {

			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoPost(sb.toString(), headers, createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_DB_CREATE_FAILED,httpCode,locale);
		}
		
		//get database info from create body
		List<Database> dbList = getDbsfromRequest(createBody,instanceId);
		//save to cloud db
		databaseMapper.insertOrUpdateBatch(dbList);
		//TODO sync cloud db with os
		return databaseMapper.selectByinstanceId(instanceId);


	}

	private List<Database> getDbsfromRequest(String createBody, String instanceId) {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode;
		try {
			rootNode = mapper.readTree(createBody);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			log.error(e);
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
			return null;
		}
		JsonNode dbsNode = rootNode.path(ResponseConstant.DATABASES);
		if (null == dbsNode)
			return null;
		int dbsCount = dbsNode.size();
		if (0 == dbsCount)
			return null;
		List<Database> dbList = new ArrayList<Database>();
		for (int index = 0; index < dbsCount; ++index) {
			Database db = getDBInfo(dbsNode.get(index));
			if (null == db)
				continue;
			db.setId(Util.makeUUID());
			db.setInstanceId(instanceId);
			dbList.add(db);
		}
		return dbList;
	}

	@Override
	public void deleteDB(String instanceId,String dbId,TokenOs ostoken) throws BusinessException{
		//get db from cloud db
		Database db = databaseMapper.selectByPrimaryKey(dbId);
		
		//String region = ostoken.getCurrentRegion();
		//HttpClientForOsBase osClient = new HttpClientForOsBase(cloudconfig);
		//TokenOs ot = osClient.getToken();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_DB, ostoken.getCurrentRegion()).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/instances/");
		sb.append(instanceId);
		sb.append("/databases/");
		sb.append(db.getName());
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));

		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE: {
			break;
		}
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:{
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoDelete(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_DB_DELETE_FAILED,httpCode,locale);
		}
		
		databaseMapper.deleteByPrimaryKey(dbId);
		//TODO make sure delete success in os
		return ;
	}
	
	@Override
	public List<DBInstance> getDBInstances(TokenOs ostoken)
			throws BusinessException {
		String region = ostoken.getCurrentRegion();
	//	HttpClientForOsBase osClient = new HttpClientForOsBase(cloudconfig);
		/**
		TokenOs ot = osClient.getToken();
		String url = ot.getEndPoint(TokenOs.EP_TYPE_DB, region).getPublicURL();
		**/
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_DB, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/instances");

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		List<DBInstance> dbInstances = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				dbInstances = getDBInstances(rs,null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoGet(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				dbInstances = getDBInstances(rs, null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_DB_INSTANCE_GET_FAILED,httpCode,locale);
		}
		return dbInstances;
	}

	@Override
	public DBInstance getDBInstance( String instanceId, TokenOs ostoken)
			throws BusinessException {
		//get dbinstances detail info from db
		DBInstance dbInstance = null;
		dbInstance = dbinstanceMapper.selectByPrimaryKey(instanceId);
		if (null != dbInstance){
			return dbInstance; 
		}
		//get dbinstances detail info from openstack
		dbInstance = null;
		String region = ostoken.getCurrentRegion();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_DB, region).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/instances/");
		sb.append(instanceId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				dbInstance = getDBInstance(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoGet(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				dbInstance = getDBInstance(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_DB_INSTANCE_DETAIL_GET_FAILED,httpCode,locale);
		}
		dbInstance.setTenantId(ostoken.getTenantid());
		dbinstanceMapper.insertOrUpdate(dbInstance);
		nomalDBInstance(dbInstance);
		return dbInstance;
	}

	private void nomalDBInstance(DBInstance dbInstance) {
		if (dbInstance != null){
			dbInstance.setImageIds(null);
			dbInstance.setVolumeIds(null);
			dbInstance.setNetworkIds(null);
			dbInstance.setKeypairIds(null);
			dbInstance.setFloatingIps(null);
			dbInstance.setSecurityGroupIds(null);
			dbInstance.setPortIds(null);
			dbInstance.setImages(null);
			dbInstance.setVolumes(null);
			dbInstance.setNetworks(null);
			dbInstance.setKeypairs(null);
			dbInstance.setSecurityGroups(null);
			dbInstance.setAttachedFloatingIPs(null);
			dbInstance.setIps(null);
			dbInstance.setPorts(null);
			dbInstance.setSourceType(null);
			dbInstance.setSourceId(null);
			dbInstance.setSourceName(null);
			dbInstance.setSystemName(null);
			dbInstance.setTenantId(null);
			//TODO more setting null
		}
		
	}

	@Override
	public DBInstance createInstance(String createBody, TokenOs ostoken)
			throws BusinessException, JsonParseException, JsonMappingException, IOException {
		
		String createBody4os= "{\"instance\": {\"name\": \"%s\", \"nics\": %s, \"flavorRef\": %s, \"replica_count\": 1, \"volume\": %s, \"datastore\": %s}} ";
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode instanceNode = mapper.readTree(createBody).path("instance");
		String name = instanceNode.path("name").textValue();
		String volume = instanceNode.path("volume").toString();
		String datastore = instanceNode.path("datastore").toString();
		
		String nics = instanceNode.path("nics").toString();
		String flavor_vcpus =  instanceNode.path("core").toString();
        String flavor_ram =  Integer.toString(instanceNode.path("ram").asInt()*1024);
        String flavorid = flavorService.getFlavor(ostoken, flavor_vcpus, flavor_ram, "0",false,ParamConstant.DATABASE);

		String dbInstanceCreateBody = String.format(createBody4os, name, nics, flavorid, volume, datastore);
		
		//String region = ostoken.getCurrentRegion();
		//HttpClientForOsBase osClient = new HttpClientForOsBase(cloudconfig);
		//TokenOs ot = osClient.getToken();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_DB, ostoken.getCurrentRegion()).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/instances");

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());

        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        
        //DBInstanceJSON rDBInstanceJSON = mapper.readValue(createBody, DBInstanceJSON.class);
        
        
        //rDBInstanceJSON.getInstance().setFlavorRef(flavorid);
        
  //      String troveNetid = cloudconfig.getTroveNetId();
        Locale locale = new Locale(ostoken.getLocale());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoPost(sb.toString(), headers, dbInstanceCreateBody);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		DBInstance dbInstance = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				dbInstance = getDBInstance(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoPost(sb.toString(), headers, dbInstanceCreateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				dbInstance = getDBInstance(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_DB_INSTANCE_CREATE_FAILED,httpCode,locale);
		}
		
		dbInstance.setTenantId(ostoken.getTenantid());
		dbInstance.setCore(flavor_vcpus);
		dbInstance.setRam(flavor_ram);
		dbInstance.setDataVolumeSize(instanceNode.path("volume").path("size").asInt());
		dbInstance.setDataVolumeType(instanceNode.path("volume").path("type").textValue());
		dbInstance.setMillionSeconds(Util.getCurrentMillionsecond());
		//TODO get from propertie
		dbInstance.setSystemVolumeSize(10);
		dbInstance.setSystemVolumeType("");
		dbinstanceMapper.insertOrUpdate(dbInstance);
		updateSyncResourceInfo(ostoken.getTenantid(),dbInstance.getId(),null,ParamConstant.ACTIVE_STATUS, ostoken.getCurrentRegion());
		storeResourceEventInfo(ostoken.getTenantid(),dbInstance.getId(),ParamConstant.DATABASE,null,ParamConstant.ACTIVE_STATUS,dbInstance.getMillionSeconds());
		return dbInstance;
	}

	private void updateSyncResourceInfo(String tenantId,String id,String orgStatus,String expectedStatus, String region) {
		SyncResource resource = new SyncResource();
		resource.setId(id);
		resource.setType(ParamConstant.DATABASE);
		resource.setOrgStatus(orgStatus);
		resource.setExpectedStatus(expectedStatus);
		resource.setRegion(region);
		syncResourceMapper.insertSelective(resource);
		
		ResourceCreateProcess createProcess = new ResourceCreateProcess();
		createProcess.setId(id);
		createProcess.setTenantId(tenantId);
		createProcess.setType(ParamConstant.DATABASE);
		createProcess.setBegineSeconds(Util.time2Millionsecond(Util.getCurrentDate(),ParamConstant.TIME_FORMAT_01));
		resourceCreateProcessMapper.insertOrUpdate(createProcess);
	}

	private void storeResourceEventInfo(String tenantId,String id,String type,String beginState,String endState,long time){
		ResourceEvent event = new ResourceEvent();
		event.setTenantId(tenantId);
		event.setResourceId(id);
		event.setResourceType(type);
		event.setBeginState(beginState);
		event.setEndState(endState);
		event.setMillionSeconds(time);
		resourceEventMapper.insertSelective(event);
	}
	
//	private DBInstance getInstance(String createBody) throws JsonProcessingException, IOException {
//		// TODO Auto-generated method stub
//		DBInstance instance = new DBInstance();
//		ObjectMapper mapper = new ObjectMapper();
//		JsonNode instanceNode = mapper.readTree(createBody).path("instance");
//		if(instanceNode != null){
//			instance.setName(instanceNode.path("name").textValue());
//			instance.setAvailabilityZone("vdi-zone");
//			JsonNode volumeNode = instanceNode.path("volume");
//			instance.setDatastore(null);
//			instance.setNetworks(null);
//			
//			instanceNode.path("volume");
//			instanceNode.path("ram");
//			instanceNode.path("core");
//			instanceNode.path("nics");
//			;
//			instanceNode.path("datastore");
//			
//		}
//		return instance;
//	}
	

	@Override
	public void deleteDBInstance(String instanceId,TokenOs ostoken) throws BusinessException{
		
		
		Locale locale = new Locale(ostoken.getLocale());
		checkResource(instanceId,locale);
		
		//String region = ostoken.getCurrentRegion();
		//HttpClientForOsBase osClient = new HttpClientForOsBase(cloudconfig);
		//TokenOs ot = osClient.getToken();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_DB, ostoken.getCurrentRegion()).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/instances/");
		sb.append(instanceId);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs, locale);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE: {
			break;
		}
		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE:{
			break;
		}
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:{
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoDelete(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE || httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE){
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_DB_INSTANCE_DELETE_FAILED,httpCode,locale);
		}
		
		DBInstance dbInstance = dbinstanceMapper.selectByPrimaryKey(instanceId);
		dbinstanceMapper.deleteByPrimaryKey(instanceId);
		dbuserMapper.deleteByInstanceId(instanceId);
		databaseMapper.deleteByInstanceId(instanceId);
	
		updateSyncResourceInfo(ostoken.getTenantid(),dbInstance.getId(),dbInstance.getStatus(),ParamConstant.DELETED_STATUS, ostoken.getCurrentRegion());
		storeResourceEventInfo(ostoken.getTenantid(),dbInstance.getId(),ParamConstant.DATABASE,dbInstance.getStatus(),ParamConstant.DELETED_STATUS,Util.getCurrentMillionsecond());

		//TODO make sure deleting success in os
		return;
		
	}
	
	@Override
    public List<DBUser> getUsers(String instanceId,TokenOs ostoken) throws BusinessException{
		
		//get user from db
		List<DBUser> dbuserList = null;
		dbuserList = dbuserMapper.selectByinstanceId(instanceId);
		if(!Util.isNullOrEmptyList(dbuserList))
			return dbuserList;
		
		//get user from openstack
		//String region = ostoken.getCurrentRegion();
		//HttpClientForOsBase osClient = new HttpClientForOsBase(cloudconfig);
		//TokenOs ot = osClient.getToken();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_DB, ostoken.getCurrentRegion()).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/instances/");
		sb.append(instanceId);
		sb.append("/users");
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		List<DBUser> dbUsers = new ArrayList<DBUser>();
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				dbUsers = getDBInstanceUsers(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs =  client.httpDoGet(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE){
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			}
			try {
				dbUsers = getDBInstanceUsers(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_DB_INSTANCE_DETAIL_USER_GET_FAILED,httpCode,locale);
		}
		if(!Util.isNullOrEmptyList(dbUsers)){
			for (DBUser dbUser : dbUsers) {
				dbUser.setInstanceId(instanceId);
			}
			saveUsers(dbUsers);
		}		
		return dbUsers;
    }

	//save dbusers 2 cloud db
	private void saveUsers(List<DBUser> dbUsers) {
		for (DBUser dbUser : dbUsers) {
			dbuserMapper.insertOrUpdate(dbUser);
		}
		
	}

	@Override
	public void createDBUser(String instanceId,String createBody, TokenOs ostoken) throws BusinessException{
		
		//String region = ostoken.getCurrentRegion();
		//HttpClientForOsBase osClient = new HttpClientForOsBase(cloudconfig);
		//TokenOs ot = osClient.getToken();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_DB, ostoken.getCurrentRegion()).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/instances/");
		sb.append(instanceId);
		sb.append("/users");
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		// Map<String, String> rs =client.httpDoGet(url, headers);
		Map<String, String> rs = client.httpDoPost(sb.toString(), headers, createBody);
		Util.checkResponseBody(rs,locale);	
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE: {
			//save dbuser info to 
			List<DBUser> userList = new ArrayList<DBUser>();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode usernode =  null;
			try {
				usernode = mapper.readTree(createBody).path(ResponseConstant.USERS);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				log.error(e);
			}
			int size = usernode.size();
			for(int index=0; index<size ; index++){
				DBUser user = getDBUserInfo(usernode.get(index));
				user.setInstanceId(instanceId);
				userList.add(user);
			}
			dbuserMapper.insertOrUpdateBatch(userList);
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoPost(sb.toString(), headers, createBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE){
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			}
			//save dbuser info to 
			List<DBUser> userList = new ArrayList<DBUser>();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode usernode =  null;
			try {
				usernode = mapper.readTree(createBody).path(ResponseConstant.USERS);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				log.error(e);
			}
			int size = usernode.size();
			for(int index=0; index<size ; index++){
				DBUser user = getDBUserInfo(usernode.get(index));
				user.setInstanceId(instanceId);
				userList.add(user);
			}
			dbuserMapper.insertOrUpdateBatch(userList);
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_DB_INSTANCE_USER_CREATE_FAILED,httpCode,locale);
		}

	}

	private List<DBUser> getDBInstanceUsers(Map<String, String> rs) throws JsonProcessingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode usersNode = rootNode.path(ResponseConstant.USERS);
		if (null == usersNode)
			return null;
		int usersCount = usersNode.size();
		if (0 == usersCount)
			return null;
		List<DBUser> dbUsers = new ArrayList<DBUser>();
		for (int index = 0; index < usersCount; ++index) {
			DBUser dbUser = getDBUserInfo(usersNode.get(index));
			if (null == dbUser)
				continue;
			dbUser.setId(Util.makeUUID());
			dbUsers.add(dbUser);
		}
		return dbUsers;
	}
	
	private DBUser getDBUserInfo(JsonNode userNode){
		if(null == userNode)
			return null;
		DBUser dbUser = new DBUser();
		dbUser.setName(userNode.path(ResponseConstant.NAME).textValue());
		dbUser.setHost(userNode.path(ResponseConstant.HOST).textValue());
		JsonNode attrNode = userNode.path(ResponseConstant.PASSWORD);
		if(null != attrNode)
			dbUser.setPassword(attrNode.textValue());
		
		setDatabaseInfo(dbUser,userNode.path(ResponseConstant.DATABASES));
		return dbUser;
	}
	
	private void setDatabaseInfo(DBUser dbUser,JsonNode databasesNode){
		if(null == databasesNode)
			return;
		int databasesCount = databasesNode.size();
		if (0 == databasesCount)
			return;
		List<Database> databases = new ArrayList<Database>();
		String granteddatabases = "" ;
		for (int index = 0; index < databasesCount; ++index) {
			Database database = getDBInfo(databasesNode.get(index));
			if (null == database)
				continue;
			databases.add(database);
			if (index != 0)
				granteddatabases+=",";
			granteddatabases += database.getName();
			
		}
		dbUser.setDatabases(databases);
		dbUser.setGranteddatabases(granteddatabases);
	}
	private List<DBInstance> getDBInstances(Map<String, String> rs , String dbtype) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode dbsNode = rootNode.path(ResponseConstant.DBINSTANCES);
		if (null == dbsNode)
			return null;
		int dbsCount = dbsNode.size();
		if (0 == dbsCount)
			return null;

		List<DBInstance> dbInstances = new ArrayList<DBInstance>();
		for (int index = 0; index < dbsCount; ++index) {
			DBInstance dbInstance = getDBInstanceInfo(dbsNode.get(index));
			if (null == dbInstance)
				continue;
			if (dbtype.equals(dbInstance.getType()))
				dbInstances.add(dbInstance);
		}
		return dbInstances;
	}

	// get DBInstance info from openstack response
	private DBInstance getDBInstanceInfo(JsonNode dbNode) {
		if (null == dbNode)
			return null;
		DBInstance dbInstance = new DBInstance();
		dbInstance.setMillionSeconds(Util.utc2Millionsecond(dbNode.path(ResponseConstant.CREATED).textValue()));
		dbInstance.setCreatedAt(Util.millionSecond2Date(dbInstance.getMillionSeconds()));
		JsonNode flavorNode = dbNode.path(ResponseConstant.FLAVOR);
		if (null != flavorNode)
			dbInstance.setFlavorId(flavorNode.path(ResponseConstant.ID).textValue());

		dbInstance.setId(dbNode.path(ResponseConstant.ID).textValue());
		dbInstance.setHostname(dbNode.path(ResponseConstant.HOSTNAME).textValue());
		dbInstance.setName(dbNode.path(ResponseConstant.NAME).textValue());
		dbInstance.setStatus(dbNode.path(ResponseConstant.STATUS).textValue());
		//List<String> ips = new ArrayList<String>();
		for(int i=0; i<dbNode.path(ResponseConstant.IP).size();i++){
			if(i==0){
				dbInstance.setFixedips(dbNode.path(ResponseConstant.IP).get(i).textValue());
				}
			else{
				dbInstance.setFixedips(dbInstance.getFixedips()+","+dbNode.path(ResponseConstant.IP).get(i).textValue());
			}
		}
		//dbInstance.setIps(ips);

		JsonNode volumeNode = dbNode.path(ResponseConstant.VOLUME);
		if (null != volumeNode) {
	//		Volume volume = new Volume();
	//		volume.setSize(volumeNode.path(ResponseConstant.SIZE).intValue());
			dbInstance.setVolumeSize(Integer.toString(volumeNode.path(ResponseConstant.SIZE).intValue()));
//			dbInstance.setVolume(volume);
		}
		
		JsonNode dataStoreNode = dbNode.path(ResponseConstant.DATASTORE);
		if (null != dataStoreNode) {
//			Datastore dataStore = new Datastore();
//			dataStore.setType(dataStoreNode.path(ResponseConstant.TYPE).textValue());
//			dataStore.setVersion(dataStoreNode.path(ResponseConstant.VERSION).textValue());
			dbInstance.setDataStoreType(dataStoreNode.path(ResponseConstant.TYPE).textValue());
			if(dbInstance.getDataStoreType().indexOf("sql") != -1 ){
				dbInstance.setType("sql");
			}else{
				dbInstance.setType("nosql");
			}
			dbInstance.setDataStoreVersion(dataStoreNode.path(ResponseConstant.VERSION).textValue());
		}
		return dbInstance;
	}

	private DBInstance getDBInstance(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode dbNode = rootNode.path(ResponseConstant.DBINSTANCE);
		return getDBInstanceInfo(dbNode);
	}

//	private DBInstance getDBInstanceInfo(String createBody)
//			throws JsonParseException, JsonMappingException, IOException {
//		ObjectMapper mapper = new ObjectMapper();
//		DBInstance instanceInfo = mapper.readValue(createBody, DBInstance.class);
//		return instanceInfo;
//	}
	
//	private DBInstanceJSON getDBInstanceJson(String createBody)
//			throws JsonParseException, JsonMappingException, IOException {
//		ObjectMapper mapper = new ObjectMapper();
//		DBInstanceJSON instanceInfo = mapper.readValue(createBody, DBInstanceJSON.class);
//		return instanceInfo;
//	}

//	private String getCreatedDBInstanceBody(String createBody)
//			throws JsonParseException, JsonMappingException, IOException {
//		createBody = createBody.replaceFirst("type", "volume_type");
//		//DBInstance dbInstance = getDBInstanceInfo(createBody);
//		DBInstanceJSON dbInstanceJson = getDBInstanceJson(createBody);
//		
//		//TODO tanggc add nic infomation
//		JsonHelper<DBInstanceJSON, String> jsonHelp = new JsonHelper<DBInstanceJSON, String>();
//		String dbinstanceStr = jsonHelp.generateJsonBodySimple(dbInstanceJson);
//		dbinstanceStr = dbinstanceStr.replace("volume_type", "type");
//		return dbinstanceStr;
//	}

	private Database getDBInfo(JsonNode dbNode) {
		if (null == dbNode)
			return null;
		Database db = new Database();
		db.setName(dbNode.path(ResponseConstant.NAME).textValue());
		if (null != dbNode.path(ResponseConstant.CHARACTER_SET))
			db.setCharacter_set(dbNode.path(ResponseConstant.CHARACTER_SET).textValue());
		if (null != dbNode.path(ResponseConstant.COLLATE))
			db.setCollate(dbNode.path(ResponseConstant.COLLATE).textValue());
		return db;
	}

	private List<Database> getDBs(Map<String, String> rs) throws JsonProcessingException, IOException {
		/* resp body example
		 {"databases": [{"name": "test"}, {"name": "test2"}, {"name": "test3"}]}
		 */
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode dbsNode = rootNode.path(ResponseConstant.DATABASES);
		int dbsCount = dbsNode.size();
		if (0 == dbsCount)
			return null;

		List<Database> dbs = new ArrayList<Database>();
		for (int index = 0; index < dbsCount; ++index) {
			Database db = getDBInfo(dbsNode.get(index));
			if (null == db)
				continue;
			db.setId(Util.makeUUID());
			dbs.add(db);
		}

		return dbs;
	}

	@Override
	public List<Datastore> getDatastores(TokenOs ostoken, String type) throws BusinessException {
		// TODO Auto-generated method stub
		//get datastores from cloudapi db
		List<DataStoreVersion> dsvList = null;
		if(type==null){
			dsvMapper.selectAll();
		}else{
			dsvList = dsvMapper.selectByDataStoreType(type);
		}
		if(!Util.isNullOrEmptyList(dsvList)){
			List<Datastore> datastores = new ArrayList<Datastore>();
			boolean ds_exist = false;
			for(DataStoreVersion dsv : dsvList){
				ds_exist = false;
				if(!Util.isNullOrEmptyList(datastores)){
					for(Datastore datastore : datastores){
						if(datastore.getType().equals(dsv.getDatastore_name())){
							ds_exist = true;
							datastore.getVersion().add(dsv.getDatastore_name());
							break;
						}
					}
				}
				if (ds_exist)
					continue;
				Datastore ds  = new Datastore();
				ds.setType(dsv.getDatastore_name());
				ds.getVersion().add(dsv.getVersion_name());
				datastores.add(ds);
			}
			return datastores;
		}
		//get from openstack
	
		//String region = ostoken.getCurrentRegion();
		//HttpClientForOsBase osClient = new HttpClientForOsBase(cloudconfig);
		//TokenOs ot = osClient.getToken();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_DB, ostoken.getCurrentRegion()).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/datastores");
		
		

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoGet(sb.toString(), headers);
		Util.checkResponseBody(rs,locale);
		
		List<Datastore> datastores = null;
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				datastores = getDatastores(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoGet(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE){
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			}
			try {
				datastores = getDatastores(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_DB_INSTANCE_GET_FAILED,httpCode,locale);
		}
		try {
			dsvList = getDataStoreVersions(rs);
			dsvMapper.insertOrUpdateBatch(dsvList);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return datastores;
	}
	
	private List<DataStoreVersion> getDataStoreVersions(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode dtsNode = rootNode.path(ResponseConstant.DATASTORES);
		if (null == dtsNode)
			return null;
		int dtsCount = dtsNode.size();
		if (0 == dtsCount)
			return null;

		List<DataStoreVersion> dsvList = new ArrayList<DataStoreVersion>();
		String datastore_name = null;
		String type = ParamConstant.DBTYPE_SQL;
		String datastore_id = null;
		String version_id = null;
		String version_name = null;
		for (int index = 0; index < dtsCount; index++) {
			JsonNode dts = dtsNode.get(index);
			if (null == dts)
				continue;
			datastore_name = dts.path(ResponseConstant.NAME).textValue();
			datastore_id = dts.path(ResponseConstant.ID).textValue();
			if (datastore_name.indexOf("sql") == -1){
				type = ParamConstant.DBTYPE_NOSQL;
			}
			JsonNode versionS = dts.path(ResponseConstant.VERSIONS);
			if(versionS == null || versionS.size()==0 ){
				continue;
			}
			for(int indexv = 0; indexv < versionS.size(); indexv++){
				version_name = versionS.get(index).path(ResponseConstant.NAME).textValue();
				version_id = versionS.get(index).path(ResponseConstant.ID).textValue();
				DataStoreVersion dsv = new DataStoreVersion();
				dsv.setDatastore_id(datastore_id);
				dsv.setDatastore_name(datastore_name);
				dsv.setType(type);
				dsv.setVersion_id(version_id);
				dsv.setVersion_name(version_name);
				dsvList.add(dsv);
			}
		}
		return dsvList;
	}

	private List<Datastore> getDatastores(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode dtsNode = rootNode.path(ResponseConstant.DATASTORES);
		if (null == dtsNode)
			return null;
		int dtsCount = dtsNode.size();
		if (0 == dtsCount)
			return null;

		List<Datastore> datastores = new ArrayList<Datastore>();
		for (int index = 0; index < dtsCount; ++index) {
			Datastore datastore = getDatastoreInfo(dtsNode.get(index));
			if (null == datastore)
				continue;
			datastores.add(datastore);
		}
		return datastores;
	}

	private Datastore getDatastoreInfo(JsonNode jsonNode) {
		if (null == jsonNode)
			return null;
		Datastore datastore = new Datastore();
		datastore.setType(jsonNode.path(ResponseConstant.NAME).textValue());
		JsonNode versionS = jsonNode.path(ResponseConstant.VERSIONS);
		if(versionS == null || versionS.size()==0 ){
			return datastore;
		}
		
		
		for (int index = 0; index < versionS.size(); ++index) {
			datastore.getVersion().add(versionS.get(index).path(ResponseConstant.NAME).textValue());
			
		}
		return datastore;
	}

	@Override
	public void deleteDBUser(String instanceId, String userId, TokenOs ostoken) throws BusinessException {
		
		DBUser user = dbuserMapper.selectByPrimaryKey(userId);
		
		//String region = ostoken.getCurrentRegion();
		//HttpClientForOsBase osClient = new HttpClientForOsBase(cloudconfig);
		//TokenOs ot = osClient.getToken();
		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_DB, ostoken.getCurrentRegion()).getPublicURL();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/instances/");
		sb.append(instanceId);
		sb.append("/users/");
		sb.append(user.getName());
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = client.httpDoDelete(sb.toString(), headers);
		Util.checkResponseBody(rs, locale);
		
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_ASYNC_RESPONSE_CODE: {
			break;
		}
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:{
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = client.httpDoDelete(sb.toString(), headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			if(httpCode != ParamConstant.NORMAL_ASYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_DB_INSTANCE_USER_DELETE_FAILED,httpCode,locale);
		}
		dbuserMapper.deleteByPrimaryKey(userId);
		//TODO make sure deleting success in os
		return ;
		
	}
	
	private void checkResource(String id, Locale locale) throws BusinessException {
		SyncResource syncResource = syncResourceMapper.selectByPrimaryKey(id);
		if (null != syncResource)
			throw new ResourceBusinessException(Message.CS_RESOURCE_IS_DOING,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE, locale);
	}
}
