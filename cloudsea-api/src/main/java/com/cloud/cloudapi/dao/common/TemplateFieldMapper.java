package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.rating.TemplateField;

public interface TemplateFieldMapper extends SuperMapper<TemplateField, String> {
	public Integer countNum();
    public Integer insertOrUpdate(TemplateField field);
    public Integer insertOrUpdateBatch(List<TemplateField> fields);
    public TemplateField selectByFieldCodeAndServiceId(String fieldCode,String serviceId);
	public List<TemplateField> selectAll();
	public List<TemplateField> selectFiledsByServiceId(String service_id);
	public List<TemplateField> selectRatingFiledsByServiceId(String service_id);
	public List<TemplateField> selectListByIds(List<String> ids);
}
