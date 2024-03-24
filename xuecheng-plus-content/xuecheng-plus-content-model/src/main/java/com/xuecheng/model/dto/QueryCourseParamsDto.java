package com.xuecheng.model.dto;

import lombok.Data;
import lombok.ToString;

/**
 * @author zengweichuan
 * @description课程查询参数Dto
 * @date 2024/3/24
 */
@Data
@ToString
public class QueryCourseParamsDto {

    //审核状态
    private String auditStatus;
    //课程名称
    private String courseName;
    //发布状态
    private String publishStatus;
}
