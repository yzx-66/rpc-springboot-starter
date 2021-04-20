package net.yzx66.rpc.exception;

public class HystrixException extends RuntimeException {
    public HystrixException(String message) {
        super(message);
    }
}
