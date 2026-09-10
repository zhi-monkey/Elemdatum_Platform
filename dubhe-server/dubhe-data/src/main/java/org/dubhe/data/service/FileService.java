package org.dubhe.data.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dubhe.biz.base.vo.ProgressVO;
import org.dubhe.biz.file.dto.FileDTO;
import org.dubhe.biz.file.dto.FilePageDTO;
import org.dubhe.data.domain.bo.FileAnnotationBO;
import org.dubhe.data.domain.bo.TaskSplitBO;
import org.dubhe.data.domain.dto.*;
import org.dubhe.data.domain.entity.Dataset;
import org.dubhe.data.domain.entity.File;
import org.dubhe.data.domain.entity.Task;
import org.dubhe.data.domain.vo.*;
import org.dubhe.data.machine.enums.FileStateEnum;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @description 文件信息服务
 * @date 2020-04-10
 */
public interface FileService {

    /**
     * 文件详情
     *
     * @param fileId    文件id
     * @param datasetId 数据集id
     * @return FileVO 文件详情
     */
    FileVO get(Long fileId, Long datasetId);

    /**
     * 文件查询
     *
     * @param datasetId         数据集id
     * @param page              分页条件
     * @param fileQueryCriteria 查询文件参数
     * @return Map<String, Object> 文件查询列表
     */
    Map<String, Object> listPage(Long datasetId, Page page, FileQueryCriteriaVO fileQueryCriteria);

    Integer getAbsoluteOffsetForAnnotationStatus(Long datasetId, Integer annotationStatus, Integer startOffset, Integer total);

    /**
     * 文件查询，物体检测标注页面使用
     *
     * @param datasetId 数据集id
     * @param offset    offset
     * @param limit     页容量
     * @param page      分页条件
     * @param type      文件类型
     * @return Page<File> 文件查询列表
     */
    Page<File> listByLimit(Long datasetId, Long offset, Integer limit, Integer page, Integer[] type, Long[] labelId);

    /**
     * 获取offset
     *
     * @param datasetId 数据集id
     * @param fileId    文件id
     * @param type      文件类型
     * @return Integer 获取到的offset
     */
    Integer getOffset(Long fileId, Long datasetId, Integer[] type, Long[] labelIds);

    /**
     * 获取首个文件
     *
     * @param datasetId 数据集id
     * @param type      文件类型
     * @return Long 获取首个文件
     */
    Long getFirst(Long datasetId, String versionName, Integer type);


    /**
     * 获取文件对应所有增强文件
     *
     * @param fileId    文件id
     * @param datasetId 数据集id
     * @return List<File> 获取文件对应所有增强文件列表
     */
    List<File> getEnhanceFileList(Long fileId, Long datasetId);

    /**
     * 视频采样任务
     */
    void videoSample(String finishedQueue, String failedQueue);

    /**
     * 更新文件状态
     *
     * @param files          文件集合
     * @param fileStatusEnum 文件状态
     * @return int 更新结果是否成功
     */
    int update(Collection<File> files, FileStateEnum fileStatusEnum);

    /**
     * 根据文件ID获取文件内容
     *
     * @param fileId 文件ID
     * @return 文件实体
     */
    File selectById(Long fileId, Long datasetId);

    /**
     * 根据查询条件获取第一个文件
     *
     * @param queryWrapper 查询条件
     * @return 文件详情
     */
    File selectOne(QueryWrapper<File> queryWrapper);


    /**
     * 如果ids为空，则返回空
     *
     * @param fileIds   文件id集合
     * @param datasetId 数据集ID
     * @return Set<File>    获取到的文件
     */
    Set<File> get(List<Long> fileIds, Long datasetId);

    /**
     * 批量获取文件并返回Map
     *
     * @param fileIds   文件id集合
     * @param datasetId 数据集ID
     * @return Map<Long, FileVO> key为文件ID，value为文件VO
     */
    Map<Long, FileVO> batchGetFileMap(List<Long> fileIds, Long datasetId);

    /**
     * 保存文件
     *
     * @param fileId 文件id
     * @param files  file文件
     * @return List<Long>   保存文件数量
     */
    List<File> saveFiles(Long fileId, List<FileCreateDTO> files);

    /**
     * 数据集标注进度
     *
     * @param datasets 数据集
     * @return Map<Long, ProgressVO> 数据集标注进度
     */
    Map<Long, ProgressVO> listStatistics(List<Dataset> datasets);

    /**
     * 删除文件
     *
     * @param datasetId 数据集id
     */
    void delete(Long datasetId);

    /**
     * 判断视频数据集是否已存在视频
     *
     * @param datasetId 数据集id
     */
    void isExistVideo(Long datasetId);

    /**
     * 保存视频文件
     *
     * @param fileId fileId
     * @param files  file文件
     * @param type   文件类型
     * @param pid    文件父id
     * @param userId 用户id
     * @return List<File> 文件列表
     */
    List<File> saveVideoFiles(Long fileId, List<FileCreateDTO> files, int type, Long pid, Long userId);


    /**
     * 将整体任务分割
     *
     * @param files file文件
     * @param task  任务
     * @return List<TaskSplitBO> 分割后的任务
     */
    List<TaskSplitBO> split(Collection<File> files, Task task);

    /**
     * 根据条件获取文件列表
     *
     * @param wrapper 查询条件
     * @return
     */
    List<File> listFile(QueryWrapper<File> wrapper);

    /**
     * 批量获取数据集文件
     *
     * @param datasetId 数据集ID
     * @param offset    偏移量
     * @param batchSize 批大小
     * @param status    文件标注状态
     * @return 文件列表
     */
    List<File> listBatchFile(Long datasetId, int offset, int batchSize, Collection<Integer> status);

    /**
     * 采样任务过期
     */
    void expireSampleTask();

    /**
     * 根据版本和数据集ID获取文件url
     *
     * @param datasetId   数据集ID
     * @param versionName 版本名
     * @return List<String> url列表
     */
    List<String> selectUrls(Long datasetId, String versionName);

    List<String> selectUrlsFromFileStatusNormalOrDeleted(Long datasetId, String versionName);

    /**
     * 根据version.changed获取文件name列表
     *
     * @param datasetId   数据集ID
     * @param changed     版本文件是否改动
     * @param versionName 版本名称
     * @return List<FileAnnotationBO>   文件列表
     */
    List<FileAnnotationBO> selectFileAnnotations(Long datasetId, Integer changed, String versionName);

    /**
     * 音频数据集文件查询
     *
     * @param datasetId         数据集id
     * @param page              分页条件
     * @param fileQueryCriteria 查询文件参数
     * @return Map<String, Object> 文件查询列表
     */
    Map<String, Object> audioFilesByPage(Long datasetId, Page page, FileQueryCriteriaVO fileQueryCriteria);

//    /**
//     * 文本数据集文件查询
//     *
//     * @param datasetId         数据集id
//     * @param page              分页条件
//     * @param fileQueryCriteria 查询文件参数
//     * @return Map<String, Object> 文件查询列表
//     */
//    Map<String, Object> txtContentByPage(Long datasetId, Page page, FileQueryCriteriaVO fileQueryCriteria);

    /**
     * 文本状态数量统计
     *
     * @param datasetId               数据集ID
     * @param fileScreenStatSearchDTO 文件查询条件
     * @return 文本状态数量统计
     */
    FileScreenStatVO getFileCountByStatus(Long datasetId, FileScreenStatSearchDTO fileScreenStatSearchDTO);

    /**
     * 获取数据集文件数量
     *
     * @param datasetId 数据集ID
     * @return 数据集文件数量
     */
    int getFileCountByDatasetId(Long datasetId);

    /**
     * 获取数据集原图文件数量
     *
     * @param datasetId   数据集ID
     * @param versionName 版本名称
     * @return 数据集原图文件数量
     */
    int getOriginalFileCountOfDataset(Long datasetId, String versionName);


    /**
     * 备份数据集文件数据
     *
     * @param originDataset 原数据集实体
     * @param targetDataset 目标数据集实体
     * @return 新生成文件数据
     */
    List<File> backupFileDataByDatasetId(Dataset originDataset, Dataset targetDataset);

    /**
     * 文本数据集csv导入
     *
     * @param datasetCsvImportDTO 导入信息
     */
    void tableImport(DatasetCsvImportDTO datasetCsvImportDTO);


//    /**
//     * 将文本数据同步至ES
//     *
//     * @param dataset        数据集
//     * @param fileIdsNotToEs 需要同步的文件ID
//     */
//    void transportTextToEs(Dataset dataset, List<Long> fileIdsNotToEs, Boolean ifImport);
//
//    /**
//     * 还原es_transport状态
//     *
//     * @param datasetId 数据集ID
//     * @param fileId    文件ID
//     */
//    void recoverEsStatus(Long datasetId, Long fileId);
//
//    /**
//     * 删除es中数据
//     *
//     * @param fileIds 文件ID数组
//     */
//    void deleteEsData(Long[] fileIds);

    /**
     * 获取文件列表
     *
     * @param datasetId   数据集ID
     * @param prefix      匹配前缀
     * @param recursive   是否递归
     * @param versionName 版本
     * @return List<FileListDTO> 文件列表
     */
    List<FileDTO> fileList(Long datasetId, String prefix, boolean recursive, String versionName, boolean isVersionFile);

    /**
     * 分页获取文件列表
     *
     * @param filePageDTO 文件查询和响应实体
     * @param datasetId   数据集ID
     */
    void filePage(FilePageDTO filePageDTO, Long datasetId);

    /**
     * 根据版本和数据集ID获取文件url width height
     *
     * @param datasetId   数据集ID
     * @param versionName 版本名
     * @return List<String> url列表
     */
    List<FileAnnotationBO> listByDatasetIdAndVersionName(Long datasetId, String versionName);

    /**
     * 根据数据集id获取数据集中的图片（不依赖数据集版本）
     *
     * @param datasetId 数据集ID
     * @return List<ImageFileDTO> 图片信息
     */
    IPage<ImageFileVO> listImages(Long datasetId, Page page, FileQueryCriteriaVO queryCriteria);

    /**
     * 根据数据集id获取数据集中的视频（不依赖数据集版本）
     *
     * @param datasetId 数据集ID
     * @return List<ImageFileDTO> 视频信息
     */
    IPage<VideoFileVO> listVideos(Long datasetId, Integer page, Integer pageSize, Integer status, String name, List<Long> createTime, String order);


    /**
     * 根据数据集id获取数据集中视频的数量及标注信息（不依赖数据集版本）
     *
     * @param datasetId 数据集ID
     * @return ImageStatisticsDTO 视频信息
     */
    VideoStatisticsDTO getVideoStatistics(Long datasetId);

    /**
     * 保存zlm录制的视频信息
     *
     * @param mp4RecordDTO 数据集ID
     */
    void saveVideoInfo(MP4RecordDTO mp4RecordDTO);

    //获取数据集内的图片总大小
    Long getImagesSize(Long datasetId, String versionName);

    //获取数据集内的视频总大小
    Long getVideosSize(Long datasetId);

    // 获取文件标注信息
    String getFileAnnotation(Long fileId, Long datasetId, String labelType);

    // 获取YOLO格式的标签文本文件
    String getYoloLabelsFile(Long datasetId);

    /**
     * 文件查询，物体检测标注页面使用，带annotation字段
     *
     * @param datasetId 数据集ID
     * @param offset    Offset
     * @param limit     页容量
     * @param page      分页条件
     * @param type      数据集类型
     * @param labelId   标签ID数组
     * @param labelType 标签类型(VOC/YOLO)
     * @return Page<JSONObject> 带annotation字段的文件查询分页列表
     */
    Page<JSONObject> listByLimitWithAnnotation(Long datasetId, String versionName, Long offset, Integer limit, Integer page, Integer[] type, Long[] labelId, String labelType);


    Integer getVersionImgCount(Long datasetId, String versionName);

    /**
     * 根据URL批量获取文件ID
     */
    Map<String, Long> getFileIdsByUrls(Long datasetId, List<String> urls);

    /**
     * 获取视频文件信息
     * 
     * @param datasetId 数据集ID
     * @param fileId 文件ID
     * @return 视频信息
     */
    VideoInfoVO getVideoInfo(Long datasetId, Long fileId);

}
