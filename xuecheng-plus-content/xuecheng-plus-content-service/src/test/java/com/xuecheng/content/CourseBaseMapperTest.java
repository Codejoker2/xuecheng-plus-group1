package com.xuecheng.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.model.PageParams;
import com.xuecheng.model.PageResult;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author zengweichuan
 * @description
 * @date 2024/3/29
 */
@SpringBootTest
public class CourseBaseMapperTest {

    @Resource
    private CourseBaseMapper courseBaseMapper;
    /**
     *课程基础信息查询测试
     */
    @Test
    public void testCourseBaseMapper(){
        //测试查询接口

        //查询条件
        QueryCourseParamsDto queryCourseParamsDto = new QueryCourseParamsDto();
        queryCourseParamsDto.setCourseName("java");
        queryCourseParamsDto.setAuditStatus("202004");
        queryCourseParamsDto.setPublishStatus("203001");

        //分页条件
        PageParams pageParams = new PageParams();
        pageParams.setPageNo(1L);
        pageParams.setPageSize(2L);

        LambdaQueryWrapper<CourseBase> courseBaseQueryWrapper = new LambdaQueryWrapper<>();
        courseBaseQueryWrapper.like(StringUtils.isEmpty(queryCourseParamsDto.getCourseName()), CourseBase::getName,queryCourseParamsDto.getCourseName());
        courseBaseQueryWrapper.eq(StringUtils.isEmpty(queryCourseParamsDto.getAuditStatus()),CourseBase::getAuditStatus,queryCourseParamsDto.getAuditStatus());
        courseBaseQueryWrapper.eq(StringUtils.isEmpty(queryCourseParamsDto.getPublishStatus()),CourseBase::getStatus,queryCourseParamsDto.getPublishStatus());

        //分页查询E page 分页参数, @Param("ew") Wrapper<T> queryWrapper 查询条件
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(),pageParams.getPageSize());
        Page<CourseBase> courseBasePage = courseBaseMapper.selectPage(page, courseBaseQueryWrapper);

        //返回值
        PageResult<CourseBase> courseBasePageResult = new PageResult<CourseBase>(courseBasePage.getRecords(),page.getTotal(),page.getSize(),page.getCurrent());
        System.out.println(courseBasePageResult);
    }
}
