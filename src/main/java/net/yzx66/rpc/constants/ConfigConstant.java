package net.yzx66.rpc.constants;

public class ConfigConstant {
    // application.prperties 中配置的要生成 net.yzx66.rpc 代理类的包名
    public final static String SCAN_PACKAGE = "net.yzx66.rpc.remote.package";

    // 自动携带的 tokenName
    public final static String TOKEN_NAME = "net.yzx66.rpc.token.name";

    // 接口统一的 apiRespon 的类型
    public final static String API_RESPONE_TYPE = "net.yzx66.rpc.respone.type";
    // apiRespon 里面哪个属性是数据
    public final static String API_RESPONE_DATA_FILENAME = "net.yzx66.rpc.respone.data_filed";

    // 是否开启服务降级，只对 RpcReturnException 异常进行服务降级
    // 即只对无法连接，或者返回的状态码不是 2 开头进行降级
    public final static String HYSTRIX_ENABLE = "net.yzx66.rpc.hystrix.enable";

    // 是否开启 http 请求的 keepalive
    public final static String KEEP_ALIVE = "net.yzx66.rpc.http.keepalive";
    // 如果开启了 keepalive，那么连接池大小
    public final static String MAX_CONNECT = "net.yzx66.rpc.http.max_connect";
    // 建立连接超时的时间
    public final static String CONNECT_TIMEOUT = "net.yzx66.rpc.http.connect_timeout";
    // 通信超时的时间
    public final static String SOCKET_TIMEOUT = "net.yzx66.rpc.http.socket_timeout";
    // http 连接空闲时间
    public final static String MAX_IDLETIME = "net.yzx66.rpc.http.max_idletime";

}
