package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.MediaProcessService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author zengweichuan
 * @description
 * @date 2024/4/12
 */
@Service
public class MediaProcessServiceImpl implements MediaProcessService {

    @Resource
    private MediaProcessMapper mediaProcessMapper;

    @Resource
    private MediaProcessHistoryMapper mediaProcessHistoryMapper;

    public List<MediaProcess> selectListByShardIndex(int shardIndex,
                                                     int shardTotal,
                                                     int count) {
        //Todo 将任务放到redis中并使用分布式锁锁住,以防止视频重复处理

        return mediaProcessMapper.selectListByShardIndex(shardIndex, shardTotal, count);
    }

    @Override
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {
        //查询任务
        MediaProcess mediaProcessFromDb = mediaProcessMapper.selectById(taskId);
        //查不出来就啥也不做
        if (mediaProcessFromDb == null) return;
        //更新任务状态
        LambdaQueryWrapper<MediaProcess> wrapper = new LambdaQueryWrapper<MediaProcess>().eq(MediaProcess::getId, taskId);
        //任务处理失败
        if (status.equals("3")) {
            MediaProcess mediaProcess = new MediaProcess();
            mediaProcess.setStatus("3");
            mediaProcess.setErrormsg(errorMsg);
            mediaProcess.setFailCount(mediaProcessFromDb.getFailCount() + 1);
            mediaProcessMapper.update(mediaProcess, wrapper);
            return;
        }
        //处理成功,更新url和状态
        mediaProcessFromDb.setUrl(url);
        mediaProcessFromDb.setStatus("2");
        mediaProcessFromDb.setFinishDate(LocalDateTime.now());
        mediaProcessMapper.updateById(mediaProcessFromDb);
        //添加到历史记录
        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcessFromDb, mediaProcessHistory);
        mediaProcessHistoryMapper.insert(mediaProcessHistory);
        //将任务迁移到任务历史表中,从process表中删除
        mediaProcessMapper.deleteById(mediaProcessFromDb.getId());
    }
}
