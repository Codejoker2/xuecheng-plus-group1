package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zengweichuan
 * @description 课程类别服务
 * @date 2024/3/30
 */
@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {

    @Resource
    private CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes() {
        //获取整个category
        List<CourseCategoryTreeDto> courseCategories = courseCategoryMapper.selectTreeNodes();
        //获取所有的根节点
        CourseCategoryTreeDto root = getRoot(courseCategories);
        //生成category树
        combineTree(root,courseCategories);

        //只要root的所有子节点数据,不要根节点的其他数据
        List<CourseCategoryTreeDto> childrenTreeNodes = root.getChildrenTreeNodes();
        return childrenTreeNodes;
    }

    //生成整个树节点
    void combineTree(CourseCategoryTreeDto root,List<CourseCategoryTreeDto> courseCategories){
        //设置递归结束条件
        if (root.getIsLeaf() == 1)return;
        //为孩子节点申请空间进行存储
        root.setChildrenTreeNodes(new ArrayList<>());
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
