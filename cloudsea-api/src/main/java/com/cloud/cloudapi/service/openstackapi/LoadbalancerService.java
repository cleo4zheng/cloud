package com.cloud.cloudapi.service.openstackapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.LBHealthMonitor;
import com.cloud.cloudapi.pojo.openstackapi.forgui.LBPool;
import com.cloud.cloudapi.pojo.openstackapi.forgui.LBPoolMember;
import com.cloud.cloudapi.pojo.openstackapi.forgui.LBVip;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Listener;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Loadbalancer;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface LoadbalancerService {
	public List<Loadbalancer> getLoadbalancers(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public Loadbalancer getLoadbalancer(String loadblancerId,TokenOs ostoken) throws BusinessException;
	public Loadbalancer createLoadbalancer(String createBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public Loadbalancer updateLoadbalancer(String loadblancerId,String updateBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public Loadbalancer bindingFloatingIP(String loadblancerId,String updateBody,TokenOs ostoken) throws BusinessException;
	public Loadbalancer removeFloatingIP(String loadblancerId,String updateBody,TokenOs ostoken) throws BusinessException;

	public void deleteLoadbalancer(String loadblancerId,TokenOs ostoken) throws BusinessException;
	
	public List<Listener> getListeners(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public Listener getListener(String listenerId,TokenOs ostoken) throws BusinessException;
	public Listener buildListener(String createBody,TokenOs ostoken) throws BusinessException;
	public void removeListener(String listenerId,TokenOs ostoken) throws BusinessException;
	
	public List<LBPool> getPools(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public LBPool getPool(String poolId,TokenOs ostoken) throws BusinessException;
	public LBPool buildPool(String createBody,TokenOs ostoken) throws BusinessException;
	public void removePool(String poolId,TokenOs ostoken) throws BusinessException;
	
	public List<LBPoolMember> getPoolMembers(String poolId,TokenOs ostoken) throws BusinessException;
	public LBPoolMember getPoolMember(String poolId,String memberId,TokenOs ostoken) throws BusinessException;
    public LBPoolMember addPoolMember(String poolId,String createBody,TokenOs ostoken) throws BusinessException;
    public void removePoolMember(String poolId,String memberId,TokenOs ostoken) throws BusinessException;
    public LBPoolMember addPoolMember(String poolId, String instanceId,String createBody, TokenOs ostoken) throws BusinessException;
    
    public List<LBHealthMonitor> getHealthMonitors(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
    public LBHealthMonitor getHealthMonitor(String healthMonitorId,TokenOs ostoken) throws BusinessException;
    public LBHealthMonitor buildHealthMonitor(String createBody,TokenOs ostoken) throws BusinessException;
    public LBHealthMonitor associateHealthMonitorWithPool(String healthMonitorAssociateBody,String poolId,TokenOs ostoken) throws BusinessException;
    public void disassociateHealthMonitorWithPool(String poolId,String headthMonitorId,TokenOs ostoken) throws BusinessException;
    
    public List<LBVip> getVips(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
    public LBVip getVip(String vipId,TokenOs ostoken) throws BusinessException;
    public LBVip createVip(String createBody,TokenOs ostoken) throws BusinessException;
}
