/**
 * Copyright 2020 Tianshu AI Platform. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =============================================================
 */

package org.dubhe.data.rest;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.constant.ResponseCode;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.db.utils.PageUtil;
import org.dubhe.biz.file.dto.FilePageDTO;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.biz.file.utils.MinioWebTokenBody;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.constant.FileTypeEnum;
import org.dubhe.data.domain.dto.*;
import org.dubhe.data.domain.entity.Dataset;
import org.dubhe.data.domain.entity.DatasetOperationEvent;
import org.dubhe.data.domain.entity.DatasetUploadTask;
import org.dubhe.data.domain.vo.*;
import org.dubhe.data.service.DatasetOperationEventService;
import org.dubhe.data.service.DatasetService;
import org.dubhe.data.service.DatasetUploadTaskService;
import org.dubhe.data.service.ImportTransferTaskService;
import org.dubhe.data.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * @description 文件管理
 * @date 2020-04-10
 */
@Slf4j
@Api(tags = "数据处理：文件管理")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/datasets")
public class FileController {

    /**
     * 文件服务实现类
     */
    @Autowired
    private FileService fileService;

    /**
     * 数据集服务实现类
     */
    @Autowired
    private DatasetService datasetService;

    /**
     * minIO端操作
     */
    @Autowired
    private MinioWebTokenBody minioWebTokenBody;

    /**
     * minIO桶名
     */
    @Value("${minio.bucketName}")
    private String bucketName;

    /**
     * 失效时间
     */
    @Value("${minio.presignedUrlExpiryTime}")
    private Integer expiry;

    /**
     * minIO工具类
     */
    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private DatasetUploadTaskService datasetUploadTaskService;

    @Autowired
    private ImportTransferTaskService importTransferTaskService;

    @Autowired
    private DatasetOperationEventService datasetOperationEventService;


    @ApiOperation(value = "文件提交（异步）")
    @PostMapping(value = "/{datasetId}/files")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody upload(@PathVariable(name = "datasetId") Long datasetId, @Validated @RequestBody BatchFileCreateDTO batchFileCreateDTO) {
        // 1. 创建任务
        Integer fileCount = batchFileCreateDTO.getFiles() != null ? 
                           batchFileCreateDTO.getFiles().size() : 0;
        String taskId = datasetUploadTaskService.createTask(datasetId, fileCount);
        Dataset dataset = datasetService.getOneById(datasetId);
        String datasetName = dataset == null || dataset.getName() == null || dataset.getName().trim().isEmpty()
                ? "图片数据集" : dataset.getName().trim();
        org.dubhe.data.domain.dto.ImportTransferTaskCreateDTO transferTask = new org.dubhe.data.domain.dto.ImportTransferTaskCreateDTO();
        transferTask.setTaskName("数据集“" + datasetName + "”导入任务");
        transferTask.setDatasetType("IMAGE");
        transferTask.setDatasetId(datasetId);
        transferTask.setSourceType("LOCAL");
        transferTask.setTotalFiles(fileCount);
        Long transferTaskId = importTransferTaskService.create(transferTask);
        final int finalFileCount = fileCount;
        
        // 触发开始上传事件
        datasetOperationEventService.createAndInsertEvent(
                datasetId,
                new Date(),
                "数据集开始上传图片/视频",
                DatasetOperationEvent.EventType.INFO,
                DatasetOperationEvent.OperationType.DATA_UPLOAD
        );
        
        // 2. 异步处理文件保存（使用 CompletableFuture，不需要线程池配置）
        CompletableFuture.runAsync(() -> {
            try {
                datasetService.uploadFiles(datasetId, batchFileCreateDTO);
                datasetUploadTaskService.updateTaskSuccess(taskId);
                importTransferTaskService.complete(transferTaskId, "图片导入完成");
                
                // 触发上传完成事件
                datasetOperationEventService.createAndInsertEvent(
                        datasetId,
                        new Date(),
                        "数据集上传图片/视频完成，共上传" + finalFileCount + "个文件",
                        DatasetOperationEvent.EventType.INFO,
                        DatasetOperationEvent.OperationType.DATA_UPLOAD
                );
            } catch (Exception e) {
                log.error("上传文件处理失败，taskId: {}, error: ", taskId, e);
                String errorMsg = e.getMessage();
                if (errorMsg == null || errorMsg.length() > 500) {
                    errorMsg = "处理失败，请稍后重试";
                }
                try {
                    datasetUploadTaskService.updateTaskFailed(taskId, errorMsg);
                    importTransferTaskService.fail(transferTaskId, errorMsg);
                } catch (Exception ex) {
                    log.error("更新任务状态失败，taskId: {}, error: ", taskId, ex);
                }
                
                // 触发上传失败事件
                datasetOperationEventService.createAndInsertEvent(
                        datasetId,
                        new Date(),
                        "数据集上传图片/视频失败",
                        DatasetOperationEvent.EventType.ERROR,
                        DatasetOperationEvent.OperationType.DATA_UPLOAD
                );
            }
            // 不立即清理任务，让前端可以查询到任务结果
        });
        
        // 3. 立即返回taskId
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("transferTaskId", transferTaskId);
        return new DataResponseBody(result);
    }

    @ApiOperation(value = "视频提交")
    @PostMapping(value = "/{datasetId}/video")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody uploadVideo(@PathVariable(name = "datasetId") Long datasetId, @Validated @RequestBody BatchFileCreateDTO batchFileCreateDTO) {
        datasetService.uploadVideo(datasetId, batchFileCreateDTO);
        return new DataResponseBody();
    }

    @ApiOperation(value = "图片压缩包导入数据集")
    @PostMapping(value = "/{datasetId}/files/zip")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody uploadZip(@PathVariable(name = "datasetId") Long datasetId,
                                      @Validated @RequestBody DatasetImportDTO datasetImportDTO) {
        datasetImportDTO.setDatasetId(datasetId);
        datasetService.datasetImport(datasetImportDTO);
        return new DataResponseBody();
    }

    @ApiOperation(value = "视频采样")
    @RequestMapping(value = "/sample")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody sampleVideo(@RequestParam Long datasetId, @RequestParam Long fileId, @RequestParam Integer frameInterval, @RequestParam Integer startTime, @RequestParam Integer endTime, @RequestParam(required = false, defaultValue = "1") Integer resolutionType, @RequestParam(required = false) Integer customWidth, @RequestParam(required = false) Integer customHeight) {
        //后端接收到抽帧请求，输出接收到的参数信息
        log.info("controller接收到抽帧请求，参数信息如下");
        log.info("datasetId: {}, fileId: {}, frameInterval: {}, startTime: {}, endTime: {}, resolutionType: {}, customWidth: {}, customHeight: {}", datasetId, fileId, frameInterval, startTime, endTime, resolutionType, customWidth, customHeight);
        datasetService.sampleVideo(datasetId, fileId, frameInterval, startTime, endTime, resolutionType, customWidth, customHeight);
        return new DataResponseBody();
    }

    @ApiOperation(value = "批量视频采样")
    @RequestMapping(value = "/sampleList")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody sampleVideoList(@RequestParam Long datasetId, @RequestParam List<Long> fileIds, @RequestParam Integer frameInterval) {
        datasetService.sampleVideoList(datasetId, fileIds, frameInterval);
        return new DataResponseBody();
    }

    @ApiOperation(value = "查询上传任务状态")
    @GetMapping(value = "/{datasetId}/upload-tasks/{taskId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getUploadTaskStatus(@PathVariable(name = "datasetId") Long datasetId,
                                                @PathVariable(name = "taskId") String taskId) {
        DatasetUploadTask task = datasetUploadTaskService.getTaskById(taskId);
        if (task == null) {
            return new DataResponseBody(ResponseCode.ERROR, "任务不存在");
        }
        
        // 验证datasetId是否匹配
        if (!task.getDatasetId().equals(datasetId)) {
            return new DataResponseBody(ResponseCode.ERROR, "任务与数据集不匹配");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        result.put("status", task.getStatus());
        result.put("errorMessage", task.getErrorMessage());
        result.put("fileCount", task.getFileCount());
        result.put("createdTime", task.getCreatedTime());
        result.put("updatedTime", task.getUpdatedTime());
        
        return new DataResponseBody(result);
    }

    @ApiOperation(value = "获取视频信息")
    @GetMapping(value = "/{datasetId}/video/{fileId}/info")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getVideoInfo(@PathVariable(name = "datasetId") Long datasetId,
                                         @PathVariable(name = "fileId") Long fileId) {
        return new DataResponseBody(fileService.getVideoInfo(datasetId, fileId));
    }

    @ApiOperation(value = "文件详情", notes = "状态: 101-未标注, 102-手动标注中, 103-自动标注完成, 104-标注完成, 105-标注未识别, 201-目标跟踪完成")
    @GetMapping(value = "/files/{datasetId}/{fileId}/info")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody get(@PathVariable(name = "fileId") Long fileId, @PathVariable(name = "datasetId") Long datasetId) {
        return new DataResponseBody(fileService.get(fileId, datasetId));
    }

    @ApiOperation(value = "批量获取文件信息", notes = "状态: 101-未标注, 102-手动标注中, 103-自动标注完成, 104-标注完成, 105-标注未识别, 201-目标跟踪完成")
    @PostMapping(value = "/files/infos")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getBatch(@Validated @RequestBody BatchFileInfoDTO batchFileInfoDTO) {
        ArrayList<FileVO> fileVOS = new ArrayList<>();
        Long datasetId = batchFileInfoDTO.getDatasetId();
        batchFileInfoDTO.getFileIds().stream().forEach((fileId) -> {
            fileVOS.add(fileService.get(fileId, datasetId));
        });
        return new DataResponseBody(fileVOS);
    }

    @ApiOperation(value = "获取文件标注信息(createML和VOC格式)", notes = "createML,VOC,YOLO")
    @GetMapping(value = "/files/{datasetId}/{fileId}/label")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getFileAnnotation(@PathVariable(name = "fileId") Long fileId, @PathVariable(name = "datasetId") Long datasetId, @RequestParam String labelType) {
        return new DataResponseBody(fileService.getFileAnnotation(fileId, datasetId, labelType));
    }

    @ApiOperation(value = "批量获取文件标注信息(createML和VOC格式)", notes = "createML,VOC,YOLO")
    @GetMapping(value = "/files/{datasetId}/batchLabel")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getBatchFileAnnotation(@PathVariable(name = "datasetId") Long datasetId, @RequestParam List<Long> fileIds, @RequestParam String labelType) {
        ArrayList<String> strings = new ArrayList<>();
        fileIds.stream().forEach((fileId) -> {
            strings.add(fileService.getFileAnnotation(fileId, datasetId, labelType));
        });
        return new DataResponseBody(strings);
    }

    @ApiOperation(value = "获取YOLO格式的标签文本文件", notes = "YOLO")
    @GetMapping(value = "/files/{datasetId}/yoloLabelsFile")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getYoloLabelsFile(@PathVariable(name = "datasetId") Long datasetId) {
        return new DataResponseBody(fileService.getYoloLabelsFile(datasetId));
    }

    @ApiOperation(value = "文件查询", notes = "状态: 101-未标注, 102-手动标注中, 103-自动标注完成, 104-标注完成, 105-标注未识别, 201-目标跟踪完成")
    @GetMapping(value = "/{datasetId}/files")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody query(@PathVariable(name = "datasetId") Long datasetId, Page page, FileQueryCriteriaVO fileQueryCriteria) {
        return new DataResponseBody(fileService.listPage(datasetId, page, fileQueryCriteria));
    }


    @ApiOperation(value = "音频数据集文件查询", notes = "状态: 101-未标注, 102-手动标注中, 103-自动标注完成, 104-标注完成, 105-标注未识别")
    @GetMapping(value = "/{datasetId}/files/audio")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody audioFilesByPage(@PathVariable(name = "datasetId") Long datasetId, Page page, FileQueryCriteriaVO fileQueryCriteria) {
        return new DataResponseBody(fileService.audioFilesByPage(datasetId, page, fileQueryCriteria));
    }

//    @ApiOperation(value = "文本数据集内容查询", notes = "状态: 101-未标注, 102-手动标注中, 103-自动标注完成, 104-标注完成, 105-标注未识别")
//    @GetMapping(value = "/{datasetId}/files/content")
//    @PreAuthorize(Permissions.DATA)
//    public DataResponseBody txtContentByPage(@PathVariable(name = "datasetId") Long datasetId, Page page, FileQueryCriteriaVO fileQueryCriteria) {
//        return new DataResponseBody(fileService.txtContentByPage(datasetId, page, fileQueryCriteria));
//    }

    @ApiOperation(value = "文件查询，物体检测标注页面使用", notes = "状态: 101-未标注, 102-手动标注中, 103-自动标注完成, 104-标注完成, 105-标注未识别, 201-目标跟踪完成")
    @GetMapping(value = "/{datasetId}/files/detection")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody query(@PathVariable(name = "datasetId") Long datasetId,
                                  @RequestParam(required = false) String versionName,
                                  @RequestParam(required = false) Long offset,
                                  @RequestParam(required = false) Integer limit,
                                  @RequestParam(required = false) Integer[] type,
                                  @RequestParam(required = false) Integer page,
                                  @RequestParam(required = false) Long[] labelId,
                                  @RequestParam(required = false) String labelType) {

        // 直接调用带annotation的方法
        Page<JSONObject> resultPage = fileService.listByLimitWithAnnotation(datasetId, versionName, offset, limit, page, type, labelId, labelType);

        return new DataResponseBody(PageUtil.toPage(resultPage, resultPage.getRecords()));
    }

    @ApiOperation(value = "获取文件的offset，物体检测标注页面使用")
    @GetMapping(value = "/{datasetId}/files/{fileId}/offset")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getOffset(@PathVariable(name = "fileId") Long fileId,
                                      @RequestParam(required = false) Integer[] type,
                                      @RequestParam(required = false) Long[] labelId,
                                      @PathVariable(name = "datasetId") Long datasetId) {
        return new DataResponseBody(fileService.getOffset(fileId, datasetId, type, labelId));
    }

    @ApiOperation(value = "获取多人标注场景下特定状态文件的绝对偏移量")
    @GetMapping(value = "/{datasetId}/annotation-offset")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getAbsoluteOffsetForAnnotationStatus(
            @PathVariable(name = "datasetId") Long datasetId,
            @RequestParam(name = "annotationStatus") Integer annotationStatus,
            @RequestParam(name = "startOffset") Integer startOffset,
            @RequestParam(name = "total") Integer total) {

        Integer result = fileService.getAbsoluteOffsetForAnnotationStatus(datasetId, annotationStatus, startOffset, total);
        return new DataResponseBody(result);
    }

    @ApiOperation(value = "获取当前数据集的第一个文件id，物体检测标注页面使用")
    @GetMapping(value = "/{datasetId}/files/first")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getFirstId(@PathVariable(name = "datasetId") Long datasetId,
                                       @RequestParam(required = false) String versionName,
                                       @RequestParam(required = false) Integer type) {
        return new DataResponseBody(fileService.getFirst(datasetId, versionName, type));
    }

    @ApiOperation(value = "文件删除", notes = "删除文件或数据集下的所有文件,不删除dataset.数据集正在自动标注中的文件不允许删除")
    @DeleteMapping(value = "/files")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody delete(@Validated @RequestBody FileDeleteDTO fileDeleteDTO) {
        datasetService.delete(fileDeleteDTO);
        return new DataResponseBody();
    }

    @ApiOperation(value = "视频删除", notes = "删除文件或数据集下的所有文件,不删除dataset.数据集正在自动标注中的文件不允许删除")
    @DeleteMapping(value = "/files/videos")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody deleteVideo(@Validated @RequestBody FileDeleteDTO fileDeleteDTO) {
        datasetService.deleteFiles(fileDeleteDTO);
        return new DataResponseBody();
    }

    @ApiOperation("MinIO下载压缩包参数")
    @GetMapping(value = "/zip")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody downloadFile(@RequestParam String prefix, @RequestParam List<String> objects, @RequestParam String zipName) {
        return new DataResponseBody(minioWebTokenBody.getDownloadParam(bucketName, prefix, objects, zipName));
    }

    @ApiOperation("MinIO生成put请求的上传路径")
    @GetMapping(value = "/minio/url/put")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getEncryptedPutUrl(@RequestParam String objectName) {
        return new DataResponseBody(minioUtil.getEncryptedPutUrl(bucketName, objectName, expiry));
    }

    @ApiOperation("MinIO生成put请求的上传路径列表")
    @PostMapping(value = "/minio/getUrls")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getEncryptedPutUrls(@RequestBody String objectNames) {
        return new DataResponseBody(minioUtil.getEncryptedPutUrls(bucketName, objectNames, expiry));
    }


    @ApiOperation("获取文件对应增强文件列表")
    @GetMapping(value = "/{datasetId}/{fileId}/enhanceFileList")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getEnhanceFileList(@PathVariable(value = "fileId") Long fileId, @PathVariable(value = "datasetId") Long datasetId) {
        return new DataResponseBody(fileService.getEnhanceFileList(fileId, datasetId));
    }

    @ApiOperation("文本状态数量统计")
    @GetMapping(value = "/{datasetId}/count")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getFileCountByStatus(@PathVariable(value = "datasetId") Long datasetId, @Validated FileScreenStatSearchDTO fileScreenStatSearchDTO) {
        return new DataResponseBody(fileService.getFileCountByStatus(datasetId, fileScreenStatSearchDTO));
    }

    @ApiOperation("文本数据集csv/xlsx导入")
    @PostMapping(value = "/tableImport")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody tableImport(@RequestBody DatasetCsvImportDTO datasetCsvImportDTO) {
        fileService.tableImport(datasetCsvImportDTO);
        return new DataResponseBody();
    }

    @ApiOperation("根据指定前缀获取文件/文件夹列表, 支持递归以及非递归模式(默认递归模式)")
    @GetMapping(value = "/{datasetId}/files/fileList")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody fileList(@PathVariable(value = "datasetId") Long datasetId, @RequestParam(required = false) String prefix,
                                     @RequestParam(defaultValue = "false", required = false) boolean recursive,
                                     @RequestParam(required = false) String versionName,
                                     @RequestParam(required = false) boolean isVersionFile) {
        return new DataResponseBody(fileService.fileList(datasetId, prefix, recursive, versionName, isVersionFile));
    }

    @ApiOperation("分页获取文件列表")
    @PostMapping(value = "/{datasetId}/files/filePage")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody filePage(@PathVariable(value = "datasetId") Long datasetId, @RequestBody FilePageDTO filePageDTO) {
        fileService.filePage(filePageDTO, datasetId);
        return new DataResponseBody(filePageDTO);
    }

    @ApiOperation(value = "查询数据集中的图片")
    @GetMapping(value = "/{datasetId}/files/images")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody queryImages(@PathVariable(name = "datasetId") Long datasetId,
                                        Page page,
                                        @RequestParam(required = false) String name,
                                        @RequestParam(required = false) Integer status,
                                        @RequestParam(required = false) String order
    ) {

        FileQueryCriteriaVO fileQueryCriteriaVO = new FileQueryCriteriaVO();
        fileQueryCriteriaVO.setName(name);
        fileQueryCriteriaVO.setStatus(new Integer[]{status});
        fileQueryCriteriaVO.setOrder(order);

        IPage<ImageFileVO> imageFileVOIPage = fileService.listImages(datasetId, page, fileQueryCriteriaVO);
        return new DataResponseBody(PageUtil.toPage(imageFileVOIPage));
    }

    @ApiOperation(value = "查询数据集中的视频")
    @GetMapping(value = "/{datasetId}/files/videos")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody queryVideo(@PathVariable(name = "datasetId") Long datasetId,
                                       @RequestParam(required = false) Integer pageSize,
                                       @RequestParam(required = false) Integer page,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(required = false) String name,
                                       @RequestParam(required = false) List<Long> createTime,
                                       @RequestParam(required = false) String order) {
        IPage<VideoFileVO> VideoFileVOIPage = fileService.listVideos(datasetId, page, pageSize, status, name, createTime, order);

        return new DataResponseBody(PageUtil.toPage(VideoFileVOIPage));
    }

    @ApiOperation(value = "信息总览接口")
    @GetMapping(value = "/{datasetId}/files/infos")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody infoOverView(@PathVariable(name = "datasetId") Long datasetId) {
        //ImageStatisticsDTO imageStatistics = fileService.getImageStatistics(datasetId);
        VideoStatisticsDTO videoStatistics = fileService.getVideoStatistics(datasetId);

        //获取所有文件的标注状态
        FileScreenStatSearchDTO fileScreenStatSearchDTO = new FileScreenStatSearchDTO();
        fileScreenStatSearchDTO.setAnnotationResult(FileTypeEnum.HAVE_ANNOTATION.getValue());
        FileScreenStatVO fileCountByStatus = fileService.getFileCountByStatus(datasetId, fileScreenStatSearchDTO);

        //用总数-视频数量得到总图片数量
        Long totalImages = fileCountByStatus.getHaveAnnotation() + fileCountByStatus.getNoAnnotation();
        Long unlabeledImages = fileCountByStatus.getNoAnnotation();
        Long labeledImages = fileCountByStatus.getHaveAnnotation();

        Map<String, Object> datasetStatistics = new HashMap<>();
        datasetStatistics.put("totalImages", totalImages);
        datasetStatistics.put("unlabeledImages", unlabeledImages);
        datasetStatistics.put("labeledImages", labeledImages);
        datasetStatistics.put("totalVideos", videoStatistics.getTotalVideos());
        datasetStatistics.put("unextractedVideos", videoStatistics.getUnExtractedVideos());
        datasetStatistics.put("extractedVideos", videoStatistics.getExtractedVideos());

        //返回标签信息
        List<DatasetLabelInfoDTO> datasetLabelInfo = datasetService.getDatasetLabelInfo(datasetId);
        int totalLabels = datasetLabelInfo.size();
        long totalSamples = datasetLabelInfo.stream()
                .mapToLong(DatasetLabelInfoDTO::getAnnotationCount)
                .sum();

        datasetStatistics.put("totalLabels", totalLabels);
        datasetStatistics.put("totalSamples", totalSamples);


        return new DataResponseBody(datasetStatistics);
    }

    @ApiOperation(value = "zlmediakit web hook视频录制完成后触发")
    @PostMapping(value = "/zlm/record")
    public DataResponseBody mp4Record(@RequestBody MP4RecordDTO mp4RecordDTO) {
        // todo: 消息队列异步保存视频信息
        fileService.saveVideoInfo(mp4RecordDTO);
        return new DataResponseBody<>();
    }


    public static String readableFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.00").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }


    @ApiOperation(value = "数据集文件大小接口")
    @PostMapping(value = "/{datasetId}/sizes")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody infoOverView(@PathVariable(name = "datasetId") Long datasetId,
                                         @RequestBody JSONObject jsonObject) {
        String datasetVersionName = jsonObject.getString("datasetVersionName");
        System.out.println(datasetVersionName);
        Long imagesSize = fileService.getImagesSize(datasetId, datasetVersionName);
        Long videosSize = fileService.getVideosSize(datasetId);

        Long totalSize = imagesSize + videosSize;
        Map<String, String> sizes = new HashMap<>();
        sizes.put("imagesSize", readableFileSize(imagesSize));
        sizes.put("videosSize", readableFileSize(videosSize));
        sizes.put("totalSize", readableFileSize(totalSize));
        return new DataResponseBody(sizes);
    }


}
