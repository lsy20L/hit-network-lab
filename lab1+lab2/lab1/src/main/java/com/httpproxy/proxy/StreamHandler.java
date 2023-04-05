package com.httpproxy.proxy;

import com.httpproxy.utils.IoUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;

public class StreamHandler extends Thread {
    private final CountDownLatch countDownLatch;
    private  DataInputStream in=null;
    private  DataOutputStream out=null;
    private FileInputStream fileInputStream=null;
    private FileOutputStream fileOutputStream=null;

    public StreamHandler(DataInputStream in, DataOutputStream out, CountDownLatch countDownLatch) {
        this.in = in;
        this.out = out;
        this.countDownLatch = countDownLatch;
    }
    public StreamHandler(FileInputStream fileInputStream, DataOutputStream out, CountDownLatch countDownLatch) {
        this.fileInputStream = fileInputStream;
        this.out = out;
        this.countDownLatch = countDownLatch;
    }
    public StreamHandler(DataInputStream in, DataOutputStream out,FileOutputStream fileOutputStream, CountDownLatch countDownLatch) {
        this.in = in;
        this.out = out;
        this.countDownLatch = countDownLatch;
        this.fileOutputStream=fileOutputStream;
    }
    @Override
    public void run() {
        int len;
        byte buf[] = new byte[10240];
        try {
            if(fileInputStream==null){
                while ((len = in.read(buf, 0, buf.length)) != -1) {
                    out.write(buf, 0, len);
                    out.flush();
                    if(fileOutputStream!=null){
                        fileOutputStream.write(buf, 0, len);
                        fileOutputStream.flush();
                    }
                }
            }else{
                while((len=fileInputStream.read(buf,0,buf.length))!=-1){
                    out.write(buf, 0, len);
                    out.flush();
                }
            }
        } catch (Exception ignore) {

        } finally {
            countDownLatch.countDown();
            IoUtil.close(in, out,fileOutputStream,fileInputStream);
        }
    }
}
