package com.cloud.cloudapi.sync;
/** 
* @author  wangw
* @create  2016年8月22日 下午2:58:29 
* 
*/
public class ThreadPoolParameter {
	/** 默认核心线程数 */
    private static final int DEFAULT_CORE_POOL_SIZE = 10;
    
    /** 默认最大线程数 */
    private static final int DEFAULT_MAX_POOL_SIZE = Integer.MAX_VALUE;
    
    /** 默认最大超时时长 */
    private static final int DEFAULT_KEEP_ALIVE = 3000;
    
    /** 默认最大超时时长 */
    private static final int DEFAULT_QUEUE_DEPTH = 10;

    /** 核心线程数 */
    private int corePoolSize = DEFAULT_CORE_POOL_SIZE;

    /** 最大线程数 */
    private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;

    /** 最大超时时长 */
    private long keepAlive = DEFAULT_KEEP_ALIVE;

    /** 有界队列深度 */
    private int queeDeep = DEFAULT_QUEUE_DEPTH;

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public long getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(long keepAlive) {
        this.keepAlive = keepAlive;
    }

    public int getQueeDeep() {
        return queeDeep;
    }

    public void setQueeDeep(int queeDeep) {
        this.queeDeep = queeDeep;
    }

}
