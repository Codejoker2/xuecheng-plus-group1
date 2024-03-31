package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.exception.XuechengPlusException;
import com.xuecheng.model.PageParams;
import com.xuecheng.model.PageResult;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

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

    //插入课程基本信息表
    private CourseBase saveCorseBase(AddCourseDto addCourseDto){

        //进行校验数据
        if (StringUtils.isBlank(addCourseDto.getName())) {
            throw new XuechengPlusException("课程名称为空");
        }

        if (StringUtils.isBlank(addCourseDto.getMt())) {
            throw new XuechengPlusException("课程分类为空");
        }

        if (StringUtils.isBlank(addCourseDto.getSt())) {
            throw new XuechengPlusException("课程分类为空");
        }

        if (StringUtils.isBlank(addCourseDto.getGrade())) {
            throw new XuechengPlusException("课程等级为空");
        }

        if (StringUtils.isBlank(addCourseDto.getTeachmode())) {
            throw new XuechengPlusException("教育模式为空");
        }

        if (StringUtils.isBlank(addCourseDto.getUsers())) {
            throw new XuechengPlusException("适应人群为空");
        }

        if (StringUtils.isBlank(addCourseDto.getCharge())) {
            throw new XuechengPlusException("收费规则为空");
        }
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
