package com.cloud.cloudapi.service.openstackapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Console;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Image;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.InstanceConfig;
import com.cloud.cloudapi.pojo.openstackapi.forgui.InterfaceAttachment;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Port;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Volume;
import com.cloud.cloudapi.pojo.openstackapi.forgui.VolumeAttachment;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface InstanceService {

	public List<Instance> getInstanceList(Map<String,String> paramMap,String type,TokenOs ostoken) throws BusinessException;
	public List<Instance> getVDIInstanceList(Map<String, String> paramMap, String type,TokenOs ostoken)throws BusinessException;
	public Instance getInstance(String instanceId,String type,TokenOs ostoken, Boolean details) throws BusinessException;
//	public Instance getVDIInstance(String instanceId,TokenOs ostoken,HttpServletResponse response) throws BusinessException;
	public Instance updateInstance(String instanceId,String updateBody,TokenOs ostoken) throws BusinessException, JsonParseException, JsonMappingException, IOException;
	public VolumeAttachment attachVolume(String instanceId,String attachmentBody,TokenOs ostoken) throws BusinessException, JsonParseException, JsonMappingException, IOException;
	public InterfaceAttachment attachPort(String instanceId,String type,String attachmentBody,TokenOs ostoken) throws BusinessException, JsonParseException, JsonMappingException, IOException;
	public void detachVolume(String instanceId,String volumeId,TokenOs ostoken) throws BusinessException;
	public Port detachPort(String instanceId,String portId,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public List<Instance> createInstance(String createBody,String type,TokenOs ostoken) throws BusinessException, JsonParseException, JsonMappingException, IOException ;
//	public void deleteInstance(String id,TokenOs ostoken,HttpServletResponse response) throws BusinessException;
	public String deleteInstances(String deleteBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public List<Volume>  getAttachedVolumes(String instanceId,TokenOs ostoken) throws BusinessException;
	public Image createInstanceImage(String instanceId, String type,TokenOs ostoken,String body) throws BusinessException, JsonProcessingException, IOException;
	public Instance liveMigrationInstance(String instanceId, String action,TokenOs ostoken,String body) throws BusinessException;

	public Console getInstanceConsole(String instanceId,String action,TokenOs ostoken,String body) throws BusinessException, JsonProcessingException, IOException;
	public void operateInstance(String instanceId,String body,String action,String type,TokenOs ostoken) throws BusinessException;
	public InstanceConfig getInstanceConfig(String type,TokenOs authToken) throws BusinessException;
	public void createSnapshot(String instanceId,TokenOs ostoken) throws BusinessException;

}
