package com.xuecheng.content.api.freemarker;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CoursePublishService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;

/**
 * @author zengweichuan
 * @description
 * @date 2024/4/17
 */
@Controller
public class CoursePublishController {

    @Resource
    private CoursePublishService coursePublishService;
    @GetMapping("/coursepreview/{courseId}")
    public ModelAndView preview(@PathVariable("courseId") Long courseId){

        ModelAndView modelAndView = new ModelAndView();
        CoursePreviewDto coursePreviewDto = coursePublishService.getCoursePreviewInfo(courseId);
        modelAndView.addObject("model",coursePreviewDto);
        modelAndView.setViewName("course_template");
        return modelAndView;
    }

    /**
     * 课程发布
     * @param courseId
     */
    @ResponseBody
    @PostMapping("/coursepublish/{courseId}")
    public void coursePublish(@PathVariable Long courseId){
        //机构id，由于认证系统没有上线暂时硬编码
        Long companyId = 1232141425L;
        coursePublishService.coursePublish(companyId,courseId);
    }
}
