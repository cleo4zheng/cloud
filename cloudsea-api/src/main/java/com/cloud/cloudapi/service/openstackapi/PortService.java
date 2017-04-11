package com.cloud.cloudapi.service.openstackapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Port;

public interface PortService {
	public List<Port> getPortList(Map<String,String> paramMap,TokenOs ostoken,Boolean bFromDB) throws BusinessException;
	public List<Port> refreshPorts(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public List<Port> getBindingPortsInfo(String routerId,TokenOs ostoken) throws BusinessException;
	public Port getPort(String portId,TokenOs ostoken,Boolean bFromDB) throws BusinessException;
	public Port addSecurityGroup(String securityGroupId,String portId,TokenOs ostoken) throws BusinessException;
	public Port removeSecurityGroup(String securityGroupId,String portId,TokenOs ostoken) throws BusinessException;
	public Port createPort(String createBody,TokenOs ostoken) throws BusinessException, IOException;
	public Port updatePort(String portId,String updateBody,TokenOs ostoken) throws BusinessException;
	public void deletePort(String portId,TokenOs ostoken) throws BusinessException;
}
