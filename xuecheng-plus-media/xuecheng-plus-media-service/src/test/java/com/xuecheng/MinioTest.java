package com.xuecheng;

import io.minio.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;


/**
 * @author zengweichuan
 * @description
 * @date 2024/4/5
 */
public class MinioTest {

    @Test
    public void upload() throws Exception {
        //创建minio连接客户端
        MinioClient minioClient = MinioClient.builder()
                .endpoint("http://192.168.101.65:9001")
                .credentials("minioadmin", "minioadmin")
                .build();

        //判断该桶是否存在
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket("mediafiles").build());
        if(!bucketExists){
            minioClient.makeBucket(MakeBucketArgs.builder().bucket("mediafiles").build());
        }
        System.out.println("mediafiles is already exist");

        //上传
        UploadObjectArgs mediafiles = UploadObjectArgs.builder()
                .bucket("mediafiles").object("Grand Theft Auto V 2024_2_7 19_17_26.png")
                .filename("C:\\Users\\ZWC\\Videos\\Captures\\Grand Theft Auto V 2024_2_7 19_17_26.png")
                .build();
        ObjectWriteResponse objectWriteResponse = minioClient.uploadObject(mediafiles);
        System.out.println(objectWriteResponse);
    }

    @Test
    public void delTest() throws Exception{
        //创建minio连接客户端
        MinioClient minioClient = MinioClient.builder()
                .endpoint("http://192.168.101.65:9001")
                .credentials("minioadmin", "minioadmin")
                .build();

        RemoveObjectArgs mediafiles = RemoveObjectArgs.builder().bucket("mediafiles").object("Grand Theft Auto V 2024_2_7 19_17_26.png").build();

        minioClient.removeObject(mediafiles);
    }

    @Test
    public void seleTest() throws Exception{
        //创建minio连接客户端
        MinioClient minioClient = MinioClient.builder()
                .endpoint("http://192.168.101.65:9001")
                .credentials("minioadmin", "minioadmin")
                .build();

        GetObjectArgs mediafiles = GetObjectArgs.builder().bucket("mediafiles").object("屏幕截图 2024-01-25 170253.png").build();

        FilterInputStream inputStream = minioClient.getObject(mediafiles);
        FileOutputStream outputStream = new FileOutputStream(new File("C:\\Users\\ZWC\\Pictures\\Screenshots\\屏幕截图 2024-01-25 1702531.png"));
        IOUtils.copy(inputStream,outputStream);

        //对下载的文件进行md5校验文件完整性
        String downloadFileMD5 = DigestUtils.md5Hex(new FileInputStream(new File("C:\\Users\\ZWC\\Pictures\\Screenshots\\屏幕截图 2024-01-25 170253.png")));
        String localFileMD5 = DigestUtils.md5Hex(new FileInputStream(new File("C:\\Users\\ZWC\\Pictures\\Screenshots\\屏幕截图 2024-01-25 1702531.png")));


        if(downloadFileMD5.equals(localFileMD5) ){
            System.out.println("下载成功!");
        }

    }
}
