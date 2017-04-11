package com.cloud.cloudapi.service.common.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.BillingMapper;
import com.cloud.cloudapi.dao.common.BillingReportMapper;
import com.cloud.cloudapi.dao.common.CloudRoleMapper;
import com.cloud.cloudapi.dao.common.CloudUserMapper;
import com.cloud.cloudapi.dao.common.DomainTenantUserMapper;
import com.cloud.cloudapi.dao.common.RatingTemplateMapper;
import com.cloud.cloudapi.dao.common.SuperMapper;
import com.cloud.cloudapi.dao.common.TemplateVersionMapper;
import com.cloud.cloudapi.dao.common.TenantEndpointMapper;
import com.cloud.cloudapi.dao.common.TenantMapper;
import com.cloud.cloudapi.dao.common.TokenGuiMapper;
import com.cloud.cloudapi.dao.common.UserBindInfoMapper;
import com.cloud.cloudapi.dao.common.UserRoleTenantMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.CloudRole;
import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.common.DomainTenantUser;
import com.cloud.cloudapi.pojo.common.TenantEndpoint;
import com.cloud.cloudapi.pojo.common.TokenGui;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.TokenOsEndPoint;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.crm.UserBindInfo;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Tenant;
import com.cloud.cloudapi.pojo.openstackapi.forgui.UserRoleTenant;
import com.cloud.cloudapi.pojo.openstackapi.foros.Project;
import com.cloud.cloudapi.pojo.openstackapi.foros.Role;
import com.cloud.cloudapi.pojo.openstackapi.foros.User;
import com.cloud.cloudapi.pojo.rating.Billing;
import com.cloud.cloudapi.pojo.rating.BillingReport;
import com.cloud.cloudapi.pojo.rating.RatingTemplate;
import com.cloud.cloudapi.pojo.rating.TemplateTenantMapping;
import com.cloud.cloudapi.pojo.rating.TemplateVersion;
import com.cloud.cloudapi.service.common.CloudRegionService;
import com.cloud.cloudapi.service.common.CloudRoleService;
import com.cloud.cloudapi.service.common.CloudUserService;
import com.cloud.cloudapi.service.openstackapi.QuotaService;
import com.cloud.cloudapi.service.openstackapi.RoleService;
import com.cloud.cloudapi.service.openstackapi.TenantService;
import com.cloud.cloudapi.service.openstackapi.UserService;
import com.cloud.cloudapi.service.rating.BillingService;
import com.cloud.cloudapi.service.rating.RatingTemplateService;
import com.cloud.cloudapi.util.ExceptionMessage;
import com.cloud.cloudapi.util.JWTTokenHelper;
import com.cloud.cloudapi.util.MD5Helper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.StringHelper;

@Service
public class CloudUserServiceImpl extends SuperDaoServiceImpl<CloudUser, String> implements CloudUserService {
   
	@Resource
    private CloudUserMapper cloudUserMapper;

    @Resource
    private TenantEndpointMapper tenantEndpointMapper;
  
    @Resource
    private DomainTenantUserMapper domainTenantUserMapper;

	@Autowired
	private TokenGuiMapper tokenGuiMapper;
	
	@Autowired
	private BillingReportMapper billingReportMapper;

	@Autowired
	private BillingMapper billingMapper;
	
    @Resource
    private CloudRegionService cloudRegionService;
	
    @Resource
	private CloudConfig cloudconfig; 
   
	@Resource
    private TenantService tenantService;
   
    @Resource
    private UserService   userService;
  
    @Resource
    private RoleService   roleService;	
	
    @Resource
	private QuotaService quotaService; 
	
	@Resource
	private BillingService billingService;
	
    @Resource
	private CloudRoleMapper cloudRoleMapper;
    
    @Resource
    private DomainTenantUserMapper  tenantUserMapper;
    
//    @Resource
//    private UserRoleMapper userRoleMapper;
    
	@Resource
	private CloudRoleService cloudRoleService; 
	
	@Resource
	private UserBindInfoMapper userBindInfoMapper;
	
	@Autowired
	private RatingTemplateMapper ratingTemplateMapper;
	
	@Autowired
	private TemplateVersionMapper templateVersionMapper;
	
    @Resource
	private UserRoleTenantMapper userRoleTenantMapper;
	
    @Resource
	private TenantMapper tenantMapper;
    
	@Resource
	private RatingTemplateService ratingService; 
	
    private Logger log = LogManager.getLogger(CloudUserServiceImpl.class);

	@Override
	public SuperMapper<CloudUser, String> getMapper(){
		// TODO Auto-generated method stub
		return this.cloudUserMapper;
	}

//	@Override
//	public void addRoles(String userId,String body,TokenOs ostoken) throws ResourceBusinessException{
//		
//		ObjectMapper mapper = new ObjectMapper();
//		JsonNode rootNode = null;
//		try {
//			rootNode = mapper.readTree(body);
//		} catch (Exception e) {
//			log.error(e);
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
//		}
//		
//		JsonNode idsNode = rootNode.path(ResponseConstant.IDS);
//		int idsCount = idsNode.size();
//		if(0 == idsCount)
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
//
//		List<String> rolesId = new ArrayList<String>();
//		for (int index = 0; index < idsCount; ++index) {
//			rolesId.add(idsNode.get(index).textValue());
//		}
//		        
//        UserRole userRole = userRoleMapper.selectRoleIdByUserId(userId);
//        if(null == userRole){
//        	userRole = new UserRole();
//        	userRole.setId(Util.makeUUID());
//        	userRole.setUserId(userId);
//        	userRole.setRoleId(Util.listToString(rolesId, ','));
//        	userRoleMapper.insertSelective(userRole);
//        }else{
//         	 userRole.setRoleId(Util.getAppendedIds(userRole.getRoleId(), rolesId));
//         	 userRoleMapper.updateByPrimaryKeySelective(userRole);
//        }
//	}
//	
	@Override
	public List<CloudUser> selectList()throws BusinessException, Exception{
		// TODO Auto-generated method stub
		List<CloudUser> userlist=cloudUserMapper.selectList();
		return userlist;
	}

	@Override
	public List<CloudUser> selectListForPage(int start, int end)throws BusinessException, Exception {
		// TODO Auto-generated method stub
		List<CloudUser> userlist=cloudUserMapper.selectListForPage(start, end);
		return userlist;
	}

	@Override
	public CloudUser selectUserByOsTokenId(String osTokenId) throws BusinessException, Exception {
		// TODO Auto-generated method stub
		return cloudUserMapper.selectByOsTokenId(osTokenId);
	}

	@Override
	public CloudUser selectUserByGuiTokenId(String guiTokenId) throws BusinessException, Exception {
		// TODO Auto-generated method stub
		return cloudUserMapper.selectByGuiTokenId(guiTokenId);
	}	
	
	
	@Override
	public int countNum() throws BusinessException, Exception{
		// TODO Auto-generated method stub
		int num=this.cloudUserMapper.countNum();
		return num;
	}
	
	@Override
	public Boolean checkIsAdmin(TokenOs ostoken){
		CloudUser user = cloudUserMapper.selectByOsTokenId(ostoken.getTokenid());
		if(null == user)
			return false;
		return checkIsAdmin(user.getUserid());
	}
	
	@Override
	public Boolean checkIsAdmin(String clouduserId) {
	//	boolean isAdmin = false;
		List<UserRoleTenant> userRoleTenants = userRoleTenantMapper.selectByUserId(clouduserId);
		if(Util.isNullOrEmptyList(userRoleTenants))
			return false;
		List<String> roleIds = new ArrayList<String>();
		for(UserRoleTenant userRoleTenant : userRoleTenants){
			if(roleIds.contains(userRoleTenant.getRoleId()))
				continue;
			roleIds.add(userRoleTenant.getRoleId());
		}
		List<CloudRole> roles = cloudRoleMapper.selectRoleByIds(roleIds);
	    if(null == roles)
	    	return false;
	    for(CloudRole role : roles){
	    	if (Util.ADMIN_ROLE_SIGN.equals(role.getRoleSign()))
				return true;
	    }
//		CloudRole role = cloudRoleMapper.selectByPrimaryKey(userRole.getRoleId());
//		if(null == role)
//			return false;
//		if (Util.ADMIN_ROLE_SIGN.equals(role.getRoleSign())){
//			return true;
//		}
//		List<CloudRole> roleList =  cloudRoleMapper.selectListByUserId(clouduserId);
//		for(CloudRole role : roleList){
//			if (UserRolePermissionConstant.ADMIN_ROLE_SIGN.equals(role.getRoleSign())){
//				isAdmin = true;
//				break;
//			}
//		}
		return false;
	}
	
	@Override
	public Boolean checkIsSystemAdmin(String clouduserId) {
	//	boolean isAdmin = false;
		CloudUser user = cloudUserMapper.selectByPrimaryKey(clouduserId);
		if(null == user)
			return false;
		if(user.getAccount().equals(cloudconfig.getSystemAdminDefaultAccount()))
			return true;
		return false;
	}
	
	@Override
	/**
	 * 得到系统用户一览
	 */
	public List<CloudUser> getCloudUsersList() throws Exception {
		
		List<CloudUser> list =  new ArrayList<CloudUser>();
		list = cloudUserMapper.selectListWithTenant();
		for (CloudUser user : list) {
			// 不显示密码
			user.setPassword(null);
		}
		return list;
	}
	
	@Override
	/**
	 * 得到用户的详细信息
	 */
	public CloudUser getCloudUserDetail(TokenOs token, String userId) throws BusinessException {
		CloudUser user = this.cloudUserMapper.selectWithTenantByPrimaryKey(userId);
		if(null == user)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(token.getLocale()));
		// 不显示密码
		//user.setPassword(null);
		String tenantId = user.getTenantId();
		List<BillingReport> reports = billingReportMapper.selectByTenantId(tenantId);
		if(null == reports)
			return user;
		Billing defaultAccount = billingMapper.selectDefaultUserAccount(userId);
		for (BillingReport report : reports) {
			if (null == report.getBillingId())
				report.setBilling(defaultAccount);
			else
				report.setBilling(billingMapper.selectByPrimaryKey(report.getBillingId()));
		}
		user.setBillings(reports);
		return user;
	}
	
	@Override
	public String updateUserProject(TokenGui token,String name) throws BusinessException{
		DomainTenantUser tenantUser = domainTenantUserMapper.selectByPrimaryKey(token.getTenantuserid());
		if(null == tenantUser)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(token.getLocale()));
		CloudUser cloudUser = cloudUserMapper.selectByPrimaryKey(tenantUser.getClouduserid());
		if(null == cloudUser)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(token.getLocale()));
		Tenant tenant = tenantMapper.selectByName(StringHelper.string2Ncr(name));
		if(null == tenant)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(token.getLocale()));
		cloudUser.setCurrentTenantId(tenant.getId());
		cloudUserMapper.updateByPrimaryKeySelective(cloudUser);
		token.setTenantname(name);
		
		return JWTTokenHelper.createEncryptToken(token, cloudUser,checkIsAdmin(tenantUser.getClouduserid()));
	}
	
	/**
	 * 启用与禁用用户,reset密码
	 * @throws Exception 
	 */
	@Override
	public CloudUser operateUser(String userId,String action) throws Exception{
		
		CloudUser user = this.cloudUserMapper.selectByPrimaryKey(userId);
		if(ParamConstant.ENABLE_USER_ACTION.equals(action)){
			user.setEnabled(true);
		}else if(ParamConstant.DISABLE_USER_ACTION.equals(action)){
			user.setEnabled(false);
			DomainTenantUser tenantUser = domainTenantUserMapper.selectListByTenantAndUserId(user.getTenantId(),user.getUserid());
			if(null != tenantUser){
				List<TokenGui> tokens = tokenGuiMapper.selectListByUserId(tenantUser.getId());
				if(null != tokens){
					for(TokenGui token : tokens){
						token.setExpiresTime(Util.getCurrentMillionsecond());
					}
					tokenGuiMapper.insertOrUpdateBatch(tokens);
				}
			}
		}else if(ParamConstant.RESETPASSWORD_USER_ACTION.equals(action)){
			//采用MD5加密
       	     user.setPassword(MD5Helper.encode(ParamConstant.DEFAULT_PASSWORD));
	     }
		this.update(user);
		return user;
	}


	@Override
	public CloudUser insertUserAndTenant(CloudUser user,TokenOs amdintoken) throws BusinessException, Exception {
		// step1:重复性check： 如果此账户已存在，则抛出异常
		int num = this.cloudUserMapper.countNumByUserAccount(user.getAccount());
		if (num != 0)
			throw new ResourceBusinessException(ExceptionMessage.USER_ALREADY_EXIST,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(amdintoken.getLocale()));
		
		String parent_tenant_id = user.getParentId();
		String roleId = user.getRole();
		//if(!StringHelper.isNullOrEmpty(user.getCustomerManager())){
			//CRM connection
			
			//temp deal,cloud002's tenantid
		//	parent_tenant_id="38ed6a1cca5e49d48c1f300cdf9b47e1";
			//it need to change later
			//parent_tenant_id = amdintoken.getTenantid();
		//}

		// step2:访问openstack 根据用户"tenant_"+code为tenant名创建tenant
		Project project = null;
		if(Util.isNullOrEmptyValue(parent_tenant_id)){
			project = new Project();
			project.setName(ParamConstant.API2OS_PREFIX_TENANT+ user.getAccount());
			project.setEnabled(true);
			project.setDescription("The tenant for user:" + user.getAccount());
			project.setDomain_id(cloudconfig.getOs_authdomainid());
			project.setParent_id(cloudconfig.getOs_authtenantid());
//			TenantService tenantService = OsApiServiceFactory.getTenantService();
			try {
				project = tenantService.createProject(project,amdintoken);
			} catch (Exception e) {
				log.error("create project failed:", e);
				throw new ResourceBusinessException(Message.CS_COMPUTE_USER_CREATE_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(user.getLocale()));
			}
		}else{
			try {
				project = tenantService.getProject(parent_tenant_id,amdintoken);
			} catch (Exception e) {
				log.error("create project failed:", e);
				throw new ResourceBusinessException(Message.CS_COMPUTE_USER_CREATE_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(user.getLocale()));
			}
		}
		
		// step3:创建openstack user
		User osuser = new User();
		osuser.setDefault_project_id(project.getId());
		osuser.setDescription("project:" + project.getName() + " user:" + user.getName());
		osuser.setEmail(user.getMail());
		osuser.setEnabled(true);
		osuser.setDomain_id(cloudconfig.getOs_authdomainid());
		String os_username = ParamConstant.API2OS_PREFIX_USER + user.getAccount();
		osuser.setName(os_username);
		String os_password = ParamConstant.API2OS_PREFIX_USER_PWD + user.getAccount();
		osuser.setPassword(os_password);

//		UserService userService = OsApiServiceFactory.getUserService();
		try {
			osuser = userService.createUser(osuser,amdintoken);
		} catch (Exception e) {

			log.error("create user failed:", e);
			tenantService.deleteProject(project.getId(), amdintoken);
			throw new ResourceBusinessException(Message.CS_COMPUTE_USER_SIGN_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(user.getLocale()));

		}

		// step4:获取名字叫user的role
//		RoleService roleService = OsApiServiceFactory.getRoleService();
		Role role = null;
		try {
			roleService.setTokenOs(amdintoken);
			role = roleService.getRoleByName(ParamConstant.OS_CLOUDUSER_ROLE);
		} catch (Exception e) {
			userService.deleteUser(osuser.getId(),amdintoken);
			if(Util.isNullOrEmptyValue(parent_tenant_id))
				tenantService.deleteProject(project.getId(), amdintoken);
			log.error("get user role failed:", e);
			throw new ResourceBusinessException(Message.CS_COMPUTE_USER_SIGN_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(user.getLocale()));

		}

		// step5:user,role,project权限绑定
		try {
			roleService.setTokenOs(amdintoken);
			roleService.grantRoleToUserOnProject(role.getId(), osuser.getId(), project.getId());
		} catch (Exception e) {
			userService.deleteUser(osuser.getId(),amdintoken);
			if(Util.isNullOrEmptyValue(parent_tenant_id))
				tenantService.deleteProject(project.getId(), amdintoken);
			log.error("grant role to user on project failed:", e);
			throw new ResourceBusinessException(Message.CS_COMPUTE_USER_SIGN_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(user.getLocale()));
		}

		// step6:中间层用户信息数据库保存
		// 手动生成UUID
		user.setUserid(Util.makeUUID());
		user.setCreate_time(System.currentTimeMillis());
		user.setOsUserId(osuser.getId());
		user.setTenantId(project.getId());
		user.setCurrentTenantName(project.getName());
		user.setCurrentTenantId(project.getId());
		user.setDomainId(cloudconfig.getOs_authdomainid());
		// 设置从openstack的返回体中取出tenant 信息
//		user.setOstenantid(project.getId());
//		user.setOsdomainid(project.getDomain_id());
		try {
			//super.insert(user);
          	user.setEnabled(true);
          	//采用MD5加密
       	   // user.setPassword(MD5Helper.encode(user.getPassword()));
          	user.setPassword(user.getPassword());
          	user.setCurrentregion(cloudconfig.getOs_defaultregion());
          	//user.setLocale(Locale.getDefault().getLanguage());
          	user.setLocale(cloudconfig.getSystemDefaultLocale());
	//		cloudUserMapper.insertSelective(user);
		} catch (Exception e) {
			log.error("inter user to apidabase failed :", e);
			userService.deleteUser(osuser.getId(),amdintoken);
			if(Util.isNullOrEmptyValue(parent_tenant_id))
				tenantService.deleteProject(project.getId(), amdintoken);
			throw new ResourceBusinessException(Message.CS_COMPUTE_USER_SIGN_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(user.getLocale()));
		}

		// step7:store user teant domain 关系 to db
		DomainTenantUser oneDTU = new DomainTenantUser();
		oneDTU.setId(Util.makeUUID());
		oneDTU.setOsdomainid(project.getDomain_id());
		oneDTU.setOstenantid(project.getId());
		oneDTU.setClouduserid(user.getUserid());

		try {
			domainTenantUserMapper.insertSelective(oneDTU);
		} catch (Exception e) {
			log.error("add user to tenant and domain  failed :", e);
			userService.deleteUser(osuser.getId(),amdintoken);
			if(Util.isNullOrEmptyValue(parent_tenant_id))
				tenantService.deleteProject(project.getId(), amdintoken);
			super.delete(user.getUserid());
			throw new ResourceBusinessException(Message.CS_COMPUTE_USER_SIGN_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(user.getLocale()));
		}

		/*
		// step8:get endpoints of tenant from os 
		HttpClientForOsBase tokenclient = new HttpClientForOsBase(cloudconfig.getOs_authurl(), os_username, os_password,
				project.getDomain_id(), project.getId());
		TokenOs userToken = tokenclient.getToken();
				
		if (userToken == null) {
			throw new Exception("get token by new user failed");
		}
		List<TokenOsEndPoints> lists = userToken.getEndpointlist();
		if (lists == null) {
			userService.deleteUser(osuser.getId(),amdintoken);
			if(Util.isNullOrEmptyValue(parent_tenant_id))
				tenantService.deleteProject(project.getId(), amdintoken);
			super.delete(user.getUserid());
			throw new ResourceBusinessException(Message.CS_COMPUTE_USER_SIGN_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(user.getLocale()));
		}
		*/
		// step9: store endpoints to db
		/*
		List<String> insertEndpoints = new ArrayList<String>();
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
					oneTEPDb.setOstenantid(project.getId());
					tenantEndpointMapper.insertSelective(oneTEPDb);
					insertEndpoints.add(oneTEPDb.getId());
				}
			}
			insertWorkFlowEndPointsToDb(project.getId(),insertEndpoints);
			
		} catch (Exception e) {
			log.error("add endpoint for user-tenant failed :", e);
			userService.deleteUser(osuser.getId(),amdintoken);
			tenantService.deleteProject(project.getId(), amdintoken);
			deleteInvalidEndpoint(insertEndpoints);
			super.delete(user.getUserid());
			
			throw new ResourceBusinessException(Message.CS_COMPUTE_USER_SIGN_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(user.getLocale()));
		}*/
		
/*		//// step11: 在工作流系统中插入用户默认角�?
		String workflowUserRoleId = "";
		try {
			WorkflowUserRole workflowUserRole = new WorkflowUserRole();
			workflowUserRole.setId(Util.makeUUID());
			workflowUserRoleId = workflowUserRole.getId();
			workflowUserRole.setAccount(user.getAccount());
			workflowUserRole.setRoleName(WorkFlowConstant.DEFAULT_ROLE);
			workflowUserRole.setDescription(WorkFlowConstant.DEFAULT_DESC);
		    workflowUserRoleDao.insertSelective(workflowUserRole);
		    quotaService.createDefaultQuota(project.getId(),new Locale(user.getLocale()));
		} catch (Exception e) {
			log.error("Insert role to workflow failed", e);
			userService.deleteUser(osuser.getId(),amdintoken);
			tenantService.deleteProject(project.getId(), amdintoken);
			deleteInvalidEndpoint(insertEndpoints);
			workflowUserRoleDao.deleteByPrimaryKey(workflowUserRoleId);
			super.delete(user.getUserid());
			throw new ResourceBusinessException(Message.CS_COMPUTE_USER_SIGN_FAILED,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(user.getLocale()));
		}
*/		
	    //// step12: binding user rating template
		TemplateTenantMapping bindRating = ratingService.bindTenantRatingTemplate(amdintoken, project.getId());
		//TenantRating bindRating = bindTenantRatingTemplate(amdintoken,project.getId(),osuser.getId());
		if(null != bindRating){
			user.setTemplateId(bindRating.getTemplate_id());
			RatingTemplate template = ratingTemplateMapper.selectByPrimaryKey(bindRating.getTemplate_id());
			if(null != template)
				user.setUnitPriceName(template.getName());
			TemplateVersion version = templateVersionMapper.selectByPrimaryKey(bindRating.getVersion_id());
			if(null != version)
				user.setVersion(version.getName());
		}
		cloudUserMapper.insertSelective(user);
		
		///step13 bind default user role
		CloudRole defaultRole = null;
		if(Util.isNullOrEmptyValue(roleId)){
			defaultRole = cloudRoleMapper.selectByRoleName(Util.USER_ROLE_NAME);
		}else{
			defaultRole = cloudRoleMapper.selectByPrimaryKey(roleId);
		}
		if(null != defaultRole){
			UserRoleTenant userRole = new UserRoleTenant();
			userRole.setId(Util.makeUUID());
			userRole.setUserId(user.getUserid());
			userRole.setRoleId(defaultRole.getId());
			userRole.setTenantId(project.getId());
			userRoleTenantMapper.insertOrUpdate(userRole);
//			UserRole userRole = new UserRole();
//			userRole = new UserRole();
//			userRole.setId(Util.makeUUID());
//			userRole.setUserId(user.getUserid());
//			userRole.setRoleId(defaultRole.getId());
//			userRoleMapper.insertSelective(userRole);
		}
	
		return user;
	}
	
    @Override
	public void addEndPointsNoOSToObject(TokenOs token){
		//added workflow not openstack
		TokenOsEndPoint workflow_ep= new TokenOsEndPoint();
		workflow_ep.setAdminURL(cloudconfig.getWorkflow_url());
		workflow_ep.setInternalURL(cloudconfig.getWorkflow_url());
		workflow_ep.setPublicURL(cloudconfig.getWorkflow_url());
		workflow_ep.setRegion(cloudconfig.getOs_defaultregion());		
		token.getEndpointsMap().put(TokenOs.EP_TYPE_WORKFLOW+"_"+cloudconfig.getOs_defaultregion(), workflow_ep);	
    }
    
    @Override
    public void insertWorkFlowEndPointsToDb(String ostenantid,List<String> insertEndpoints){
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
		insertEndpoints.add(workflow_ep.getId());	
    }

	@Override
	public int bindddh(String userId, String ddh) throws Exception {
		UserBindInfo userBindInfo = new UserBindInfo();
		userBindInfo.setDdh(ddh);
		userBindInfo.setUserid(userId);
		userBindInfo.setMillionSeconds(Util.getCurrentMillionsecond());
		userBindInfo.setAction("bind");
		return userBindInfoMapper.insertSelective(userBindInfo);
	}
	
	@Override
	public int unbindddh(String userId, String ddh) throws Exception {
		UserBindInfo userBindInfo = new UserBindInfo();
		userBindInfo.setDdh(ddh);
		userBindInfo.setUserid(userId);
		userBindInfo.setMillionSeconds(Util.getCurrentMillionsecond());
		userBindInfo.setAction("unbind");
		return userBindInfoMapper.insertSelective(userBindInfo);
	}
    
   /* 
   private void deleteInvalidEndpoint(List<String> insertEndpoints){
	   if(Util.isNullOrEmptyList(insertEndpoints))
		   return;
	   tenantEndpointMapper.deleteByIds(insertEndpoints);
   }
   */

}
