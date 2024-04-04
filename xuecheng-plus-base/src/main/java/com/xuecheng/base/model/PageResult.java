package com.xuecheng.base.model;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @author zengweichuan
 * @description  分页查询结果模型类
 * @date 2024/3/24
 */
@Data
@ToString
public class PageResult<T> {
    //数据列表
    private List<T> items;

    //总记录数
    private Long counts;
    //每页记录数
    private Long pageSize;
    //当前页码
    private Long page;

    public PageResult(List<T> items, Long counts, Long pageSize, Long page) {
        this.items = items;
        this.counts = counts;
        this.pageSize = pageSize;
        this.page = page;
    }
}
