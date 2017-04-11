package com.cloud.cloudapi.service.vmware.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.CloudServiceMapper;
import com.cloud.cloudapi.dao.common.HostAggregateMapper;
import com.cloud.cloudapi.dao.common.HostDetailMapper;
import com.cloud.cloudapi.dao.common.HostMapper;
import com.cloud.cloudapi.dao.common.InstanceMapper;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.openstackapi.forgui.CloudService;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Host;
import com.cloud.cloudapi.pojo.openstackapi.forgui.HostAggregate;
import com.cloud.cloudapi.pojo.openstackapi.forgui.HostDetail;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.service.vmware.VMWareService;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("vmwareService")
public class VMWareServiceImpl implements VMWareService {
	
	@Resource
	private OSHttpClientUtil httpClient;
	
	@Autowired
	private CloudConfig cloudconfig;
	
	@Resource
	private CloudServiceMapper serviceMapper;
	
	@Resource
	private HostAggregateMapper hostAggregateMapper;
	
	@Resource
	private HostMapper hostMapper;
	
	@Resource
	private HostDetailMapper hostDetailMapper;
	
	@Resource
	private InstanceMapper instanceMapper;
	
	public Logger log = LogManager.getLogger(VMWareServiceImpl.class);

	@Override
	public void makeVCenterResources(TokenOs ostoken) throws ResourceBusinessException{
		
		//call the cloudvmware api to get vcenter info
		String url = cloudconfig.getCloudVMWareUrl()+"/cloudsea-vmware/vcenter-all";
		
		Map<String, String> rs = httpClient.httpDoGet(url);
		
		Locale locale = new Locale(ostoken.getLocale());
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(rs.get(ResponseConstant.JSONBODY));
		} catch (Exception e) {
			log.error("error",e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,locale);
		} 
		
		int vcsCount = rootNode.size();
		for (int index = 0; index < vcsCount; ++index) {
			this.makeVCenterResource(rootNode.get(index),ostoken.getTenantid());
		}
	}
	
	private void makeVCenterResource(JsonNode vcNode,String tenantId){
		String source = vcNode.path(ResponseConstant.VCENTER).textValue();
	//	Map<String,String> aggregateMap = null;
		if(!vcNode.path(ResponseConstant.AGGREGATES).isMissingNode()){
			makeVCenterAggregateInfo(vcNode.path(ResponseConstant.AGGREGATES),source);
		}
		makeVCHostsInfo(vcNode.path(ResponseConstant.HOSTS),source,tenantId);
	}
	
	private Map<String,String> makeVCenterAggregateInfo(JsonNode aggregateNodes,String source){
		Map<String,String> aggregateMap = new HashMap<String,String>();
		CloudService service = serviceMapper.selectByType(ParamConstant.VMWARE_TYPE);
	    if(null == service)
	    	return aggregateMap;
		List<HostAggregate> aggregates = new ArrayList<HostAggregate>();
		int count = aggregateNodes.size();
		for (int index = 0; index < count; ++index) {
			JsonNode node = aggregateNodes.get(index);
			HostAggregate aggregate = new HostAggregate();
			aggregate.setSource(source);
			aggregate.setId(Util.makeUUID());
			aggregate.setName(node.path(ResponseConstant.NAME).textValue());
			aggregate.setAvailabilityZone(node.path(ResponseConstant.AVAILABILITY_ZONE).textValue());
			aggregate.setMillionSeconds(Util.getCurrentMillionsecond());
			aggregate.setServiceId(service.getId());
			aggregate.setHostIds(makeVCHostIdsInfo(node.path(ResponseConstant.HOSTS)));
			aggregates.add(aggregate);
			aggregateMap.put(aggregate.getId(), aggregate.getName());
		}		
		hostAggregateMapper.insertOrUpdateBatch(aggregates);
		return aggregateMap;
	}
	
	private String makeVCHostIdsInfo(JsonNode hostsNode){
		if(hostsNode.isMissingNode())
			return null;
		
		List<String> ids = new ArrayList<String>();
		int count = hostsNode.size();
		for (int index = 0; index < count; ++index) {
			ids.add(hostsNode.get(index).path(ResponseConstant.UUID).textValue());
		}
		return Util.listToString(ids, ',');
	}
	
	private void makeVCHostsInfo(JsonNode hostsNode,String source,String tenantId){
		if(hostsNode.isMissingNode())
			return;
		List<Host> hosts = new ArrayList<Host>();
		int count = hostsNode.size();
		for (int index = 0; index < count; ++index) {
			JsonNode node = hostsNode.get(index);
			Host host = new Host();
			host.setId(node.path(ResponseConstant.UUID).textValue());
			host.setSource(source);
			host.setHostName(node.path(ResponseConstant.NAME).textValue());
			host.setZoneName(node.path(ResponseConstant.AVAILABILITY_ZONE).textValue());
			host.setServiceName(ParamConstant.COMPUTE);
            String hostDetailsId = makeVCHostDetailInfo(node,host.getZoneName());
            host.setHostDetailsId(hostDetailsId);
            hosts.add(host);
            makeVCHostVMsInfo(node.path(ResponseConstant.VMS),source,host.getZoneName(),host.getHostName(),tenantId);
		}
		hostMapper.insertOrUpdateBatch(hosts);
	}
	
	private String makeVCHostDetailInfo(JsonNode hostNode,String zoneName){
		List<HostDetail> details = new ArrayList<HostDetail>();
		
		HostDetail cpu = new HostDetail();
		cpu.setId(Util.makeUUID());
		cpu.setName(ParamConstant.CORE);
		cpu.setType(zoneName+"_core");
		cpu.setTotal(hostNode.path(ResponseConstant.CPU_TOTAL).intValue());
		cpu.setUsed(hostNode.path(ResponseConstant.CPU_USAGE).intValue());
		cpu.setUnit(Message.CS_COUNT_UNIT);
		details.add(cpu);
		
		HostDetail mem = new HostDetail();
		mem.setId(Util.makeUUID());
		mem.setName(ParamConstant.RAM);
		mem.setType(zoneName+"_ram");
		mem.setTotal(hostNode.path(ResponseConstant.MEM_TOTAL).intValue());
		mem.setUsed(hostNode.path(ResponseConstant.MEM_USAGE).intValue());
		mem.setUnit(Message.CS_RAM_UNIT);
		details.add(mem);
		
		HostDetail disk = new HostDetail();
		disk.setId(Util.makeUUID());
		disk.setName(ParamConstant.STORAGE);
		disk.setTotal(hostNode.path(ResponseConstant.DISK_TOTAL).intValue());
		disk.setUsed(hostNode.path(ResponseConstant.DISK_USAGE).intValue());
		disk.setUnit(Message.CS_CAPACITY_UNIT);
		details.add(disk);
		
		hostDetailMapper.insertOrUpdateBatch(details);
		return cpu.getId()+","+mem.getId()+","+disk.getId();
	}
	
	private void makeVCHostVMsInfo(JsonNode vmsNode,String source,String zoneName,String hostName,String tenantId){
		if(vmsNode.isMissingNode())
			return;
		List<Instance> instances = new ArrayList<Instance>();
		int count = vmsNode.size();
		for (int index = 0; index < count; ++index) {
			JsonNode node = vmsNode.get(index);
			Instance instance = new Instance();
			instance.setType(ParamConstant.INSTANCE_TYPE);
			instance.setId(node.path(ResponseConstant.UUID).textValue());
			instance.setName(node.path(ResponseConstant.NAME).textValue());
			instance.setTenantId(tenantId);
			instance.setStatus(node.path(ResponseConstant.STATUS).textValue());
			instance.setHostName(hostName);
			instance.setAvailabilityZone(zoneName);
			instance.setSource(source);
			if(!node.path(ResponseConstant.CPU_TOTAL).isMissingNode())
				instance.setCore(Integer.toString(node.path(ResponseConstant.CPU_TOTAL).intValue()));
			if(!node.path(ResponseConstant.MEM_TOTAL).isMissingNode())
				instance.setRam(Integer.toString(node.path(ResponseConstant.MEM_TOTAL).intValue()*ParamConstant.MB));
			instance.setFixedips(node.path(ResponseConstant.IP_ADDRESS).textValue());
			instance.setMillionSeconds(Util.getCurrentMillionsecond());
			instances.add(instance);
		}
		instanceMapper.insertOrUpdateBatch(instances);
	}
}
