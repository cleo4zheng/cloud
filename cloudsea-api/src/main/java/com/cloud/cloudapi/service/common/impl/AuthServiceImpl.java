package com.cloud.cloudapi.service.common.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.CloudRoleMapper;
import com.cloud.cloudapi.dao.common.CloudUserMapper;
import com.cloud.cloudapi.dao.common.DomainTenantUserMapper;
import com.cloud.cloudapi.dao.common.TenantEndpointMapper;
import com.cloud.cloudapi.dao.common.TenantMapper;
import com.cloud.cloudapi.dao.common.TokenGuiMapper;
import com.cloud.cloudapi.dao.common.TokenOsMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.businessapi.workflow.WorkflowUserRole;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.CloudRole;
import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.common.DomainTenantUser;
import com.cloud.cloudapi.pojo.common.TenantEndpoint;
import com.cloud.cloudapi.pojo.common.TokenGui;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.TokenOsEndPoint;
import com.cloud.cloudapi.pojo.common.TokenOsEndPoints;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Tenant;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.CloudUserService;
import com.cloud.cloudapi.service.openstackapi.TenantService;
import com.cloud.cloudapi.util.DateHelper;
import com.cloud.cloudapi.util.JWTTokenHelper;
import com.cloud.cloudapi.util.MD5Helper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.StringHelper;
import com.cloud.cloudapi.util.http.HttpClientForOsBase;
import com.cloud.cloudapi.workflow.WorkFlowConstant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuthServiceImpl implements AuthService {
	
	@Resource
	private CloudConfig cloudconfig;
	
	@Resource 
	private TokenOsMapper tokenOsMapper;
	
	@Resource
	private TokenGuiMapper tokenGuiMapper;
	
	@Resource
	private CloudUserMapper cloudUserMapper;	
	
	@Resource
	private DomainTenantUserMapper domainTenantUserMapper;
	
    @Resource
    private TenantEndpointMapper tenantEndpointMapper;
    
    @Resource
	private CloudRoleMapper cloudRoleMapper;
    
    @Resource
    private CloudUserService cloudUserService;
    
    @Resource
    private TenantService tenantService;
    
	@Resource
	private TenantMapper tenantMapper;
	
    @Resource
    private DomainTenantUserMapper tenantUserMapper;
	
    private Logger log = LogManager.getLogger(AuthServiceImpl.class);
    
	public CloudConfig getCloudconfig() {
		return cloudconfig;
	}

	public void setCloudconfig(CloudConfig cloudconfig) {
		this.cloudconfig = cloudconfig;
	}	
	
	@Override
	public TokenGui insertLogin(CloudUser user) throws ResourceBusinessException,BusinessException, Exception {
		// TODO 1 select db and check the user exis,if no throw exception	
		if(cloudUserMapper.countNumByUserAccount(user.getAccount())<=0){
			throw new ResourceBusinessException(Message.CS_AUTH_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(user.getLocale()));
			//throw new BusinessException()
		}
		
		CloudUser userdb=cloudUserMapper.selectByUserInfo(user);
		if(userdb==null ||"".equals(user.getUserid())){
			throw new ResourceBusinessException(Message.CS_AUTH_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(user.getLocale()));
		}
		if(false == userdb.isEnabled())
			throw new ResourceBusinessException(Message.CS_LOGIN_FORBIDDEN,ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE,new Locale(user.getLocale()));

		
		// TODO 2 create guitoken  don't care whether is exist or not		
		List<DomainTenantUser> DTU_links=domainTenantUserMapper.selectListByUserId(userdb.getUserid());
		if(DTU_links==null){
	//		throw new Exception("the link for user-tenant-domain get failed");
			throw new ResourceBusinessException(Message.CS_AUTH_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(user.getLocale()));
		}
		
		TokenGui tokenGui = new TokenGui();
		tokenGui.setTokenid(Util.makeUUID());
	//	tokenGui.setCurrentRegion(cloudconfig.getOs_defaultregion());	
		tokenGui.setEnableWorkflow(Util.string2Boolean(cloudconfig.getWorkflow_enabled()));
		tokenGui.setEnableZabbix(Util.string2Boolean(cloudconfig.getZabbix_enabled()));
	//	tokenGui.setLocale(Locale.getDefault().getLanguage());
		tokenGui.setCurrentRegion(userdb.getCurrentregion());
		tokenGui.setLocale(cloudconfig.getSystemDefaultLocale());
	//	tokenGui.setLocale(userdb.getLocale()
		String tenantId = userdb.getCurrentTenantId();
		if(Util.isNullOrEmptyValue(tenantId))
			tenantId = userdb.getTenantId();
		Tenant tenant = tenantMapper.selectByPrimaryKey(tenantId);
		tokenGui.setTenantname(StringHelper.ncr2String(tenant.getName()));
		tokenGui.setProjectNames(tenantService.getTenantNamesByUser(userdb));
		//the degin for user-tenant-domain exits,but the now is 1:1
		//if really user:tenant 1：N to develop. the teantid and domain must be input.
		DomainTenantUser DTUone=DTU_links.get(0);
		tokenGui.setTenantuserid(DTUone.getId());
		tokenGui.setTenantid(DTUone.getOstenantid());
//		Domain domain = domainMapper.selectByPrimaryKey(DTUone.getOsdomainid());
//		if(null != domain){
//			tokenGui.setDomainName(StringHelper.ncr2String(domain.getName()));
//		}else{
//			tokenGui.setDomainName(DTUone.getOsdomainid());		
//		}
			
		long nowtime=System.currentTimeMillis();
		tokenGui.setCreateTime(nowtime);
		long expiretime=nowtime+cloudconfig.getTimeout_token_cloudapi()*3600*1000;
		tokenGui.setExpiresTime(expiretime);
		
		if(tokenGuiMapper.insertSelective(tokenGui)<=0){
	//		throw new Exception("insert tokengui to db is failed");
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(user.getLocale()));
		}
        //clear the timeout token
		tokenGuiMapper.deleteBytime(System.currentTimeMillis());
		
		return tokenGui;
	}

	@Override
	public boolean deleteLogout(String guitokenid) throws ResourceBusinessException,BusinessException, Exception {
		// TODO delete guitoken by guitokenid
		
		return tokenGuiMapper.deleteByPrimaryKey(guitokenid)>0;
	}

	@Override
	public TokenGui selectCheckGui(String guitokenid) throws ResourceBusinessException,BusinessException, Exception {
		// TODO 1 select db if the guitoken exits,if not throw. if timeout throw
		TokenGui tokenGui = tokenGuiMapper.selectByPrimaryKey(guitokenid);
		if (tokenGui == null) {
			throw new ResourceBusinessException(Message.CS_AUTH_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE);
		}else if(tokenGui.getExpiresTime()<System.currentTimeMillis()){
			throw new ResourceBusinessException(Message.CS_AUTH_TIMEOUT_ERROR,ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE);
		}
		DomainTenantUser tenantUser = domainTenantUserMapper.selectByPrimaryKey(tokenGui.getTenantuserid());
		if(null == tenantUser)
			throw new ResourceBusinessException(Message.CS_AUTH_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE);
		CloudUser user = cloudUserMapper.selectByPrimaryKey(tenantUser.getClouduserid());
		if(null == user)
			throw new ResourceBusinessException(Message.CS_AUTH_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE);
		tokenGui.setCurrentRegion(user.getCurrentregion());
		tokenGui.setLocale(user.getLocale());
		return tokenGui;
	}
	
	@Override
	public TokenGui selectCheckGuiByEncrypt(String encryptToken) throws ResourceBusinessException,BusinessException, Exception {
		// TODO 1 select db if the guitoken exits,if not throw. if timeout throw
		TokenGui tokenGui=null;
		try {
			String guiToken = JWTTokenHelper.getGuiTokenFromEncryptToken(encryptToken);
			String guiTokenId = getGuiTokenId(guiToken);
			String projectNames = getGuiTokenProjectNames(guiToken);
			//String domainName = getGuiTokenDomainName(guiToken);
			log.info("guiTokenId from EncryptToken:"+guiTokenId);
			tokenGui = selectCheckGui(guiTokenId);
			tokenGui.setProjectNames(Util.stringToList(projectNames,","));	
			/*
			Domain domain = domainMapper.selectByName(StringHelper.string2Ncr(domainName));
			if(null != domain){
				tokenGui.setDomainName(domainName);
				tokenGui.setDomainid(domain.getId());
			}else{
				tokenGui.setDomainName("default");
				tokenGui.setDomainid(cloudconfig.getOs_authdomainid());	
			}*/			
		} catch (ResourceBusinessException e) {
			//throw  e;
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE);
		} catch (Exception e) {
			log.error("get guiToken Error:"+e.getMessage());
			throw new ResourceBusinessException(Message.CS_AUTH_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE);
		}
		
		DomainTenantUser tenantUser = domainTenantUserMapper.selectByPrimaryKey(tokenGui.getTenantuserid());
		if(null == tenantUser)
			throw new ResourceBusinessException(Message.CS_AUTH_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE);
		CloudUser user = cloudUserMapper.selectByPrimaryKey(tenantUser.getClouduserid());
		if(null == user)
			throw new ResourceBusinessException(Message.CS_AUTH_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE);
		tokenGui.setCurrentRegion(user.getCurrentregion());
		tokenGui.setLocale(user.getLocale());
		
		return tokenGui;
	}
	
	@Override
	public TokenOs insertCheckGuiAndOsToken(String guitokenid) throws ResourceBusinessException, Exception {
		// 1 select db if the guitoken exits,if not throw. if timeout throw
		TokenGui tokenGui = tokenGuiMapper.selectByPrimaryKey(guitokenid);

		if (tokenGui == null) {
		//	throw new Exception("can't found the input guitokenID:" + guitokenid);
			throw new ResourceBusinessException(Message.CS_AUTH_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE);
		} else if (tokenGui.getExpiresTime() < System.currentTimeMillis()) {
		//	throw new Exception("the guitoken:" + guitokenid + " is timeout");
			throw new ResourceBusinessException(Message.CS_AUTH_TIMEOUT_ERROR,ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE);
		}

		DomainTenantUser tenantUser = domainTenantUserMapper.selectByPrimaryKey(tokenGui.getTenantuserid());
		if(null == tenantUser)
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE);
		
		CloudUser user = cloudUserMapper.selectByPrimaryKey(tenantUser.getClouduserid());
		if(null == user)
			throw new ResourceBusinessException(Message.CS_ENV_INIT_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE);
		
		TokenOs tokenos_old = tokenOsMapper.selectByGuiTokenId(tokenGui.getTokenid());
		TokenOs tokenos_new = null;

		// 2 select db get tokenos by guitokenid,if not exits,create. if timeout
		// creat new and delete old
//		long expiretimeUTC0 = tokenos_old.getExpirestime();
		long pre_expiretimeUTC0 = DateHelper.changLocalTimeToUTC0(System.currentTimeMillis()+ DateHelper.getMillisecondByMinute(cloudconfig.getTime_createtoken_beforehand()));
//		log.debug("token_exptime:"+DateHelper.longToStrByFormat(expiretimeUTC0, "yyyy-MM-dd'T'HH:mm:ssZ"));
		log.debug("pre_expiretimeUTC0:"+DateHelper.longToStrByFormat(pre_expiretimeUTC0, "yyyy-MM-dd'T'HH:mm:ssZ"));
		if (tokenos_old == null){
			// get ostoken from openstack by user
			tokenos_new = createNewTokenByUser(tokenGui);
			tokenos_new.setCurrentRegion(user.getCurrentregion());
			tokenos_new.setLocale(user.getLocale());
			return tokenos_new;
		} else if (tokenos_old.getExpirestime()<=pre_expiretimeUTC0) {
			// get ostoken from openstack by old token
			tokenos_new = createNewTokenByOsToken_implbyUser(tokenos_old);	
			tokenos_new.setCurrentRegion(user.getCurrentregion());
			tokenos_new.setLocale(user.getLocale());
			return tokenos_new;
		}else{
//			// 时间处理
//			long nowtimeUTC0= System.currentTimeMillis();
//			long expiretimeUTC0 = tokenos_old.getExpirestime();
//			long pre_expiretimeUTC0 = DateHelper.changLocalTimeToUTC0(nowtimeUTC0+ DateHelper.getMillisecondByMinute(cloudconfig.getTime_createtoken_beforehand()));
//			log.debug("notimeUTC0:"+DateHelper.longToStrByFormat(nowtimeUTC0, "yyyy-MM-dd'T'HH:mm:ssZ"));
//			log.debug("token_exptime:"+DateHelper.longToStrByFormat(expiretimeUTC0, "yyyy-MM-dd'T'HH:mm:ssZ"));
//			log.debug("pre_expiretimeUTC0:"+DateHelper.longToStrByFormat(pre_expiretimeUTC0, "yyyy-MM-dd'T'HH:mm:ssZ"));
//
//			if (expiretimeUTC0 <=nowtimeUTC0) {
//				
//				// get ostoken from openstack by user
//				return createNewTokenByUser(tokenGui);
//			} else if(expiretimeUTC0 > nowtimeUTC0 && expiretimeUTC0<=pre_expiretimeUTC0){
//				
//				// get ostoken from openstack by tokenid	
//				return createNewTokenByToken(tokenos_old);
//			} else{
//				
//				// get endpoints from db for tokenos. if token exits,enpoints should be exits too.
//				List<TenantEndpoint> list = tenantEndpointMapper.selectListByTenantId(tokenos_old.getTenantid());
//				if (list == null) {
//					throw new Exception("can't found the endpoints in db for teantid:" + tokenos_old.getTenantid());
//				}
//				tokenos_old.setEndpointlist(buildEndpointsForDataFromDb(list));
//				return tokenos_old;
//			}
			
			// get endpoints from db for tokenos. if token exits,enpoints should be exits too.
		    readEndPointsFromDB(tokenos_old);
            /*//02-03
			List<TenantEndpoint> list = tenantEndpointMapper.selectListByTenantId(tokenos_old.getTenantid());
			if (list == null) {
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(tokenGui.getLocale()));
			}
			tokenos_old.setEndpointlist(buildEndpointsForDataFromDb(list));
			*/
			tokenos_old.setCurrentRegion(user.getCurrentregion());
			tokenos_old.setLocale(user.getLocale());
			return tokenos_old;	
		}
	}
   
	private String getGuiTokenId(String guiToken) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			JsonNode node = objectMapper.readTree(guiToken);
			return node.path("token").path("tokenid").textValue();
		} catch (Exception e) {
			log.error("error", e);
			return null;
		}
	}

//	private String getGuiTokenDomainName(String guiToken) {
//		ObjectMapper objectMapper = new ObjectMapper();
//		try {
//			JsonNode node = objectMapper.readTree(guiToken);
//			return node.path("token").path("domain").textValue();
//		} catch (Exception e) {
//			log.error("error", e);
//			return null;
//		}
//	}
	
	private String getGuiTokenProjectNames(String guiToken) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			JsonNode node = objectMapper.readTree(guiToken);
			return node.path("token").path("projectNames").textValue();
		} catch (Exception e) {
			log.error("error", e);
			return null;
		}
	}
	
	private String getGuiTokenTenantId(String guiToken) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			JsonNode node = objectMapper.readTree(guiToken);
			String tenantName = node.path("token").path("projectName").textValue();
		//	String domainName = node.path("token").path("domain").textValue();
		//	Domain domain = domainMapper.selectByName(StringHelper.string2Ncr(domainName));
		//	if(null == domain)
		//		return null;
			Tenant tenant = tenantMapper.selectByName(StringHelper.string2Ncr(tenantName));
			if(null != tenant)
				return tenant.getId();
			return null;
		} catch (Exception e) {
			log.error("error", e);
			return null;
		}
	}
	
	@Override
	public TokenOs insertCheckGuiAndOsTokenByEncrypt(String encryptToken)
			throws ResourceBusinessException, BusinessException, Exception {
		TokenOs authToken=null;
		try {
			String guiToken = JWTTokenHelper.getGuiTokenFromEncryptToken(encryptToken);
			String guiTokenId = getGuiTokenId(guiToken);
			String tenantId = getGuiTokenTenantId(guiToken);
			log.info("guiTokenId from EncryptToken:"+guiTokenId);
			authToken = this.insertCheckGuiAndOsToken(guiTokenId);
			//change the token tenant id
			if(!Util.isNullOrEmptyValue(tenantId)){
				authToken.setTenantid(tenantId);
			}
		} catch (ResourceBusinessException e) {
		//	throw  e;
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(cloudconfig.getSystemDefaultLocale()));
		} catch (Exception e) {
			log.error("get guiToken Error:"+e.getMessage());
			throw new ResourceBusinessException(Message.CS_AUTH_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(cloudconfig.getSystemDefaultLocale()));
		}
		return authToken;
	}

	@Override
	public TokenOs insertOsTokenByGuiId(String guitokenid) throws ResourceBusinessException,BusinessException, Exception {
		// TODO select db get tokenos by guitokenid,if not exits,create. if timeout creat new and delet old
		return this.insertCheckGuiAndOsToken(guitokenid);
	}

	@Override
	public CloudUser getUserByGuiToken(String guiToken) throws Exception{
		String guiTokenId = JWTTokenHelper.getGuiTokenIdFromEncryptToken(guiToken);
		return cloudUserMapper.selectByGuiTokenId(guiTokenId);
	}
	
	@Override
	public TokenOs insertOsTokenById(String ostokenid) throws BusinessException, Exception {
		//select os token from db by ostokenid. if not exist throw,if timeout recreate
		TokenOs tokenos_old = tokenOsMapper.selectByPrimaryKey(ostokenid);
		if (tokenos_old == null) {
		//	throw new Exception("can't found the input ostokenid:" + ostokenid);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(cloudconfig.getSystemDefaultLocale()));
		}
		// 2-1 get endpoints from db for tokenos. if token exits,enpoints should be exits too.
		//02-03
		//List<TenantEndpoint> list = tenantEndpointMapper.selectListByTenantId(tokenos_old.getTenantid());
		List<TenantEndpoint> list = tenantEndpointMapper.selectAll();
		if (list == null) {
		//	throw new Exception("can't found the endpoints in db for teantid:" + tokenos_old.getTenantid());
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(cloudconfig.getSystemDefaultLocale()));
		}

		// 时间处理
		long expiretimeUTC0 = tokenos_old.getExpirestime();
		long pre_expiretimeUTC0 = DateHelper.changLocalTimeToUTC0(System.currentTimeMillis()+ DateHelper.getMillisecondByMinute(cloudconfig.getTime_createtoken_beforehand()));
		log.debug("token_exptime:"+DateHelper.longToStrByFormat(expiretimeUTC0, "yyyy-MM-dd'T'HH:mm:ssZ"));
		log.debug("pre_expiretimeUTC0:"+DateHelper.longToStrByFormat(pre_expiretimeUTC0, "yyyy-MM-dd'T'HH:mm:ssZ"));
		if (expiretimeUTC0 <= pre_expiretimeUTC0) {
			
			// 2-2 get ostoken from openstack by oldtoken
			return createNewTokenByOsToken_implbyUser(tokenos_old);
		}
		
		tokenos_old.setEndpointlist(buildEndpointsForDataFromDb(tokenos_old.getTenantid(),list));
		return tokenos_old;
	}	

	@Override
	public TokenOs insertNewOsTokenById(String ostokenid) throws ResourceBusinessException,BusinessException, Exception {
		// 1-select os token from db by ostokenid. if not exist throw,if exist recreate
		TokenOs tokenos_old = tokenOsMapper.selectByPrimaryKey(ostokenid);
		if (tokenos_old == null) {
		//	throw new Exception("can't found the input ostokenid:" + ostokenid);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(cloudconfig.getSystemDefaultLocale()));
		}
		return createNewTokenByOsToken_implbyUser(tokenos_old);
	}	
	
	@Override
	public TokenOs createDefaultAdminOsToken() throws BusinessException {
		HttpClientForOsBase client = new HttpClientForOsBase(cloudconfig);
		TokenOs tokenos_new = client.getToken();
		if (tokenos_new == null) {
		//	throw new Exception("the token from openstack get failed");
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(cloudconfig.getSystemDefaultLocale()));

		}
		return tokenos_new;
	}
	
	@Override
	public CloudUser getCloudUserByEncryptGuiToken(String encryptGuiTokenId)
			throws ResourceBusinessException, BusinessException, Exception {
		CloudUser user = null;
		try {
			String guiTokenId = JWTTokenHelper.getGuiTokenIdFromEncryptToken(encryptGuiTokenId);
			user = cloudUserMapper.selectByGuiTokenId(guiTokenId);
		} catch (Exception e) {
			throw new ResourceBusinessException(Message.CS_AUTH_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(cloudconfig.getSystemDefaultLocale()));
		}
		// TODO Auto-generated method stub
		return user;
	}
	
	@Override
	public String getCloudUserNameByEncryptGuiToken(String encryptGuiTokenId){
		CloudUser user = null;
		try {
			user = getCloudUserByEncryptGuiToken(encryptGuiTokenId);
		} catch (Exception e) {
			log.error("get user by guitokenid failed:"+e.getMessage());
		}
		return user==null?null:user.getAccount();
	}

	@Override
	public CloudUser getCloudUserByGuiTokenId(String guiTokenId)
			throws ResourceBusinessException, BusinessException, Exception {
		// TODO Auto-generated method stub
		return cloudUserMapper.selectByGuiTokenId(guiTokenId);
	}

	@Override
	public CloudUser getCloudUserByOsTokenId(String osTokenId)
			throws ResourceBusinessException, BusinessException, Exception {
		// TODO Auto-generated method stub
		return cloudUserMapper.selectByOsTokenId(osTokenId);
	}	

	@Override
	public String getCloudUserNameByOsToken(TokenOs token){
		// TODO Auto-generated method stub
		CloudUser user = null;
		try {
			user = cloudUserMapper.selectByOsTokenId(token.getTokenid());
		} catch (Exception e) {
			log.error("get user by guitokenid failed:"+e.getMessage());
		}
		return user==null?"":user.getAccount();
	}		
	
	/**
		 * 想根据旧tokenid发行新token，经验证发行的新token的有效期和老token一样�?
		 * 故，废弃不用 
		 * @param tokenos_old
		 * @return
		 * @throws Exception
		 */
/*		private TokenOs createNewTokenByOsToken_implbytokenid(TokenOs tokenos_old) throws Exception{
			
			String ks_auth_url = cloudconfig.getOs_authurl();
	//		String ks_auth_url = tokenos_old.getEndPoint(TokenOs.EP_TYPE_IDENTIFY).getPublicURL();
			String ks_domainid = tokenos_old.getDomainid();
			String ks_tenantid = tokenos_old.getTenantid();
			String ks_oldtokenid = tokenos_old.getTokenid();
	
			HttpClientForOsBase client = new HttpClientForOsBase(ks_auth_url, ks_oldtokenid, ks_domainid, ks_tenantid);
			//2-1 create new Token
			TokenOs tokenos_new =null;
			if(cloudconfig.isEndpoint_refresh()){
				//if refresh get token from openstack containe end points
				tokenos_new = client.getTokenByToken();
				if (tokenos_new == null) {
			//		throw new Exception("the token from openstack get failed");
					throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(tokenos_old.getLocale()));

				}
				
				refreshEndPoints(tokenos_new);
			}else{
				//if no refresh get token from openstack with no points,and use the endpoints from db
				tokenos_new = client.getTokenByTokenNoEndPoints();
				if (tokenos_new == null) {
				//	throw new Exception("the token from openstack get failed");
					throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(tokenos_old.getLocale()));
				}
	
				//get endpoints from db for tokenos. if token exits,enpoints should be exits too.
				List<TenantEndpoint> list = tenantEndpointMapper.selectListByTenantId(tokenos_old.getTenantid());
				if (list == null) {
				//	throw new Exception("can't found the endpoints in db for teantid:" + tokenos_old.getTenantid());
					throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(tokenos_old.getLocale()));
				}
				tokenos_new.setEndpointlist(buildEndpointsForDataFromDb(list));
			}
	
			// 2-2 store the ostoken to db
			tokenos_new.setTenantUserid(tokenos_old.getTenantUserid());
			tokenOsMapper.insertSelective(tokenos_new);
	
			// 2-3 delete the old token os,if exist.
			tokenOsMapper.deleteByPrimaryKey(tokenos_old.getTokenid());
			
			// 2-4 set currentRegion
			tokenos_new.setCurrentRegion(tokenos_old.getCurrentRegion());
			tokenos_new.setLocale(tokenos_old.getLocale());
			return tokenos_new;		
		}*/

	@Override
	public void checkIsAdmin(TokenOs token) throws ResourceBusinessException{
		CloudUser user = cloudUserMapper.selectByOsTokenId(token.getTokenid());
		if(null == user)
			throw new ResourceBusinessException(Message.CS_HAVE_NO_PERMISSION,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(token.getLocale()));

		if(!cloudUserService.checkIsAdmin(user.getUserid()))
			throw new ResourceBusinessException(Message.CS_HAVE_NO_PERMISSION,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(token.getLocale()));
        return;
	}
	
	@Override
	public String getUserPassword(TokenOs token)  throws ResourceBusinessException{
		CloudUser user = cloudUserMapper.selectByOsTokenId(token.getTokenid());
		if(null == user)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(token.getLocale()));
        return null;
	}
	/**
		 * @param tokenos_old
		 * @return
		 * @throws Exception
		 */
	private TokenOs createNewTokenByOsToken_implbyUser(TokenOs tokenos_old) throws Exception {

		String ks_auth_url = cloudconfig.getOs_authurl();
		// String ks_auth_url =
		// tokenos_old.getEndPoint(TokenOs.EP_TYPE_IDENTIFY).getPublicURL();
		String ks_domainid = tokenos_old.getDomainid();
		String ks_tenantid = tokenos_old.getTenantid();
		String ks_oldtokenid = tokenos_old.getTokenid();
		CloudUser user = cloudUserMapper.selectByOsTokenId(ks_oldtokenid);

		// 确定user的权�?判断是否为管理员
		String ks_user = ParamConstant.API2OS_PREFIX_USER + user.getAccount();
		String ks_pwd = ParamConstant.API2OS_PREFIX_USER_PWD + user.getAccount();
		if (cloudUserService.checkIsAdmin(user.getUserid())) {
			ks_user = cloudconfig.getOs_authuser();
			ks_pwd = cloudconfig.getOs_authpwd();
		}

		HttpClientForOsBase client = new HttpClientForOsBase(ks_auth_url, ks_user, ks_pwd, ks_domainid, ks_tenantid);
		// 2-1 create new Token
		TokenOs tokenos_new = null;
		if (cloudconfig.isEndpoint_refresh()) {
			// if refresh get token from openstack containe end points
			//tokenos_new = client.getToken(); //02-03
			tokenos_new = client.getTokenNoEndPoints();
			if (tokenos_new == null) {
				// throw new Exception("the token from openstack get failed");
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,
						ParamConstant.SERVICE_ERROR_RESPONSE_CODE, new Locale(tokenos_old.getLocale()));
			}
           
			//refreshEndPoints(tokenos_new); //02-03
			readEndPointsFromDB(tokenos_new);
		//	tokenos_new = addEndPointsNoOSToObject(tokenos_new);
		} else {
			// if no refresh get token from openstack with no points,and use the
			// endpoints from db
			tokenos_new = client.getTokenNoEndPoints();
			if (tokenos_new == null) {
				// throw new Exception("the token from openstack get failed");
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,
						ParamConstant.SERVICE_ERROR_RESPONSE_CODE, new Locale(tokenos_old.getLocale()));
			}

			readEndPointsFromDB(tokenos_new);
			// get endpoints from db for tokenos. if token exits,enpoints should
			// be exits too.
			/*
			List<TenantEndpoint> list = tenantEndpointMapper.selectListByTenantId(tokenos_old.getTenantid());
			if (list == null) {
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,
						ParamConstant.SERVICE_ERROR_RESPONSE_CODE, new Locale(tokenos_old.getLocale()));
			}
			tokenos_new.setEndpointlist(buildEndpointsForDataFromDb(list));
			*/
		}

		// 2-2 store the ostoken to db
		tokenos_new.setTenantUserid(tokenos_old.getTenantUserid());
		tokenOsMapper.insertSelective(tokenos_new);

		// 2-3 delete the old token os,if exist.
		tokenOsMapper.deleteByPrimaryKey(tokenos_old.getTokenid());

		// 2-4 set currentRegion
		tokenos_new.setCurrentRegion(tokenos_old.getCurrentRegion());
		tokenos_new.setLocale(tokenos_old.getLocale());
		return tokenos_new;
	}

	@Override
	public TokenOs createNewToken(String userId,String region,String locale) throws Exception{
		// 2-1 get ostoken from openstack by userid and password.
		DomainTenantUser tenantUser = tenantUserMapper.selectByPrimaryKey(userId);
		if(null == tenantUser)
			return null;
		CloudUser user = cloudUserMapper.selectByPrimaryKey(tenantUser.getClouduserid());
		if(null == user)
			return null;
		String ks_auth_url = cloudconfig.getOs_authurl();
		//确定user的权�?判断是否为管理员
		String ks_user = ParamConstant.API2OS_PREFIX_USER + user.getAccount();
		String ks_pwd = ParamConstant.API2OS_PREFIX_USER_PWD + user.getAccount();
		if(cloudUserService.checkIsAdmin(user.getUserid())){
			ks_user = cloudconfig.getOs_authuser();
			ks_pwd = cloudconfig.getOs_authpwd();
		}
//		if (tenantUser == null) {
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(locale));
//
//		}
		String ks_domainid = tenantUser.getOsdomainid();
		String ks_tenantid = tenantUser.getOstenantid();

		HttpClientForOsBase client = new HttpClientForOsBase(ks_auth_url, ks_user, ks_pwd, ks_domainid,
				ks_tenantid);
		TokenOs tokenos_new = client.getToken();
		if (tokenos_new == null) {
		//	throw new Exception("the token from openstack get  failed");
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(locale));
		}

		// 2-2 store the ostoken to db
		tokenos_new.setTenantUserid(tenantUser.getId());
		tokenOsMapper.insertSelective(tokenos_new);

		// 2-3 refresh the endpoints in db
		//02-03
		readEndPointsFromDB(tokenos_new);
//		if (cloudconfig.isEndpoint_refresh()) {
//			refreshEndPoints(tokenos_new);
//			tokenos_new=addEndPointsNoOSToObject(tokenos_new);
//		}
		// 2-4 set currentRegion
		tokenos_new.setCurrentRegion(region);
		tokenos_new.setLocale(locale);
		return tokenos_new;
	}
	
	private TokenOs createNewTokenByUser(TokenGui tokenGui)throws Exception{
		// 2-1 get ostoken from openstack by userid and password.
		CloudUser user = cloudUserMapper.selectByGuiTokenId(tokenGui.getTokenid());
		String ks_auth_url = cloudconfig.getOs_authurl();
		//确定user的权�?判断是否为管理员
		String ks_user = ParamConstant.API2OS_PREFIX_USER + user.getAccount();
		String ks_pwd = ParamConstant.API2OS_PREFIX_USER_PWD + user.getAccount();
		if(cloudUserService.checkIsAdmin(user.getUserid())){
			ks_user = cloudconfig.getOs_authuser();
			ks_pwd = cloudconfig.getOs_authpwd();
		}
		DomainTenantUser DTU_one = domainTenantUserMapper.selectByPrimaryKey(tokenGui.getTenantuserid());
		if (DTU_one == null) {
		//	throw new Exception("the link for user-tenant-domain get failed");
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(tokenGui.getLocale()));
		}
		String ks_domainid = DTU_one.getOsdomainid();
		String ks_tenantid = DTU_one.getOstenantid();

		HttpClientForOsBase client = new HttpClientForOsBase(ks_auth_url, ks_user, ks_pwd, ks_domainid,
				ks_tenantid);
		//TokenOs tokenos_new = client.getToken(); //fix 02-03
		TokenOs tokenos_new = client.getTokenNoEndPoints();
		if (tokenos_new == null) {
		//	throw new Exception("the token from openstack get  failed");
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(tokenGui.getLocale()));
		}

		// 2-2 store the ostoken to db
		tokenos_new.setTenantUserid(DTU_one.getId());
		tokenOsMapper.insertSelective(tokenos_new);

		// 2-3 refresh the endpoints in db
		this.readEndPointsFromDB(tokenos_new);
		/*//02-03
		if (cloudconfig.isEndpoint_refresh()) {
			refreshEndPoints(tokenos_new);
			tokenos_new=addEndPointsNoOSToObject(tokenos_new);
		}*/
		// 2-4 set currentRegion
		tokenos_new.setCurrentRegion(tokenGui.getCurrentRegion());
		tokenos_new.setLocale(tokenGui.getLocale());
		return tokenos_new;
	}
	
	private String replaceTenantId(String tenantId,String url){
		if(null == url)
			return url;
		int pos = url.lastIndexOf("/");
		if(-1 == pos)
			return url;
		url = url.substring(0,pos+1) + tenantId;
		return url;
	}
	
	private List<TokenOsEndPoints> buildEndpointsForDataFromDb(String tenantId,List<TenantEndpoint> listdb){	
		// convert format to the same as fromopenstack		
		List<String> serviceTypelist = new ArrayList<String>();
		for (TenantEndpoint one : listdb) {
			serviceTypelist.add(one.getServiceType());
		}
	//	System.out.println(serviceTypelist);
		HashSet<String> tempset = new HashSet<String>(serviceTypelist);
		serviceTypelist.clear();
		serviceTypelist.addAll(tempset);
	//	System.out.println(serviceTypelist);

		List<TokenOsEndPoints> listos = new ArrayList<TokenOsEndPoints>();
		for (String type : serviceTypelist) {
			TokenOsEndPoints oneps = new TokenOsEndPoints();
			List<TokenOsEndPoint> listp = new ArrayList<TokenOsEndPoint>();
			for (TenantEndpoint one : listdb) {
				if (one.getServiceType().equals(type)) {
					oneps.setServiceType(one.getServiceType());
					oneps.setServiceName(one.getServiceName());
					TokenOsEndPoint toep = new TokenOsEndPoint();
                    if(TokenOs.EP_TYPE_COMPUTE.equals(one.getServiceType()) || 
                       TokenOs.EP_TYPE_DB.equals(one.getServiceType()) || 
                       TokenOs.EP_TYPE_VOLUMEV2.equals(one.getServiceType())||
                       TokenOs.EP_TYPE_VOLUME.equals(one.getServiceType())||
                       TokenOs.EP_TYPE_ORCHESTRATION.equals(one.getServiceType())){
                        toep.setRegion(one.getBelongRegion());
    					toep.setAdminURL(replaceTenantId(tenantId,one.getAdminUrl()));
    					toep.setInternalURL(replaceTenantId(tenantId,one.getInternalUrl()));
    					toep.setPublicURL(replaceTenantId(tenantId,one.getPublicUrl()));	
                    }else{
    					toep.setRegion(one.getBelongRegion());
    					toep.setAdminURL(one.getAdminUrl());
    					toep.setInternalURL(one.getInternalUrl());
    					toep.setPublicURL(one.getPublicUrl());	
                    }
					listp.add(toep);
				}
			}
			oneps.setEndpointList(listp);
			listos.add(oneps);
		}
		
		return listos;
	}
	

	private void readEndPointsFromDB(TokenOs tokenos){
	   List<TenantEndpoint> endPoints = tenantEndpointMapper.selectAll();
	   if(null == endPoints)
		   return;
	   tokenos.setEndpointlist(buildEndpointsForDataFromDb(tokenos.getTenantid(),endPoints));
	}
	
	/*
	private boolean  refreshEndPoints(TokenOs tokenos_new) throws Exception{
		//1-delete old endpoints by tokenos from db
		tenantEndpointMapper.deleteByTenantId(tokenos_new.getTenantid());
		
		//2-inset new endpoints to  db	
		List<TokenOsEndPoints> lists = tokenos_new.getEndpointlist();
		if (lists == null) {
		//	throw new Exception("get TokenOsEndPoints by new user failed");
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(tokenos_new.getLocale()));
		}
		try {
			for (TokenOsEndPoints oneEPs : lists) {
				List<TokenOsEndPoint> list = oneEPs.getEndpointList();
				for (TokenOsEndPoint oneEP : list) {
					TenantEndpoint oneTEPDb = new TenantEndpoint();
					oneTEPDb.setId(Util.makeUUID());
					oneTEPDb.setServiceType(oneEPs.getServiceType());
					oneTEPDb.setServiceName(oneEPs.getServiceName());
					oneTEPDb.setBelongRegion(oneEP.getRegion());
					oneTEPDb.setAdminUrl(oneEP.getAdminURL());
					oneTEPDb.setInternalUrl(oneEP.getInternalURL());
					oneTEPDb.setPublicUrl(oneEP.getPublicURL());
					oneTEPDb.setOstenantid(tokenos_new.getTenantid());
					tenantEndpointMapper.insertSelective(oneTEPDb);
				}
			}
			insertEndPointsNoOSToDb(tokenos_new.getTenantid());
		} catch (Exception e) {
			log.error("add endpoint for user-tenant failed :", e);
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(tokenos_new.getLocale()));
		}
		return true;	
	}*/
	
	
	/*
    private TokenOs addEndPointsNoOSToObject(TokenOs token){
		//added workflow not openstack
		TokenOsEndPoint workflow_ep= new TokenOsEndPoint();
		workflow_ep.setAdminURL(cloudconfig.getWorkflow_url());
		workflow_ep.setInternalURL(cloudconfig.getWorkflow_url());
		workflow_ep.setPublicURL(cloudconfig.getWorkflow_url());
		workflow_ep.setRegion(cloudconfig.getOs_defaultregion());		
		token.getEndpointsMap().put(TokenOs.EP_TYPE_WORKFLOW+"_"+cloudconfig.getOs_defaultregion(), workflow_ep);	
		
		return token;
    }*/
    
    /*
    private void insertEndPointsNoOSToDb(String ostenantid) throws Exception{
		//added workflow not openstack		
		TenantEndpoint workflow_ep = new TenantEndpoint();
		workflow_ep.setId(Util.makeUUID());
		workflow_ep.setServiceType(TokenOs.EP_TYPE_WORKFLOW);
		workflow_ep.setServiceName(TokenOs.EP_TYPE_WORKFLOW);
		workflow_ep.setBelongRegion(cloudconfig.getOs_defaultregion());
		workflow_ep.setAdminUrl(cloudconfig.getWorkflow_url());
		workflow_ep.setInternalUrl(cloudconfig.getWorkflow_url());
		workflow_ep.setPublicUrl(cloudconfig.getWorkflow_url());
		workflow_ep.setOstenantid(ostenantid);
		tenantEndpointMapper.insertSelective(workflow_ep);		
    }*/
    
    @Override
	public boolean checkOldPassword(CloudUser user) throws ResourceBusinessException{
    	boolean isExist = true;
		// TODO 1 select db and check the user exis,if no throw exception
		if (cloudUserMapper.countNumByUserAccount(user.getAccount()) <= 0) {
			isExist = false;
			throw new ResourceBusinessException(Message.CS_AUTH_INFO_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(user.getLocale()));
			// throw new BusinessException()
		}

		CloudUser userdb = cloudUserMapper.selectByUserInfo(user);
		if (userdb == null || "".equals(user.getUserid())) {
			isExist = false;
			throw new ResourceBusinessException(Message.CS_AUTH_INFO_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(user.getLocale()));

		}
    			
        return isExist;
    	
    }
    
    @Override
	public boolean modifyPassword(String account ,String newPassword) throws ResourceBusinessException{
    	 CloudUser user = cloudUserMapper.selectByAccount(account);
    	 if(user==null)
    		 throw new ResourceBusinessException(Message.CS_AUTH_INFO_ERROR,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(cloudconfig.getSystemDefaultLocale()));
    	 //user.setPassword(MD5Helper.encode(newPassword));
    	 user.setPassword(newPassword);
    	 cloudUserMapper.updateByPrimaryKeySelective(user);
		 return true;	
    }
    
//    private void createDefaultAdminPermission(){
//    	CloudRole cloudRole = cloudRoleMapper.selectListByRoleName(Util.ADMIN_ROLE_NAME);
//    	if(null == cloudRole){
//    		cloudRole = new CloudRole();
//    		cloudRole.setId(Util.makeUUID());
//    		cloudRole.setRoleName(Util.ADMIN_ROLE_NAME);
//    		cloudRole.setRoleSign(Util.ADMIN_ROLE_SIGN);
//    	}
//    	cloudRole.setDescription(Util.DEFAULT_DISPLAY_PERMISSION);
//    	cloudRole.setPermission(Util.DEFAULT_OPERATION_PERMISSION);
//    	cloudRoleMapper.insertOrUpdate(cloudRole);
//    }
    
    @Override
	public void createDefaultUserRole(){
    	List<CloudRole> roles = new ArrayList<CloudRole>();
    	
    	CloudRole userRole = new CloudRole();
    	userRole.setId(Util.makeUUID());
    	userRole.setRoleName(Util.USER_ROLE_NAME);
    	userRole.setDisplayPermission(Util.DEFAULT_USER_DISPLAY_PERMISSION);
    	userRole.setOperationPermission(Util.DEFAULT_USER_OPERATION_PERMISSION);
    	
    	CloudRole adminRole = new CloudRole();
    	adminRole.setId(Util.makeUUID());
    	adminRole.setRoleName(Util.ADMIN_ROLE_NAME);
    	adminRole.setDisplayPermission(Util.DEFAULT_DISPLAY_PERMISSION);
    	adminRole.setOperationPermission(Util.DEFAULT_OPERATION_PERMISSION);
    	adminRole.setRoleSign(Util.ADMIN_ROLE_SIGN);
    	
    	roles.add(userRole);
    	roles.add(adminRole);
		cloudRoleMapper.insertOrUpdateBatch(roles);
    }
    
    @Override
	public CloudUser createAdminUser() {
		// 插入用户
		log.info("创建中间层admin用户  start!");
		CloudUser user = new CloudUser();
		user.setUserid(Util.makeUUID());
		user.setName(cloudconfig.getSystemAdminDefaultName());
		user.setAccount(cloudconfig.getSystemAdminDefaultAccount());
		user.setPassword(MD5Helper.encode(cloudconfig.getSystemAdminDefaultPassword()));
		user.setMail(cloudconfig.getSystemAdminDefaultMail());
		user.setPhone(cloudconfig.getSystemAdminDefaultPhone());
		user.setCompany(cloudconfig.getSystemAdminDefaultCompany());
		user.setEnabled(true);
		user.setCreate_time(System.currentTimeMillis());
		user.setCurrentregion(cloudconfig.getOs_defaultregion());
		user.setLocale(Locale.getDefault().getLanguage());
		user.setTenantId(cloudconfig.getOs_authtenantid());
		user.setOsUserId(user.getUserid()); //
		cloudUserMapper.insertSelective(user);
		log.info("创建中间层admin用户  end! -> { table:clouduser, id:" + user.getUserid() + "}");

		log.info("创建中间层admin tenant start!");
		Tenant tenant = new Tenant();
		tenant.setId(cloudconfig.getOs_authtenantid());
		tenant.setName(cloudconfig.getOs_authuser());
		tenant.setDescription(cloudconfig.getSystemAdminDefaultName());
		tenant.setDomain_id(cloudconfig.getOs_authdomainid());
		tenant.setEnabled(true);
		tenantMapper.insertSelective(tenant);
		log.info("创建中间层admin tenant  end! -> { table:tenants, id:" + cloudconfig.getOs_authtenantid() + "}");

		// 绑定openstack admin用户关系
		log.info("绑定openstack中admin用户信息  start!");
		DomainTenantUser oneDTU = new DomainTenantUser();
		oneDTU.setId(Util.makeUUID());
		oneDTU.setOsdomainid(cloudconfig.getOs_authdomainid());
		oneDTU.setOstenantid(cloudconfig.getOs_authtenantid());
		oneDTU.setClouduserid(user.getUserid());
		domainTenantUserMapper.insertSelective(oneDTU);
		log.info("绑定openstack中admin用户信息  end! ->{ table:user_tenant_domain , id:" + oneDTU.getId() + " }");

		// 将admin的endpoint存放到数据库�?
		log.info("存放admin的endpoint start!");
		HttpClientForOsBase client = new HttpClientForOsBase(cloudconfig);
		TokenOs userToken = client.getToken();
		List<TokenOsEndPoints> lists = userToken.getEndpointlist();
		String ids = "";
		for (TokenOsEndPoints oneEPs : lists) {
			List<TokenOsEndPoint> list = oneEPs.getEndpointList();
			for (TokenOsEndPoint oneEP : list) {
				TenantEndpoint oneTEPDb = new TenantEndpoint();
				oneTEPDb.setId(Util.makeUUID());
				oneTEPDb.setServiceType(oneEPs.getServiceType());
				oneTEPDb.setServiceName(oneEPs.getServiceName());
				oneTEPDb.setBelongRegion(oneEP.getRegion());
				oneTEPDb.setAdminUrl(oneEP.getAdminURL());
				oneTEPDb.setInternalUrl(oneEP.getInternalURL());
				oneTEPDb.setPublicUrl(oneEP.getPublicURL());
				oneTEPDb.setOstenantid(cloudconfig.getOs_authtenantid());
				tenantEndpointMapper.insertSelective(oneTEPDb);
				ids += oneTEPDb.getId() + ",";
			}
		}
		if (!"".equals(ids))
			ids = ids.substring(0, ids.length() - 1);
	//	cloudUserService.addEndPointsNoOSToObject(userToken);
		cloudUserService.insertWorkFlowEndPointsToDb(cloudconfig.getOs_authtenantid(),new ArrayList<String>());
		log.info("存放admin的endpoint end! ->{ table:tenant_endpoint , id:'" + ids + "' }");

		/*log.info("创建admin workflow 权限 start!");
		ids = "";
		for (String role : WorkFlowConstant.ADMIN_ROLES) {
			WorkflowUserRole workflowUserRole = new WorkflowUserRole();
			workflowUserRole.setId(Util.makeUUID());
			workflowUserRole.setAccount(user.getAccount());
			workflowUserRole.setRoleName(role);
			ids += workflowUserRole.getId() + ",";
		}
		if (!"".equals(ids))
			ids = ids.substring(0, ids.length() - 1);
		log.info("创建admin workflow 权限 end! -> { table:workflow_user_role , id:'" + ids + "' }");*/

		log.info("创建 admin 中间层权限 start!");
//		CloudRole adminRole = new CloudRole();
//		adminRole.setId(Util.makeUUID());
//		adminRole.setRoleName(Util.ADMIN_ROLE_NAME);
//		adminRole.setRoleSign(Util.ADMIN_ROLE_SIGN);
//		adminRole.setDisplayPermission(Util.DEFAULT_DISPLAY_PERMISSION);
//		adminRole.setOperationPermission(Util.DEFAULT_OPERATION_PERMISSION);
//		cloudRoleMapper.insertSelective(adminRole);
//		log.info("创建 admin 中间层权�?end! -> { table:cloudrole , id:" + adminRole.getId() + " }");

		log.info("绑定 admin 中间层权限start!");
	//	String userRoleId = Util.makeUUID();
		
		CloudRole adminRole = cloudRoleMapper.selectByRoleName(Util.ADMIN_ROLE_NAME);
		tenantService.assignUserRoleToTenant(user.getUserid(), adminRole.getId(), cloudconfig.getOs_authtenantid());
		
	//	cloudRoleMapper.insertRoleToUser(userRoleId, , adminRole.getId());
	//	log.info("绑定 admin 中间层权�?end!-> { table:user_role , id:" + userRoleId + " }");

		log.info("系统初始化  创建 admin 用户结束!");

		return user;
	}
}
