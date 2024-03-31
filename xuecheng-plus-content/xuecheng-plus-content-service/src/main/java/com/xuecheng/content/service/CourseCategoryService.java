package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;

import java.util.List;

/**
 * @author zengweichuan
 * @description
 * @date 2024/3/30
 */
public interface CourseCategoryService {
    public List<CourseCategoryTreeDto> queryTreeNodes();
}
