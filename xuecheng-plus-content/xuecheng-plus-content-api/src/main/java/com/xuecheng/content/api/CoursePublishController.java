package com.xuecheng.content.api;

import com.xuecheng.content.service.CoursePublishService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class CoursePublishController {

    @Resource
    CoursePublishService coursePublishService;
    /**
     * 课程发布
     * @param courseId
     */
    @PostMapping("/coursepublish/{courseId}")
    public void coursePublish(@PathVariable Long courseId){
        //机构id，由于认证系统没有上线暂时硬编码
        Long companyId = 1232141425L;
        coursePublishService.coursePublish(companyId,courseId);
    }
}
