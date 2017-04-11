package com.cloud.cloudapi.dao.common;

import java.util.List;

import com.cloud.cloudapi.pojo.monitor.MonitorTemplate;

public interface MonitorTemplateMapper extends SuperMapper<MonitorTemplate,String>{
	public Integer countNum();
    public Integer insertOrUpdate(MonitorTemplate template);
    public Integer insertOrUpdateBatch(List<MonitorTemplate> templates);
	public List<MonitorTemplate> selectAll();
	public MonitorTemplate selectByName(String name);
	public MonitorTemplate selectDefaultTemplate();
}
