package org.dubhe.data.service.task;

import org.dubhe.data.constant.TaskStatus;
import org.dubhe.data.domain.vo.TaskStatusVO;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.*;


//数据集合并切分用的
@Service
public class TaskStatusService {

    // 使用内存存储，生产环境建议用Redis或数据库
    private final Map<String, TaskStatusVO> taskStatusMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    // 存储CompletableFuture引用
    private final Map<String, CompletableFuture<Void>> taskFutureMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 定时清理过期任务（24小时）
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredTasks, 1, 1, TimeUnit.HOURS);
    }

    /**
     * 创建任务
     */
    public TaskStatusVO createTask(String taskId) {
        TaskStatusVO taskStatus = new TaskStatusVO(taskId, TaskStatus.PENDING);
        taskStatusMap.put(taskId, taskStatus);
        return taskStatus;
    }
    /**
     * 设置任务的CompletableFuture引用
     */
    public void setTaskFuture(String taskId, CompletableFuture<Void> future) {
        taskFutureMap.put(taskId, future);

        // 任务完成后自动清理Future引用
        future.whenComplete((result, throwable) -> {
            taskFutureMap.remove(taskId);
            if (throwable != null) {
                // 如果是CancellationException，说明任务被取消
                if (throwable instanceof CancellationException) {
                    updateTaskStatus(taskId, TaskStatus.CANCELLED, "任务已被取消", null);
                } else {
                    updateTaskStatus(taskId, TaskStatus.FAILED, throwable.getMessage(), null);
                }
            }
        });
    }

    /**
     * 取消任务
     */
    public boolean cancelTask(String taskId) {
        CompletableFuture<Void> future = taskFutureMap.get(taskId);
        TaskStatusVO taskStatus = taskStatusMap.get(taskId);

        if (future == null || taskStatus == null) {
            return false;
        }

        // 检查任务是否已经完成
        if (taskStatus.getStatus() == TaskStatus.COMPLETED ||
                taskStatus.getStatus() == TaskStatus.FAILED ||
                taskStatus.getStatus() == TaskStatus.CANCELLED) {
            return false;
        }

        // 尝试取消CompletableFuture
        boolean cancelled = future.cancel(true);

        if (cancelled) {
            // 更新任务状态为已取消
            updateTaskStatus(taskId, TaskStatus.CANCELLED, "任务已被用户取消", null);
            System.out.println("cancancancancacancanac " + taskId);
            // 清理Future引用
            taskFutureMap.remove(taskId);
        }

        return cancelled;
    }

    /**
     * 更新任务状态
     */
    public void updateTaskStatus(String taskId, TaskStatus status, String errorMessage, Object result) {
        TaskStatusVO taskStatus = taskStatusMap.get(taskId);
        if (taskStatus != null) {
            taskStatus.setStatus(status);
            taskStatus.setErrorMessage(errorMessage);
            taskStatus.setResult(result);
        }
    }

    /**
     * 更新任务进度
     */
    public void updateTaskProgress(String taskId, int progress) {
        TaskStatusVO taskStatus = taskStatusMap.get(taskId);
        if (taskStatus != null) {
            taskStatus.setProgress(progress);
        }
    }

    /**
     * 获取任务状态
     */
    public TaskStatusVO getTaskStatus(String taskId) {
        return taskStatusMap.get(taskId);
    }

    /**
     * 检查任务是否被取消
     */
    public boolean isTaskCancelled(String taskId) {
        CompletableFuture<Void> future = taskFutureMap.get(taskId);
        return future != null && future.isCancelled();
    }

    /**
     * 清理过期任务
     */
    private void cleanupExpiredTasks() {
        long expireTime = System.currentTimeMillis() - 24 * 60 * 60 * 1000; // 24小时前

        // 清理过期的任务状态
        taskStatusMap.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().getUpdateTime() < expireTime;
            if (expired) {
                // 同时清理对应的Future引用
                String taskId = entry.getKey();
                CompletableFuture<Void> future = taskFutureMap.remove(taskId);
                if (future != null && !future.isDone()) {
                    future.cancel(true);
                }
            }
            return expired;
        });
    }




    @PreDestroy
    public void shutdown() {
        // 取消所有未完成的任务
        taskFutureMap.values().forEach(future -> {
            if (!future.isDone()) {
                future.cancel(true);
            }
        });

        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}