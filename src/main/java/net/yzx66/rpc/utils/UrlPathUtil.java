package net.yzx66.rpc.utils;

public class UrlPathUtil {

    public static String appendUrlPath(String path , String append){
        if(append.length() == 0){
            return path;
        }

        if((path.length() != 0 && path.charAt(path.length() - 1) == '/')
                || append.charAt(0) == '/'){
            return path + append;
        }
        return path + "/" + append;
    }
}
