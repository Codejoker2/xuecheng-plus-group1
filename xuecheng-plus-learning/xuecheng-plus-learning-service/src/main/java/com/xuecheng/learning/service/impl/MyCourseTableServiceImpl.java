package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XuechengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class MyCourseTableServiceImpl implements MyCourseTablesService {

    @Resource
    private ContentServiceClient contentServiceClient;

    @Resource
    private XcChooseCourseMapper chooseCourseMapper;

    @Resource
    private XcCourseTablesMapper courseTablesMapper;

    @Override
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        //远程查询课程信息
        CoursePublish coursePublish = contentServiceClient.getCoursepublish(courseId);
        String charge = coursePublish.getCharge();
        //课程收费标准
        //选课记录
        XcChooseCourse chooseCourse = null;
        //免费
        if (charge.equals("201000")) {
            //添加免费课程到选课记录表
            chooseCourse = addFreeCourse(userId, coursePublish);
            //添加到我的课程表
            XcCourseTables courseTables = addCourseTables(chooseCourse);
        }
        if (charge.equals("201001")) {
            //添加收费课程到选课记录表
            chooseCourse = addChargeCourse(userId, coursePublish);
        }
        //准备返回数据
        XcChooseCourseDto dto = new XcChooseCourseDto();
        BeanUtils.copyProperties(chooseCourse,dto);
        //查询学习资格
        XcCourseTablesDto xcCourseTablesDto = getLearningStatus(userId, courseId);
        dto.setLearnStatus(xcCourseTablesDto.getLearnStatus());

        return dto;
    }

    //获取学习资格
    /*
    XcCourseTablesDto 学习资格状态
    [{"code":"702001","desc":"正常学习"},
    {"code":"702002","desc":"没有选课或选课后没有支付"},
    {"code":"702002","desc":"已过期需要申请续期或重新支付"}]
     */
    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        XcCourseTablesDto dto = new XcCourseTablesDto();

        //1.判断是否有选课，如果课程表中没有该课程，就是没有选课
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);
        if (xcCourseTables == null) {
            dto.setLearnStatus("702002");
            return dto;
        }
        BeanUtils.copyProperties(xcCourseTables, dto);
        //2.判断是否过期
        boolean isBefore = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if (isBefore) {
            dto.setLearnStatus("702002");
            return dto;
        }

        //3.正常学习
        dto.setLearnStatus("702001");

        return dto;
    }

    @Override
    public PageResult<XcCourseTables> selectCourseTablePage(String userId, MyCourseTableParams params) {
        Page<XcCourseTables> page = new Page<>(params.getPage(), params.getSize());
        LambdaQueryWrapper<XcCourseTables> wrapper = new LambdaQueryWrapper<XcCourseTables>().eq(XcCourseTables::getUserId, userId);
        //查询
        Page<XcCourseTables> selectPage = courseTablesMapper.selectPage(page, wrapper);
        long counts = selectPage.getTotal();
        List<XcCourseTables> records = selectPage.getRecords();

        //准备返回值
        PageResult<XcCourseTables> result = new PageResult<XcCourseTables>(
                records,
                counts,
                Long.parseLong(params.getSize()+""),
                Long.parseLong(params.getPage() + "")
        );

        return result;
    }

    private XcChooseCourse addChargeCourse(String userId, CoursePublish coursePublish) {
        //如果存在待支付交易记录直接返回
        LambdaQueryWrapper<XcChooseCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursePublish.getId())
                .eq(XcChooseCourse::getOrderType, "700002")//收费订单
                .eq(XcChooseCourse::getStatus, "701002");//待支付
        List<XcChooseCourse> xcChooseCourses = chooseCourseMapper.selectList(wrapper);
        if (!xcChooseCourses.isEmpty()) {
            return xcChooseCourses.get(0);
        }
        XcChooseCourse chooseCourse = new XcChooseCourse();
        chooseCourse.setCourseId(coursePublish.getId());
        chooseCourse.setCourseName(coursePublish.getName());
        chooseCourse.setUserId(userId);
        chooseCourse.setCompanyId(coursePublish.getCompanyId());
        chooseCourse.setOrderType("700002");
        chooseCourse.setCreateDate(LocalDateTime.now());
        chooseCourse.setCoursePrice(coursePublish.getPrice());
        //有效期配置
        chooseCourse.setValidDays(coursePublish.getValidDays());
        chooseCourse.setStatus("701002");//选课状态:待支付
        //开始有效时间
        chooseCourse.setValidtimeStart(LocalDateTime.now());
        //结束有效时间
        chooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(chooseCourse.getValidDays()));

        //插入到选课记录表
        int insert = chooseCourseMapper.insert(chooseCourse);
        if (insert == 0) {
            log.error("插入到选课记录表失败！课程id:{}", coursePublish.getId());
            throw new XuechengPlusException("插入课程：" + coursePublish.getId() + "到选课记录表失败！");
        }
        return chooseCourse;
    }

    private XcCourseTables addCourseTables(XcChooseCourse chooseCourse) {
        //选课记录完成且未过期可以添加课程到课程表
        String status = chooseCourse.getStatus();
        if (!"701001".equals(status)) {
            throw new XuechengPlusException("选课未成功，无法添加到课程表");
        }
        //查找我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(chooseCourse.getUserId(), chooseCourse.getCourseId());
        //如果已存在就不用再次添加，直接返回即可
        if (xcCourseTables != null) {
            return xcCourseTables;
        }
        //给courseTablesNew赋值
        XcCourseTables courseTablesNew = new XcCourseTables();
        BeanUtils.copyProperties(chooseCourse, courseTablesNew);
        courseTablesNew.setChooseCourseId(chooseCourse.getId());
        courseTablesNew.setCourseType(chooseCourse.getOrderType());

        //插入到我的课程表中
        int insert = courseTablesMapper.insert(courseTablesNew);
        return courseTablesNew;
    }

    private XcChooseCourse addFreeCourse(String userId, CoursePublish coursePublish) {
        //查询选课记录表是否存在免费的且选课成功的订单，有的话就直接返回
        LambdaQueryWrapper<XcChooseCourse> wrapper = new LambdaQueryWrapper<XcChooseCourse>()
                .eq(XcChooseCourse::getCourseId, coursePublish.getId())
                .eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getOrderType, "700001")//免费课程
                .eq(XcChooseCourse::getStatus, "701001");//选课成功

        List<XcChooseCourse> chooseCourses = chooseCourseMapper.selectList(wrapper);
        if (!chooseCourses.isEmpty()) {
            return chooseCourses.get(0);
        }

        XcChooseCourse chooseCourse = new XcChooseCourse();
        chooseCourse.setCourseId(coursePublish.getId());
        chooseCourse.setCourseName(coursePublish.getName());
        chooseCourse.setUserId(userId);
        chooseCourse.setCompanyId(coursePublish.getCompanyId());
        chooseCourse.setOrderType("700001");
        chooseCourse.setCreateDate(LocalDateTime.now());
        chooseCourse.setCoursePrice(coursePublish.getPrice());
        //一年的有效期
        chooseCourse.setValidDays(365);
        chooseCourse.setStatus("701001");//选课状态
        //开始有效时间
        chooseCourse.setValidtimeStart(LocalDateTime.now());
        //结束有效时间
        chooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(chooseCourse.getValidDays()));

        //插入到选课记录表
        int insert = chooseCourseMapper.insert(chooseCourse);
        if (insert == 0) {
            log.error("插入到选课记录表失败！课程id:{}", coursePublish.getId());
            throw new XuechengPlusException("插入课程：" + coursePublish.getId() + "到选课记录表失败！");
        }
        return chooseCourse;
    }

    /**
     * @param userId
     * @param courseId
     * @return com.xuecheng.learning.model.po.XcCourseTables
     * @description 根据课程和用户查询我的课程表中某一门课程
     * @author Mr.M
     * @date 2022/10/2 17:07
     */
    public XcCourseTables getXcCourseTables(String userId, Long courseId) {
        XcCourseTables xcCourseTables = courseTablesMapper.selectOne(new LambdaQueryWrapper<XcCourseTables>()
                .eq(XcCourseTables::getUserId, userId)
                .eq(XcCourseTables::getCourseId, courseId));
        return xcCourseTables;

    }
}
