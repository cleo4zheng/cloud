package com.cloud.cloudapi.service.openstackapi.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Hypervisor;
import com.cloud.cloudapi.service.common.AuthService;
import com.cloud.cloudapi.service.openstackapi.HypervisorService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.RequestUrlHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class HypervisorServiceImpl implements HypervisorService {

	@Resource
	private OSHttpClientUtil httpClient;

	@Resource
	private AuthService authService;
	
	private Logger log = LogManager.getLogger(HypervisorServiceImpl.class);
	
	@Override
	public List<Hypervisor> listHypervisorDetail(TokenOs ostoken)
			throws BusinessException {
		
		String region = ostoken.getCurrentRegion();

		String url = ostoken.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
		url = RequestUrlHelper.createFullUrl(url + "/os-hypervisors/detail", null);

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, ostoken.getTokenid());
        Locale locale = new Locale(ostoken.getLocale());
		Map<String, String> rs = httpClient.httpDoGet(url, headers);
		Util.checkResponseBody(rs,locale);
		String failedMessage = Util.getFailedReason(rs);
		if (!Util.isNullOrEmptyValue(failedMessage))
			log.error(failedMessage);
		int httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
		List<Hypervisor> hvs = null;
		switch (httpCode) {
		case ParamConstant.NORMAL_SYNC_RESPONSE_CODE: {
			try {
				hvs = this.getHypervisors(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.UN_AUTHHORIZED_RESPONSE_CODE: {
			String tokenid = "";// TODO reget the token id
			try {
				TokenOs newToken = authService.createNewToken(ostoken.getTenantUserid(),ostoken.getCurrentRegion(),ostoken.getLocale());
				tokenid = newToken.getTokenid();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			headers.put(ParamConstant.OPENSTACK_AUTH_TOKEN, tokenid);
			rs = httpClient.httpDoGet(url, headers);
			httpCode = Integer.parseInt(rs.get(ResponseConstant.HTTPCODE));
			failedMessage = Util.getFailedReason(rs);
			if (!Util.isNullOrEmptyValue(failedMessage))
				log.error(failedMessage);
			if(httpCode != ParamConstant.NORMAL_SYNC_RESPONSE_CODE)
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
			try {
				hvs = this.getHypervisors(rs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
				throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.SERVICE_ERROR_RESPONSE_CODE,locale);
			}
			break;
		}
		case ParamConstant.BAD_REQUEST_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,httpCode,locale);
		case ParamConstant.NOT_FOUND_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_NOT_EXISTING,httpCode,locale);
		case ParamConstant.SERVICE_FORBIDDEN_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_IS_FORBIDDEN,httpCode,locale);
		case ParamConstant.SERVICE_UNAVAILABLE_RESPONSE_CODE:
			throw new ResourceBusinessException(Message.CS_SERVICE_UN_AVAILABLE,httpCode,locale);
		default:
			throw new ResourceBusinessException(Message.CS_HOST_GET_FAILED,httpCode,locale);
		}

		return hvs;
	}

	private List<Hypervisor> getHypervisors(Map<String, String> rs) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		JsonNode hypervisorsNode = rootNode.path("hypervisors");
		int hvCount = hypervisorsNode.size();
		if (0 == hvCount) {
			return null;
		}

		List<Hypervisor> hypervisors = new ArrayList<Hypervisor>();
		for (int index = 0; index < hvCount; ++index) {
			Hypervisor hypervisor = this.getHypervisor(hypervisorsNode.get(index));
			if (null == hypervisor)
				continue;
			hypervisors.add(hypervisor);
		}
		return hypervisors;
	}

	private Hypervisor getHypervisor(JsonNode hvNode) {
		if (null == hvNode) {
			return null;
		}
		Hypervisor hypervisor = new Hypervisor();
		hypervisor.setCpu_info(hvNode.path("cpu_info").textValue());
		hypervisor.setCurrent_workload(hvNode.path("current_workload").intValue());
		hypervisor.setDisk_available_least(hvNode.path("disk_available_least").intValue());
		hypervisor.setFree_disk_gb(hvNode.path("free_disk_gb").intValue());
		hypervisor.setFree_ram_mb(hvNode.path("free_ram_mb").intValue());
		hypervisor.setHost_ip(hvNode.path("host_ip").textValue());
		hypervisor.setHypervisor_hostname(hvNode.path("hypervisor_hostname").textValue());
		hypervisor.setHypervisor_type(hvNode.path("hypervisor_type").textValue());
		hypervisor.setHypervisor_version(hvNode.path("hypervisor_version").intValue());
		hypervisor.setId(hvNode.path("id").intValue());
		hypervisor.setLocal_gb(hvNode.path("local_gb").intValue());
		hypervisor.setLocal_gb_used(hvNode.path("local_gb_used").intValue());
		hypervisor.setMemory_mb(hvNode.path("memory_mb").intValue());
		hypervisor.setMemory_mb_used(hvNode.path("memory_mb_used").intValue());
		hypervisor.setRunning_vms(hvNode.path("running_vms").intValue());
		hypervisor.setState(hvNode.path("state").textValue());
		hypervisor.setStatus(hvNode.path("status").textValue());
		hypervisor.setVcpus(hvNode.path("vcpus").intValue());
		hypervisor.setVcpus_used(hvNode.path("vcpus_used").intValue());
		return hypervisor;
	}
}
