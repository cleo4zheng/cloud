package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.rating.TemplateService;

public interface TemplateServiceMapper extends SuperMapper<TemplateService, String> {
	public Integer countNum();
    public Integer insertOrUpdate(TemplateService template);
	public List<TemplateService> selectAll();
	public TemplateService selectByServiceCode(String service_code);
}
