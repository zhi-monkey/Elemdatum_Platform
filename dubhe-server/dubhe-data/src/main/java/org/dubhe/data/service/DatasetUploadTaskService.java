package org.dubhe.data.service;

import org.dubhe.data.domain.entity.DatasetUploadTask;

/**
 * @description 数据集上传任务服务接口
 * @date 2024-01-01
 */
public interface DatasetUploadTaskService {
    
    /**
     * 创建上传任务
     * @param datasetId 数据集ID
     * @param fileCount 文件数量
     * @return 任务ID
     */
    String createTask(Long datasetId, Integer fileCount);
    
    /**
     * 根据任务ID查询任务
     * @param taskId 任务ID
     * @return 任务信息
     */
    DatasetUploadTask getTaskById(String taskId);
    
    /**
     * 更新任务状态为成功
     * @param taskId 任务ID
     */
    void updateTaskSuccess(String taskId);
    
    /**
     * 更新任务状态为失败
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    void updateTaskFailed(String taskId, String errorMessage);
    
    /**
     * 清理任务内存占用
     * @param taskId 任务ID
     */
    void cleanupTask(String taskId);
}