package com.cloud.cloudapi.service.businessapi.zabbix.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloud.cloudapi.dao.common.MonitorMapper;
import com.cloud.cloudapi.dao.common.MonitorObjMapper;
import com.cloud.cloudapi.dao.businessapi.MonitorObjsOperationHistoryMapper;
import com.cloud.cloudapi.pojo.businessapi.zabbix.MonitorObjsOperationHistory;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Monitor;
import com.cloud.cloudapi.pojo.openstackapi.forgui.MonitorObj;
import com.cloud.cloudapi.service.businessapi.zabbix.ZabbixService;
import com.cloud.cloudapi.util.DictConstant;

/** 
* @author  zhangxw@cn.fujitsu.com 
* @create  2016/7/28 10:35:06 
* 
*/

@Service
public class ZabbixServiceImpl implements ZabbixService {
	
	@Autowired
    private MonitorObjsOperationHistoryMapper mOOHMapper;
    
    @Autowired
	private MonitorObjMapper monitorObjMapper;
    
    @Autowired
   	private MonitorMapper monitorMapper;
    
	public Logger log = LogManager.getLogger(ZabbixServiceImpl.class);

	@Override
	public void addMonitorObjs(Monitor monitor, List<MonitorObj> monitorObjs) {
		log.debug("addMonitorObjs(Monitor monitor, List<MonitorObj> monitorObjs) ...");
		try {
			if(!monitor.getEnable()){
				log.info("addMonitorObjs: monitor is disable, so not create obejct to zabbix.");
				return;
			}
			List<MonitorObjsOperationHistory> tmpList = new ArrayList<MonitorObjsOperationHistory>();
			for(MonitorObj monitorObj : monitorObjs){
				MonitorObjsOperationHistory mOOH = new MonitorObjsOperationHistory();
				mOOH.setMonitorId(monitorObj.getMonitorId());
				mOOH.setMonitorType(monitor.getType());
				mOOH.setMonitorObjId(monitorObj.getId());
				mOOH.setOperationResult(-1);
				mOOH.setOperationType(DictConstant.MONITOR_OBJS_OPERATION_TYPE_ADD);
				tmpList.add(mOOH);
			}

			mOOHMapper.insertSelectives(tmpList);
		} catch (Exception e) {
			log.error("addMonitorObjs:monitor objs insert to history error, e:" + e);
		}
		
	}
	
	@Override
	public void addMonitorObjsByMonitor(Monitor monitor) {
		log.debug("addMonitorObjsByMonitorId(String monitorId) ...");
		try {
			if(!monitor.getEnable()){
				log.info("addMonitorObjs: monitor is disable, so not create obejct to zabbix.");
				return;
			}
//			log.info("=============monitor.getType: "+ monitor.getType());
			List<MonitorObj> tmpMonitorObjList = monitorObjMapper.selectListByMonitorId(monitor.getId());
			List<MonitorObjsOperationHistory> tmpList = new ArrayList<MonitorObjsOperationHistory>();
			for(MonitorObj monitorObj : tmpMonitorObjList){
				MonitorObjsOperationHistory mOOH = new MonitorObjsOperationHistory();
				mOOH.setMonitorId(monitorObj.getMonitorId());
				mOOH.setMonitorType(monitor.getType());
				mOOH.setMonitorObjId(monitorObj.getId());
				mOOH.setOperationResult(-1);
				mOOH.setOperationType(DictConstant.MONITOR_OBJS_OPERATION_TYPE_ADD);
				tmpList.add(mOOH);
			}

			mOOHMapper.insertSelectives(tmpList);
		} catch (Exception e) {
			log.error("addMonitorObjs:monitor objs insert to history error, e:" + e);
		}
		
	}

	@Override
	public void deleteMonitorObj(String objId) {
		log.debug("deleteMonitorObj(String objId) ...");
		try {
			//first get the monitorObj's monitor type
			MonitorObj monitorObj = monitorObjMapper.selectByPrimaryKey(objId);
			String monitorType = "";
			if(null != monitorObj)
				monitorType = monitorObj.getType();
			if(monitorType.contains(DictConstant.MONITOR_TYPE_SERVICE))
				monitorType = DictConstant.MONITOR_TYPE_SERVICE;
			
			//second delete monitor_objs item, because this operation not in the monitor module
			monitorObjMapper.deleteByPrimaryKey(objId);
			
			MonitorObjsOperationHistory mOOH = new MonitorObjsOperationHistory();
			mOOH.setMonitorObjId(objId);
			mOOH.setMonitorType(monitorType);
			mOOH.setOperationResult(-1);
			mOOH.setOperationType(DictConstant.MONITOR_OBJS_OPERATION_TYPE_DELETE);

			mOOHMapper.insertSelective(mOOH);
		} catch (Exception e) {
			log.error("deleteMonitorObj: monitor objs insert to history error, objId:" + objId);
			log.error("deleteMonitorObj:monitor objs insert to history error, e:" + e);
		}
		
	}
	
	@Override
	public void deleteMonitorObjByMonitorId(String monitorId, String objId) {
		log.debug("deleteMonitorObjByMonitorId(String monitorId, String objId) ...");
		try {
			//get the monitor type
			Monitor monitor = monitorMapper.selectByPrimaryKey(monitorId);
		
			MonitorObjsOperationHistory mOOH = new MonitorObjsOperationHistory();
			mOOH.setMonitorId(monitorId);
			mOOH.setMonitorType(monitor.getType());
			mOOH.setMonitorObjId(objId);
			mOOH.setOperationResult(-1);
			mOOH.setOperationType(DictConstant.MONITOR_OBJS_OPERATION_TYPE_DELETE);

			mOOHMapper.insertSelective(mOOH);
		} catch (Exception e) {
			log.error("deleteMonitorObjByMonitorId:monitor objs insert to history error, objId:" + objId + " , monitorId: " + monitorId);
			log.error("deleteMonitorObjByMonitorId:monitor objs insert to history error, e:" + e);
		}
		
	}
	
	
	@Override
	public void deleteMonitorObjs(List<String> objIds) {
		log.debug("deleteMonitorObjs(List<String> objIds) ...");
		try {
			List<MonitorObjsOperationHistory> tmpList = new ArrayList<MonitorObjsOperationHistory>();
			String monitorType = null;
			for(String objId : objIds){
				if(null == monitorType){
					MonitorObj monitorObj = monitorObjMapper.selectByPrimaryKey(objId);
					monitorType = monitorObj.getType();
					if(monitorType.contains(DictConstant.MONITOR_TYPE_SERVICE))
						monitorType = DictConstant.MONITOR_TYPE_SERVICE;
				}
				MonitorObjsOperationHistory mOOH = new MonitorObjsOperationHistory();
				mOOH.setMonitorType(monitorType);
				mOOH.setMonitorObjId(objId);
				mOOH.setOperationResult(-1);
				mOOH.setOperationType(DictConstant.MONITOR_OBJS_OPERATION_TYPE_DELETE);
				tmpList.add(mOOH);
			}
			mOOHMapper.insertSelectives(tmpList);
		} catch (Exception e) {
			log.error("deleteMonitorObjs:monitor objs insert to history error, e:" + e);
		}
	}

	@Override
	public void deleteMonitorObjsByMonitorId(String monitorId) {
		log.debug("deleteMonitorObjs(String monitorId,  List<String> objIds) ...");
		try {
			List<MonitorObj> monitorObjs = monitorObjMapper.selectListByMonitorId(monitorId);
			List<MonitorObjsOperationHistory> tmpList = new ArrayList<MonitorObjsOperationHistory>();
			String monitorType = null;
			for(MonitorObj monitorObj : monitorObjs){
				if(null == monitorType){
					monitorType = monitorObj.getType();
					if(monitorType.contains(DictConstant.MONITOR_TYPE_SERVICE))
						monitorType = DictConstant.MONITOR_TYPE_SERVICE;
				}
				MonitorObjsOperationHistory mOOH = new MonitorObjsOperationHistory();
				mOOH.setMonitorId(monitorObj.getMonitorId());
				mOOH.setMonitorType(monitorType);
				mOOH.setMonitorObjId(monitorObj.getId());
				mOOH.setOperationResult(-1);
				mOOH.setOperationType(DictConstant.MONITOR_OBJS_OPERATION_TYPE_DELETE);
				tmpList.add(mOOH);
			}
			mOOHMapper.insertSelectives(tmpList);
		} catch (Exception e) {
			log.error("deleteMonitorObjsByMonitorId:monitor objs insert to history error, monitorId:" + monitorId);
			log.error("deleteMonitorObjsByMonitorId:monitor objs insert to history error, e:" + e);
		}
		
	}
	
	//only update monitor_objs , not need update zabbix
	@Override
	public void updateMonitorObjName(String objId,  String name) {
		log.debug("updateMonitorObjName(String objId,  String name) ...");
		try {
			monitorObjMapper.updateName(objId, name);
		} catch (Exception e) {
			log.error("updateMonitorObjName:monitor objs insert to history error, objId:" + objId +" , name: " + name);
			log.error("updateMonitorObjName:monitor objs insert to history error, e:" + e);
		}
	}

}