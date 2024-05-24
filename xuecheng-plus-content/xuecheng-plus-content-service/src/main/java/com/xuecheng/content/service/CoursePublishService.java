package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import java.io.File;

/**
 * @author zengweichuan
 * @description 课程预览、发布接口
 * @date 2024/4/17
 */
public interface CoursePublishService {
    /**
     * @description 获取课程预览信息
     * @param courseId 课程id
     * @return com.xuecheng.content.model.dto.CoursePreviewDto
     * @author Mr.M
     * @date 2022/9/16 15:36
     */
    public CoursePreviewDto getCoursePreviewInfo(Long courseId);

    /**
     * 发布课程接口
     * @param companyId 机构id
     * @param courseId 课程id
     */
    void coursePublish(Long companyId,Long courseId);

    /**
     * @description 课程静态化
     * @param courseId  课程id
     * @return File 静态化文件
     * @author Mr.M
     * @date 2022/9/23 16:59
     */
    public File generateCourseHtml(Long courseId) ;
    /**
     * @description 上传课程静态化页面
     * @param file  静态化文件
     * @return void
     * @author Mr.M
     * @date 2022/9/23 16:59
     */
    public void  uploadCourseHtml(Long courseId,File file);
}
