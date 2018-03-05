package com.hzgc.collect;

import com.hzgc.collect.expand.util.FTPDownloadUtils;

import java.io.File;
import java.util.Random;
import java.util.concurrent.*;

public class UpDataToFtp {
    public static void main(String[] args) {
        int threadNum = 3;

        String path = "/home/test/picFrom"; //图片路径
        int loopNum = 2; //循环次数
        String ipcId = "DS-2DE72XYZIW-ABCVS20160823CCCH641752612"; //ipcId

        ExecutorService pool = Executors.newFixedThreadPool(threadNum);
        for (int i = 0; i < threadNum ; i++) {
            pool.execute(new UpDataThread(path, loopNum, ipcId));
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("The total count send to FTP is: " );

        long awaitTIme = 5*1000;
        try {
            pool.shutdown();
            if (!pool.awaitTermination(awaitTIme,TimeUnit.MILLISECONDS))
                pool.shutdownNow();
        } catch (InterruptedException e){
            System.out.println("awaitTermination interrupted: " + e);
            pool.shutdownNow();
        }

        System.out.println("The total pic count send to FTP is: " + threadNum * (10 * loopNum));
        System.out.println("End");
    }
}

/**
 * 对于本地path路径下的所有文件，循环loopNum次，发送到Ftp服务器
 */
class UpDataThread implements Runnable{

    private String path; //图片路径
    private int loopNum; //循环次数
    private String ipcId; //ipcId
    private int count;

    UpDataThread(String path, int loopNum, String ipcId){
        this.path = path;
        this.loopNum = loopNum;
        this.ipcId = ipcId;
    }

    @Override
    public void run() {
        File file = new File(path);
        File[] tempList = file.listFiles();
        for (int i = 0; i < loopNum; i++) {
            Random random = new Random();
            int randNum = random.nextInt(10000000);
            String randName = String.valueOf(randNum);
            for (int j = 0; j < ( tempList != null ? tempList.length : 0); j++) {
                if (tempList[j].isFile()){
                    String originFilePath = tempList[j].getAbsolutePath();
                    String fileName = randName + tempList[j].getName();
                    StringBuilder filePath = new StringBuilder();
                    //拼接路径
                    filePath = filePath.append(ipcId).append("/")
                            .append(tempList[j].getName().substring(0, 14).replaceAll("_","/"));

                    //basePath FTP服务器基础目录
                    //filePath FTP服务器文件存放路径。例如分日期存放：/2015/01/01。
                    //文件的路径为 basePath + filePath
                    FTPDownloadUtils.upLoadFromProduction("172.18.18.163", 2222, "admin",
                            "123456", "", filePath.toString(), fileName, originFilePath);
                    count ++;
                    System.out.println(Thread.currentThread().getName() + ", count: " + count);
                }
            }
        }
        System.out.println("Thread name is: " + Thread.currentThread().getName() + ", Picture count send to FTP is：" + count);
    }
}