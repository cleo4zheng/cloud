package com.cloud.cloudapi.service.common.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

import com.cloud.cloudapi.dao.common.InstanceMapper;
import com.cloud.cloudapi.dao.common.LoadbalancerMapper;
import com.cloud.cloudapi.dao.common.MonitorHistoryMapper;
import com.cloud.cloudapi.dao.common.MonitorMapper;
import com.cloud.cloudapi.dao.common.MonitorObjMapper;
import com.cloud.cloudapi.dao.common.MonitorRuleMapper;
import com.cloud.cloudapi.dao.common.MonitorTemplateMapper;
import com.cloud.cloudapi.dao.common.NotificationListMapper;
import com.cloud.cloudapi.dao.common.NotificationMapper;
import com.cloud.cloudapi.dao.common.NotificationObjMapper;
import com.cloud.cloudapi.dao.common.ResourceEventMapper;
import com.cloud.cloudapi.dao.common.TenantMapper;
import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.exception.ResourceBusinessException;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.pojo.monitor.MonitorTemplate;
import com.cloud.cloudapi.pojo.openstackapi.forgui.CloudService;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Instance;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Loadbalancer;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Monitor;
import com.cloud.cloudapi.pojo.openstackapi.forgui.MonitorHistory;
import com.cloud.cloudapi.pojo.openstackapi.forgui.MonitorObj;
import com.cloud.cloudapi.pojo.openstackapi.forgui.MonitorRule;
import com.cloud.cloudapi.pojo.openstackapi.forgui.NotificationList;
import com.cloud.cloudapi.pojo.openstackapi.forgui.NotificationObj;
import com.cloud.cloudapi.pojo.openstackapi.forgui.ResourceEvent;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Tenant;
import com.cloud.cloudapi.service.businessapi.zabbix.ZabbixService;
import com.cloud.cloudapi.service.common.MonitorPaasService;
import com.cloud.cloudapi.service.common.MonitorService;
import com.cloud.cloudapi.service.openstackapi.CloudServiceService;
import com.cloud.cloudapi.service.openstackapi.InstanceService;
import com.cloud.cloudapi.service.openstackapi.PhysNodeService;
import com.cloud.cloudapi.util.DateHelper;
import com.cloud.cloudapi.util.DictConstant;
import com.cloud.cloudapi.util.JsonHelper;
import com.cloud.cloudapi.util.Message;
import com.cloud.cloudapi.util.ParamConstant;
import com.cloud.cloudapi.util.ResponseConstant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MonitorServiceImpl implements MonitorService{
	
	@Resource
	private MonitorMapper monitorMapper;
	
	@Resource
	private MonitorObjMapper monitorObjMapper;
	
	@Resource
	private NotificationMapper notificationMapper;
	
	@Resource
	private NotificationListMapper notificationListMapper;
	
	@Resource
	private MonitorRuleMapper monitorRuleMapper;
	
	@Resource
	private InstanceMapper instanceMapper;

	@Resource
	private TenantMapper tenantMapper;
	
	@Resource
	private NotificationObjMapper notificationObjMapper;
	
	@Resource
	private MonitorHistoryMapper monitorHistoryMapper;
	
	@Resource
	private LoadbalancerMapper loadBalancerMapper;
	
	@Resource
	private CloudConfig cloudconfig; 
	
	@Resource
	private PhysNodeService physNodeService;
	
	@Resource
	private CloudServiceService serviceService;
	
	@Resource
	private ZabbixService zabbixService;
	
	@Autowired
	private ResourceEventMapper resourceEventMapper;
	
	@Resource
	private MonitorTemplateMapper monitorTemplateMapper;
	
	@Resource
	private InstanceService instanceService;
		
	@Autowired(required=false)
	private MonitorPaasService monitorPaasServiceImpl;
	
	private Logger log = LogManager.getLogger(MonitorServiceImpl.class);
	
	private static final String CONFIG_FILE_NAME = "monitor_new.config";
	
	private int MonitorDataNum4GUI = 60;
	
	/*
	 * (select the monitors for UI)
	 * param : paramMap 
	 *         limit
	 *         name
	 *         status
	 *         type
	 *         resource
	 * 
	 * @see com.cloud.cloudapi.service.openstackapi.MonitorService#getMonitorList(java.util.Map, java.lang.String, javax.servlet.http.HttpServletResponse)
	 */
	
	@Override
	public List<Monitor> getMonitorListPage(Map<String, String> paramMap, TokenOs ostoken)
			throws BusinessException {
        
        //@TODO
        //get the tenant_id by guiToken
		// authority : admin or other
		String tenant_id = ostoken.getTenantid();
		
		List<Monitor> monitors = monitorMapper.selectListForPage(paramMap,tenant_id);
		if (!Util.isNullOrEmptyList(monitors)) {
			for(Monitor monitor : monitors){
				monitor.normalInfo();//set no used property to null
				List<MonitorObj> monitorObjs = monitorObjMapper.selectListByMonitorId(monitor.getId());
				monitor.setResources(monitorObjs);
			}
			return monitors;
		}
		
		return null;
	}

	@Override
	public Monitor getMonitorById(String id, TokenOs ostoken) throws BusinessException {

        //get the tenant_id by guiToken
		// authority : admin or other check the the id is used by this tenant
		String tenant_id = ostoken.getTenantid();
		
		Monitor monitor = monitorMapper.selectByPrimaryKey(id);
		if (null != monitor) {
			//check the resource had by the tenant
			if(!tenant_id.equals(monitor.getTenantId())){
				log.error("selected monitor is not the user created.");
				return null;
			}
		
			//get resources
			List<MonitorObj> monitorObjs = monitorObjMapper.selectListByMonitorId(monitor.getId());
			monitor.setResources(monitorObjs);
			//get notificationLists
            List<NotificationObj> notificationObjs = notificationObjMapper.selectByMonitorId(monitor.getId());
            NotificationList notificationList = null;
            for(NotificationObj notificationObj : notificationObjs){
            	notificationList = notificationListMapper.selectByPrimaryKey(notificationObj.getTo());
            	notificationObj.setNotificationList(notificationList);
            }
			monitor.setNotificationObjs(notificationObjs);
			//get rules
			List<MonitorRule> monitorRules = monitorRuleMapper.selectListByMonitor(monitor.getId());
			monitor.setRules(monitorRules);
			monitor.normalInfo();//set no used property to null
			return monitor;
		}
		
		return null;
	}
	
	@Override
	public void createMonitor(String createBody, TokenOs ostoken) throws BusinessException{
		Monitor monitorCreate = null;
		ResourceBusinessException resBEMonitorNULL = null;
		try{
			ObjectMapper mapper = new ObjectMapper();
			monitorCreate = mapper.readValue(createBody, Monitor.class);
		}catch(Exception e){
			log.error(e);
			ResourceBusinessException resBEReadValue = new ResourceBusinessException("CS_MONITOR_CREATE_0001",new Locale(ostoken.getLocale()));
			throw resBEReadValue;
		}
		
		if(null == monitorCreate){
			resBEMonitorNULL = new ResourceBusinessException("CS_MONITOR_CREATE_0001",new Locale(ostoken.getLocale()));
			throw resBEMonitorNULL;
		}
		
		//get the monitor monitorList monitorRule
		//monitor
		monitorCreate.setId(Util.makeUUID());
		monitorCreate.setMillionSeconds(Util.getCurrentMillionsecond());
		monitorCreate.setTenantId(ostoken.getTenantid());
		
		//monitorObjs
		Instance instance = null;
		List<MonitorObj> monitorObjsCreate = monitorCreate.getResources();
		for(MonitorObj monitorObj : monitorObjsCreate){
			if(DictConstant.MONITOR_TYPE_INSTANCE.equals(monitorCreate.getType()) || DictConstant.MONITOR_TYPE_BAREMETAL.equals(monitorCreate.getType()) || DictConstant.MONITOR_TYPE_VDI_INSTANCE.equals(monitorCreate.getType())){
				instance = instanceMapper.selectByPrimaryKey(monitorObj.getId());
				if(null == instance){
					resBEMonitorNULL = new ResourceBusinessException("CS_MONITOR_CREATE_0003",new Locale(ostoken.getLocale()));
					throw resBEMonitorNULL;
				}
				monitorObj.setName(instance.getName());
				monitorObj.setStatus(instance.getStatus());
				monitorObj.setType(monitorCreate.getType());
			}else if(DictConstant.MONITOR_TYPE_SERVICE.equals(monitorCreate.getType())){
				CloudService cloudService = serviceService.getService(monitorObj.getId(), ostoken);
				if(null == cloudService){
					resBEMonitorNULL = new ResourceBusinessException("CS_MONITOR_CREATE_0003",new Locale(ostoken.getLocale()));
					throw resBEMonitorNULL;
				}
				monitorObj.setName(cloudService.getName());
				monitorObj.setStatus(cloudService.getStatus());
				monitorObj.setType(DictConstant.MONITOROBJ_TYPE_SERVICE_ + cloudService.getType());
			}else if(DictConstant.MONITOR_TYPE_APPLICATION.equals(monitorCreate.getType())){ //PaaS app monitor
				if(null == monitorPaasServiceImpl){
					log.error("monitor obj is app, but paas component not contained!");
					resBEMonitorNULL = new ResourceBusinessException("CS_MONITOR_CREATE_0003",new Locale(ostoken.getLocale()));
					throw resBEMonitorNULL;
				}

				HashMap<String,String> app = monitorPaasServiceImpl.getAppDetail(monitorObj.getId());
				if(null == app){
					resBEMonitorNULL = new ResourceBusinessException("CS_MONITOR_CREATE_0003",new Locale(ostoken.getLocale()));
					throw resBEMonitorNULL;
				}
				monitorObj.setName(app.get("name"));
				monitorObj.setStatus(app.get("status"));
				monitorObj.setType(monitorCreate.getType());
			}else if(DictConstant.MONITOR_TYPE_LOADBANLANCER.equals(monitorCreate.getType())){ //PaaS loadbanlance monitor
				Loadbalancer loadBanlancer = loadBalancerMapper.selectByPrimaryKey(monitorObj.getId());
				if(null == loadBanlancer){
					resBEMonitorNULL = new ResourceBusinessException("CS_MONITOR_CREATE_0003",new Locale(ostoken.getLocale()));
					throw resBEMonitorNULL;
				}
				monitorObj.setName(loadBanlancer.getName());
				monitorObj.setStatus(loadBanlancer.getOperating_status());
				monitorObj.setType(monitorCreate.getType());
			}else{
				resBEMonitorNULL = new ResourceBusinessException("CS_MONITOR_CREATE_0006",new Locale(ostoken.getLocale()));
				throw resBEMonitorNULL;
			}
			monitorObj.setMonitorId(monitorCreate.getId());
			monitorObj.setCreatedAt(Util.getCurrentDate());
		}
		
		//notificationObjs
		List<NotificationObj> notificationObjsCreate = monitorCreate.getNotificationObjs();
		NotificationList notificationList = null;
		for(NotificationObj notificationObj: notificationObjsCreate){
			notificationObj.setId(Util.makeUUID());
        	notificationList = notificationListMapper.selectByPrimaryKey(notificationObj.getTo());
        	if(null == notificationList){
				resBEMonitorNULL = new ResourceBusinessException("CS_MONITOR_CREATE_0004",new Locale(ostoken.getLocale()));
				throw resBEMonitorNULL;
			}
        	notificationObj.setNotificationList(notificationList);
		}

		//monitorRules  service no rule
		List<MonitorRule> monitorRulesCreate = monitorCreate.getRules();
		for(MonitorRule monitorRule : monitorRulesCreate){
			monitorRule.setId(Util.makeUUID());
			monitorRule.setCreatedAt(Util.getCurrentDate());
		}
		
		//save to db
		createMonitorAndRelatedToDB(monitorCreate, monitorObjsCreate, notificationObjsCreate, monitorRulesCreate);
	}
	
	private void createMonitorAndRelatedToDB(Monitor monitor, List<MonitorObj> monitorObjs, List<NotificationObj> notificationObjs, List<MonitorRule> monitorRules) {
		//save monitor to db
		monitorMapper.insertSelective(monitor);
		
		//save monitorObjs to db
		for(MonitorObj monitorObj: monitorObjs){
			monitorObjMapper.insertSelective(monitorObj);
		}
		//update zabbix
		zabbixService.addMonitorObjs(monitor, monitorObjs);
		
		//save notificationObjs and relations to db
		for(NotificationObj notificationObj: notificationObjs){				
			notificationObjMapper.insertSelective(notificationObj);
			notificationObjMapper.insertNotificationObjRelations(notificationObj.getId(), monitor.getId());
		}
		
		//save monitorRules to db
		for(MonitorRule monitorRule: monitorRules){
			monitorRuleMapper.insertSelective(monitorRule);
			monitorRuleMapper.insertMonitorRuleRelations(monitorRule.getId(), monitor.getId());
		}
		
		//save the resource event info
		storeResourceEventInfo(monitor.getTenantId(),monitor.getId(),ParamConstant.MAAS,null,ParamConstant.STOPPED_STATUS,Util.getCurrentMillionsecond());
	}
	
	//update name and description
	@Override
	public void updateMonitor(String updateBody, TokenOs ostoken) throws BusinessException{
		Monitor monitorUpdate = null;
		try{
			ObjectMapper mapper = new ObjectMapper();
			monitorUpdate = mapper.readValue(updateBody, Monitor.class);
		}catch(Exception e){
			log.error(e);
			throw new ResourceBusinessException("CS_MONITOR_UPDATE_0001",new Locale(ostoken.getLocale()));
		}
		monitorMapper.updateNameAndDescrition(monitorUpdate);
	}
	
	@Override
	public void addResource(String addBody, TokenOs ostoken) throws BusinessException{
		Monitor monitor = null;
		ResourceBusinessException resBEMonitorNULL = null;
		try{
			ObjectMapper mapper = new ObjectMapper();
			JsonNode bodyNode = mapper.readTree(addBody);
			monitor = monitorMapper.selectByPrimaryKey(bodyNode.path("pid").textValue());
			List<String> resourceIds = mapper.readValue(bodyNode.path("Ids").toString(), ArrayList.class);
			List<MonitorObj> tmpMonitorObjList = new ArrayList<MonitorObj>();
			for(String resourceId : resourceIds){
				MonitorObj monitorObj = new MonitorObj();
				monitorObj.setId(resourceId);
				monitorObj.setMonitorId(monitor.getId());
				monitorObj.setCreatedAt(Util.getCurrentDate());
				
				List<MonitorObj> monitorObjList = monitorObjMapper.selectListByMonitorId(monitorObj.getMonitorId());
				for(MonitorObj mo : monitorObjList){
					if(resourceId.equals(mo.getId())){
						throw new ResourceBusinessException("CS_MONITOR_UPDATE_0001",new Locale(ostoken.getLocale()));
					}
				}
				
				if(DictConstant.MONITOR_TYPE_INSTANCE.equals(monitor.getType()) || DictConstant.MONITOR_TYPE_BAREMETAL.equals(monitor.getType())|| DictConstant.MONITOR_TYPE_VDI_INSTANCE.equals(monitor.getType())){
					Instance instance = instanceMapper.selectByPrimaryKey(monitorObj.getId());
					if(null == instance){
						resBEMonitorNULL = new ResourceBusinessException("CS_MONITOR_CREATE_0003",new Locale(ostoken.getLocale()));
						throw resBEMonitorNULL;
					}
					monitorObj.setName(instance.getName());
					monitorObj.setStatus(instance.getStatus());
					monitorObj.setType(monitor.getType());
				}else if(DictConstant.MONITOR_TYPE_SERVICE.equals(monitor.getType())){
					CloudService cloudService = serviceService.getService(monitorObj.getId(), ostoken);
					if(null == cloudService){
						resBEMonitorNULL = new ResourceBusinessException("CS_MONITOR_CREATE_0003",new Locale(ostoken.getLocale()));
						throw resBEMonitorNULL;
					}
					monitorObj.setName(cloudService.getName());
					monitorObj.setStatus(cloudService.getStatus());
					monitorObj.setType(DictConstant.MONITOROBJ_TYPE_SERVICE_ + cloudService.getType());
				}else if(DictConstant.MONITOR_TYPE_APPLICATION.equals(monitor.getType())){ //PaaS app monitor
					if(null == monitorPaasServiceImpl){
						log.error("monitor obj is app, but paas component not contained!");
						resBEMonitorNULL = new ResourceBusinessException("CS_MONITOR_CREATE_0003",new Locale(ostoken.getLocale()));
						throw resBEMonitorNULL;
					}

					HashMap<String,String> app = monitorPaasServiceImpl.getAppDetail(monitorObj.getId());
					if(null == app){
						resBEMonitorNULL = new ResourceBusinessException("CS_MONITOR_CREATE_0003",new Locale(ostoken.getLocale()));
						throw resBEMonitorNULL;
					}
					monitorObj.setName(app.get("name"));
					monitorObj.setStatus(app.get("status"));
					monitorObj.setType(monitor.getType());
				}else if(DictConstant.MONITOR_TYPE_LOADBANLANCER.equals(monitor.getType())){ //PaaS loadbanlance monitor
					Loadbalancer loadBanlancer = loadBalancerMapper.selectByPrimaryKey(monitorObj.getId());
					if(null == loadBanlancer){
						resBEMonitorNULL = new ResourceBusinessException("CS_MONITOR_CREATE_0003",new Locale(ostoken.getLocale()));
						throw resBEMonitorNULL;
					}
					monitorObj.setName(loadBanlancer.getName());
					monitorObj.setStatus(loadBanlancer.getOperating_status());
					monitorObj.setType(monitor.getType());
				}else{
					resBEMonitorNULL = new ResourceBusinessException("CS_MONITOR_CREATE_0006",new Locale(ostoken.getLocale()));
					throw resBEMonitorNULL;
				}

				monitorObjMapper.insertSelective(monitorObj);
				tmpMonitorObjList.add(monitorObj);
			}
			//update zabbix
			zabbixService.addMonitorObjs(monitor, tmpMonitorObjList);
		}catch(Exception e){
			log.error(e);
			throw new ResourceBusinessException("CS_MONITOR_UPDATE_0001",ParamConstant.SERVICE_ERROR_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		}
	}
	
	@Override
	public void deleteResource(String monitorId, String resourceId, TokenOs ostoken) throws BusinessException {
		try{

			Monitor monitor = monitorMapper.selectByPrimaryKey(monitorId);
			if(null == monitor)
				throw new ResourceBusinessException("CS_MONITOR_UPDATE_0001",new Locale(ostoken.getLocale()));

			monitorObjMapper.deleteResource(resourceId, monitorId);
			
			//update zabbix  delete trigger then re-create
			zabbixService.deleteMonitorObjByMonitorId(monitorId, resourceId);
			
		}catch(Exception e){
			log.error(e);
			throw new ResourceBusinessException("CS_MONITOR_UPDATE_0001",new Locale(ostoken.getLocale()));
		}
		
	}
	
	@Override
	public void addRule(String monitorId, String addBody, TokenOs ostoken) throws BusinessException {
		try{
			Monitor monitor = monitorMapper.selectByPrimaryKey(monitorId);	
			if(null == monitor)
				throw new ResourceBusinessException("CS_MONITOR_UPDATE_0001",new Locale(ostoken.getLocale()));
			
			ObjectMapper mapper = new ObjectMapper();
			MonitorRule rule = mapper.readValue(addBody, MonitorRule.class);
			rule.setId(Util.makeUUID());
			monitorRuleMapper.insertSelective(rule);
			monitorRuleMapper.insertMonitorRuleRelations(rule.getId(), monitorId);
			
		}catch(Exception e){
			log.error(e);
			throw new ResourceBusinessException("CS_MONITOR_UPDATE_0001",new Locale(ostoken.getLocale()));
		}
		
	}
	
	@Override
	public void deleteRule(String ruleId, String monitorId, TokenOs ostoken) throws BusinessException {
		try{
			monitorRuleMapper.deleteMonitorRuleRelations(ruleId, monitorId);
			monitorRuleMapper.deleteMonitorRule(ruleId);
			
		}catch(Exception e){
			log.error(e);
			throw new ResourceBusinessException("CS_MONITOR_UPDATE_0001",new Locale(ostoken.getLocale()));
		}
		
	}
	
	@Override
	public void addNotificationList(String addBody, TokenOs ostoken) throws BusinessException {
		try{
			ObjectMapper mapper = new ObjectMapper();
			JsonNode bodyNode = mapper.readTree(addBody);
			String monitorId = bodyNode.path("pid").textValue();
			JsonNode notificationObjsNode = bodyNode.path("notificationObjs");
			ArrayList<HashMap> notificationObjs = mapper.readValue(notificationObjsNode.toString(), ArrayList.class);

			for(HashMap<String,String> nobjh :notificationObjs){
				NotificationObj nobj = new NotificationObj();
				nobj.setId(Util.makeUUID());
				nobj.setStatus(nobjh.get("status"));
				nobj.setTo(nobjh.get("to"));
				
				List<NotificationObj> noList = notificationObjMapper.selectByMonitorId(monitorId);
				for(NotificationObj no : noList){
					if(nobj.getId().equals(no.getId())){
						throw new ResourceBusinessException("CS_MONITOR_UPDATE_0001",new Locale(ostoken.getLocale()));
					}
				}
				
				notificationObjMapper.insertSelective(nobj);
				notificationObjMapper.insertNotificationObjRelations(nobj.getId(),monitorId);
			}
		}catch(Exception e){
			log.error(e);
			throw new ResourceBusinessException("CS_MONITOR_UPDATE_0001",new Locale(ostoken.getLocale()));
		}
	}

	@Override
	public void deleteNotificationObj(String notificationObjId, String monitorId, TokenOs ostoken) throws BusinessException {
		try{
			notificationObjMapper.deleteNotificationObjRelations(notificationObjId, monitorId);
			notificationObjMapper.deleteNotificationObj(notificationObjId);
		}catch(Exception e){
			log.error(e);
			throw new ResourceBusinessException("CS_MONITOR_UPDATE_0001",new Locale(ostoken.getLocale()));
		}
	}
	
	@Override
	public void deleteMonitor(String monitorId, TokenOs ostoken) throws BusinessException{
		Monitor monitor = null;
		ResourceBusinessException resBEMonitorNULL = null;
		monitor = monitorMapper.selectByPrimaryKey(monitorId);
		
		if(null == monitor){
			resBEMonitorNULL = new ResourceBusinessException("CS_MONITOR_DELETE_0003",new Locale(ostoken.getLocale()));
			throw resBEMonitorNULL;
		}		
		if(!monitor.getTenantId().equals(ostoken.getTenantid())){
			resBEMonitorNULL = new ResourceBusinessException("CS_MONITOR_DELETE_0004",new Locale(ostoken.getLocale()));
			throw resBEMonitorNULL;
		}

		//fist update zabbix
		zabbixService.deleteMonitorObjsByMonitorId(monitorId);
				
		//save the resource event info
		String startState = ParamConstant.STOPPED_STATUS;
		if(monitor.getEnable()){
			startState = ParamConstant.ACTIVE_STATUS;
		}
		storeResourceEventInfo(monitor.getTenantId(),monitor.getId(),ParamConstant.MAAS,startState,ParamConstant.DELETED_STATUS,Util.getCurrentMillionsecond());

		//delete monitorObj
		monitorObjMapper.deleteResourceByMonitorId(monitorId);
		
		//delete notificationObj from db
		List<NotificationObj> notificationObjs = notificationObjMapper.selectByMonitorId(monitorId);
		if(!Util.isNullOrEmptyList(notificationObjs)){
			for(NotificationObj notificationObj:notificationObjs){
				notificationObjMapper.deleteByPrimaryKey(notificationObj.getId());
			}
		}
		notificationObjMapper.deleteMonitorNotificationObjsByMonitorId(monitorId);		
		//delete monitorRules from db
		List<MonitorRule> monitorRules = monitorRuleMapper.selectListByMonitor(monitorId);
		for(MonitorRule monitorRule: monitorRules){
			monitorRuleMapper.deleteByPrimaryKey(monitorRule.getId());
		}
		monitorRuleMapper.deleteRelationsForMonitorByMonitoId(monitorId);
		//delete monitor
		monitorMapper.deleteByPrimaryKey(monitorId);
	}
	
	//enable or disable monitor
	@Override
	public void actionMonitor(String id, String actionBody, TokenOs ostoken) throws BusinessException{
		String enable = null;
		try{
			ObjectMapper mapper = new ObjectMapper();
			JsonNode bodyNode = mapper.readTree(actionBody);
			enable = bodyNode.path("enable").toString();
		}
		catch (IOException e){
        	log.error(e);
        	throw new ResourceBusinessException("CS_MONITOR_ACTION_001",new Locale(ostoken.getLocale()));
		}
		
		if (Util.isNullOrEmptyValue(id)) {
			throw new ResourceBusinessException("CS_MONITOR_ACTION_0003",new Locale(ostoken.getLocale()));
		}
		
		Monitor monitor = monitorMapper.selectByPrimaryKey(id);
		
		if(null == monitor){
			throw new ResourceBusinessException("CS_MONITOR_ACTION_0003",new Locale(ostoken.getLocale()));
		}
		if(!enable.equals(monitor.getEnable().toString())){
			monitor.setEnable(!monitor.getEnable());
			//update zabbix && save the resource event info
			if(monitor.getEnable()){
				zabbixService.addMonitorObjsByMonitor(monitor);
				storeResourceEventInfo(monitor.getTenantId(),monitor.getId(),ParamConstant.MAAS,ParamConstant.STOPPED_STATUS,ParamConstant.ACTIVE_STATUS,Util.getCurrentMillionsecond());
			}else{
				zabbixService.deleteMonitorObjsByMonitorId(id);
				storeResourceEventInfo(monitor.getTenantId(),monitor.getId(),ParamConstant.MAAS,ParamConstant.ACTIVE_STATUS,ParamConstant.STOPPED_STATUS,Util.getCurrentMillionsecond());
			}
			monitorMapper.updateByPrimaryKeySelective(monitor);
		}
	}
	
	@Override
	public String getMonitorNewConfigInfo() throws BusinessException{
	    InputStreamReader read = null;
	    String content = "";
	    String lineTxt = "";
	    String filePath = null;
	    BufferedReader bufferedReader = null;
        try { 
        	filePath = MonitorServiceImpl.class.getClassLoader().getResource(CONFIG_FILE_NAME).getPath();
            String encoding="utf-8";
            File file=new File(filePath);            
            if(file.isFile() && file.exists()){ //判断文件是否存在
            	read = new InputStreamReader(new FileInputStream(file),encoding);//考虑到编码格�?
                bufferedReader = new BufferedReader(read);
                while((lineTxt = bufferedReader.readLine()) != null){
                   content += lineTxt;
                }
            }else{
            	log.error("配置文件不存在：文件-->"+filePath);
            }
        } catch (Exception e) {
        	log.error("读取配置文件出错：文�?->"+ filePath);
            log.error(e);
        }finally{
        	 try {
        		 if(null != read)
        			 read.close();
        		 if(null != bufferedReader)
        			 bufferedReader.close();
			} catch (IOException e) {
				log.error(e);
			}
        }
        
        return content;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.cloud.cloudapi.service.openstackapi.MonitorService#getVMCpuUsageStatics(java.util.Map, java.util.Locale)
	 * [
           {

            'avg': 5.0968,
            'duration_start': '2016-05-24 18: 50: 23',
            'unit': '%'
            }
        ]
	 */
	
	@Override
	public String getVMCpuUsageStatics(Map<String, Object> paramMap,Locale locale) throws BusinessException{
		String instanceId = String.valueOf(paramMap.get(ParamConstant.INSTANCE_ID));
		Integer duration = Integer.valueOf(String.valueOf(paramMap.get(ParamConstant.DURATION)));
		
		List<MonitorObj> monitorObjs = monitorObjMapper.selectListById(instanceId);
		if(monitorObjs.size() == 0){
			return null;
		}
		
		long endTime = DateHelper.getNowTimeSecond(); 
		long startTime = endTime - duration * 61;
		String dataType = DictConstant.MONITOR_HISTORY_DATA_TYPE_VM_CPU;
		List<MonitorHistory> monitorHistoyGuis = monitorHistoryMapper.selectListByMonitorObjIdAndDataTime(instanceId, String.valueOf(startTime), String.valueOf(endTime), dataType);

        String result = get60DataPoints(monitorHistoyGuis, startTime, duration);
		return result;
	}
	
	@Override
	public String getVMMemoryUsageStatics(Map<String, Object> paramMap,Locale locale) throws BusinessException{
		String instanceId = String.valueOf(paramMap.get(ParamConstant.INSTANCE_ID));
		List<MonitorObj> monitorObjs = monitorObjMapper.selectListById(instanceId);
		if(monitorObjs.size() == 0){
			return null;
		}

		Integer duration = Integer.valueOf(String.valueOf(paramMap.get(ParamConstant.DURATION)));
		long endTime = DateHelper.getNowTimeSecond();
		long startTime = endTime - duration * 61;
		String dataType = DictConstant.MONITOR_HISTORY_DATA_TYPE_VM_MEMORY;
		List<MonitorHistory> monitorHistoyGuis = monitorHistoryMapper.selectListByMonitorObjIdAndDataTime(instanceId, String.valueOf(startTime), String.valueOf(endTime), dataType);

		String result = get60DataPoints(monitorHistoyGuis, startTime, duration);
		return result;
	}
	
	@Override
	public String getVMDiskUsageStatics(Map<String, Object> paramMap,Locale locale) throws BusinessException{
		String instanceId = String.valueOf(paramMap.get(ParamConstant.INSTANCE_ID));
		List<MonitorObj> monitorObjs = monitorObjMapper.selectListById(instanceId);
		if(monitorObjs.size() == 0){
			return null;
		}
		
		Integer duration = Integer.valueOf(String.valueOf(paramMap.get(ParamConstant.DURATION)));
		long endTime = DateHelper.getNowTimeSecond();
		long startTime = endTime - duration * 60;
		String dataType = DictConstant.MONITOR_HISTORY_DATA_TYPE_VM_DISK;
		List<MonitorHistory> monitorHistoyGuis = monitorHistoryMapper.selectListByMonitorObjIdAndDataTime(instanceId, String.valueOf(startTime), String.valueOf(endTime), dataType);

		String result = get60DataPoints(monitorHistoyGuis, startTime, duration);
		return result;
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see com.cloud.cloudapi.service.openstackapi.MonitorService#getVMNetworkUsageStatics(java.util.Map, java.util.Locale)
	 * [
            {
            u'net_data': {
                u'outer_outgoing': 0.0,
                u'inner_incoming': 11.0100,
                u'outer_incoming': 0.0,
                u'inner_outgoing': 23.6733
            },
            u'unit': u'MB',
            u'time': u'2016-09-2912: 17: 13'
        },

        ]
	 */
	
	@Override
	public String getVMNetworkUsageStatics(Map<String, Object> paramMap,Locale locale) throws BusinessException{
		String instanceId = String.valueOf(paramMap.get(ParamConstant.INSTANCE_ID));
		List<MonitorObj> monitorObjs = monitorObjMapper.selectListById(instanceId);
		if(monitorObjs.size() == 0){
			return null;
		}
		Integer duration = Integer.valueOf(String.valueOf(paramMap.get(ParamConstant.DURATION)));
		long endTime = DateHelper.getNowTimeSecond();
		long startTime = endTime - duration * 61;
		
		String dataType = DictConstant.MONITOR_HISTORY_DATA_TYPE_VM_EXTERNALNET_IN;
		List<MonitorHistory> monitorHistoyGuisEIn = monitorHistoryMapper.selectListByMonitorObjIdAndDataTime(instanceId, String.valueOf(startTime), String.valueOf(endTime), dataType);
		List<HashMap<String, String>> restultNetEIn = get60DataPointDatas(monitorHistoyGuisEIn, startTime, duration);
		
		dataType = DictConstant.MONITOR_HISTORY_DATA_TYPE_VM_EXTERNALNET_OUT;
		List<MonitorHistory> monitorHistoyGuisEOut = monitorHistoryMapper.selectListByMonitorObjIdAndDataTime(instanceId, String.valueOf(startTime), String.valueOf(endTime), dataType);
		List<HashMap<String, String>> restultNetEOut = get60DataPointDatas(monitorHistoyGuisEOut, startTime, duration);
		
		dataType = DictConstant.MONITOR_HISTORY_DATA_TYPE_VM_INTERNALNET_IN;
		List<MonitorHistory> monitorHistoyGuisIIn = monitorHistoryMapper.selectListByMonitorObjIdAndDataTime(instanceId, String.valueOf(startTime), String.valueOf(endTime), dataType);
		List<HashMap<String, String>> restultNetIIn = get60DataPointDatas(monitorHistoyGuisIIn, startTime, duration);
		
		dataType = DictConstant.MONITOR_HISTORY_DATA_TYPE_VM_INTERNALNET_OUT;
		List<MonitorHistory> monitorHistoyGuisIOut = monitorHistoryMapper.selectListByMonitorObjIdAndDataTime(instanceId, String.valueOf(startTime), String.valueOf(endTime), dataType);
		List<HashMap<String, String>> restultNetIOut = get60DataPointDatas(monitorHistoyGuisIOut, startTime, duration);
		
		if(null == restultNetEIn || null == restultNetEOut || null == restultNetIIn || null == restultNetIOut)
			return null;
		
		List<HashMap<String,Object>> result = new ArrayList<HashMap<String,Object>>();
		HashMap<String,Object> tmp = null;
		HashMap<String,Object> tmpNetData = null;
		
		for(int i = 0; i < restultNetEIn.size(); i++){
			tmp =  new HashMap<String,Object>();
			tmp.put(ResponseConstant.UNIT, restultNetEIn.get(i).get(ResponseConstant.UNIT));
			tmp.put(ResponseConstant.TIME, restultNetEIn.get(i).get(ResponseConstant.DURATION_START));
			tmpNetData = new HashMap<String,Object>();
			tmpNetData.put(ResponseConstant.OUTER_INCOMING, restultNetEIn.get(i).get(ParamConstant.AVG));
			tmpNetData.put(ResponseConstant.OUTER_OUTGOING, restultNetEOut.get(i).get(ParamConstant.AVG));
			tmpNetData.put(ResponseConstant.INNER_INCOMING, restultNetIIn.get(i).get(ParamConstant.AVG));
			tmpNetData.put(ResponseConstant.INNER_OUTGOING, restultNetIOut.get(i).get(ParamConstant.AVG));			
			tmp.put(ResponseConstant.NET_DATA, tmpNetData);
			result.add(tmp);
		}
		
		JsonHelper<List<HashMap<String,Object>>, String> jsonHelp = new JsonHelper<List<HashMap<String,Object>>, String>();
		return jsonHelp.generateJsonBodyWithEmpty(result);
	}
	
	@Override
	public String getPhysicalCpuUsageStatics(Map<String, Object> paramMap,Locale locale) throws BusinessException{		
		String PhysicalId = String.valueOf(paramMap.get(ParamConstant.PHY_HOST));
		Integer duration = Integer.valueOf(String.valueOf(paramMap.get(ParamConstant.DURATION)));
		long endTime = DateHelper.getNowTimeSecond();
		long startTime = endTime - duration * 61;
		String dataType = DictConstant.MONITOR_HISTORY_DATA_TYPE_PHYSICAL_CPU;
		List<MonitorHistory> monitorHistoyGuis = monitorHistoryMapper.selectListByMonitorObjIdAndDataTime(PhysicalId, String.valueOf(startTime), String.valueOf(endTime), dataType);
		
		String result = get60DataPoints(monitorHistoyGuis, startTime, duration);
		return result;

	}
	
	@Override
	public String getPhysicalMemoryUsageStatics(Map<String, Object> paramMap,Locale locale) throws BusinessException{
		String PhysicalId = String.valueOf(paramMap.get(ParamConstant.PHY_HOST));
		Integer duration = Integer.valueOf(String.valueOf(paramMap.get(ParamConstant.DURATION)));
		long endTime = DateHelper.getNowTimeSecond();
		long startTime = endTime - duration * 61;
		String dataType = DictConstant.MONITOR_HISTORY_DATA_TYPE_PHYSICAL_MEMORY;
		List<MonitorHistory> monitorHistoyGuis = monitorHistoryMapper.selectListByMonitorObjIdAndDataTime(PhysicalId, String.valueOf(startTime), String.valueOf(endTime), dataType);
		
		String result = get60DataPoints(monitorHistoyGuis, startTime, duration);
		return result;
	}
	
	@Override
	public String getPhysicalDiskUsageStatics(Map<String, Object> paramMap,Locale locale) throws BusinessException{
		String PhysicalId = String.valueOf(paramMap.get(ParamConstant.PHY_HOST));
		Integer duration = Integer.valueOf(String.valueOf(paramMap.get(ParamConstant.DURATION)));
		long endTime = DateHelper.getNowTimeSecond();
		long startTime = endTime - duration * 61;
		String dataType = DictConstant.MONITOR_HISTORY_DATA_TYPE_PHYSICAL_DISK;
		List<MonitorHistory> monitorHistoyGuis = monitorHistoryMapper.selectListByMonitorObjIdAndDataTime(PhysicalId, String.valueOf(startTime), String.valueOf(endTime), dataType);
		
		String result = get60DataPoints(monitorHistoyGuis, startTime, duration);
		return result;
	}
	
	@Override
	public String getPhysicalNetworkUsageStatics(Map<String, Object> paramMap,Locale locale) throws BusinessException{
		String physicalId = String.valueOf(paramMap.get(ParamConstant.PHY_HOST));
		Integer duration = Integer.valueOf(String.valueOf(paramMap.get(ParamConstant.DURATION)));
		long endTime = DateHelper.getNowTimeSecond();
		long startTime = endTime - duration * 61;
		
		String dataType = DictConstant.MONITOR_HISTORY_DATA_TYPE_PHYSICAL_NET_IN;
		List<MonitorHistory> monitorHistoyGuisNetIn = monitorHistoryMapper.selectListByMonitorObjIdAndDataTime(physicalId, String.valueOf(startTime), String.valueOf(endTime), dataType);
		List<HashMap<String, String>> restultNetNetIn = get60DataPointDatas(monitorHistoyGuisNetIn, startTime, duration);
		
		dataType = DictConstant.MONITOR_HISTORY_DATA_TYPE_VM_EXTERNALNET_OUT;
		List<MonitorHistory> monitorHistoyGuisNetOut = monitorHistoryMapper.selectListByMonitorObjIdAndDataTime(physicalId, String.valueOf(startTime), String.valueOf(endTime), dataType);
		List<HashMap<String, String>> restultNetNetOut = get60DataPointDatas(monitorHistoyGuisNetOut, startTime, duration);
		
		List<HashMap<String,Object>> result = new ArrayList<HashMap<String,Object>>();
		HashMap<String,Object> tmp = null;
		HashMap<String,Object> tmpNetData = null;
		
		for(int i = 0; i < restultNetNetIn.size(); i++){
			tmp =  new HashMap<String,Object>();
			tmp.put(ResponseConstant.UNIT, restultNetNetIn.get(i).get(ResponseConstant.UNIT));
			tmp.put(ResponseConstant.TIME, restultNetNetIn.get(i).get(ResponseConstant.DURATION_START));
			tmpNetData = new HashMap<String,Object>();
			tmpNetData.put(ResponseConstant.NET_IN, restultNetNetIn.get(i).get(ParamConstant.AVG));
			tmpNetData.put(ResponseConstant.NET_OUT, restultNetNetOut.get(i).get(ParamConstant.AVG));
			tmp.put(ResponseConstant.NET_DATA, tmpNetData);
			result.add(tmp);
		}
		
		JsonHelper<List<HashMap<String,Object>>, String> jsonHelp = new JsonHelper<List<HashMap<String,Object>>, String>();
		return jsonHelp.generateJsonBodyWithEmpty(result);
	}
	
	@Override
	public List<MonitorTemplate> getMonitorTemplates(Map<String, String>paramMap, TokenOs ostoken) throws BusinessException{
		return monitorTemplateMapper.selectAll();
	}
	
	@Override
	public MonitorTemplate getMonitorTemplate(String id,TokenOs ostoken) throws BusinessException{
		return monitorTemplateMapper.selectByPrimaryKey(id);
	}
	
	@Override
	public MonitorTemplate createMonitorTemplate(String body,TokenOs ostoken) throws BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(body);
		} catch (Exception e) {
		    log.error("error",e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		} 
		
		MonitorTemplate template = new MonitorTemplate();
		String name = rootNode.path(ResponseConstant.NAME).textValue();
		String description = rootNode.path(ResponseConstant.DESCRIPTION).textValue();
		
		String templateId = Util.makeUUID();
		template.setId(templateId);
		template.setName(name);
		template.setDescription(description);
		template.setDefaultFlag(false);
		template.setMillionSeconds(Util.getCurrentMillionsecond());
	
		monitorTemplateMapper.insertSelective(template);
		return template;
	}
	
	@Override
	public MonitorTemplate updateMonitorTemplate(String id,String body,TokenOs ostoken) throws BusinessException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(body);
		} catch (Exception e) {
		    log.error("error",e);
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		} 

		String name = null;
		String description = null;
		Boolean defaultFlag = null;
		if(!rootNode.path(ResponseConstant.NAME).isMissingNode())
			name = rootNode.path(ResponseConstant.NAME).textValue();
		if(!rootNode.path(ResponseConstant.DESCRIPTION).isMissingNode())
			description = rootNode.path(ResponseConstant.DESCRIPTION).textValue();
		if(!rootNode.path(ResponseConstant.FLAG).isMissingNode())
			defaultFlag = rootNode.path(ResponseConstant.FLAG).booleanValue();
		
		MonitorTemplate template = monitorTemplateMapper.selectByPrimaryKey(id);
		if(null == template)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));

		if(null != name){
			template.setName(name);
			checkName(name,ostoken);
		}
		if(null != description)
			template.setDescription(description);
		if(null != defaultFlag){
			resetTemplatesFlag();
			template.setDefaultFlag(defaultFlag);
		}
		monitorTemplateMapper.insertOrUpdate(template);
		return template;
	}
	
	@Override
	public void applyMonitorTemplate(String id,String tenantId,TokenOs ostoken) throws BusinessException{
		Tenant tenant = tenantMapper.selectByPrimaryKey(tenantId);
		if(null == tenant)
			throw new ResourceBusinessException(Message.CS_REQUEST_IS_WRONG,ParamConstant.BAD_REQUEST_RESPONSE_CODE,new Locale(ostoken.getLocale()));
		tenant.setMonitor_template_id(id);
		tenantMapper.insertOrUpdate(tenant);
	}
	
	@Override
	public void deleteMonitorTemplate(String id,TokenOs ostoken) throws BusinessException{
		monitorTemplateMapper.deleteByPrimaryKey(id);
	}
	//get the monitor data for 60's data
	//startTime unit is second
	//duration unit is minute
	private String get60DataPoints(List<MonitorHistory> monitorHistoyGuis, long startTime, long duration){
		List<HashMap<String,String>> result = new ArrayList<HashMap<String,String>>();
		result = get60DataPointDatas(monitorHistoyGuis, startTime, duration);
		
		JsonHelper<List<HashMap<String,String>>, String> jsonHelp = new JsonHelper<List<HashMap<String,String>>, String>();
		return jsonHelp.generateJsonBodyWithEmpty(result);
	}
	
	//get the monitor data for 60's data
	//startTime unit is second
	//duration unit is minute
	private List<HashMap<String,String>> get60DataPointDatas(List<MonitorHistory> monitorHistoyGuis, long startTime, long duration){
		List<HashMap<String,String>> result = new ArrayList<HashMap<String,String>>();
		if(monitorHistoyGuis.size() == 0){
			return null;	
		}
		long intevalTime = (duration * 60)/MonitorDataNum4GUI ;
		String dataUnit = monitorHistoyGuis.get(0).getDataUnit();
	
		HashMap<String,String> tmp = null;
		long endTime = startTime + intevalTime;;
		float tmpValue = 0;
		int times = 0;
		for(MonitorHistory mhg : monitorHistoyGuis){
			while(true){
				if(mhg.getDataTime() <= endTime){
					float dataValue = Float.valueOf(mhg.getDataValue());
					tmpValue = tmpValue + dataValue;	
					times = times + 1;
					break;
				}else{
					if(times > 0){
						tmp = new HashMap<String,String>();
						tmp.put(ParamConstant.DURATION_START, DateHelper.longToStr(endTime*1000));
						tmp.put(ParamConstant.UNIT, dataUnit);
						tmp.put(ParamConstant.AVG, String.valueOf(tmpValue/times));
						result.add(tmp);
					}else{
						tmp = new HashMap<String,String>();
						tmp.put(ParamConstant.DURATION_START, String.valueOf(DateHelper.longToStr(startTime*1000)));
						tmp.put(ParamConstant.UNIT, dataUnit);
						tmp.put(ParamConstant.AVG, "0");
						result.add(tmp);
					}
					startTime = endTime;
					endTime = startTime + intevalTime;
					tmpValue = 0;
					times = 0;
				}
			}
	    }
		//save the last data and  ---endTime
		tmp = new HashMap<String,String>();
		tmp.put(ParamConstant.DURATION_START, String.valueOf(DateHelper.longToStr(startTime*1000)));
		tmp.put(ParamConstant.UNIT, dataUnit);
		tmp.put(ParamConstant.AVG, times == 0? "0": String.valueOf(tmpValue/times));
		result.add(tmp);
		
		int lackResultsize = MonitorDataNum4GUI - result.size();
		for(int i = 0; i < lackResultsize; i++){
			startTime = startTime + intevalTime;
			tmp = new HashMap<String,String>();
			tmp.put(ParamConstant.DURATION_START, String.valueOf(DateHelper.longToStr((startTime)*1000)));
			tmp.put(ParamConstant.UNIT, dataUnit);
			tmp.put(ParamConstant.AVG, (lackResultsize > 1 || times == 0) ? "0": String.valueOf(tmpValue/times));
			result.add(tmp);
		}

		return result;
	}
	
	private void storeResourceEventInfo(String tenantId,String id,String type,String beginState,String endState,long time){
		ResourceEvent event = new ResourceEvent();
		event.setTenantId(tenantId);
		event.setResourceId(id);
		event.setResourceType(type);
		event.setBeginState(beginState);
		event.setEndState(endState);
		event.setMillionSeconds(time);
		resourceEventMapper.insertSelective(event);
	}
	
	private void checkName(String name, TokenOs ostoken) throws BusinessException {
		List<MonitorTemplate> monitorTemplates = monitorTemplateMapper.selectAll();
		if (Util.isNullOrEmptyList(monitorTemplates))
			return;
		for (MonitorTemplate monitorTemplate : monitorTemplates) {
			if (name.equals(monitorTemplate.getName()))
				throw new ResourceBusinessException(Message.CS_RESOURCE_NAME_IS_SAME,ParamConstant.BAD_REQUEST_RESPONSE_CODE, new Locale(ostoken.getLocale()));
		}
	}
	
	private void resetTemplatesFlag(){
		List<MonitorTemplate> monitorTemplates = monitorTemplateMapper.selectAll();
		if (Util.isNullOrEmptyList(monitorTemplates))
			return;
		for (MonitorTemplate monitorTemplate : monitorTemplates){
			monitorTemplate.setDefaultFlag(false);
		}
		monitorTemplateMapper.insertOrUpdateBatch(monitorTemplates);
	}
}
