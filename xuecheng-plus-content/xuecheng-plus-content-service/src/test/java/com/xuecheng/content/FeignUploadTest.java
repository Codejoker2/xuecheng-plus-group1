package com.xuecheng.content;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;

/**
 * @author Mr.M
 * @version 1.0
 * @description 测试使用feign远程上传文件
 * @date 2022/9/20 20:36
 */
@SpringBootTest
public class FeignUploadTest {

    @Resource
    private MediaServiceClient mediaServiceClient;
    @Test
    public void test() {
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(new File("D:\\Java\\learn\\temp\\120.html"));
        mediaServiceClient.uploadCourseFile(multipartFile,"course/120.html");
    }

}
