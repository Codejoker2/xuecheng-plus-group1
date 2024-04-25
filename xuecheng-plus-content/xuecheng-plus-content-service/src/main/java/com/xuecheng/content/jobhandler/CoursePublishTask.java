package com.xuecheng.content.jobhandler;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author zengweichuan
 * @description 课程发布任务调度
 * @date 2024/4/25
 */
@Slf4j
@Component
public class CoursePublishTask {
    //任务调度入口
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler(){
        System.out.println("牛逼");
    }
}
