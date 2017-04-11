package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.rating.Currency;

public interface CurrencyMapper  extends SuperMapper<Currency, String>{
	public Integer countNum();

	public Integer insertOrUpdate(Currency currency);

	public Integer insertOrUpdateBatch(List<Currency> currencies);
	
	public List<Currency> selectAll();

	public List<Currency> selectListWithLimit(int limit);
	
	public List<Currency> selectListForPage(int start, int end);
}
