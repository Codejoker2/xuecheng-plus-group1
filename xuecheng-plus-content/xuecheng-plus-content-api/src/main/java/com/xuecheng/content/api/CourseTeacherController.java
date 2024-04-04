package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.AddCourseTeacherDto;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import com.xuecheng.base.exception.ValidationGroups;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author zengweichuan
 * @description 教师信息接口
 * @date 2024/4/3
 */
@RestController
@RequestMapping("/courseTeacher")
public class CourseTeacherController {

    @Resource
    private CourseTeacherService courseTeacherService;
    @GetMapping("/list/{courseId}")
    public List<CourseTeacher> getTeacherByCourseId(@PathVariable Long courseId){
        return courseTeacherService.getTeacherByCourseId(courseId);
    }

    @PostMapping
    public CourseTeacher addCourseTeacher(@RequestBody @Validated(value = ValidationGroups.Inster.class) AddCourseTeacherDto courseTeacherDto){
        return courseTeacherService.addOrEditCourseTeacher(courseTeacherDto);
    }

    @DeleteMapping("/course/{courseId}/{teacherId}")
    public void delCourseTeacher(@PathVariable Long courseId,@PathVariable Long teacherId){
        courseTeacherService.delCourseTeacher(courseId,teacherId);
    }
}
