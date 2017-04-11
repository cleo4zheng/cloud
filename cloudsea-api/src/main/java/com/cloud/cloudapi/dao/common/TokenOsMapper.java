package com.cloud.cloudapi.dao.common;
import java.util.Date;

import com.cloud.cloudapi.pojo.common.DomainTenantUser;
import com.cloud.cloudapi.pojo.common.TokenOs;

public interface TokenOsMapper extends SuperMapper<TokenOs,String>{
	//查看Token是否存在
	public int countNum(String ostokenid);
	
	public TokenOs selectByDomainTenantUserId(String tenantuserid);
	
	public TokenOs selectByDomainTenantUser(DomainTenantUser domainTenantUser);
	public TokenOs selectByGuiTokenId(String guitokenid);
	public int deleteBytime(Date nowtime);

}
