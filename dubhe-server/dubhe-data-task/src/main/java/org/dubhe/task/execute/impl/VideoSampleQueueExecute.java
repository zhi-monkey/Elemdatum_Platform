

package org.dubhe.task.execute.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dubhe.biz.base.constant.MagicNumConstant;
import org.dubhe.biz.redis.utils.RedisUtils;
import org.dubhe.data.domain.entity.DatasetOperationEvent;
import org.dubhe.data.domain.entity.Task;
import org.dubhe.data.machine.enums.DataStateEnum;
import org.dubhe.data.service.DatasetOperationEventService;
import org.dubhe.data.service.TaskService;
import org.dubhe.data.service.impl.DatasetServiceImpl;
import org.dubhe.data.service.impl.FileServiceImpl;
import org.dubhe.task.execute.AbstractAlgorithmExecute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@DependsOn("springContextHolder")
@Component
@Slf4j
public class VideoSampleQueueExecute extends AbstractAlgorithmExecute {

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private FileServiceImpl fileServiceImpl;

    @Autowired
    @Lazy
    private TaskService taskService;
    @Autowired
    private DatasetServiceImpl datasetServiceImpl;
    @Autowired
    private DatasetOperationEventService datasetOperationEventService;


    @Override
    public void finishExecute(JSONObject taskDetail) {
        Long datasetId = null;
        try {
            // 检查object字段是否存在，如果不存在说明annotation key已被删除，无法处理
            Object objectField = taskDetail.get("object");
            if (objectField == null) {
                log.warn("[finishExecute] object字段为null，annotation key可能已被删除，无法处理此segment");
                return;
            }
            
            JSONObject detailObject = JSON.parseObject(objectField.toString(),JSONObject.class);
            String datasetIdAndSub = detailObject.getString("datasetIdAndSub");
            Integer height = Integer.valueOf(detailObject.getString("height"));
            Integer width = Integer.valueOf(detailObject.getString("width"));
            List<String> pictureNames = JSON.parseObject(detailObject.getString("pictureNames"), ArrayList.class);
            Integer segment = Integer.valueOf(StringUtils.substringAfter(String.valueOf(datasetIdAndSub), ":"));
            QueryWrapper<Task> taskQueryWrapper = new QueryWrapper<>();
            taskQueryWrapper.lambda().eq(Task::getId, Long.valueOf(detailObject.getString("id")));
            Task task = taskService.selectOne(taskQueryWrapper);
            datasetId = task.getDatasetId();
            
            // 使用数据库原子操作：只有当finished等于segment-1时才更新
            // 这样保证了即使多个segment同时到达，也只有正确的那个能更新成功
            Integer expectedFinished = segment - 1;
            
            int rows = taskService.finishFileIfExpected(task.getId(), expectedFinished);
            
            if (rows > 0) {
                // 更新成功，说明这个segment是按顺序到达的
                
                // 重新查询task获取最新的finished值，确保传递给videSampleFinished的是最新数据
                task = taskService.selectOne(taskQueryWrapper);
                
                datasetOperationEventService.createAndInsertEvent(datasetId,new Date(),"视频抽帧完成", DatasetOperationEvent.EventType.INFO,DatasetOperationEvent.OperationType.VIDEO_FRAME_EXTRACTION);
                fileServiceImpl.videSampleFinished(pictureNames, task, height, width);
                
                // 再次查询task，检查是否所有segment都已完成
                task = taskService.selectOne(taskQueryWrapper);
                
                if (task.getFinished().equals(task.getTotal())) {
                    task.setStatus(MagicNumConstant.THREE);
                    taskService.updateByTaskId(task);
                }
            } else {
                // 更新失败，说明这个segment不是下一个应该处理的，重新放回队列等待
                
                Object queueObject = taskDetail.get("queueObject");
                if (queueObject != null) {
                    String originalKey = queueObject.toString();
                    // 构造finished队列名（替换annotation为finished）
                    String finishedQueueKey = originalKey.replace("annotation", "finished");
                    
                    // 构造detail key并保存任务参数（防止detail key被删除）
                    String detailKey = originalKey.replace("annotation", "detail");
                    // 保存原始任务参数到detail key（包含object字段，防止annotation key被删除后无法恢复）
                    JSONObject paramToSave = new JSONObject(taskDetail);
                    paramToSave.remove("queueObject");
                    // 确保id字段是字符串格式（与初始推送保持一致）
                    if (paramToSave.containsKey("id") && !(paramToSave.get("id") instanceof String)) {
                        paramToSave.put("id", paramToSave.get("id").toString());
                    }
                    // 将JSONObject转换为字符串保存到Redis
                    redisUtils.set(detailKey, paramToSave.toJSONString());
                    
                    // 重新入队：队列名使用finished格式，但value保持原始annotation格式（以便能找到detail）
                    redisUtils.zAdd(finishedQueueKey, System.currentTimeMillis() / 1000, 
                        String.format("\"%s\"", originalKey).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } else {
                    log.error("✗✗✗ queueObject为空，无法重新入队 segment {}", segment);
                }
            }
        } catch (NumberFormatException e) {
            log.error("video sample error cause of {}", e.getMessage());
            datasetOperationEventService.createAndInsertEvent(datasetId,new Date(),"视频抽帧中途出错", DatasetOperationEvent.EventType.ERROR,DatasetOperationEvent.OperationType.VIDEO_FRAME_EXTRACTION);
            QueryWrapper<Task> taskQueryWrapper = new QueryWrapper<>();
            taskQueryWrapper.lambda().eq(Task::getId, Long.valueOf(taskDetail.getString("id")));
            Task task = taskService.selectOne(taskQueryWrapper);
            taskService.taskError(task.getId());
            datasetServiceImpl.updateStatus(task.getDatasetId(), DataStateEnum.getState((Integer) redisUtils.hget("datasetOriginStatus", String.valueOf(task.getDatasetId()))));
        }
    }

    @Override
    public void failExecute(JSONObject failDetail) {
        String datasetIdAndSub = failDetail.getString("datasetIdAndSub");
        fileServiceImpl.videoSampleFailed(datasetIdAndSub);
        QueryWrapper<Task> taskQueryWrapper = new QueryWrapper<>();
        taskQueryWrapper.lambda().eq(Task::getId, Long.valueOf(failDetail.getString("id")));
        Task task = taskService.selectOne(taskQueryWrapper);
        task.setStatus(MagicNumConstant.FOUR);
        taskService.updateByTaskId(task);
        datasetOperationEventService.createAndInsertEvent((long) Integer.parseInt(StringUtils.substringBefore(String.valueOf(datasetIdAndSub), ":")),new Date(),"视频抽帧中途出错", DatasetOperationEvent.EventType.ERROR,DatasetOperationEvent.OperationType.VIDEO_FRAME_EXTRACTION);
    }

    @Override
    public boolean checkStop(Object object, String queueName, JSONObject taskDetail) {
        // id可能是String或Long类型，需要兼容处理
        Long taskId = null;
        if (taskDetail.containsKey("id")) {
            Object idObj = taskDetail.get("id");
            try {
                if (idObj instanceof Long) {
                    taskId = (Long) idObj;
                } else if (idObj instanceof String) {
                    taskId = Long.parseLong((String) idObj);
                } else if (idObj instanceof Integer) {
                    taskId = ((Integer) idObj).longValue();
                }
            } catch (Exception e) {
                log.error("[checkStop] 解析taskId失败: {}, idObj: {}", e.getMessage(), idObj);
            }
        }
        
        if (taskId == null) {
            log.error("[checkStop] 无法获取taskId，taskDetail: {}", taskDetail);
            return false;
        }
        
        return taskService.isStop(taskId);
    }

    @Override
    public void deleteRedisKey(Object object, String detailQueue) throws Exception {
        super.deleteRedisKey(object, detailQueue);
    }

}
