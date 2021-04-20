package net.yzx66.rpc.utils;

public class StringUtil {

    // 把字符串第一个字母转为大写
    public static String makeFirstCapital(String s){
        if(s.charAt(0) <= 'z' && s.charAt(0) >= 'a'){
            char[] chars = s.toCharArray();
            chars[0] = (char)(chars[0] - 32);
            s = new String(chars);
        }

        return s;
    }
    // 把字符串第一个字母转为小写
    public static String makeFirstLower(String s){
        if(s.charAt(0) <= 'Z' && s.charAt(0) >= 'A'){
            char[] chars = s.toCharArray();
            chars[0] = (char)(chars[0] + 32);
            s = new String(chars);
        }

        return s;
    }

}
