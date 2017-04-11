package com.cloud.cloudapi.util.http.pool;

import org.apache.http.impl.client.CloseableHttpClient;

/** 
* @author  wangw
* @create  2016年5月25日 上午11:07:11 
* 
*/
public interface IPoolingHttpManager {
	
	
    /**
     * 初始化连接池
     */
	public void initPool();
	
	
	/**
	 * 取得一个http连接对象
	 * @return
	 */
	public CloseableHttpClient getClient();
	
	/**
	 * 取得一个http连接对象,使用basic auth方法进行验证
	 * @return
	 */
	public CloseableHttpClient getClientWithCredential(String host, int port, String username, String password);
	
	
	/**
	 * 得到连接池中的剩余连接数
	 */
	public int getAvailableCount();
	
	
	/**
	 * 关闭连接池
	 */
	
	public void destoryPool();

}
