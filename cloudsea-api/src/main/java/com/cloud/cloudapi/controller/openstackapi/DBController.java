package com.cloud.cloudapi.controller.openstackapi;

import java.util.ArrayList;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloud.cloudapi.controller.common.BaseController;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.DBInstance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.DBUser;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Database;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Datastore;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.OperationLogService;
import com.cloud.cloudapi.service.openstackapi.DBService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;

@RestController
public class DBController extends BaseController{
	
	@Resource
	private OperationLogService operationLogService;
	
	@Resource
	private DBService dbService;
	
	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(DBController.class);
	
	@RequestMapping(value = "/dbinstances/config", method = RequestMethod.GET )
	public String getConfig(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			HttpServletResponse response){
		
		return null;
	}
	
	//return datastore version list of sqldb or nosqldb
	@RequestMapping(value = "/datastores", method = RequestMethod.GET )
	public String getDatastores(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.TYPE, defaultValue = "") String type,
			HttpServletResponse response){
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<Datastore> datastores = dbService.getDatastores(authToken, type);
			if (Util.isNullOrEmptyList(datastores)) {
				JsonHelper<List<Datastore>, String> jsonHelp = new JsonHelper<List<Datastore>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Datastore>());
			}
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.GET_DB_INSTANCE, ParamConstant.DATABASEINSTANCE, getDBInstancesId(dbInstances),
//					Message.SUCCESSED_FLAG, "");
			JsonHelper<List<Datastore>, String> jsonHelp = new JsonHelper<List<Datastore>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(datastores);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_DB_INSTANCE, ParamConstant.DATABASEINSTANCE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.GET_DB_INSTANCE, ParamConstant.DATABASEINSTANCE, "",
					Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_INSTANCE_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.GET_DB_INSTANCE, ParamConstant.DATABASEINSTANCE, "",
					Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}

	//获取非关系型数据库主机一�?
	@RequestMapping(value = "/nonRelationalDatabases", method = RequestMethod.GET)
	public String getNonRelationDBInstances(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			HttpServletResponse response) {
		
		TokenOs authToken = null;
		Map<String, String> paramMap = Util.makeRequestParamInfo(limit,name,status,null,"");
		String dbtype ="nosql";
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<DBInstance> dbInstances = dbService.getDBInstances(paramMap, dbtype, authToken);
			if (Util.isNullOrEmptyList(dbInstances)) {
				JsonHelper<List<Database>, String> jsonHelp = new JsonHelper<List<Database>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Database>());
			}
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.GET_DB_INSTANCE, ParamConstant.DATABASEINSTANCE, getDBInstancesId(dbInstances),
//					Message.SUCCESSED_FLAG, "");
			JsonHelper<List<DBInstance>, String> jsonHelp = new JsonHelper<List<DBInstance>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(dbInstances);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_DB_INSTANCE, ParamConstant.DATABASEINSTANCE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.GET_DB_INSTANCE, ParamConstant.DATABASEINSTANCE, "",
					Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_INSTANCE_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.GET_DB_INSTANCE, ParamConstant.DATABASEINSTANCE, "",
					Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
		
	}
	
	//获取关系型数据库主机一�?
	@RequestMapping(value = "/relationalDatabases", method = RequestMethod.GET)
	public String getRelationDBInstances(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestParam(value = ParamConstant.LIMIT, defaultValue = "") String limit,
			@RequestParam(value = ParamConstant.NAME, defaultValue = "") String name,
			@RequestParam(value = ParamConstant.STATUS, defaultValue = "") String status,
			HttpServletResponse response) {

		TokenOs authToken = null;
		Map<String, String> paramMap = Util.makeRequestParamInfo(limit,name,status,null,"");
		String dbtype = "sql";
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<DBInstance> dbInstances = dbService.getDBInstances(paramMap, dbtype, authToken);

			if (Util.isNullOrEmptyList(dbInstances)) {
//				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_DB_INSTANCE, ParamConstant.DATABASEINSTANCE, "",
//						Message.SUCCESSED_FLAG, "");
				JsonHelper<List<Database>, String> jsonHelp = new JsonHelper<List<Database>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<Database>());
			}
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.GET_DB_INSTANCE, ParamConstant.DATABASEINSTANCE, getDBInstancesId(dbInstances),
//					Message.SUCCESSED_FLAG, "");
			JsonHelper<List<DBInstance>, String> jsonHelp = new JsonHelper<List<DBInstance>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(dbInstances);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_DB_INSTANCE, ParamConstant.DATABASEINSTANCE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.GET_DB_INSTANCE, ParamConstant.DATABASEINSTANCE, "",
					Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_INSTANCE_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.GET_DB_INSTANCE, ParamConstant.DATABASEINSTANCE, "",
					Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}

	//获取数据库instance详细信息，暂时没有用�?
	public String getDBInstancenew(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String instanceId, HttpServletResponse response) {

		TokenOs authToken = null;

		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			DBInstance dbInstance = dbService.getRelationDBInstance(instanceId, authToken);
			if (null == dbInstance) {
//				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_DB_INSTANCE_DETAIL,
//						ParamConstant.DATABASEINSTANCE, "", Message.SUCCESSED_FLAG, "");
				JsonHelper<DBInstance, String> jsonHelp = new JsonHelper<DBInstance, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new DBInstance());
			}
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.GET_DB_INSTANCE_DETAIL,
//					ParamConstant.DATABASEINSTANCE, instanceId, Message.SUCCESSED_FLAG, "");
			JsonHelper<DBInstance, String> jsonHelp = new JsonHelper<DBInstance, String>();
			return jsonHelp.generateJsonBodyWithEmpty(dbInstance);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_DB_INSTANCE_DETAIL,
					ParamConstant.DATABASEINSTANCE, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.GET_DB_INSTANCE_DETAIL,
					ParamConstant.DATABASEINSTANCE, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_DB_INSTANCE_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.GET_DB_INSTANCE_DETAIL,
					ParamConstant.DATABASEINSTANCE, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}

	
	//删除数据�? OK1
	@RequestMapping(value = "/dbinstances/{instanceId}/dbs/{dbId}", method = RequestMethod.DELETE)
	public String deleteDB(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String instanceId, @PathVariable String dbId,
			HttpServletResponse response) {
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			DBInstance db =  dbService.getDBInstance(instanceId, authToken);
			if(db.getType().equals(ParamConstant.DBTYPE_SQL)){
				this.checkUserPermission(authToken, ParamConstant.RELATIONAL_DATABASE_DELETE_DATABASE);
			}else if(db.getType().equals(ParamConstant.DBTYPE_NOSQL)){
				this.checkUserPermission(authToken, ParamConstant.NON_RELATIONAL_DATABASE_DELETE_DATABASE);
			}
			dbService.deleteDB(instanceId, dbId, authToken);
			if(null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_DELETE_SUCCESSED);
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_DB, ParamConstant.DATABASE, "",
					Message.SUCCESSED_FLAG, message);
			return message;
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.DELETE_DB, ParamConstant.DATABASE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_DB, ParamConstant.DATABASE, "",
					Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_DB, ParamConstant.DATABASE, "",
					Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}

	//创建数据�?OK1
	@RequestMapping(value = "/dbinstances/{instanceId}/dbs", method = RequestMethod.POST)
	public String createDBs(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, @PathVariable String instanceId, HttpServletResponse response) {

		//create body example {"databases": [{"name": "test"}]}  
		TokenOs authToken = null;

		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			DBInstance db =  dbService.getDBInstance(instanceId, authToken);
			if(db.getType().equals(ParamConstant.DBTYPE_SQL)){
				this.checkUserPermission(authToken, ParamConstant.RELATIONAL_DATABASE_ADD_DATABASE);
			}else if(db.getType().equals(ParamConstant.DBTYPE_NOSQL)){
				this.checkUserPermission(authToken, ParamConstant.NON_RELATIONAL_DATABASE_ADD_DATABASE);
			}
			List<Database> dbs = dbService.createDBs(createBody,instanceId, authToken);
			if (Util.isNullOrEmptyList(dbs)) {
				response.setStatus(ParamConstant.NOT_FOUND_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,new Locale(authToken.getLocale()));
				String message = exception.getResponseMessage();
				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_DB, ParamConstant.DATABASE, "",
						Message.FAILED_FLAG, message);
				return message;
			}
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.CREATE_DB, ParamConstant.DATABASE, getDBsId(dbs),
					Message.SUCCESSED_FLAG, "");
			JsonHelper<List<Database>, String> jsonHelp = new JsonHelper<List<Database>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(dbs);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.CREATE_DB, ParamConstant.DATABASE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.CREATE_DB, ParamConstant.DATABASE, "",
					Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_DB, ParamConstant.DATABASE, "",
					Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}

	//获取所有数据库主机一�?暂时未使�?
	@RequestMapping(value = "/dbinstances", method = RequestMethod.GET)
	public String getDBInstances(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			 HttpServletResponse response) {

		TokenOs authToken = null;

		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<DBInstance> dbInstances = dbService.getDBInstances(authToken);
			if (Util.isNullOrEmptyList(dbInstances)) {
//				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_DB_INSTANCE,
//						ParamConstant.DATABASEINSTANCE, "", Message.SUCCESSED_FLAG, "");
				JsonHelper<List<DBInstance>, String> jsonHelp = new JsonHelper<List<DBInstance>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<DBInstance>());
			}
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.GET_DB_INSTANCE, ParamConstant.DATABASEINSTANCE,
//					getDBInstancesId(dbInstances), Message.SUCCESSED_FLAG, "");
			JsonHelper<List<DBInstance>, String> jsonHelp = new JsonHelper<List<DBInstance>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(dbInstances);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_DB_INSTANCE, ParamConstant.DATABASEINSTANCE,
					"", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.GET_DB_INSTANCE, ParamConstant.DATABASEINSTANCE,
					"", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_INSTANCE_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_DB_INSTANCE, ParamConstant.DATABASEINSTANCE,
					"", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}

	//获取数据库主机详细信�?同时返回上面的数据库和用户信�?OK1
	@RequestMapping(value = "/dbinstances/{instanceId}", method = RequestMethod.GET)
	public String getDBInstance(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String instanceId, HttpServletResponse response) {

		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			DBInstance dbInstance = dbService.getDBInstance(instanceId, authToken);

			if (null == dbInstance) {
				JsonHelper<DBInstance, String> jsonHelp = new JsonHelper<DBInstance, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new DBInstance());
			}
			List<DBUser> users = dbService.getUsers(instanceId, authToken);
			dbInstance.setUsers(users);
			List<Database> databaseList = dbService.getDBs(instanceId, authToken);
			dbInstance.setDatabases(databaseList);
			
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.GET_DB_INSTANCE_DETAIL,
//					ParamConstant.DATABASEINSTANCE, instanceId, Message.SUCCESSED_FLAG, "");
			JsonHelper<DBInstance, String> jsonHelp = new JsonHelper<DBInstance, String>();
			return jsonHelp.generateJsonBodyWithEmpty(dbInstance);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_DB_INSTANCE_DETAIL,
					ParamConstant.DATABASEINSTANCE, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.GET_DB_INSTANCE_DETAIL,
					ParamConstant.DATABASEINSTANCE, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(
					Message.CS_DB_INSTANCE_DETAIL_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.GET_DB_INSTANCE_DETAIL,
					ParamConstant.DATABASEINSTANCE, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}

	//创建数据库主�?
	@RequestMapping(value = "/dbinstances", method = RequestMethod.POST)
	public String createDBInstance(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@RequestBody String createBody, HttpServletResponse response) {

		
		/**
		 * create body example old
		 * {
    "instance":{
        "user":[
            {
                "name":"213",
                "password":"23434",
                "databases":[
                    {
                        "name":"test"
                    }
                ]
            }
        ],
        "nics":[
            {
                "net-id":"8f5ca9e0-645e-491c-b725-af0eb1a3c458"
            }
        ],
        "flavorRef":"cloudapi",
        "core":1,
        "ram":1,
        "volume":{
            "type":"7a6449ce-6fd0-4c40-ab4d-fe66ce0fa109",
            "size":50
        },
        "databases":[
            {
                "name":"test"
            }
        ],
        "datastore":{
            "version":"postgre-centos",
            "type":"postgresql"
        },
        "name":"test"
    }
}
		 */
		TokenOs authToken = null;

		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			if(createBody.equals(ParamConstant.DBTYPE_SQL)){
				this.checkUserPermission(authToken, ParamConstant.RELATIONAL_DATABASE_NEW);
			}else if(createBody.equals(ParamConstant.DBTYPE_NOSQL)){
				this.checkUserPermission(authToken, ParamConstant.NON_RELATIONAL_DATABASE_NEW);
			}
			DBInstance dbInstance = dbService.createInstance(createBody, authToken);

			if (null == dbInstance) {
				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.CREATE_DB_INSTANCE,
						ParamConstant.DATABASEINSTANCE, "", Message.SUCCESSED_FLAG, "");
				JsonHelper<DBInstance, String> jsonHelp = new JsonHelper<DBInstance, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new DBInstance());
			}
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.CREATE_DB_INSTANCE,
					ParamConstant.DATABASEINSTANCE, dbInstance.getId(), Message.SUCCESSED_FLAG, "");
			JsonHelper<DBInstance, String> jsonHelp = new JsonHelper<DBInstance, String>();
			return jsonHelp.generateJsonBodyWithEmpty(dbInstance);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.CREATE_DB_INSTANCE,
					ParamConstant.DATABASEINSTANCE, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_DB_INSTANCE,
					ParamConstant.DATABASEINSTANCE, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_INSTANCE_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_DB_INSTANCE,
					ParamConstant.DATABASEINSTANCE, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}

	//删除数据库主机，当然同时删除了数据库
	@RequestMapping(value = "/dbinstances/{instanceId}", method = RequestMethod.DELETE)
	public String deleteDBInstance(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String instanceId,HttpServletResponse response) {

		TokenOs authToken = null;

		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			DBInstance db =  dbService.getDBInstance(instanceId, authToken);
			if(db.getType().equals(ParamConstant.DBTYPE_SQL)){
				this.checkUserPermission(authToken, ParamConstant.RELATIONAL_DATABASE_DELETE);
			}else if(db.getType().equals(ParamConstant.DBTYPE_NOSQL)){
				this.checkUserPermission(authToken, ParamConstant.NON_RELATIONAL_DATABASE_DELETE);
			}
			dbService.deleteDBInstance(instanceId, authToken);
			if(null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.DELETE_DB_INSTANCE,
					ParamConstant.DATABASEINSTANCE, instanceId, Message.SUCCESSED_FLAG, "");
			JsonHelper<DBInstance, String> jsonHelp = new JsonHelper<DBInstance, String>();
			return jsonHelp.generateJsonBodyWithEmpty(new DBInstance());
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_DB_INSTANCE,
					ParamConstant.DATABASEINSTANCE, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_DB_INSTANCE,
					ParamConstant.DATABASEINSTANCE, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_INSTANCE_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_DB_INSTANCE,
					ParamConstant.DATABASEINSTANCE, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	//获取具体数据库主机上的用户一�?
	@RequestMapping(value = "/dbinstances/{instanceId}/users", method = RequestMethod.GET)
	public String getDBUsers(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String instanceId,HttpServletResponse response) {

		TokenOs authToken = null;

		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			List<DBUser> dbUsers = dbService.getUsers(instanceId,authToken);

			if (Util.isNullOrEmptyList(dbUsers)) {
//				this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_DB_INSTANCE_USER,
//						ParamConstant.DATABASEINSTANCE_USER, "", Message.SUCCESSED_FLAG, "");
				JsonHelper<List<DBInstance>, String> jsonHelp = new JsonHelper<List<DBInstance>, String>();
				return jsonHelp.generateJsonBodyWithEmpty(new ArrayList<DBInstance>());
			}
//			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_DB_INSTANCE_USER, ParamConstant.DATABASEINSTANCE_USER,
//					getDBUsersId(dbUsers), Message.SUCCESSED_FLAG, "");
			JsonHelper<List<DBUser>, String> jsonHelp = new JsonHelper<List<DBUser>, String>();
			return jsonHelp.generateJsonBodyWithEmpty(dbUsers);
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_DB_INSTANCE_USER, ParamConstant.DATABASEINSTANCE_USER,
					"", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_DB_INSTANCE_USER, ParamConstant.DATABASEINSTANCE_USER,
					"", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_INSTANCE_USER_GET_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.GET_DB_INSTANCE_USER, ParamConstant.DATABASEINSTANCE_USER,
					"", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	//创建数据库用�?
	@RequestMapping(value = "/dbinstances/{instanceId}/users", method = RequestMethod.POST)
	public String createDBUser(
			@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String instanceId, @RequestBody String createBody,
			HttpServletResponse response) {

		TokenOs authToken = null;

		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			DBInstance db =  dbService.getDBInstance(instanceId, authToken);
			if(db.getType().equals(ParamConstant.DBTYPE_SQL)){
				this.checkUserPermission(authToken, ParamConstant.RELATIONAL_DATABASE_ADD_USER);
			}else if(db.getType().equals(ParamConstant.DBTYPE_NOSQL)){
				this.checkUserPermission(authToken, ParamConstant.NON_RELATIONAL_DATABASE_ADD_USER);
			}
			dbService.createDBUser(instanceId,createBody, authToken);
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_DB_INSTANCE_USER,
					ParamConstant.DATABASEINSTANCE_USER, "", Message.SUCCESSED_FLAG, "");
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_INSTANCE_USER_CREATE_SUCCESSED,new Locale(authToken.getLocale()));
			return exception.getResponseMessage();
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.CREATE_DB_INSTANCE_USER,
					ParamConstant.DATABASEINSTANCE_USER, "", Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_DB_INSTANCE_USER,
					ParamConstant.DATABASEINSTANCE_USER, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_INSTANCE_USER_CREATE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.CREATE_DB_INSTANCE_USER,
					ParamConstant.DATABASEINSTANCE_USER, "", Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	
	//删除数据库用�?
	@RequestMapping(value = "/dbinstances/{instanceId}/users/{userId}", method = RequestMethod.DELETE)
	public String deleteDBUser(@RequestHeader(value = ParamConstant.AUTH_TOKEN, defaultValue = "nownoimpl") String guiToken,
			@PathVariable String instanceId, @PathVariable String userId,
			HttpServletResponse response){
		TokenOs authToken = null;
		try {
			authToken = this.getUserOsToken(guiToken);
			if(null == authToken){
				response.setStatus(ParamConstant.BAD_REQUEST_RESPONSE_CODE);
				ResourceBusinessException exception = new ResourceBusinessException(Message.CS_USER_TOKEN_INVALID,new Locale(this.getConfig().getSystemDefaultLocale()));
				return exception.getResponseMessage();
			}
			DBInstance db =  dbService.getDBInstance(instanceId, authToken);
			if(db.getType().equals(ParamConstant.DBTYPE_SQL)){
				this.checkUserPermission(authToken, ParamConstant.RELATIONAL_DATABASE_DELETE_USER);
			}else if(db.getType().equals(ParamConstant.DBTYPE_NOSQL)){
				this.checkUserPermission(authToken, ParamConstant.NON_RELATIONAL_DATABASE_DELETE_USER);
			}
			dbService.deleteDBUser(instanceId, userId, authToken);
			if(null != response)
				response.setStatus(ParamConstant.NORMAL_SYNC_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_INSTANCE_USER_DELETE_SUCCESSED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_DB, ParamConstant.DATABASE, "",
					Message.SUCCESSED_FLAG, message);
			return message;
		} catch (ResourceBusinessException e) {
			log.error(e.getResponseMessage(),e);
			response.setStatus(e.getStatusCode());
			String message = e.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(), Message.DELETE_DB, ParamConstant.DATABASE, "",
					Message.FAILED_FLAG, message);
			return message;
		} catch (MyBatisSystemException e) {
			response.setStatus(ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_CONNECTION_ERROR,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_DB, ParamConstant.DATABASE, "",
					Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		} catch (Exception e) {
			response.setStatus(ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
			ResourceBusinessException exception = new ResourceBusinessException(Message.CS_DB_INSTANCE_USER_DELETE_FAILED,new Locale(authToken.getLocale()));
			String message = exception.getResponseMessage();
			this.operationLogService.addOperationLog(authService.getCloudUserNameByOsToken(authToken), authToken.getTenantid(),Message.DELETE_DB, ParamConstant.DATABASE, "",
					Message.FAILED_FLAG, message);
			log.error(exception.getResponseMessage(),e);
			return message;
		}
	}
	private String getDBsId(List<Database> dbs) {
		if (Util.isNullOrEmptyList(dbs))
			return "";
		List<String> ids = new ArrayList<String>();
		for (Database db : dbs) {
			ids.add(db.getId());
		}
		return Util.listToString(ids, ',');
	}
}
