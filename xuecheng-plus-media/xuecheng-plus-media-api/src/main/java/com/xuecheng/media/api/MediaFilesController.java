package com.xuecheng.media.api;

import com.xuecheng.base.exception.XuechengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @description 媒资文件管理接口
 * @author Mr.M
 * @date 2022/9/6 11:29
 * @version 1.0
 */
@Slf4j
 @Api(value = "媒资文件管理接口",tags = "媒资文件管理接口")
 @RestController
public class MediaFilesController {


  @Autowired
  MediaFileService mediaFileService;


 @ApiOperation("媒资列表查询接口")
 @PostMapping("/files")
 public PageResult<MediaFiles> list(PageParams pageParams, @RequestBody QueryMediaParamsDto queryMediaParamsDto){
  Long companyId = 1232141425L;
  return mediaFileService.queryMediaFiels(companyId,pageParams,queryMediaParamsDto);

 }

 @PostMapping(value = "/upload/coursefile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
 public UploadFileResultDto uploadCourseFile(@RequestPart("filedata") MultipartFile filedata,
                                             @RequestParam(value = "objectName", required = false) String objectName) {

     Long companyId = 12323123L;

     UploadFileParamsDto dto = new UploadFileParamsDto();
     //文件大小
     dto.setFileSize(filedata.getSize());
     //文件类型为图片
     dto.setFileType("001001");
     //文件名称
     dto.setFilename(filedata.getOriginalFilename());
     //文件大小
     dto.setFileSize(filedata.getSize());
     //创建临时文件
     String absolutePath ;
     try {
         File tempFile = File.createTempFile("minio", "temp");
         //上传文件拷贝到临时文件
         filedata.transferTo(tempFile);
         //临时文件路径
         absolutePath = tempFile.getAbsolutePath();
     }catch (IOException e){
         log.error("在上传图片时创建临时文件失败!,失败信息:{}",e.getMessage(),e);
         throw new XuechengPlusException("上传图片时出现错误,请重试!");
     }
     UploadFileResultDto uploadFileResultDto = mediaFileService.uploadCourseFile(companyId, dto, absolutePath,objectName);


     return uploadFileResultDto;
 }

}
