package net.yzx66.rpc.core.resolver;

import net.yzx66.rpc.constants.ProxyConstant;
import net.yzx66.rpc.enums.RequestHttpType;
import net.yzx66.rpc.utils.UrlPathUtil;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class PathResolver {

    private static final Map<MetadataReader , String> ipCache = new HashMap<>();
    private static final Map<MetadataReader , String> localtionCahce = new HashMap<>();


    public static String getUrl(Method method, MetadataReader metadataReader, RequestHttpType type){
        String ip = getIp(metadataReader);
        String localtion = getLocaltion(metadataReader);
        String path = getPath(method , metadataReader ,type);

        String url = "http://" + UrlPathUtil.appendUrlPath(ip , localtion) + path;
        return url;
    }

    public static String getIp(MetadataReader metadataReader){
        if(ipCache.get(metadataReader) != null){
            return ipCache.get(metadataReader);
        }

        Map<String, Object> feginConfig = metadataReader.getAnnotationMetadata()
                .getAnnotationAttributes(ProxyConstant.RPC_ANNOTATION_TYPE);

        String ip = (String)feginConfig.get("value");

        ipCache.put(metadataReader , ip);
        return ip;
    }

    private static String getLocaltion(MetadataReader metadataReader){
        if(localtionCahce.get(metadataReader) != null){
            return localtionCahce.get(metadataReader);
        }

        Map<String, Object> feginConfig = metadataReader.getAnnotationMetadata()
                .getAnnotationAttributes(ProxyConstant.RPC_ANNOTATION_TYPE);

        String location = (String) feginConfig.get("localtion");

        localtionCahce.put(metadataReader , location);
        return location;
    }

    private static String getPath(Method method , MetadataReader metadataReader , RequestHttpType type){
        String path = "";
        Class<? extends Method> providerClass = method.getClass();

        // 1、解析接口上的路径
        Map<String, Object> requestMappingConfig = metadataReader.getAnnotationMetadata()
                .getAnnotationAttributes(RequestMapping.class.getName());

        if(requestMappingConfig != null && requestMappingConfig.get("value") != null){
            String append = ((String[])requestMappingConfig.get("value"))[0];
            path = UrlPathUtil.appendUrlPath(path , append);
        }

        // 2、解析方法上的路径
        switch (type){
            case GET:
                GetMapping methodGetMapping = method.getAnnotation(GetMapping.class);
                path = UrlPathUtil.appendUrlPath(path , methodGetMapping.value()[0]);
                break;
            case POST:
                PostMapping methodPostMapping = method.getAnnotation(PostMapping.class);
                path = UrlPathUtil.appendUrlPath(path , methodPostMapping.value()[0]);
                break;
            case PUT:
                PutMapping methodPutMapping = method.getAnnotation(PutMapping.class);
                path = UrlPathUtil.appendUrlPath(path , methodPutMapping.value()[0]);
                break;
            case DELETE:
                DeleteMapping methodDeleteMapping = method.getAnnotation(DeleteMapping.class);
                path = UrlPathUtil.appendUrlPath(path , methodDeleteMapping.value()[0]);
                break;
        }

        if(path.length() == 0){
            throw new RuntimeException("path 解析之后长度为零");
        }
        return path;
    }
}
