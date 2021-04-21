# rpc-springboot-starter

基于 Http 的轻量 RPC 框架，具备迁移到 SpringCloud 的能力！

# 安装

```
mvn install
```

# 依赖

```
<dependency>
  <groupId>net.yzx66</groupId>
  <artifactId>rpc-springboot-starter</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```


# 可选配置说明
* 直接配置在 application.properties

远程代理类相关（必选）：

```
# 要生成代理对象的包名
rpc.remote.package=
例如：rpc.remote.package=net.yzx66.remote
```

返回值解析（可选，如果不配置，那么就是代理类该方法返回类型的对象）

```
# 远程接口 controller 返回的 respone 类型
rpc.respone.type=
例如：rpc.respone.type=net.yzx66.commen.ApiRespone

# 返回的 repone 类型里哪个属性是需要的数据（必须和上面那个属性一起配置）
rpc.respone.data_filed=
例如：rpc.respone.data_filed=data
```

服务降级（可选）

```
# 是否开启服务降级
rpc.hystrix.enable=
例如：rpc.hystrix.enable=true
```


自动携带 token（可选）

```
# 访问远程服务时自动携带的 cookie 名称
# 因为没有统一网关，所以要让远程服务鉴权，还必须通过访问该服务时的 token
rpc.token.name=
例如：rpc.token.name=token
```

网络参数（可选）

```
# 是否开启 keep-alive
rpc.http.keepalive=
例如：rpc.http.keepalive=true

# 如果开启 keep-alive，那么一个 ip 地址的 http 连接池最大数
rpc.http.max_connect=
例如：rpc.http.max_connect=1000

# 如果开启 keep-alive，那么连接池中每个连接最大空闲时间，单位（s）
rpc.http.max_idletime=
例如：rpc.http.max_idletime=45

# http 连接超时时间，单位（s）
rpc.http.connect_timeout=
例如：rpc.http.connect_timeout=3

# http 通信超时时间，单位（s）
#rpc.http.socket_timeout
#例如：rpc.http.socket_timeout=3
```

# 使用方法

* 假设服务 B 要调用服务 A

## Server A

现在服务 A 有该 Controller

```java
@Controller
@RequestMapping("t")
public class TestController {

    // 测试解析 @PathVariable 是否生效
    //@GetMapping("/{id}")，因为与  @GetMapping("/{id1}") 冲突
    @ResponseBody
    ApiRespone testPathVariable(@PathVariable Integer id){
        System.out.println("TestController：" + id);
        return ApiRespone.ok(new TestEntity("1","1"));
    }
    
    // 测试解析多个 @RequestParam 是否生效
    @GetMapping("/ids")
    @ResponseBody
    ApiRespone testParamters(@RequestParam("id1") Integer id1 , @RequestParam("id2") Integer id2){
        System.out.println("TestController：" + id1 + " " + id2);
        return ApiRespone.ok(new TestEntity("1","1"));
    }
  
    // 测试解析 @PathVariable 与多个 @RequestParam 是否生效
    @GetMapping("/{id1}")
    @ResponseBody
    ApiRespone testPathAndParams(@PathVariable Integer id1 , @RequestParam("id2") Integer id2 , @RequestParam("id3") Integer id3){
        System.out.println("TestController：" + id1 + " " + id2 + " " + id3);
        return ApiRespone.ok(new TestEntity("1","1"));
    }

    // 测试解析 @RequestBody 是否生效
    @PostMapping("/body")
    @ResponseBody
    ApiRespone testBody(@RequestBody TestEntity testEntity){
        System.out.println("TestController：" + testEntity.toString());
        return ApiRespone.ok(new TestEntity("1","1"));
    }

    // 测试解析 @PathVariable 与 @RequestBody 是否生效
    @PostMapping("/{s}")
    @ResponseBody
    ApiRespone testPathAndBody(@PathVariable String s ,@RequestBody TestEntity testEntity){
        System.out.println("TestController：" + s + " " + testEntity.toString());
        return ApiRespone.ok(new TestEntity("1","1"));
    }

    // 测试不带参数是否生效
    @GetMapping("/parse")
    @ResponseBody
    ApiRespone testParseApiRespone(){
        System.out.println("访问这台！");
        return ApiRespone.ok(new TestEntity("1","1"));
    }
}
```

## Server B

### 1、配置与使用

```java
package net.yzx66.remote.client;

// 点对点的方式
@FeginClient(value = "127.0.0.1：8080")
@RequestMapping("t")
public interface TestClient {

    //@GetMapping("/{id}")
    @ResponseBody
    TestEntity testPathVariable(@PathVariable Integer id);

    @GetMapping("/ids")
    @ResponseBody
    TestEntity testParamters(@RequestParam("id1") Integer id1, @RequestParam("id2") Integer id2);

    @GetMapping("/{id1}")
    @ResponseBody
    TestEntity testPathAndParams(@PathVariable Integer id1, @RequestParam("id2") Integer id2, @RequestParam("id3") Integer id3);

    @PostMapping("/body")
    @ResponseBody
    TestEntity testBody(@RequestBody TestEntity testEntity);

    @PostMapping("/{s}")
    @ResponseBody
    TestEntity testPathAndBody(@PathVariable String s, @RequestBody TestEntity testEntity);

    @GetMapping("/parse")
    @ResponseBody
    TestEntity testParseApiRespone();
}
```

配置

```
# 要生成代理对象的包名（可以递归解析，所以不一定要指定到直接的一层）
rpc.remote.package=net.yzx66.remote

# 接口 controller 返回的 respone 类型
rpc.respone.type=net.yzx66.commen.ApiRespone
# 返回的 repone 类型里哪个属性是数据
rpc.respone.data_filed=data
```

说明：

* 1、@FeginClient 还可以配置成 nginx 地址（即使用 nginx 代理 serverA 的集群），并指定 localtion（因为 nginx 里一般会把 localtion rewrite 掉，所以这里的 localtion 只起 nginx 里的路由作用），如 `@FeginClient(value = "127.0.0.1" , localtion = "serverA")`，然后 nginx 的 /serverA 里进行 rewrite ` rewrite ^/serverA/(.*)$ /$1 break;`
* 2、参数 @RequestParam 必须指定参数名，因为没有使用字节码类库去读取参数名
* 3、controller 的返回是 ApiRespon，但这里可以指定成 TestEntity 是因为配置文件指定了 ApiRespon 的类型，并且指明了哪个字段是 TestEntity 类型，如果没有在配置文件指定，那么反序列化会报错

### 2、注入代理类

```java
@Component
public class TestRemote {

    @Autowired
    TestClient testClient;
    
    public void testClient(){
        TestEntity testEntity = testClient.testParseApiRespone();
        assert testEntity.getName().equals("1") && testEntity.getId().equals("1"); 
    }
}
```

### 服务降级

1、fallback 实现类

* 说明：fallback 的类要加上 @Component 注入容器，不然 fallback 的类里面无法使用 @Autowire

```java
@Component
public class TestApiFallback {

    public TestEntity testPathVariable(Integer id) {
        return new TestEntity("fallback" , "fallback");
    }
    public TestEntity testParamters(Integer id1 , Integer id2){
        return new TestEntity("fallback" , "fallback");
    }
    public TestEntity testPathAndParams(Integer id1 , Integer id2 , Integer id3){
        return new TestEntity("fallback" , "fallback");
    }
    public TestEntity testBody(TestEntity testEntity){
        return new TestEntity("fallback" , "fallback");
    }
    public TestEntity testPathAndBody(String s , TestEntity testEntity){
        return new TestEntity("fallback" , "fallback");
    }
    public TestEntity testParseApiRespone(){
        return new TestEntity("fallback" , "fallback");
    }
}
```

2、@FeginClient 指定 fallback 的类

```java
package net.yzx66.park.remote.client;

// 指定 fallback 对应的 class
@FeginClient(value = "127.0.0.1" , localtion = "serverA" ,fallback = TestFallback.class)
@RequestMapping("t")
public interface TestClient {
   ...
}
```

3、开启 fallback

```
# 是否开启服务降级
rpc.hystrix.enable=true
```

测试：关闭 serverA

```java
@Autowired
TestClient testClient;

public void testClient(){
     TestEntity testEntity = testClient.testParseApiRespone();
     assert testEntity.getName().equals("fallback") && testEntity.getId().equals("fallback"); 
}
```

### 自动携带 token

配置中指明 token 对应的 cookieName 即可

```
# 要自动携带的 cookie 名称
rpc.token.name=token
```

# 性能测试

* 测试环境:Jmeter，1000 个线程，10s，循环 10 次

直接调用 serverA
![image](https://user-images.githubusercontent.com/47884693/115391759-67d42700-a212-11eb-9888-24b0004f5f34.png)

启动两台 serverA 作为集群，然后在 nginx 按照轮询方式配置负载均衡，并且开启 nginx 的 keepalive，然后按照如下网络参数对 B 进行配置

```
# 是否开启 keep-alive
rpc.http.keepalive=true
# 一个 ip 地址的 http 连接池最大数
rpc.http.max_connect=1000
# 连接池每个连接最大空闲时间
rpc.http.max_idletime=45
# 连接超时时间 
rpc.http.connect_timeout=3
```

![image](https://user-images.githubusercontent.com/47884693/115392127-cc8f8180-a212-11eb-9c88-aa7a37f1e21a.png)

结论：

* 吞吐量与直接调用相同，平均延时在预热在建立连接池之后，也仅有几毫秒

说明

* 在不开启 keep-alive 的时候，平均延时为几百毫秒，因为大量的 tcp 三握四挥；
* 开启 keep-alive 延时为几毫秒的前提是连接池中必须有足够的已经建立好的连接

# 对于参数转换器的拓展

即在构造 http 请求时，需要将参数转换为字符串，框架默认情况下直接调用 toString，所以如果要进行拓展，可按照以下步骤

* 1、实现 ToStringConverter 接口

* 2、找到 net.yzx66.rpc.core.resolver，然后在下面的静态代码块直接添加即可

  ```java
  // 初始化 conveters
  static {
      converters.put(Date.class , new Date2StringConverter());
  }
  ```







