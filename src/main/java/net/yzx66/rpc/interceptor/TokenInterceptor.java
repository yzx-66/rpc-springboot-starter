package net.yzx66.rpc.interceptor;

import lombok.extern.slf4j.Slf4j;
import net.yzx66.rpc.constants.ConfigConstant;
import net.yzx66.rpc.utils.ClassPathPropertiesUtil;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Properties;

// 保存 token，然后传递给调用的服务
// 比在 postProcessBeforeInitize 里再代理一层优雅
@Slf4j
public class TokenInterceptor extends HandlerInterceptorAdapter {

    public static ThreadLocal<String> TOKEN_THREAD_LOCAL = new ThreadLocal<>();
    public static String tokenName;

    public TokenInterceptor(){
        Properties properties = ClassPathPropertiesUtil.buildProperties("application.properties");
        if(properties == null){
            return;
        }
        tokenName = (String)properties.get(ConfigConstant.TOKEN_NAME);

        log.info("======= 成功初始化 token 拦截器 =======");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Cookie[] cookies = request.getCookies();
        if(cookies != null){
            for(Cookie c : cookies){
                if(c.getName().equals(tokenName)){
                    TOKEN_THREAD_LOCAL.set(c.getValue());
                }
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        TOKEN_THREAD_LOCAL.remove();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        TOKEN_THREAD_LOCAL.remove();
    }
}
