package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.rating.BillingReport;


public interface BillingReportMapper extends SuperMapper<BillingReport,String>{
	public Integer countNum();

	public Integer insertOrUpdate(BillingReport report);
	
	public BillingReport selectByTenantIdAndName(String name,String tenantId);
	
	public List<BillingReport> selectByTenantId(String tenantId);
	
	public List<BillingReport> selectAll();

	public List<BillingReport> selectListForPage(int start, int end);
}
