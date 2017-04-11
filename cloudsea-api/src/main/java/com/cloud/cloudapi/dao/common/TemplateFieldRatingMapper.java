package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.rating.TemplateFieldRating;

public interface TemplateFieldRatingMapper extends SuperMapper<TemplateFieldRating, String> {
	public Integer countNum();
    public Integer insertOrUpdate(TemplateFieldRating template);
    public Integer insertOrUpdateBatch(List<TemplateFieldRating> fields);
	public List<TemplateFieldRating> selectAll();
	public List<TemplateFieldRating> selectByTemplateAndVersionId(String template_id,String version_id);
	public List<TemplateFieldRating> selectByFieldAndServiceId(String field_id,String service_id);
	public Integer deleteByTemplateId(String template_id);
	public Integer deleteByTemplateAndVersionId(String template_id,String version_id);

}
