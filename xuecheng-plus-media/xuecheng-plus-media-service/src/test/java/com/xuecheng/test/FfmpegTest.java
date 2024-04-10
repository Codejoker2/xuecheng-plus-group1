package com.xuecheng.test;

import com.xuecheng.base.utils.Mp4VideoUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * @author zengweichuan
 * @description
 * @date 2024/4/10
 */
public class FfmpegTest {

    //使用cmd开启一个应用
    @Test
    public void test() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("C:\\Program Files\\Tencent\\WeChat\\WeChat.exe");
        //将标准输入流和错误输入流合并，通过标准输入流程读取信息
        processBuilder.redirectErrorStream(true);
        Process p = processBuilder.start();
    }

    @Test
    public void ffmpegTest(){
        //ffmpeg的路径
        String ffmpeg_path = "D:\\soft\\ffmpeg\\ffmpeg.exe";//ffmpeg的安装位置
        //源avi视频的路径
        String video_path = "D:\\develop\\bigfile_test\\nacos01.avi";
        //转换后mp4文件的名称
        String mp4_name = "nacos01.mp4";
        //转换后mp4文件的路径
        String mp4_path = "D:\\develop\\bigfile_test\\nacos01.mp4";
        //创建工具类对象
        Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpeg_path,video_path,mp4_name,mp4_path);
        //开始视频转换，成功将返回success
        String s = videoUtil.generateMp4();
        System.out.println(s);
    }
}
