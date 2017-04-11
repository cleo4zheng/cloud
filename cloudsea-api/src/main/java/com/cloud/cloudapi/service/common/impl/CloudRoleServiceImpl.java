package com.cloud.cloudapi.service.common.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.CloudRoleMapper;
import com.cloud.cloudapi.dao.common.CloudUserMapper;
import com.cloud.cloudapi.dao.common.DomainTenantUserMapper;
import com.cloud.cloudapi.dao.common.InstanceMapper;
import com.cloud.cloudapi.dao.common.TenantMapper;
import com.cloud.cloudapi.dao.common.UserBindInfoMapper;
import com.cloud.cloudapi.dao.common.UserRoleTenantMapper;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudRole;
import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.common.DomainTenantUser;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.crm.UserBindInfo;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Tenant;
import com.cloud.cloudapi.pojo.openstackapi.forgui.UserRoleTenant;
import com.cloud.cloudapi.service.common.CloudRoleService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CloudRoleServiceImpl implements CloudRoleService{

    @Resource
    private CloudUserMapper cloudUserMapper;
    
    @Resource
    private UserBindInfoMapper userBindInfoMapper;
//    @Resource
//    private UserRoleMapper userRoleMapper;
    
    @Resource
    private CloudRoleMapper cloudRoleMapper;
    
    @Resource
    private InstanceMapper instanceMapper;
	
    @Resource
    private UserRoleTenantMapper userRoleTenantMapper;
    
    @Resource
    private DomainTenantUserMapper tenantUserMapper;
    
    @Resource
    private TenantMapper tenantMapper;
    
	private Logger log = LogManager.getLogger(CloudRoleServiceImpl.class);
	
    @Override
	public List<CloudRole> getUserRoles(TokenOs ostoken,String name) throws ResourceBusinessException{
    	CloudUser user = cloudUserMapper.selectByAccount(name);
    	if(null == user)
    		 return new ArrayList<CloudRole>();
    	List<UserRoleTenant> userRoleTenants = userRoleTenantMapper.selectByUserId(user.getUserid());
    	if(Util.isNullOrEmptyList(userRoleTenants))
    		 return new ArrayList<CloudRole>();
    	List<String> roleIds = new ArrayList<String>();
    	for(UserRoleTenant userRoleTenant : userRoleTenants){
    		if(roleIds.contains(userRoleTenant.getRoleId()))
    				continue;
    		roleIds.add(userRoleTenant.getRoleId());
    	}
		List<CloudRole> roles = cloudRoleMapper.selectRoleByIds(roleIds);
		return roles;
	}
    
	@Override
	public List<CloudRole> getRoles(TokenOs ostoken) throws ResourceBusinessException{
//		CloudUser cloudUser = getCloudUser(ostoken);
//		if(cloudUser.getAccount().equals("admin")){
//			List<CloudRole> roles = cloudRoleMapper.selectList();
//			return roles;
//		}
//		UserRole role = userRoleMapper.selectRoleIdByUserId(cloudUser.getUserid()); 
//		if(null == role)
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
//		List<String> rolesId = Util.stringToList(role.getRoleId(), ",");
		List<CloudRole> roles = cloudRoleMapper.selectList();
		return roles;
	}
	
    @Override
	public CloudRole updateRole(TokenOs ostoken,String id,String body) throws ResourceBusinessException{
    	ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(body);
		} catch (Exception e) {
			log.error(e);
			throw new ResourceBusinessException(Message.CS_ROLE_UPDATE_FAILED,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
		
		CloudRole role = cloudRoleMapper.selectByPrimaryKey(id);
		if(null == role)
			throw new ResourceBusinessException(Message.CS_ROLE_UPDATE_FAILED,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
 			
		String name = rootNode.path(ResponseConstant.NAME).textValue();
		if(!rootNode.path(ResponseConstant.NAME).isMissingNode())
			role.setRoleName(name);
		if(!rootNode.path(ResponseConstant.PERMISSION).isMissingNode())
			 role.setOperationPermission(rootNode.path(ResponseConstant.PERMISSION).textValue());
		if(!rootNode.path(ResponseConstant.VALUE).isMissingNode())
			role.setDisplayPermission(rootNode.path(ResponseConstant.VALUE).textValue());
		if(!rootNode.path(ResponseConstant.DESCRIPTION).isMissingNode())
			role.setDescription(rootNode.path(ResponseConstant.DESCRIPTION).textValue());
        cloudRoleMapper.updateByPrimaryKeySelective(role);
		return role;
	}
    
	private void checkRoleName(String name,TokenOs ostoken)  throws ResourceBusinessException{
		List<CloudRole> roles = cloudRoleMapper.selectList();
		if(Util.isNullOrEmptyList(roles))
			return;
		for(CloudRole role : roles){
			if(role.getRoleName().equals(name))
				throw new ResourceBusinessException(Message.CS_ROLE_NAME_CONFLICT,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
	
	}
	@Override
	public CloudRole createRoles(String body,TokenOs ostoken) throws ResourceBusinessException, JsonProcessingException, IOException{
		
		CloudRole role = new CloudRole();
		role.setId(Util.makeUUID());
		ObjectMapper mapper = new ObjectMapper();
		JsonNode roleNode = mapper.readTree(body);
		role.setRoleName(roleNode.path(ResponseConstant.NAME).textValue());
		checkRoleName(role.getRoleName(),ostoken);
		role.setDisplayPermission(roleNode.path(ResponseConstant.VALUE).textValue());
		role.setOperationPermission(roleNode.path(ResponseConstant.PERMISSION).textValue());
		role.setDescription(roleNode.path(ResponseConstant.DESCRIPTION).textValue());
		cloudRoleMapper.insertSelective(role);
		return role;
	}
	
//	@Override
//	public CloudRole removeRole(String roleId,String userId,TokenOs ostoken) throws ResourceBusinessException{
//		CloudRole role = cloudRoleMapper.selectByPrimaryKey(roleId);
//		if(null == role)
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
//        CloudUser user = cloudUserMapper.selectByPrimaryKey(userId);
//        if(null == user)
//        	throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
//        
//        UserRole userRole = userRoleMapper.selectRoleIdByUserId(userId);
//        if(null == userRole){
//        	throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
//        }else{
//        	 String existingRoleId = userRole.getRoleId(); 
//         	 userRole.setRoleId((Util.listToString(Util.getCorrectedIdInfo(existingRoleId, roleId), ',')));
//         	 userRoleMapper.updateByPrimaryKeySelective(userRole);
//        }
//		return role;
//	}
	
//	@Override
//	public void addUsers(String id,String body,TokenOs ostoken) throws ResourceBusinessException{
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
//		List<String> usersId = new ArrayList<String>();
//		for (int index = 0; index < idsCount; ++index) {
//			usersId.add(idsNode.get(index).textValue());
//		}
//		
//		List<CloudUser> users = cloudUserMapper.selectUserByIds(usersId);
//		if(Util.isNullOrEmptyList(users))
//			return;
//		for (CloudUser user : users) {
//			UserRole userRole = userRoleMapper.selectRoleIdByUserId(user.getUserid());
//			if (null == userRole) {
//				userRole = new UserRole();
//				userRole.setId(Util.makeUUID());
//				userRole.setUserId(user.getUserid());
//				userRole.setRoleId(id);
//				userRoleMapper.insertSelective(userRole);
//			} else {
//				userRole.setRoleId(Util.getAppendedIds(userRole.getRoleId(), id));
//				userRoleMapper.updateByPrimaryKeySelective(userRole);
//			}
//		}
//	}
	
	@Override
	public void deleteRole(String roleId,TokenOs ostoken) throws ResourceBusinessException{
		CloudRole role = cloudRoleMapper.selectByPrimaryKey(roleId);
		if(null == role)
			return;
		if(role.getRoleName().equals(Util.ADMIN_ROLE_NAME) || role.getRoleName().equals(Util.USER_ROLE_NAME))
			throw new ResourceBusinessException(Message.CS_DELETE_SYSTEM_ROLE,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		if(!Util.isNullOrEmptyList(userRoleTenantMapper.selectByRoleId(roleId))){
			throw new ResourceBusinessException(Message.CS_ROLE_IS_USED,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));

		}
		cloudRoleMapper.deleteByPrimaryKey(roleId);
	}
	
	@Override
	public void checkUserPermission(TokenOs ostoken,String operation,String instanceId) throws ResourceBusinessException{
		if(null == instanceId)
			throw new ResourceBusinessException(Message.CS_USER_NO_PERMISSION,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		Instance instance = instanceMapper.selectByPrimaryKey(instanceId);
		if(null == instance)
	    	throw new ResourceBusinessException(Message.CS_USER_NO_PERMISSION,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		if(ParamConstant.VDI_TYPE.equals(instance.getType())){
			checkUserPermission(ostoken,ParamConstant.INSTANCE_UPDATE);
			operation = operation.replace("instance", "vdiInstance");
		}
		checkUserPermission(ostoken,operation);
	}
	
	@Override
	public void checkUserPermission(TokenOs ostoken,String operation) throws ResourceBusinessException{
	//	String tenantId = ostoken.getTenantid();
		String tenantName = ostoken.getTenantname();
		Tenant tenant = tenantMapper.selectByName(tenantName);
		if(null == tenant)
			throw new ResourceBusinessException(Message.CS_USER_NO_PERMISSION,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));

		List<DomainTenantUser> users = tenantUserMapper.selectListByTenantId(tenant.getId());
		if(Util.isNullOrEmptyList(users))
			throw new ResourceBusinessException(Message.CS_USER_NO_PERMISSION,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
        String userId = users.get(0).getClouduserid();
        
        List<UserRoleTenant> userRoleTenants = userRoleTenantMapper.selectByUserTenantId(userId, tenant.getId());
        if(Util.isNullOrEmptyList(userRoleTenants))
			throw new ResourceBusinessException(Message.CS_USER_NO_PERMISSION,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));

        List<String> roleIds = new ArrayList<String>();
        for(UserRoleTenant userRoleTenant : userRoleTenants){
        	roleIds.add(userRoleTenant.getRoleId());
        }
		List<CloudRole> roles = cloudRoleMapper.selectRoleByIds(roleIds);
	    if(Util.isNullOrEmptyList(roles))
	    	throw new ResourceBusinessException(Message.CS_USER_NO_PERMISSION,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		Boolean avaiable = false;
		Boolean isadmin = false;
	    for(CloudRole role : roles){
			if(null == role.getOperationPermission())
				continue;
			if(role.getRoleName().equals("admin")){
				isadmin = true;
				avaiable = true;
				break;
			}
	    	if(role.getOperationPermission().contains(operation)){
				avaiable = true;
				break;
			}
		}
	    //ddh check
	    Boolean userHasDdh = true;
	    UserBindInfo userBindInfo = userBindInfoMapper.getLastBindActionByUserId(userId);
	    if(userBindInfo == null || userBindInfo.getAction() == null || userBindInfo.getAction().equals("unbind")){
	    	userHasDdh = false;
	    }
	    if(!userHasDdh && avaiable == true && !operation.equals(ParamConstant.BOND_DDH) && !isadmin){
	    	avaiable =false;
	    }
	    if(false == avaiable)
	    	throw new ResourceBusinessException(Message.CS_USER_NO_PERMISSION,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
	}
//	private CloudUser getCloudUser(TokenOs ostoken) throws ResourceBusinessException{
//		String userId = ostoken.getTenantUserid();
//		DomainTenantUser tenantUser = tenantUserMapper.selectByPrimaryKey(userId);
//		if(null == tenantUser)
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
//
//		CloudUser cloudUser = cloudUserMapper.selectByPrimaryKey(tenantUser.getClouduserid());
//		if(null == cloudUser)
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
//
//		return cloudUser;
//	}
}
