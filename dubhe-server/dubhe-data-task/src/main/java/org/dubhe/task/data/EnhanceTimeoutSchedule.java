package org.dubhe.task.data;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.dubhe.biz.base.constant.MagicNumConstant;
import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.dubhe.biz.redis.utils.RedisUtils;
import org.dubhe.data.domain.entity.DatasetOperationEvent;
import org.dubhe.data.domain.entity.Task;
import org.dubhe.data.machine.enums.DataStateEnum;
import org.dubhe.data.service.DatasetOperationEventService;
import org.dubhe.data.service.DatasetService;
import org.dubhe.data.service.TaskService;
import org.dubhe.task.constant.TaskQueueNameEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author mingming
 * @date 2025/06/26
 */
@Component
@Slf4j
public class EnhanceTimeoutSchedule {
    @Autowired
    private TaskService taskService;
    @Autowired
    private DatasetService datasetService;
    @Autowired
    private RedisUtils redisUtils;

    /**
     * 抽帧任务超时时间（分钟）
     */
    @Value("${recycle.task.timeout-minutes.enhance:60}")
    private Integer taskTimeoutMinutes;

    @Autowired
    private DatasetOperationEventService datasetOperationEventService;

    @Scheduled(initialDelay = 30 * 1000, fixedDelay = 1000 * 5 * 60)
    public void checkEnhanceTimeoutTasks() {
        // 获取处理中的任务（状态为1或2的任务）
        Integer[] statuses = new Integer[]{MagicNumConstant.ONE, MagicNumConstant.TWO};
        QueryWrapper<Task> processingQuery = new QueryWrapper<>();
        processingQuery.lambda().eq(Task::getType, MagicNumConstant.THREE)
                .in(Task::getStatus, statuses)
                .eq(Task::isStop, false);

        List<Task> processingTasks = taskService.selectByQueryWrapper(processingQuery);
        if (processingTasks == null || processingTasks.isEmpty()) {
            return;
        }
        // 检查每个处理中的任务是否超时
        for (Task task : processingTasks) {
            Date createTime = task.getCreateTime();
            log.info("正在监测数据增强任务(ID:{}, 数据集ID:{}), 创建时间:{}", task.getId(), task.getDatasetId(), createTime);
            if (createTime == null) {
                log.error("数据增强任务(ID:{}, 数据集ID:{})的创建时间未设置，无法判断是否超时", task.getId(), task.getDatasetId());
                updateDatasetStatus(task);
                continue;
            }
            Calendar timeout = Calendar.getInstance();
            timeout.setTime(createTime);
            timeout.add(Calendar.MINUTE, taskTimeoutMinutes);

            if (Calendar.getInstance().after(timeout)) {
                // 任务已超时，将状态更新为失败
                task.setStop(true);
                task.setStatus(MagicNumConstant.FOUR);
                taskService.updateByTaskId(task);

                LogUtil.warn(LogEnum.BIZ_DATASET,
                        "数据增强任务(ID:{}, 数据集ID:{})已超时，超时时间设置为{}分钟",
                        task.getId(), task.getDatasetId(), taskTimeoutMinutes);
                datasetOperationEventService.createAndInsertEvent(task.getDatasetId(),new Date(),"数据增强任务超时", DatasetOperationEvent.EventType.INFO,DatasetOperationEvent.OperationType.DATA_AUGMENTATION);

                // 修改数据集状态
                updateDatasetStatus(task);
                // 清理相关的Redis数据
                cleanupRedisData(task);
            }
        }
    }

    /**
     * 清理任务相关的Redis数据
     *
     * @param task 需要清理的任务
     */
    private void cleanupRedisData(Task task) {
        try {
            String taskQueuePattern = TaskQueueNameEnum.getTemplate(
                    TaskQueueNameEnum.TASK,
                    TaskQueueNameEnum.TaskQueueConfigEnum.IMGPROCESS,
                    String.valueOf(task.getDatasetId()),
                    task.getId().toString()
            );

            String detailPattern = TaskQueueNameEnum.getTemplate(
                    TaskQueueNameEnum.DETAIL,
                    TaskQueueNameEnum.TaskQueueConfigEnum.IMGPROCESS,
                    String.valueOf(task.getDatasetId()),
                    task.getId().toString(),
                    "*"
            );

            // 查找并删除详情键
            List<String> detailKeys = redisUtils.scan(detailPattern);
            if (detailKeys != null && !detailKeys.isEmpty()) {
                for (String key : detailKeys) {
                    redisUtils.del(key);
                }
            }

            // 删除任务队列
            redisUtils.del(taskQueuePattern);

            LogUtil.info(LogEnum.BIZ_DATASET, "已清理超时任务(ID:{})的Redis数据", task.getId());
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "清理超时任务Redis数据失败:{}", e);
        }
    }

    private void updateDatasetStatus(Task task) {
        datasetService.updateStatus(task.getDatasetId(), DataStateEnum.getState((Integer) redisUtils.hget("datasetOriginStatus", String.valueOf(task.getDatasetId()))));
    }
}
