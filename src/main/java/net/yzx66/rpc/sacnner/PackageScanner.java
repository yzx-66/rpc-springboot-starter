package net.yzx66.rpc.sacnner;

import lombok.extern.slf4j.Slf4j;
import net.yzx66.rpc.constants.ProxyConstant;
import net.yzx66.rpc.proxy.RpcProxyFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import java.io.File;
import java.net.URL;
import java.util.Set;

@Slf4j
public class PackageScanner {

    MetadataReaderFactory metadataReaderFactory =  new CachingMetadataReaderFactory();

    public void doScanner(String scanPackage , BeanDefinitionRegistry registry) {
        // 1.拿到 package 的 url
        URL url = this.getClass().getClassLoader().
                getResource(scanPackage.replaceAll("\\.", "/"));
        // 通过getFile获取到要扫描包的File对象（文件夹）
        File classpath = new File(url.getFile());

        // 2.遍历文件夹，寻找class文件
        for (File file : classpath.listFiles()) {
            if (file.isDirectory()) {
                // 这里是通过递归遍历文件夹，还是包就再执行上述步骤（解析路径->创建目录->遍历）
                doScanner(scanPackage + "." + file.getName() , registry);
            } else {
                // 不是class文件的不管
                if (!file.getName().endsWith("class")) {continue;}

                try{
                    Resource resource = new UrlResource(file.toURL());
                    registerBean(resource  , registry);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    // 仿 mybatis 的 classPathMapperScaner
    private void registerBean(Resource resource , BeanDefinitionRegistry registry) throws Exception {
        MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
        if(!isCandidateComponent(metadataReader)){
            return;
        }

        ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
        sbd.setResource(resource);
        sbd.setSource(resource);

        processBeanDefinition(sbd , metadataReader);

        Class<?> rpcInterface = Class.forName(metadataReader.getClassMetadata().getClassName());
        registry.registerBeanDefinition(rpcInterface.getName() , sbd);
        log.info("======= 成功注册 net.yzx66.rpc 代理类：" + rpcInterface.getName() +" =======");
    }

    private boolean isCandidateComponent(MetadataReader metadataReader){
        AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
        Set<String> annotationTypes = annotationMetadata.getAnnotationTypes();
        for(String types : annotationTypes){
            if(types.equals(ProxyConstant.RPC_ANNOTATION_TYPE)){
                return true;
            }
        }
        return false;
    }

    private void processBeanDefinition(BeanDefinition definition , MetadataReader metadataReader)  {
        definition.setBeanClassName(RpcProxyFactoryBean.class.getName());
        definition.getConstructorArgumentValues().addGenericArgumentValue(metadataReader);
    }
}
