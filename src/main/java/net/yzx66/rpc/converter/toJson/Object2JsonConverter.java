package net.yzx66.rpc.converter.toJson;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import net.yzx66.rpc.constants.ConfigConstant;
import net.yzx66.rpc.exception.RpcResponParseException;
import net.yzx66.rpc.utils.ClassPathPropertiesUtil;
import net.yzx66.rpc.utils.StringUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;

public class Object2JsonConverter {

    private static Class<?> API_RESPONE_TYPE;
    private static String API_RESPONE_DATA_FILEDNAME;

    static {
        Properties properties = ClassPathPropertiesUtil.buildProperties("application.properties");
        if(properties != null){
            Object type = properties.get(ConfigConstant.API_RESPONE_TYPE);
            if(type != null){
                try {
                    API_RESPONE_TYPE = Class.forName((String)type);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            Object dataFiled = properties.get(ConfigConstant.API_RESPONE_DATA_FILENAME);
            if(dataFiled != null){
                API_RESPONE_DATA_FILEDNAME = StringUtil.makeFirstCapital((String)dataFiled);
            }
        }
    }

    public static String object2Json(Object obj){
        return JSONObject.toJSONString(obj);
    }

    public static<T> T json2Ojbect(String json , Class<T> clazz){
        return parseJson(json , clazz);
    }

    public static<T> T json2Ojbect(InputStream jsonStream , Class<T> clazz) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        int pos;
        while((pos = jsonStream.read(buffer)) != -1){
            outputStream.write(buffer , 0 ,pos);
        }

        String json = outputStream.toString();
        outputStream.close();
        return parseJson(json , clazz);
    }

    private static<T> T parseJson(String json , Class<T> targetClass){
        if(API_RESPONE_TYPE == null){
            return JSONObject.parseObject(json , targetClass);
        }
        if(API_RESPONE_DATA_FILEDNAME == null){
            throw new RpcResponParseException("只指定了 " + ConfigConstant.API_RESPONE_TYPE
                    + "，但未指定 " + ConfigConstant.API_RESPONE_DATA_FILENAME);
        }

        Object apiRespon = JSONObject.parseObject(json, API_RESPONE_TYPE);
        try {
            Method getMethod = API_RESPONE_TYPE.getMethod("get" + API_RESPONE_DATA_FILEDNAME);
            getMethod.setAccessible(true);
            // 嵌套对象都是 JSONObject 类型，还要再转换一次
            JSONObject res = (JSONObject)getMethod.invoke(apiRespon);
            return JSON.toJavaObject(res, targetClass);
        } catch (NoSuchMethodException e) {
            throw new RpcResponParseException("没有对应 " + API_RESPONE_DATA_FILEDNAME + " 的 get 方法" );
        }catch (Exception e){
            throw new RpcResponParseException(e.getMessage());
        }
    }
}
