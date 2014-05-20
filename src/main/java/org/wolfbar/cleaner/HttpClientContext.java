package org.wolfbar.cleaner;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.wolfbar.cleaner.http.HashCookieStore;

public class HttpClientContext
{
	public final HttpClient client;
	public final HashCookieStore cookieStore;
	public final PoolingHttpClientConnectionManager cxMgr;
	
	public final ThreadPoolExecutor executor;
	
	
	public HttpClientContext
	(int maxTotalConnections, int maxPerRouterConnections, int threads, int socketTimeoutTime, int connectTimeoutTime, int bufferSizeKb)
	{
		cxMgr = new PoolingHttpClientConnectionManager();
		cxMgr.setMaxTotal(maxTotalConnections);
		cxMgr.setDefaultMaxPerRoute(maxPerRouterConnections);
		
		cookieStore = new HashCookieStore();
		
		SocketConfig socketConfig = SocketConfig.custom()
				.setSoKeepAlive(true)
				.setSoTimeout(socketTimeoutTime)
				.setTcpNoDelay(true)
				.build();
		
		ConnectionConfig connectionConfig = ConnectionConfig.custom()
				.setBufferSize(1024 * bufferSizeKb)
				.build();
		
		RequestConfig requestConfig = RequestConfig.custom()
				.setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY)
				.setSocketTimeout(socketTimeoutTime)
				.setConnectTimeout(connectTimeoutTime)
				.build();
		
		client = HttpClientBuilder.create()
				.setDefaultSocketConfig(socketConfig)
				.setDefaultConnectionConfig(connectionConfig)
				.setDefaultRequestConfig(requestConfig)
				.setDefaultCookieStore(cookieStore)
				.build();
		
		executor = new ThreadPoolExecutor(threads, threads, 1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
	}
}
