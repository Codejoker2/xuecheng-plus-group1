package com.xuecheng.test;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author zengweichuan
 * @description
 * @date 2024/4/9
 */
@Slf4j
@SpringBootTest
public class MinoTest2 {

    @Resource
    private MinioClient minioClient;

    //上传分块文件
    @Test
    public void uploadChunks() throws Exception {
        //源文件
        File chunkFiles = new File("C:\\Users\\ZWC\\Videos\\chunk\\");
        //分块文件
        File[] files = chunkFiles.listFiles();

        //循环上传到minio
        for (File file : files) {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder().bucket("mediafiles")
                    .object("chunk/" + file.getName())
                    .filename(file.getAbsolutePath()).build();
            minioClient.uploadObject(uploadObjectArgs);
            System.out.println("上传分块成功" + file.getName());
        }
    }

    @Test
    public void merge() throws Exception {
        //调用minio的合并文件api即可
        //分块文件位置
        List<ComposeSource> sources = new ArrayList<>();
        int sourceFileNum = 22;
        for (int i = 1; i <= sourceFileNum; i++) {
            ComposeSource composeSource = ComposeSource.builder().bucket("mediafiles").object("chunk/" + i).build();
            sources.add(composeSource);
        }
        //排序
        sources.sort((o1, o2)->Integer.parseInt(o1.object().substring(o1.object().indexOf('/') + 1)) - Integer.parseInt(o1.object().substring(o1.object().indexOf('/') + 1)));
        //合并后的目标文件位置
        //合并
        ComposeObjectArgs mergeFile = ComposeObjectArgs.builder().bucket("mediafiles").object("merge01.mp4").sources(sources).build();
        minioClient.composeObject(mergeFile);

        //删除分块文件
        delChunkFiles(sources);

    }

    //清除分块文件
    public void delChunkFiles(List<ComposeSource> sources) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        List<DeleteObject> deleteObjects = new ArrayList<>();

        for (ComposeSource source : sources) {
            //object
            String object = source.object();
            deleteObjects.add(new DeleteObject(object));
        }
        //删除分块文件
        RemoveObjectsArgs args = RemoveObjectsArgs.builder().bucket(sources.get(0).bucket()).objects(deleteObjects).build();
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(args);
        //需要进行遍历results,不然无法删除
        results.forEach(r->{
            DeleteError deleteError = null;
            try {
                deleteError = r.get();
            } catch (Exception e) {
                e.printStackTrace();
                log.error("清楚分块文件失败,objectname:{}",deleteError.objectName(),e);
            }
        });
    }

    @Test
    //获取文件信息
    public void getFileInfo() throws Exception {
        //获取合并后的文件大小
        long video = minioClient.statObject(StatObjectArgs.builder().bucket("video").object("/5/4/549be5b53f9d4c71ecd2bba55b002b05/549be5b53f9d4c71ecd2bba55b002b05.mp4").build()).size();
        System.out.println(video);
    }
}
