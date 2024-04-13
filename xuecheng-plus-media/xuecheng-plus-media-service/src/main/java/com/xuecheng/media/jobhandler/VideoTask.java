package com.xuecheng.media.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import com.xuecheng.media.service.MediaProcessService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author zengweichuan
 * @description
 * @date 2024/4/12
 */
@Slf4j
@Component
public class VideoTask {
    @Resource
    private MediaFileService mediaFileService;

    @Resource
    private MediaProcessService mediaProcessService;

    @Resource
    private RedissonClient redissonClient;

    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegpath;

    /**
     * 非mp4视频转码成mp4
     */
    @XxlJob("VideoTask")
    public void videoJobHandler() {
        //获取分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        //获取本机电脑所有核心
        int processors = Runtime.getRuntime().availableProcessors();
        //一次处理视频数量不要超过cpu核心数
        //每次最多拿到的任务量
        int count = processors;
        //拿到任务
        List<MediaProcess> mediaProcessList = mediaProcessService.selectListByShardIndex(shardIndex, shardTotal, count);


        //计数器
        CountDownLatch countDownLatch = new CountDownLatch(processors);

        //使用线程池,开启多线程
        ExecutorService executorService = Executors.newFixedThreadPool(mediaProcessList.size());
        //为每一个视频处理都分配一个核心
        for (MediaProcess mediaProcess : mediaProcessList) {
            executorService.execute(()->{
                //获取分布式锁,锁住文件id
                RLock lock = redissonClient.getLock(mediaProcess.getFileId());
                try {
                    //1.开始抢锁
                    boolean getLock = lock.tryLock();
                    //没获取到锁就返回
                    if (!getLock) return;

                    //获取到锁
                    //2.从minio下载文件
                    File tempFile = downloadFileFromMinIO(mediaProcess);
                    if (tempFile == null) return;

                    //更新视频处理状态为处理中
                    mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(),"4",mediaProcess.getFileId(),null,"");

                    //3.下载成功
                    String bucket = mediaProcess.getBucket();
                    String filePath = mediaProcess.getFilePath();

                    //4.处理视频,转换视频格式
                    File mp4File = transformToMp4(tempFile, mediaProcess);
                    if (mp4File == null) return;

                    //视频转码成功
                    //5.上传到minio(按照原来的文件路径存入)
                    String fileId = mediaProcess.getFileId();
                    //获取minio的文件路径
                    String minioPath = mediaFileService.getFilePathByMd5(fileId, ".mp4");
                    boolean b = mediaFileService.addMediaFilesToMinIO(bucket, "video/mp4", minioPath, mp4File.getAbsolutePath());
                    if (!b) {
                        log.error("上传文件失败,本机文件位置:{},目标文件位置:{}", mp4File.getAbsolutePath(), bucket + filePath);
                    }

                    //保存视频处理后的数据到数据库
                    mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(), "2", fileId, minioPath, "success");
                } catch (Exception e) {

                } finally {
                    lock.unlock();
                    countDownLatch.countDown();
                }
            });
            //等待,给一个充裕的超时时间,防止无限等待，到达超时时间还没有处理完成则结束任务
            try {
                countDownLatch.await(30, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                log.error("进程计数器等待超时,自动更新为处理完成!文件为:{}", mediaProcess.getFilePath());
                throw new RuntimeException(e);
            }
        }


    }

    private File downloadFileFromMinIO(MediaProcess mediaProcess) {
        File tempFile = mediaFileService.downloadFileFromMinIO(mediaProcess.getBucket(), mediaProcess.getFilePath());
        if (tempFile == null) {
            log.debug("下载待处理文件失败,originalFile:{}", mediaProcess.getBucket().concat(mediaProcess.getFilePath()));
            //将处理结果保存到数据库
            mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", mediaProcess.getFileId(),
                    null, "下载待处理文件失败!");
        }
        return tempFile;
    }

    private File transformToMp4(File tempFile, MediaProcess mediaProcess) {
        String bucket = mediaProcess.getBucket();
        String filePath = mediaProcess.getFilePath();

        //处理结束的文件
        File mp4File;
        String result = null;
        //创建处理视频格式完后的文件
        try {
            mp4File = File.createTempFile("mp4", ".mp4");
        } catch (IOException e) {
            log.error("处理结束后的文件创建错误:{}", e.getMessage(), e);
            throw new RuntimeException();
        }

        try {
            Mp4VideoUtil mp4VideoUtil = new Mp4VideoUtil(ffmpegpath, tempFile.getAbsolutePath(), mp4File.getName(), mp4File.getAbsolutePath());
            result = mp4VideoUtil.generateMp4();
        } catch (Exception e) {
            log.error("视频格式转换失败,视频文件:{},出错:{}", bucket + filePath, e.getMessage());
        }
        if (!result.equals("success")) {
            log.error("处理视频失败,视频地址:{},错误信息:{}", bucket + filePath, result);
            mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", mediaProcess.getFileId(),
                    null, result);
            return null;
        }
        return mp4File;
    }
}
