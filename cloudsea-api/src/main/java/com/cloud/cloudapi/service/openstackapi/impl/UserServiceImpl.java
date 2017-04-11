package com.cloud.cloudapi.service.openstackapi.impl;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.DomainTenantUserMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.common.DomainTenantUser;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.foros.Role;
import com.cloud.cloudapi.pojo.openstackapi.foros.User;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.RoleService;
import com.cloud.cloudapi.service.openstackapi.UserService;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
@Service
public class UserServiceImpl implements UserService{
	
	@Resource
	private OSHttpClientUtil httpClient;

	@Resource
	private CloudConfig cloudconfig;	
	
	@Resource
	private AuthService authService;
	  
    @Resource
    private RoleService   roleService;	
    
	@Resource
	private DomainTenantUserMapper domainTenantUserMapper;
	
	private Logger log = LogManager.getLogger(UserServiceImpl.class);
	
	@Override
	public User createUser(User user,TokenOs admintoken) throws BusinessException {
		//todo 1: 使用默认配置admin用户创建租户
		if(user==null) 
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(admintoken.getLocale()));

		TokenOs ot = admintoken;
		user.setDomain_id(cloudconfig.getOs_authdomainid());	
		String url=cloudconfig.getOs_authurl();
		url=RequestUrlHelper.createFullUrl(url+"/users",null);
		
		//生成post body体
		String postbody=new JsonHelper<User,String>().generateJsonBodySimple(user, "user");
		log.debug("postbody:"+postbody); 
		
		Map<String, String>  rs = httpClient.httpDoPost(url,ot.getTokenid(), postbody);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
	    Locale locale = new Locale(ot.getLocale());
	    
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE:{
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				JsonNode userNode = rootNode.path(ResponseConstant.USER);	
				user = mapper.readValue(userNode.toString(), User.class);
			}catch(Exception e){
				// TODO Auto-generated catch block
				log.error(e);
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
			rs =  httpClient.httpDoPost(url,tokenid,postbody);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,locale);
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
				JsonNode userNode = rootNode.path(ResponseConstant.USER);	
				user = mapper.readValue(userNode.toString(), User.class);
			}catch(Exception e){
				// TODO Auto-generated catch block
				log.error(e);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_USER_CREATE_FAILED,httpCode,locale);
		}
	
		 return user;
	}

	@Override
	public void removeUserFromProject(CloudUser user,String projectId,TokenOs ostoken) throws BusinessException {

		Locale locale = new Locale(ostoken.getLocale());
		Role role = roleService.getRoleByName(ParamConstant.OS_CLOUDUSER_ROLE);
		if(null == role)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);

		roleService.removeRoleUserFromProject(role.getId(), user.getOsUserId(), projectId);
		
		domainTenantUserMapper.deleteByTenantAndUserId(projectId,user.getUserid());
	}
	
	@Override
	public void addUserToProject(CloudUser user,String projectId,TokenOs ostoken) throws BusinessException {

		Locale locale = new Locale(ostoken.getLocale());
		Role role = roleService.getRoleByName(ParamConstant.OS_CLOUDUSER_ROLE);
		if(null == role)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);

		roleService.grantRoleToUserOnProject(role.getId(), user.getOsUserId(), projectId);
		
		DomainTenantUser domainTenantUser = domainTenantUserMapper.selectListByTenantAndUserId(projectId,user.getUserid());
		if(null == domainTenantUser){
			domainTenantUser = new DomainTenantUser();
			domainTenantUser.setId(Util.makeUUID());
			domainTenantUser.setOsdomainid(cloudconfig.getOs_authdomainid());
			domainTenantUser.setOstenantid(projectId);
			domainTenantUser.setClouduserid(user.getUserid());
			domainTenantUserMapper.insertSelective(domainTenantUser);
		}
		
		/*
		String url=cloudconfig.getOs_authurl();
		url=RequestUrlHelper.createFullUrl(url+"/users/",null);
		url+=userId;
		
		StringBuilder sb = new StringBuilder();
		sb.append("{\"user\":{");
		sb.append("\"default_project_id\":\"");
		sb.append(projectId);
		sb.append("\"}}");

		Map<String, String> rs = httpClient.httpDoPatch(url, ostoken.getTokenid(), sb.toString());
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE:
			break;
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			rs =  httpClient.httpDoPatch(url, tokenid, sb.toString());
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedError = Util.getFailedReason(rs);
			if(!Util.isNullOrEmptyValue(failedError))
				log.error(failedError);
			if(httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE )
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
			throw new ResourceBusinessException(Message.CS_TENANT_CREATE_FAILED,httpCode,locale);
		}
		*/
	}
	
	@Override
	public void deleteUser(String userId,TokenOs ostoken) throws BusinessException {
		String url = cloudconfig.getOs_authurl();
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		sb.append("/users/");
		sb.append(userId);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
        Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = httpClient.httpDoDelete(sb.toString(), ostoken.getTokenid());
		Util.checkResponseBody(rs,locale);
		String failedError = Util.getFailedReason(rs);
		if(!Util.isNullOrEmptyValue(failedError))
			log.error(failedError);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
//		if (null != response)
//			response.setStatus(httpCode);
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
//			if (null != response)
//				response.setStatus(httpCode);
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
			throw new ResourceBusinessException(Message.CS_COMPUTE_USER_DELETE_FAILED,httpCode,locale);
		}
	}

}
