package net.yzx66.rpc.exception;

// 调用了 HttpClient 返回的异常
public class RpcReturnException extends RuntimeException {
    public RpcReturnException(String message) {
        super(message);
    }

}
