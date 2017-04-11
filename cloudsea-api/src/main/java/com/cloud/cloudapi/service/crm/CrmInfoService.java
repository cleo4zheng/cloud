package com.cloud.cloudapi.service.crm;

import java.util.List;

import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.crm.CrmInfo;

public interface CrmInfoService {

	public List<CrmInfo> getAllavaialeCrmInfo() throws ResourceBusinessException ;
}
