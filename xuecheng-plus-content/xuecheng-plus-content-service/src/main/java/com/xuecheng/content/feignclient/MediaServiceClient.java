package com.xuecheng.content.feignclient;

import com.xuecheng.content.config.MultipartSupportConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * @description 媒资管理服务远程接口
 * @author Mr.M
 * @date 2022/9/20 20:29
 * @version 1.0
 */
@FeignClient(value = "media-api",configuration = MultipartSupportConfig.class)
public interface MediaServiceClient {

    /*
     @PostMapping(value = "/upload/coursefile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
 public UploadFileResultDto uploadCourseFile(@RequestPart("filedata")MultipartFile filedata)
     */
    @PostMapping(value = "/media/upload/coursefile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadCourseFile(@RequestPart("filedata") MultipartFile filedata,
                                                @RequestParam(value = "objectName", required = false) String objectName);

}
