package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.quota.QuotaTemplate;

public interface QuotaTemplateMapper extends SuperMapper<QuotaTemplate,String>{
	public Integer countNum();
    public Integer insertOrUpdate(QuotaTemplate template);
    public Integer insertOrUpdateBatch(List<QuotaTemplate> templates);
	public List<QuotaTemplate> selectAll();
	public QuotaTemplate selectByName(String name);
	public QuotaTemplate selectDefaultTemplate();
}
