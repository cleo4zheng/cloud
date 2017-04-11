package com.cloud.cloudapi.service.openstackapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FloatingIP;
import com.cloud.cloudapi.pojo.openstackapi.forgui.FloatingIPConfig;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface FloatingIPService {
	
	public List<FloatingIP> getFloatingIPList(Map<String, String> paramMap, TokenOs ostoken) throws BusinessException;
	public FloatingIP getFloatingIP(String floatingIpId, TokenOs ostoken) throws BusinessException;
	public FloatingIP createFloatingIp(String createBody, TokenOs ostoken) throws BusinessException;
	public FloatingIP associateFloatingIp(String floatingIPId, String lbId,String portId,TokenOs ostoken) throws BusinessException;
	public FloatingIP disassociateFloatingIp(String floatingIPId,String lbId,TokenOs ostoken) throws BusinessException;
//	public List<FloatingIP> getFloatingIPExtList(Map<String, String> paraMap, TokenOs ostoken) throws BusinessException;
	public FloatingIPConfig getFloatingIPConfig(TokenOs ostoken) throws BusinessException;
	public FloatingIP updateFloatingIP(String floatingIpId, String updateBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public FloatingIP refreshFloatingIP(String floatingIpId, TokenOs ostoken) throws BusinessException;
	public void deleteFloatingIP(String floatingIpId, TokenOs ostoken) throws BusinessException;
	FloatingIPConfig getFloatingIPConfig2(TokenOs ostoken) throws BusinessException;
}
