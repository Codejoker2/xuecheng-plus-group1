package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.base.exception.ValidationGroups;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.utils.SecurityUtil;
import io.swagger.annotations.Api;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author zengweichuan
 * @description 课程信息编辑接口
 * @date 2024/3/24
 */
@Api(value = "课程信息编辑接口",tags = "课程信息编辑接口")
@RestController
public class CourseBaseInfoController {

    @Resource
    private CourseBaseInfoService courseBaseInfoService;
    @PostMapping("/course/list")
    @PreAuthorize("hasAuthority('xc_teachmanager_course_list ')")
    public PageResult<CourseBase> list(PageParams pageParams, @RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto){
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        String companyId = user.getCompanyId();
        Long company = null;
        if (companyId != null) {
            company = Long.parseLong(companyId);
        }
        PageResult<CourseBase> courseBasePageResult = courseBaseInfoService.queryCourseBaseList(company,pageParams, queryCourseParamsDto);
        return courseBasePageResult;
    }

    @PostMapping("/course")
    public CourseBaseInfoDto createCourseBase(@RequestBody @Validated(ValidationGroups.Inster.class) AddCourseDto addCourseDto){
        CourseBaseInfoDto courseBase = courseBaseInfoService.createCourseBase(addCourseDto);
        return courseBase;
    }

    @GetMapping("/course/{courseId}")
    public CourseBaseInfoDto getCourseBaseById(@PathVariable Long courseId){
        return courseBaseInfoService.getCourseBaseById(courseId);
    }

    @PutMapping("/course")
    public CourseBaseInfoDto updateCourseBase(@RequestBody @Validated(ValidationGroups.Update.class)EditCourseDto editCourseDto){
        //机构id，由于认证系统没有上线暂时硬编码
        Long companyId = 1232141425L;

        return courseBaseInfoService.updateCourseBase(companyId,editCourseDto);
    }

    @DeleteMapping("/course/{courseId}")
    public void delCourseBase(@PathVariable Long courseId){
        courseBaseInfoService.delCourseBase(courseId);
    }

    /**
     * 提交课程审核
     * @param courseId
     */
    @PostMapping("/courseaudit/commit/{courseId}")
    public void auditCommit(@PathVariable Long courseId){
        //机构id，由于认证系统没有上线暂时硬编码
        Long companyId = 1232141425L;
        courseBaseInfoService.auditCommit(companyId,courseId);
    }
    /**
     * 由于没有审核的具体功能
     * 手动审核通过
     * @param courseId
     */
    @PostMapping("/courseaudit/pass/{courseId}")
    public void auditPassCourse(@PathVariable Long courseId){
        //机构id，由于认证系统没有上线暂时硬编码
        Long companyId = 1232141425L;
        //courseBaseInfoService.auditCommit(companyId,courseId);
        courseBaseInfoService.auditPassCourse(companyId,courseId);
    }

}
