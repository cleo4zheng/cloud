package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.rating.TemplateTenantMapping;

public interface TemplateTenantMappingMapper extends SuperMapper<TemplateTenantMapping, String> {
	public Integer countNum();
    public Integer insertOrUpdate(TemplateTenantMapping template);
	public List<TemplateTenantMapping> selectAll();
	public List<TemplateTenantMapping> selectByTemplateId(String template_id);
	public TemplateTenantMapping selectByTenantId(String tenant_id);
	public TemplateTenantMapping selectByTemplateAndVersionId(String template_id,String version_id);
}
