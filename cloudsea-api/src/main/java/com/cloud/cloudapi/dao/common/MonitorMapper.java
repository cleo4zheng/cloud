package com.cloud.cloudapi.dao.common;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.cloud.cloudapi.pojo.openstackapi.forgui.Monitor;

public interface MonitorMapper extends SuperMapper<Monitor, String> {
	public List<Monitor> selectAllList();
	public List<Monitor> selectListByTenantId(@Param("tenant_id") String tenant_id);
	public List<Monitor> selectListForPage(@Param("paramMap") Map<String,String> paramMap, @Param("tenant_id") String tenant_id);
	public void updateNameAndDescrition(Monitor monitor);
}
