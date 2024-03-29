package com.xuecheng.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

/**
 * @author zengweichuan
 * @description:分页查询通用参数
 * @date 2024/3/24
 */
@Data
@ToString
public class PageParams {
    @ApiModelProperty(value = "当前页码",example = "123")
    //当前页码
    private Long pageNo = 1L;
    @ApiModelProperty(value = "每页记录数默认值",example = "123")
    //每页记录数默认值
    private Long pageSize = 10L;

    public PageParams() {
    }

    public PageParams(Long pageNo, Long pageSize) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }
}
