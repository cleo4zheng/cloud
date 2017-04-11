package com.cloud.cloudapi.sync;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.cloud.cloudapi.dao.sync.StatussyncTaskInfoDao;
import com.cloud.cloudapi.pojo.businessapi.sync.StatussyncTaskInfo;
import com.cloud.cloudapi.util.ResponseConstant;
import com.cloud.cloudapi.util.StringHelper;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 
 * 执行同步任务的task
* @author  wangw
* @create  2016年8月23日 下午2:12:59 
* 
*/
public class StatusSyncTask implements Runnable{
	
	
	private StatussyncTaskInfo taskInfo;
	private String osTokenId = "";
	
	
	private StatussyncTaskInfoDao stDao;

	private OSHttpClientUtil httpClientUtil;
	
	ObjectMapper mapper = new ObjectMapper();
	
	HashMap result = null;
	
	private static Logger log = LogManager.getLogger(StatusSyncDeamon.logName);
	
	//状态信息
	private String beginStatus;
	private String targetStatus;
	
	
	public StatusSyncTask(StatussyncTaskInfo taskInfo, String token,OSHttpClientUtil httpClientUtil,StatussyncTaskInfoDao stDao){
		this.taskInfo = taskInfo;
		this.osTokenId = token;
		this.httpClientUtil = httpClientUtil;
		this.stDao = stDao;
		this.beginStatus = StringHelper.objectToString(taskInfo.getResourceBeginStatus());
		this.targetStatus = StringHelper.objectToString(taskInfo.getResourceTargetStatus());
	}

	@Override
	public void run() {
		int count = StatusSyncDeamon.TASK_SYNC_COUNT;
		try {
			//如果是状态为retrying，则设置成retryed，防止重复执行
			if(StatusSyncDeamon.STATUS_OF_TASK_RETRYING.equals(taskInfo.getSyncTaskStatus())){
				taskInfo.setSyncTaskStatus(StatusSyncDeamon.STATUS_OF_TASK_RETRYED);
				stDao.updateByPrimaryKeySelective(taskInfo);
			}
			while (count > 0) {
				count--;
				String url = taskInfo.getResourceOsUrl();
				result = (HashMap) httpClientUtil.httpDoGet(url, this.osTokenId);
				int httpcode = Integer.parseInt(StringHelper.objectToString(result.get(ResponseConstant.HTTPCODE)));
				// token无效或者过期
				if (401 == httpcode) {
					// 检查Token是否已经更新
					if (!StatusSyncDeamon.osToken.equals(this.osTokenId)) {
						this.osTokenId = StatusSyncDeamon.osToken;
					} else { // 若token没有更新,则等待token更新
						StatusSyncDeamon.setOsTokenExpired(true);
						this.waitUtilTokenUpdate();
					}
				} else if (200 == httpcode) {
					// 超时
					if (0 == count && (!checkStatus(StringHelper.objectToString(result.get(ResponseConstant.JSONBODY))))) {
						//taskInfo.setSyncTaskStatus(StatusSyncDeamon.STATUS_OF_TASK_TIMEOUT);
						if(StatusSyncDeamon.STATUS_OF_TASK_RETRYED.equals(taskInfo.getSyncTaskStatus())){
							taskInfo.setSyncTaskStatus(StatusSyncDeamon.STATUS_OF_TASK_TIMEOUT);
							log.error("等待资源信息更新失败: 超时 task id -> " + taskInfo.getId() + " ,timeout = "
									+ StatusSyncDeamon.TASK_SYNC_COUNT * StatusSyncDeamon.TASK_SYNC_INTERVAL);
						}else{
							taskInfo.setSyncTaskStatus(StatusSyncDeamon.STATUS_OF_TASK_RETRYING);
							log.error("等待资源信息更新失败: 超时 , 设置状态retrying, 等待下次执行! task id -> " + taskInfo.getId());
						}
						
						break;
						// 更新成功
					} else if (checkStatus(StringHelper.objectToString(result.get(ResponseConstant.JSONBODY)))) {
						taskInfo.setSyncTaskStatus(StatusSyncDeamon.STATUS_OF_TASK_COMPLETED);
						break;
					}
				} else {
					// 执行api出错
					taskInfo.setSyncTaskStatus(StatusSyncDeamon.STATUS_OF_TASK_FAILED);
					log.error("等待资源信息更新失败: 执行API出错   task id -> " + taskInfo.getId() + " ,responseBody = "
							+ result.get(ResponseConstant.JSONBODY) + " httpCode = " + httpcode);
					break;
				}
				Thread.sleep(StatusSyncDeamon.TASK_SYNC_INTERVAL);
			}
		} catch (Exception e) { //发生异常时设置为failed
			taskInfo.setSyncTaskStatus(StatusSyncDeamon.STATUS_OF_TASK_FAILED);
			log.error("等待资源信息更新失败：未知异常 ->: "+e.getStackTrace());
			stDao.updateByPrimaryKeySelective(taskInfo);
		}
		stDao.updateByPrimaryKeySelective(taskInfo);
		
	}
	
	/**
	 * 等待os的token更新,每次最多等待10s,且不计入线程的执行时间
	 */
	private void waitUtilTokenUpdate(){
		int count = 10;
		while(count > 0){
			count -= 1;
			if (!StatusSyncDeamon.osToken.equals(osTokenId)){
				this.osTokenId = StatusSyncDeamon.osToken;
				break;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * 检查response中的字段值，并判断是否需要更新数据库
	 * @throws IOException 
	 * @throws JsonProcessingException 
	 */
	private boolean checkStatus(String jsonbody) throws JsonProcessingException, IOException{
		
		String status = "";
		String os_key = taskInfo.getResourceOsKey();
		if(StringHelper.isNullOrEmpty(os_key))
			return false;
		/*String[] keys = os_key.split("/");
		JsonNode jsonNode = null;
		try {
			jsonNode = mapper.readTree(jsonbody);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(int i =0 ;i< keys.length ;i++){
			if(StringHelper.isNullOrEmpty(keys[i]))
				continue;
			jsonNode = jsonNode.path(keys[i]);
			if(jsonNode.isArray()&&jsonNode.size()>0){
				jsonNode = jsonNode.get(0);
			}
		}
		if (null != jsonNode)
		    status = jsonNode.asText();*/
		JsonNode jsonNode = null;
		List<String> values = null;
		jsonNode = mapper.readTree(jsonbody);
		values = jsonNode.findValuesAsText(os_key);
		if (values.size() > 0){
			for(String str:values ){
				status += str + ",";
			}
		}
		if (!StringHelper.isNullOrEmpty(status))
		    status = status.substring(0, status.length()-1);
		//当前得到的状态与期望的状态符合
		if((!StringHelper.isNullOrEmpty(targetStatus)) && targetStatus.contains(status)){
			//向目标表中写入数据
			stDao.updateTargetTable(taskInfo.getResourceDbTable(), taskInfo.getResourceDbColumn(), status, taskInfo.getResourceUuid());
			taskInfo.setResourceEndStatus(status);
			log.info("资源信息更新成功: task id -> "+taskInfo.getId()+" ,beginStatus = "+beginStatus+", targetStatus = "+targetStatus + ", currentStatus = "+status);
			return true;
		}
		//在无期望状态的情况下,当前得到的状态与初始的状态不同,则默认状态已经更新
		if(StringHelper.isNullOrEmpty(targetStatus) && (!beginStatus.contains(status))){
			stDao.updateTargetTable(taskInfo.getResourceDbTable(), taskInfo.getResourceDbColumn(), status, taskInfo.getResourceUuid());
			taskInfo.setResourceEndStatus(status);
			log.info("资源信息更新成功: task id -> "+taskInfo.getId()+" ,beginStatus = "+beginStatus+", targetStatus = "+targetStatus + ", currentStatus = "+status);
			return true;
		}
		return false;
	}

}
