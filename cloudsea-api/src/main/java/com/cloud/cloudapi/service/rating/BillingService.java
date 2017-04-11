package com.cloud.cloudapi.service.rating;

import java.util.List;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.rating.Billing;
import com.cloud.cloudapi.pojo.rating.BillingReport;

public interface BillingService {
	public List<BillingReport> getBillStatistics(String tenantId,String billingMonthUntil,TokenOs ostoken) throws BusinessException;
	public BillingReport getBillDetails(String tenantId,String billingMonth,TokenOs ostoken) throws BusinessException;

    public List<Billing> getBillings(TokenOs ostoken);
    public Billing createBilling(TokenOs ostoken,String createBody) throws BusinessException;
    public BillingReport bindingBilling(TokenOs ostoken,String reportName,String billingId) throws BusinessException;
    public Billing setDefaultBilling(TokenOs ostoken,String billingId) throws BusinessException;

    public void deleteBilling(TokenOs ostoken,String billingId) throws BusinessException;
}
