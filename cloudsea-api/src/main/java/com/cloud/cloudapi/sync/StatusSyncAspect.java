package com.cloud.cloudapi.sync;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;

import com.cloud.cloudapi.dao.sync.StatussyncTaskInfoDao;
import com.cloud.cloudapi.pojo.businessapi.sync.StatussyncTaskInfo;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.common.Util;
import com.cloud.cloudapi.util.StringHelper;



/** 中间层数据中资源状态与openstack资源状态进行同步
 *  资源包括：instance, volume, network...
* @author  wangw
* @create  2016年8月20日 下午4:55:28 
* 
*/

public class StatusSyncAspect {
	
	
	@Resource
	private ThreadPoolParameter poolPara;
	@Resource
	private StatussyncTaskInfoDao stDao;
	
	private  Logger log = LogManager.getLogger(StatusSyncDeamon.logName);
	/**
	 * 执行同步方法
	 * @param joinPoint
	 * @param resource 切点方法返回的值
	 */
	public void doSync(JoinPoint joinPoint, Object resource){
		
		//如果以List的形式返回对象
		if(resource instanceof List){
			for(Object obj:(ArrayList)resource){
				createSyncTaskInfo(obj);
			}
			
		}else{
			createSyncTaskInfo(resource);
		}
	}
	
	
	
	private boolean createSyncTaskInfo(Object resource){
		StatussyncTaskInfo taskInfo =  new StatussyncTaskInfo();
		String region = StatusSyncDeamon.tokenObject.getCurrentRegion();
		String resourceId = "";
		try {
			Method getIdMethod = resource.getClass().getMethod("getId");
			resourceId = StringHelper.objectToString(getIdMethod.invoke(resource));
			//创建instance任务
			if(resource instanceof com.cloud.cloudapi.pojo.openstackapi.forgui.Instance){
				log.info("开始插入新的sync task....");
				String url = StatusSyncDeamon.tokenObject.getEndPoint(TokenOs.EP_TYPE_COMPUTE, region).getPublicURL();
				StringBuilder sb = new StringBuilder();
				sb.append(url);
				sb.append("/servers/");
				sb.append(resourceId);
				taskInfo.setId(Util.makeUUID());
				taskInfo.setResourceUuid(resourceId);
				taskInfo.setResourceBeginStatus("BUILDING");
				taskInfo.setResourceTargetStatus("ACTIVE|ERROR");
				taskInfo.setResourceOsUrl(sb.toString());
				taskInfo.setResourceOsKey("status");
				taskInfo.setResourceDbTable("instances");
				taskInfo.setResourceDbColumn("status");
				taskInfo.setSyncTaskStatus(StatusSyncDeamon.STATUS_OF_TASK_RUNNING);
				stDao.insertSelective(taskInfo);
				StatusSyncDeamon.taskqueue.add(taskInfo);
				log.info("sync task 插入完成,task info:{"+taskInfo.toString()+"}");
				
				log.info("开始插入新的sync task....");
				//增加对IP的同步
				taskInfo = new StatussyncTaskInfo();
				sb = new StringBuilder();
				sb.append(url).
				append("/servers/").
				append(resourceId).
				append("/ips");
				taskInfo.setId(Util.makeUUID());
				taskInfo.setResourceUuid(resourceId);
				taskInfo.setResourceBeginStatus("");
				taskInfo.setResourceTargetStatus("");
				taskInfo.setResourceOsUrl(sb.toString());
				taskInfo.setResourceOsKey("addr");
				taskInfo.setResourceDbTable("instances");
				taskInfo.setResourceDbColumn("fixedips");
				taskInfo.setSyncTaskStatus(StatusSyncDeamon.STATUS_OF_TASK_RUNNING);
				stDao.insertSelective(taskInfo);
				StatusSyncDeamon.taskqueue.add(taskInfo);
				log.info("sync task 插入完成,task info:{"+taskInfo.toString()+"}");
				
				log.info("开始插入新的sync task....");
				//增加对volume id的同步
				taskInfo = new StatussyncTaskInfo();
				sb = new StringBuilder();
				sb.append(url).
				append("/servers/").
				append(resourceId).
				append("/os-volume_attachments");
				taskInfo.setId(Util.makeUUID());
				taskInfo.setResourceUuid(resourceId);
				taskInfo.setResourceBeginStatus("");
				taskInfo.setResourceTargetStatus("");
				taskInfo.setResourceOsUrl(sb.toString());
				taskInfo.setResourceOsKey("volumeId");
				taskInfo.setResourceDbTable("instances");
				taskInfo.setResourceDbColumn("volume_ids");
				taskInfo.setSyncTaskStatus(StatusSyncDeamon.STATUS_OF_TASK_RUNNING);
				stDao.insertSelective(taskInfo);
				StatusSyncDeamon.taskqueue.add(taskInfo);
				log.info("sync task 插入完成,task info:{"+taskInfo.toString()+"}");
				
			}else if(resource instanceof com.cloud.cloudapi.pojo.openstackapi.forgui.Volume){ //创建volume任务
				log.info("开始插入新的sync task....");
				String url = StatusSyncDeamon.tokenObject.getEndPoint(TokenOs.EP_TYPE_VOLUMEV2, region).getPublicURL();
				StringBuilder sb = new StringBuilder();
				sb.append(url);
				sb.append("/volumes/");
				sb.append(resourceId);
				taskInfo.setId(Util.makeUUID());
				taskInfo.setResourceUuid(resourceId);
				taskInfo.setResourceBeginStatus("creating");
				//taskInfo.setResourceTargetStatus("");
				taskInfo.setResourceOsUrl(sb.toString());
				taskInfo.setResourceOsKey("status");
				taskInfo.setResourceDbTable("volumes");
				taskInfo.setResourceDbColumn("status");
				taskInfo.setSyncTaskStatus(StatusSyncDeamon.STATUS_OF_TASK_RUNNING);
				stDao.insertSelective(taskInfo);
				StatusSyncDeamon.taskqueue.add(taskInfo);
				log.info("sync task 插入完成,task info:{"+taskInfo.toString()+"}");
			}else if(resource instanceof com.cloud.cloudapi.pojo.openstackapi.forgui.Network){ //创建网络任务
				log.info("开始插入新的sync task....");
				String url = StatusSyncDeamon.tokenObject.getEndPoint(TokenOs.EP_TYPE_NETWORK, region).getPublicURL();
				StringBuilder sb = new StringBuilder();
				sb.append(url);
				sb.append("/v2.0/networks/");
				sb.append(resourceId);
				taskInfo.setId(Util.makeUUID());
				taskInfo.setResourceUuid(resourceId);
				taskInfo.setResourceBeginStatus("BUILDING");
				taskInfo.setResourceTargetStatus("ACTIVE|ERROR");
				taskInfo.setResourceOsUrl(sb.toString());
				taskInfo.setResourceOsKey("/network/status");
				taskInfo.setResourceDbTable("networks");
				taskInfo.setResourceDbColumn("status");
				taskInfo.setSyncTaskStatus(StatusSyncDeamon.STATUS_OF_TASK_RUNNING);
				stDao.insertSelective(taskInfo);
				StatusSyncDeamon.taskqueue.add(taskInfo);
				log.info("sync task 插入完成,task info:{"+taskInfo.toString()+"}");
			}else if(resource instanceof com.cloud.cloudapi.pojo.openstackapi.forgui.Container ){ //创建容器
				log.info("开始插入新的sync task....");
				String url = StatusSyncDeamon.tokenObject.getEndPoint(TokenOs.EP_TYPE_CONTAINER, region).getPublicURL();
				StringBuilder sb = new StringBuilder();
				sb.append(url);
				sb.append("/bays/");
				sb.append(resourceId);
				taskInfo.setId(Util.makeUUID());
				taskInfo.setResourceUuid(resourceId);
				taskInfo.setResourceBeginStatus("CREATE_IN_PROGRESS");
				taskInfo.setResourceTargetStatus("CREATE_FAILED|CREATE_COMPLETE");
				taskInfo.setResourceOsUrl(sb.toString());
				taskInfo.setResourceOsKey("status");
				taskInfo.setResourceDbTable("containers");
				taskInfo.setResourceDbColumn("status");
				taskInfo.setSyncTaskStatus(StatusSyncDeamon.STATUS_OF_TASK_RUNNING);
				stDao.insertSelective(taskInfo);
				StatusSyncDeamon.taskqueue.add(taskInfo);
				log.info("sync task 插入完成,task info:{"+taskInfo.toString()+"}");
			}
			else{
				log.warn("无法找到此资源类型: type ->" + resource.getClass());
				return false;
			}
		} catch (Exception e){
			log.error("sync task 插入失败!"+e.getStackTrace());
		}
		return true;
	}
	

}
