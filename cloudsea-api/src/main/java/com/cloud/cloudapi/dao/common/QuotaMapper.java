package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Quota;

public interface QuotaMapper extends SuperMapper<Quota, String> {
	public List<Quota> selectAll();
	public List<Quota> selectAllByTenantId(String tenantId);
	public Integer insertOrUpdateBatch(List<Quota> quotas);
	public Integer insertOrUpdate(Quota quota);
	public Quota selectQuota(String tenantId,String quotaType);
	public Integer deleteByIds(List<String> ids);
}
