package com.xuecheng.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("course_audit")
public class CourseAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 课程id
     */
    private Long courseId;

    /**
     * 审核意见
     */
    private String auditMind;

    /**
     * 审核状态
     */
    private String auditStatus;

    /**
     * 审核人
     */
    private String auditPeople;

    /**
     * 审核时间
     */
    private LocalDateTime auditDate;


}
