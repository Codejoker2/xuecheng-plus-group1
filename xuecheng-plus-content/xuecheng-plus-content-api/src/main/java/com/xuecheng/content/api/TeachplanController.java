package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.service.TeachplanService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author zengweichuan
 * @description 课程计划前端控制器
 * @date 2024/4/2
 */
@RestController
@RequestMapping("/teachplan")
public class TeachplanController {
    @Resource
    private TeachplanService teachplanService;

    @GetMapping("/{courseId}/tree-nodes")
    public List<TeachplanDto> getTreeNodes(@PathVariable Long courseId){
        return teachplanService.getTreeNodes(courseId);
    }

    @PostMapping
    public void saveTeachplan(@RequestBody SaveTeachplanDto dto){
        teachplanService.saveTeachplan(dto);
    }

    @DeleteMapping("/{id}")
    public void deleteTeachplan(@PathVariable Long id){
        teachplanService.deleteTeachplan(id);
    }

    @PostMapping("/movedown/{id}")
    public void movedown(@PathVariable Long id){
        teachplanService.movedown(id);
    }
    @PostMapping("/moveup/{id}")
    public void moveup(@PathVariable Long id){
        teachplanService.moveup(id);
    }

    @PostMapping("/association/media")
    public void associationMedia(@RequestBody BindTeachplanMediaDto dto){
        teachplanService.associationMedia(dto);
    }

    @DeleteMapping("/association/media/{teachPlanId}/{mediaId}")
    public void delAssociationMedia(@PathVariable("teachPlanId") String teachPlanId,@PathVariable("mediaId") String mediaId){
        teachplanService.delAssociationMedia(teachPlanId,mediaId);
    }
}
