package com.cloud.cloudapi.sync;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.cloud.cloudapi.dao.sync.StatussyncTaskInfoDao;
import com.cloud.cloudapi.pojo.businessapi.sync.StatussyncTaskInfo;
import com.cloud.cloudapi.pojo.common.CloudConfig;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.util.StringHelper;
import com.cloud.cloudapi.util.http.HttpClientForOsBase;
import com.cloud.cloudapi.util.http.pool.OSHttpClientUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 
 * 
 * 随系统启动的状态同步线程
* @author  wangw
* @create  2016年8月22日 下午3:57:04 
* 
*/
public class StatusSyncDeamon {
	
	
	@Resource
	private ThreadPoolParameter poolPara;
	
	@Resource
	private StatussyncTaskInfoDao stDao;
	
	@Resource
	private  CloudConfig cloudConfig;
	
	@Resource
	private OSHttpClientUtil httpClientUtil;
	
	/**
	 * 配置模板
	 */
	public static  List template = null;
	
	/**
	 * 存放同步任务信息的FIFO队列
	 */
	public static ConcurrentLinkedQueue<StatussyncTaskInfo> taskqueue = new ConcurrentLinkedQueue<StatussyncTaskInfo>();
	
	public static String STATUS_OF_TASK_RUNNING = "running"; //task运行中
	public static String STATUS_OF_TASK_RETRYING = "retrying"; //task重试中
	public static String STATUS_OF_TASK_RETRYED = "retryed"; //task已结重试
	public static String STATUS_OF_TASK_COMPLETED = "completed"; //task已完成
	public static String STATUS_OF_TASK_FAILED = "failed";//task已失败
	public static String STATUS_OF_TASK_TIMEOUT = "timeout";//状态更新超时
	
	static String logName = "sync";
	private static Logger log = LogManager.getLogger(logName);
	
	private String templateJson = "/com/fnst/conf/tasksync-template.json";
	
	//openstack token
	public static volatile String osToken = "";
	
	public static volatile TokenOs tokenObject;
	
	//openstack token是否已经过期
	private static volatile boolean osTokenExpired = true;
	
	//默认task的访问api间隔(毫秒)
	public static int TASK_SYNC_INTERVAL = 5000;
	public static int TASK_SYNC_COUNT = 60;
	public static int TASK_RETRY_INTERVAL = 450000;
	
	private final int sleepTime = 1000;
	
	private int syncInterval = 0;
	private int syncCount = 0;
	private int retryInterval = 0;
	
	
	/**
	 * 初始化deamon线程
	 */
	public void initDeamon(){
		
		ObjectMapper mapper = new ObjectMapper();
		if (0 != syncInterval)
			TASK_SYNC_INTERVAL = syncInterval;
		if (0 != syncCount)
			TASK_SYNC_COUNT = syncCount;
		if (0 == retryInterval){
			TASK_RETRY_INTERVAL = (int) (TASK_SYNC_COUNT*TASK_SYNC_INTERVAL*1.5);
		}else{
			TASK_RETRY_INTERVAL = retryInterval;
		}
	    final int retryCount = TASK_RETRY_INTERVAL/sleepTime;
		
		log.info("status sync deamon初始化 -> { syncInterval:"+TASK_SYNC_INTERVAL +" , syncCount:"+TASK_SYNC_COUNT+" , retryInterval:"+TASK_RETRY_INTERVAL+" }");
		try {
			ThreadPool pool = ThreadPool.getInstance(poolPara);
			
			/*//读取配置文件
			String path = StatusSyncDeamon.class.getClassLoader().getResource(templateJson).getPath();
			this.template =mapper.readValue(readTxtFile(path), ArrayList.class);
			log.debug("读取sync task模板配置文件完成!");*/
			
			List<StatussyncTaskInfo> tasks = stDao.selectUnfinishedTasks();
			taskqueue.addAll(tasks);
			log.info("加载未结束的sync task完成! task size ->"+tasks.size());
            //启动一个监控线程
			Thread t = new Thread(new Runnable() {
				private StatussyncTaskInfo taskInfo = null;
				public void run() {
					int count = retryCount;
					while(true){
						count--;
						try {
							// 检查token是否过期
							if (isOsTokenExpired()) {
								setToken();
							}
							taskInfo = taskqueue.poll();
							if (null != taskInfo) {
								log.info("获取到一个task: task id -->" + taskInfo.getId());
								pool.getPoolExecutor()
										.execute(new StatusSyncTask(taskInfo, osToken, httpClientUtil, stDao));
							}
							// retrying任务的checkpoint
							if (0 == count) {
								List<StatussyncTaskInfo> tasks = stDao.selectRetryingTasks();
								taskqueue.addAll(tasks);
								count = retryCount;
								log.info("加载状态为retrying的sync task完成! task size ->"+tasks.size());
							}
							Thread.sleep(sleepTime);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			});
			t.setDaemon(true);
			t.start();
			log.info("启动监控线程完成!");
			
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	}
	

    /**
     * 设置标志位:token是否过期
     * @param osTokenExpired
     */
	public static synchronized void setOsTokenExpired(boolean expired) {
		osTokenExpired = expired;
	}


	public static  boolean isOsTokenExpired() {
		return osTokenExpired;
	}

	/**
	 * 获取openstack token
	 */
    public  void setToken(){
    	HttpClientForOsBase client = new HttpClientForOsBase(cloudConfig);
    	tokenObject=client.getToken();
		osToken=client.getToken().getTokenid();
		setOsTokenExpired(false);
    }


	public int getSyncInterval() {
		return syncInterval;
	}


	public void setSyncInterval(int syncInterval) {
		this.syncInterval = syncInterval;
	}


	public int getSyncCount() {
		return syncCount;
	}


	public void setSyncCount(int syncCount) {
		this.syncCount = syncCount;
	}


	public int getRetryInterval() {
		return retryInterval;
	}


	public void setRetryInterval(int retryInterval) {
		this.retryInterval = retryInterval;
	}

    
	/**
	 * 读取配置文件
	 * @param filePath
	 * @return
	 */
	/* private  String readTxtFile(String filePath){
		    InputStreamReader read = null;
		    String content = "";
		    String lineTxt = "";
	        try {
	                String encoding="utf-8";
	                File file=new File(filePath);
	                if(file.isFile() && file.exists()){ //判断文件是否存在
	                    read = new InputStreamReader(
	                    new FileInputStream(file),encoding);//考虑到编码格式
	                    BufferedReader bufferedReader = new BufferedReader(read);
	                    while((lineTxt = bufferedReader.readLine()) != null){
	                    	content += lineTxt;
	                    }
	                    bufferedReader.close();
	                    read.close();
	                   
	        }else{
	            log.error("配置文件不存在：文件-->"+templateJson);
	        }
	        } catch (Exception e) {
	        	log.error("读取配置文件出错：文件-->"+templateJson);
	            e.printStackTrace();
	        }
	        
	        return content;
	     
	    }*/
    
    

}
