package com.cloud.cloudapi.dao.common;

import com.cloud.cloudapi.pojo.openstackapi.forgui.PortalSkin;

public interface PortalSkinMapper extends SuperMapper<PortalSkin, String>{

	public PortalSkin selectByUserId(String userId);
}
