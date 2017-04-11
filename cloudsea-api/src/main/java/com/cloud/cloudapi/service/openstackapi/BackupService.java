package com.cloud.cloudapi.service.openstackapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Backup;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Volume;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface BackupService {

	public List<Backup> getBackupList(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public Backup getBackup(String backupId,TokenOs ostoken) throws BusinessException;
	public Volume createBackup(String createBody,TokenOs ostoken) throws BusinessException, JsonParseException, JsonMappingException, IOException;
	public Volume restoreBackup(String restoreBody,String backupId,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public void deleteBackup(String backupId,TokenOs ostoken) throws BusinessException;
}
