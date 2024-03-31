package com.xuecheng.content.model.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author weichuan
 */
@Data
@TableName("teachplan_work")
public class TeachplanWork implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 作业信息标识
     */
    private Long workId;

    /**
     * 作业标题
     */
    private String workTitle;

    /**
     * 课程计划标识
     */
    private Long teachplanId;

    /**
     * 课程标识
     */
    private Long courseId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createDate;

    private Long coursePubId;


}
