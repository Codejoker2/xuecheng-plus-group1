package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseCategory;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zengweichuan
 * @description
 * @date 2024/3/30
 */
@Data
public class CourseCategoryTreeDto extends CourseCategory {

    List<CourseCategoryTreeDto> childrenTreeNodes;
}
