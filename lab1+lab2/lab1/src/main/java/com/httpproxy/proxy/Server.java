package com.httpproxy.proxy;

import com.httpproxy.common.Data;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


public class Server extends Thread{
    private Integer port;
    private ServerSocket server;
    public Server(Integer port) throws IOException {
        this.port=port;
        server=new ServerSocket(port);
        System.out.println("代理服务器已启动，端口号为: "+port);
    }
    @Override
    public void run() {
        try {
            while(Data.isRun){
                File downloadPath= new File(System.getProperty("user.dir"),"caches");
                if (!downloadPath.exists()){
                    downloadPath.mkdirs();
                }
                Socket client = server.accept();
                new SocketHandler(client).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}