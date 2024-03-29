package com.xuecheng.model.dto;

import io.swagger.annotations.ApiModelProperty;
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

    @ApiModelProperty(value = "审核状态")
    //审核状态
    private String auditStatus;
    @ApiModelProperty(value = "课程名称")
    //课程名称
    private String courseName;
    @ApiModelProperty(value = "发布状态")
    //发布状态
    private String publishStatus;
}
