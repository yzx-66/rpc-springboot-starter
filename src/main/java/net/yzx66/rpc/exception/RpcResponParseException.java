package net.yzx66.rpc.exception;

// 解析 net.yzx66.rpc 返回的请求体时出现的异常
public class RpcResponParseException extends RuntimeException{
    public RpcResponParseException(String message) {
        super(message);
    }
}
