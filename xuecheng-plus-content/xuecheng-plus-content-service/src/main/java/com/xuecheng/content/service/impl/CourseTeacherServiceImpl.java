package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.dto.AddCourseTeacherDto;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import com.xuecheng.exception.XuechengPlusException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author zengweichuan
 * @description 课程老师信息服务
 * @date 2024/4/3
 */
@Service
public class CourseTeacherServiceImpl implements CourseTeacherService {

    @Resource
    private CourseTeacherMapper courseTeacherMapper;

    @Override
    public List<CourseTeacher> getTeacherByCourseId(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseTeacher::getCourseId, courseId);
        List<CourseTeacher> courseTeachers = courseTeacherMapper.selectList(wrapper);
        return courseTeachers;
    }

    @Override
    public CourseTeacher addOrEditCourseTeacher(AddCourseTeacherDto dto) {

        //判断id是否为空用来确定是修改还是更新
        if (dto.getId() == null){
            //新增课程老师
            Long id = addCourseTeacher(dto);
            dto.setId(id);
        }else{
            //更新
            CourseTeacher courseTeacher = new CourseTeacher();
            BeanUtils.copyProperties(dto,courseTeacher);
            courseTeacherMapper.updateById(courseTeacher);
        }
        //返回更改后结果
        return courseTeacherMapper.selectById(dto.getId());

    }

    @Transactional
    @Override
    public void delCourseTeacher(Long courseId, Long teacherId) {
        LambdaQueryWrapper<CourseTeacher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseTeacher::getCourseId,courseId);
        wrapper.eq(CourseTeacher::getId,teacherId);

        courseTeacherMapper.delete(wrapper);
    }

    //新增课程老师:检查教师是否已经存在,不存在则添加
    //返回添加后的主键值id
    public Long addCourseTeacher(AddCourseTeacherDto dto){
        LambdaQueryWrapper<CourseTeacher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseTeacher::getCourseId,dto.getCourseId());
        wrapper.eq(CourseTeacher::getTeacherName,dto.getTeacherName());
        CourseTeacher courseTeacherFromDb = courseTeacherMapper.selectOne(wrapper);
        if (courseTeacherFromDb != null) throw new XuechengPlusException("该课程的这名老师已经存在,无需重复添加!");

        CourseTeacher courseTeacher = new CourseTeacher();
        BeanUtils.copyProperties(dto,courseTeacher);
        courseTeacherMapper.insert(courseTeacher);
        return courseTeacher.getId();
    }
}
