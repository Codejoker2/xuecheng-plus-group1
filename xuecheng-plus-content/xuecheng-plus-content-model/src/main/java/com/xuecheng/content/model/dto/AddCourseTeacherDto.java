package com.xuecheng.content.model.dto;

import com.xuecheng.exception.ValidationGroups;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * @author zengweichuan
 * @description 添加课程老师dto
 * @date 2024/4/3
 */
@Data
public class AddCourseTeacherDto {
    /**
     * 主键
     */

    private Long id;

    /**
     * 课程标识
     */
    @NotNull(message = "课程id不能为空",groups = {ValidationGroups.Inster.class})
    private Long courseId;

    /**
     * 教师标识
     */
    @NotEmpty(message = "教师名称不能为空",groups = {ValidationGroups.Inster.class})
    private String teacherName;

    /**
     * 教师职位
     */
    private String position;

    /**
     * 教师简介
     */
    private String introduction;

    /**
     * 照片
     */
    private String photograph;

    /**
     * 创建时间
     */
    private LocalDateTime createDate;
}
