package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XuechengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/10 8:58
 */
@Slf4j
@Service
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Resource
    private MinioClient minioClient;

    @Value("${minio.bucket.files}")
    private String bucketFiles;

    @Value("${minio.bucket.videofiles}")
    private String bucketVideoFiles;

    @Resource
    private MediaFileService mediaFileService;

    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    //获取文件扩展名
    private String getFileExtendName(String fileName) {
        return fileName.substring(fileName.indexOf('.'));
    }

    //文件mineType
    private String getMimeType(String extension) {
        if (extension == null) extension = "";
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        //通用mimeType字节流
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }
    //获取文件md5
    private String getFileMD5(File file) {
        try{
            return DigestUtils.md5Hex(new FileInputStream(file));
        }catch (Exception e){
            e.getStackTrace();
            return null;
        }
    }
    //文件默认路径设置
    private String getDefaultPath(){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd");
        String folder = format.format(new Date()).replace("-", "/") + "/";
        return folder;
    }
    //将文件上传到minio
    private boolean addMediaFilesToMinIO(String bucket,String mimeType,String objectName,String localFilePath){
        try {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .filename(localFilePath)
                    .contentType(mimeType)
                    .build();
            minioClient.uploadObject(uploadObjectArgs);
            log.debug("上传文件到minio成功,bucket:{},objectName:{}",bucket,objectName);
            return true;
        }catch (Exception e){
            log.debug("上传文件到minio失败,bucket:{},objectName:{},错误原因:{}",bucket,objectName,e.getMessage(),e);
            throw new XuechengPlusException("上传文件到文件系统失败!");
        }
    }
    @Transactional
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName){
        //从数据库中查询文件
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        //文件已存在就不操作数据库
        if (mediaFiles != null)return mediaFiles;
        //拷贝基本信息
        mediaFiles = new MediaFiles();
        BeanUtils.copyProperties(uploadFileParamsDto,mediaFiles);
        mediaFiles.setId(fileMd5);
        mediaFiles.setFileId(fileMd5);
        mediaFiles.setCompanyId(companyId);
        mediaFiles.setUrl("/" + bucket + "/" + objectName);
        mediaFiles.setBucket(bucket);
        mediaFiles.setFilePath(objectName);
        mediaFiles.setCreateDate(LocalDateTime.now());
        mediaFiles.setAuditStatus("002003");
        mediaFiles.setStatus("1");

        int insert = mediaFilesMapper.insert(mediaFiles);
        if (insert <= 0){
            log.error("保存文件到数据库失败,{}", mediaFiles);
            throw new XuechengPlusException("报错文件信息失败!");
        }
        log.error("保存文件到数据库成功,{}", mediaFiles);
        return mediaFiles;

    }

    @Override
    public UploadFileResultDto uploadCourseFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {
        File file = new File(localFilePath);
        if (!file.exists()) throw new XuechengPlusException("文件不存在!");

        //文件名称
        String filename = uploadFileParamsDto.getFilename();
        //文件扩展名
        String extension = getFileExtendName(filename);
        //文件mineType
        String mimeType = getMimeType(extension);
        //文件的md5值
        String md5 = getFileMD5(file);
        //文件默认目录
        String folder = getDefaultPath();
        //存储到minio中的对象名(带目录)
        String objectName = folder + md5 + extension;
        //将文件上传到minio
        addMediaFilesToMinIO(bucketFiles,mimeType,objectName,localFilePath);
        //文件大小设置
        uploadFileParamsDto.setFileSize(file.length());
        //将文件信息存储到数据库
        MediaFiles mediaFiles = mediaFileService.addMediaFilesToDb(companyId, md5, uploadFileParamsDto, bucketFiles, objectName);
        //准备返回数据
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles,uploadFileResultDto);
        return uploadFileResultDto;
    }
}
