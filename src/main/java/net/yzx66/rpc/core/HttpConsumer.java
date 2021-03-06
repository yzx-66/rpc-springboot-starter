package net.yzx66.rpc.core;

import lombok.extern.slf4j.Slf4j;
import net.yzx66.rpc.constants.ConfigConstant;
import net.yzx66.rpc.core.hystrix.HystrixHandler;
import net.yzx66.rpc.core.resolver.PathResolver;
import net.yzx66.rpc.core.resolver.ResponResolver;
import net.yzx66.rpc.core.support.HttpRequestBuilder;
import net.yzx66.rpc.core.support.PoolHttpClient;
import net.yzx66.rpc.exception.RpcCallException;
import net.yzx66.rpc.exception.RpcReturnException;
import net.yzx66.rpc.utils.ClassPathPropertiesUtil;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.ApplicationContext;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Properties;

@Slf4j
public class HttpConsumer {

    static boolean isKeppalive;

    static {
        Properties properties = ClassPathPropertiesUtil.buildProperties("application.properties");
        if(! (properties == null) && ! (properties.get(ConfigConstant.KEEP_ALIVE) == null)){
            String ka = (String) properties.get(ConfigConstant.KEEP_ALIVE);
            isKeppalive = Boolean.parseBoolean(ka);
        }
    }

    public Object getRespon(Method method, Object[] args , MetadataReader metadataReader , ApplicationContext applicationContext) throws IOException {
        CloseableHttpClient httpclient = null;
        if(isKeppalive){
            httpclient = PoolHttpClient.getHttpClient(PathResolver.getIp(metadataReader));
        }else {
            httpclient = HttpClients.createDefault();
        }

        CloseableHttpResponse httpResponse = null;
        try{
            httpResponse = resolveAndExcute(httpclient , method, args , metadataReader);
            return ResponResolver.resloveRespone(method ,httpResponse);
        }catch (RpcReturnException rpcEx){
            if(! HystrixHandler.isThisApiEnalbeHystrix(metadataReader)){
                throw rpcEx;
            }
            // ????????????
            return HystrixHandler.handleHystrix(method , args , metadataReader ,applicationContext);
        }finally {
            if (httpResponse != null) {
                httpResponse.close();
            }
            //????????????????????????
            // httpclients 4.5.x ?????????????????? ClosableHttpResponse.close() ????????????????????????????????????,?????????????????????,
            // ????????????????????????????????????????????????????????????????????????
            httpclient.close();
        }
    }

    private CloseableHttpResponse resolveAndExcute(CloseableHttpClient httpclient ,
                                                   Method method , Object[] args , MetadataReader metadataReader)  {
        Annotation[] annotations = method.getDeclaredAnnotations();
        try{
            for(Annotation a : annotations){
                if(a.annotationType() == GetMapping.class){
                    HttpGet httpGet = HttpRequestBuilder.buildGet(method, args , metadataReader);
                    log.debug("?????????????????????GET " + httpGet.getURI().toString() );
                    return httpclient.execute(httpGet);
                }else if(a.annotationType() == PostMapping.class){
                    HttpPost httpPost = HttpRequestBuilder.buildPost(method, args, metadataReader );
                    log.debug("?????????????????????POST " + httpPost.getURI().toString() );
                    return httpclient.execute(httpPost);
                }else if(a.annotationType() == PutMapping.class){
                    HttpPut httpPut = HttpRequestBuilder.buildPut(method, args, metadataReader);
                    log.debug("?????????????????????PUT " + httpPut.getURI().toString() );
                    return httpclient.execute(httpPut);
                }else if(a.annotationType() == DeleteMapping.class){
                    HttpDelete httpDelete = HttpRequestBuilder.buildDelete(method, args, metadataReader);
                    log.debug("?????????????????????DELETE " + httpDelete.getURI().toString() );
                    return httpclient.execute(httpDelete);
                }
            }
        }catch (IOException e){
            throw new RpcReturnException(e.getMessage());
        }

        throw new RpcCallException(" net.yzx66.rpc ????????? restful ????????? " +
                "@GetMapping???@PostMapping???@PutMapping???@DeleteMapping");
    }
}
