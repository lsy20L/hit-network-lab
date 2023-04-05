package timerPackage;

import gbn.GBNHost;
import sr.SRClient;

/**
 * 计时器
 */
public class Timer extends Thread {

    private Model model;
    private GBNHost gbnHost;
    private SRClient srClient;
    public Timer(GBNHost gbnHost, Model model){
        this.gbnHost = gbnHost;
        this.model = model;
    }
    public Timer(SRClient srClient,Model model){
        this.srClient = srClient;
        this.model = model;
    }
    @Override
    public void run(){
            int time = model.getTime();
            int count=0;
            while(model.getTime()>0&&count<time*1000){
                try {
                    Thread.sleep(1);
                    count++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(count==time*1000&&count!=0){
                System.out.println("\n");
                if(gbnHost!=null){
                    System.out.println(gbnHost.getHostName() + "客户端等待ACK超时");
                }else if(srClient!=null) {
                    System.out.println("客户端等待ACK超时");
                }
                try {
                    if(gbnHost!=null){
                        gbnHost.timeOut();
                    }else if(srClient!=null) {
                        srClient.timeOut();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                model.setTime(0);
            }
    }
}