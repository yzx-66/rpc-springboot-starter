package net.yzx66.rpc.annotation;

import net.yzx66.rpc.constants.EmptyFallbackClass;

// 可以不用加 @Retention(RetentionPolicy.RUNTIME)
// 因为和接口上的 @RequestMapping 注解都是用 Spring 的 metaReader 从 class 文件中读的
public @interface FeginClient {
    // ip 地址
    String value();

    // nginx 的 localtion
    // 如果没有使用 nginx 则不用配置这个参数
    // 如果配置了这个参数，那么必须在 nginx.conf 中 rewrite 过这个 localtion
    String localtion() default "";

    Class<?> fallback() default EmptyFallbackClass.class;
}


