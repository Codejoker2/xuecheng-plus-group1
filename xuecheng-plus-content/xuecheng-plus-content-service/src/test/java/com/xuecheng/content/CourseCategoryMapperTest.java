package com.xuecheng.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.po.CourseCategory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zengweichuan
 * @description
 * @date 2024/3/30
 */
@SpringBootTest
public class CourseCategoryMapperTest {
    @Resource
    private CourseCategoryMapper courseCategoryMapper;

    @Test
    public void categoryTreeTest() throws JsonProcessingException {
        //获取整个category
        List<CourseCategoryTreeDto> courseCategories = courseCategoryMapper.selectTreeNodes();
        //获取所有的根节点
        CourseCategoryTreeDto root = getRoot(courseCategories);
        //生成category树
        combineTree(root,courseCategories);

        //只要root的所有子节点数据,不要根节点的其他数据
        List<CourseCategoryTreeDto> childrenTreeNodes = root.getChildrenTreeNodes();

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(childrenTreeNodes);
        System.out.println(json);
    }

    void combineTree(CourseCategoryTreeDto root,List<CourseCategoryTreeDto> courseCategories){
        //设置递归结束条件
        if (root.getIsLeaf() == 1)return;

        //找到以当前节点为父节点的子节点,并添加到当前节点的子节点集合中
        for (CourseCategoryTreeDto category : courseCategories) {
            if(ischild(root,category)){
                root.getChildrenTreeNodes().add(category);
            }
        }
        //遍历孩子节点,并继续递归向下调用
        List<CourseCategoryTreeDto> categoryChildren = root.getChildrenTreeNodes();

        for (CourseCategoryTreeDto categoryChild : categoryChildren) {
            combineTree(categoryChild,courseCategories);
        }
    }

    //判断该节点是否是当前的子节点
    boolean ischild(CourseCategoryTreeDto root,CourseCategoryTreeDto category){
        return root.getId().equals(category.getParentid());
    }
    //找到根节点
    CourseCategoryTreeDto getRoot(List<CourseCategoryTreeDto> courseCategories){
        CourseCategoryTreeDto root = null;

        for (CourseCategoryTreeDto courseCategory : courseCategories) {
            if (isRoot(courseCategory)){
                root = courseCategory;
                break;
            }
        }
        return root;
    }
    //判断根节点
    boolean isRoot(CourseCategoryTreeDto courseCategory){
        return courseCategory.getParentid().equals("0");
    }
}
