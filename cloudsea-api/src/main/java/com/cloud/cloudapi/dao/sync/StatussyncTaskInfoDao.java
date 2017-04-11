package com.cloud.cloudapi.dao.sync;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.cloud.cloudapi.dao.common.SuperMapper;
import com.cloud.cloudapi.pojo.businessapi.sync.StatussyncTaskInfo;

/** 
* @author  wangw
* @create  2016年8月23日 下午1:56:06 
* 
*/
public interface StatussyncTaskInfoDao extends SuperMapper<StatussyncTaskInfo,String>{
	
	public List<StatussyncTaskInfo> selectAll();

	public List<StatussyncTaskInfo> selectAllForPage(int start, int end);
	
	/**
	 * 根据表名，列名，值来更新目标table
	 * @return
	 */
	public int updateTargetTable(@Param(value="table") String table,@Param(value="column") String column,
			                     @Param(value="value") String value,@Param(value="id") String id);
	
	
	/**
	 * 取得未结束的task，未结束的状态包括: running,retrying,retryed
	 * @return
	 */
	public List<StatussyncTaskInfo> selectUnfinishedTasks();
	
	
	/**
	 * 取得retrying状态的task
	 * @return
	 */
	public List<StatussyncTaskInfo> selectRetryingTasks();
	

}
