package com.httpproxy.utils;




import com.httpproxy.common.Data;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileUtil {
    //从caches文件夹下读取出所有缓存文件的文件名，存储在一个List对象中返回
    public static List<String> concreteCacheList(){
        List<String> res=new ArrayList<>();
        File file = new File(Data.cachePath);
        File[] files = file.listFiles();
        for(File f :files){
            res.add(f.getName());
        }
        return res;
    }
}
