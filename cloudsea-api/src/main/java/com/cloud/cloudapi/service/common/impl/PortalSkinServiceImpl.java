package com.cloud.cloudapi.service.common.impl;

import java.io.IOException;
import java.util.Locale;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.CloudUserMapper;
import com.cloud.cloudapi.dao.common.DomainTenantUserMapper;
import com.cloud.cloudapi.dao.common.PortalSkinMapper;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.common.DomainTenantUser;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PortalSkin;
import com.cloud.cloudapi.service.common.PortalSkinService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("portalSkinService")
public class PortalSkinServiceImpl implements PortalSkinService {
	
	@Resource
	private CloudUserMapper cloudUserMapper;	
	
	@Resource
	private DomainTenantUserMapper domainTenantUserMapper;
	
	@Autowired
	private PortalSkinMapper portalSkinMapper;
	
	@Override
	public PortalSkin getPortakSkin(TokenOs ostoken) throws ResourceBusinessException{
		DomainTenantUser tenantUser = domainTenantUserMapper.selectByPrimaryKey(ostoken.getTenantUserid());
		if(null == tenantUser)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		CloudUser user = cloudUserMapper.selectByPrimaryKey(tenantUser.getClouduserid());
		if(null == user)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		
		PortalSkin portalSkin = portalSkinMapper.selectByUserId(user.getUserid());
		if(null == portalSkin)
			return new PortalSkin();
		return portalSkin;
	}
	
	@Override
	public PortalSkin createPortalSkin(String skinvalue,TokenOs ostoken) throws ResourceBusinessException, JsonProcessingException, IOException{
		DomainTenantUser tenantUser = domainTenantUserMapper.selectByPrimaryKey(ostoken.getTenantUserid());
		if(null == tenantUser)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		CloudUser user = cloudUserMapper.selectByPrimaryKey(tenantUser.getClouduserid());
		if(null == user)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		ObjectMapper mapper = new ObjectMapper();
		JsonNode skinNode = mapper.readTree(skinvalue);
		PortalSkin portalSkin = portalSkinMapper.selectByUserId(user.getUserid());
		if(null == portalSkin){
			portalSkin = new PortalSkin();
			portalSkin.setName(skinNode.path(ResponseConstant.NAME).textValue());
			portalSkin.setSkin(skinNode.path(ResponseConstant.VALUE).textValue());
			portalSkin.setUserId(user.getUserid());
			portalSkinMapper.insertSelective(portalSkin);
		}else{
			portalSkin.setName(skinNode.path(ResponseConstant.NAME).textValue());
			portalSkin.setSkin(skinNode.path(ResponseConstant.VALUE).textValue());
			portalSkinMapper.updateByPrimaryKeySelective(portalSkin);
		}
	
		return portalSkin;
	}
}
