package com.cloud.cloudapi.dao.common;
import java.util.List;

import com.cloud.cloudapi.pojo.common.DomainTenantUser;

public interface DomainTenantUserMapper extends SuperMapper<DomainTenantUser,String>{

	public int countNum();
	public List<DomainTenantUser> selectListByUserId(String clouduserid);
	public List<DomainTenantUser> selectListByDomainId(String osdomainid);
	public List<DomainTenantUser> selectListByTenantId(String ostenantid);
	public DomainTenantUser selectListByTenantAndUserId(String ostenantid,String osuserid);
	public Integer deleteByTenantAndUserId(String ostenantid,String osuserid);
}
