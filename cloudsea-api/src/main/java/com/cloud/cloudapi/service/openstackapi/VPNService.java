package com.cloud.cloudapi.service.openstackapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.IPSecPolicy;
import com.cloud.cloudapi.pojo.openstackapi.forgui.IPSecSiteConnection;
import com.cloud.cloudapi.pojo.openstackapi.forgui.IkePolicy;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VPN;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface VPNService {
	public List<VPN> getVPNs(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public VPN getVPN(String vpnId,TokenOs ostoken) throws BusinessException;
	public VPN createVPN(String createBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public VPN updateVPN(String vpnId,String updateBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public void removeVPN(String vpnId,TokenOs ostoken) throws BusinessException;
	public List<IkePolicy> getIkePolicies(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public IkePolicy getIkePolicy(String policyId,TokenOs ostoken) throws BusinessException;
	public IkePolicy createIkePolicy(String createBody,String vpnName,TokenOs ostoken) throws BusinessException;
	public void removeIkePolicy(String ikePolicyId,TokenOs ostoken) throws BusinessException;
	public List<IPSecPolicy> getIPSecPolicies(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public IPSecPolicy getIPSecPolicy(String policyId,TokenOs ostoken) throws BusinessException;
	public IPSecPolicy createIPSecPolicy(String createBody,String vpnName,TokenOs ostoken) throws BusinessException;
	public void removeIPSecPolicy(String ipsecPolicyId,TokenOs ostoken) throws BusinessException;
	public List<IPSecSiteConnection> getIPSecConnections(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public IPSecSiteConnection getIPSecConnection(String ipsecConnectionId,TokenOs ostoken) throws BusinessException;
	public IPSecSiteConnection createIPSecConnection(String createBody,Boolean convert,TokenOs ostoken) throws BusinessException, JsonParseException, JsonMappingException, IOException;
	public void removeIPSecSiteConnection(String ipsecSiteConId,TokenOs ostoken) throws BusinessException;
}
