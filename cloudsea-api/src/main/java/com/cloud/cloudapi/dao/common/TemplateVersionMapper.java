package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.rating.TemplateVersion;

public interface TemplateVersionMapper extends SuperMapper<TemplateVersion, String> {
	public Integer countNum();
    public Integer insertOrUpdate(TemplateVersion TemplateVersion);
	public List<TemplateVersion> selectAll();
	public List<TemplateVersion> selectByTemplateId(String template_id);
	public Integer deleteByTemplateId(String template_id);
}
