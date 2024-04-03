package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.AddCourseTeacherDto;
import com.xuecheng.content.model.po.CourseTeacher;
import java.util.List;

/**
 * @author zengweichuan
 * @description
 * @date 2024/4/3
 */
public interface CourseTeacherService {
    List<CourseTeacher> getTeacherByCourseId(Long courseId);

    CourseTeacher addOrEditCourseTeacher(AddCourseTeacherDto courseTeacher);

    public void delCourseTeacher(Long courseId, Long teacherId);
}
