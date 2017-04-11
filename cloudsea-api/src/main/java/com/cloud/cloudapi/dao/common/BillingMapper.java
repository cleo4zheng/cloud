package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.rating.Billing;

public interface BillingMapper extends SuperMapper<Billing, String> {
	public Integer countNum();

	public Integer insertOrUpdate(Billing backup);
	
	public List<Billing> selectAll();
	
	public List<Billing> selectAllByUserId(String userId);
	
	public List<Billing> selectListWithLimit(int limit);
	
	public List<Billing> selectListForPage(int start, int end);
	
	public Billing selectDefaultUserAccount(String userId);
}
