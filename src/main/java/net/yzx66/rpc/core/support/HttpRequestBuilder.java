package net.yzx66.rpc.core.support;

import net.yzx66.rpc.constants.ConfigConstant;
import net.yzx66.rpc.core.resolver.ParamterResolver;
import net.yzx66.rpc.core.resolver.PathResolver;
import net.yzx66.rpc.enums.RequestContentType;
import net.yzx66.rpc.enums.RequestHttpType;
import net.yzx66.rpc.interceptor.TokenInterceptor;
import net.yzx66.rpc.utils.ClassPathPropertiesUtil;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.springframework.core.type.classreading.MetadataReader;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Properties;

public class HttpRequestBuilder {

    private static  int httpConnectTimeout = -1;
    private static int httpSocketTimeOut = -1;

    static {
        Properties properties = ClassPathPropertiesUtil.buildProperties("application.properties");
        Object ct = properties.get(ConfigConstant.CONNECT_TIMEOUT);
        if(ct != null){
            httpConnectTimeout = Integer.parseInt((String) ct) * 1000;
        }

        Object st = properties.get(ConfigConstant.SOCKET_TIMEOUT);
        if(st != null){
            httpSocketTimeOut =Integer.parseInt((String)st) * 1000;
        }
    }

    // 对于部分浏览器，比如 chrom 不支持 get 携带请求体
    // 所以对于 get 只要把参数拼接在 url 后面即可，不用设置 content-type
    // 而且 HttpGet 不支持请求体
    public static HttpGet buildGet(Method method, Object[] args, MetadataReader metadataReader) {
        HttpGet httpGet = new HttpGet();
        StringBuilder urlBuilder = new StringBuilder(PathResolver.getUrl(method, metadataReader, RequestHttpType.GET));

        doGetAndDeleteBuild(httpGet , method , args ,urlBuilder);
        return httpGet;
    }

    // delete 请求的 HttpDelete 也不支持请求体
    public static HttpDelete buildDelete(Method method, Object[] args, MetadataReader metadataReader){
        HttpDelete httpDelete = new HttpDelete();
        StringBuilder urlBuilder = new StringBuilder(PathResolver.getUrl(method, metadataReader, RequestHttpType.DELETE));

        doGetAndDeleteBuild(httpDelete , method , args , urlBuilder);
        return httpDelete;
    }

    public static HttpPost buildPost(Method method, Object[] args, MetadataReader metadataReader) throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost();
        StringBuilder urlBuilder = new StringBuilder(PathResolver.getUrl(method, metadataReader, RequestHttpType.POST));

        doPostAndPutBuild(httpPost , method , args , urlBuilder);
        return httpPost;
    }

    public static HttpPut buildPut(Method method, Object[] args, MetadataReader metadataReader) throws UnsupportedEncodingException {
        HttpPut httpPut = new HttpPut();
        StringBuilder urlBuilder = new StringBuilder(PathResolver.getUrl(method, metadataReader, RequestHttpType.PUT));

        doPostAndPutBuild(httpPut , method , args , urlBuilder);
        return httpPut;
    }

    private static void doGetAndDeleteBuild(HttpRequestBase requestBase , Method method ,
                                            Object[] args, StringBuilder urlBuilder){
        URI urI = ParamterResolver.getCompleteUrI(method, args, urlBuilder);
        requestBase.setURI(urI);

        processConfig(requestBase);
        processCookie(requestBase);
    }

    private static void doPostAndPutBuild(HttpEntityEnclosingRequestBase requestBase , Method method ,
                                Object[] args, StringBuilder urlBuilder) throws UnsupportedEncodingException {
        RequestContentType contentType = ParamterResolver.getContentType(method);

        if(contentType == RequestContentType.Form){
            requestBase.addHeader(HTTP.CONTENT_TYPE,contentType.getContentType());

            UrlEncodedFormEntity formEntity = ParamterResolver.getFormEntity(method, args, urlBuilder);
            requestBase.setEntity(formEntity);
        }else if(contentType == RequestContentType.JSON){
            requestBase.addHeader(HTTP.CONTENT_TYPE, contentType.getContentType());

            StringEntity stringEntity = ParamterResolver.getJsonStringEntity(method, args, urlBuilder);
            requestBase.setEntity(stringEntity);
        }

        requestBase.setURI(URI.create(urlBuilder.toString()));

        processConfig(requestBase);
        processCookie(requestBase);
    }

    private static void processConfig(HttpRequestBase requestBase) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(httpConnectTimeout) // 连接超时
                .setConnectTimeout(httpConnectTimeout)
                .setSocketTimeout(httpSocketTimeOut) // 通信超时
                .build();

        requestBase.setConfig(requestConfig);
    }


    private static void processCookie(HttpRequestBase requestBase){
        // 添加 token
        if(ParamterResolver.getToken() != null){
            String token = TokenInterceptor.tokenName + "=" + ParamterResolver.getToken();
            requestBase.addHeader("cookie" , token);
        }
    }

}
