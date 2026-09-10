package org.dubhe.data.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.dubhe.data.domain.entity.DatasetUploadTask;
import org.dubhe.data.service.DatasetUploadTaskService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @description 数据集上传任务服务实现（基于内存存储）
 * @date 2024-01-01
 */
@Slf4j
@Service
public class DatasetUploadTaskServiceImpl implements DatasetUploadTaskService {
    
    // 使用内存存储任务信息
    private final Map<String, DatasetUploadTask> taskMap = new ConcurrentHashMap<>();
    
    // 延迟清理任务的线程池
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(2);
    
    @Override
    public String createTask(Long datasetId, Integer fileCount) {
        Date now = new Date();
        DatasetUploadTask task = DatasetUploadTask.builder()
                .id(java.util.UUID.randomUUID().toString())
                .datasetId(datasetId)
                .status(DatasetUploadTask.Status.PROCESSING)
                .fileCount(fileCount != null ? fileCount : 0)
                .createdTime(now)
                .updatedTime(now)
                .build();
        
        taskMap.put(task.getId(), task);
        log.info("创建上传任务，taskId: {}, datasetId: {}, fileCount: {}", 
                task.getId(), datasetId, fileCount);
        return task.getId();
    }
    
    @Override
    public DatasetUploadTask getTaskById(String taskId) {
        return taskMap.get(taskId);
    }
    
    @Override
    public void updateTaskSuccess(String taskId) {
        DatasetUploadTask task = taskMap.get(taskId);
        if (task != null) {
            task.setStatus(DatasetUploadTask.Status.SUCCESS);
            task.setUpdatedTime(new Date());
            log.info("更新任务状态为成功，taskId: {}", taskId);
            // 任务完成后延迟清理
            scheduleTaskCleanup(taskId);
        }
    }
    
    @Override
    public void updateTaskFailed(String taskId, String errorMessage) {
        DatasetUploadTask task = taskMap.get(taskId);
        if (task != null) {
            task.setStatus(DatasetUploadTask.Status.FAILED);
            // 限制错误信息长度
            if (errorMessage != null && errorMessage.length() > 500) {
                errorMessage = errorMessage.substring(0, 500);
            }
            task.setErrorMessage(errorMessage);
            task.setUpdatedTime(new Date());
            log.info("更新任务状态为失败，taskId: {}, errorMessage: {}", taskId, errorMessage);
            // 任务完成后延迟清理
            scheduleTaskCleanup(taskId);
        }
    }
    
    @Override
    public void cleanupTask(String taskId) {
        DatasetUploadTask removedTask = taskMap.remove(taskId);
        if (removedTask != null) {
            log.info("清理任务内存占用，taskId: {}", taskId);
        }
    }
    
    /**
     * 延迟清理任务，确保前端在任务完成后仍能查询到任务状态
     * @param taskId 任务ID
     */
    private void scheduleTaskCleanup(String taskId) {
        cleanupExecutor.schedule(() -> {
            try {
                cleanupTask(taskId);
            } catch (Exception e) {
                log.error("延迟清理任务失败，taskId: {}", taskId, e);
            }
        }, 3, TimeUnit.MINUTES); // 3分钟后清理任务
    }
}