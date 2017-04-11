package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.rating.RatingTemplate;

public interface RatingTemplateMapper extends SuperMapper<RatingTemplate,String>{
	public Integer countNum();
    public Integer insertOrUpdate(RatingTemplate template);
	public Integer insertOrUpdateBatch(List<RatingTemplate> templates);
	public List<RatingTemplate> selectAll();
	public RatingTemplate selectByName(String name);
	public RatingTemplate selectDefaultTemplate();
}
