package org.dubhe.data.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.dubhe.biz.base.constant.MagicNumConstant;
import org.dubhe.biz.base.constant.NumberConstant;
import org.dubhe.biz.base.constant.ResponseCode;
import org.dubhe.biz.base.enums.OperationTypeEnum;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.utils.StringUtils;
import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.dubhe.biz.permission.annotation.DataPermissionMethod;
import org.dubhe.biz.statemachine.dto.StateChangeDTO;
import org.dubhe.data.constant.*;
import org.dubhe.data.domain.bo.TaskSplitBO;
import org.dubhe.data.domain.dto.AnnotationDTO;
import org.dubhe.data.domain.dto.AnnotationInfoCreateDTO;
import org.dubhe.data.domain.dto.AutoTrackCreateDTO;
import org.dubhe.data.domain.dto.BatchAnnotationInfoCreateDTO;
import org.dubhe.data.domain.entity.*;
import org.dubhe.data.machine.constant.DataStateMachineConstant;
import org.dubhe.data.machine.constant.FileStateCodeConstant;
import org.dubhe.data.machine.constant.FileStateMachineConstant;
import org.dubhe.data.machine.enums.FileStateEnum;
import org.dubhe.data.machine.utils.StateMachineUtil;
import org.dubhe.data.service.*;
import org.dubhe.data.service.store.IStoreService;
import org.dubhe.data.service.store.MinioStoreServiceImpl;
import org.dubhe.data.util.GeneratorKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

/**
 * @description 标注service
 * @date 2020-03-27
 */
@Service
public class AnnotationServiceImpl implements AnnotationService {


    /**
     * 文件信息服务
     */
    @Autowired
    private FileService fileService;

    /**
     * 任务服务类
     */
    @Autowired
    private TaskService taskService;

    /**
     * 数据集服务类
     */
    @Autowired
    private DatasetService datasetService;

    /**
     * 文件存储服务类
     */
    @Resource(type = MinioStoreServiceImpl.class)
    private IStoreService storeService;

    /**
     * 版本文件服务类
     */
    @Autowired
    private DatasetVersionFileService datasetVersionFileService;

    @Autowired
    private GeneratorKeyUtil generatorKeyUtil;

    @Autowired
    private DatasetLabelService datasetLabelService;

    /**
     * 标注任务队列
     */
    static PriorityBlockingQueue<TaskSplitBO> queue;

    /**
     * 自动标注任务
     */
    private ConcurrentHashMap<String, TaskSplitBO> autoAnnotating;

    /**
     * 版本服务类
     */
    @Autowired
    private DatasetVersionService datasetVersionService;

    /**
     * 数据文件标注服务
     */
    @Autowired
    private DataFileAnnotationService dataFileAnnotationService;

//    @Autowired
//    private RestHighLevelClient restHighLevelClient;

    /**
     * 目标跟踪任务
     */
    private ConcurrentHashSet<Long> tracking;

    /**
     * 文件工具类
     */
    @Autowired
    private org.dubhe.data.util.FileUtil fileUtil;

//    @Autowired
//    private AutoLabelModelServiceService autoLabelModelServiceService;

    /**
     * 队列长度
     */
    public static final int QUEUE_SIZE = MagicNumConstant.FIFTY;

    /**
     * 跟踪数量
     */
    public static final int TRACKING_SIZE = MagicNumConstant.FIVE;

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        queue = new PriorityBlockingQueue<>(QUEUE_SIZE, Comparator.comparingInt(TaskSplitBO::getPriority).reversed());
        autoAnnotating = new ConcurrentHashMap<>(MagicNumConstant.SIXTEEN);
        tracking = new ConcurrentHashSet<>(MagicNumConstant.SIXTEEN);
    }

    /**
     * 标注保存(分类批量)
     *
     * @param batchAnnotationInfoCreateDTO 标注信息
     * @return int 标注修改的数量
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    @DataPermissionMethod
    public void save(Long datasetId, BatchAnnotationInfoCreateDTO batchAnnotationInfoCreateDTO) {
        for (AnnotationInfoCreateDTO annotationInfoCreateDTO : batchAnnotationInfoCreateDTO.getAnnotations()) {
            save(datasetId, annotationInfoCreateDTO);
        }
    }

    /**
     * 标注保存实现
     *
     * @param annotationInfoCreateDTO 标注信息
     * @return int 标注修改的数量
     */
    @Override
    public void save(Long datasetId, AnnotationInfoCreateDTO annotationInfoCreateDTO) {
        Dataset dataset = datasetService.getOneById(datasetId);
        datasetService.checkPublic(dataset, OperationTypeEnum.UPDATE);
        //判断数据集是否在发布中
        if (!StringUtils.isBlank(dataset.getCurrentVersionName())) {
            if (datasetVersionService.getDatasetVersionSourceVersion(dataset).getDataConversion().equals(NumberConstant.NUMBER_4)) {
                throw new BusinessException(ErrorEnum.DATASET_PUBLISH_ERROR);
            }
        }
        datasetVersionFileService.deleteByFileId(datasetId, annotationInfoCreateDTO.getId());
        annotationInfoCreateDTO.setDatasetId(datasetId);
        annotationInfoCreateDTO.setCurrentVersionName(dataset.getCurrentVersionName());
        annotationInfoCreateDTO.setDataType(dataset.getDataType());
        doSave(annotationInfoCreateDTO);
        saveDatasetFileAnnotationsByImage(annotationInfoCreateDTO);
        //改变文件的状态为标注完成
        StateMachineUtil.stateChange(new StateChangeDTO() {{
            setObjectParam(new Object[]{new DatasetVersionFile() {{
                setDatasetId(dataset.getId());
                setFileId(annotationInfoCreateDTO.getId());
                setVersionName(dataset.getCurrentVersionName());
            }}});
            setEventMethodName(FileStateMachineConstant.FILE_SAVE_COMPLETE_EVENT);
            setStateMachineType(FileStateMachineConstant.FILE_STATE_MACHINE);
        }});
        //改变数据集的状态为标注完成
        StateMachineUtil.stateChange(new StateChangeDTO() {{
            setObjectParam(new Object[]{dataset});
            setEventMethodName(DataStateMachineConstant.DATA_FINISH_MANUAL_EVENT);
            setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
        }});
    }

    /**
     * 标注保存(单个)
     *
     * @param annotationInfoCreateDTO 标注信息
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    @DataPermissionMethod
    public void save(Long fileId, Long datasetId, AnnotationInfoCreateDTO annotationInfoCreateDTO) {
        Dataset dataset = datasetService.getOneById(datasetId);
        if (dataset == null) {
            throw new BusinessException(ErrorEnum.DATASET_ABSENT);
        }
        datasetService.checkPublic(dataset, OperationTypeEnum.UPDATE);
        //判断数据集是否在发布中
        if (!StringUtils.isBlank(dataset.getCurrentVersionName())) {
            if (datasetVersionService.getDatasetVersionSourceVersion(dataset).getDataConversion().equals(NumberConstant.NUMBER_4)) {
                throw new BusinessException(ErrorEnum.DATASET_PUBLISH_ERROR);
            }
        }
        annotationInfoCreateDTO.setId(fileId);
        annotationInfoCreateDTO.setDatasetId(datasetId);
        annotationInfoCreateDTO.setCurrentVersionName(dataset.getCurrentVersionName());
        annotationInfoCreateDTO.setDataType(dataset.getDataType());
        doSave(annotationInfoCreateDTO);
        saveDatasetFileAnnotationsByImage(annotationInfoCreateDTO);
        //改变数据集的状态为标注中
        StateMachineUtil.stateChange(new StateChangeDTO() {{
            setObjectParam(new Object[]{dataset});
            setEventMethodName(DataStateMachineConstant.DATA_MANUAL_ANNOTATION_SAVE_EVENT);
            setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
        }});
        //将文件改为标注中的状态
        StateMachineUtil.stateChange(new StateChangeDTO() {{
            setObjectParam(new Object[]{new DatasetVersionFile() {{
                setDatasetId(dataset.getId());
                setFileId(fileId);
                setVersionName(dataset.getCurrentVersionName());
            }}});
            setEventMethodName(FileStateMachineConstant.FILE_MANUAL_ANNOTATION_SAVE_EVENT);
            setStateMachineType(FileStateMachineConstant.FILE_STATE_MACHINE);
        }});
    }

    /**
     * 标注文件保存
     *
     * @param annotationInfoCreateDTO 标注信息
     */
    private void doSave(AnnotationInfoCreateDTO annotationInfoCreateDTO) {

        if (annotationInfoCreateDTO == null || annotationInfoCreateDTO.getAnnotation() == null
                || annotationInfoCreateDTO.getId() == null) {
            LogUtil.warn(LogEnum.BIZ_DATASET, "annotation info invalid. annotation:{}", annotationInfoCreateDTO);
            return;
        }
        QueryWrapper<File> fileQueryWrapper = new QueryWrapper<>();
        fileQueryWrapper
                .eq("id", annotationInfoCreateDTO.getId()).eq("dataset_id", annotationInfoCreateDTO.getDatasetId());
        File fileOne = fileService.selectOne(fileQueryWrapper);
        if (fileOne == null) {
            LogUtil.warn(LogEnum.BIZ_DATASET, ErrorEnum.FILE_ABSENT.getMsg() + "fileId is" + annotationInfoCreateDTO.getId());
            throw new BusinessException(ErrorEnum.FILE_ABSENT);
        }
//        datasetService.autoAnnotatingCheck(fileOne);
        String filePath = fileUtil.getWriteAnnotationAbsPath(fileOne.getDatasetId(), fileOne.getName());
        String annotation = annotationInfoCreateDTO.getAnnotation();
        storeService.write(filePath, annotation);


    }

    private void doSaveBatch(List<AnnotationInfoCreateDTO> annotationInfoCreateDTOList) {
        if (annotationInfoCreateDTOList == null || annotationInfoCreateDTOList.isEmpty()) {
            LogUtil.warn(LogEnum.BIZ_DATASET, "annotation info list is empty.");
            return;
        }

        // 1. 只收集当前批次涉及到的 fileId 和唯一的 datasetId
        Set<Long> fileIds = new HashSet<>();
        Long datasetId = null;
        for (AnnotationInfoCreateDTO dto : annotationInfoCreateDTOList) {
            if (dto != null && dto.getId() != null && dto.getDatasetId() != null) {
                fileIds.add(dto.getId());
                if (datasetId == null) {
                    datasetId = dto.getDatasetId();
                } else if (!datasetId.equals(dto.getDatasetId())) {
                    LogUtil.warn(LogEnum.BIZ_DATASET, "Batch annotation has multiple datasetIds! Only the first will be used. Found: {} and {}", datasetId, dto.getDatasetId());
                }
            }
        }
        if (fileIds.isEmpty() || datasetId == null) {
            LogUtil.warn(LogEnum.BIZ_DATASET, "No valid fileId or datasetId for batch save.");
            return;
        }

        // 2. 批量查出所有 fileId + datasetId 对应的 File
        QueryWrapper<File> fileQueryWrapper = new QueryWrapper<>();
        fileQueryWrapper.in("id", fileIds).eq("dataset_id", datasetId);
        List<File> fileList = fileService.listFile(fileQueryWrapper);

        // 3. 建立 fileId -> File 的映射
        Map<Long, File> fileIdMap = new HashMap<>();
        for (File file : fileList) {
            fileIdMap.put(file.getId(), file);
        }

        // 4. 依次写入 annotation
        for (AnnotationInfoCreateDTO annotationInfoCreateDTO : annotationInfoCreateDTOList) {
            if (annotationInfoCreateDTO == null || annotationInfoCreateDTO.getAnnotation() == null
                    || annotationInfoCreateDTO.getId() == null) {
                LogUtil.warn(LogEnum.BIZ_DATASET, "annotation info invalid. annotation:{}", annotationInfoCreateDTO);
                continue;
            }
            File fileOne = fileIdMap.get(annotationInfoCreateDTO.getId());
            if (fileOne == null) {
                LogUtil.warn(LogEnum.BIZ_DATASET, ErrorEnum.FILE_ABSENT.getMsg() + "fileId is" + annotationInfoCreateDTO.getId());
                throw new BusinessException(ErrorEnum.FILE_ABSENT);
            }
            // datasetService.autoAnnotatingCheck(fileOne);
            String filePath = fileUtil.getWriteAnnotationAbsPath(fileOne.getDatasetId(), fileOne.getName());
            String annotation = annotationInfoCreateDTO.getAnnotation();
            storeService.write(filePath, annotation);
        }

    }




    /**
     * 保存数据集文件标注信息
     * 优化：直接删除该version_file_id的所有标注，然后插入新标注，减少查询和锁竞争
     *
     * @param annotationInfoCreateDTO 标注详情实体
     */
    private void saveDatasetFileAnnotations(AnnotationInfoCreateDTO annotationInfoCreateDTO) {
        List<AnnotationDTO> annotationDTOS = JSONObject.parseArray(annotationInfoCreateDTO.getAnnotation(), AnnotationDTO.class);
        Long datasetId = annotationInfoCreateDTO.getDatasetId();
        DatasetVersionFile datasetVersionFile = datasetVersionFileService.getDatasetVersionFile(
                datasetId, annotationInfoCreateDTO.getCurrentVersionName(), annotationInfoCreateDTO.getId());
        if (Objects.isNull(datasetVersionFile)) {
            throw new BusinessException(ErrorEnum.DATASET_VERSION_FILE_IS_ERROR);
        }
        if (!CollectionUtil.isEmpty(annotationDTOS)) {
            Long versionFileId = datasetVersionFile.getId();
            List<Long> fileLabelIds = annotationDTOS.stream().map(a -> a.getCategoryId()).collect(Collectors.toList());
            // 注意：deleteByFileId已经在finishManualInternal中调用过了，已经物理删除了旧标注
            // 这里直接插入新标注即可，不需要再次删除，减少锁竞争
            dataFileAnnotationService.insertAnnotationFileByVersionIdAndLabelIds(datasetId, versionFileId, fileLabelIds, datasetVersionFile.getFileName());
            //改变文件的状态为标注完成
            StateMachineUtil.stateChange(new StateChangeDTO() {{
                setObjectParam(new Object[]{new DatasetVersionFile() {{
                    setDatasetId(annotationInfoCreateDTO.getDatasetId());
                    setFileId(annotationInfoCreateDTO.getId());
                    setVersionName(annotationInfoCreateDTO.getCurrentVersionName());
                }}});
                setEventMethodName(FileStateMachineConstant.FILE_SAVE_COMPLETE_EVENT);
                setStateMachineType(FileStateMachineConstant.FILE_STATE_MACHINE);
            }});
        } else {
            datasetVersionFileService.updateStatusById(
                    DatasetVersionFile.builder().id(datasetVersionFile.getId())
                            .datasetId(datasetVersionFile.getDatasetId())
                            .annotationStatus(FileStateEnum.NOT_ANNOTATION_FILE_STATE.getCode()).build());
            // 注意：deleteByFileId已经在finishManualInternal中调用过了，已经物理删除了旧标注
            // 这里不需要再次删除，减少锁竞争
        }
    }

    /**
     * 保存数据集文件标注信息
     *
     * @param annotationInfoCreateDTO 标注详情实体
     */
    private void saveDatasetFileAnnotationsForUpload(AnnotationInfoCreateDTO annotationInfoCreateDTO) {
        List<AnnotationDTO> annotationDTOS = JSONObject.parseArray(annotationInfoCreateDTO.getAnnotation(), AnnotationDTO.class);
        Long datasetId = annotationInfoCreateDTO.getDatasetId();
        DatasetVersionFile datasetVersionFile = datasetVersionFileService.getDatasetVersionFile(
                datasetId, annotationInfoCreateDTO.getCurrentVersionName(), annotationInfoCreateDTO.getId());
        if (Objects.isNull(datasetVersionFile)) {
            throw new BusinessException(ErrorEnum.DATASET_VERSION_FILE_IS_ERROR);
        }
        if (!CollectionUtil.isEmpty(annotationDTOS)) {
            Long versionFileId = datasetVersionFile.getId();
            List<Long> fileLabelIds = annotationDTOS.stream().map(a -> a.getCategoryId()).collect(Collectors.toList());
            List<Long> dbLabelIds = dataFileAnnotationService.findInfoByVersionId(datasetId, versionFileId);
            if (!CollectionUtil.isEmpty(dbLabelIds)) {
                dataFileAnnotationService.deleteAnnotationFileByVersionIdAndLabelIds(datasetId, versionFileId, dbLabelIds);
            }
            // 直接设置为已标注
            datasetVersionFileService.updateStatusById(
                    DatasetVersionFile.builder().id(datasetVersionFile.getId())
                            .datasetId(datasetVersionFile.getDatasetId())
                            .annotationStatus(FileStateEnum.ANNOTATION_COMPLETE_FILE_STATE.getCode()).build());
            dataFileAnnotationService.insertAnnotationFileByVersionIdAndLabelIds(datasetId, versionFileId, fileLabelIds, datasetVersionFile.getFileName());
        } else {
            // 直接设置为已标注
            datasetVersionFileService.updateStatusById(
                    DatasetVersionFile.builder().id(datasetVersionFile.getId())
                            .datasetId(datasetVersionFile.getDatasetId())
                            .annotationStatus(FileStateEnum.ANNOTATION_COMPLETE_FILE_STATE.getCode()).build());
            dataFileAnnotationService.deleteBatch(datasetId, Arrays.asList(datasetVersionFile.getId()));
        }
    }


    //自动标注完图片设置都成未标注，但标注信息都在
    private void saveDatasetFileAnnotationsForAutoLabel(AnnotationInfoCreateDTO annotationInfoCreateDTO, boolean isModified) {
        List<AnnotationDTO> annotationDTOS = JSONObject.parseArray(annotationInfoCreateDTO.getAnnotation(), AnnotationDTO.class);
        Long datasetId = annotationInfoCreateDTO.getDatasetId();
        DatasetVersionFile datasetVersionFile = datasetVersionFileService.getDatasetVersionFile(
                datasetId, annotationInfoCreateDTO.getCurrentVersionName(), annotationInfoCreateDTO.getId());
        if (Objects.isNull(datasetVersionFile)) {
            throw new BusinessException(ErrorEnum.DATASET_VERSION_FILE_IS_ERROR);
        }
        //自动标注改过的设置为未标注
        if (isModified){
            datasetVersionFileService.updateStatusById(
                    DatasetVersionFile.builder().id(datasetVersionFile.getId())
                            .datasetId(datasetVersionFile.getDatasetId())
                            .annotationStatus(FileStateEnum.NOT_ANNOTATION_FILE_STATE.getCode()).build());
        }
        else if (!CollectionUtil.isEmpty(annotationDTOS)) {
            Long versionFileId = datasetVersionFile.getId();
            List<Long> fileLabelIds = annotationDTOS.stream().map(a -> a.getCategoryId()).collect(Collectors.toList());
            List<Long> dbLabelIds = dataFileAnnotationService.findInfoByVersionId(datasetId, versionFileId);
            if (!CollectionUtil.isEmpty(dbLabelIds)) {
                dataFileAnnotationService.deleteAnnotationFileByVersionIdAndLabelIds(datasetId, versionFileId, dbLabelIds);
            }
            // 直接设置为已标注
            datasetVersionFileService.updateStatusById(
                    DatasetVersionFile.builder().id(datasetVersionFile.getId())
                            .datasetId(datasetVersionFile.getDatasetId())
                            .annotationStatus(FileStateEnum.ANNOTATION_COMPLETE_FILE_STATE.getCode()).build());
            dataFileAnnotationService.insertAnnotationFileByVersionIdAndLabelIds(datasetId, versionFileId, fileLabelIds, datasetVersionFile.getFileName());
        } else {
            datasetVersionFileService.updateStatusById(
                    DatasetVersionFile.builder().id(datasetVersionFile.getId())
                            .datasetId(datasetVersionFile.getDatasetId())
                            .annotationStatus(FileStateEnum.NOT_ANNOTATION_FILE_STATE.getCode()).build());
            dataFileAnnotationService.deleteBatch(datasetId, Arrays.asList(datasetVersionFile.getId()));
        }
    }



    private void saveDatasetFileAnnotationsForUploadBatch(List<AnnotationInfoCreateDTO> annotationInfoCreateDTOList) {
        if (annotationInfoCreateDTOList == null || annotationInfoCreateDTOList.isEmpty()) {
            return;
        }
        // 正常来说所有DTO属于同一个 datasetId 和 versionName
        Long datasetId = annotationInfoCreateDTOList.get(0).getDatasetId();
        String versionName = annotationInfoCreateDTOList.get(0).getCurrentVersionName();

        // 1. 一次查出全部 DatasetVersionFile
        List<DatasetVersionFile> versionFiles = datasetVersionFileService.findByDatasetIdAndVersionNameWithoutStatus(datasetId, versionName);
        // 2. 以 fileId 为 key 建立 map
        Map<Long, DatasetVersionFile> versionFileMap = versionFiles.stream()
                .collect(Collectors.toMap(DatasetVersionFile::getFileId, v -> v));
        for (Map.Entry<Long, DatasetVersionFile> entry : versionFileMap.entrySet()) {
            Long fileId = entry.getKey();
            DatasetVersionFile versionFile = entry.getValue();
        }


        List<Long> completeFileIds = new ArrayList<>();
        List<Long> notAnnotatedFileIds = new ArrayList<>();
        List<Long> versionFileIdsToDelete = new ArrayList<>();
        Map<Long, List<Long>> insertLabelMap = new HashMap<>();
        Map<Long, String> fileNameMap = new HashMap<>();

        for (AnnotationInfoCreateDTO annotationInfoCreateDTO : annotationInfoCreateDTOList) {
            List<AnnotationDTO> annotationDTOS = JSONObject.parseArray(annotationInfoCreateDTO.getAnnotation(), AnnotationDTO.class);
            DatasetVersionFile datasetVersionFile = versionFileMap.get(annotationInfoCreateDTO.getId());
            if (datasetVersionFile == null) {
                throw new BusinessException(ErrorEnum.DATASET_VERSION_FILE_IS_ERROR);
            }
            Long versionFileId = datasetVersionFile.getId();

            if (!CollectionUtil.isEmpty(annotationDTOS)) {
                completeFileIds.add(versionFileId);
                List<Long> fileLabelIds = annotationDTOS.stream().map(AnnotationDTO::getCategoryId).collect(Collectors.toList());
                List<Long> dbLabelIds = dataFileAnnotationService.findInfoByVersionId(datasetId, versionFileId);
                if (!CollectionUtil.isEmpty(dbLabelIds)) {
                    dataFileAnnotationService.deleteAnnotationFileByVersionIdAndLabelIds(datasetId, versionFileId, dbLabelIds);
                }
                insertLabelMap.put(versionFileId, fileLabelIds);
                fileNameMap.put(versionFileId, datasetVersionFile.getFileName());
            } else {
                notAnnotatedFileIds.add(versionFileId);
                versionFileIdsToDelete.add(versionFileId);
            }
        }

        try{
            // 批量设置为未标注(现在压缩包导入都认为是已标注
            if (!notAnnotatedFileIds.isEmpty()) {
                datasetVersionFileService.updateStatusByIds(
                        FileStateEnum.ANNOTATION_COMPLETE_FILE_STATE.getCode(), datasetId, notAnnotatedFileIds);
            }
            // 批量设置为已标注
            if (!completeFileIds.isEmpty()) {
                datasetVersionFileService.updateStatusByIds(
                        FileStateEnum.ANNOTATION_COMPLETE_FILE_STATE.getCode(), datasetId, completeFileIds);
            }
            // 批量插入 annotation
            dataFileAnnotationService.insertAnnotationFileBatch(datasetId, insertLabelMap, fileNameMap);
            // 批量删除未标注的数据
            if (!versionFileIdsToDelete.isEmpty()) {
                dataFileAnnotationService.deleteBatch(datasetId, versionFileIdsToDelete);
            }
        }catch (Exception e){
            e.printStackTrace();
            throw e;
        }

    }



    /**
     * 标注完成（带死锁重试机制）
     *
     * @param annotationInfoCreateDTO 标注信息
     * @param fileId                  文件id
     * @return int 标注完成的数量
     */
    @Override
    @DataPermissionMethod
    public void finishManual(Long fileId, Long datasetId, AnnotationInfoCreateDTO annotationInfoCreateDTO) {
        int maxRetries = 3;
        int retryDelay = 100; // 初始延迟100ms，每次重试递增
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                finishManualInternal(fileId, datasetId, annotationInfoCreateDTO);
                return; // 成功则直接返回
            } catch (Exception e) {
                // 检查是否是死锁异常
                if (isDeadlockException(e) && attempt < maxRetries) {
                    long delay = retryDelay * attempt; // 递增延迟：100ms, 200ms, 300ms
                    LogUtil.warn(LogEnum.BIZ_DATASET, 
                        "标注完成时发生死锁，进行第{}次重试，fileId: {}, datasetId: {}, 延迟: {}ms", 
                        attempt, fileId, datasetId, delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        LogUtil.error(LogEnum.BIZ_DATASET, "重试被中断，fileId: {}, datasetId: {}", fileId, datasetId);
                        throw new BusinessException(ErrorEnum.THREAD_ERROR);
                    }
                } else {
                    // 非死锁异常或已达到最大重试次数，直接抛出
                    if (isDeadlockException(e) && attempt >= maxRetries) {
                        LogUtil.error(LogEnum.BIZ_DATASET, 
                            "标注完成时发生死锁，已达到最大重试次数，fileId: {}, datasetId: {}", 
                            fileId, datasetId);
                    }
                    throw e;
                }
            }
        }
    }

    /**
     * 判断是否是死锁异常
     */
    private boolean isDeadlockException(Exception e) {
        if (e == null) {
            return false;
        }
        String errorMessage = e.getMessage();
        if (errorMessage != null) {
            String lowerMessage = errorMessage.toLowerCase();
            return lowerMessage.contains("deadlock") 
                || lowerMessage.contains("try restarting transaction");
        }
        // 检查嵌套异常
        Throwable cause = e.getCause();
        if (cause != null) {
            errorMessage = cause.getMessage();
            if (errorMessage != null) {
                String lowerMessage = errorMessage.toLowerCase();
                return lowerMessage.contains("deadlock") 
                    || lowerMessage.contains("try restarting transaction");
            }
        }
        return false;
    }

    /**
     * 标注完成内部实现（实际的事务方法）
     *
     * @param annotationInfoCreateDTO 标注信息
     * @param fileId                  文件id
     */
    @Transactional(rollbackFor = Exception.class)
    public void finishManualInternal(Long fileId, Long datasetId, AnnotationInfoCreateDTO annotationInfoCreateDTO) {
        annotationInfoCreateDTO.setDatasetId(datasetId);
        Dataset dataset = datasetService.getOneById(datasetId);
        datasetService.checkPublic(dataset, OperationTypeEnum.UPDATE);
        //判断数据集是否在发布中
        if (!StringUtils.isBlank(dataset.getCurrentVersionName())) {
            if (datasetVersionService.getDatasetVersionSourceVersion(dataset).getDataConversion().equals(NumberConstant.NUMBER_4)) {
                throw new BusinessException(ErrorEnum.DATASET_PUBLISH_ERROR);
            }
        }
        datasetVersionFileService.deleteByFileId(datasetId, fileId);
        annotationInfoCreateDTO.setId(fileId);
        annotationInfoCreateDTO.setDataType(dataset.getDataType());
        annotationInfoCreateDTO.setCurrentVersionName(dataset.getCurrentVersionName());
        doSave(annotationInfoCreateDTO);
        //解析文本标注Json串将数据保存到DB
        if (dataset.getAnnotateType().equals(AnnotateTypeEnum.TEXT_CLASSIFICATION.getValue())
                || DatatypeEnum.IMAGE.getValue().equals(annotationInfoCreateDTO.getDataType())
                || DatatypeEnum.VIDEO.getValue().equals(annotationInfoCreateDTO.getDataType())
                || dataset.getAnnotateType().equals(AnnotateTypeEnum.AUDIO_CLASSIFY.getValue())) {
            saveDatasetFileAnnotations(annotationInfoCreateDTO);
        }
        if (DatatypeEnum.IMAGE.getValue().equals(annotationInfoCreateDTO.getDataType())) {
            //改变文件的状态为标注完成
            StateMachineUtil.stateChange(new StateChangeDTO() {{
                setObjectParam(new Object[]{new DatasetVersionFile() {{
                    setDatasetId(dataset.getId());
                    setFileId(fileId);
                    setVersionName(dataset.getCurrentVersionName());
                }}});
                setEventMethodName(FileStateMachineConstant.FILE_SAVE_COMPLETE_EVENT);
                setStateMachineType(FileStateMachineConstant.FILE_STATE_MACHINE);
            }});
        }
        if (dataset.getAnnotateType().equals(AnnotateTypeEnum.TEXT_SEGMENTATION.getValue()) ||
                dataset.getAnnotateType().equals(AnnotateTypeEnum.NAMED_ENTITY_RECOGNITION.getValue()) ||
                dataset.getAnnotateType().equals(AnnotateTypeEnum.SPEECH_RECOGNITION.getValue())) {
            List<AnnotationDTO> annotationDTOS = JSONObject.parseArray(annotationInfoCreateDTO.getAnnotation(), AnnotationDTO.class);
            if (!CollectionUtil.isEmpty(annotationDTOS)) {
                //改变文件的状态为标注完成
                StateMachineUtil.stateChange(new StateChangeDTO() {{
                    setObjectParam(new Object[]{new DatasetVersionFile() {{
                        setDatasetId(annotationInfoCreateDTO.getDatasetId());
                        setFileId(annotationInfoCreateDTO.getId());
                        setVersionName(annotationInfoCreateDTO.getCurrentVersionName());
                    }}});
                    setEventMethodName(FileStateMachineConstant.FILE_SAVE_COMPLETE_EVENT);
                    setStateMachineType(FileStateMachineConstant.FILE_STATE_MACHINE);
                }});
            } else {
                DatasetVersionFile datasetVersionFile = datasetVersionFileService.getDatasetVersionFile(
                        datasetId, annotationInfoCreateDTO.getCurrentVersionName(), annotationInfoCreateDTO.getId());
                datasetVersionFileService.updateStatusById(
                        DatasetVersionFile.builder().id(datasetVersionFile.getId())
                                .datasetId(datasetVersionFile.getDatasetId())
                                .annotationStatus(FileStateEnum.NOT_ANNOTATION_FILE_STATE.getCode()).build());
                dataFileAnnotationService.deleteBatch(datasetId, Arrays.asList(datasetVersionFile.getId()));
            }
        }
        //改变数据集的状态为标注完成
        StateMachineUtil.stateChange(new StateChangeDTO() {{
            setObjectParam(new Object[]{dataset});
            setEventMethodName(DataStateMachineConstant.DATA_FINISH_MANUAL_EVENT);
            setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
        }});
//        fileService.recoverEsStatus(datasetId, fileId);
        if (dataset.getDataType().equals(MagicNumConstant.TWO) || dataset.getDataType().equals(MagicNumConstant.THREE) ||
                dataset.getAnnotateType().equals(AnnotateTypeEnum.TEXT_SEGMENTATION.getValue()) ||
                dataset.getAnnotateType().equals(AnnotateTypeEnum.NAMED_ENTITY_RECOGNITION.getValue())) {
//            UpdateRequest updateRequest = new UpdateRequest(esIndex,"_doc",fileId.toString());
            JSONObject esJsonObject = new JSONObject();
            if (annotationInfoCreateDTO.getAnnotation() == null) {
                esJsonObject.put("labelId", null);
                esJsonObject.put("prediction", null);
                esJsonObject.put("annotation", null);
                esJsonObject.put("status", String.valueOf(FileTypeEnum.UNFINISHED.getValue()));
            } else {
                JSONArray jsonArray = JSONArray.parseArray(annotationInfoCreateDTO.getAnnotation());
                List<String> labelIds = jsonArray.stream().map(json -> {
                    return JSONObject.parseObject(json.toString()).getString("category_id");
                }).collect(Collectors.toList());
                esJsonObject.put("labelId", labelIds);
                esJsonObject.put("prediction", jsonArray.getJSONObject(0).getString("score"));
                esJsonObject.put("status", String.valueOf(FileTypeEnum.FINISHED.getValue()));
                esJsonObject.put("annotation", annotationInfoCreateDTO.getAnnotation());
            }
//            updateRequest.doc(esJsonObject,XContentType.JSON);
//            updateRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//            try {
//                restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
//            } catch (IOException e) {
//                LogUtil.error(LogEnum.BIZ_DATASET, "update es data error:{}", e);
//            }
        }
    }

    /**
     * 标注完成
     *
     * @param annotationInfoCreateDTO 标注信息
     * @param fileId                  文件id
     * @return int 标注完成的数量
     */
    @Override
    @DataPermissionMethod
    public void finishManualForUpload(Long fileId, Long datasetId, AnnotationInfoCreateDTO annotationInfoCreateDTO) {
        annotationInfoCreateDTO.setDatasetId(datasetId);
        Dataset dataset = datasetService.getOneById(datasetId);
        //判断数据集是否在发布中
        if (!StringUtils.isBlank(dataset.getCurrentVersionName())) {
            if (datasetVersionService.getDatasetVersionSourceVersion(dataset).getDataConversion().equals(NumberConstant.NUMBER_4)) {
                throw new BusinessException(ErrorEnum.DATASET_PUBLISH_ERROR);
            }
        }
        datasetVersionFileService.deleteByFileId(datasetId, fileId);
        annotationInfoCreateDTO.setId(fileId);
        annotationInfoCreateDTO.setDataType(dataset.getDataType());
        annotationInfoCreateDTO.setCurrentVersionName(dataset.getCurrentVersionName());
        doSave(annotationInfoCreateDTO);
        //解析文本标注Json串将数据保存到DB
        if (dataset.getAnnotateType().equals(AnnotateTypeEnum.TEXT_CLASSIFICATION.getValue())
                || DatatypeEnum.IMAGE.getValue().equals(annotationInfoCreateDTO.getDataType())
                || DatatypeEnum.VIDEO.getValue().equals(annotationInfoCreateDTO.getDataType())
                || dataset.getAnnotateType().equals(AnnotateTypeEnum.AUDIO_CLASSIFY.getValue())) {
            saveDatasetFileAnnotationsForUpload(annotationInfoCreateDTO);
        }
    }

    @Override
    @DataPermissionMethod
    public void finishManualForAutoLabel(Long fileId, Long datasetId, AnnotationInfoCreateDTO annotationInfoCreateDTO, boolean isModified) {
        annotationInfoCreateDTO.setDatasetId(datasetId);
        Dataset dataset = datasetService.getOneById(datasetId);

        // 判断数据集是否在发布中
        if (!StringUtils.isBlank(dataset.getCurrentVersionName())) {
            if (datasetVersionService.getDatasetVersionSourceVersion(dataset).getDataConversion().equals(NumberConstant.NUMBER_4)) {
                throw new BusinessException(ErrorEnum.DATASET_PUBLISH_ERROR);
            }
        }
        datasetVersionFileService.deleteByFileId(datasetId, fileId);
        annotationInfoCreateDTO.setId(fileId);
        annotationInfoCreateDTO.setDataType(dataset.getDataType());
        annotationInfoCreateDTO.setCurrentVersionName(dataset.getCurrentVersionName());
        doSave(annotationInfoCreateDTO);

        saveDatasetFileAnnotationsForAutoLabel(annotationInfoCreateDTO,isModified);
    }




    @DataPermissionMethod
    @Override
    public void finishManualForUploadBatch(
            List<Long> fileIds,
            Long datasetId,
            List<AnnotationInfoCreateDTO> annotationInfoList
    ) {
        if (fileIds == null || annotationInfoList == null || fileIds.size() != annotationInfoList.size()) {
            throw new IllegalArgumentException("fileIds 和 annotationInfoList 数量不一致！");
        }
        Dataset dataset = datasetService.getOneById(datasetId);
        // 判断数据集是否在发布中
        if (!StringUtils.isBlank(dataset.getCurrentVersionName())) {
            if (datasetVersionService.getDatasetVersionSourceVersion(dataset).getDataConversion().equals(NumberConstant.NUMBER_4)) {
                throw new BusinessException(ErrorEnum.DATASET_PUBLISH_ERROR);
            }
        }

        // ---------- 1. 批量删除 ----------
        //datasetVersionFileService.deleteByFileIds(datasetId, fileIds);

        for (int i = 0; i < fileIds.size(); i++) {
            Long fileId = fileIds.get(i);
            AnnotationInfoCreateDTO annotationInfoCreateDTO = annotationInfoList.get(i);
            annotationInfoCreateDTO.setId(fileId);
            annotationInfoCreateDTO.setDatasetId(datasetId);
            annotationInfoCreateDTO.setDataType(dataset.getDataType());
            annotationInfoCreateDTO.setCurrentVersionName(dataset.getCurrentVersionName());
        }
        // ---------- 3. 批量写入 ----------
        doSaveBatch(annotationInfoList);

        // ---------- 4. 批量处理DB标注保存 ----------
        if (dataset.getAnnotateType().equals(AnnotateTypeEnum.TEXT_CLASSIFICATION.getValue())
                || dataset.getAnnotateType().equals(AnnotateTypeEnum.AUDIO_CLASSIFY.getValue())) {
            // 文本或音频分类，直接批量处理全部
            try {
                saveDatasetFileAnnotationsForUploadBatch(annotationInfoList);
            }catch (Exception e){
                throw e;
            }


        } else {
            // 图像、视频等类型，先筛选类型再批量处理
            List<AnnotationInfoCreateDTO> imageOrVideoList = annotationInfoList.stream()
                    .filter(dto -> DatatypeEnum.IMAGE.getValue().equals(dto.getDataType())
                            || DatatypeEnum.VIDEO.getValue().equals(dto.getDataType()))
                    .collect(Collectors.toList());
            if (!imageOrVideoList.isEmpty()) {
                try {
                    saveDatasetFileAnnotationsForUploadBatch(imageOrVideoList);
                }catch (Exception e){
                    throw e;
                }
            }
        }

    }



//    /**
//     * 重新自动标注
//     *
//     * @param annotationDeleteDTO 标注清除条件
//     * @return boolean 清除标注是否成功
//     */
//    @Transactional(rollbackFor = Exception.class)
//    @Override
//    @DataPermissionMethod
//    public void reAuto(AnnotationDeleteDTO annotationDeleteDTO) {
//        Dataset dataset = datasetService.getOneById(annotationDeleteDTO.getDatasetId());
//        if (!Objects.isNull(dataset)) {
//            verificationAnnotationCondition(dataset.getAnnotateType());
//        }
//        //判断数据集是否在发布中
//        if (!StringUtils.isBlank(dataset.getCurrentVersionName())) {
//            if (datasetVersionService.getDatasetVersionSourceVersion(dataset).getDataConversion().equals(NumberConstant.NUMBER_4)) {
//                throw new BusinessException(ErrorEnum.DATASET_PUBLISH_ERROR);
//            }
//        }
//        //改数据集相关状态
//        StateMachineUtil.stateChange(new StateChangeDTO() {{
//            setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
//            setEventMethodName(DataStateMachineConstant.DATA_DELETE_ANNOTATING_EVENT);
//            setObjectParam(new Object[]{annotationDeleteDTO.getDatasetId().intValue()});
//        }});
//
//        //根据当前数据集ID修改Changed字段为改变
//        datasetVersionFileService.updateChanged(annotationDeleteDTO.getDatasetId(), dataset.getCurrentVersionName());
//
//        List<Long> taskIds = taskService.auto(new AutoAnnotationCreateDTO() {{
//            setDatasetIds(new Long[]{annotationDeleteDTO.getDatasetId()});
//            setType(DataTaskTypeEnum.AGAIN_ANNOTATION.getValue());
//        }});
//        //更新task任务类型为重新自动标注
//        if (CollectionUtil.isNotEmpty(taskIds)) {
//            taskIds.stream().forEach(aLong -> {
//                Task task = taskService.detail(aLong);
//                task.setType(DataTaskTypeEnum.AGAIN_ANNOTATION.getValue());
//                taskService.updateByTaskId(task);
//            });
//        }
//    }

    /**
     * 标注文件删除
     *
     * @param files 文件set
     */
    public void delete(Set<File> files) {
        files.forEach(this::delete);
    }

    /**
     * 标注文件删除
     *
     * @param file 文件
     */
    public void delete(File file) {
        if (file == null) {
            return;
        }
        String filePath = fileUtil.getWriteAnnotationAbsPath(file.getDatasetId(), file.getName());
        storeService.delete(filePath);
        LogUtil.info(LogEnum.BIZ_DATASET, "delete file. file:{}", filePath);
    }

    /**
     * 获取任务map
     *
     * @return Map<String, TaskSplitBO> 当前正在进行中的任务(已经发送给算法的)
     */
    @Override
    public Map<String, TaskSplitBO> getTaskPool() {
        return autoAnnotating;
    }

    /**
     * 完成自动标注
     *
     * @param taskId                       子任务id
     * @param batchAnnotationInfoCreateDTO 标注信息
     * @return boolean 标注任务完成return true 失败抛出异常
     */
    @Override
    public boolean finishAuto(String taskId, BatchAnnotationInfoCreateDTO batchAnnotationInfoCreateDTO) {
        LogUtil.info(LogEnum.BIZ_DATASET, "finishAuto log is:" + taskId, batchAnnotationInfoCreateDTO);
        TaskSplitBO taskSplitBO = autoAnnotating.get(taskId);
        if (taskSplitBO == null) {
            throw new BusinessException(ErrorEnum.TASK_SPLIT_ABSENT);
        }
        doFinishAuto(taskSplitBO, batchAnnotationInfoCreateDTO.toMap());
        return true;
    }

    /**
     * 查询需要目标跟踪的数据集
     *
     * @return List<Dataset> 需要目标跟踪的数据集
     */
    public List<Dataset> queryDatasetsToBeTracked() {
        //读所有数据集
        QueryWrapper<Dataset> datasetQueryWrapper = new QueryWrapper<>();
        datasetQueryWrapper.lambda()
                .eq(Dataset::getDataType, DatatypeEnum.VIDEO.getValue())
                .in(Dataset::getStatus, Constant.AUTO_TRACK_NEED_STATUS);
        return datasetService.queryList(datasetQueryWrapper);
    }

    /**
     * 根据当前版本和状态查询文件
     *
     * @param dataset 数据集
     * @return Map<Long, List < DatasetVersionFile>> 根据当前版本和状态查询文件列表
     */
    @Override
    public Map<Long, List<DatasetVersionFile>> queryFileAccordingToCurrentVersionAndStatus(Dataset dataset) {
        Map<Long, List<DatasetVersionFile>> fileMap = new HashMap<>(MagicNumConstant.SIXTEEN);
        //根据数据集读数据集版本文件中间表
        List<DatasetVersionFile> fileList = filterFilesThatNeedToBeTracked(dataset.getId(), dataset.getCurrentVersionName());
        if (fileList != null) {
            fileMap.put(dataset.getId(), fileList);
        }
        return fileMap;
    }

    /**
     * 筛选需要跟踪的文件
     *
     * @param datasetId   数据集id
     * @param versionName 版本名称
     * @return List<DatasetVersionFile> 版本文件列表
     */
    public List<DatasetVersionFile> filterFilesThatNeedToBeTracked(Long datasetId, String versionName) {
        List<DatasetVersionFile> versionFiles = datasetVersionFileService.getFilesByDatasetIdAndVersionName(datasetId, versionName);
        long size = versionFiles.stream().filter(f ->
                !FileStateCodeConstant.AUTO_TAG_COMPLETE_FILE_STATE.equals(f.getStatus()) || FileStateCodeConstant.ANNOTATION_COMPLETE_FILE_STATE.equals(f.getStatus())).count();
        return size == versionFiles.size() ? versionFiles : null;
    }

    /**
     * 完成自动标注
     *
     * @param taskSplit 标注任务
     * @param resMap    标注文件保存条件
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<Long, AnnotationInfoCreateDTO> doFinishAuto(TaskSplitBO taskSplit, Map<Long, AnnotationInfoCreateDTO> resMap) {
        LogUtil.info(LogEnum.BIZ_DATASET, "finish auto. ts:{}, resMap:{}", taskSplit, resMap);
        //图片状态变更为自动标注完成
        Dataset dataset = datasetService.getOneById(taskSplit.getDatasetId());
        //保存标注信息
        List<Label> labels = datasetLabelService.listLabelByDatasetId(dataset.getId());
        List<Label> uniqueLabels = labels.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(
                () -> new TreeSet<>(Comparator.comparing(l -> l.getName() == null ? "" : l.getName()))), ArrayList::new));
        // 标签名匹配采用两级策略：先按原始大小写精确匹配（保留库中同时存在 "Person"/"PERSON" 等多大小写并存场景的可区分性）；
        // 精确匹配不到再做大小写不敏感兜底，避免模型输出 "person" 与库中 "PERSON" 因大小写不一致而创建重复标签。
        Map<String, Long> exactNameMap = uniqueLabels.stream()
                .filter(l -> l.getName() != null)
                .collect(Collectors.toMap(Label::getName, Label::getId, (a, b) -> a));
        Map<String, Long> lowerNameMap = uniqueLabels.stream()
                .filter(l -> l.getName() != null)
                .collect(Collectors.toMap(l -> l.getName().toLowerCase(), Label::getId, (a, b) -> a));
        taskSplit.getFiles().forEach(fileBO -> {
            AnnotationInfoCreateDTO annotationInfo = resMap.get(fileBO.getId());
            if (annotationInfo == null) {
                return;
            }
            JSONArray jsonArray = JSONObject.parseObject(annotationInfo.getAnnotation(), JSONArray.class);
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String categoryName = jsonObject.getString("category_id");
                Long labelId = null;
                if (categoryName != null) {
                    labelId = exactNameMap.get(categoryName);
                    if (labelId == null) {
                        labelId = lowerNameMap.get(categoryName.toLowerCase());
                    }
                }
                jsonObject.put("category_id", labelId);
            }
            storeService.write(fileUtil.getAnnotationAbsPath(taskSplit.getDatasetId(), fileBO.getName()), jsonArray.toJSONString());
            annotationInfo.setAnnotation(jsonArray.toJSONString());
            resMap.put(fileBO.getId(), annotationInfo);
        });
        taskSplit.setVersionName(dataset.getCurrentVersionName());
        List<DatasetVersionFile> versionFiles = datasetVersionFileService.getVersionFileByDatasetAndFile(dataset.getId(), dataset.getCurrentVersionName(), resMap.keySet());
        //清空之前的数据库标注信息
        List<Long> versionFileIds = versionFiles.stream().map(DatasetVersionFile::getId).collect(Collectors.toList());
        dataFileAnnotationService.deleteBatch(dataset.getId(), versionFileIds);
        //写入标签关系
        if (!CollectionUtils.isEmpty(resMap)) {
            List<DataFileAnnotation> dataFileAnnotations = new ArrayList<>();
            versionFiles.forEach(versionFile -> {
                List<Long> dbLabelIds = dataFileAnnotationService.findInfoByVersionId(dataset.getId(), versionFile.getId());
                if (!CollectionUtil.isEmpty(dbLabelIds)) {
                    dataFileAnnotationService.deleteAnnotationFileByVersionIdAndLabelIds(dataset.getId(), versionFile.getId(), dbLabelIds);
                }
                List<AnnotationDTO> annotationDTOS = JSONObject.parseArray(resMap.get(versionFile.getFileId()).getAnnotation(), AnnotationDTO.class);
                if (!CollectionUtils.isEmpty(annotationDTOS)) {
                    if (AnnotateTypeEnum.CLASSIFICATION.getValue().equals(dataset.getAnnotateType()) || AnnotateTypeEnum.TEXT_CLASSIFICATION.getValue().equals(dataset.getAnnotateType())) {
                        AnnotationDTO annotationDTO = annotationDTOS.stream().max(Comparator.comparingDouble(AnnotationDTO::getScore)).get();
                        dataFileAnnotations.add(new DataFileAnnotation(dataset.getId(), annotationDTO.getCategoryId(), versionFile.getId(), annotationDTOS.get(0).getScore(), versionFile.getFileName()));
                    }
                    if (AnnotateTypeEnum.OBJECT_DETECTION.getValue().equals(dataset.getAnnotateType()) || AnnotateTypeEnum.OBJECT_TRACK.getValue().equals(dataset.getAnnotateType())
                            || AnnotateTypeEnum.SEMANTIC_CUP.getValue().equals(dataset.getAnnotateType())) {
                        annotationDTOS.forEach(annotationDTO -> {
                            dataFileAnnotations.add(new DataFileAnnotation(dataset.getId(), annotationDTO.getCategoryId(), versionFile.getId(), annotationDTO.getScore(), versionFile.getFileName()));
                        });
                    }
                }
            });
            if (!CollectionUtils.isEmpty(dataFileAnnotations)) {
                Queue<Long> dataFileAnnotionIds = generatorKeyUtil.getSequenceByBusinessCode(Constant.DATA_FILE_ANNOTATION, dataFileAnnotations.size());
                for (DataFileAnnotation dataFileAnnotation : dataFileAnnotations) {
                    dataFileAnnotation.setId(dataFileAnnotionIds.poll());
                    dataFileAnnotation.setStatus(MagicNumConstant.ZERO);
                    dataFileAnnotation.setInvariable(MagicNumConstant.ZERO);
                }
                dataFileAnnotationService.insertDataFileBatch(dataFileAnnotations);
            }
        }

        HashSet<Long> annotationInfoIsNotEmpty = new HashSet<Long>() {{
            addAll(resMap.keySet().stream().filter(k -> !JSON.parseArray(resMap.get(k).getAnnotation()).isEmpty()).collect(Collectors.toSet()));
        }};
        //嵌入状态机（改变文件状态，标记文件状态被改变）->改变有标注数据的文件
        if (!annotationInfoIsNotEmpty.isEmpty()) {
            StateMachineUtil.stateChange(new StateChangeDTO() {{
                setObjectParam(new Object[]{annotationInfoIsNotEmpty, taskSplit.getDatasetId(), taskSplit.getVersionName()});
                setEventMethodName(FileStateMachineConstant.FILE_DO_FINISH_AUTO_ANNOTATION_BATCH_EVENT);
                setStateMachineType(FileStateMachineConstant.FILE_STATE_MACHINE);
            }});
        }
        HashSet<Long> annotationInfoIsEmpty = new HashSet<Long>() {{
            addAll(resMap.keySet().stream().filter(k -> JSON.parseArray(resMap.get(k).getAnnotation()).isEmpty()).collect(Collectors.toSet()));
        }};
        //嵌入状态机（改变文件状态，标记文件状态被改变）->改变无标注数据的文件
        if (!annotationInfoIsEmpty.isEmpty()) {
            StateMachineUtil.stateChange(new StateChangeDTO() {{
                setObjectParam(new Object[]{annotationInfoIsEmpty, taskSplit.getDatasetId(), taskSplit.getVersionName()});
                setEventMethodName(FileStateMachineConstant.FILE_DO_FINISH_AUTO_ANNOTATION_INFO_IS_EMPTY_BATCH_EVENT);
                setStateMachineType(FileStateMachineConstant.FILE_STATE_MACHINE);
            }});
        }
//        if (taskSplit.getAnnotateType().equals(MagicNumConstant.SEVEN)) {
//            List<FileBO> fileBOS = taskSplit.getFiles();
//            fileBOS.forEach(fileBO -> fileService.recoverEsStatus(taskSplit.getDatasetId(),fileBO.getId()));
//        }
        //任务加文件数量
        taskService.finishFile(taskSplit.getTaskId(), taskSplit.getFiles().size(), dataset);
        return resMap;
    }


    /**
     * 完成目标跟踪
     *
     * @param datasetId          数据集id
     * @param autoTrackCreateDTO 自动跟踪结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void finishAutoTrack(Long datasetId, AutoTrackCreateDTO autoTrackCreateDTO) {
        if (!ResponseCode.SUCCESS.equals(autoTrackCreateDTO.getCode())) {
            LogUtil.info(LogEnum.BIZ_DATASET, "auto track is error" + autoTrackCreateDTO.getMsg());
            return;
        }
        LogUtil.info(LogEnum.BIZ_DATASET, "target tracking success modify status");
        Dataset dataset = datasetService.getOneById(datasetId);
        if (dataset == null) {
            LogUtil.error(LogEnum.BIZ_DATASET, "datasetId can't null");
        } else if (!DatatypeEnum.VIDEO.getValue().equals(dataset.getDataType())) {
            LogUtil.error(LogEnum.BIZ_DATASET, "wrong dataset type, not video. dataset:{}", datasetId);
        } else {
            //嵌入状态机（目标跟踪中—>目标跟踪完成）
            StateMachineUtil.stateChange(new StateChangeDTO() {{
                setObjectParam(new Object[]{dataset});
                setEventMethodName(DataStateMachineConstant.DATA_TARGET_COMPLETE_EVENT);
                setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
            }});
            //嵌入状态机（自动标注完成->目标跟踪完成）
            StateMachineUtil.stateChange(new StateChangeDTO() {{
                setObjectParam(new Object[]{dataset});
                setEventMethodName(FileStateMachineConstant.FILE_DO_FINISH_AUTO_TRACK_EVENT);
                setStateMachineType(FileStateMachineConstant.FILE_STATE_MACHINE);
            }});
            tracking.remove(datasetId);
            LogUtil.info(LogEnum.BIZ_DATASET, "target tracking is complete dataset:{}", datasetId);
        }
        LogUtil.info(LogEnum.BIZ_DATASET, "exception update of target tracking algorithm callback. dataset:{}", datasetId);
    }

//    /**
//     * 重新目标跟踪
//     *
//     * @param datasetId
//     */
//    @Override
//    public void track(Long datasetId, Long modelServiceId) {
//        // 检查模型服务是否存在，以及模型是否运行中
//        AutoLabelModelService autoLabelModelService = autoLabelModelServiceService.getOneById(modelServiceId);
//        if (ObjectUtil.isNull(autoLabelModelService) || !AutoLabelModelServiceStatusEnum.checkAvailable(autoLabelModelService.getStatus())) {
//            throw new BusinessException(ErrorEnum.MODEL_SERVER_NOT_AVAILABLE);
//        }
//        Dataset dataset = datasetService.getOneById(datasetId);
//        if (dataset == null || !DatatypeEnum.VIDEO.getValue().equals(dataset.getDataType())) {
//            throw new BusinessException(ErrorEnum.DATASET_TRACK_TYPE_ERROR);
//        }
//        //判断数据集是否在发布中
//        if (!StringUtils.isBlank(dataset.getCurrentVersionName())) {
//            if (datasetVersionService.getDatasetVersionSourceVersion(dataset).getDataConversion().equals(NumberConstant.NUMBER_4)) {
//                throw new BusinessException(ErrorEnum.DATASET_PUBLISH_ERROR);
//            }
//        }
//        taskService.track(dataset, modelServiceId);
//    }

    /**
     * 重新自动标注更新文件状态
     *
     * @param datasetId 数据集ID
     */
    @Override
    public void deleteAnnotating(Long datasetId) {
        datasetVersionFileService.deleteAnnotating(datasetId);
    }

    @Override
    public void finishAnnotation(JSONObject taskDetail) {
        JSONObject jsonObject = JSON.parseObject(taskDetail.get("object").toString(), JSONObject.class);
        TaskSplitBO taskSplitBO = JSON.parseObject(JSON.toJSONString(taskDetail), TaskSplitBO.class);
        JSONArray jsonArray = jsonObject.getJSONArray("annotations");
        List<AnnotationInfoCreateDTO> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            list.add(JSON.toJavaObject(jsonArray.getJSONObject(i), AnnotationInfoCreateDTO.class));
        }
        BatchAnnotationInfoCreateDTO batchAnnotationInfoCreateDTO = new BatchAnnotationInfoCreateDTO();
        batchAnnotationInfoCreateDTO.setAnnotations(list);
        doFinishAuto(taskSplitBO, batchAnnotationInfoCreateDTO.toMap());
    }

    /**
     * 通过标注类型验证自动标注条件
     *
     * @param annotationType 自动标注类型
     */
    private void verificationAnnotationCondition(Integer annotationType) {
        if (AnnotateTypeEnum.SEMANTIC_CUP.getValue().compareTo(annotationType) == 0) {
            throw new BusinessException(AnnotateTypeEnum.SEMANTIC_CUP.getMsg() + ErrorEnum.DATASET_NOT_ANNOTATION);
        }
    }

    /**
     * 保存数据集文件标注信息
     *
     * @param annotationInfoCreateDTO 标注详情实体
     */
    private void saveDatasetFileAnnotationsByImage(AnnotationInfoCreateDTO annotationInfoCreateDTO) {

        List<AnnotationDTO> annotationDTOS = JSONObject.parseArray(annotationInfoCreateDTO.getAnnotation(), AnnotationDTO.class);
        if (CollectionUtil.isEmpty(annotationDTOS)) {
            return;
        }
        Long datasetId = annotationInfoCreateDTO.getDatasetId();
        DatasetVersionFile datasetVersionFile = datasetVersionFileService.getDatasetVersionFile(
                datasetId, annotationInfoCreateDTO.getCurrentVersionName(), annotationInfoCreateDTO.getId());
        if (Objects.isNull(datasetVersionFile)) {
            throw new BusinessException(ErrorEnum.DATASET_VERSION_FILE_IS_ERROR);
        }
        Long versionFileId = datasetVersionFile.getId();
        List<Long> fileLabelIds = annotationDTOS.stream().map(a -> a.getCategoryId()).collect(Collectors.toList());
        List<Long> dbLabelIds = dataFileAnnotationService.findInfoByVersionId(datasetId, versionFileId);
        if (!CollectionUtil.isEmpty(dbLabelIds)) {
            dataFileAnnotationService.deleteAnnotationFileByVersionIdAndLabelIds(datasetId, versionFileId, dbLabelIds);
        }
        dataFileAnnotationService.insertAnnotationFileByVersionIdAndLabelIds(datasetId, versionFileId, fileLabelIds, datasetVersionFile.getFileName());
    }

//    /**
//     * 清除es中的标注信息
//     *
//     * @param datasetId 数据集id
//     */
//    @Override
//    public void deleteEsData(Long datasetId) {
//        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery().must(QueryBuilders.termQuery("datasetId",datasetId.toString()));
//        UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest(esIndex);
//        updateByQueryRequest.setRefresh(true).setScript(new Script("ctx._source['status']='101'"))
//                .setQuery(boolQueryBuilder);
//        try{
//            restHighLevelClient.updateByQuery(updateByQueryRequest,RequestOptions.DEFAULT);
//        } catch (Exception e){
//            LogUtil.info(LogEnum.BIZ_DATASET, "delete es annotation error:", e);
//        }
//    }

    @Override
    public void finishManualBatchForAutoLabel(List<BatchAnnotationInfo> batchAnnotationInfos, Long datasetId, Boolean requireManualConfirmation) {
        if (CollectionUtils.isEmpty(batchAnnotationInfos)) {
            LogUtil.info(LogEnum.BIZ_DATASET, "batchAnnotationInfos 为空，跳过处理");
            return;
        }

        LogUtil.info(LogEnum.BIZ_DATASET, "开始批量处理标注，文件数量: {}", batchAnnotationInfos.size());

        Dataset dataset = datasetService.getOneById(datasetId);
        LogUtil.info(LogEnum.BIZ_DATASET, "数据集信息 - ID: {}, 当前版本: {}", datasetId, dataset.getCurrentVersionName());

        // 判断数据集是否在发布中
        if (!StringUtils.isBlank(dataset.getCurrentVersionName())) {
            if (datasetVersionService.getDatasetVersionSourceVersion(dataset).getDataConversion().equals(NumberConstant.NUMBER_4)) {
                throw new BusinessException(ErrorEnum.DATASET_PUBLISH_ERROR);
            }
        }

        // 1. 先查询版本文件信息（在删除之前）
        List<Long> fileIds = batchAnnotationInfos.stream()
                .map(BatchAnnotationInfo::getFileId)
                .collect(Collectors.toList());

        LogUtil.info(LogEnum.BIZ_DATASET, "准备查询版本文件，fileIds: {}", fileIds);

        Map<Long, DatasetVersionFile> versionFileMap = datasetVersionFileService.getVersionFileMapByFileIds(
                datasetId, dataset.getCurrentVersionName(), fileIds);

        LogUtil.info(LogEnum.BIZ_DATASET, "查询到版本文件数量: {}, 需要处理文件数量: {}",
                versionFileMap.size(), fileIds.size());

        // 打印详细的查询结果
        for (Long fileId : fileIds) {
            DatasetVersionFile versionFile = versionFileMap.get(fileId);
            if (versionFile == null) {
                LogUtil.warn(LogEnum.BIZ_DATASET, "未找到版本文件 - fileId: {}, datasetId: {}, versionName: {}",
                        fileId, datasetId, dataset.getCurrentVersionName());
            }
        }

        // 2. 批量删除这些版本文件对应的标注信息（这里应该还能优化一下，可以直接传进去versionId）
        datasetVersionFileService.deleteByFileIds(datasetId, fileIds);
        LogUtil.info(LogEnum.BIZ_DATASET, "完成批量删除版本文件关联");

        // 3. 向文件里面写入标注信息
        List<AnnotationInfoCreateDTO> annotationInfoList = batchAnnotationInfos.stream()
                .map(batchInfo -> {
                    AnnotationInfoCreateDTO dto = AnnotationInfoCreateDTO.builder()
                            .id(batchInfo.getFileId())
                            .datasetId(datasetId)
                            .dataType(dataset.getDataType())
                            .currentVersionName(dataset.getCurrentVersionName())
                            .annotation(batchInfo.getAnnotation())
                            .build();
                    return dto;
                })
                .collect(Collectors.toList());

        doSaveBatch(annotationInfoList);
        LogUtil.info(LogEnum.BIZ_DATASET, "完成批量保存标注信息");

        // 4. 批量保存标注的数据库信息
        saveDatasetFileAnnotationsBatchForAutoLabel(batchAnnotationInfos, datasetId, dataset.getCurrentVersionName(), versionFileMap, requireManualConfirmation);
    }

    /**
     * 批量保存数据集文件标注信息
     */
    private void saveDatasetFileAnnotationsBatchForAutoLabel(List<BatchAnnotationInfo> batchAnnotationInfos,
                                                             Long datasetId, String currentVersionName,
                                                             Map<Long, DatasetVersionFile> preQueryVersionFileMap,
                                                             Boolean requireManualConfirmation) {
        if (CollectionUtils.isEmpty(batchAnnotationInfos)) {
            LogUtil.info(LogEnum.BIZ_DATASET, "batchAnnotationInfos 为空，跳过版本文件标注处理");
            return;
        }

        LogUtil.info(LogEnum.BIZ_DATASET, "开始批量处理文件标注，文件数量: {}, 预查询版本文件数量: {}",
                batchAnnotationInfos.size(), preQueryVersionFileMap.size());

        // 分组处理不同状态的文件
        List<DatasetVersionFile> toUpdateNotAnnotation = new ArrayList<>();
        List<DatasetVersionFile> toUpdateAnnotated = new ArrayList<>();
        List<Long> versionFileIdsToDeleteAnnotations = new ArrayList<>();
        List<BatchFileAnnotationInfo> toInsertAnnotations = new ArrayList<>();

        int processedCount = 0;
        int skippedCount = 0;

        for (BatchAnnotationInfo batchInfo : batchAnnotationInfos) {
            DatasetVersionFile datasetVersionFile = preQueryVersionFileMap.get(batchInfo.getFileId());
            if (Objects.isNull(datasetVersionFile)) {
                LogUtil.warn(LogEnum.BIZ_DATASET, "跳过处理 - 未找到版本文件，fileId: {}", batchInfo.getFileId());
                skippedCount++;
                continue;
            }

            processedCount++;
            List<AnnotationDTO> annotationDTOS = JSONObject.parseArray(batchInfo.getAnnotation(), AnnotationDTO.class);

            if (batchInfo.isModified()) {
                // 自动标注改过的：如果需要人工确认则设置为未标注，否则设置为已标注
                if (requireManualConfirmation != null && requireManualConfirmation) {
                    toUpdateNotAnnotation.add(DatasetVersionFile.builder()
                            .id(datasetVersionFile.getId())
                            .datasetId(datasetVersionFile.getDatasetId())
                            .annotationStatus(FileStateEnum.NOT_ANNOTATION_FILE_STATE.getCode())
                            .changed(MagicNumConstant.ONE)
                            .build());
                } else {
                    // 不需要人工确认，直接设置为已标注
                    Long versionFileId = datasetVersionFile.getId();
                    List<Long> fileLabelIds = annotationDTOS.stream()
                            .map(AnnotationDTO::getCategoryId)
                            .collect(Collectors.toList());

                    toUpdateAnnotated.add(DatasetVersionFile.builder()
                            .id(datasetVersionFile.getId())
                            .datasetId(datasetVersionFile.getDatasetId())
                            .annotationStatus(FileStateEnum.ANNOTATION_COMPLETE_FILE_STATE.getCode())
                            .changed(MagicNumConstant.ONE)
                            .build());

                    versionFileIdsToDeleteAnnotations.add(versionFileId);
                    toInsertAnnotations.add(BatchFileAnnotationInfo.builder()
                            .datasetId(datasetId)
                            .versionFileId(versionFileId)
                            .labelIds(fileLabelIds)
                            .fileName(datasetVersionFile.getFileName())
                            .build());
                }
            }else{
                // 自动标注没改过的：如果不需要人工确认则设置为已标注
                if (requireManualConfirmation != null && !requireManualConfirmation) {
                    toUpdateNotAnnotation.add(DatasetVersionFile.builder()
                            .id(datasetVersionFile.getId())
                            .datasetId(datasetVersionFile.getDatasetId())
                            .annotationStatus(FileStateEnum.ANNOTATION_COMPLETE_FILE_STATE.getCode())
                            .build());
                } else if (requireManualConfirmation != null) {
                    //需要人工确认则设置为未标注
                    toUpdateNotAnnotation.add(DatasetVersionFile.builder()
                            .id(datasetVersionFile.getId())
                            .datasetId(datasetVersionFile.getDatasetId())
                            .annotationStatus(FileStateEnum.NOT_ANNOTATION_FILE_STATE.getCode())
                            .build());
                }
            }
        }

        LogUtil.info(LogEnum.BIZ_DATASET, "批量处理统计 - 已处理: {}, 跳过: {}, 设为未标注: {}, 设为已标注: {}, 需删除标注: {}, 需插入标注: {}",
                processedCount, skippedCount, toUpdateNotAnnotation.size(), toUpdateAnnotated.size(),
                versionFileIdsToDeleteAnnotations.size(), toInsertAnnotations.size());

        // 批量执行数据库操作
        if (!CollectionUtils.isEmpty(toUpdateNotAnnotation)) {
            LogUtil.info(LogEnum.BIZ_DATASET, "执行批量更新为未标注状态，数量: {}", toUpdateNotAnnotation.size());
            datasetVersionFileService.updateStatusBatch(toUpdateNotAnnotation);
        }

        if (!CollectionUtils.isEmpty(toUpdateAnnotated)) {
            LogUtil.info(LogEnum.BIZ_DATASET, "执行批量更新为已标注状态，数量: {}", toUpdateAnnotated.size());
            datasetVersionFileService.updateStatusBatch(toUpdateAnnotated);
        }

        // 批量删除和插入标注
        if (!CollectionUtils.isEmpty(versionFileIdsToDeleteAnnotations)) {
            LogUtil.info(LogEnum.BIZ_DATASET, "查询需要删除的标注，版本文件数量: {}", versionFileIdsToDeleteAnnotations.size());
            Map<Long, List<Long>> versionFileToLabelIds = dataFileAnnotationService.findInfoByVersionIds(
                    datasetId, versionFileIdsToDeleteAnnotations);

            LogUtil.info(LogEnum.BIZ_DATASET, "找到需要删除标注的版本文件数量: {}", versionFileToLabelIds.size());

            for (Map.Entry<Long, List<Long>> entry : versionFileToLabelIds.entrySet()) {
                if (!CollectionUtils.isEmpty(entry.getValue())) {
//                    LogUtil.info(LogEnum.BIZ_DATASET, "删除标注 - 版本文件ID: {}, 标签数量: {}",
//                            entry.getKey(), entry.getValue().size());
                    dataFileAnnotationService.deleteAnnotationFileByVersionIdAndLabelIds(
                            datasetId, entry.getKey(), entry.getValue());
                }
            }
        }

        // 批量插入新标注
        if (!CollectionUtils.isEmpty(toInsertAnnotations)) {
            LogUtil.info(LogEnum.BIZ_DATASET, "执行批量插入标注，文件数量: {}", toInsertAnnotations.size());
            dataFileAnnotationService.insertAnnotationFileBatch(toInsertAnnotations);
        }

        LogUtil.info(LogEnum.BIZ_DATASET, "批量文件标注处理完成");
    }
}