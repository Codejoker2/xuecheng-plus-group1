package com.xuecheng.test;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zengweichuan
 * @description 大文件分块与合并测试
 * @date 2024/4/5
 */
public class BigFileTest {



    //测试大文件分块
    @Test
    public void testChunk() throws IOException {
        //源文件
        File sourceFile = new File("C:\\Users\\ZWC\\Videos\\1.mp4");
        //大文件分块
        //块大小
        long chunkSize = 1024 * 1024 * 5;
        //分块数量
        long chunkNum = (long) Math.ceil(sourceFile.length() * 1.0 / chunkSize);
        //存放在哪里
        String targetPath = "C:\\Users\\ZWC\\Videos\\chunk\\";
        //缓存区
        byte[] buffer = new byte[1024];

        //使用RandomAccessFile读文件
        RandomAccessFile raf_read = new RandomAccessFile(sourceFile, "rw");

        //读一块内容就创建一个块文件
        for (long i = 1; i <= chunkNum; i++) {
            //块文件名
            File file = new File(targetPath + i);
            //存在就删除
            if (file.exists()) file.delete();
            //如果文件创建失败就跳出循环,否则创建成功继续
            if (!file.createNewFile()) break;

            //向分块文件中写数据
            RandomAccessFile raf_write = new RandomAccessFile(file, "rw");
            int len = -1;
            while ((len = raf_read.read(buffer)) != -1) {
                raf_write.write(buffer,0,len);
                if(file.length() >= chunkSize)break;
            }
            raf_write.close();
            System.out.println("完成分块+" + i);
        }
        raf_read.close();
    }
    //测试块文件合并
    @Test
    public void merge() throws IOException {
        //分块文件位置
        String chunkFilePath = "C:\\Users\\ZWC\\Videos\\chunk\\";
        //拿到所有的块文件
        File[] files = new File(chunkFilePath).listFiles();
        //进行排序并按照文件名排序
        List<File> chunkFiles = Arrays.stream(files)
                .sorted((o1, o2) -> Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName()))
                .collect(Collectors.toList());

        //目标文件路径
        String targetPath = "C:\\Users\\ZWC\\Videos\\merge\\1_1.mp4";
        //向目标文件写数据
        RandomAccessFile raf_write = new RandomAccessFile(new File(targetPath), "rw");

        //缓冲区
        byte[] buffer = new byte[1024];
        //读取每个块文件,并都写入到raf_write文件中
        for (File chunkFile : chunkFiles) {
            RandomAccessFile raf_read = new RandomAccessFile(chunkFile, "r");
            int len = -1;
            while ((len = raf_read.read(buffer)) != -1){
                raf_write.write(buffer,0,len);
            }
            raf_read.close();
        }
        raf_write.close();

        //取出源文件的md5
        String mergeFileMD5 = DigestUtils.md5Hex(new FileInputStream(new File("C:\\Users\\ZWC\\Videos\\merge\\1_1.mp4")));
        String sourceFileMD5 = DigestUtils.md5Hex(new FileInputStream(new File("C:\\Users\\ZWC\\Videos\\1.mp4")));

        if (sourceFileMD5.equals(mergeFileMD5)){
            System.out.println("分块文件合并成功!");
        }
    }
}
