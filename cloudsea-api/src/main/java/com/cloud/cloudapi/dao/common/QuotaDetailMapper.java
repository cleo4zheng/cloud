package com.cloud.cloudapi.dao.common;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.cloud.cloudapi.pojo.openstackapi.forgui.QuotaDetail;

public interface QuotaDetailMapper extends SuperMapper<QuotaDetail, String>{
	public List<QuotaDetail> selectAll();
	public List<QuotaDetail> getQuotaDetailsById(String[] quotaDetailIds);
	public List<QuotaDetail> findQuotaDetailsByTypes(@Param("tenantId") String tenantId,@Param("types") List<String> types);
	public Integer insertOrUpdateBatch(List<QuotaDetail> quotaDetails);
	public Integer insertOrUpdate(QuotaDetail quotaDetail);
	public int addQuotaDetailsBatch(List<QuotaDetail> quotaDetails);
	public QuotaDetail selectByResourceType(String tenantId,String type);
	public Integer deleteByIds(List<String> ids);
}
