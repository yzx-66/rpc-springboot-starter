package net.yzx66.rpc.core.resolver;

import net.yzx66.rpc.converter.toJson.Object2JsonConverter;
import net.yzx66.rpc.converter.toString.Date2StringConverter;
import net.yzx66.rpc.converter.toString.ToStringConverter;
import net.yzx66.rpc.enums.RequestContentType;
import net.yzx66.rpc.interceptor.TokenInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;

/**
 *  1、已经规定了，在 api 的方法里，其入参不可以写 HttpServletRequest 或者 HttpSerlvetRespone
 *     所以不用在解析 interface 模块的 api 包里接口时，考虑方法的参数有 request 或者 respone 参数
 *
 *     如果方法的入参真的需要 request 或者 respone 对象，那么可以写在 controller 对应方法的参数里
 *
 *  2、参数里面不存在当 contentType 是 formEntity 时，里面某个属性是 json 的形式，同时还用 @RequestBody 注解了
 *     即 contentType 是 formEntity 时，允许某个参数值是 json 字符串，但是不可以在入参用 @RequestBody 注解
 */
public class ParamterResolver {

    private static final Map<Class<?> , ToStringConverter> converters = new HashMap<>();

    // 初始化 conveters
    static {
        converters.put(Date.class , new Date2StringConverter());
    }

    public static RequestContentType getContentType(Method method){
        if(method.getParameterCount() == 0){
            return RequestContentType.None;
        }

        if(method.getParameterCount() == 1){
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            for(Annotation a : parameterAnnotations[0]){
                if(a.annotationType() == RequestBody.class){
                    return RequestContentType.JSON;
                }
                if(a.annotationType() == RequestParam.class || a.annotationType() == PathVariable.class){
                    return RequestContentType.Form;
                }
            }
        }

        if(method.getParameterCount() == 2){
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            boolean hasRequestBody = false , hasPathVariable = false;
            for(int i = 0 ; i < parameterAnnotations.length ; i ++){
                for(int j = 0 ; j < parameterAnnotations[i].length ; j ++){
                    if(parameterAnnotations[i][j].annotationType() == RequestBody.class){
                        hasRequestBody = true;
                    }
                    if(parameterAnnotations[i][j].annotationType() ==  PathVariable.class){
                        hasPathVariable = true;
                    }
                }
            }
            if(hasPathVariable && hasRequestBody){
                return RequestContentType.JSON;
            }
        }

        // 多于一个参数，那么肯定不可能参数 json 的请求体
        return RequestContentType.Form;
    }

    /**
     * 对于 get 和 delete，httpClient 不支持请求体，所以只能是把参数解析在 uri 上
     */
    public static URI getCompleteUrI(Method method , Object[] args , StringBuilder urlBuilder) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Map<String , String> paramters = new HashMap<>();

        for(int i = 0 ; i < parameterAnnotations.length ; i ++){
            for(int j = 0 ; j < parameterAnnotations[i].length ; j ++){
                if(parameterAnnotations[i][j] instanceof RequestParam){
                    String propertyName = ((RequestParam) parameterAnnotations[i][j]).name();
                    if(propertyName.equals("")){
                        propertyName = ((RequestParam) parameterAnnotations[i][j]).value();
                    }

                    if(args[i] == null){
                        continue;
                    }

                    ToStringConverter conveter = getConveter(args[i].getClass());
                    if(conveter != null){
                        paramters.put(propertyName, conveter.convert(args[i]));
                    }else {
                        // 没有 convter 就对参数值直接 toString 即可
                        paramters.put(propertyName,args[i].toString());
                    }
                }

                if(parameterAnnotations[i][j] instanceof PathVariable){
                    urlBuilder.delete(urlBuilder.lastIndexOf("/"),urlBuilder.length());
                    urlBuilder.append("/").append(args[i].toString());
                }
            }
        }

        int idx = 0;
        for(Map.Entry<String,String> e : paramters.entrySet()){
            if(idx == 0){
                urlBuilder.append("?").append(e.getKey()).append("=").append(e.getValue());
            }else{
                urlBuilder.append("&").append(e.getKey()).append("=").append(e.getValue());
            }
            idx ++;
        }

        return URI.create(urlBuilder.toString());
    }

    /**
     * urlBuilder：因为参数可能有 PathVariable 注解，因此路径还要拼接
     */
    public static UrlEncodedFormEntity getFormEntity(Method method , Object[] args , StringBuilder urlBuilder) throws UnsupportedEncodingException {
        Object[] res = new Object[2];

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        // 创建参数队列
        List<NameValuePair> formparams = new ArrayList<>();

        for(int i = 0 ; i < parameterAnnotations.length ; i ++){
            for(int j = 0 ; j < parameterAnnotations[i].length ; j ++){
                if(parameterAnnotations[i][j] instanceof RequestParam){
                    String propertyName = ((RequestParam) parameterAnnotations[i][j]).name();
                    if(propertyName.equals("")){
                        propertyName = ((RequestParam) parameterAnnotations[i][j]).value();
                    }

                    ToStringConverter conveter = getConveter(args[i].getClass());
                    if(conveter != null){
                        formparams.add(new BasicNameValuePair(propertyName, conveter.convert(args[i])));
                    }else {
                        // 没有 convter 就对参数值直接 toString 即可
                        formparams.add(new BasicNameValuePair(propertyName, args[i].toString()));
                    }
                }

                if(parameterAnnotations[i][j] instanceof PathVariable){
                    urlBuilder.delete(urlBuilder.lastIndexOf("/"),urlBuilder.length());
                    urlBuilder.append("/").append(args[i].toString());
                }
            }
        }

        //参数转码
        return new UrlEncodedFormEntity(formparams, "UTF-8");
    }

    public static StringEntity getJsonStringEntity(Method method ,Object[] args , StringBuilder urlBuilder) throws UnsupportedEncodingException {
        String json = null;
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        // i < 2 (即可能存在一个 PathVariable）
        for(int i = 0 ; i < parameterAnnotations.length ; i ++){
            for(int j = 0 ; j < parameterAnnotations[i].length ; j ++){
                if(parameterAnnotations[i][j] instanceof RequestBody){
                    json = Object2JsonConverter.object2Json(args[i]);
                }
                if(parameterAnnotations[i][j] instanceof PathVariable){
                    urlBuilder.delete(urlBuilder.lastIndexOf("/"),urlBuilder.length());
                    urlBuilder.append("/").append(args[i].toString());
                }
            }
        }

        return new StringEntity(json, "UTF-8");
    }


    public static String getToken(){
        return TokenInterceptor.TOKEN_THREAD_LOCAL.get();
    }

    private static ToStringConverter getConveter(Class parameterClass){
        return converters.get(parameterClass);
    }
}
