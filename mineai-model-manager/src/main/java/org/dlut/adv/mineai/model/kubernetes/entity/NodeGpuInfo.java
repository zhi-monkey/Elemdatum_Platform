package org.dlut.adv.mineai.model.kubernetes.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NodeGpuInfo {
    private String nodeName;
    private Integer totalGpuCount;      // 物理GPU总数
    private Integer availableGpuCount;  // 可用GPU数量
    private List<GpuInfo> gpus;         // GPU列表
    private Long totalGpuMemory;        // 总显存
    private Long usedGpuMemory;         // 已使用显存
    private Double gpuMemoryPercent;    // 显存使用率

    public NodeGpuInfo() {
        this.gpus = new ArrayList<>();
    }

    public void calculateTotals() {
        this.totalGpuCount = gpus.size();
        this.totalGpuMemory = gpus.stream()
                .mapToLong(GpuInfo::getTotalMemory)
                .sum();
        this.usedGpuMemory = gpus.stream()
                .mapToLong(GpuInfo::getUsedMemory)
                .sum();
        this.gpuMemoryPercent = totalGpuMemory > 0 ?
                (usedGpuMemory * 100.0 / totalGpuMemory) : 0.0;
        this.availableGpuCount = (int) gpus.stream()
                .filter(gpu -> "Healthy".equals(gpu.getHealth()))
                .count();
    }
}
