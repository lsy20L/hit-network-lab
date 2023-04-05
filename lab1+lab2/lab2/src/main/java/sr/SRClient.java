package sr;

import com.sun.org.apache.xpath.internal.operations.Mod;
import timerPackage.Model;
import timerPackage.Timer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class SRClient {
    //共用端口号
    private final int port = 80;
    private final int dataNumber = 20;
    private DatagramSocket datagramSocket = new DatagramSocket();
    private DatagramPacket datagramPacket;
    private InetAddress destAddress;
    private List<Model> models=new ArrayList<>();
    private volatile int nextSeq = 1;
    private volatile int base = 1;
    private List<Boolean> marks=new ArrayList<>();
    private final int windowSize = 5;
    private final int timeOut = 3;


    public SRClient() throws Exception {
        new Thread(()-> {
            try {
                sendData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        while(true){
            byte[] bytes = new byte[4096];
            datagramPacket = new DatagramPacket(bytes, bytes.length);
            datagramSocket.receive(datagramPacket);
            String fromServer = new String(bytes, 0, datagramPacket.getLength());
            int ack = Integer.parseInt(fromServer.substring(fromServer.indexOf("ack:")+4).trim());
            marks.set(ack-1,true);
            System.out.println("从服务器获得的数据:" + fromServer);
            System.out.println("\n");
            if(base == ack){
                models.get(base-1).setTime(0);
                base++;
                for(int i = base; i < nextSeq;i++){
                    if(marks.get(i-1)){
                        base = i +1;
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new SRClient();
    }

    /**
     * 向服务器发送数据
     *
     * @throws Exception
     */
    private void sendData() throws Exception {
        destAddress = InetAddress.getLocalHost();
        for (int i = 0; i < dataNumber; i++) {
            Model temp = new Model();
            temp.setTime(0);
            models.add(temp);
            marks.add(false);
        }
        while(nextSeq <= dataNumber){
            while (nextSeq < base + windowSize&&nextSeq<=dataNumber) {
                if(nextSeq == 3) {
                    nextSeq++;
                }
                String clientData = "客户端发送的数据编号:" + nextSeq;
                System.out.println("向服务器发送的数据:"+nextSeq);
                byte[] data = clientData.getBytes();
                DatagramPacket datagramPacket = new DatagramPacket(data, data.length, destAddress, port);
                datagramSocket.send(datagramPacket);
                models.get(base-1).setTime(timeOut);
                new Timer(this,models.get(base-1)).start();
                nextSeq++;
            }
        }

    }

    public void timeOut() throws Exception {
        String clientData = "客户端重新发送的数据编号:" + base;
        System.out.println("向服务器重新发送的数据:" + base);
        byte[] data = clientData.getBytes();
        models.get(base-1).setTime(timeOut);
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, destAddress, port);
        datagramSocket.send(datagramPacket);
    }

}
