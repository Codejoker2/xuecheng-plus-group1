package com.xuecheng.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.dto.CourseIndex;
import com.xuecheng.content.model.po.CoursePublish;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

/**
 * 测试远程调用es添加
 */
@SpringBootTest
public class CourseIndexSearchTest {

    @Resource
    private SearchServiceClient searchServiceClient;
    @Resource
    private CoursePublishMapper coursePublishMapper;
    @Test
    public void addIndex(){
        //从数据库中查询所有数据
        List<CoursePublish> coursePublishes = coursePublishMapper.selectList(new LambdaQueryWrapper<CoursePublish>());
        //远程调用添加es索引
        for (CoursePublish coursePublish : coursePublishes) {
            CourseIndex courseIndex = new CourseIndex();
            BeanUtils.copyProperties(coursePublish,courseIndex);
            searchServiceClient.add(courseIndex);
        }

    }
}
