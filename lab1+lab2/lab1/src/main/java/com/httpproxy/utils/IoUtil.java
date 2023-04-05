package com.httpproxy.utils;

import java.io.Closeable;
import java.io.IOException;

public class IoUtil {
    //关闭所有IO流
    public static void close(Closeable... closeables){
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
