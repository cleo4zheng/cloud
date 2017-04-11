package com.cloud.cloudapi.service.openstackapi;

import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Chassis;

public interface ChassisService {
	
	public List<Chassis> getChassises(Map<String,String> paramMap,TokenOs guiToken) throws BusinessException;
	public Chassis getChassis(String chassisId,TokenOs guiToken) throws BusinessException;	
	public Chassis createChassis(String createBody,TokenOs guiToken) throws BusinessException;
}
