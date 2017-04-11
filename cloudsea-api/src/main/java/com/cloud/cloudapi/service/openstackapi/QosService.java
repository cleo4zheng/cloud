package com.cloud.cloudapi.service.openstackapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.QosBandwith;
import com.cloud.cloudapi.pojo.openstackapi.forgui.QosPolicy;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface QosService {
	public List<QosPolicy> getQosPolicies(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public QosPolicy getQosPolicy(String policyId,TokenOs ostoken) throws BusinessException;
	public QosPolicy createQosPolicy(String createBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	
	public List<QosBandwith> getQosBandwiths(Map<String,String> paramMap,String policyId,TokenOs ostoken) throws BusinessException;
	public QosBandwith getQosBandwith(String policyId,String bandwithId,TokenOs ostoken) throws BusinessException;
	public QosBandwith createBandwith(String createBody,String policyId,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
}
