package net.yzx66.rpc.core.support;

import lombok.extern.slf4j.Slf4j;
import net.yzx66.rpc.constants.ConfigConstant;
import net.yzx66.rpc.utils.ClassPathPropertiesUtil;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PoolHttpClient {

    private static final HashMap<String , CloseableHttpClient> singonHttpClientCache = new HashMap<>();
    private final static Object syncLock = new Object(); // 相当于线程锁,用于线程安全

    private static int httpPoolMaxConnect = -1;
    private static int httpMaxIdleTime = -1;


    static {
        Properties properties = ClassPathPropertiesUtil.buildProperties("application.properties");
        Object mc = properties.get(ConfigConstant.MAX_CONNECT);
        if(mc != null){
            httpPoolMaxConnect = Integer.parseInt((String) mc);
        }

        Object it = properties.get(ConfigConstant.MAX_IDLETIME);
        if(it != null){
            httpMaxIdleTime = Integer.parseInt((String) it) * 1000;
        }
    }

    public static CloseableHttpClient getHttpClient(String ip){
        if(singonHttpClientCache.get(ip) != null){
            return singonHttpClientCache.get(ip);
        }

        String hostName = ip;
        int port = -1;
        if (ip.contains(":")){
            String[] args = ip.split(":");
            hostName = args[0];
            port = Integer.parseInt(args[1]);
        }

        CloseableHttpClient httpClient;
        //多线程下多个线程同时调用getHttpClient容易导致重复创建httpClient对象的问题,所以加上了同步锁
        synchronized (syncLock){
            // 双重锁判断
            if (singonHttpClientCache.get(ip) != null) {
                return singonHttpClientCache.get(ip);
            }

            httpClient = createHttpClient(hostName, port);
            singonHttpClientCache.put(hostName , httpClient);
        }

        return httpClient;
    }


    public static CloseableHttpClient createHttpClient(String host, int port){
        // 配置 socketFacotry
        ConnectionSocketFactory plainSocketFactory = PlainConnectionSocketFactory.getSocketFactory();
        LayeredConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactory.getSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create().register("http", plainSocketFactory)
                .register("https", sslSocketFactory).build();

        // 池化
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager(registry);

        //设置连接参数
        manager.setMaxTotal(httpPoolMaxConnect); // 最大连接数
        manager.setDefaultMaxPerRoute(httpPoolMaxConnect); // 单个路由最大连接数

//        // 配置 httpHost 对应的最大连接数
//        HttpHost httpHost = new HttpHost(host, port);
//        manager.setMaxPerRoute(new HttpRoute(httpHost), httpPoolMaxConnect);

        HttpRequestRetryHandler retryHandler = getHttpRequestRetryHandler();
        ConnectionKeepAliveStrategy keepAliveStrategy = getConnectionKeepAliveStrategy();

        CloseableHttpClient client = HttpClients.custom()
                .setConnectionManagerShared(true) // 否咋会报错  Connection pool shut down
                .setConnectionManager(manager)
                .setRetryHandler(retryHandler)
                .setKeepAliveStrategy(keepAliveStrategy)
                .build();

        //开启监控线程,对异常和空闲线程进行关闭
        ScheduledExecutorService monitorExecutor = Executors.newScheduledThreadPool(1);
        monitorExecutor.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                //关闭异常或者超过 keepalive-timeout 时间的连接
                manager.closeExpiredConnections();
                //关闭空闲的连接（防止有的有的 respone 把 time-out 设置的过长，导致浪费资源）
                // nginx 配置的默认 keepalive-timeout 是 75s
                manager.closeIdleConnections(httpMaxIdleTime, TimeUnit.MILLISECONDS);
                log.debug("close expired and idle for over 5s connection");
            }
        }, 0, httpMaxIdleTime, TimeUnit.MILLISECONDS);
        return client;
    }

    private static  HttpRequestRetryHandler getHttpRequestRetryHandler(){
        //请求失败时,进行请求重试
        return new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(IOException e, int i, HttpContext httpContext) {
                if (i > 3){
                    //重试超过3次,放弃请求
                    log.debug("retry has more than 3 time, give up request");
                    return false;
                }
                if (e instanceof NoHttpResponseException){
                    //服务器没有响应,可能是服务器断开了连接,应该重试
                    log.debug("receive no response from server, retry");
                    return true;
                }
                if (e instanceof SSLHandshakeException){
                    // SSL握手异常
                    log.debug("SSL hand shake exception");
                    return false;
                }
                if (e instanceof InterruptedIOException){
                    //超时
                    log.debug("InterruptedIOException");
                    return false;
                }
                if (e instanceof UnknownHostException){
                    // 服务器不可达
                    log.debug("server host unknown");
                    return false;
                }
                if (e instanceof ConnectTimeoutException){
                    // 连接超时
                    log.debug("Connection Time out");
                    return false;
                }
                if (e instanceof SSLException){
                    log.debug("SSLException");
                    return false;
                }

                HttpClientContext context = HttpClientContext.adapt(httpContext);
                HttpRequest request = context.getRequest();
                if (!(request instanceof HttpEntityEnclosingRequest)){
                    //如果请求不是关闭连接的请求
                    return true;
                }
                return false;
            }
        };
    }

    private static ConnectionKeepAliveStrategy getConnectionKeepAliveStrategy(){
         return new ConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                HeaderElementIterator it = new BasicHeaderElementIterator
                        (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                while (it.hasNext()) {
                    HeaderElement he = it.nextElement();
                    String param = he.getName();
                    String value = he.getValue();
                    if (value != null && param.equalsIgnoreCase
                            ("timeout")) {
                        return Long.parseLong(value) * 1000;
                    }
                }
                // 上面那些是自带的，直接 copy 过来
                // 原先是直接返回 -1.即一直保持连接

                return 60 * 1000;//如果 respone 没有约定，则默认定义时长为60s
            }
        };
    }


}
