package net.yzx66.rpc.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

public class ClassPathPropertiesUtil {

    private static final HashMap<String  , Properties> propertiesCache = new HashMap<>();

    public static Properties buildProperties(String propertieFileName){
        if(propertiesCache.get(propertieFileName) != null){
            return propertiesCache.get(propertieFileName);
        }

        ClassLoader classLoader = ClassPathPropertiesUtil.class.getClassLoader();
        InputStream stream = classLoader.getResourceAsStream(propertieFileName);

        if(stream == null){
            return null;
        }
        Properties properties = new Properties();
        try {
            properties.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("读取配置文件" + propertieFileName + "失败！");
        }

        propertiesCache.put(propertieFileName , properties);
        return properties;
    }
}
