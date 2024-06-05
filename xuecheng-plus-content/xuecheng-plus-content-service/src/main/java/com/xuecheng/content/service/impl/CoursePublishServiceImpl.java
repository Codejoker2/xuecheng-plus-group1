package com.xuecheng.content.service.impl;

import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XuechengPlusException;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zengweichuan
 * @description
 * @date 2024/4/17
 */
@Service
@Slf4j
public class CoursePublishServiceImpl implements CoursePublishService {

    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @Autowired
    TeachplanService teachplanService;

    @Resource
    CoursePublishPreMapper coursePublishPreMapper;

    @Resource
    MqMessageService mqMessageService;

    @Resource
    CoursePublishMapper coursePublishMapper;

    @Resource
    CourseBaseMapper courseBaseMapper;

    @Resource
    CoursePublishService coursePublishService;

    @Resource
    MediaServiceClient mediaServiceClient;

    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        //课程基本信息、营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseById(courseId);

        //课程计划信息
        List<TeachplanDto> teachplanTree= teachplanService.getTreeNodes(courseId);

        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfo);
        coursePreviewDto.setTeachplans(teachplanTree);
        return coursePreviewDto;
    }

    @Transactional
    @Override
    public void coursePublish(Long companyId, Long courseId) {
        //约束校验
        //查询课程预发布表
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPre == null){
            XuechengPlusException.cast("请先提交课程审核，审核通过才可以发布");
        }
        //本机构只允许提交本机构的课程
        if(!coursePublishPre.getCompanyId().equals(companyId)){
            XuechengPlusException.cast("不允许提交其它机构的课程。");
        }
        //课程审核状态,审核通过才能发布
        String status = coursePublishPre.getStatus();
        if(!status.equals("202004")){
            XuechengPlusException.cast("审核通过才能发布哦!");
        }
        //保存课程发布信息
        saveCoursePublish(courseId);
        // 向mq_message消息表插入一条消息，消息类型为：course_publish
        saveCoursePublishMessage(courseId);

        //删除课程预发布表的对应记录。
        coursePublishPreMapper.deleteById(courseId);
    }

    @Override
    public File generateCourseHtml(Long courseId) {
        //配置freemarker
        Configuration configuration = new Configuration(Configuration.getVersion());
        File newFile = null;

        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            //加载模板
            //选指定模板路径，classpath下templates下
            //得到classpath路径
            String classPath = this.getClass().getResource("/").getPath();
            configuration.setDirectoryForTemplateLoading(new File(classPath + "/templates/"));

            //设置字符编码
            configuration.setDefaultEncoding("utf-8");

            //指定模板文件名称
            Template template = configuration.getTemplate("course_template.ftl");

            //准备数据
            CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(courseId);
            log.debug("开始进行页面静态化处理");
            Map<String, Object> map = new HashMap<>();
            map.put("model", coursePreviewInfo);

            //静态化、
            //参数：1.模板2，数据模型
            String content = null;
            content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);

            System.out.println(content);

            //静态化完成后就保存到本地
            inputStream = IOUtils.toInputStream(content);
            //输出流
            newFile = new File("D:\\Java\\learn\\temp\\" + courseId + ".html");
            outputStream = new FileOutputStream(newFile);
            //保存到本地
            IOUtils.copy(inputStream, outputStream);
            log.debug("页面静态化处理结束");
        } catch (Exception e) {
            log.error("静态化页面时报错，影响的课程id:{}", courseId, e);
            throw new RuntimeException("课程静态化异常",e);
        } finally {
            try {
                inputStream.close();
                outputStream.close();
            } catch (Exception e) {
                throw new RuntimeException("关闭资源时异常",e);
            }
        }
        return newFile;
    }

    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        String course = mediaServiceClient.uploadCourseFile(multipartFile, "course/" + courseId + ".html");
        if(course==null){
            XuechengPlusException.cast("上传静态文件异常");
        }
    }

    @Override
    public CoursePublish getCoursePublish(Long courseId) {
        return coursePublishMapper.selectById(courseId);
    }

    private void saveCoursePublishMessage(Long courseId) {
        //MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        MqMessage mqMessage = mqMessageService
                .addMessage("course_publish",
                        String.valueOf(courseId),
                        null,
                        null);

        if(mqMessage==null){
            XuechengPlusException.cast(CommonError.UNKOWN_ERROR);
        }
    }

    /**
     * 保存信息到课程发布表,并更新课程基本信息表,删除对应的课程预发布对应的记录
     * @param courseId 课程id
     */
    private void saveCoursePublish(Long courseId){
        //1、向课程发布表course_publish插入一条记录,记录来源于课程预发布表，如果存在则更新，发布状态为：已发布。
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        CoursePublish coursePublishNew = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre,coursePublishNew);

        CoursePublish coursePublishFromDb = coursePublishMapper.selectById(courseId);
        if(coursePublishFromDb == null){
            coursePublishMapper.insert(coursePublishNew);
        }else{
            coursePublishMapper.updateById(coursePublishNew);
        }

        //2、更新course_base表的课程发布状态为：已发布
        CourseBase courseBase = new CourseBase();
        courseBase.setId(courseId);
        courseBase.setStatus("203002");
        courseBaseMapper.updateById(courseBase);
    }
}
