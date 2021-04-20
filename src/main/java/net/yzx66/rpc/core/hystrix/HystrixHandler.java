package net.yzx66.rpc.core.hystrix;

import net.yzx66.rpc.constants.ConfigConstant;
import net.yzx66.rpc.constants.EmptyFallbackClass;
import net.yzx66.rpc.constants.ProxyConstant;
import net.yzx66.rpc.exception.HystrixException;
import net.yzx66.rpc.utils.ClassPathPropertiesUtil;
import net.yzx66.rpc.utils.StringUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.core.type.classreading.MetadataReader;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

public class HystrixHandler {

    static boolean isEnalbeHystrix;
    static {
        Properties properties = ClassPathPropertiesUtil.buildProperties("application.properties");
        if(properties != null){
            Object enable = properties.get(ConfigConstant.HYSTRIX_ENABLE);
            if(enable != null){
                isEnalbeHystrix = Boolean.parseBoolean((String)enable);
            }
        }
    }

    public static boolean isThisApiEnalbeHystrix(MetadataReader metadataReader){
        if(!isEnalbeHystrix){
            return false;
        }

        Map<String, Object> feginAtrr = metadataReader.getAnnotationMetadata()
                .getAnnotationAttributes(ProxyConstant.RPC_ANNOTATION_TYPE);
        Class<?> fallback = (Class<?>)feginAtrr.get("fallback");
        if(fallback == EmptyFallbackClass.class){
            return false;
        }

        return true;
    }

    public static Object handleHystrix(Method method , Object[] args , MetadataReader metadataReader
            , ApplicationContext applicationContext){
        Map<String, Object> feginAtrr = metadataReader.getAnnotationMetadata()
                .getAnnotationAttributes(ProxyConstant.RPC_ANNOTATION_TYPE);
        Class<?> fallback = (Class<?>)feginAtrr.get("fallback");

        Object bean = applicationContext.getBean(StringUtil.makeFirstLower(fallback.getSimpleName()));
        try{
            Class<?>[] classes = new Class[args.length];
            for(int i = 0 ; i < args.length ; i ++){
                classes[i] = args[i].getClass();
            }

            Method fallbackMethod = fallback.getDeclaredMethod(method.getName() , classes);
            fallbackMethod.setAccessible(true);
            return fallbackMethod.invoke(bean , args);
        }catch (Exception e){
            throw new HystrixException(e.getMessage());
        }
    }
}
