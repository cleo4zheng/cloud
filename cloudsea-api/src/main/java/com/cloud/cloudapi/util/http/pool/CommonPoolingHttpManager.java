package com.cloud.cloudapi.util.http.pool;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/** 
* @author  wangw
* @create  2016年5月25日 上午11:07:11 
* 
*/
public class CommonPoolingHttpManager implements IPoolingHttpManager {
	
	//连接池管理器
	private  PoolingHttpClientConnectionManager cm;
	
	//请求管理
	private  HttpRequestRetryHandler httpRequestRetryHandler;
	
	private Logger log = LogManager.getLogger(CommonPoolingHttpManager.httpLogName);

	//最大的连接数
	private int maxTotal;
	
	//每一个路由的最大连接数
	private int maxPerRoute;
	
	public static String httpLogName = "http_util";
	
	@Override
	public void initPool() {
		
		log.info("初始化http连接池开始: 启动参数 -> {maxTotal:"+maxTotal+", maxPerRoute:"+maxPerRoute+"}");
		//同时启用http和https连接
		ConnectionSocketFactory plainsf = PlainConnectionSocketFactory
                .getSocketFactory();
        LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory
                .getSocketFactory();
		Registry<ConnectionSocketFactory> registry = RegistryBuilder
                .<ConnectionSocketFactory> create().register("http", plainsf)
                .register("https", sslsf).build();
        cm = new PoolingHttpClientConnectionManager(
                registry);
        
        // 将最大连接数增加
        cm.setMaxTotal(maxTotal);
        // 将每个路由基础的连接增加
        cm.setDefaultMaxPerRoute(maxPerRoute);

        // 请求重试处理
        httpRequestRetryHandler = new HttpRequestRetryHandler() {
            @Override
			public boolean retryRequest(IOException exception,
                    int executionCount, HttpContext context) {
                if (executionCount >= 5) {// 如果已经重试了5次，就放弃
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
                    return true;
                }
                if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
                    return false;
                }
                if (exception instanceof InterruptedIOException) {// 超时
                    return false;
                }
                if (exception instanceof UnknownHostException) {// 目标服务器不可达
                    return false;
                }
                if (exception instanceof ConnectTimeoutException) {// 连接被拒绝
                    return false;
                }
                if (exception instanceof SSLException) {// SSL握手异常
                    return false;
                }

                HttpClientContext clientContext = HttpClientContext
                        .adapt(context);
                HttpRequest request = clientContext.getRequest();
                // 如果请求是幂等的，就再次尝试
                if (!(request instanceof HttpEntityEnclosingRequest)) {
                    return true;
                }
                return false;
            }
        };
        log.info("初始化http连接池结束!");
	}

	
	@Override
	public CloseableHttpClient getClient() {
		 CloseableHttpClient httpClient = HttpClients.custom()
	                .setConnectionManager(cm)
	                .setRetryHandler(httpRequestRetryHandler).build();
		 
	     return httpClient;
		
	}
	

	@Override
	public int getAvailableCount() {
		// TODO Auto-generated method stub
		return cm.getTotalStats().getAvailable();
	}

	@Override
	public void destoryPool(){
		cm.close();
	}
	
	
	//setter方法
	public void setMaxTotal(int maxTotal) {
		this.maxTotal = maxTotal;
	}
	public void setMaxPerRoute(int maxPerRoute) {
		this.maxPerRoute = maxPerRoute;
	}


	@Override
	public CloseableHttpClient getClientWithCredential(String host, int port, String username, String password) {
		// TODO Auto-generated method stub
		if(host==null || host.isEmpty()){
			host = AuthScope.ANY_HOST;
		}
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
                new AuthScope(host, port),
                new UsernamePasswordCredentials(username,password));
		
		CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .setRetryHandler(httpRequestRetryHandler).build();
	 
     return httpClient;
	}
	
	
	
}
