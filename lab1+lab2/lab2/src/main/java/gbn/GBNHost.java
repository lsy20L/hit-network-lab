package gbn;

import common.Data;
import timerPackage.Model;
import timerPackage.Timer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GBNHost {
    private int windowSize;
    private int dataNumber;
    private int timeOut;
    private String hostName;
    /*
    发送数据需要的变量
    */
    private int destPort;
    private  volatile int nextSeq = 1;
    private volatile int base = 1;
    private InetAddress destAddress;
    /*
    接受数据需要的变量
     */
    private int exceptedSeq = 1;
    /*
    发送和接受的Socket
     */
    private DatagramSocket GBNSocket;
    private DatagramSocket sendSocket;
    /*
    定时器
     */
    private List<Model> models=new ArrayList<>();
    /*
    文件位置
     */
    private List<byte[]> sendDatas=new ArrayList<>();
    private int lastSave=0;
    public GBNHost(int receivePort,int windowSize,int dataNumber,int timeOut,String name) throws SocketException, UnknownHostException {
        this.GBNSocket=new DatagramSocket();
        this.sendSocket=new DatagramSocket(receivePort);
        this.windowSize=windowSize;
        this.dataNumber=dataNumber;
        this.destAddress = InetAddress.getLocalHost();
        this.timeOut=timeOut;
        hostName=name;
    }

    public void setDestPort(int destPort) {
        this.destPort = destPort;
    }

    public void sendData(String filePath,int packetSize) throws Exception {
        File file = new File(filePath);
        if(!file.exists()){
            System.out.println("该文件不存在");
            return;
        }
        dataNumber=0;
        FileInputStream inputStream=new FileInputStream(file);
        byte[] bytes=new byte[packetSize];
        int length=0;
        while((length=inputStream.read(bytes,0,bytes.length))!=-1){
            Model model = new Model();
            model.setTime(0);
            models.add(model);
            dataNumber++;
            byte[] temp = Arrays.copyOf(bytes,length);
            sendDatas.add(temp);
        }
        System.out.println(hostName+"将文件拆分为了"+dataNumber+"个包");
        inputStream.close();
        while(nextSeq <= dataNumber) {
            while (nextSeq < base + windowSize&&nextSeq <= dataNumber) {
                if (nextSeq == 3) {
                    nextSeq++;
                    continue;
                }
                byte[] clientData = ("From: " + hostName + ", To: " + destPort + ", Seq: " + nextSeq  + ", length: " + sendDatas.get(nextSeq - 1).length + ", dataNumber: " + dataNumber + "@@@@@@@@").getBytes();
                System.out.println(hostName + "向" + destPort + "发送的数据:" + nextSeq);
                clientData = addBytes(clientData, sendDatas.get(nextSeq - 1));
                DatagramPacket datagramPacket = new DatagramPacket(clientData, clientData.length, destAddress, destPort);
                GBNSocket.send(datagramPacket);
                if (nextSeq == base) {
                    models.get(nextSeq-1).setTime(timeOut);
                    new Timer(this,models.get(nextSeq-1)).start();
                }
                nextSeq++;
            }
        }

    }

    public void timeOut() throws Exception {
        for(int i = base;i < nextSeq;i++){
            byte[] clientData = ("From: "+hostName+", To: " + destPort+", Seq: "+i+", length: "+sendDatas.get(i-1).length+", dataNumber: "+dataNumber+"@@@@@@@@").getBytes();
            System.out.println(hostName+"向"+destPort+"重新发送的数据:" + i);
            models.get(base-1).setTime(timeOut);
            clientData=addBytes(clientData,sendDatas.get(i-1));
            DatagramPacket datagramPacket = new DatagramPacket(clientData, clientData.length, destAddress, destPort);
            GBNSocket.send(datagramPacket);
        }
    }


    public void receiveData(String filePath) throws IOException {
        File file = new File(filePath);
        if(file.exists()){
            if(file.delete()){
                if(file.createNewFile()){
                    System.out.println("文件创建成功！");
                }
            }
        }
        FileOutputStream outputStream=new FileOutputStream(file);
        while(true){
            byte[] receivedData = new byte[4096];
            DatagramPacket datagramPacket = new DatagramPacket(receivedData, receivedData.length);
            sendSocket.receive(datagramPacket);
            String received = new String(receivedData, 0, datagramPacket.getLength());//offset是初始偏移量
            if(received.contains("ack: ")){
                int ack = Integer.parseInt(received.substring(received.indexOf("ack:") + 5).trim());
                base = ack + 1;
                if (base == nextSeq&&nextSeq<=models.size()) {
                    models.get(nextSeq-1).setTime(0);
                }
                System.out.println(hostName+"从"+destPort+"获得的数据:" + received);
                System.out.println("\n");
            }else{
                String label=received.split("@@@@@@@@")[0];
                int labelSize = (label+"@@@@@@@@").getBytes().length;
                String pattern="From: .*, To: \\d+, Seq: (\\d+), length: (\\d+), dataNumber: (\\d+)";
                Matcher matcher= Pattern.compile(pattern).matcher(label);
                if(!matcher.find()){
                    System.out.println(hostName+"收到未知数据"+label);
                    sendAck(exceptedSeq-1);
                }
                int receivedSeq=Integer.parseInt(matcher.group(1));
                int dataLength = Integer.parseInt(matcher.group(2));
                int dataNumberReceive= Integer.parseInt(matcher.group(3));
                if (receivedSeq == exceptedSeq) {
                    if(lastSave==receivedSeq-1){
                        outputStream.write(receivedData, labelSize, dataLength);
                        lastSave=receivedSeq;
                    }
                    if(exceptedSeq==5){
                        System.out.println("模拟丢失ACK");
                    }
                    else{
                        sendAck(exceptedSeq);
                    }
                    System.out.println(hostName + "收到了期待的数据编号:" + exceptedSeq);
                    if(exceptedSeq==dataNumberReceive){
                        System.out.println(hostName+"已收到所有数据");
                        outputStream.flush();
                        outputStream.close();
                    }
                    exceptedSeq++;
                    System.out.println('\n');
                } else {
                    System.out.println(hostName + "未收到期待的数据编号:" + exceptedSeq);
                    sendAck(exceptedSeq - 1);
                    System.out.println('\n');
                }
            }
        }
    }

    public void sendAck(int ack) throws IOException {
        String response =hostName+ " response ack: "+ack;
        byte[] responseData = response.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(responseData,responseData.length,destAddress,destPort);
        sendSocket.send(datagramPacket);
    }
    public String getHostName(){
        return hostName;
    }
    public static byte[] addBytes(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;

    }

    public static void main(String[] args) throws SocketException, UnknownHostException {
        GBNHost host1 = new GBNHost(8060,5,10,3,"host1");
        host1.setDestPort(8070);
        GBNHost host2 = new GBNHost(8070,5,10,3,"host2");
        host2.setDestPort(8060);
        new Thread(()->{
            try{
                host1.receiveData(Data.host1Path+"\\demo.txt");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(()->{
            try{
                host2.receiveData(Data.host2Path+"\\demo.txt");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(()->{
            try{
                host2.sendData(Data.host2Path+"\\test.txt",1024);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(()->{
            try{
                host1.sendData(Data.host1Path+"\\test.txt",1024);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

}
