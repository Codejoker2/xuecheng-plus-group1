package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.content.mapper.*;
import com.xuecheng.content.model.dto.*;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.base.exception.XuechengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.service.TeachplanService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author zengweichuan
 * @description
 * @date 2024/3/29
 */
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Resource
    private CourseBaseMapper courseBaseMapper;

    @Resource
    private CourseMarketMapper courseMarketMapper;

    @Resource
    private CourseCategoryMapper courseCategoryMapper;

    @Resource
    private TeachplanMapper teachplanMapper;

    @Resource
    private CourseTeacherMapper courseTeacherMapper;

    @Resource
    private TeachplanMediaMapper teachplanMediaMapper;

    @Resource
    private TeachplanService teachplanService;

    @Resource
    private CoursePublishPreMapper coursePublishPreMapper;

    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {

        //对查询条件参数进行校验生成warpper对象
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()), CourseBase::getName,queryCourseParamsDto.getCourseName());
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()),CourseBase::getAuditStatus,queryCourseParamsDto.getAuditStatus());
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()),CourseBase::getStatus,queryCourseParamsDto.getPublishStatus());

        //分页查询E page 分页参数, @Param("ew") Wrapper<T> queryWrapper 查询条件
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(),pageParams.getPageSize());
        Page<CourseBase> courseBasePage = courseBaseMapper.selectPage(page, queryWrapper);

        //返回值
        PageResult<CourseBase> courseBasePageResult = new PageResult<CourseBase>(courseBasePage.getRecords(),page.getTotal(),page.getSize(),page.getCurrent());

        return courseBasePageResult;
    }

    /**
     *  新增课程:需要向课程信息和营销信息两张表插入数据,返回插入后的数据
     * @param addCourseDto
     * @return
     */
    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(AddCourseDto addCourseDto) {

        //插入课程基本信息表
        CourseBase courseBaseNew = saveCorseBase(addCourseDto);

        //插入到营销信息表
        CourseMarket courseMarketNew = saveCourseMarket(courseBaseNew.getId(), addCourseDto);

        //返回插入的信息
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBaseNew,courseBaseInfoDto);
        BeanUtils.copyProperties(courseMarketNew,courseBaseInfoDto);

        //找到对应的大分类名称和小分类名称汉字
        String mtName = courseCategoryMapper.selectById(courseBaseNew.getMt()).getName();
        String stName = courseCategoryMapper.selectById(courseBaseNew.getSt()).getName();

        courseBaseInfoDto.setMtName(mtName);
        courseBaseInfoDto.setStName(stName);
        return courseBaseInfoDto;
    }

    @Override
    public CourseBaseInfoDto getCourseBaseById(Long courseId) {
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();

        CourseBase courseBase = courseBaseMapper.selectById(courseId);

        //查询mtName,stName
        String mtName = courseCategoryMapper.selectById(courseBase.getMt()).getName();
        String stName = courseCategoryMapper.selectById(courseBase.getSt()).getName();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        courseBaseInfoDto.setMtName(mtName);
        courseBaseInfoDto.setStName(stName);

        //查询营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);

        //课程等级查询

        return courseBaseInfoDto;
    }

    @Transactional
    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId,EditCourseDto editCourseDto) {

        //校验:课程信息是否存在
        CourseBase courseBase1 = courseBaseMapper.selectById(editCourseDto.getId());
        if(courseBase1 == null) throw new XuechengPlusException("课程信息不存在");
        //校验本机构只能修改本机构的课程
        if(!courseBase1.getCompanyId().equals(companyId))throw new XuechengPlusException("只能修改同一个机构的课程!");

        //修改课程基本信息
        CourseBase courseBase = new CourseBase();
        BeanUtils.copyProperties(editCourseDto,courseBase);
        courseBase.setChangeDate(LocalDateTime.now());
        int updateCourseBaseRows = courseBaseMapper.updateById(courseBase);
        if (updateCourseBaseRows <= 0) throw new XuechengPlusException("修改课程信息失败");

        //修改营销信息
        this.saveCourseMarket(editCourseDto.getId(), editCourseDto);
        return this.getCourseBaseById(editCourseDto.getId());
    }

    /**
     *  删除课程需要删除课程相关的基本信息,营销信息,课程计划,课程教师信息
     * @param courseId
     */
    @Transactional
    @Override
    public void delCourseBase(Long courseId) {
        //删除课程相关基本信息
        courseBaseMapper.deleteById(courseId);
        //删除营销信息
        courseMarketMapper.deleteById(courseId);
        //删除课程计划
        teachplanMapper.delete(new LambdaQueryWrapper<Teachplan>().eq(Teachplan::getCourseId,courseId));
            //删除课程计划的媒体信息
        teachplanMediaMapper.delete(new LambdaQueryWrapper<TeachplanMedia>().eq(TeachplanMedia::getCourseId,courseId));
        //删除课程教师信息
        courseTeacherMapper.delete(new LambdaQueryWrapper<CourseTeacher>().eq(CourseTeacher::getCourseId,courseId));
    }

    @Transactional
    @Override
    public void auditCommit(Long companyId, Long courseId) {
        //约束校验
        //获取课程
        CourseBaseInfoDto courseBase = this.getCourseBaseById(courseId);
        //课程审核状态
        String auditStatus = courseBase.getAuditStatus();
        //当前审核状态为已提交不允许再次提交
        if(auditStatus.equals("202003")){
            XuechengPlusException.cast("当前审核状态为已提交不允许再次提交");
        }
        //本机构只允许提交本机构的课程
        if(!courseBase.getCompanyId().equals(companyId)){
            XuechengPlusException.cast("本机构只允许提交本机构的课程,不允许提交其他机构的课程");
        }
        //课程图片是否填写
        if(StringUtils.isEmpty(courseBase.getPic())){
            XuechengPlusException.cast("提交失败，请上传课程图片");
        }


        //课程计划信息
        List<TeachplanDto> teachplans =teachplanService.getTreeNodes(courseId);
        //课程营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);

        CoursePublishPre coursePublishPreNew = new CoursePublishPre();
        BeanUtils.copyProperties(courseBase,coursePublishPreNew);
        coursePublishPreNew.setMarket(JSON.toJSONString(courseMarket));
        coursePublishPreNew.setTeachplan(JSON.toJSONString(teachplans));
        coursePublishPreNew.setCreateDate(LocalDateTime.now());
        //设置预发布记录状态,已提交
        coursePublishPreNew.setStatus("202003");
        //查询课程预发布表是否存在该课程,存在则修改,不存在则插入
        CoursePublishPre coursePublishPreFromDB = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPreFromDB != null){
            coursePublishPreMapper.updateById(coursePublishPreNew);
        }else{
            coursePublishPreMapper.insert(coursePublishPreNew);
        }
        //修改课程表的审核字段未已提交
        CourseBase courseBaseNew = new CourseBase();
        courseBaseNew.setId(courseId);
        courseBaseNew.setAuditStatus("202003");

        courseBaseMapper.updateById(courseBaseNew);


    }

    //插入课程基本信息表
    private CourseBase saveCorseBase(AddCourseDto addCourseDto){

        //改为使用框架进行校验数据,使用注解在Controller层进行校验
        //插入到课程信息表
        CourseBase courseBaseNew = new CourseBase();
        BeanUtils.copyProperties(addCourseDto,courseBaseNew);
        //设置审核状态
        courseBaseNew.setAuditStatus("202002");
        //设置发布状态
        courseBaseNew.setStatus("203001");
        //机构id
        Long companyId = 1232141425L;
        courseBaseNew.setCompanyId(companyId);
        //添加时间
        courseBaseNew.setCreateDate(LocalDateTime.now());
        int insert = courseBaseMapper.insert(courseBaseNew);
        if (insert <= 0)throw new XuechengPlusException("新增课程基本信息失败");

        return courseBaseNew;
    }

    //插入到营销信息表
    private CourseMarket saveCourseMarket(Long id,AddCourseDto addCourseDto){

        //校验
        //收费规则
        if(StringUtils.isBlank(addCourseDto.getCharge())){
            throw new XuechengPlusException("收费规则没有选择");
        }
        if(addCourseDto.getCharge().equals("201001")){
            if(addCourseDto.getPrice().floatValue() <= 0 || addCourseDto.getPrice() == null){
                throw new XuechengPlusException("课程为收费价格不能为空且必须大于0");
            }
        }
        //插入
        CourseMarket courseMarketNew = new CourseMarket();
        BeanUtils.copyProperties(addCourseDto,courseMarketNew);
        courseMarketNew.setId(id);

        //如果数据库查询为空就创建,否则就更新
        CourseMarket courseMarketFromDb = courseMarketMapper.selectById(id);
        int effecttRow = 0;
        if (courseMarketFromDb == null){
            effecttRow = courseMarketMapper.insert(courseMarketNew);
        }
        effecttRow = courseMarketMapper.updateById(courseMarketNew);

        if(effecttRow <= 0) throw new RuntimeException("保存课程营销信息失败");

        return courseMarketNew;
    }
}
