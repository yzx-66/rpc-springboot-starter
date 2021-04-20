package net.yzx66.rpc.exception;

// 发起 net.yzx66.rpc 时的异常
// 即还没有调用 httpClient
public class RpcCallException extends RuntimeException {
    public RpcCallException(String message) {
        super(message);
    }
}
