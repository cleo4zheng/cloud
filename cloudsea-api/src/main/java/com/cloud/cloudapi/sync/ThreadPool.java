package com.cloud.cloudapi.sync;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ArrayBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** 
* @author  wangw
* @create  2016年8月22日 下午2:56:58 
* 
*/
public class ThreadPool {
    private static Logger logger = LoggerFactory.getLogger(ThreadPool.class);

    /** 单例 */
    private static ThreadPool threadPool;

    /** 线程池执行器 */
    private ThreadPoolExecutor poolExecutor;

    private static AtomicInteger threadCount = new AtomicInteger(0);

    /**
     * 获取线程池对象
     * @param parameter 线程池执行器参数对象
     * @return ThreadPool 线程池对象
     */
    public synchronized static ThreadPool getInstance(ThreadPoolParameter parameter) {
        if (null == threadPool) {
            threadPool = new ThreadPool(parameter);
        }
        return threadPool;
    }
    
    /**
     * 获取线程池对象
     * @param parameter 线程池执行器参数对象
     * @return ThreadPool 线程池对象
     */
    public synchronized static ThreadPool getInstance() {
        if (null == threadPool) {
            threadPool = new ThreadPool(new ThreadPoolParameter());
        }
        return threadPool;
    }

    /**
     * 私有构造函数
     * @param parameter 线程池执行器参数对象
     */
    private ThreadPool(ThreadPoolParameter parameter) {
        poolExecutor = new ThreadPoolExecutor(parameter.getCorePoolSize(), parameter.getMaxPoolSize(),
                parameter.getKeepAlive(), TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(
                        parameter.getQueeDeep()), new ThreadProduceFactory(), new ThreadPoolExecutor.CallerRunsPolicy());

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (null != poolExecutor) {
                    poolExecutor.shutdown();
                    logger.info("poolExecutor sync-status has been shutdown!");
                }
            }
        });
    }

    public ThreadPoolExecutor getPoolExecutor() {
        return this.poolExecutor;
    }

    /**
     * 线程工厂
     * 
     */
    private class ThreadProduceFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            threadCount.incrementAndGet();
            return new Thread(r, "sync-status" + threadCount);
        }

    }

}

