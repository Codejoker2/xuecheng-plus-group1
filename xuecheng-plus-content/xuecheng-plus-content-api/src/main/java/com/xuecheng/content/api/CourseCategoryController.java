package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import com.xuecheng.model.PageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author zengweichuan
 * @description 课程类别前端控制器
 * @date 2024/3/30
 */
@RestController
@RequestMapping("/course-category")
public class CourseCategoryController {

    @Resource
    private CourseCategoryService courseCategoryService;
    @GetMapping("/tree-nodes")
    public List<CourseCategoryTreeDto> getTreeNodes(){
        List<CourseCategoryTreeDto> courseCategoryTreeDtos= courseCategoryService.queryTreeNodes();

        return courseCategoryTreeDtos;
    }
}
