package com.xuecheng.ucenter.model.po;

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
 * @author itcast
 */
@Data
@TableName("xc_user_role")
public class XcUserRole implements Serializable {

    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;

    private String roleId;

    private LocalDateTime createTime;

    private String creator;


}
