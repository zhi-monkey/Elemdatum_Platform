package org.dlut.adv.mineai.model.dto;

import lombok.Data;
import org.dlut.adv.mineai.core.entity.ModelJob;

import java.util.Date;

/**
 * 训练任务概览DTO，用于大屏展示
 * 包含任务基本信息、进度、资源使用、排队时间等
 */
@Data
public class TrainingTaskOverviewDTO {
    /**
     * 任务ID
     */
    private Long id;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 训练进度（百分比，0-100）
     */
    private Double progress;

    /**
     * 资源使用信息（统一格式：CPU核心数、GPU数量、显存大小、内存）
     * 格式：CPU: X核, GPU: Y个, 显存: ZGiB, 内存: WGB
     */
    private String resourceUsage;

    /**
     * 排队时间（毫秒）
     * 当前时间 - 任务创建时间
     */
    private Long queuingTime;

    /**
     * 创建者（暂不实现，预留字段）
     */
    private String creator;

    /**
     * 从ModelJob转换为TrainingTaskOverviewDTO
     */
    public static TrainingTaskOverviewDTO fromModelJob(ModelJob job, Double progress, String creator) {
        TrainingTaskOverviewDTO dto = new TrainingTaskOverviewDTO();
        dto.setId(job.getId());
        dto.setName(job.getName());
        dto.setStatus(job.getStatus());
        dto.setCreateTime(job.getCreateTime());
        dto.setProgress(progress != null ? progress : 0.0);
        dto.setCreator(creator != null ? creator : "");

        // 构建资源使用信息
        StringBuilder resourceUsage = new StringBuilder();
        if (job.getCpus() != null && !job.getCpus().isEmpty()) {
            resourceUsage.append("CPU: ").append(job.getCpus()).append("核");
        }
        if (job.getGpuCount() != null && job.getGpuCount() > 0) {
            if (resourceUsage.length() > 0) resourceUsage.append(", ");
            resourceUsage.append("GPU: ").append(job.getGpuCount()).append("个");
        }
        if (job.getGpuMemory() != null && !job.getGpuMemory().isEmpty()) {
            if (resourceUsage.length() > 0) resourceUsage.append(", ");
            resourceUsage.append("显存: ").append(job.getGpuMemory());
        }
        if (job.getMemory() != null && !job.getMemory().isEmpty()) {
            if (resourceUsage.length() > 0) resourceUsage.append(", ");
            resourceUsage.append("内存: ").append(job.getMemory());
        }
        dto.setResourceUsage(resourceUsage.length() > 0 ? resourceUsage.toString() : "未配置");

        // 计算排队时间（毫秒）
        if (job.getCreateTime() != null) {
            long queuingTime = System.currentTimeMillis() - job.getCreateTime().getTime();
            dto.setQueuingTime(queuingTime > 0 ? queuingTime : 0);
        } else {
            dto.setQueuingTime(0L);
        }

        return dto;
    }
}

