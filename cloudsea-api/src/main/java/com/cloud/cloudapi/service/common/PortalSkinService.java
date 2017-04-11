package com.cloud.cloudapi.service.common;

import java.io.IOException;

import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.PortalSkin;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface PortalSkinService {
	public PortalSkin getPortakSkin(TokenOs ostoken) throws ResourceBusinessException;
	public PortalSkin createPortalSkin(String skinvalue,TokenOs ostoken) throws  ResourceBusinessException, JsonProcessingException, IOException; 
}
