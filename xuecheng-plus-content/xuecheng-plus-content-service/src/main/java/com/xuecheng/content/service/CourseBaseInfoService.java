package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;

/**
 * @author zengweichuan
 * @description 课程基本信息管理业务接口
 * @date 2024/3/29
 */
public interface CourseBaseInfoService {

    PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto);

    CourseBaseInfoDto createCourseBase(AddCourseDto addCourseDto);

    CourseBaseInfoDto getCourseBaseById(Long courseId);

    CourseBaseInfoDto updateCourseBase(Long companyId,EditCourseDto editCourseDto);

    void delCourseBase(Long courseId);

    /**
     * 提交审核接口
     * @param companyId 机构id
     * @param courseId 课程id
     */
    void auditCommit(Long companyId,Long courseId);


    /**
     * 提交审核接口
     * @param companyId 机构id
     * @param courseId 课程id
     */
    void auditPassCourse(Long companyId,Long courseId);
}
