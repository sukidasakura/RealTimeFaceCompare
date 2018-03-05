package com.hzgc.collect.ftp;

import com.hzgc.collect.expand.log.LogEvent;
import com.hzgc.collect.expand.merge.MergeUtil;
import com.hzgc.collect.expand.util.JSONHelper;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

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
            // receiveFiles[i]: receive-0
            if (receiveFiles[i].isDirectory()){
                File[] receiveLogFiles = receiveFiles[i].listFiles();
                Map<Integer, String> fileNameMap = new HashMap<>();
                if (receiveLogFiles != null){
                    for(File receiveLogFile: receiveLogFiles){
                        String filename = receiveLogFile.getName().replace(".log", "");
                        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
                        //判断文件名是否是整数，并排除读到error/error.log的可能性
                        if (pattern.matcher(filename).matches() && !filename.contains("error")) {
                            int fileName = Integer.parseInt(filename);
                            fileNameMap.put(fileName, receiveLogFile.toString());
                        }
                    }
                    String minFilePath;
                    if (receiveFiles.length > 1) {
                        fileNameMap.remove(0000000000000000000);
                        int minFileName = Collections.min(fileNameMap.keySet());
                        minFilePath = fileNameMap.get(minFileName);
                    } else {
                        minFilePath = fileNameMap.get(0000000000000000000);
                    }
                    System.out.println(minFilePath);

                    // content of 0000003000.log
                    List<String> receiveLogContent = mergeUtil.getAllContentFromFile(minFilePath);
                    // last event of 000000003000.log
                    LogEvent logEvent = JSONHelper.toObject(receiveLogContent.get(0), LogEvent.class);
                    Long timeStamp = logEvent.getTimeStamp();
                    receiveTimeStamps.add(timeStamp);
                    System.out.println(timeStamp);
                }
            }
        }
        Long minReceiveTime = Collections.min(receiveTimeStamps);
        System.out.println("The min time of receive Log is: " + minReceiveTime);


        //get max time of process Log
        File[] processFiles = processPath.listFiles();
        List<Long> processTimeStamps = new ArrayList<>();
        for (int i = 0; i < processFiles.length; i++) {
            // processFiles[i]: process-0
            if (processFiles[i].isDirectory()){
                File[] processLogFiles = processFiles[i].listFiles();
                Map<Integer, String> fileNameMap = new HashMap<>();
                if (processLogFiles != null){
                    for(File processLogFile: processLogFiles){
                        String filename = processLogFile.getName().replace(".log", "");
                        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
                        //判断文件名是否是整数，并排除读到error/error.log的可能性
                        if (pattern.matcher(filename).matches() && !filename.contains("error")) {
                            int fileName = Integer.parseInt(filename);
                            fileNameMap.put(fileName, processLogFile.toString());
                        }
                    }

                    int minFileName = Collections.min(fileNameMap.keySet());
                    String minFilePath = fileNameMap.get(minFileName);
                    System.out.println(minFilePath);

                    // content of 00000000.log
                    List<String> processLogContent = mergeUtil.getAllContentFromFile(minFilePath);
                    // last event of 000000.log
                    LogEvent logEvent = JSONHelper.toObject(processLogContent.get(processLogContent.size()-1), LogEvent.class);
                    Long timeStamp = logEvent.getTimeStamp();
                    processTimeStamps.add(timeStamp);
                    System.out.println(timeStamp);
                }
            }
        }
        Long maxProcessTime = Collections.max(processTimeStamps);
        System.out.println("The max time of process Log is: " + maxProcessTime);

        System.out.println("FTP receive and process TIME is:" + (maxProcessTime - minReceiveTime));
    }
}
