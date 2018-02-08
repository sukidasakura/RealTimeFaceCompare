package com.hzgc.collect.expand.merge;

import com.hzgc.collect.expand.conf.CommonConf;
import com.hzgc.collect.expand.log.LogEvent;
import com.hzgc.collect.expand.processer.FaceObject;
import com.hzgc.collect.expand.processer.KafkaProducer;
import com.hzgc.collect.expand.util.JSONHelper;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class RecoverErrProDataTest {
    private MergeUtil mergeUtil = new MergeUtil();
    private Logger LOG = Logger.getLogger(RecoverErrProDataTest.class);

    //get log dir from CommonConf
    private CommonConf commonConf = new CommonConf();
    private String processLogDir = commonConf.getProcessLogDir();
    private String receiveDir = commonConf.getReceiveLogDir();
    private String successDir = commonConf.getSuccessLogDir();
    private String mergeErrLogDir = commonConf.getMergeLogDir() + "/error";
    private LogEvent logEvent = new LogEvent();
    private String SUFFIX = ".log";
    private SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 测试获取process下错误日志的锁，并移动到success和merge的部分。
     */
    @Test
    public void testLockAndMove() {

        System.out.println(("***列出process目录下所有error日志路径***"));
        List<String> allErrorDir = mergeUtil.listAllErrorLogAbsPath(processLogDir);
        for (String errFile : allErrorDir) {
            System.out.println(("************************************"));
            System.out.println(("*********one of the error log*********"));
            System.out.println(("************************************"));
            //获取每个error.log需要移动到的success和merge目录下的路径
            String successErrFile = mergeUtil.getSuccessFilePath(errFile);
            String mergeErrFile = mergeUtil.getMergeFilePath(errFile);
            System.out.println("---*---每个error.log对应的success备份路径和merge处理路径---*---");
            System.out.println(errFile);
            System.out.println(successErrFile);
            System.out.println(mergeErrFile);
            List<String> errContent1 = mergeUtil.getAllContentFromFile(errFile);

            LOG.info("***将process下的error日志，移动到merge和success***");
            //移动到merge后，拷贝一份到success
            mergeUtil.lockAndMove(errFile, mergeErrFile); //其中包括判断锁是否存在
            mergeUtil.copyFile(mergeErrFile, successErrFile);
            System.out.println("---*---移动后，原data/process路径下的error日志内容---*---");

            List<String> errContent2 = mergeUtil.getAllContentFromFile(errFile);
            assertEquals("",0,errContent2.size());
            List<String> errContent3 = mergeUtil.getAllContentFromFile(mergeErrFile);
            List<String> errContent4 = mergeUtil.getAllContentFromFile(successErrFile);


        }
    }




    public void testOther(){

        //获取merge/error下所有error日记文件的绝对路径，放入一个List中（errLogPaths）
        List<String> errFilePaths = mergeUtil.listAllFileAbsPath(mergeErrLogDir);
        //若errLogPaths这个list不为空（merge/error下有错误日志）
        if (errFilePaths != null && errFilePaths.size() != 0) { // V-1 if start
            //对于每一个error.log
            for (String errorFilePath : errFilePaths) {
                //获取其中每一行数据
                List<String> errorRows = mergeUtil.getAllContentFromFile(errorFilePath);
                //判断errorRows是否为空，若不为空，则需要处理出错数据
                if (errorRows != null && errorRows.size() != 0) { // V-2 if start
                    for (String row : errorRows) {
                        //用JSONHelper将某行数据转化为LogEvent格式
                        LogEvent event = JSONHelper.toObject(row, LogEvent.class);

                        //每一条记录的格式为：
                        //"count":0, "url":"ftp://s100:/2018/01/09", "timestamp":"2018-01-02", "status":"0"
                        //用LogEvent获取该条数据的ftpUrl
                        String ftpUrl = event.getPath();
                        //获取该条数据的序列号
                        long count = event.getCount();
                        //根据路径取得对应的图片，并提取特征，封装成FaceObject，发送Kafka
                        FaceObject faceObject = GetFaceObject.getFaceObject(row);
                        if (faceObject != null) { // V-3 if start
                            SendDataToKafka sendDataToKafka = SendDataToKafka.getSendDataToKafka();
                            sendDataToKafka.sendKafkaMessage(KafkaProducer.getFEATURE(), ftpUrl, faceObject);
                            boolean success = sendDataToKafka.isSuccessToKafka();

                            //若发送kafka不成功，将错误日志写入/merge/error/下一个新的errorN-NEW日志中
                            String mergeErrFileNew = errorFilePath.replace(SUFFIX,"")+"-N"+SUFFIX;
                            logEvent.setPath(ftpUrl);
                            if (success) {
                                logEvent.setStatus("0");
                            } else {
                                logEvent.setStatus("1");
                            }
                            logEvent.setCount(count);
                            logEvent.setTimeStamp(Long.valueOf(SDF.format(new Date())));
                            mergeUtil.writeMergeFile(event, mergeErrFileNew);
                        } // V-3 if end：faceObject不为空的判断结束
                    }
                } // V-2 if end：errorRows为空的判断结束
                //删除已处理过的error日志
                mergeUtil.deleteFile(errorFilePath);
            }
        } else { //若merge/error目录下无日志
            LOG.info("Nothing in " + mergeErrLogDir);
        } // V-1 if end：对merge/error下是否有日志的判断结束
    }

}
