package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.base.exception.XuechengPlusException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author zengweichuan
 * @description
 * @date 2024/4/2
 */
@Service
public class TeachplanServiceImpl implements TeachplanService {

    @Resource
    private TeachplanMapper teachplanMapper;

    @Resource
    private TeachplanMediaMapper teachplanMediaMapper;

    @Override
    public List<TeachplanDto> getTreeNodes(Long courseId) {

        //按照课程id查询课程计划
        List<TeachplanDto> teachplans = teachplanMapper.selectTeachPlanByCourseId(courseId);

        //生成树形结构
        //1.找根节点
        List<TeachplanDto> roots = teachplans.stream().filter(teachplan -> teachplan.getParentid() == 0L).collect(Collectors.toList());
        //2.排序
        roots.sort((o1, o2) -> o1.getOrderby() - o2.getOrderby());

        //2.找到根节点下的所有节点以及其子节点
        roots.stream().forEach(teachplanDto -> combineTree(teachplanDto,teachplans));
        return roots;
    }

    @Transactional
    @Override
    public void saveTeachplan(SaveTeachplanDto dto) {

        //1.判断是新增/修改,根据前端传过来的数据判断
        //新增
        if(dto.getId() == null){
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(dto,teachplan);
            //设置同级别排序字段(默认是最后一个章(节),需要进行当前章(节)数目计算,才能确定,是章(节)数 + 1)
            Integer orderNum = orderbyCount(dto);
            teachplan.setOrderby(orderNum);

            int insert = teachplanMapper.insert(teachplan);
            if(insert <= 0)throw new XuechengPlusException("新增章(节)失败!");
        }
        //修改
        else{
            Teachplan teachplan = teachplanMapper.selectById(dto.getId());
            BeanUtils.copyProperties(dto,teachplan);
            teachplan.setChangeDate(LocalDateTime.now());
            int update = teachplanMapper.updateById(teachplan);
            if(update <= 0)throw new XuechengPlusException("更新章(节)失败!");
        }
    }

    @Transactional
    @Override
    public void deleteTeachplan(Long id) {
        Teachplan teachplan = teachplanMapper.selectById(id);

        if(isChapter(teachplan)){
            delChapter(teachplan);
        }else{
            //2.节则可以直接删除
            delSection(id);
        }
    }

    @Override
    public void movedown(Long id) {
        Teachplan currentPlan = teachplanMapper.selectById(id);
        //如果已经是最后一个了，就不要向下移动了
        //计数同层次的个数是否与orderby相等，相等就是最后一个
        Integer num = teachplanMapper
                .selectCount(new LambdaQueryWrapper<Teachplan>()
                .eq(Teachplan::getCourseId, currentPlan.getCourseId())
                .eq(Teachplan::getParentid, currentPlan.getParentid()));
        if(num.equals(currentPlan.getOrderby())){
            throw new XuechengPlusException("如果已经是最后一个了，就不要向下移动了");
        }
        //查询他的下一节
        Teachplan downPlan = teachplanMapper.selectOne(new LambdaQueryWrapper<Teachplan>()
                .eq(Teachplan::getCourseId, currentPlan.getCourseId())
                .eq(Teachplan::getParentid, currentPlan.getParentid())
                .eq(Teachplan::getOrderby, currentPlan.getOrderby() + 1));
        //交换两个章节的orderBy即可
        Integer temp = currentPlan.getOrderby();
        currentPlan.setOrderby(downPlan.getOrderby());
        downPlan.setOrderby(temp);

        teachplanMapper.updateById(currentPlan);
        teachplanMapper.updateById(downPlan);
    }

    @Override
    public void associationMedia(BindTeachplanMediaDto dto) {

        //教学计划id
        Long teachplanId = dto.getTeachplanId();
        //查询教学计划
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if(teachplan==null){
            new XuechengPlusException("教学计划不存在");
        }
        Integer grade = teachplan.getGrade();
        if(grade!=2){
            new XuechengPlusException("只允许第二级教学计划绑定媒资文件");
        }

        LambdaQueryWrapper<TeachplanMedia> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeachplanMedia::getMediaId,dto.getMediaId());
        TeachplanMedia teachplanMedia = teachplanMediaMapper.selectOne(wrapper);
        //删除原有的绑定关系
        if(teachplanMedia != null){
            teachplanMediaMapper.deleteById(teachplanMedia.getId());
        }
        //新增现有的绑定

        Long courseId = teachplan.getCourseId();
        String fileName = dto.getFileName();
        String mediaId = dto.getMediaId();
        TeachplanMedia newTeachPlanMedia = new TeachplanMedia();
        newTeachPlanMedia.setMediaId(mediaId);
        newTeachPlanMedia.setTeachplanId(teachplanId);
        newTeachPlanMedia.setCourseId(courseId);
        newTeachPlanMedia.setMediaFilename(fileName);
        newTeachPlanMedia.setCreateDate(LocalDateTime.now());

        teachplanMediaMapper.insert(newTeachPlanMedia);

    }

    @Override
    public void delAssociationMedia(String teachPlanId, String mediaId) {
        LambdaQueryWrapper<TeachplanMedia> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeachplanMedia::getTeachplanId,teachPlanId);
        wrapper.eq(TeachplanMedia::getMediaId,mediaId);
        teachplanMediaMapper.delete(wrapper);
    }

    @Override
    public void moveup(Long id) {
        Teachplan currentPlan = teachplanMapper.selectById(id);
        //如果本来就是第一个就不要移动了
        if(currentPlan.getOrderby().equals(1)){
            throw new XuechengPlusException("已经是第一个了，不能再往上移动了！");
        }
        //查询他的下一节
        Teachplan upPlan = teachplanMapper.selectOne(new LambdaQueryWrapper<Teachplan>()
                .eq(Teachplan::getCourseId, currentPlan.getCourseId())
                .eq(Teachplan::getParentid, currentPlan.getParentid())
                .eq(Teachplan::getOrderby, currentPlan.getOrderby() - 1));
        //交换两个章节的orderBy即可
        Integer temp = currentPlan.getOrderby();
        currentPlan.setOrderby(upPlan.getOrderby());
        upPlan.setOrderby(temp);

        teachplanMapper.updateById(currentPlan);
        teachplanMapper.updateById(upPlan);
    }

    //判断是否是章
    private boolean isChapter(Teachplan teachplan){
        return teachplan.getParentid() == 0L;
    }
    //删除章的处理
    //需要先删除所有节,才能删除章
    private void delChapter(Teachplan teachplan){
        //以当前章为父节点的节个数,为0就没有
        Integer count = teachplanMapper.selectCount(new LambdaQueryWrapper<Teachplan>().eq(Teachplan::getParentid, teachplan.getId()));
        XuechengPlusException xuechengPlusException = new XuechengPlusException("120409", "课程计划信息还有子级信息，无法操作");
        if(count > 0)throw new XuechengPlusException("120409","课程计划信息还有子级信息，无法操作");

        teachplanMapper.deleteById(teachplan.getId());

    }
    private void delSection(Long id){
        //删除节
        teachplanMapper.deleteById(id);
        //删除节关联的信息
        LambdaQueryWrapper<TeachplanMedia> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeachplanMedia::getTeachplanId,id);

        teachplanMediaMapper.delete(wrapper);
    }

    private void combineTree(TeachplanDto root,List<TeachplanDto> teachplans){

        //设置teachplanMedia
        root.setTeachplanMedia(teachplanMediaMapper
                .selectOne(new LambdaQueryWrapper<TeachplanMedia>()
                        .eq(TeachplanMedia::getTeachplanId,root.getId())));

        //递归结束条件
        List<TeachplanDto> nodes = hasNodes(root, teachplans);
        if(nodes.size() <= 0)return;

        //如果有子节点,就添加
        root.setTeachPlanTreeNodes(new ArrayList<>());
        root.getTeachPlanTreeNodes().addAll(nodes);
        //排序
        root.getTeachPlanTreeNodes().sort((o1, o2) -> o1.getOrderby() - o2.getOrderby());

        //继续向下递归找子节点
        for (TeachplanDto teachPlanTreeNode : root.getTeachPlanTreeNodes()) {
            combineTree(teachPlanTreeNode,teachplans);
        }

    }
    //查找该节点下所有子节点
    private List<TeachplanDto> hasNodes(TeachplanDto root,List<TeachplanDto> teachplans){
        List<TeachplanDto> nodes = teachplans.stream().filter(teachplan -> Objects.equals(teachplan.getParentid(), root.getId())).collect(Collectors.toList());
        return nodes;
    }

    //计算orderby的值
    private Integer orderbyCount(SaveTeachplanDto dto){
        Integer orderNum  = teachplanMapper.selectCount(new LambdaQueryWrapper<Teachplan>()
                    .eq(Teachplan::getCourseId, dto.getCourseId())
                    .eq(Teachplan::getParentid,dto.getParentid()));
        return orderNum + 1;
    }
}
