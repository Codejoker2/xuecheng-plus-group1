package com.xuecheng.learning.service;

import com.xuecheng.base.model.RestResponse;

public interface LearningService {
    /**
     * 获取课程视频网址
     * @param userId 用户名
     * @param courseId  课程id
     * @param teachplanId   课程计划id
     * @param mediaId   媒体文件id
     * @return
     */
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId);

}
