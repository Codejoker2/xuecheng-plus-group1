package com.xuecheng.content.api;

import com.xuecheng.model.PageParams;
import com.xuecheng.model.PageResult;
import com.xuecheng.model.dto.QueryCourseParamsDto;
import com.xuecheng.model.po.CourseBase;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zengweichuan
 * @description 课程信息编辑接口
 * @date 2024/3/24
 */
@RestController
@RequestMapping("/cource")
public class CourseBaseInfoController {

    @RequestMapping("/list")
    public PageResult<CourseBase> list(PageParams pageParams, @RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto){

        return null;
    }

}
