package net.yzx66.rpc.proxy;

import net.yzx66.rpc.core.HttpConsumer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.type.classreading.MetadataReader;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class RpcProxyFactoryBean<T> implements InvocationHandler , FactoryBean<T> , ApplicationContextAware {

    // 传 MetadataReader 而不是只传个 interface
    // 因为通过 java 的反射无法获取到接口上的注解
    // 所以就没法拿到  @FeginClient 何 @RequestMapping 里的值
    private MetadataReader metadataReader;
    // 服务降级时要从容器中取（直接反射的话对 fallback 类中 @Autowire 注解会失效）
    // 实现 ApplicationContextAware 更优雅，不然强转 BeanDefitionRegistry 也可以
    private ApplicationContext applicationContext;
    private Class<?> rpcInterface;

    public RpcProxyFactoryBean(MetadataReader metadataReader) throws ClassNotFoundException {
        this.metadataReader = metadataReader;

        String interfaceName = metadataReader.getClassMetadata().getClassName();
        rpcInterface = Class.forName(interfaceName);
    }

    @Override
    public T getObject() throws Exception {
        return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{rpcInterface}, this);
    }

    @Override
    public Class<?> getObjectType() {
        return rpcInterface;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        HttpConsumer consumer = new HttpConsumer();
        return consumer.getRespon(method , args , metadataReader ,applicationContext);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
