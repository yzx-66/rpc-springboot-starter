package net.yzx66.rpc.core.resolver;

import net.yzx66.rpc.converter.toJson.Object2JsonConverter;
import net.yzx66.rpc.exception.RpcResponParseException;
import net.yzx66.rpc.exception.RpcReturnException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import java.lang.reflect.Method;

public class ResponResolver {

    public static Object resloveRespone(Method method , CloseableHttpResponse httpResponse)  {
        int returnCode = httpResponse.getStatusLine().getStatusCode();
        if(returnCode / 100 != 2){
            String error = "net.yzx66.rpc 请求失败 \n 状态码：" + returnCode + "\n 错误原因：" + httpResponse.getStatusLine().getReasonPhrase();
            throw new RpcReturnException(error);
        }

        try{
            String response = EntityUtils.toString(httpResponse.getEntity());
            return Object2JsonConverter.json2Ojbect( response, method.getReturnType());
        }catch (Exception e){
            throw new RpcResponParseException(e.getMessage());
        }
    }
}
