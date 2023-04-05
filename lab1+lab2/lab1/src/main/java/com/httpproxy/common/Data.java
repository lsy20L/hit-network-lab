package com.httpproxy.common;

import com.httpproxy.utils.FileUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Data {
    public static boolean isRun=true;
    //被禁止访问的网址
    public static String[] ban={"www.hitsz.edu.cn"};
    //被禁止访问的IP
    public static String[] banIP={""};//127.0.0.1
    //将会被重定向的网址
    public static String[] trans={"http://www.hitwh.edu.cn/"};
    public static List<String> transList=Arrays.stream(trans).collect(Collectors.toList());
    public static List<String> bannedIP=Arrays.stream(banIP).collect(Collectors.toList());
    public static List<String> banned= Arrays.stream(ban).collect(Collectors.toList());
    //缓存文件所在目录的路径
    public static String cachePath =System.getProperty("user.dir")+"\\caches";
    //虚假的请求报文（钓鱼用的）
    public static String transPath ="GET http://jwts.hit.edu.cn/ HTTP/1.1\r\n" +
            "Host: jwts.hit.edu.cn\r\n" +
            "\r\n";
}
