package org.dubhe.data.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.dubhe.biz.base.constant.MagicNumConstant;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.dubhe.biz.redis.utils.RedisUtils;
import org.dubhe.biz.statemachine.dto.StateChangeDTO;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.constant.ErrorEnum;
import org.dubhe.data.domain.bo.DatasetFileBO;
import org.dubhe.data.domain.bo.EnhanceTaskSplitBO;
import org.dubhe.data.domain.dto.DatasetEnhanceFinishDTO;
import org.dubhe.data.domain.dto.DatasetEnhanceRequestDTO;
import org.dubhe.data.domain.dto.FileCreateDTO;
import org.dubhe.data.domain.entity.DatasetVersionFile;
import org.dubhe.data.domain.entity.File;
import org.dubhe.data.domain.entity.Label;
import org.dubhe.data.domain.entity.Task;
import org.dubhe.data.domain.vo.FileVO;
import org.dubhe.data.machine.constant.DataStateMachineConstant;
import org.dubhe.data.machine.utils.StateMachineUtil;
import org.dubhe.data.service.*;
import org.dubhe.data.util.FileUtil;
import org.dubhe.data.util.TaskUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description 数据集增强
 * @date 2020-06-28
 */
@Service
public class DatasetEnhanceServiceImpl implements DatasetEnhanceService {

    @Autowired
    private DatasetVersionFileService datasetVersionFileService;
    @Autowired
    private FileService fileService;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private TaskUtils taskUtils;
    @Autowired
    private MinioUtil minioUtil;
    @Autowired
    @Lazy
    private TaskService taskService;
    @Value("${data.enhance.task.splitSize:64}")
    private Integer taskSplitSize;
    @Value("${storage.file-store-root-path}")
    private String nfs;

    @Autowired
    private FileUtil fileUtil;

    @Autowired
    private DataFileAnnotationService dataFileAnnotationService;

    @Autowired
    private DatasetLabelService datasetLabelService;

    /**
     * 批量查询大小限制，避免IN子句过大
     */
    private static final int BATCH_QUERY_SIZE = 1000;

    /**
     * 提交任务
     *
     * @param datasetVersionFiles      数据版本文件关系列表
     * @param task                     任务
     * @param datasetEnhanceRequestDTO 数据增强请求条件
     */
    @Override
    public void commitEnhanceTask(List<DatasetVersionFile> datasetVersionFiles, Task task, DatasetEnhanceRequestDTO datasetEnhanceRequestDTO, String taskQueue, String detail, String placeholder) {
        try {
            System.out.println("commitEnhanceTask");
            Set<File> fileSet = fileService.get(datasetVersionFiles.stream().
                    map(DatasetVersionFile::getFileId).collect(Collectors.toList()), task.getDatasetId());
            List<File> fileList = new ArrayList<>(fileSet);
            Collections.shuffle(fileList);
            int total = task.getTotal();
            int typeCount = datasetEnhanceRequestDTO.getTypes().size();
            Map<Long, DatasetVersionFile> datasetVersionFilesMap = new HashMap<>(datasetVersionFiles.size());
            datasetVersionFiles.stream().forEach(datasetVersionFile -> {
                datasetVersionFilesMap.put(datasetVersionFile.getFileId(), datasetVersionFile);
            });
            String enhanceMode = Optional.ofNullable(task.getTaskParams())
                    .map(tp -> JSONObject.parseObject(tp).getString("enhanceMode"))
                    .orElse("parallel");
            System.out.println("enhanceMode = " + enhanceMode);
            
            // 构建标签映射表（标签名称 -> 标签ID），传递给Python端
            List<Label> datasetLabels = datasetLabelService.listLabelByDatasetId(task.getDatasetId());
            Map<String, Long> labelMapping = new HashMap<>();
            datasetLabels.forEach(label -> {
                labelMapping.put(label.getName(), label.getId());
            });

            List<EnhanceTaskSplitBO> tasks = new ArrayList<>();
            // 并行遍历（parallel）
            if ("parallel".equalsIgnoreCase(enhanceMode)) {

                int baseCount = total / typeCount;
                int remainder = total % typeCount;

                for (int i = 0; i < typeCount; i++) {
                    int currentType = datasetEnhanceRequestDTO.getTypes().get(i);
                    // 平均分配 + 多余补足，保证最后总和等于total
                    int count = baseCount + (i < remainder ? 1 : 0);

                    // 打乱后取前 count 个
                    Collections.shuffle(fileList);
                    List<File> selectedFiles = fileList.subList(0, Math.min(count, fileList.size()));

                    List<List<File>> splitFiles = CollectionUtil.split(selectedFiles, taskSplitSize);
                    for (List<File> list : splitFiles) {
                        EnhanceTaskSplitBO enhanceTaskSplitBO = new EnhanceTaskSplitBO(
                                task.getId(),
                                list,
                                datasetVersionFiles.get(MagicNumConstant.ZERO).getDatasetId(),
                                datasetVersionFiles.get(MagicNumConstant.ZERO).getVersionName(),
                                datasetVersionFilesMap,
                                currentType,
                                fileUtil
                        );
                        String uuid = IdUtil.simpleUUID().substring(0, 5);
                        enhanceTaskSplitBO.setReTaskId(uuid);
                        enhanceTaskSplitBO.setAlgorithm(104);
                        // 将标签映射添加到任务参数中
                        JSONObject taskParamsObj = JSONObject.parseObject(task.getTaskParams());
                        taskParamsObj.put("labelMapping", labelMapping);
                        enhanceTaskSplitBO.setTaskParams(taskParamsObj.toJSONString());

                        try {
                            Boolean imgProcessUnprocessed = taskUtils.zAdd(taskQueue.replace(placeholder, uuid), uuid, 10L);
                            if (imgProcessUnprocessed) {
                                redisUtils.set(detail.replace(placeholder, uuid), enhanceTaskSplitBO);
                                System.out.println("enhanceTaskSplitBO = " + enhanceTaskSplitBO);
                            }
                        } catch (Exception e) {
                            StateMachineUtil.stateChange(StateChangeDTO.builder()
                                    .objectParam(new Object[]{datasetEnhanceRequestDTO.getDatasetId().intValue()})
                                    .eventMethodName(DataStateMachineConstant.ENHANCE_ERROR_EVENT)
                                    .stateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE)
                                    .build());
                            LogUtil.error(LogEnum.BIZ_DATASET, "enhancingTask add fail. task:{} exception:{}", enhanceTaskSplitBO, e);
                        }
                    }
                }

            }else {
                //串行叠加（serial）
                List<File> selectedFiles = fileList.stream()
                        .limit(Math.min(total, fileList.size()))
                        .collect(Collectors.toList());
                List<List<File>> files = CollectionUtil.split(selectedFiles, taskSplitSize);
                for (List<File> list : files) {
                    EnhanceTaskSplitBO enhanceTaskSplitBO = new EnhanceTaskSplitBO(
                            task.getId(),
                            list,
                            datasetVersionFiles.get(MagicNumConstant.ZERO).getDatasetId(),
                            datasetVersionFiles.get(MagicNumConstant.ZERO).getVersionName(),
                            datasetVersionFilesMap,
                            datasetEnhanceRequestDTO.getTypes(),
                            fileUtil);
                    String uuid = IdUtil.simpleUUID().substring(0, 5);
                    enhanceTaskSplitBO.setReTaskId(uuid);
                    enhanceTaskSplitBO.setAlgorithm(104);
                    // 将标签映射添加到任务参数中
                    JSONObject taskParamsObj = JSONObject.parseObject(task.getTaskParams());
                    taskParamsObj.put("labelMapping", labelMapping);
                    enhanceTaskSplitBO.setTaskParams(taskParamsObj.toJSONString());
                    try {
                        Boolean imgProcessUnprocessed = taskUtils.zAdd(taskQueue.replace(placeholder, uuid), uuid, 10L);
                        if (imgProcessUnprocessed) {
                            redisUtils.set(detail.replace(placeholder, uuid), enhanceTaskSplitBO);
                            System.out.println("enhanceTaskSplitBO = " + enhanceTaskSplitBO);
                        }
                    } catch (Exception e) {
                        StateMachineUtil.stateChange(StateChangeDTO.builder()
                                .objectParam(
                                        new Object[]{datasetEnhanceRequestDTO.getDatasetId().intValue()})
                                .eventMethodName(DataStateMachineConstant.ENHANCE_ERROR_EVENT)
                                .stateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE)
                                .build());
                        LogUtil.error(LogEnum.BIZ_DATASET, "enhancingTask add fail. task:{} exception:{}", enhanceTaskSplitBO, e);
                    }
                }
            }

        } catch (Exception e) {
            StateMachineUtil.stateChange(StateChangeDTO.builder()
                    .objectParam(
                            new Object[]{datasetEnhanceRequestDTO.getDatasetId().intValue(), datasetVersionFiles.get(MagicNumConstant.ZERO).getVersionName()})
                    .eventMethodName(DataStateMachineConstant.ENHANCE_ERROR_EVENT)
                    .stateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE)
                    .build());
            LogUtil.error(LogEnum.BIZ_DATASET, "enhancingTask add fail. datasetId:{}, message:{}", datasetEnhanceRequestDTO.getDatasetId(), e);
        }
    }

    /**
     * 获取增强完成任务
     *
     * @return
     */
    public boolean getEnhanceFinishedTask(String failedQueue, String finishedQueue) {
        Object failedIdKey = taskUtils.getFailedTask(failedQueue);
        if (ObjectUtil.isNotNull(failedIdKey)) {
            Object object = redisUtils.get(failedIdKey.toString().replace("annotation", "detail"));
            String enhanceTaskSplitBOString = JSON.toJSONString(object);
            EnhanceTaskSplitBO enhanceTaskSplitBO = JSON.parseObject(enhanceTaskSplitBOString, EnhanceTaskSplitBO.class);
            if (taskService.isStop(enhanceTaskSplitBO.getId())) {
                //删除标注
                redisUtils.del(failedIdKey.toString());
                //删除详情
                redisUtils.del(failedIdKey.toString().replace("annotation", "detail"));
                return true;
            }
            Integer fileNum = enhanceTaskSplitBO.getFileDtos().size();
            taskService.finishTaskFile(enhanceTaskSplitBO, fileNum);

            //删除标注
            redisUtils.del(failedIdKey.toString());
            //删除详情
            redisUtils.del(failedIdKey.toString().replace("annotation", "detail"));
        }
        Object object = taskUtils.getFinishedTask(finishedQueue);
        if (ObjectUtil.isNotNull(object)) {
            String taskId = object.toString();
            DatasetEnhanceFinishDTO datasetEnhanceFinishDTO = JSONObject.parseObject(JSON.toJSONString(redisUtils.get(taskId))
                    , DatasetEnhanceFinishDTO.class);
            LogUtil.info(LogEnum.BIZ_DATASET, "start finish enhance task datasetEnhanceFinishDTO:{}", datasetEnhanceFinishDTO);
            enhanceFinish(datasetEnhanceFinishDTO, JSONObject.parseObject(JSON.toJSONString(redisUtils.get(taskId))));
        }
        return ObjectUtil.isNotNull(failedIdKey) || ObjectUtil.isNotNull(object);
    }

    /**
     * 增强任务完成
     *
     * @param datasetEnhanceFinishDTO 增强完成任务详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enhanceFinish(DatasetEnhanceFinishDTO datasetEnhanceFinishDTO, JSONObject taskDetail) {
        EnhanceTaskSplitBO enhanceTaskSplitBO = JSON.parseObject(taskDetail.toJSONString(), EnhanceTaskSplitBO.class);
        if (ObjectUtil.isNull(enhanceTaskSplitBO)) {
            throw new BusinessException(ErrorEnum.TASK_SPLIT_ABSENT);
        }
        Integer fileNum = enhanceTaskSplitBO.getFileDtos().size();
        LogUtil.info(LogEnum.BIZ_DATASET, "DatasetEnhanceServiceImpl enhance finish file count {}", fileNum);
        //写入图片到data_file表
        List<DatasetFileBO> datasetFileBOList = enhanceTaskSplitBO.getFileDtos();
        List<FileCreateDTO> fileDTOList = Lists.transform(datasetFileBOList, new Function<DatasetFileBO, FileCreateDTO>() {
            @Nullable
            @Override
            public FileCreateDTO apply(@Nullable DatasetFileBO datasetFileBO) {
                return new FileCreateDTO(enhanceTaskSplitBO.createEnhanceFilePath(datasetEnhanceFinishDTO.getSuffix(), datasetFileBO).replaceFirst(nfs, ""),
                        datasetFileBO.getFileId(), datasetFileBO.getAnnotationStatus(), enhanceTaskSplitBO.getTypes().get(0), enhanceTaskSplitBO.getUserId(),
                        datasetFileBO.getWidth(), datasetFileBO.getHeight());
            }
        });
        Map<Long, Integer> fileStatus = new HashMap<>(fileDTOList.size());
        datasetFileBOList.stream().forEach(datasetFileBO -> {
            fileStatus.put(datasetFileBO.getFileId(), datasetFileBO.getAnnotationStatus());
        });
        List<File> files = fileService.saveFiles(enhanceTaskSplitBO.getDatasetId(), fileDTOList);

        List<DatasetVersionFile> datasetVersionFileList = new ArrayList<>();
        files.forEach(file -> {
            datasetVersionFileList.add(new DatasetVersionFile(
                    enhanceTaskSplitBO.getDatasetId(),
                    enhanceTaskSplitBO.getVersionName(),
                    file.getId(),
                    fileStatus.get(file.getPid()),
                    file.getName(),
                    enhanceTaskSplitBO.getVersionName() == null ? Constant.UNCHANGED : Constant.CHANGED));
        });
        //文件写入关系表
        datasetVersionFileService.insertList(datasetVersionFileList);

        // 批量查询优化：构建增强文件映射
        Map<Long, File> enhancedFileMap = files.stream()
                .collect(Collectors.toMap(File::getId, file -> file));
        
        // 收集所有父文件ID，用于批量查询
        List<Long> parentFileIds = files.stream()
                .map(File::getPid)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        // 分批批量查询父文件，避免IN子句过大
        Map<Long, FileVO> parentFileMap = new HashMap<>();
        Map<Long, DatasetVersionFile> parentVersionFileMap = new HashMap<>();
        
        if (!parentFileIds.isEmpty()) {
            List<List<Long>> parentFileIdBatches = CollectionUtil.split(parentFileIds, BATCH_QUERY_SIZE);
            for (List<Long> batch : parentFileIdBatches) {
                // 批量查询父文件
                Map<Long, FileVO> batchParentFileMap = fileService.batchGetFileMap(batch, enhanceTaskSplitBO.getDatasetId());
                parentFileMap.putAll(batchParentFileMap);
                
                // 批量查询父文件的版本文件信息
                Map<Long, DatasetVersionFile> batchParentVersionFileMap = datasetVersionFileService.batchGetDatasetVersionFile(
                        enhanceTaskSplitBO.getDatasetId(),
                        enhanceTaskSplitBO.getVersionName(),
                        batch);
                parentVersionFileMap.putAll(batchParentVersionFileMap);
            }
        }
        
        // 构建增强文件到父版本文件的映射关系
        Map<Long, DatasetVersionFile> enhancedToParentVersionFileMap = new HashMap<>();
        Map<Long, FileVO> enhancedToParentFileVOMap = new HashMap<>();
        
        for (DatasetVersionFile datasetVersionFile : datasetVersionFileList) {
            File enhancedFile = enhancedFileMap.get(datasetVersionFile.getFileId());
            if (enhancedFile == null || enhancedFile.getPid() == null) {
                continue;
            }
            
            FileVO parentFileVO = parentFileMap.get(enhancedFile.getPid());
            if (parentFileVO == null) {
                continue;
            }
            
            DatasetVersionFile parentVersionFile = parentVersionFileMap.get(parentFileVO.getId());
            if (parentVersionFile == null) {
                continue;
            }
            
            enhancedToParentVersionFileMap.put(datasetVersionFile.getId(), parentVersionFile);
            enhancedToParentFileVOMap.put(datasetVersionFile.getId(), parentFileVO);
        }
        
        // 分批批量查询所有父版本文件的标注信息，避免IN子句过大
        List<Long> parentVersionFileIds = new ArrayList<>(enhancedToParentVersionFileMap.values().stream()
                .map(DatasetVersionFile::getId)
                .distinct()
                .collect(Collectors.toList()));
        
        Map<Long, List<Long>> parentVersionFileToLabelsMap = new HashMap<>();
        if (!parentVersionFileIds.isEmpty()) {
            List<List<Long>> versionFileIdBatches = CollectionUtil.split(parentVersionFileIds, BATCH_QUERY_SIZE);
            for (List<Long> batch : versionFileIdBatches) {
                Map<Long, List<Long>> batchLabelsMap = dataFileAnnotationService.findInfoByVersionIds(
                        enhanceTaskSplitBO.getDatasetId(), 
                        batch);
                parentVersionFileToLabelsMap.putAll(batchLabelsMap);
            }
        }
        
        Map<Long, List<Long>> versionFileIdToLabelIdsMap = new HashMap<>();
        Map<Long, String> versionFileIdToFileNameMap = new HashMap<>();
        
        // 遍历所有增强文件，收集标签关联信息
        for (DatasetVersionFile datasetVersionFile : datasetVersionFileList) {
            File enhancedFile = enhancedFileMap.get(datasetVersionFile.getFileId());
            if (enhancedFile == null || enhancedFile.getPid() == null) {
                continue;
            }
            
            DatasetVersionFile parentVersionFile = enhancedToParentVersionFileMap.get(datasetVersionFile.getId());
            FileVO parentFileVO = enhancedToParentFileVOMap.get(datasetVersionFile.getId());
            
            if (parentVersionFile == null || parentFileVO == null) {
                continue;
            }
            
            // 从批量查询结果中获取标注信息
            List<Long> labelIds = parentVersionFileToLabelsMap.get(parentVersionFile.getId());
            
            if (labelIds != null && !labelIds.isEmpty()) {
                versionFileIdToLabelIdsMap.put(datasetVersionFile.getId(), labelIds);
                versionFileIdToFileNameMap.put(datasetVersionFile.getId(), datasetVersionFile.getFileName());
            }
        }
        
        // 批量保存标签关联到数据库
        if (!versionFileIdToLabelIdsMap.isEmpty()) {
            dataFileAnnotationService.insertAnnotationFileBatch(
                    enhanceTaskSplitBO.getDatasetId(), 
                    versionFileIdToLabelIdsMap, 
                    versionFileIdToFileNameMap);
        }
        
        // 标注文件处理完全由Python端负责，Java端不再处理MinIO标注文件

        taskService.finishTaskFile(enhanceTaskSplitBO, fileNum);
    }

}
