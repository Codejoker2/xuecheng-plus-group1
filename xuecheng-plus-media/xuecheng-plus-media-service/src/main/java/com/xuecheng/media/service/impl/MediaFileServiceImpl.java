package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XuechengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
        try {
            return DigestUtils.md5Hex(new FileInputStream(file));
        } catch (Exception e) {
            e.getStackTrace();
            return null;
        }
    }

    //文件默认路径设置
    private String getDefaultPath() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd");
        String folder = format.format(new Date()).replace("-", "/") + "/";
        return folder;
    }

    //将文件上传到minio
    private boolean addMediaFilesToMinIO(String bucket, String mimeType, String objectName, String localFilePath) {
        try {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .filename(localFilePath)
                    .contentType(mimeType)
                    .build();
            minioClient.uploadObject(uploadObjectArgs);
            log.debug("上传文件到minio成功,bucket:{},objectName:{}", bucket, objectName);
            return true;
        } catch (Exception e) {
            log.debug("上传文件到minio失败,bucket:{},objectName:{},错误原因:{}", bucket, objectName, e.getMessage(), e);
            throw new XuechengPlusException("上传文件到文件系统失败!");
        }
    }

    @Transactional
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName) {
        //从数据库中查询文件
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        //文件已存在就不操作数据库
        if (mediaFiles != null) return mediaFiles;
        //拷贝基本信息
        mediaFiles = new MediaFiles();
        BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
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
        if (insert <= 0) {
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
        addMediaFilesToMinIO(bucketFiles, mimeType, objectName, localFilePath);
        //文件大小设置
        uploadFileParamsDto.setFileSize(file.length());
        //将文件信息存储到数据库
        MediaFiles mediaFiles = mediaFileService.addMediaFilesToDb(companyId, md5, uploadFileParamsDto, bucketFiles, objectName);
        //准备返回数据
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
        return uploadFileResultDto;
    }

    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        //1.检查数据库是否存在
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles != null) {
            //2.检查minio上的文件是否存在
            String bucket = mediaFiles.getBucket();
            String filePath = mediaFiles.getFilePath();
            GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket(bucket).object(filePath).build();
            InputStream stream = null;
            try {
                stream = minioClient.getObject(getObjectArgs);
                if (stream != null) {
                    //文件已存在
                    return RestResponse.validfail("文件已存在");
                }
            } catch (Exception e) {

            } finally {
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        //其他情况说明文件不存在
        return RestResponse.success(false);
    }

    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {
        //得到分块文件目录
        String path = getchunkFileFolderPath(fileMd5);
        //得到分块文件的路径
        String chunkFilePath = path + chunkIndex;

        GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket(bucketVideoFiles).object(chunkFilePath).build();
        InputStream stream = null;
        try {
            stream = minioClient.getObject(getObjectArgs);
            if (stream != null) {
                return RestResponse.success(true);
            }
        } catch (Exception e) {

        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        //分块不存在
        return RestResponse.success(false);
    }

    //将大文件分片上传到minio
    @Override
    public RestResponse uploadChunk(String fileMd5, int chunk, String tempPath) {
        //然后将本地文件上传到minio
        //mimetype
        String mimeType = getMimeType(null);
        //获取minio路径
        String chunkPath = getchunkFileFolderPath(fileMd5) + chunk;
        try {
            addMediaFilesToMinIO(bucketVideoFiles, mimeType, chunkPath, tempPath);
            return RestResponse.success(true);
        } catch (Exception e) {
            e.printStackTrace();
            log.debug("上传分块文件:{},失败:{}", chunkPath, e);
        }
        return RestResponse.success(false);
    }

    //得到分块文件目录(md5的前两个字母分别是是前两个路径,再加上chunk, + 文件号 i)
    private String getchunkFileFolderPath(String fileMd5) {
        String firstPath = fileMd5.substring(0, 1) + "/";
        //获取minio目录信息
        String secPath = fileMd5.substring(1, 2) + "/";
        return firstPath + secPath + "chunk/";
    }

    @Override
    public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {

        //合并分块
        String mergeFilePath = mergeMinioChunks(fileMd5, uploadFileParamsDto, chunkTotal);

        //获取合并后的文件大小
        Long fileSize = 0L;
        try {
            fileSize = minioClient.statObject(StatObjectArgs.builder().bucket(bucketVideoFiles).object(mergeFilePath).build()).size();
            uploadFileParamsDto.setFileSize(fileSize);
        } catch (Exception e) {

        }

        //进行md5值校验
        boolean b = compareLocalAndMinoMd5(bucketVideoFiles, mergeFilePath);
        if (!b) return RestResponse.validfail(false, "文件合并校验失败,最终上传失败.");

        //将文件保存到媒资数据库
        mediaFileService.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucketVideoFiles, mergeFilePath);

        //合并及保存数据库后删除分块文件
        delChunkFiles(fileMd5, chunkTotal);

        return RestResponse.success(true);
    }

    //返回合并后文件的路径
    private String mergeMinioChunks(String fileMd5, UploadFileParamsDto uploadFileParamsDto, int chunkTotal) {
        List<ComposeSource> sources = new ArrayList<>();
        //分块文件目录
        String chunkFileFolderPath = getchunkFileFolderPath(fileMd5);
        //获取文件后缀
        String extendName = getFileExtendName(uploadFileParamsDto.getFilename());
        //获取mimetype
        String mimeType = getMimeType(extendName);
        //合并后的文件目录
        String mergeFilePath = getFilePathByMd5(fileMd5, extendName);
        //合并各个分块文件
        for (int i = 0; i < chunkTotal; i++) {
            ComposeSource composeSource = ComposeSource.builder().bucket(bucketVideoFiles).object(chunkFileFolderPath + i).build();
            sources.add(composeSource);
        }
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket(bucketVideoFiles)
                .object(mergeFilePath)
                .sources(sources).build();
        try {
            //开始合并
            minioClient.composeObject(composeObjectArgs);
        } catch (Exception e) {
            log.error("文件分块路径{},合并文件时出现错误:{}", chunkFileFolderPath, e);
            throw new XuechengPlusException("上传文件时遇到错误,请稍后重试!");
        }
        return mergeFilePath;
    }

    /**
     * 得到合并后的文件的地址
     *
     * @param fileMd5 文件id即md5值
     * @param fileExt 文件扩展名
     * @return
     */
    private String getFilePathByMd5(String fileMd5, String fileExt) {
        String firstPath = fileMd5.substring(0, 1) + "/";
        String secPath = fileMd5.substring(1, 2) + "/";

        return firstPath + secPath + fileMd5 + "/" + fileMd5 + fileExt;
    }

    // 删除有bug,在断点续传后删除时总是卡住,总是在关闭springboot服务器时才真正删除(把能关闭的流都关闭了,不然有各种各样的bug)
    private void delChunkFiles(String fileMd5, int chunkTotal) {

        List<DeleteObject> deleteObjects = new ArrayList<>();

        String chunkFilePath = getchunkFileFolderPath(fileMd5);
        for (int i = 0; i < chunkTotal; i++) {
            DeleteObject deleteObject = new DeleteObject(chunkFilePath + i);
            deleteObjects.add(deleteObject);
        }

        RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket(bucketVideoFiles).objects(deleteObjects).build();
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
        //进行遍历才能删除
        for (Result<DeleteError> result : results) {
            DeleteError deleteError = null;
            try {
                deleteError = result.get();
            } catch (Exception e) {
                e.printStackTrace();
                log.error("清除分块文件失败,objectname:{}", deleteError.objectName(), e);
            }
        }
    }

    //从minio下载文件
    private File downloadFileFromMinIO(String bucket, String mergeFilePath) {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket(bucket).object(mergeFilePath).build();
        FilterInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            //下载文件位置
            File outputFile = File.createTempFile("minio", ".merge");
            //将文件进行下载
            inputStream = minioClient.getObject(getObjectArgs);
            outputStream = new FileOutputStream(outputFile);
            IOUtils.copy(inputStream, outputStream);
            return outputFile;
        } catch (Exception e) {
            log.debug("下载合并后文件失败,mergeFilePath:{}", mergeFilePath);
            throw new XuechengPlusException("下载合并后文件失败.");
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean compareLocalAndMinoMd5(String bucket, String mergeFilePath) {
        //下载文件
        File filePath = downloadFileFromMinIO(bucket, mergeFilePath);
        //从文件路径获取原文件的md5值
        String md5FromMergeFilePath = getMD5FromMergeFilePath(mergeFilePath);

        FileInputStream inputStream = null;
        //从本地文件获取md5值
        try {
            inputStream = new FileInputStream(filePath);
            String downFileMd5 = DigestUtils.md5Hex(inputStream);

            inputStream.close();
            if (md5FromMergeFilePath.equals(downFileMd5)) return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    //从文件路径上取出md5值
    private String getMD5FromMergeFilePath(String mergeFilePath) {
        int start = mergeFilePath.lastIndexOf('/') + 1;
        int end = mergeFilePath.indexOf(".");
        return mergeFilePath.substring(start, end);
    }

}
