package net.yzx66.rpc.enums;

public enum RequestContentType {

    None("") , // 没有请求的参数
    Form("application/x-www-form-urlencoded") ,
    JSON("application/json");

    String contentType;

    RequestContentType(String contentType){
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }
}
