package com.hzgc.collect.expand.merge;

import com.hzgc.collect.expand.conf.CommonConf;
import com.hzgc.collect.expand.log.LogEvent;
import com.hzgc.collect.expand.processer.FaceObject;
import com.hzgc.collect.expand.processer.KafkaProducer;
import com.hzgc.collect.expand.util.JSONHelper;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

        System.out.println(("************************************" +
                "testLockAndMove：将process目录下所有能获取到锁的error日志，移动到success和merge" +
                "************************************"));
        List<String> allErrorDir = mergeUtil.listAllErrorLogAbsPath(processLogDir);
        for (String errFile : allErrorDir) {
            //获取每个error.log需要移动到的success和merge目录下的路径
            String successErrFile = mergeUtil.getSuccessFilePath(errFile);
            String mergeErrFile = mergeUtil.getMergeFilePath(errFile);
            System.out.println("每个error.log对应的success备份路径和merge处理路径：");
            System.out.println(errFile);
            System.out.println(successErrFile);
            System.out.println(mergeErrFile);
            //移动前，每个errorFile中的内容
            List<String> errContent1 = mergeUtil.getAllContentFromFile(errFile);

            //移动到merge后，拷贝一份到success
            mergeUtil.lockAndMove(errFile, mergeErrFile); //其中包括判断锁是否存在
            mergeUtil.copyFile(mergeErrFile, successErrFile);

            //移动后，每个errorFile中的内容
            List<String> errContent2 = mergeUtil.getAllContentFromFile(errFile);
            assertEquals("移动后，原error日志是否为空：", 0, errContent2.size());
            List<String> errContent3 = mergeUtil.getAllContentFromFile(mergeErrFile);
            assertArrayEquals("移动后，merge/error日志是否与原日志内容相同：", errContent1.toArray(), errContent3.toArray());
            List<String> errContent4 = mergeUtil.getAllContentFromFile(successErrFile);
            assertArrayEquals("移动后，success/error日志是否与原日志内容相同：", errContent1.toArray(), errContent4.toArray());
            assertArrayEquals("测试assertArrayEquals是否能够比较两个List是否相同：", errContent1.toArray(), errContent2.toArray());

        }
    }


    /**
     * 测试处理merge/error的错误日志部分
     * 假设每个error.log的前两条发送kafka失败，看是否能够写入到merge/error下的新日志中。
     * 不包含发送kafka
     */
    public void testDealMergeError() throws IOException {

        System.out.println(("************************************" +
                "testDealMergeError：测试处理merge/error的错误日志部分。" +
                "假设每个error.log的前两条发送kafka失败，看是否能够写入到merge/error下的新日志中。" +
                "************************************"));

        List<String> errFilePaths = mergeUtil.listAllFileAbsPath(mergeErrLogDir);
        if (errFilePaths != null && errFilePaths.size() != 0) {
            //对于每一个error.log
            for (String errorFilePath : errFilePaths) {
                String mergeErrFileNew = errorFilePath.replace(SUFFIX, "") + "-N" + SUFFIX;
                System.out.println("errorFile对应的新的errorFile-N：");
                System.out.println(errorFilePath);
                System.out.println(mergeErrFileNew);
                List<String> errorRows = mergeUtil.getAllContentFromFile(errorFilePath);
                if (errorRows != null && errorRows.size() != 0) {
                    for (int i = 0; i < errorRows.size(); i++) {
                        LogEvent event = JSONHelper.toObject(errorRows.get(i), LogEvent.class);
                        String ftpUrl = event.getPath();
                        long count = event.getCount();

                        //测试时，假设每个error.log的前两条发送kafka失败
                        boolean success;
                        if (i < 2) {
                            success = false;
                        } else {
                            success = true;
                        }

                        //发送失败的前两条需要写入新的merge/error-N.log，其他的均发送成功
                        if (!success) {
                            logEvent.setCount(count);
                            logEvent.setPath(ftpUrl);
                            logEvent.setTimeStamp(Long.valueOf(SDF.format(new Date())));
                            logEvent.setStatus("1");
                            mergeUtil.writeMergeFile(logEvent, mergeErrFileNew);
                        }
                    }
                }
                assertEquals("每个error.log发送失败的前两条是否都写入到新的merge/error", 2, mergeErrFileNew.length());
                //原本error.log中的前两行，放入List中
                List<String> errorTwo = new ArrayList<>();
                errorTwo.add(errorRows.get(0));
                errorTwo.add(errorRows.get(1));
                //error.log的前两行新写入的error-N，放入List中
                List<String> mergeErrFileNewList =Files.readAllLines(Paths.get(mergeErrFileNew));
                //比较新写入的mergeErrFileNew，是否和error.log中的前两行相同
                assertArrayEquals("比较新写入的mergeErrFileNew，是否和error.log中的前两行相同：",errorRows.toArray(), mergeErrFileNewList.toArray());
                mergeUtil.deleteFile(errorFilePath); //删除已处理过的error日志
            }
        }else{ //若merge/error目录下无日志
            LOG.info("Nothing in " + mergeErrLogDir);
        }
    }


}
