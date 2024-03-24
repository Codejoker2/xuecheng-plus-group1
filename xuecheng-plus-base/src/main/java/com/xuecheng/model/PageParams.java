package com.xuecheng.model;

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
    //当前页码
    private Long pageNo = 1L;
    //每页记录数默认值
    private Long pageSize = 10L;

    public PageParams() {
    }

    public PageParams(Long pageNo, Long pageSize) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }
}
