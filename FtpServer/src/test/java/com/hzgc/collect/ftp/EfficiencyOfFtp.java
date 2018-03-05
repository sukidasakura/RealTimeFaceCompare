package com.hzgc.collect.ftp;

import com.hzgc.collect.expand.log.LogEvent;
import com.hzgc.collect.expand.merge.MergeUtil;
import com.hzgc.collect.expand.util.JSONHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EfficiencyOfFtp {
    public static void main(String[] args) {
        String rPath = "/home/test/data/receive";
        String pPath = "/home/test/data/process";
        MergeUtil mergeUtil = new MergeUtil();

        File receivePath = new File(rPath);
        File processPath = new File(pPath);

        //get min time of receive Log
        File[] receiveFiles = receivePath.listFiles();
        List<Long> receiveTimeStamps = new ArrayList<>();
        for (int i = 0; i < receiveFiles.length ; i++) {
            if (receiveFiles[i].isDirectory()){
                List<String> receiveLogAbsPath =
                        mergeUtil.listAllFileAbsPath(receiveFiles[i].toString());
                // content of 0000000000.log
                List<String> receiveLogContent = mergeUtil.getAllContentFromFile(receiveLogAbsPath.get(0));
                // first event of 000000000000.log
                LogEvent logEvent = JSONHelper.toObject(receiveLogContent.get(0), LogEvent.class);
                Long timeStamp = logEvent.getTimeStamp();
                receiveTimeStamps.add(timeStamp);
            }
        }
        Long minReceiveTime = Collections.min(receiveTimeStamps);
        System.out.println("The min time of receive Log is: " + minReceiveTime);


        //get max time of process Log
        File[] processFiles = processPath.listFiles();
        List<Long> processTimeStamps = new ArrayList<>();
        for (int i = 0; i < processFiles.length; i++) {
            if (processFiles[i].isDirectory()){
                List<String> processLogAbsPath =
                        mergeUtil.listAllFileAbsPath(processFiles[i].toString());
                // content of max.log
                List<String> processLogContent =
                        mergeUtil.getAllContentFromFile(processLogAbsPath.get(processLogAbsPath.size()-1));
                // last event of max.log
                LogEvent logEvent = JSONHelper.toObject(processLogContent.get(processLogContent.size()-1), LogEvent.class);
                Long timeStamp = logEvent.getTimeStamp();
                processTimeStamps.add(timeStamp);
                System.out.println(timeStamp);
            }
        }
        Long maxProcessTime = Collections.max(processTimeStamps);
        System.out.println("The max time of process Log is: " + maxProcessTime);

        System.out.println("FTP receive and process TIME is:" + (maxProcessTime - minReceiveTime));

    }
}
