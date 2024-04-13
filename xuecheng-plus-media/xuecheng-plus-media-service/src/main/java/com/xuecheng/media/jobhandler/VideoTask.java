package com.xuecheng.media.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import com.xuecheng.media.service.MediaProcessService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    @Resource
    private MinioClient minioClient;

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
        //进行转码工作
        processing(mediaProcessList);

    }

    /**
     * 使用xxl-job处理在数据库中处于处理中状态超时的任务,进行重试操作
     */
    @XxlJob("RetryVideoTask")
    public void retryVideoTask() {
        //获取分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        //获取本机电脑所有核心
        int processors = Runtime.getRuntime().availableProcessors();
        //一次处理视频数量不要超过cpu核心数
        //每次最多拿到的任务量
        int count = processors;
        //查询所有处理中的任务,并进行
        List<MediaProcess> mediaProcessList = mediaProcessService.selectListByShardIndexAndProcessing(count);

        //过滤出执行时间大于30分钟的任务并进行重试
        List<MediaProcess> retryList = mediaProcessList.stream()
                .filter(mediaProcess->LocalDateTime.now().minusMinutes(30).isAfter(mediaProcess.getCreateDate()))
                .collect(Collectors.toList());
        if (retryList.size() <= 0) return;
        //进行转码工作
        processing(mediaProcessList);
    }
    //Todo 增加一个重试次数为3后通知开发者邮件的功能

    //Todo 分片文件清理

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

    //处理任务
    private void processing(List<MediaProcess> mediaProcessList) {
        if (mediaProcessList.size() <= 0 )return;

        //线程计数器
        CountDownLatch countDownLatch = new CountDownLatch(mediaProcessList.size());
        //开启线程池
        ExecutorService executorService = Executors.newFixedThreadPool(mediaProcessList.size());
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
                    mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(), "4", mediaProcess.getFileId(), null, "");

                    //3.下载成功
                    String bucket = mediaProcess.getBucket();
                    String origFilePath = mediaProcess.getFilePath();

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
                        log.error("上传文件失败,本机文件位置:{},目标文件位置:{}", mp4File.getAbsolutePath(), bucket + origFilePath);
                    }
                    //5.删除原来的avi文件
                    RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(origFilePath)
                            .build();
                    minioClient.removeObject(removeObjectArgs);


                    //6.保存视频处理后的数据到数据库,并移入到历史表
                    mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(), "2", fileId, minioPath, "success");

                    //7.更新media_file的url,改为mp4的路径
                    //先查询该文件信息
                    MediaFiles mediaFiles = mediaFileService.selectById(fileId);
                    String filename = mediaFiles.getFilename();
                    filename = filename.substring(0,filename.indexOf(".")) + ".mp4";

                    mediaFiles.setId(fileId);
                    mediaFiles.setFilename(filename);
                    mediaFiles.setFilePath(minioPath);
                    mediaFiles.setUrl("/" + bucket + "/" + minioPath);
                    mediaFiles.setChangeDate(LocalDateTime.now());
                    mediaFiles.setFileSize(mp4File.length());
                    mediaFileService.updateFileURL(mediaFiles);

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
}
