package com.xuecheng.content.model.dto;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @author zengweichuan
 * @description 课程预览数据模型
 * @date 2024/4/17
 */
@Data
@ToString
public class CoursePreviewDto {
    //课程基本信息,课程营销信息
    CourseBaseInfoDto courseBase;


    //课程计划信息
    List<TeachplanDto> teachplans;
}
