package net.yzx66.rpc;


import lombok.extern.slf4j.Slf4j;
import net.yzx66.rpc.constants.ConfigConstant;
import net.yzx66.rpc.interceptor.WebConfigurer;
import net.yzx66.rpc.sacnner.PackageScanner;
import net.yzx66.rpc.utils.ClassPathPropertiesUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Properties;


@Configuration
@Import(WebConfigurer.class)
@Slf4j
// 原理是 invokePostProcess 方法中，在 configureClassPostProcessor 回调完后，
// 如果发现新的 BeanDefinitionRegistryPostProcessor 那么会接着调用
public class RpcAutoConfigure implements BeanDefinitionRegistryPostProcessor {

    // 这里不能注入，因为还没到 getBean 的实例化阶段，所以会 npe
//    @Autowired
//    RpcProperties rpcProperties;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Properties properties = ClassPathPropertiesUtil.buildProperties("application.properties");
        if(properties == null || properties.getProperty(ConfigConstant.SCAN_PACKAGE) == null){
            return;
        }

        String scanConfig = properties.getProperty(ConfigConstant.SCAN_PACKAGE);
        PackageScanner scanner = new PackageScanner();

        String[] scanPakcages =  scanConfig.split(",");
        for(String s : scanPakcages){
            log.info("======= 将为 " + s + " 包下的 net.yzx66.rpc 类生成代理对象 =======" );
            scanner.doScanner(s , registry);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
