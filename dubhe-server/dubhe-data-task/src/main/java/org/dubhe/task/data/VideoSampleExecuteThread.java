package org.dubhe.task.data;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
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
import org.dubhe.task.constant.DataAlgorithmEnum;
import org.dubhe.task.constant.TaskQueueNameEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @description 视频抽帧任务分发线程
 * @date 2022-04-06
 */
@Slf4j
@Component
public class VideoSampleExecuteThread implements Runnable {

    /**
     * 路径名前缀
     */
    @Value("${storage.file-store-root-path:/nfs/}")
    private String prefixPath;

    /**
     * 抽帧任务超时时间（分钟）
     */
    @Value("${recycle.task.timeout-minutes.video-sample:60}")
    private Integer taskTimeoutMinutes;

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private DatasetOperationEventService datasetOperationEventService;

    /**
     * 启动生成任务线程
     */
    @PostConstruct
    public void start() {
        Thread thread = new Thread(this, "抽帧任务生成");
        log.info("抽帧任务生成");
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                work();
                TimeUnit.MILLISECONDS.sleep(MagicNumConstant.TEN_THOUSAND);
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "get frame_split task failed:{}", e);
            }
        }
    }

    /**
     * 单个任务处理
     */
    public void work() {
        // 获取一个待抽帧任务
        QueryWrapper<Task> pendingQuery = new QueryWrapper<>();
        pendingQuery.lambda().eq(Task::getStatus, MagicNumConstant.ZERO).eq(Task::getType, MagicNumConstant.FIVE)
                .eq(Task::isStop, false);
        List<Task> pendingTasks = taskService.selectByQueryWrapper(pendingQuery);
        
        // 添加调试信息：输出查询到的待处理任务数量
        log.info("查询到的待处理视频抽帧任务数量: {}", pendingTasks.size());

        // 检查处理中的任务是否超时
        checkVideoSampleTimeoutTasks();

        Integer[] statuses = new Integer[]{MagicNumConstant.ONE, MagicNumConstant.TWO};
        QueryWrapper<Task> proceedQuery = new QueryWrapper<>();
        proceedQuery.lambda().eq(Task::getType, MagicNumConstant.FIVE).in(Task::getStatus, statuses);
        List<Task> proceedTasks = taskService.selectByQueryWrapper(proceedQuery);
        List<Long> proceedDatasetIds = proceedTasks.stream().map(Task::getDatasetId).collect(Collectors.toList());
        
        // 添加调试信息：输出正在处理中的任务信息
        log.info("当前正在处理中的任务数量: {}, 涉及数据集ID列表: {}", proceedTasks.size(), proceedDatasetIds);
        
        //只能处理单个数据集的抽帧任务
        List<Task> filterTasks = pendingTasks.stream().filter(task -> !proceedDatasetIds.contains(task.getDatasetId())).collect(
                Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Task::getDatasetId))), ArrayList::new));
        
        // 添加调试信息：输出过滤后的任务信息
        log.info("经过数据集冲突过滤后的任务数量: {}", filterTasks.size());
        filterTasks.forEach(task -> {
            log.info("待处理任务详情 - ID: {}, 数据集ID: {}, 目标文件ID: {}, 帧间隔: {}, 起始时间: {}, 结束时间: {}", 
                     task.getId(), task.getDatasetId(), task.getTargetId(), task.getFrameInterval(), 
                     task.getStartTime(), task.getEndTime());
        });
        
        filterTasks.stream().forEach(filterTask -> {
            int count = taskService.updateTaskStatus(filterTask.getId(), MagicNumConstant.ZERO, MagicNumConstant.ONE);
            if (count != 0) {
                log.info("成功更新任务状态，准备执行任务 - ID: {}", filterTask.getId());
                execute(filterTask);
                taskService.updateTaskStatus(filterTask.getId(), MagicNumConstant.ONE, MagicNumConstant.TWO);
                log.info("任务执行完成，更新状态为已完成部分 - ID: {}", filterTask.getId());
            } else {
                log.info("任务状态更新失败，可能已被其他线程处理 - ID: {}", filterTask.getId());
            }
        });
    }

    /**
     * 检查视频抽帧任务是否超时
     */
    private void checkVideoSampleTimeoutTasks() {
        // 获取处理中的任务（状态为1或2的任务）
        Integer[] statuses = new Integer[]{MagicNumConstant.ONE, MagicNumConstant.TWO};
        QueryWrapper<Task> processingQuery = new QueryWrapper<>();
        processingQuery.lambda().eq(Task::getType, MagicNumConstant.FIVE)
                .in(Task::getStatus, statuses)
                .eq(Task::isStop, false);

        List<Task> processingTasks = taskService.selectByQueryWrapper(processingQuery);
        if (processingTasks == null || processingTasks.isEmpty()) {
            return;
        }
        // 检查每个处理中的任务是否超时
        for (Task task : processingTasks) {
            Date createTime = task.getCreateTime();
            log.info("正在监测视频抽帧任务(ID:{}, 数据集ID:{}), 创建时间:{}", task.getId(), task.getDatasetId(), createTime);
            if (createTime == null) {
               log.error("视频抽帧任务(ID:{}, 数据集ID:{})的创建时间未设置，无法判断是否超时", task.getId(), task.getDatasetId());
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
                        "视频抽帧任务(ID:{}, 数据集ID:{})已超时，超时时间设置为{}分钟",
                        task.getId(), task.getDatasetId(), taskTimeoutMinutes);
                datasetOperationEventService.createAndInsertEvent(task.getDatasetId(),new Date(),"视频抽帧任务超时", DatasetOperationEvent.EventType.INFO,DatasetOperationEvent.OperationType.VIDEO_FRAME_EXTRACTION);

                // 修改数据集状态
                updateDatasetStatus(task);
                // 清理相关的Redis数据
                cleanupRedisData(task);
            }
        }
    }

    private void updateDatasetStatus(Task task) {
        datasetService.updateStatus(task.getDatasetId(), DataStateEnum.getState((Integer) redisUtils.hget("datasetOriginStatus", String.valueOf(task.getDatasetId()))));
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
                    TaskQueueNameEnum.TaskQueueConfigEnum.VIDEOSAMPLE,
                    String.valueOf(task.getDatasetId()),
                    task.getId().toString()
            );

            String detailPattern = TaskQueueNameEnum.getTemplate(
                    TaskQueueNameEnum.DETAIL,
                    TaskQueueNameEnum.TaskQueueConfigEnum.VIDEOSAMPLE,
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

    /**
     * 执行抽帧任务
     *
     * @param task 任务详情
     */
    public void execute(Task task) {
        videoSampleExecute(task);
    }

    /**
     * 采样任务
     *
     * @param task 任务详情
     */
    private void videoSampleExecute(Task task) {
        java.io.File file = new java.io.File(prefixPath + task.getUrl());
        if (!file.exists()) {
            LogUtil.error(LogEnum.BIZ_DATASET, "视频抽帧任务(ID:{}, 数据集ID:{})失败，文件:{}不存在", task.getId(), task.getDatasetId(), prefixPath + task.getUrl());
            taskService.taskError(task.getId());
        }
        log.info("视频抽帧任务(ID:{}, 数据集ID:{})开始", task.getId(), task.getDatasetId());
        
        // 添加详细的调试信息
        log.info("视频抽帧任务详细参数 - ID: {}, 数据集ID: {}, 文件路径: {}, 帧间隔: {}, 起始时间: {}, 结束时间: {}, 目标ID: {}", 
                 task.getId(), task.getDatasetId(), task.getUrl(), task.getFrameInterval(), 
                 task.getStartTime(), task.getEndTime(), task.getTargetId());
        
        double frameRate = 0;
        int startFrame = 1;
        int endFrame = 1;
        int lengthInFrames = 0;
        List<Integer> frames = new ArrayList<>();
        try {
            FFmpegFrameGrabber ff = FFmpegFrameGrabber.createDefault(file);
            ff.start();
            if (task.getStartTime() == null) {
                lengthInFrames = ff.getLengthInVideoFrames();
                log.info("视频总帧数: {}", lengthInFrames);
                for (int i = 1; i < lengthInFrames; ) {
                    frames.add(i);
                    i += Optional.ofNullable(task.getFrameInterval()).orElse(15);
                }
            } else {
                frameRate = ff.getFrameRate();
                startFrame += (int) (Optional.ofNullable(task.getStartTime()).orElse(0) * frameRate);
                endFrame += (int) (Optional.ofNullable(task.getEndTime()).orElse(0) * frameRate);
                log.info("视频帧率: {}, 起始帧: {}, 结束帧: {}", frameRate, startFrame, endFrame);
                for (int i = startFrame; i < endFrame; ) {
                    frames.add(i);
                    i += Optional.ofNullable(task.getFrameInterval()).orElse(15);
                }
            }
            ff.stop();
            
            // 输出抽帧计划
            log.info("计划抽取的帧列表: {}", frames);
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "get frames error:{}", e);
            taskService.taskError(task.getId());
            datasetService.updateStatus(task.getDatasetId(), DataStateEnum.getState((Integer) redisUtils.hget("datasetOriginStatus", String.valueOf(task.getDatasetId()))));
        }
        // 20帧一组
        List<List<Integer>> framesSplitTasks = CollectionUtil.split(frames, 20);
        taskService.setTaskTotal(task.getId(), framesSplitTasks.size());
        log.info("抽帧任务分组详情 - 总组数: {}, 每组20帧", framesSplitTasks.size());
        
        AtomicInteger j = new AtomicInteger(1);
        framesSplitTasks.forEach(framesSplitTask -> {
            log.info("处理第{}组抽帧任务，包含帧: {}", j.get(), framesSplitTask);
            JSONObject param = new JSONObject();
            param.put("datasetId", task.getDatasetId() + ":" + j);
            param.put("path", prefixPath + task.getUrl());
            param.put("frames", framesSplitTask);
            param.put("id", task.getId().toString());
            param.put("algorithm", DataAlgorithmEnum.VIDEO_SAMPLE.getAlgorithmType());
            
            // 从 taskParams 读取分辨率参数
            Integer resolutionType = 1;
            Integer customWidth = null;
            Integer customHeight = null;
            
            if (task.getTaskParams() != null && !task.getTaskParams().isEmpty()) {
                try {
                    JSONObject taskParams = JSONObject.parseObject(task.getTaskParams());
                    resolutionType = taskParams.getInteger("resolutionType");
                    customWidth = taskParams.getInteger("customWidth");
                    customHeight = taskParams.getInteger("customHeight");
                    log.info("从taskParams读取分辨率参数 - 类型: {}, 宽: {}, 高: {}", resolutionType, customWidth, customHeight);
                } catch (Exception e) {
                    log.warn("解析taskParams失败，使用默认分辨率参数: {}", e.getMessage());
                }
            }
            
            // 添加分辨率参数到Redis
            param.put("resolutionType", Optional.ofNullable(resolutionType).orElse(1));
            if (customWidth != null) {
                param.put("customWidth", customWidth);
            }
            if (customHeight != null) {
                param.put("customHeight", customHeight);
            }
            JSONObject paramKey = new JSONObject();
            paramKey.put("datasetIdKey", task.getDatasetId() + ":" + j);
            String detailKey = UUID.randomUUID().toString();
            String taskQueue = TaskQueueNameEnum.getTemplate(
                    TaskQueueNameEnum.TASK,
                    TaskQueueNameEnum.TaskQueueConfigEnum.VIDEOSAMPLE,
                    String.valueOf(task.getDatasetId()),
                    task.getId().toString()
            );

            String detail = TaskQueueNameEnum.getTemplate(
                    TaskQueueNameEnum.DETAIL,
                    TaskQueueNameEnum.TaskQueueConfigEnum.VIDEOSAMPLE,
                    String.valueOf(task.getDatasetId()),
                    task.getId().toString(),
                    detailKey
            );

            log.info("准备将任务放入Redis队列 - 队列名: {}, 详情键: {}, 任务参数: {}", taskQueue, detail, param.toJSONString());
            redisUtils.zSet(taskQueue, -1, detailKey);
            redisUtils.set(detail, param);
            log.info("已将第{}组抽帧任务放入Redis队列 - 队列名: {}, 详情键: {}", j.get(), taskQueue, detail);
            j.addAndGet(1);
        });
    }
}
