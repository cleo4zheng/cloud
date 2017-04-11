package com.cloud.cloudapi.service.openstackapi.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.RoleMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.foros.Role;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.RoleService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RoleServiceImpl implements RoleService{
    @Resource 
	private AuthService authService;
	
    @Resource
	private CloudConfig cloudconfig;
	
    @Resource
	private RoleMapper roleMapper;
    
	@Resource
	private OSHttpClientUtil httpClient;
	
//    private int ERROR_HTTP_CODE = 400;
	private Logger log = LogManager.getLogger(RoleServiceImpl.class);
	private TokenOs currentToken=null;

	
	@Override
	public void setTokenOs(TokenOs token) {
		// TODO Auto-generated method stub
		this.currentToken=token;
		
	}
	@Override
	public void clearTokenOs() {
		// TODO Auto-generated method stub
		this.currentToken=null;
	}
	@Override
	public Role createRole(Role role) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	
	public void removeRoleUserFromProject(String role_id, String user_id, String project_id) throws BusinessException {
		//todo 1: 使用默认配置admin用户创建租户
		TokenOs ot = this.currentToken;
		if(null == ot)
			ot = authService.createDefaultAdminOsToken();	

		Locale locale = new Locale(ot.getLocale());
		String url=cloudconfig.getOs_authurl();
		log.debug("role_id:"+role_id); 
		log.debug("user_id:"+user_id); 
		log.debug("project_id:"+project_id); 
		StringBuffer sb= new StringBuffer();
		sb.append(url);
		sb.append("projects");
		sb.append("/");
		sb.append(project_id);
		sb.append("/");
		sb.append("users");
		sb.append("/");
		sb.append(user_id);
		sb.append("/");		
		sb.append("roles");
		sb.append("/");
		sb.append(role_id);
        url=sb.toString();

		Map<String, String>  rs = httpClient.httpDoDelete(url, ot.getTokenid());
		
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_DELETE_RESPONSE_CODE:{
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
			rs = httpClient.httpDoDelete(url, tokenid);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (httpCode != ParamConstant.NORMAL_DELETE_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			break;
		}
//		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
//		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
//		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
//		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
//			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
//		default:
//			throw new ResourceBusinessException(Message.CS_ROLE_UPDATE_FAILED,httpCode,locale);
		}
	}
	
	@Override
	public boolean grantRoleToUserOnProject(String role_id, String user_id, String project_id) throws BusinessException {
		// TODO Auto-generated method stub
		
		//todo 1: 使用默认配置admin用户创建租户
		TokenOs ot = this.currentToken;
		if(null == ot)
			ot = authService.createDefaultAdminOsToken();	

		Locale locale = new Locale(ot.getLocale());
		String url=cloudconfig.getOs_authurl();
		log.debug("role_id:"+role_id); 
		log.debug("user_id:"+user_id); 
		log.debug("project_id:"+project_id); 
		StringBuffer sb= new StringBuffer();
		sb.append(url);
		sb.append("projects");
		sb.append("/");
		sb.append(project_id);
		sb.append("/");
		sb.append("users");
		sb.append("/");
		sb.append(user_id);
		sb.append("/");		
		sb.append("roles");
		sb.append("/");
		sb.append(role_id);
        url=sb.toString();
		String url1=url+"projects/​"+project_id.trim()+"/users/"+user_id.trim()+"/roles/​"+role_id.trim();
		//url+="projects/​"+project_id.trim()+"/users/"+user_id.trim()+"/roles/​"+role_id.trim();
		log.debug("url:"+url); 
		log.debug("url1:"+url1); 
		Map<String, String>  rs = httpClient.httpDoPut(url,ot.getTokenid(),"");
		
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		switch (httpCode) {
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE:
		case ParamConstant.NORMAL_CREATE_RESPONSE_CODE_WITHOUT_RESPONSE: {
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
			rs = httpClient.httpDoPut(url,tokenid,"");
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE_WITHOUT_RESPONSE && httpCode != ParamConstant.NORMAL_CREATE_RESPONSE_CODE)
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
			throw new ResourceBusinessException(Message.CS_ROLE_UPDATE_FAILED,httpCode,locale);
		}
		
//		if(rs==null||Integer.parseInt(rs.get("httpcode")) >= ERROR_HTTP_CODE){
//			log.error("wo cha：get rolelist request failed");
//			return false;
//		}
//		
//		log.debug("httpcode:"+rs.get("httpcode")); 
//		log.debug("jsonbody:"+rs.get("jsonbody")); 
		
		return true;
	}

	@Override
	public List<Role> getRoleList() throws BusinessException {
		
		//todo 1: 使用默认配置admin用户创建租户
		TokenOs ot = this.currentToken;

		//but keystone shuild be in default region
		//String region =OpenStackBaseConstant.OS_DEFAULT_REGION;
		
		Locale locale = new Locale(ot.getLocale());
		
		String url=ot.getEndPoint(TokenOs.EP_TYPE_IDENTIFY).getPublicURL();	
		url=RequestUrlHelper.createFullUrl(url+"/roles",null);
		Map<String, String>  rs =httpClient.httpDoGet(url,ot.getTokenid());
		
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		List<Role> roles = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(rs.get("jsonbody"));
				JsonNode rolesNode =rootNode.path(ResponseConstant.ROLES);
				roles = mapper.readValue(rolesNode.toString(),new TypeReference<List<Role>>(){});
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			rs = httpClient.httpDoGet(url,tokenid);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if (httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(rs.get("jsonbody"));
				JsonNode rolesNode =rootNode.path(ResponseConstant.ROLES);
				roles = mapper.readValue(rolesNode.toString(),new TypeReference<List<Role>>(){});
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
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
			throw new ResourceBusinessException(Message.CS_ROLE_GET_FAILED,httpCode,locale);
		}
		roleMapper.insertOrUpdateBatch(roles);
		return roles;
	}

	
	@Override
	public Role getRoleByName(String name) throws BusinessException {
		
		Role role = roleMapper.selectByName(name);
		if(null != role)
			return role;
		// TODO Auto-generated method stub	
		List<Role> roles = this.getRoleList();
	    if(roles != null){
	    	for(Role osRole : roles){
	    		if(osRole.getName().equals(name)) return osRole;
	    	}
	    }
		
		return null;
	}

}
