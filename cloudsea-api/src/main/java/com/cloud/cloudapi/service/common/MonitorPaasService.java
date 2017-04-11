package com.cloud.cloudapi.service.common;

import java.util.HashMap;

import com.cloud.cloudapi.exception.BusinessException;

public interface MonitorPaasService {
	
	public HashMap<String,String> getAppDetail(String monitorObjId) throws BusinessException;
}
