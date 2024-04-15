package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * @author zengweichuan
 * @description
 * @date 2024/4/2
 */
public interface TeachplanService {
    public List<TeachplanDto> getTreeNodes(Long courseId);

    void saveTeachplan(SaveTeachplanDto dto);

    void deleteTeachplan(Long id);

    public void movedown(Long id);

    public void moveup(Long id);

    public void associationMedia(BindTeachplanMediaDto dto);

    public void delAssociationMedia(String teachPlanId,String mediaId);
}
