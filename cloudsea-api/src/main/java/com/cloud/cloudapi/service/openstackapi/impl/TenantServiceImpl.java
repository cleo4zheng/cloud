package com.cloud.cloudapi.service.openstackapi.impl;

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

import com.cloud.cloudapi.dao.common.CloudUserMapper;
import com.cloud.cloudapi.dao.common.DomainTenantUserMapper;
import com.cloud.cloudapi.dao.common.QuotaTemplateMapper;
import com.cloud.cloudapi.dao.common.TenantMapper;
import com.cloud.cloudapi.dao.common.TokenGuiMapper;
import com.cloud.cloudapi.dao.common.UserRoleTenantMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.common.DomainTenantUser;
import com.cloud.cloudapi.pojo.common.TokenGui;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Tenant;
import com.cloud.cloudapi.pojo.openstackapi.forgui.UserRoleTenant;
import com.cloud.cloudapi.pojo.openstackapi.foros.Project;
import com.cloud.cloudapi.pojo.quota.QuotaTemplate;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.common.CloudUserService;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.service.openstackapi.RoleService;
import com.cloud.cloudapi.service.openstackapi.TenantService;
import com.cloud.cloudapi.service.openstackapi.UserService;
import com.cloud.cloudapi.util.JWTTokenHelper;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.StringHelper;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("tenantService")
public class TenantServiceImpl implements TenantService {
	
	@Resource
	private OSHttpClientUtil httpClient;
	
	private Logger log = LogManager.getLogger(TenantServiceImpl.class);

	@Resource
	private AuthService authService;
	
	@Autowired
	private TenantMapper tenantMapper;
	
	@Autowired
	private CloudConfig cloudconfig;
	
	@Resource
	private TokenGuiMapper tokenGuiMapper;
	
	@Resource
	private CloudUserMapper cloudUserMapper;
	
	@Resource
	private QuotaTemplateMapper quotaTemplateMapper;
	
	@Resource
	private DomainTenantUserMapper domainTenantUserMapper;
	
    @Resource
    private UserRoleTenantMapper userRoleTenantMapper;
    
	@Resource
	private CloudUserService cloudUserService;
	
	@Resource
	private UserService userService;
	
    @Resource
	private QuotaService quotaService; 
	
    @Resource
	private RoleService roleService; 
    
	private void makeChildTenant(Tenant tenant){
		if(Util.isNullOrEmptyValue(tenant.getParent_id()))
			return;
		List<Tenant> childTenants = tenantMapper.selectListByParentId(tenant.getParent_id());
		tenant.setTenants(childTenants);
		for(Tenant child : childTenants){
			makeChildTenant(child);
		}
	}
	
	private void makeChildTenants(Tenant tenant){
		List<Tenant> childTenants = tenantMapper.selectListByParentId(tenant.getId());
		if(Util.isNullOrEmptyList(childTenants))
			return;
		tenant.setTenants(childTenants);
		for(Tenant child : childTenants){
			makeChildTenants(child);
		}
	}
	
	public void buildTenantsId(Tenant tenant,List<String> tenantsId){
		List<Tenant> childTenants = tenant.getTenants();
		if(null == childTenants){
			if(!tenantsId.contains(tenant.getId()))
				tenantsId.add(tenant.getId());
			return;
		}
		for(Tenant childTenant : childTenants){
           buildTenantsId(childTenant,tenantsId);
		}		
	}
	
	@Override
	public List<String> getTenantIdsByParentTenant(TokenOs ostoken){
		CloudUser user = cloudUserMapper.selectByOsTokenId(ostoken.getTokenid());
		if(null == user)
			return null;
		List<DomainTenantUser> tenantUsers = domainTenantUserMapper.selectListByUserId(user.getUserid());
		if(null == tenantUsers)
			return null;
		List<String> tenantsId = new ArrayList<String>();
		for(DomainTenantUser tenantUser : tenantUsers){
			tenantsId.add(tenantUser.getOstenantid());
		}
		List<Tenant> tenants = tenantMapper.selectTenantsByIds(tenantsId);
		if(null == tenants)
			return null;
		tenantsId.clear();
		for(Tenant tenant : tenants){
			if(Util.isNullOrEmptyValue(tenant.getParent_id())){
				if(!tenantsId.contains(tenant.getId()))
					tenantsId.add(tenant.getId());
			}else{
				makeChildTenant(tenant);
	            buildTenantsId(tenant,tenantsId);						
			}
		}
		return tenantsId;
	}
	
	@Override
	public List<Tenant> getTenantList(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {

		List<Tenant> tenants =  new ArrayList<Tenant>();;
		
		Tenant tenant = tenantMapper.selectByPrimaryKey(ostoken.getTenantid());
		List<DomainTenantUser> tenantUsers = domainTenantUserMapper.selectListByTenantId(tenant.getId());
		if(Util.isNullOrEmptyList(tenantUsers) || 1 == tenantUsers.size()){
			makeChildTenants(tenant);
			if(tenant.getId().equals(cloudconfig.getOs_authtenantid())){
				if(null != tenant.getTenants())
					tenants.addAll(tenant.getTenants());
			}
			else
				tenants.add(tenant);	
		}else{
			tenants = new ArrayList<Tenant>();
			for(DomainTenantUser tenantUser : tenantUsers){
				Tenant project = tenantMapper.selectByPrimaryKey(tenantUser.getOstenantid());
				if(null == project)
					continue;
				makeChildTenants(project);
				tenants.add(project);
			}
		}
//		makeChildTenants(tenant);
//		tenants = new ArrayList<Tenant>();
//		if(tenant.getId().equals(cloudconfig.getOs_authtenantid()))
//			tenants.addAll(tenant.getTenants());
//		else
//			tenants.add(tenant);		
//		if(cloudUserService.checkIsAdmin(ostoken)){
//			tenants = tenantMapper.selectAllList();
//		}else{
//			Tenant tenant = tenantMapper.selectByPrimaryKey(ostoken.getTenantid());
//			makeChildTenant(tenant);
//			tenants = new ArrayList<Tenant>();
//			tenants.add(tenant);
//		}
//		if(!Util.isNullOrEmptyList(tenants))
//			return tenants;
//		String region = ostoken.getCurrentRegion();// we should get the regioninfo by the guiTokenId
//		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_IDENTIFY, region).getPublicURL();
//		url = RequestUrlHelper.createFullUrl(url + "/projects", paramMap);
//        Locale locale = new Locale(ostoken.getLocale());
//		Map<String, String> rs = httpClient.httpDoGet(url, ostoken.getTokenid());
//		Util.checkResponseBody(rs,locale);
//		String failedError = Util.getFailedReason(rs);
//		if(!Util.isNullOrEmptyValue(failedError))
//			log.error(failedError);
//		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//		switch (httpCode) {
//		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:
//		case ParamConstant.NORMAL_GET_RESPONSE_CODE: {
//			try {
//				tenants = getTenants(rs, ostoken);
//			} catch (IOException e) {
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			break;
//		}
//		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
//			String tokenid = "";// TODO reget the token id
//			try {
//				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
//				tokenid = newToken.getTokenid();
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			rs = httpClient.httpDoGet(url, tokenid);
//			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//			failedError = Util.getFailedReason(rs);
//			if(!Util.isNullOrEmptyValue(failedError))
//				log.error(failedError);
//			try {
//				tenants = getTenants(rs, ostoken);
//			} catch (IOException e) {
//				log.error(e);
//				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE, ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
//			}
//			break;
//		}
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
//		default:
//			throw new ResourceBusinessException(Message.CS_TENANT_GET_FAILED,httpCode,locale);
//		}
//		
//		storeTenants2DB(tenants);
		return tenants;
	}


	
//	private List<Tenant> getTenants(Map<String, String> rs, TokenOs ostoken)
//			throws JsonProcessingException, IOException, BusinessException {
//		ObjectMapper mapper = new ObjectMapper();
//		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
//		JsonNode tenantsNode = rootNode.path(ResponseConstant.PROJECTS);
//		int tenantsCount = tenantsNode.size();
//		if (0 == tenantsCount)
//			return null;
//
//		List<Tenant> tenants = new ArrayList<Tenant>();
//		for (int index = 0; index < tenantsCount; ++index) {
//			Tenant tenant = getTenant(tenantsNode.get(index));
//			if (null == tenant)
//				continue;
//			tenants.add(tenant);
//		}
//		return tenants;
//	}

//	private Tenant getTenant(JsonNode tenantNode) {
//		if (null == tenantNode)
//			return null;
//		Tenant tenant = new Tenant();
//		tenant.setDescription(tenantNode.path(ResponseConstant.DESCRIPTION).textValue());
//		tenant.setEnabled(tenantNode.path(ResponseConstant.ENABLED).booleanValue());
//		tenant.setId(tenantNode.path(ResponseConstant.ID).textValue());
//		tenant.setName(tenantNode.path(ResponseConstant.NAME).textValue());
//		return tenant;
//	}


	@Override
	public Tenant getTenant(String id,TokenOs ostoken) throws BusinessException {
		Tenant tenant = tenantMapper.selectByPrimaryKey(id);
		if(null == tenant)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		List<DomainTenantUser> tenantUsers = domainTenantUserMapper.selectListByTenantId(id);
		if(Util.isNullOrEmptyList(tenantUsers))
			return tenant;
		List<String> ids = new ArrayList<String>();
		for(DomainTenantUser tenantUser : tenantUsers){
			ids.add(tenantUser.getClouduserid());
		}
		List<CloudUser> users = cloudUserMapper.selectUserByIds(ids);
		if(!Util.isNullOrEmptyList(users)){
			for(CloudUser user : users){
				List<UserRoleTenant> userRoleTenants = userRoleTenantMapper.selectByUserTenantId(user.getUserid(),id);
				if(Util.isNullOrEmptyList(userRoleTenants))
					continue;
				List<String> roleIds = new ArrayList<String>();
				for(UserRoleTenant userRoleTenant : userRoleTenants){
					roleIds.add(userRoleTenant.getRoleId());
				}
				user.setRole(Util.listToString(roleIds, ','));	
			}
		}
		tenant.setUsers(users);
		return tenant;
	}
	
	@Override
	public Tenant createTenant(String body,TokenOs ostoken) throws BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(body);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		String domainId = null;
		Project project = new Project();
		if(!rootNode.path(ParamConstant.DESCRIPTION).isMissingNode())
			project.setDescription(rootNode.path(ParamConstant.DESCRIPTION).textValue());
		if(!rootNode.path(ParamConstant.DOMAIN_ID).isMissingNode())
			domainId = rootNode.path(ParamConstant.DOMAIN_ID).textValue();
		else
			domainId = cloudconfig.getOs_authdomainid();
		project.setDomain_id(domainId);
	
	
		String name = StringHelper.string2Ncr(rootNode.path(ParamConstant.NAME).textValue());
		checkName(name,new Locale(ostoken.getLocale()));
		
		project.setName(name);
		project.setEnabled(true);
		String parentId = null;
		if(!rootNode.path(ParamConstant.PARENT_ID).isMissingNode()){
			Tenant tenant = tenantMapper.selectByPrimaryKey(rootNode.path(ParamConstant.PARENT_ID).textValue());
			if(null == tenant)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
			parentId = tenant.getId();
		}else{
			parentId = cloudconfig.getOs_authtenantid(); //maybe we need to change it later
		}
		project.setParent_id(parentId);	
		Project newProject = createProject(project, ostoken);
    	quotaService.createTenantQuota(newProject.getId(),new Locale(ostoken.getLocale()));

		return tenantMapper.selectByPrimaryKey(newProject.getId());
		
//		Tenant tenant = new Tenant();
//		tenant.setId(newProject.getId());
//		tenant.setDescription(project.getDescription());
//		tenant.setName(rootNode.path(ParamConstant.NAME).textValue());
//		tenant.setDomain_id(domainId);
//		tenant.setEnabled(true);
//		tenant.setParent_id(project.getParent_id());
//		return null;
	}
	
	private String getTenantUpdateBody(String description,String name){
		StringBuilder sb = new StringBuilder();
		if(null != description && null != name){
			sb.append("{\"project\":{");
			sb.append("\"");
			sb.append(ParamConstant.DESCRIPTION);
			sb.append("\":\"");
			sb.append(description);
			sb.append("\",\"");
			sb.append(ParamConstant.NAME);
			sb.append("\":\"");
			sb.append(name);
			sb.append("\"}}");
		}else if(null != description){
			sb.append("{\"project\":{");
			sb.append("\"");
			sb.append(ParamConstant.DESCRIPTION);
			sb.append("\":\"");
			sb.append(description);
			sb.append("\"}}");
		}else if(null != name){
			sb.append("{\"project\":{");
			sb.append("\"");
			sb.append(ParamConstant.NAME);
			sb.append("\":\"");
			sb.append(name);
			sb.append("\"}}");
		}
		return sb.toString();
	}
	
	@Override
	public Tenant updateTenant(String projectId,String body,TokenOs ostoken) throws BusinessException{
		
		Locale locale = new Locale(ostoken.getLocale());

		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(body);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		
		String description = null;
		String name = null;
		if(!rootNode.path(ParamConstant.DESCRIPTION).isMissingNode())
			description = rootNode.path(ParamConstant.DESCRIPTION).textValue();
		if(!rootNode.path(ParamConstant.NAME).isMissingNode())
			name = StringHelper.string2Ncr(rootNode.path(ParamConstant.NAME).textValue());
		checkName(name,locale);
	
		String updateBody = getTenantUpdateBody(description,name);
		
		String url = cloudconfig.getOs_authurl();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/projects/");
		sb.append(projectId);
		
		Map<String, String> rs = httpClient.httpDoPatch(sb.toString(),ostoken.getTokenid(),updateBody);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
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
			rs =  httpClient.httpDoPost(sb.toString(), tokenid, updateBody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE )
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
			throw new ResourceBusinessException(Message.CS_TENANT_UPDATE_FAILED,httpCode,locale);
		}
		
		Tenant tenant = tenantMapper.selectByPrimaryKey(projectId);
		if(null == tenant)
			return null;
		if(null != description)
			tenant.setDescription(description);
		if(null != name)
			tenant.setName(name);
		tenantMapper.insertOrUpdate(tenant);
		return tenant;
	}
	
	@Override
	public List<String> getTenantNamesByUser(CloudUser user){
		List<Tenant> tenants = null;
		if(user.getAccount().equals(cloudconfig.getOs_authuser())){
			tenants = tenantMapper.selectAllList();
		}else{
			List<DomainTenantUser> tenantUsers = domainTenantUserMapper.selectListByUserId(user.getUserid());
			if(Util.isNullOrEmptyList(tenantUsers))
				return null;
			List<String> tenantIds = new ArrayList<String>();
			for(DomainTenantUser tenantUser : tenantUsers)
				tenantIds.add(tenantUser.getOstenantid());
			tenants = tenantMapper.selectTenantsByIds(tenantIds);
		}
		if(Util.isNullOrEmptyList(tenants))
			return null;
		List<String> tenantNames = new ArrayList<String>();
		for(Tenant tenant : tenants)
			tenantNames.add(StringHelper.ncr2String(tenant.getName()));
		return tenantNames;
	}
		
	@Override
	public void deleteTenant(String tenantId,TokenOs ostoken) throws BusinessException{
		List<CloudUser> users = cloudUserMapper.selectUsersByTenantId(tenantId);
		if(!Util.isNullOrEmptyList(users))
			throw new ResourceBusinessException(Message.CS_HAVE_EXISTED_USER,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		deleteProject(tenantId,ostoken);
		
		quotaService.deleteTenantQuota(ostoken, tenantId);
	//	Tenant tenant = tenantMapper.selectByPrimaryKey(tenantId);
	//	this.deleteProject(tenant.getId(), ostoken);
	//	return tenant;
	}

	@Override
	public Tenant addUsersToTenant(String tenantId, String body, TokenOs ostoken) throws BusinessException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(body);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, ParamConstant.BAD_REQUEST_RESPONSE_CODE,
					new Locale(ostoken.getLocale()));
		}
	    
		JsonNode idsNode = rootNode.path(ResponseConstant.IDS);
		int idsCount = idsNode.size();
		List<String> usersId = new ArrayList<String>();
		for (int index = 0; index < idsCount; ++index) {
			usersId.add(idsNode.get(index).textValue());
		}
		List<CloudUser> users = cloudUserMapper.selectUserByIds(usersId);
		Tenant tenant = tenantMapper.selectByPrimaryKey(tenantId);

		if (Util.isNullOrEmptyList(users) || null == tenant)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, ParamConstant.BAD_REQUEST_RESPONSE_CODE,
					new Locale(ostoken.getLocale()));

		for (CloudUser user : users) {
			userService.addUserToProject(user,tenant.getId(), ostoken);
		//	user.setOsTenantId(tenant.getId());
		}
		//cloudUserMapper.insertOrUpdateBatch(users);
		return tenant;
	}
	
	@Override
	public void assignUserRoleToTenant(TokenOs ostoken,String tenantId,String body) throws BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(body);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, ParamConstant.BAD_REQUEST_RESPONSE_CODE,
					new Locale(ostoken.getLocale()));
		}
	    String userId = rootNode.path(ParamConstant.USERID).textValue();
	    String roleId = rootNode.path(ParamConstant.ROLEID).textValue();
	    
	    /*
	    CloudUser cloudUser = cloudUserMapper.selectByPrimaryKey(userId);
	    if(null != cloudUser){
	    	if(0 != userRoleTenantMapper.deleteByUserTenantId(userId, tenantId)){
	    		String[] roleIds = roleId.split(",");
	    		for(int index = 0 ;index < roleIds.length; ++index){
	    			try{
	    				roleService.removeRoleUserFromProject(roleIds[index], cloudUser.getOsUserId(), tenantId);		
	    			}catch(Exception e){
	    				log.error("error",e);
	    			}
	    		}
	    	}   
	    }*/
	    assignUserRoleToTenant(userId,roleId,tenantId);
	}
	
	@Override
	public void assignUserRoleToTenant(String userId,String roleId,String tenantId){
//		UserRoleTenant userRoleTenant = userRoleTenantMapper.selectByUserRoleTenantId(userId, roleId, tenantId);
//		if(null == userRoleTenant){
//			userRoleTenant = new UserRoleTenant();
//			userRoleTenant.setId(Util.makeUUID());
//		}
		List<UserRoleTenant> userRoleTenants = new ArrayList<UserRoleTenant>();
		String[] roleIds = roleId.split(",");
		for(int index = 0; index < roleIds.length; ++index){
			UserRoleTenant userRoleTenant  = new UserRoleTenant();
			userRoleTenant.setId(Util.makeUUID());
			userRoleTenant.setRoleId(roleIds[index]);
			userRoleTenant.setUserId(userId);
			userRoleTenant.setTenantId(tenantId);
			userRoleTenants.add(userRoleTenant);
		}
		userRoleTenantMapper.insertOrUpdateBatch(userRoleTenants);
	}
	
	@Override
	public void removeUserRoleFromTenant(String userId,String roleId,String tenantId){
		userRoleTenantMapper.deleteByUserRoleTenantId(userId, roleId, tenantId);
	}
	

	@Override
	public Tenant removeUsersFromTenant(String tenantId, String body, TokenOs ostoken) throws BusinessException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(body);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, ParamConstant.BAD_REQUEST_RESPONSE_CODE,
					new Locale(ostoken.getLocale()));
		}
		JsonNode idsNode = rootNode.path(ResponseConstant.IDS);
		int idsCount = idsNode.size();
		List<String> usersId = new ArrayList<String>();
		for (int index = 0; index < idsCount; ++index) {
			usersId.add(idsNode.get(index).textValue());
		}
		
		Locale locale = new Locale(ostoken.getLocale());
		List<CloudUser> users = cloudUserMapper.selectUserByIds(usersId);
		checkUsers(users,tenantId,locale);
		
		Tenant tenant = tenantMapper.selectByPrimaryKey(tenantId);

		if (Util.isNullOrEmptyList(users) || null == tenant)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG, ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);

		for (CloudUser user : users) {
			userService.removeUserFromProject(user, tenantId, ostoken);
			userRoleTenantMapper.deleteByUserTenantId(user.getUserid(), tenantId);
		}
	//	cloudUserMapper.insertOrUpdateBatch(users);
		return tenant;
	}
	
	@Override
	public Project createProject(Project project,TokenOs ostoken) throws BusinessException {
		// step 1: 浣跨敤榛樿閰嶇疆admin鐢ㄦ埛鍒涘缓绉熸埛
		if (project == null)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		
		String url = cloudconfig.getOs_authurl();
		url = RequestUrlHelper.createFullUrl(url + "/projects", null);

		// 鐢熸垚post body浣?
		String postbody = new JsonHelper<Project, String>().generateJsonBodySimple(project, ParamConstant.PROJECT);
		log.debug("postbody:" + postbody);

		Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = httpClient.httpDoPost(url, ostoken.getTokenid(), postbody);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE:{
			project = getProject(rs);
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
			rs =  httpClient.httpDoPost(url, tokenid, postbody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE )
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			project = getProject(rs);
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
			throw new ResourceBusinessException(Message.CS_TENANT_CREATE_FAILED,httpCode,locale);
		}
		storeProject(project);
		return project;
	}
	
	@Override
	public Project getProject(String projectId,TokenOs ostoken) throws BusinessException{
		Locale locale = new Locale(ostoken.getLocale());
		String url = cloudconfig.getOs_authurl();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/projects/");
		sb.append(projectId);
		
		Map<String, String> rs = httpClient.httpDoGet(sb.toString(), ostoken.getTokenid());
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
	
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		Project project  = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(rs.get("jsonbody"));
				JsonNode projectNode = rootNode.path(ResponseConstant.PROJECT);
				project = mapper.readValue(projectNode.toString(), Project.class);
				return project;
			} catch (Exception e) {
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
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
			rs =  httpClient.httpDoGet(sb.toString(), tokenid);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
//			if (null != response)
//				response.setStatus(httpCode);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE )
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(rs.get("jsonbody"));
				JsonNode projectNode = rootNode.path(ResponseConstant.PROJECT);
				project = mapper.readValue(projectNode.toString(), Project.class);
                return project;  
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
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
			throw new ResourceBusinessException(Message.CS_TENANT_DETAIL_GET_FAILED,httpCode,locale);
		}
	}
	
	@Override
	public void deleteProject(String projectId,TokenOs ostoken) throws BusinessException{
		
		Locale locale = new Locale(ostoken.getLocale());
		checkProject(projectId,locale);
		
		String url = cloudconfig.getOs_authurl();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/projects/");
		sb.append(projectId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
		Map<String, String> rs = httpClient.httpDoDelete(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE:{
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
			rs =  httpClient.httpDoDelete(sb.toString(), tokenid);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE )
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
			throw new ResourceBusinessException(Message.CS_TENANT_DELETE_FAILED,httpCode,locale);
		}
		deleteProject(projectId);
	}
	

	@Override
	public Tenant getCurrentTenant(TokenGui guitoken) throws Exception {
		// TODO Auto-generated method stub
		return this.getTenant(guitoken, guitoken.getTenantid());
	}

	@Override
	public Tenant getCurrentParentTenant(TokenGui guitoken) throws Exception {
		// TODO Auto-generated method stub
		String parentTenantId = this.getCurrentTenant(guitoken).getParent_id();
		if(Util.isNullOrEmptyValue(parentTenantId)){
			return tenantMapper.selectByName("admin");
		}
		return this.getTenant(guitoken, this.getCurrentTenant(guitoken).getParent_id());
	}

	@Override
	public String updateTenantToken(TokenGui guitoken,String locale) throws Exception {
		DomainTenantUser tenantUser = domainTenantUserMapper.selectByPrimaryKey(guitoken.getTenantuserid());
		if(null == tenantUser)
			return null;
		CloudUser user = cloudUserMapper.selectByPrimaryKey(tenantUser.getClouduserid());
		if(null == user)
			return null;
		user.setLocale(locale);
		cloudUserMapper.updateByPrimaryKeySelective(user);
		guitoken.setLocale(locale);
		
		return JWTTokenHelper.createEncryptToken(guitoken, user,cloudUserService.checkIsAdmin(tenantUser.getClouduserid()));
	}
	
	@Override
	public Tenant getTenant(TokenGui guitoken, String tenantId) throws Exception {
		// TODO Auto-generated method stub
		
		Tenant tenant=tenantMapper.selectByPrimaryKey(tenantId);
		if(tenant!=null){
			return tenant;
		}else{
		//	TokenOs ot = authService.insertCheckGuiAndOsToken(guitoken.getTokenid()); //it should execute it with admin role
			TokenOs ot = authService.createDefaultAdminOsToken();
			String url = cloudconfig.getOs_authurl();
			url = RequestUrlHelper.urlPlus(url,"/projects/"+tenantId);

			Map<String, String> rs = httpClient.httpDoGet(url, ot.getTokenid());
			// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());
			String failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
		
//			if (Integer.parseInt(rs.get("httpcode")) >= ERROR_HTTP_CODE) {
//				log.error("wo cha:create project request failed");
//				throw new Exception("create project request failed");
//			}
			Locale locale = new Locale(guitoken.getLocale());
			int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//			if (null != response)
//				response.setStatus(httpCode);
			switch (httpCode) {
			case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
				try {
					ObjectMapper mapper = new ObjectMapper();
					JsonNode rootNode = mapper.readTree(rs.get("jsonbody"));
					JsonNode projectNode = rootNode.path(ResponseConstant.PROJECT);
					log.debug("projectNode.asText():" + projectNode.asText());
					log.debug("projectNode.toString():" + projectNode.toString());
					Project project = mapper.readValue(projectNode.toString(), Project.class);
					storeProject(project);
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					log.error("Failed to get tenant !"+ e);
					throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
				}
				break;
			}
			case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
				String tokenid = "";// TODO reget the token id
				try {
					TokenOs newToken = authService.createNewToken(ot.getTenantUserid(),ot.getCurrentRegion(),ot.getLocale());
					tokenid = newToken.getTokenid();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
				}
				rs =  httpClient.httpDoGet(url, tokenid);
				httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
				failedError = Util.getFailedReason(rs);
				if(!Util.isNullOrEmptyValue(failedError))
					log.error(failedError);
//				if (null != response)
//					response.setStatus(httpCode);
				if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE )
					throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
				try {
					ObjectMapper mapper = new ObjectMapper();
					JsonNode rootNode = mapper.readTree(rs.get("jsonbody"));
					JsonNode projectNode = rootNode.path(ResponseConstant.PROJECT);
					log.debug("projectNode.asText():" + projectNode.asText());
					log.debug("projectNode.toString():" + projectNode.toString());
					Project project = mapper.readValue(projectNode.toString(), Project.class);
					storeProject(project);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					log.error("Failed to get tenant !"+ e);
					throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
				throw new ResourceBusinessException(Message.CS_TENANT_DETAIL_GET_FAILED,httpCode,locale);
			}
			
//			try {
//				ObjectMapper mapper = new ObjectMapper();
//				JsonNode rootNode = mapper.readTree(rs.get("jsonbody"));
//				JsonNode projectNode = rootNode.path(ResponseConstant.PROJECT);
//				log.debug("projectNode.asText():" + projectNode.asText());
//				log.debug("projectNode.toString():" + projectNode.toString());
//				Project project = mapper.readValue(projectNode.toString(), Project.class);
//				tenant = new Tenant();
//				tenant.setId(project.getId());
//				tenant.setName(project.getName());
//				tenant.setDescription(project.getDescription());
//				tenant.setDomain_id(project.getDomain_id());
//                tenant.setEnabled(project.getEnabled());
//                tenant.setParent_id(project.getParent_id());
//                tenantMapper.insertSelective(tenant);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				log.error(e);
//				log.error("wo cha:get tenant or insert db failed锛?+e.getMessage());
//				throw e;
//			}
			//return project;			
		}
		return tenant;
	}

	@Override
	public Tenant getTenant(TokenOs ostoken, String tenantId) throws Exception {
		// TODO Auto-generated method stub
		
		Tenant tenant=tenantMapper.selectByPrimaryKey(tenantId);
		if(tenant!=null){
			return tenant;
		}else{
			TokenOs ot = ostoken;
			String url = cloudconfig.getOs_authurl();
			url = RequestUrlHelper.urlPlus(url,"/projects/"+tenantId);

			Map<String, String> rs = httpClient.httpDoGet(url, ot.getTokenid());
			// Map<String, String> rs =client.httpDoGet(url, ot.getTokenid());

			String failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);

			Locale locale = new Locale(ostoken.getLocale());
			int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			Project project = null;
			switch (httpCode) {
			case ParamConstant.NORMAL_SYNC_RESPONSE_CODE:{
				  project = getProject(rs);
				  tenant = this.storeProject(project);
				  break;
			}
			case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
				String tokenid = "";// TODO reget the token id
				try {
					TokenOs newToken = authService.createNewToken(ot.getTenantUserid(),ot.getCurrentRegion(),ot.getLocale());
					tokenid = newToken.getTokenid();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
				}
				rs =  httpClient.httpDoGet(url, tokenid);
				httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
				failedError = Util.getFailedReason(rs);
				if(!Util.isNullOrEmptyValue(failedError))
					log.error(failedError);
				if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE )
					throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
				project = getProject(rs);
				tenant = this.storeProject(project);
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
				throw new ResourceBusinessException(Message.CS_TENANT_DETAIL_GET_FAILED,httpCode,locale);
			}			
		}
		return tenant;
	}

	@Override
	public List<CloudUser> getSubTenantUserList(TokenGui guitoken, String tenantId) throws Exception {
		// TODO Auto-generated method stub	
		List<CloudUser> list=null;
		list=tenantMapper.selectUserListByParentId(tenantId);
		
		//TODO get data from os 	
		return list;
	}

	@Override
	public List<CloudUser> gettCurrentSubTenantUserList(TokenGui guitoken) throws Exception {
		// TODO Auto-generated method stub
		return this.getSubTenantUserList(guitoken, guitoken.getTenantid());
	}

	private void checkProject(String projectId,Locale locale) throws ResourceBusinessException{
		Tenant tenant = tenantMapper.selectByPrimaryKey(projectId);
		if(null == tenant)
			return;
		if(!Util.isNullOrEmptyValue(tenant.getParent_id()))
			throw new ResourceBusinessException(Message.CS_PROJECT_HAVE_CHILD,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		
		List<DomainTenantUser> users = domainTenantUserMapper.selectListByTenantId(projectId);
		if(!Util.isNullOrEmptyList(users))
			throw new ResourceBusinessException(Message.CS_PROJECT_HAVE_USER,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
	}
	
	private void checkUsers(List<CloudUser> users, String tenantId,Locale locale) throws BusinessException{
		if(Util.isNullOrEmptyList(users))
			return;
		for(CloudUser user : users){
			if(user.getTenantId().equals(tenantId))
				throw new ResourceBusinessException(Message.CS_DEFAULT_PROJECT_REMOVE,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
	}
	
	private void checkName(String name,Locale locale) throws BusinessException{
		if(Util.isNullOrEmptyValue(name))
			return;
		List<Tenant> tenants = tenantMapper.selectAllList();
		if(Util.isNullOrEmptyList(tenants))
			return;
		for(Tenant tenant : tenants){
			if(name.equals(tenant.getName()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_NAME_IS_SAME,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		}
		return;
	}
	
	private Tenant storeProject(Project project){
		if(null == project)
			return null;
		Tenant tenant = new Tenant();
		tenant.setId(project.getId());
		tenant.setName(project.getName());
		tenant.setDescription(project.getDescription());
		tenant.setDomain_id(project.getDomain_id());
        tenant.setEnabled(project.getEnabled());
        tenant.setParent_id(project.getParent_id());
        tenant.setMillionSeconds(Util.getCurrentMillionsecond());
        QuotaTemplate template = quotaTemplateMapper.selectDefaultTemplate();
        if(null != template)
        	tenant.setQuota_template_id(template.getId());
        tenantMapper.insertSelective(tenant);
        
        return tenant;
	}
	
	private void deleteProject(String projectId){
        tenantMapper.deleteByPrimaryKey(projectId);
	}
	
//	private void storeTenants2DB(List<Tenant> tenants){
//		if(Util.isNullOrEmptyList(tenants))
//			return;
//		tenantMapper.insertOrUpdateBatch(tenants);
//	}
	
	private Project getProject(Map<String, String> rs){
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(rs.get("jsonbody"));
		} catch (Exception e) {
			log.error(e);
			return null;
		}
		JsonNode projectNode = rootNode.path(ResponseConstant.PROJECT);
		return getProjectInfo(projectNode);
	}
	
	private Project getProjectInfo(JsonNode projectNode){
		ObjectMapper mapper = new ObjectMapper();
		Project project = null;
		try {
			project = mapper.readValue(projectNode.toString(), Project.class);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e);
            return null;
		}
        return project;
	}
}
