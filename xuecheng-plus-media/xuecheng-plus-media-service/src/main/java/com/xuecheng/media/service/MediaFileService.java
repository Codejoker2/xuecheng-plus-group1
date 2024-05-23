package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;

import java.io.File;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资文件管理业务类
 * @date 2022/9/10 8:55
 */
public interface MediaFileService {

    /**
     * @param pageParams          分页参数
     * @param queryMediaParamsDto 查询条件
     * @return com.xuecheng.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
     * @description 媒资文件查询方法
     * @author Mr.M
     * @date 2022/9/10 8:57
     */
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

    /**
     * 上传文件
     *
     * @param companyId           机构id
     * @param uploadFileParamsDto 上传文件信息
     * @param localFilePath       文件磁盘路径
     * @param objectName       自定义文件路径
     * @return 文件信息
     */
    public UploadFileResultDto uploadCourseFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath, String objectName);

    MediaFiles addMediaFilesToDb(Long companyId, String md5, UploadFileParamsDto uploadFileParamsDto, String bucketFiles, String objectName);

    /**
     * @param fileMd5 文件的md5
     * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
     * @description 检查文件是否存在
     * @author Mr.M
     * @date 2022/9/13 15:38
     */
    public RestResponse<Boolean> checkFile(String fileMd5);

    /**
     * @param fileMd5    文件的md5
     * @param chunkIndex 分块序号
     * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
     * @description 检查分块是否存在
     * @author Mr.M
     * @date 2022/9/13 15:39
     */
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex);

    /**
     * @param fileMd5  文件md5
     * @param chunk    分块序号
     * @param tempPath 临时文件路径
     * @return com.xuecheng.base.model.RestResponse
     * @description 上传分块
     * @author Mr.M
     * @date 2022/9/13 15:50
     */
    public RestResponse uploadChunk(String fileMd5, int chunk, String tempPath);

    /**
     * @param companyId           机构id
     * @param fileMd5             文件md5
     * @param chunkTotal          分块总和
     * @param uploadFileParamsDto 文件信息
     * @return com.xuecheng.base.model.RestResponse
     * @description 合并分块
     * @author Mr.M
     * @date 2022/9/13 15:56
     */
    public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto);

    /**
     * 返回下载后保存到本地的文件路径
     *
     * @param bucket        桶
     * @param mergeFilePath 路径
     * @return
     */
    public File downloadFileFromMinIO(String bucket, String mergeFilePath);

    /**
     * 根据文件名获取mimetype
     *
     * @param fileName
     * @return
     */
    public String getMimeType(String fileName);

    /**
     * 返回是否已经上传到minio
     *
     * @param bucket        桶
     * @param mimeType      文件类型
     * @param objectName    路径
     * @param localFilePath 本地路径
     * @return
     */
    public boolean addMediaFilesToMinIO(String bucket, String mimeType, String objectName, String localFilePath);

    /**
     * @param file
     * @return
     */
    public String getFileMD5(File file);

    /**
     * 根据md5值生成minio文件路径
     *
     * @param fileMd5 md5
     * @param fileExt 文件扩展名
     * @return
     */
    public String getFilePathByMd5(String fileMd5, String fileExt);

    int updateFileURL(MediaFiles mediaFiles);

    MediaFiles selectById(String fileId);

    List<MediaFiles> unUploadCompleteChunk();

    int delUnUploadComplete(List<MediaFiles> mediaFiles);

    MediaFiles getMediaFileById(String mediaId);
}
