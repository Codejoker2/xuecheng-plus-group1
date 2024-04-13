package com.xuecheng.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.media.model.po.MediaProcess;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface MediaProcessMapper extends BaseMapper<MediaProcess> {

    //查询待处理任务
    List<MediaProcess> selectListByShardIndex(@Param("shardIndex") int shardIndex,
                                              @Param("shardTotal") int shardTotal,
                                              @Param("count") int count);

}
