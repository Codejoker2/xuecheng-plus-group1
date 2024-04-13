package com.xuecheng.media.service;

import com.xuecheng.media.model.po.MediaProcess;

import java.util.List;

/**
 * @author zengweichuan
 * @description
 * @date 2024/4/12
 */
public interface MediaProcessService {
    /**
     * 获取任务
     * @param shardIndex 分片序号
     * @param shardTotal   分片总量
     * @param count 每次最多取任务数量
     * @return
     */
    List<MediaProcess> selectListByShardIndex(int shardIndex,
                                              int shardTotal,
                                              int count);

    /**
     * @description 保存任务结果
     * @param taskId  任务id
     * @param status 任务状态
     * @param fileId  文件id
     * @param url url
     * @param errorMsg 错误信息
     * @return void
     * @author Mr.M
     * @date 2022/10/15 11:29
     */
    void saveProcessFinishStatus(Long taskId,String status,String fileId,String url,String errorMsg);

    List<MediaProcess> selectListByShardIndexAndProcessing(int count);

}
