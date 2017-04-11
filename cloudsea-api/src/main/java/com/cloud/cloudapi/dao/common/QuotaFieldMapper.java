package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.quota.QuotaField;

public interface QuotaFieldMapper extends SuperMapper<QuotaField,String>{
	public Integer countNum();
    public Integer insertOrUpdate(QuotaField field);
    public Integer insertOrUpdateBatch(List<QuotaField> fields);
	public List<QuotaField> selectAll();
	public List<QuotaField> selectByServiceIdAndTemplateId(String service_id,String template_id);
	public List<QuotaField> selectByTemplateId(String template_id);
	public QuotaField selectByName(String name);
	public Integer deleteByTemplateId(String template_id);
}
