package com.httpproxy.proxy;

import com.httpproxy.common.Data;
import com.httpproxy.utils.FileUtil;
import com.httpproxy.utils.IoUtil;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SocketHandler  extends Thread{
    private Socket client;
    private Socket server=null;
    private String host = null;
    private int port = 80;
    private String clientInputString ="";
    //客户端输入流
    private DataInputStream clientInputStream = null;
    //服务器输入流
    private DataInputStream serverInputStream = null;
    //客户端输出流
    private DataOutputStream clientOutputStream = null;
    //服务器输出流
    private DataOutputStream serverOutputStream = null;
    //缓存文件输入流
    private FileInputStream fileInputStream=null;
    //缓存文件输出流
    private FileOutputStream fileOutputStream=null;
    private byte[] buf=new byte[1024*1024*4];
    private String url=null;
    private String headLine=null;
    private String cacheCode=null;
    private List<String> cacheList= FileUtil.concreteCacheList();
    public SocketHandler(Socket socket){
        client=socket;
    }
    @Override
    public void run() {
        try {
            clientInputStream=new DataInputStream(client.getInputStream());
            clientOutputStream=new DataOutputStream(client.getOutputStream());
            int readLength = clientInputStream.read(buf,0,buf.length);
                if(readLength>0){
                    clientInputString= new String(buf,0,readLength);
                    headLine = clientInputString.split("\r\n")[0];
                    url=headLine.split(" ")[1];
                    cacheCode=String.valueOf(url.hashCode());
                    System.out.println("接收到"+client.getInetAddress().getHostAddress()+"向"+url+"发送的请求");
                    parseServerHost("http://([^/]+)/");
                    if(Data.transList.contains(headLine.split(" ")[1])){
                        doTrans(new CountDownLatch(2));
                        return;
                    }
                    if(host!=null) {
                        server = new Socket(host, port);
                        serverInputStream = new DataInputStream(server.getInputStream());
                        serverOutputStream = new DataOutputStream(server.getOutputStream());
                        if (Data.banned.contains(host)||Data.bannedIP.contains(client.getLocalAddress().getHostAddress())) {
                            doBan();
                        }else if (headLine.contains("CONNECT ")) {
                            doConnect(new CountDownLatch(2));
                        }else if(cacheList.contains(cacheCode)){
                            getCache(cacheCode,new CountDownLatch(2));
                        }else{
                            serverOutputStream.write(buf, 0, readLength);
                            serverOutputStream.flush();
                            doRequest(new CountDownLatch(2));
                        }
                    }

            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }finally {
            IoUtil.close(client,clientInputStream,server,clientOutputStream,serverInputStream,serverOutputStream);
        }
    }
    //钓鱼功能：根据Data.transPath来仿造请求报文发给目标服务器，从而实现重定向
    private void doTrans(CountDownLatch latch) throws IOException, InterruptedException {
        System.out.println("对"+url+"的请求已被修改为对http://jwts.hit.edu.cn/的请求");
        clientInputString=Data.transPath;
        parseServerHost("http://([^/]+)/");
        server = new Socket(host, port);
        serverInputStream = new DataInputStream(server.getInputStream());
        serverOutputStream = new DataOutputStream(server.getOutputStream());
        serverOutputStream.write(Data.transPath.getBytes());
        serverOutputStream.flush();
        new StreamHandler(serverInputStream, clientOutputStream, latch).start();
        latch.await(12, TimeUnit.SECONDS);
        IoUtil.close(server,serverInputStream,serverOutputStream);
    }
    //禁止功能：通过仿造404响应报文发给客户端，从而实现禁止访问
    private void doBan() throws IOException {
        System.out.println("对"+url+"的请求已被禁止");
        StringBuilder ack = new StringBuilder();
        String httpVersion = headLine.split(" ")[2];
        ack.append(httpVersion).append(" 404 Not Found\r\n");
        ack.append("\r\n");
        client.getOutputStream().write(ack.toString().getBytes());
        client.getOutputStream().flush();

    }
    //连接功能：通过仿造200 Connection established响应报文发给客户端，从而与客户端完成首次连接
    private  void doConnect(CountDownLatch latch) throws IOException, InterruptedException {
        System.out.println("对"+url+"的请求已被代理");
        System.out.println("正在与" + host + "建立连接");
        StringBuilder ack = new StringBuilder();
        String httpVersion = headLine.split(" ")[2];
        ack.append(httpVersion).append(" 200 Connection established\r\n");
        ack.append("Proxy-agent: proxy\r\n");
        ack.append("\r\n");
        client.getOutputStream().write(ack.toString().getBytes());
        client.getOutputStream().flush();
        new StreamHandler(serverInputStream, clientOutputStream, latch).start();
        new StreamHandler(clientInputStream, serverOutputStream, latch).start();
        latch.await(6, TimeUnit.SECONDS);
    }
    //处理请求：直接将请求报文传给客户端，在将目标服务器的响应报文传送给客户端的同时将响应报文缓存起来。
    private void doRequest(CountDownLatch latch) throws IOException, InterruptedException {
        System.out.println("对"+url+"的请求已被代理");
        System.out.println("正在向" + url + "发送请求");
        File downloadPath= new File(System.getProperty("user.dir")+"\\caches", url.hashCode()+"");
//        downloadPath.delete();
        if(downloadPath.createNewFile()){
            fileOutputStream=new FileOutputStream(downloadPath);
        }
        new StreamHandler(clientInputStream, serverOutputStream, latch).start();
        new StreamHandler(serverInputStream, clientOutputStream,fileOutputStream, latch).start();
        latch.await(6, TimeUnit.SECONDS);
        System.out.println("缓存文件" + downloadPath.getName() + "创建成功");
    }
    //缓存功能：从缓存文件夹中取出相应的响应报文，然后传递给客户端（注释掉的是报告中的那种实现方式）
    private  void getCache(String fileName,CountDownLatch latch) throws IOException, InterruptedException {
        System.out.println("对"+url+"的请求已被代理");
//        System.out.println("正在向"+url+"发送请求");
//        new StreamHandler(clientInputStream, serverOutputStream, latch).start();
//        latch.await(3,TimeUnit.SECONDS);
//        buf=new byte[1024*1024*4];
//        int readLength = serverInputStream.read(buf,0,buf.length);
        File file = new File(Data.cachePath,fileName);
        if(file.exists()){
            System.out.println("缓存命中，正在读取文件"+fileName);
            fileInputStream= new FileInputStream(file);
            new StreamHandler(fileInputStream, clientOutputStream, latch).start();
            latch.await(3,TimeUnit.SECONDS);
        }

//        String serverInputString=null;
//        if(readLength>0) {
//            serverInputString = new String(buf, 0, readLength);
//            if(serverInputString.contains("304 Not Modified")){
//                System.out.println("缓存文件有效，正在读取文件"+fileName);
//                if(file.exists()){
//                    new StreamHandler(fileInputStream, clientOutputStream, latch).start();
//                    latch.await(6, TimeUnit.SECONDS);
//                }
//            }else{
//                System.out.println("缓存文件"+fileName+"已失效");
//                file.delete();
//                if(file.createNewFile()){
//                    System.out.println("缓存文件"+fileName+"已更新");
//                    fileOutputStream.write(buf, 0, readLength);
//                    fileOutputStream.flush();
//                }
//                clientOutputStream.write(buf, 0, readLength);
//                clientOutputStream.flush();
//                new StreamHandler(serverInputStream, clientOutputStream,fileOutputStream, latch).start();
//                latch.await(3,TimeUnit.SECONDS);
//            }
//        }


    }
    //根据请求报文来生成host和port
    private void parseServerHost(String regExp) {
        Pattern pattern = Pattern.compile(regExp);
        Matcher matcher = pattern.matcher(clientInputString + "/");
        if (matcher.find()) {
            host = matcher.group(1);
            if (host.contains(":")) {
                port = Integer.parseInt(host.substring(host.indexOf(":") + 1));
                host = host.substring(0, host.indexOf(":"));
            }
        }
    }
}
