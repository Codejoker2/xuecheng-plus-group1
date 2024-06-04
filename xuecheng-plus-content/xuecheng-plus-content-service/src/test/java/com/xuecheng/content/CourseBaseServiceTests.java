package com.xuecheng.content;

import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * @author zengweichuan
 * @description
 * @date 2024/3/29
 */
@SpringBootTest
public class CourseBaseServiceTests {
    @Resource
    private CourseBaseInfoService courseBaseInfoService;
    @Test
    public void queryCourseBaseListTest(){
        //查询条件
        QueryCourseParamsDto queryCourseParamsDto = new QueryCourseParamsDto();
        queryCourseParamsDto.setCourseName("java");
        queryCourseParamsDto.setAuditStatus("202004");
        queryCourseParamsDto.setPublishStatus("203001");

        //分页参数
        PageParams pageParams = new PageParams();
        pageParams.setPageNo(1L);//页码
        pageParams.setPageSize(3L);//每页记录数
        Long companyId = 123456l;

        PageResult<CourseBase> courseBasePageResult = courseBaseInfoService.queryCourseBaseList(companyId,pageParams, queryCourseParamsDto);
        System.out.println(courseBasePageResult);
    }
}
