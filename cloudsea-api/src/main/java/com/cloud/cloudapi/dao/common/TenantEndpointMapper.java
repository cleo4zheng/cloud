package com.cloud.cloudapi.dao.common;
import java.util.List;

import com.cloud.cloudapi.pojo.common.TenantEndpoint;

public interface TenantEndpointMapper extends SuperMapper<TenantEndpoint,String>{
	
	public List<TenantEndpoint> selectListByTenantId(String ostenantid);
	
	public List<TenantEndpoint> selectAll();
	
	public int deleteByIds(List<String> ids);
	
	public int deleteByTenantId(String ostenantid);

	public int deleteByTenantAndRegionId(String ostenantid,String belongRegion);
	
	public int deleteOne(TenantEndpoint tenantEndpoint);

}
